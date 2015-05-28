package com.quantimodo.sync.sync;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

public class SyncReciever extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context,SyncService.class);
        intent.putExtra("withLock",true);
        startWakefulService(context,service);
    }
}
