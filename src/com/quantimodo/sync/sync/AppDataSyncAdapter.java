package com.quantimodo.sync.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import com.quantimodo.etl.ETL;
import com.quantimodo.sdk.QuantimodoClient;
import com.quantimodo.sdk.model.QuantimodoMeasurement;
import com.quantimodo.sync.Global;
import com.quantimodo.sync.Log;
import com.quantimodo.sync.databases.QuantiSyncDbHelper;
import com.quantimodo.sync.model.ApplicationData;
import com.quantimodo.sync.model.HistoryThing;
import com.quantimodo.sync.su.SU;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
			QuantimodoClient qmClient = QuantimodoClient.getInstance();
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
		List<QuantimodoMeasurement> newOrUpdatedData = new ArrayList<QuantimodoMeasurement>();
		List<HistoryThing> historyThings = new ArrayList<HistoryThing>();

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

				List<QuantimodoMeasurement> oldData = null;
				List<QuantimodoMeasurement> newData;
				if (cacheFile.exists())
				{
					Log.i(currentApp.packageName + ": Extracting old data");
					oldData = Arrays.asList(etl.handle(cacheFile));

					Log.i(currentApp.packageName + " Read " + oldData.size() + " old records");
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
					newData = Arrays.asList(etl.handle(cacheFile));

					Log.i(currentApp.packageName + " Read " + newData.size() + " new records");
				}
				else
				{
					Log.i(currentApp.packageName + ": No file cached, no data to sync");
					historyThings.add(new HistoryThing(currentApp.packageName, currentApp.label, new Date(), newOrUpdatedData.size(), "Couldn't cache application data"));
					return;
				}

				if (oldData == null)
				{
					newOrUpdatedData.addAll(newData);
				}
				else
				{
					Log.i(currentApp.packageName + ": Comparing new and old data");
					for (QuantimodoMeasurement newRecord : newData)
					{
						boolean contains = false;
						for (QuantimodoMeasurement oldRecord : oldData)
						{
							if (newRecord.timestamp == oldRecord.timestamp && newRecord.variable.equals(oldRecord.variable))
							{
								contains = true;
								if (newRecord.value != oldRecord.value)
								{
									newOrUpdatedData.add(newRecord);
								}
								break;
							}
						}

						if (!contains)
						{
							newOrUpdatedData.add(newRecord);
						}
					}
				}

				historyThings.add(new HistoryThing(currentApp.packageName, currentApp.label, new Date(), newOrUpdatedData.size(), null));
			}
			catch (Exception e)
			{
				e.printStackTrace();
				if (cacheFile != null)
				{
					boolean deletedFile = cacheFile.delete();
					Log.i("Deleted cache file: " + cacheFile.getPath() + ", result: " + deletedFile);
				}

				historyThings.add(new HistoryThing(currentApp.packageName, currentApp.label, new Date(), newOrUpdatedData.size(), e.getMessage()));
			}
		}

		SU.stopProcess(suProcess, false);

		if (newOrUpdatedData.size() > 0)
		{
			QuantimodoClient qmClient = QuantimodoClient.getInstance();
			int syncState = qmClient.putMeasurementsSynchronous(context, authToken, newOrUpdatedData);

			//TODO get proper error messages
			if(syncState < 0)
			{
				saveHistory(historyThings, "Failed to upload to QuantiModo");
			}
			else
			{
				saveHistory(historyThings, null);
			}
		}

		SharedPreferences prefs = context.getSharedPreferences("com.quantimodo.app_preferences", Context.MODE_MULTI_PROCESS);
		prefs.edit().putLong("lastSuccessfulAppSync", System.currentTimeMillis()).commit();
	}

	private void saveHistory(List<HistoryThing> historyThings, String syncError)
	{
		QuantiSyncDbHelper dbHelper = new QuantiSyncDbHelper(getContext());
		SQLiteDatabase db = dbHelper.getWritableDatabase();

		long now = System.currentTimeMillis();

		for(HistoryThing currentHistoryThing : historyThings)
		{
			final ContentValues values = new ContentValues();
			values.put(QuantiSyncDbHelper.History.PACKAGENAME, currentHistoryThing.packageName);
			values.put(QuantiSyncDbHelper.History.PACKAGELABEL, currentHistoryThing.packageLabel);
			values.put(QuantiSyncDbHelper.History.TIMESTAMP, now);
			values.put(QuantiSyncDbHelper.History.SYNCCOUNT, currentHistoryThing.syncCount);
			if(syncError != null)
			{
				values.put(QuantiSyncDbHelper.History.SYNCERROR, syncError);
			}
			else
			{
				values.put(QuantiSyncDbHelper.History.SYNCERROR, currentHistoryThing.syncError);
			}

			//TODO find out if it's possible to use the ContentProvider here. I was getting a NullPointerException contentResolver.insert
			//ContentResolver contentResolver = context.getContentResolver();
			//contentResolver.insert(QuantiSyncContentProvider.CONTENT_URI_HISTORY, values);

			db.insert(QuantiSyncDbHelper.History.TABLE_NAME, null, values);
		}

		dbHelper.close();
	}
}
