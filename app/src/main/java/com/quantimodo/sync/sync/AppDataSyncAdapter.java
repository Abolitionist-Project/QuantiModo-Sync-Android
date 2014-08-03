package com.quantimodo.sync.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import com.quantimodo.etl.ETL;
import com.quantimodo.android.sdk.QuantimodoApi;
import com.quantimodo.android.sdk.model.Measurement;
import com.quantimodo.android.sdk.model.MeasurementSet;
import com.quantimodo.sync.Global;
import com.quantimodo.sync.Log;
import com.quantimodo.sync.databases.QuantiSyncDbHelper;
import com.quantimodo.sync.model.ApplicationData;
import com.quantimodo.sync.model.HistoryItem;
import com.quantimodo.sync.su.SU;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

public class AppDataSyncAdapter extends AbstractThreadedSyncAdapter
{
	private final Context context;
	private final Handler handler;
	private final AccountManager accountManager;

	private String authToken;
	
	private ArrayList<ApplicationData> syncingApps;

	public AppDataSyncAdapter(Context context, boolean autoInitialize)
	{
		super(context, autoInitialize);

		this.context = context;
		this.handler = new Handler();
		this.accountManager = AccountManager.get(context);
	}

	@Override
	public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult)
	{
		try
		{
            QuantimodoApi qmClient = QuantimodoApi.getInstance();
			authToken = qmClient.getAccessTokenSynchronous(context, account, Global.QM_ID, Global.QM_SECRET, Global.QM_SCOPES);
			if(authToken == null)
			{
				Log.i("Authentication failed");
				return;
			}

			getSyncingApps();
			sync(syncingApps);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private boolean gotApps;
	private void getSyncingApps()
	{
		gotApps = false;

		ApplicationData.getCompatibleApplications(context, handler, new ApplicationData.OnCompatibleApplicationsLoaded()
		{
			@Override
			public void onComplete()
			{
				gotApps = true;
			}
		});

		while (!gotApps)
		{
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException ignored)
			{
			}
		}

		syncingApps = new ArrayList<ApplicationData>();
		for (ApplicationData currentData : Global.applications)
		{
			if (currentData.isInstalled && currentData.isSyncEnabled() && !syncingApps.contains(currentData))
			{
				syncingApps.add(currentData);
			}
		}
	}

	private void sync(ArrayList<ApplicationData> syncingApps)
	{
		ArrayList<MeasurementSet> allNewData = new ArrayList<MeasurementSet>();

		List<HistoryItem> historyItems = new ArrayList<HistoryItem>();

		Process suProcess = SU.startProcess();  // Start an elevated process
		DataOutputStream outputS = new DataOutputStream(suProcess.getOutputStream());
		BufferedReader inputS = new BufferedReader(new InputStreamReader(suProcess.getInputStream()));

		ETL etl = new ETL();                    // Get an ETL instance

		String cachePath = context.getCacheDir().getPath();
		Log.i("Cache root path at: " + cachePath);

		for (ApplicationData currentApp : syncingApps)
		{
			Log.i(currentApp.packageName + ": Started sync");
			File cacheFile = null;
			try
			{
				String filepath = cachePath + "/" + currentApp.packageName + "-" + currentApp.dataFile.getName();
				cacheFile = new File(filepath);

				ArrayList<MeasurementSet> oldData = null;
				ArrayList<MeasurementSet> newData;
				if (cacheFile.exists())
				{
					Log.i(currentApp.packageName + ": Extracting old data");
					oldData = etl.handle(cacheFile);

					Log.i(currentApp.packageName + " Read " + oldData.size() + " old measurement sets");
				}
				else
				{
					Log.i(currentApp.packageName + ": No previous sync");
				}

				Log.i(currentApp.packageName + ": Caching new data");
				SU.copyToCache(outputS, inputS, currentApp.dataFile.getPath(), cacheFile.getPath());

				if (cacheFile.exists())
				{
					Log.i(currentApp.packageName + ": Extracting new data");
					newData = etl.handle(cacheFile);

					Log.i(currentApp.packageName + " Read " + newData.size() + " new measurement sets");
				}
				else
				{
					Log.i(currentApp.packageName + ": No file cached, no data to sync");
					historyItems.add(new HistoryItem(currentApp.packageName, currentApp.label, new Date(), 0, "Couldn't cache application data"));
					return;
				}

				Log.i(currentApp.packageName + ": Comparing new and old data");

				newData = getNewData(newData, oldData);
				allNewData.addAll(newData);

				int totalNewMeasurements = 0;
				for(MeasurementSet filteredSet : newData)
				{
					totalNewMeasurements += filteredSet.measurements.size();
				}

				Log.i(currentApp.packageName + ": New measurements: " + totalNewMeasurements + " across " + newData.size() + " sets");
				historyItems.add(new HistoryItem(currentApp.packageName, currentApp.label, new Date(), totalNewMeasurements, null));
			}
			catch (Exception e)
			{
				e.printStackTrace();
				if (cacheFile != null)
				{
					boolean deletedFile = cacheFile.delete();
					Log.i("Deleted cache file: " + cacheFile.getPath() + ", result: " + deletedFile);
				}

				historyItems.add(new HistoryItem(currentApp.packageName, currentApp.label, new Date(), 0, e.getMessage()));
			}
		}

		SU.stopProcess(suProcess, false);

		if (allNewData.size() > 0)
		{
            QuantimodoApi qmClient = QuantimodoApi.getInstance();
			int syncState = qmClient.putMeasurementsSynchronous(context, authToken, allNewData);

			//TODO get proper error messages
			if(syncState < 0)
			{
				saveHistory(historyItems, "Failed to upload to QuantiModo");
			}
			else
			{
				saveHistory(historyItems, null);
			}
		}
		else
		{
			saveHistory(historyItems, null);
		}

		SharedPreferences prefs = context.getSharedPreferences("com.quantimodo.app_preferences", Context.MODE_MULTI_PROCESS);
		prefs.edit().putLong("lastSuccessfulAppSync", System.currentTimeMillis()).commit();
	}

	/*
	** Compares newData and oldData. Stores difference in newData
	** @return  number of new measurements
	*/
	private ArrayList<MeasurementSet>  getNewData(ArrayList<MeasurementSet> newData, ArrayList<MeasurementSet> oldData)
	{
		if (oldData == null)
		{
			return newData;
		}

		long startTime = System.currentTimeMillis();

		ArrayList<MeasurementSet> filteredNewData = new ArrayList<MeasurementSet>();
		for (MeasurementSet newSet : newData)
		{
			boolean containsSet = false;
			for (MeasurementSet oldSet : oldData)
			{
				// Check if the measurement sets match
				if(newSet.name.equals(oldSet.name) && newSet.category.equals(oldSet.category) && newSet.unit.equals(oldSet.unit) && newSet.source.equals(oldSet.source))
				{
					containsSet = true;

					ArrayList<Measurement> newMeasurements = new ArrayList<Measurement>();

					// Loop through measurements to get the new ones
					for(Measurement newMeasurement : newSet.measurements)
					{
						boolean containsMeasurement = false;
						for(Measurement oldMeasurement : oldSet.measurements)
						{
							if(newMeasurement.timestamp == oldMeasurement.timestamp && newMeasurement.value == oldMeasurement.value)
							{
								containsMeasurement = true;
								break;
							}
						}

						if(!containsMeasurement)
						{
							newMeasurements.add(newMeasurement);
						}
					}

					// If there are new measurements, add the new set + new measurements to the filtered list
					if(newMeasurements.size() > 0)
					{
						newSet.measurements = newMeasurements;
						filteredNewData.add(newSet);
					}

					break;
				}
			}

			if(!containsSet)
			{
				filteredNewData.add(newSet);
			}
		}

		Log.i("Filtered in " + (System.currentTimeMillis() - startTime) + "ms");

		return filteredNewData;
	}

	private void saveHistory(List<HistoryItem> historyItems, String syncError)
	{
		QuantiSyncDbHelper dbHelper = new QuantiSyncDbHelper(getContext());
		SQLiteDatabase db = dbHelper.getWritableDatabase();

		long now = System.currentTimeMillis();

		for(HistoryItem currentHistoryItem : historyItems)
		{
			final ContentValues values = new ContentValues();
			values.put(QuantiSyncDbHelper.History.PACKAGENAME, currentHistoryItem.packageName);
			values.put(QuantiSyncDbHelper.History.PACKAGELABEL, currentHistoryItem.packageLabel);
			values.put(QuantiSyncDbHelper.History.TIMESTAMP, now);
			values.put(QuantiSyncDbHelper.History.SYNCCOUNT, currentHistoryItem.syncCount);
			if(syncError != null)
			{
				values.put(QuantiSyncDbHelper.History.SYNCERROR, syncError);
			}
			else
			{
				values.put(QuantiSyncDbHelper.History.SYNCERROR, currentHistoryItem.syncError);
			}

			//TODO find out if it's possible to use the ContentProvider here. I was getting a NullPointerException contentResolver.insert
			//ContentResolver contentResolver = context.getContentResolver();
			//contentResolver.insert(QuantiSyncContentProvider.CONTENT_URI_HISTORY, values);

			db.insert(QuantiSyncDbHelper.History.TABLE_NAME, null, values);
		}

		dbHelper.close();
	}
}
