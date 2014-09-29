package com.quantimodo.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.quantimodo.sync.model.ApplicationData;
import com.quantimodo.sync.model.SensorData;

import java.util.ArrayList;
import java.util.List;

public class Global {
    public static final String QM_SCOPES = "writemeasurements";
    public static final String QM_ID = "QuantiSync";
    public static final String QM_SECRET = "joeyrpmslrf8w4wef6weq9qytpu50374bosgr8";
    public static final String historyPackage = "syncs.sqlite";

    public static List<ApplicationData> applications = new ArrayList<ApplicationData>();
    public static List<SensorData> sensors = new ArrayList<SensorData>();

    // FLags
    public static boolean welcomeCompleted;

    // Account
    public static String qmAccountName;

    // Preferences
    public static int syncInterval;
    public static int moodInterval;
    public static boolean wifiOnly;
    public static boolean chargingOnly;
    public static boolean showSyncNotification;

    public static void init(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Global.qmAccountName = prefs.getString("qmAccountName", null);

        Global.syncInterval = Integer.valueOf(prefs.getString("syncInterval", "2"));
        Global.moodInterval = Integer.valueOf(prefs.getString("moodInterval", "1"));
        Global.showSyncNotification = prefs.getBoolean("showSyncNotification", false);
        Global.wifiOnly = prefs.getBoolean("wifiOnly", true);
        Global.chargingOnly = prefs.getBoolean("wifiOnly", true);
    }
}
