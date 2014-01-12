package com.quantimodo.sync.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import com.quantimodo.etl.ETL;
import com.quantimodo.sdk.QuantimodoClient;
import com.quantimodo.sdk.model.QuantimodoMeasurement;
import com.quantimodo.sync.Global;
import com.quantimodo.sync.Log;
import com.quantimodo.sync.model.ApplicationData;
import com.quantimodo.sync.su.SU;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
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
				cacheFile = new File(cachePath + "/" + currentApp.packageName + "-" + currentApp.dataFile.getName());

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
			}
			catch (Exception e)
			{
				e.printStackTrace();
				if (cacheFile != null)
				{
					//boolean deletedFile = cacheFile.delete();
					//Log.i("Deleted cache file: " + cacheFile.getPath() + ", result: " + deletedFile);
				}
			}

		}

		SU.stopProcess(suProcess, false);

		if (newOrUpdatedData.size() > 0)
		{
			QuantimodoClient qmClient = QuantimodoClient.getInstance();
			qmClient.putMeasurementsSynchronous(context, authToken, newOrUpdatedData);
		}

		SharedPreferences prefs = context.getSharedPreferences("com.quantimodo.app_preferences", Context.MODE_MULTI_PROCESS);
		prefs.edit().putLong("lastSuccessfulAppSync", System.currentTimeMillis()).commit();
	}
}
