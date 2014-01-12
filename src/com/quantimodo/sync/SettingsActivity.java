package com.quantimodo.sync;

import android.accounts.Account;
import android.app.ActionBar;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.*;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import com.quantimodo.sdk.QuantimodoClient;
import com.quantimodo.sync.receivers.SyncTimeReceiver;

import java.text.SimpleDateFormat;
import java.util.Date;


public class SettingsActivity extends PreferenceActivity
{
	private static ListPreference lpSyncInterval;
	private static CheckBoxPreference cbWiFiOnly;
	private static CheckBoxPreference cbChargingOnly;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		addPreferencesFromResource(R.xml.settings);

		initGeneralPreferences();
		initSynchronizationPreferences();
	}

	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		/*if (isSinglePane())
		{
			initGeneralPreferences();
			initSynchronizationPreferences();
		}*/
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		Global.init(this);
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		Global.init(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
	}

	void initGeneralPreferences()
	{
		QuantimodoClient qmClient = QuantimodoClient.getInstance();

		Preference preference = findPreference("quantimodoAccount");
		preference.setSummary(qmClient.getAccount(this).name);
		preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
		{
			@Override public boolean onPreferenceClick(Preference preference)
			{
				String[] authorities = {"com.quantimodo.sync.content-appdata"};
				Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
				intent.putExtra(Settings.EXTRA_AUTHORITIES, authorities);
				startActivity(intent);
				return true;
			}
		});
	}

	void initSynchronizationPreferences()
	{
		QuantimodoClient qmClient = QuantimodoClient.getInstance();
		Account qmAccount = qmClient.getAccount(this);
		boolean syncAutomatically = ContentResolver.getSyncAutomatically(qmAccount, "com.quantimodo.sync.content-appdata");

		CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference("syncAppData");
		checkBoxPreference.setOnPreferenceChangeListener(onSyncAppDataChanged);
		checkBoxPreference.setChecked(syncAutomatically);
		if (syncAutomatically)
		{
			SharedPreferences prefs = this.getSharedPreferences("com.quantimodo.app_preferences", Context.MODE_MULTI_PROCESS);
			long lastSuccessfulAppSync = prefs.getLong("lastSuccessfulAppSync", -1);

			if (lastSuccessfulAppSync != -1)
			{
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
				String formattedDate = simpleDateFormat.format(new Date(lastSuccessfulAppSync));
				Resources res = getResources();
				String text = String.format(res.getString(R.string.pref_sync_appdata_enabled), formattedDate);
				checkBoxPreference.setSummary(text);
			}
			else
			{
				checkBoxPreference.setSummary(R.string.pref_sync_appdata_neversynced);
			}
		}
		else
		{
			checkBoxPreference.setSummary(R.string.pref_sync_appdata_disabled);
		}

		lpSyncInterval = (ListPreference) findPreference("syncInterval");
		String currentValue = lpSyncInterval.getEntries()[Global.syncInterval].toString();
		lpSyncInterval.setSummary(currentValue);
		lpSyncInterval.setOnPreferenceChangeListener(onSyncIntervalChanged);
		lpSyncInterval.setEnabled(syncAutomatically);

		cbWiFiOnly = (CheckBoxPreference) findPreference("wifiOnly");
		cbWiFiOnly.setOnPreferenceChangeListener(onWifiOnlyChanged);
		cbWiFiOnly.setEnabled(syncAutomatically);
		if (Global.wifiOnly)
		{
			cbWiFiOnly.setSummary(R.string.pref_wifiOnly_true);
		}
		else
		{
			cbWiFiOnly.setSummary(R.string.pref_wifiOnly_false);
		}

		cbChargingOnly = (CheckBoxPreference) findPreference("chargingOnly");
		cbChargingOnly.setOnPreferenceChangeListener(onChargingOnlyChanged);
		cbChargingOnly.setEnabled(syncAutomatically);
		if (Global.wifiOnly)
		{
			cbChargingOnly.setSummary(R.string.pref_chargingOnly_true);
		}
		else
		{
			cbChargingOnly.setSummary(R.string.pref_chargingOnly_false);
		}
	}


	private static Preference.OnPreferenceChangeListener onSyncAppDataChanged = new Preference.OnPreferenceChangeListener()
	{
		@Override public boolean onPreferenceChange(Preference preference, Object o)
		{
			QuantimodoClient qmClient = QuantimodoClient.getInstance();

			boolean syncAutomatically = (Boolean) o;

			ContentResolver.setSyncAutomatically(qmClient.getAccount(preference.getContext()), "com.quantimodo.sync.content-appdata", syncAutomatically);

			lpSyncInterval.setEnabled(syncAutomatically);
			cbWiFiOnly.setEnabled(syncAutomatically);
			cbChargingOnly.setEnabled(syncAutomatically);

			if (syncAutomatically)
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
				long lastSuccessfulAppSync = prefs.getLong("lastSuccessfulAppSync", -1);

				if (lastSuccessfulAppSync != -1)
				{
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
					String formattedDate = simpleDateFormat.format(new Date(lastSuccessfulAppSync));
					Resources res = preference.getContext().getResources();
					String text = String.format(res.getString(R.string.pref_sync_appdata_enabled), formattedDate);
					preference.setSummary(text);
				}
				else
				{
					preference.setSummary(R.string.pref_sync_appdata_neversynced);
				}
			}
			else
			{
				preference.setSummary(R.string.pref_sync_appdata_disabled);
			}
			return true;
		}
	};

	private static Preference.OnPreferenceChangeListener onWifiOnlyChanged = new Preference.OnPreferenceChangeListener()
	{
		@Override public boolean onPreferenceChange(Preference preference, Object o)
		{
			Global.wifiOnly = (Boolean) o;
			if (Global.wifiOnly)
			{
				preference.setSummary(R.string.pref_wifiOnly_true);
			}
			else
			{
				preference.setSummary(R.string.pref_wifiOnly_false);
			}
			return true;
		}
	};

	private static Preference.OnPreferenceChangeListener onChargingOnlyChanged = new Preference.OnPreferenceChangeListener()
	{
		@Override public boolean onPreferenceChange(Preference preference, Object o)
		{
			Global.chargingOnly = (Boolean) o;
			if (Global.chargingOnly)
			{
				preference.setSummary(R.string.pref_chargingOnly_true);
			}
			else
			{
				preference.setSummary(R.string.pref_chargingOnly_false);
			}
			return true;
		}
	};

	private static Preference.OnPreferenceChangeListener onSyncIntervalChanged = new Preference.OnPreferenceChangeListener()
	{
		@Override public boolean onPreferenceChange(Preference preference, Object o)
		{
			Global.syncInterval = Integer.valueOf((String) o);

			String currentValue = ((ListPreference) preference).getEntries()[Global.syncInterval].toString();
			preference.setSummary(currentValue);

			SyncTimeReceiver.setAlarm(preference.getContext(), Global.syncInterval);

			return true;
		}
	};
}
