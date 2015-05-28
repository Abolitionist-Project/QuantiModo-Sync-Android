package com.quantimodo.sync.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.quantimodo.sync.sync.SyncHelper;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SyncTimeReceiver.setAlarm(context);

        if (SyncHelper.isSync(context)){
            SyncHelper.scheduleSync(context);
        }
    }
}
