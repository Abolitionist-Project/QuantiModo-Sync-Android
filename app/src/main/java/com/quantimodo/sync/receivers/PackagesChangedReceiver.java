package com.quantimodo.sync.receivers;

import android.accounts.Account;
import android.content.*;
import android.os.Handler;
import com.quantimodo.android.sdk.Quantimodo;
import com.quantimodo.sync.Global;
import com.quantimodo.sync.fragments.ApplicationListFragment;
import com.quantimodo.sync.model.ApplicationData;

/*
 * BroadcastReceiver to monitor installed/uninstalled packages to refresh
 * the application list in case a compatible app was added or removed.
 */
public class PackagesChangedReceiver extends BroadcastReceiver
{
	/*
	 * Called whenever a package is added or removed
	 */
	@Override public void onReceive(Context context, Intent intent)
	{
		if (Global.applications.size() != 0) // Only request a reload if there were apps loaded to begin with
		{
			ApplicationData.getCompatibleApplications(context, new Handler(), new ApplicationData.OnCompatibleApplicationsLoaded()
			{
				@Override public void onComplete()
				{
					ApplicationListFragment.update();
				}
			});
		}
		else                                 // The app isn't in the foreground, so check if there was an uninstall and potentially remove if from the "syncing" list
		{
			if (Intent.ACTION_PACKAGE_REMOVED.equalsIgnoreCase(intent.getAction()))
			{
				String packageName = intent.getData().toString().replaceAll("package:", "");

				SharedPreferences prefs = context.getSharedPreferences("com.quantimodo.sync_preferences", Context.MODE_MULTI_PROCESS);
				String currentSyncingPackages = prefs.getString("syncingPackages", "");
				currentSyncingPackages = currentSyncingPackages.replace(packageName + ",", "");

				Account account = Quantimodo.getAccount(context);
				if (account != null)
				{
					if (currentSyncingPackages.length() == 0 && account != null)
					{
						ContentResolver.setSyncAutomatically(account, "com.quantimodo.sync.content-appdata", false);
					}
					else
					{
						ContentResolver.setSyncAutomatically(account, "com.quantimodo.sync.content-appdata", true);
					}
				}
				prefs.edit().putString("syncingPackages", currentSyncingPackages).commit();
			}
		}
	}
}
