package com.quantimodo.sync;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

import com.quantimodo.sync.fragments.ApplicationListFragment;
import com.uservoice.uservoicesdk.Config;
import com.uservoice.uservoicesdk.UserVoice;

//TODO 10.1/7 inch layouts
//TODO Look into Linux user groups to see if we can use a file monitor to automatically get changes

public class MainActivity extends FragmentActivity {
    public static final int FRAGMENT_APPLICATIONS = 1;

    private static final int REQUEST_WELCOME = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Global.init(this);

        //Init uservoice
        Config config = new Config("quantimodo.uservoice.com");
        config.setForumId(211661);
        UserVoice.init(config, this);

        if (Global.qmAccountName == null) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivityForResult(intent, REQUEST_WELCOME);
        } else {
            setCurrentFragment(this, FRAGMENT_APPLICATIONS, -1);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent openPrefsIntent = null;

        if (id == R.id.action_history) {
            openPrefsIntent = new Intent(this, HistoryActivity.class);
            startActivity(openPrefsIntent);
            return true;
        } else if (id == R.id.action_settings) {
            openPrefsIntent = new Intent(this, SettingsActivity.class);
            startActivity(openPrefsIntent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_WELCOME) {
            if (resultCode == RESULT_OK) {
                setCurrentFragment(this, FRAGMENT_APPLICATIONS, -1);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Set the fragment to be displayed in the given activity.
     *
     * @param activity  The activity to show the fragment in.
     * @param fragment  The identifier of the fragment to show.
     * @param animation An optional animation to use for the transition, or -1 for no transition.
     */
    public static void setCurrentFragment(FragmentActivity activity, int fragment, int animation) {
        Fragment newFragment;

        Fragment currentFragment = activity.getSupportFragmentManager().findFragmentById(R.id.mainFragment);

        switch (fragment) {
            case FRAGMENT_APPLICATIONS:
                if (currentFragment != null && currentFragment instanceof ApplicationListFragment) {
                    return;
                }
                newFragment = new ApplicationListFragment();
                break;
            default:
                return;
        }

        // If we get here a new fragment has been created for us, so it's time to show it.
        FragmentTransaction fragmentTransaction = activity.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.mainFragment, newFragment);
        if (animation != -1) {
            fragmentTransaction.setTransition(animation);
        }
        fragmentTransaction.commit();
    }

/*
    public static void lockDrawer(boolean lock)
    {
        if(lock)
        {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
        else
        {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }
*/
}
