package com.quantimodo.sync.receivers;

import android.accounts.Account;
import android.content.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.quantimodo.sdk.Quantimodo;
import com.quantimodo.sync.Global;
import com.quantimodo.sync.Log;

public class SyncTimeReceiver extends BroadcastReceiver
{
	public static final int ALARM_REQUEST_CODE = 1337;
	public static final int INTERVAL_MANUAL = 0;
	public static final int INTERVAL_HOURLY = 1;
	public static final int INTERVAL_TWICE_DAILY = 2;
	public static final int INTERVAL_DAILY = 3;
	public static final int INTERVAL_WEEKLY = 4;
	public static final int INTERVAL_DEBUG = 5;

	public static final long INTERVAL_HOURLY_MILLIS = 3600000;
	public static final long INTERVAL_TWICE_DAILY_MILLIS = 43200000;
	public static final long INTERVAL_DAILY_MILLIS = 86400000;
	public static final long INTERVAL_WEEKLY_MILLIS = 604800000;
	public static final long INTERVAL_DEBUG_MILLIS = 30000;

	@Override
	public void onReceive(Context context, Intent intent)
	{

	}

	/*
	 * Method that checks whether this sync can continue or not based on the current network and charging state
	 *
	 * @return true if this sync can continue, false if it should be postponed
	 */
	public boolean canSync(Context context)
	{
		Intent batteryStatus = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
		if (isCharging || !Global.chargingOnly)
		{
			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo netInfo = cm.getActiveNetworkInfo();
			if (netInfo != null && netInfo.isConnectedOrConnecting())
			{
				if (netInfo.getType() == ConnectivityManager.TYPE_WIFI || !Global.wifiOnly)
				{
					return true;
				}
				else
				{
					Log.i("Postpone sync, WiFi required");
				}
			}
			else
			{
				Log.i("Postpone sync, no internet connection");
			}
		}
		else
		{
			Log.i("Postpone sync, charging or full battery required");
		}
		return false;
	}

	public static void setAlarm(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Global.syncInterval = Integer.valueOf(prefs.getString("syncInterval", "2"));

		setAlarm(context, Global.syncInterval);
	}

	public static void setAlarm(Context context, int syncInterval)
	{
		Account qmAccount = Quantimodo.getAccount(context);
		if (qmAccount != null)
		{
			long interval = 0;
			switch (syncInterval)
			{
			case INTERVAL_MANUAL:
				ContentResolver.removePeriodicSync(qmAccount, "com.quantimodo.sync.appdata.provider", new Bundle());
				return;
			case INTERVAL_HOURLY:
				interval = INTERVAL_HOURLY_MILLIS;
				break;
			case INTERVAL_TWICE_DAILY:
				interval = INTERVAL_TWICE_DAILY_MILLIS;
				break;
			case INTERVAL_DAILY:
				interval = INTERVAL_DAILY_MILLIS;
				break;
			case INTERVAL_WEEKLY:
				interval = INTERVAL_WEEKLY_MILLIS;
				break;
			case INTERVAL_DEBUG:
				interval = INTERVAL_DEBUG_MILLIS;
				break;
			}

			Log.i("Set " + qmAccount.name + " sync every " + interval);
			ContentResolver.setSyncAutomatically(qmAccount, "com.quantimodo.sync.appdata.provider", true);
			ContentResolver.addPeriodicSync(qmAccount, "com.quantimodo.sync.appdata.provider", new Bundle(), interval / 1000);
		}
	}
}
