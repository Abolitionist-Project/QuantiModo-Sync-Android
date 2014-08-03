package com.quantimodo.sync.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AppDataSyncService extends Service
{
	private static final Object sSyncAdapterLock = new Object();
	private static AppDataSyncAdapter syncAdapter = null;

	@Override
	public void onCreate()
	{
		synchronized (sSyncAdapterLock)
		{
			if (syncAdapter == null)
			{
				syncAdapter = new AppDataSyncAdapter(this, true);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return syncAdapter.getSyncAdapterBinder();
	}
}
