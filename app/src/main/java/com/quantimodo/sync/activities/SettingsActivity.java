package com.quantimodo.sync.activities;

import android.accounts.Account;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.*;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.*;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import com.quantimodo.android.sdk.Quantimodo;
import com.quantimodo.sync.AuthHelper;
import com.quantimodo.sync.Global;
import com.quantimodo.sync.QApp;
import com.quantimodo.sync.R;
import com.quantimodo.sync.receivers.SyncTimeReceiver;
import com.quantimodo.sync.sync.SyncHelper;
import com.uservoice.uservoicesdk.UserVoice;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;


public class SettingsActivity extends PreferenceActivity {
    private static ListPreference lpSyncInterval;
    private static CheckBoxPreference cbWiFiOnly;
    private static CheckBoxPreference cbChargingOnly;

    @Inject
    AuthHelper authHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        QApp.inject(this);
        addPreferencesFromResource(R.xml.settings);

        initGeneralPreferences();
        initSynchronizationPreferences();
    }

    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        /*if (isSinglePane())
		{
			initGeneralPreferences();
			initSynchronizationPreferences();
		}*/
    }

    @Override
    protected void onResume() {
        super.onResume();

        Global.init(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Global.init(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    void initGeneralPreferences() {
        Preference preference = findPreference("quantimodoAccount");

        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override public boolean onPreferenceClick(Preference preference)
            {
                if (!authHelper.isLoggedIn()){

                    Intent auth = new Intent(SettingsActivity.this,QuantimodoWebAuthenticatorActivity.class);
                    startActivity(auth);
                }  else {
                    new AlertDialog.Builder(SettingsActivity.this)
                            .setMessage(R.string.auth_logout)
                            .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    authHelper.logOut();
                                    initGeneralPreferences();
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create().show();
                }
                return true;
            }
        });

        if(authHelper.isLoggedIn()) {
            preference.setSummary("Logged in");
        } else {
            preference.setSummary("Not logged in");
        }

        preference = findPreference("prefUserVoiceHelp");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                UserVoice.launchUserVoice(SettingsActivity.this);
                return true;
            }
        });

        preference = findPreference("prefUserVoiceFeedback");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                UserVoice.launchForum(SettingsActivity.this);
                return true;
            }
        });

        preference = findPreference("prefUserVoiceContact");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                UserVoice.launchContactUs(SettingsActivity.this);
                return true;
            }
        });

        preference = findPreference("prefUserVoiceIdea");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                UserVoice.launchPostIdea(SettingsActivity.this);
                return true;
            }
        });
    }

    void initSynchronizationPreferences() {
        Account qmAccount = Quantimodo.getAccount(this.getApplicationContext());
        boolean syncAutomatically = SyncHelper.isSync(this);

        CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference("syncAppData");
        checkBoxPreference.setOnPreferenceChangeListener(onSyncAppDataChanged);
        checkBoxPreference.setChecked(syncAutomatically);
        if (syncAutomatically) {
            SharedPreferences prefs = this.getSharedPreferences("com.quantimodo.app_preferences", Context.MODE_MULTI_PROCESS);
            long lastSuccessfulAppSync = prefs.getLong("lastSuccessfulAppSync", -1);

            if (lastSuccessfulAppSync != -1) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                String formattedDate = simpleDateFormat.format(new Date(lastSuccessfulAppSync));
                Resources res = getResources();
                String text = String.format(res.getString(R.string.pref_sync_appdata_enabled), formattedDate);
                checkBoxPreference.setSummary(text);
            } else {
                checkBoxPreference.setSummary(R.string.pref_sync_appdata_neversynced);
            }
        } else {
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
        if (Global.wifiOnly) {
            cbWiFiOnly.setSummary(R.string.pref_wifiOnly_true);
        } else {
            cbWiFiOnly.setSummary(R.string.pref_wifiOnly_false);
        }

        cbChargingOnly = (CheckBoxPreference) findPreference("chargingOnly");
        cbChargingOnly.setOnPreferenceChangeListener(onChargingOnlyChanged);
        cbChargingOnly.setEnabled(syncAutomatically);
        if (Global.wifiOnly) {
            cbChargingOnly.setSummary(R.string.pref_chargingOnly_true);
        } else {
            cbChargingOnly.setSummary(R.string.pref_chargingOnly_false);
        }
    }


    private static Preference.OnPreferenceChangeListener onSyncAppDataChanged = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {

            boolean syncAutomatically = (Boolean) o;

            if (syncAutomatically){
                SyncHelper.scheduleSync(preference.getContext());
            } else {
                SyncHelper.unscheduleSync(preference.getContext());
            }

            lpSyncInterval.setEnabled(syncAutomatically);
            cbWiFiOnly.setEnabled(syncAutomatically);
            cbChargingOnly.setEnabled(syncAutomatically);

            if (syncAutomatically) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
                long lastSuccessfulAppSync = prefs.getLong("lastSuccessfulAppSync", -1);

                if (lastSuccessfulAppSync != -1) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                    String formattedDate = simpleDateFormat.format(new Date(lastSuccessfulAppSync));
                    Resources res = preference.getContext().getResources();
                    String text = String.format(res.getString(R.string.pref_sync_appdata_enabled), formattedDate);
                    preference.setSummary(text);
                } else {
                    preference.setSummary(R.string.pref_sync_appdata_neversynced);
                }
            } else {
                preference.setSummary(R.string.pref_sync_appdata_disabled);
            }
            return true;
        }
    };

    private static Preference.OnPreferenceChangeListener onWifiOnlyChanged = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            Global.wifiOnly = (Boolean) o;
            if (Global.wifiOnly) {
                preference.setSummary(R.string.pref_wifiOnly_true);
            } else {
                preference.setSummary(R.string.pref_wifiOnly_false);
            }
            return true;
        }
    };

    private static Preference.OnPreferenceChangeListener onChargingOnlyChanged = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            Global.chargingOnly = (Boolean) o;
            if (Global.chargingOnly) {
                preference.setSummary(R.string.pref_chargingOnly_true);
            } else {
                preference.setSummary(R.string.pref_chargingOnly_false);
            }
            return true;
        }
    };

    private static Preference.OnPreferenceChangeListener onSyncIntervalChanged = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            Global.syncInterval = Integer.valueOf((String) o);

            String currentValue = ((ListPreference) preference).getEntries()[Global.syncInterval].toString();
            preference.setSummary(currentValue);

            SyncTimeReceiver.setAlarm(preference.getContext(), Global.syncInterval);

            return true;
        }
    };
}
