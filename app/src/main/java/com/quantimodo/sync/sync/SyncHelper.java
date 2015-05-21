package com.quantimodo.sync.sync;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.quantimodo.sync.Global;

public class SyncHelper {

    private static final int ID = 14754;
    public static final long RUN_SYNC_EVERY_TIME = 12 * 60 * 60 * 1000; //12 Hours

    public static boolean isSync(Context context){
        SharedPreferences preferences = context.getSharedPreferences(Global.QUANTIMODO_PREF_KEY,Context.MODE_PRIVATE);
        return preferences.getBoolean(Global.PREF_SYNC_ENABLED,false);
    }

    public static void scheduleSync(Context ctx){
        Intent intent = new Intent(ctx,SyncReciever.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(ctx,ID,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME,5000,RUN_SYNC_EVERY_TIME,alarmIntent);
        SharedPreferences preferences = ctx.getSharedPreferences(Global.QUANTIMODO_PREF_KEY,Context.MODE_PRIVATE);
        preferences.edit().putBoolean(Global.PREF_SYNC_ENABLED,true).apply();
    }

    public static void unscheduleSync(Context ctx){
        Intent intent = new Intent(ctx,SyncReciever.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(ctx,ID,intent,PendingIntent.FLAG_NO_CREATE);

        SharedPreferences preferences = ctx.getSharedPreferences(Global.QUANTIMODO_PREF_KEY, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(Global.PREF_SYNC_ENABLED,false).apply();
        if (alarmIntent != null){
            AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(alarmIntent);
        }
    }

//    public static void cleanSyncSettings(Context ctx){
//        SharedPreferences sp = ctx.getSharedPreferences(Global.QUANTIMODO_PREF_KEY, Context.MODE_MULTI_PROCESS);
//        sp.edit().putLong(Global.PREF_SYNC_FROM,0).commit();
//    }

    public static void invokeSync(Context context){
        Intent intent = new Intent(context,SyncReciever.class);
        context.sendBroadcast(intent);
    }

}
