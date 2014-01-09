package com.quantimodo.sync.sync;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.IBinder;
import com.quantimodo.sync.fragments.ApplicationListFragment;
import com.quantimodo.sync.model.ApplicationData;

import java.util.ArrayList;

public class AppDataSyncService extends Service
{
	private static Handler handler;
	private static Resources res;

	private ArrayList<ApplicationData> syncingApps;

	long startTime;

	private static final Object sSyncAdapterLock = new Object();
	private static AppDataSyncAdapter syncAdapter = null;

	@Override
	public void onCreate()
	{
		synchronized (sSyncAdapterLock)
		{
			if (syncAdapter == null)
			{
				syncAdapter = new AppDataSyncAdapter(getApplicationContext(), true);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return syncAdapter.getSyncAdapterBinder();
	}

/*	@Override public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.i("Service started");

		startTime = System.currentTimeMillis();

		Global.init(this);

		handler = new Handler();
		res = getResources();

		getSyncingApps(new OnSyncingAppsLoaded()
		{
			@Override public void onComplete(ArrayList<ApplicationData> syncingApps)
			{
				AppDataSyncService.this.syncingApps = syncingApps;
				new SyncThread(AppDataSyncService.this, syncingApps).start();
			}
		});

		return START_NOT_STICKY;
	}

	@Override public void onDestroy()
	{
		super.onDestroy();

		Log.i("Sync done in: " + (System.currentTimeMillis() - startTime) + " seconds");

		resetSyncStates();
	}*/

	private interface OnSyncingAppsLoaded
	{
		public void onComplete(ArrayList<ApplicationData> syncingApps);
	}

	public void update(final ApplicationData currentApp, final int stringResource)
	{
		try
		{
			handler.post(new Runnable()
			{
				@Override public void run()
				{
					if (stringResource > 0)
					{
						currentApp.syncStatus = res.getString(stringResource);
					}
					else
					{
						currentApp.syncStatus = null;
					}
					ApplicationListFragment.update();
				}
			});
		}
		catch (Exception ignored)
		{
		}
	}

	public void resetSyncStates()
	{
		if (syncingApps != null)
		{
			for (ApplicationData currentApp : syncingApps)
			{
				currentApp.syncStatus = null;
			}
			ApplicationListFragment.update();
		}
	}

	/*public static void postpone(Context context)
	{
		AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent newIntent = new Intent(context, SyncTimeReceiver.class);
		PendingIntent operation = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, newIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		manager.set(AlarmManager.RTC, System.currentTimeMillis() + 600000, operation);
	}*/
}
