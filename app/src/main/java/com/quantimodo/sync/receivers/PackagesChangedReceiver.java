package com.quantimodo.sync.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import com.quantimodo.sync.Global;
import com.quantimodo.sync.fragments.ApplicationListFragment;
import com.quantimodo.sync.model.ApplicationData;
import com.quantimodo.sync.sync.SyncHelper;

/*
 * BroadcastReceiver to monitor installed/uninstalled packages to refresh
 * the application list in case a compatible app was added or removed.
 */
public class PackagesChangedReceiver extends BroadcastReceiver {
    /*
     * Called whenever a package is added or removed
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Global.applications.size() != 0) // Only request a reload if there were apps loaded to begin with
        {
            ApplicationData.getCompatibleApplications(context, new Handler(), new ApplicationData.OnCompatibleApplicationsLoaded() {
                @Override
                public void onComplete() {
                    ApplicationListFragment.update();
                }
            });
        } else                                 // The app isn't in the foreground, so check if there was an uninstall and potentially remove if from the "syncing" list
        {
            if (Intent.ACTION_PACKAGE_REMOVED.equalsIgnoreCase(intent.getAction())) {
                String packageName = intent.getData().toString().replaceAll("package:", "");

                SharedPreferences prefs = context.getSharedPreferences("com.quantimodo.sync_preferences", Context.MODE_MULTI_PROCESS);
                String currentSyncingPackages = prefs.getString("syncingPackages", "");
                currentSyncingPackages = currentSyncingPackages.replace(packageName + ",", "");

                if (currentSyncingPackages.length() == 0) {
                    SyncHelper.unscheduleSync(context);
                } else {
                    SyncHelper.scheduleSync(context);
                }

                prefs.edit().putString("syncingPackages", currentSyncingPackages).commit();
            }
        }
    }
}
