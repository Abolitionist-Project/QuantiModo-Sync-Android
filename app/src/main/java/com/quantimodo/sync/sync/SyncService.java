package com.quantimodo.sync.sync;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import com.crashlytics.android.Crashlytics;
import com.quantimodo.android.sdk.QuantimodoApi;
import com.quantimodo.android.sdk.QuantimodoApiV2;
import com.quantimodo.android.sdk.SdkResponse;
import com.quantimodo.android.sdk.model.HistoryMeasurement;
import com.quantimodo.android.sdk.model.Measurement;
import com.quantimodo.android.sdk.model.MeasurementSet;
import com.quantimodo.etl.ETL;
import com.quantimodo.sync.AuthHelper;
import com.quantimodo.sync.Global;
import com.quantimodo.sync.Log;
import com.quantimodo.sync.QApp;
import com.quantimodo.sync.databases.QuantiSyncDbHelper;
import com.quantimodo.sync.model.ApplicationData;
import com.quantimodo.sync.model.HistoryItem;
import com.quantimodo.sync.su.SU;
import io.fabric.sdk.android.Fabric;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class SyncService extends IntentService {

    private long lastSuccessfulMoodSync;
    private SharedPreferences mSharePrefs;

    @Inject
    QuantimodoApiV2 mClient;

    String mToken;

    @Inject
    AuthHelper authHelper;
    private boolean gotApps;
    private final Handler handler;

    private ArrayList<ApplicationData> syncingApps;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
    public SyncService() {
        super("Sync");
        handler = new Handler();
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        startSync(intent.getExtras());
        SyncReciever.completeWakefulIntent(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Fabric.with(this, new Crashlytics());
        QApp.inject(this);
        setIntentRedelivery(false);
    }

    private void getSyncingApps() {
        gotApps = false;

        ApplicationData.getCompatibleApplications(this, handler, new ApplicationData.OnCompatibleApplicationsLoaded() {
            @Override
            public void onComplete() {
                gotApps = true;
            }
        });

        while (!gotApps) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

        syncingApps = new ArrayList<>();
        for (ApplicationData currentData : Global.applications) {
            if (currentData.isInstalled && currentData.isSyncEnabled() && !syncingApps.contains(currentData)) {
                syncingApps.add(currentData);
            }
        }
    }

    private void startSync(Bundle bundle) {
        Log.i("Sync invoked!");

        try
        {
            if (!beforeSync(bundle)){
                Log.i("Error during sync!");
                return;
            }

            mToken = authHelper.getAuthTokenWithRefresh();
            getSyncingApps();
            sync(syncingApps);
            afterSync();
        }
        catch (Exception e)
        {
            Crashlytics.getInstance().core.logException(e);
        }
    }


    private boolean beforeSync(Bundle bundle){

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if(activeNetwork == null){
            Log.i("No active network connection");
            return false;
        }

        return true;
    }

    private void afterSync(){
    }

    private void sync(ArrayList<ApplicationData> syncingApps) {
        ArrayList<MeasurementSet> allNewData = new ArrayList<MeasurementSet>();

        List<HistoryItem> historyItems = new ArrayList<HistoryItem>();

        Process suProcess = SU.startProcess();  // Start an elevated process
        DataOutputStream outputS = new DataOutputStream(suProcess.getOutputStream());
        BufferedReader inputS = new BufferedReader(new InputStreamReader(suProcess.getInputStream()));

        ETL etl = new ETL();                    // Get an ETL instance

        String cachePath = getCacheDir().getPath();
        Log.i("Cache root path at: " + cachePath);

        for (ApplicationData currentApp : syncingApps) {
            Log.i(currentApp.packageName + ": Started sync");
            File cacheFile = null;
            try {
                String filepath = cachePath + "/" + currentApp.packageName + "-" + currentApp.dataFile.getName();
                cacheFile = new File(filepath);

                ArrayList<MeasurementSet> oldData = null;
                ArrayList<MeasurementSet> newData;
                if (cacheFile.exists()) {
                    Log.i(currentApp.packageName + ": Extracting old data");
                    oldData = etl.handle(cacheFile);

                    Log.i(currentApp.packageName + " Read " + oldData.size() + " old measurement sets");
                } else {
                    Log.i(currentApp.packageName + ": No previous sync");
                }

                Log.i(currentApp.packageName + ": Caching new data");
                SU.copyToCache(outputS, inputS, currentApp.dataFile.getPath(), cacheFile.getPath());

                if (cacheFile.exists()) {
                    Log.i(currentApp.packageName + ": Extracting new data");
                    newData = etl.handle(cacheFile);

                    Log.i(currentApp.packageName + " Read " + newData.size() + " new measurement sets");
                } else {
                    Log.i(currentApp.packageName + ": No file cached, no data to sync");
                    historyItems.add(new HistoryItem(currentApp.packageName, currentApp.label, new Date(), 0, "Couldn't cache application data"));
                    return;
                }

                Log.i(currentApp.packageName + ": Comparing new and old data");

                newData = getNewData(newData, oldData);
                allNewData.addAll(newData);

                int totalNewMeasurements = 0;
                for (MeasurementSet filteredSet : newData) {
                    totalNewMeasurements += filteredSet.measurements.size();
                }

                Log.i(currentApp.packageName + ": New measurements: " + totalNewMeasurements + " across " + newData.size() + " sets");
                historyItems.add(new HistoryItem(currentApp.packageName, currentApp.label, new Date(), totalNewMeasurements, null));
            } catch (Exception e) {
                e.printStackTrace();
                if (cacheFile != null) {
                    boolean deletedFile = cacheFile.delete();
                    Log.i("Deleted cache file: " + cacheFile.getPath() + ", result: " + deletedFile);
                }

                historyItems.add(new HistoryItem(currentApp.packageName, currentApp.label, new Date(), 0, e.getMessage()));
            }
        }

        SU.stopProcess(suProcess, false);

        if (allNewData.size() > 0) {
            SdkResponse<Integer> syncState = mClient.putMeasurements(this, mToken, allNewData);

            if (!syncState.isSuccessful()) {
                saveHistory(historyItems, "Failed to upload to QuantiModo");

                //Sends message to Fabric if this is not regular error.
                if (syncState.getErrorCode() != SdkResponse.ERROR_AUTH && syncState.getErrorCode() != SdkResponse.ERROR_NO_INTERNET ){
                    Crashlytics.getInstance().core.log(syncState.getMessage());
                    if (syncState.getCause() != null) {
                        Crashlytics.getInstance().core.logException(syncState.getCause());
                    } else {
                        Crashlytics.getInstance().core.logException(new Exception(syncState.getMessage()));
                    }
                }

            } else {
                saveHistory(historyItems, null);
            }
        } else {
            saveHistory(historyItems, null);
        }

        SharedPreferences prefs = getSharedPreferences("com.quantimodo.app_preferences", Context.MODE_MULTI_PROCESS);
        prefs.edit().putLong("lastSuccessfulAppSync", System.currentTimeMillis()).commit();
    }

    /*
    ** Compares newData and oldData. Stores difference in newData
    ** @return  number of new measurements
    */
    private ArrayList<MeasurementSet> getNewData(ArrayList<MeasurementSet> newData, ArrayList<MeasurementSet> oldData) {
        if (oldData == null) {
            return newData;
        }

        long startTime = System.currentTimeMillis();

        ArrayList<MeasurementSet> filteredNewData = new ArrayList<MeasurementSet>();
        for (MeasurementSet newSet : newData) {
            boolean containsSet = false;
            for (MeasurementSet oldSet : oldData) {
                // Check if the measurement sets match
                if (newSet.name.equals(oldSet.name) && newSet.category.equals(oldSet.category) && newSet.unit.equals(oldSet.unit) && newSet.source.equals(oldSet.source)) {
                    containsSet = true;

                    ArrayList<Measurement> newMeasurements = new ArrayList<Measurement>();

                    // Loop through measurements to get the new ones
                    for (Measurement newMeasurement : newSet.measurements) {
                        boolean containsMeasurement = false;
                        for (Measurement oldMeasurement : oldSet.measurements) {
                            if (newMeasurement.timestamp == oldMeasurement.timestamp && newMeasurement.value == oldMeasurement.value) {
                                containsMeasurement = true;
                                break;
                            }
                        }

                        if (!containsMeasurement) {
                            newMeasurements.add(newMeasurement);
                        }
                    }

                    // If there are new measurements, add the new set + new measurements to the filtered list
                    if (newMeasurements.size() > 0) {
                        newSet.measurements = newMeasurements;
                        filteredNewData.add(newSet);
                    }

                    break;
                }
            }

            if (!containsSet) {
                filteredNewData.add(newSet);
            }
        }

        Log.i("Filtered in " + (System.currentTimeMillis() - startTime) + "ms");

        return filteredNewData;
    }

    private void saveHistory(List<HistoryItem> historyItems, String syncError) {
        QuantiSyncDbHelper dbHelper = new QuantiSyncDbHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long now = System.currentTimeMillis();

        for (HistoryItem currentHistoryItem : historyItems) {
            final ContentValues values = new ContentValues();
            values.put(QuantiSyncDbHelper.History.PACKAGENAME, currentHistoryItem.packageName);
            values.put(QuantiSyncDbHelper.History.PACKAGELABEL, currentHistoryItem.packageLabel);
            values.put(QuantiSyncDbHelper.History.TIMESTAMP, now);
            values.put(QuantiSyncDbHelper.History.SYNCCOUNT, currentHistoryItem.syncCount);
            if (syncError != null) {
                values.put(QuantiSyncDbHelper.History.SYNCERROR, syncError);
            } else {
                values.put(QuantiSyncDbHelper.History.SYNCERROR, currentHistoryItem.syncError);
            }

            db.insert(QuantiSyncDbHelper.History.TABLE_NAME, null, values);
        }

        dbHelper.close();
    }
}
