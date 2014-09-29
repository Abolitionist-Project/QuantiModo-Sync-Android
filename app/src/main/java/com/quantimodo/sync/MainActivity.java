package com.quantimodo.sync;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.quantimodo.sync.fragments.ApplicationListFragment;

//TODO 10.1/7 inch layouts
//TODO Look into Linux user groups to see if we can use a file monitor to automatically get changes

public class MainActivity extends Activity {
    public static final int FRAGMENT_APPLICATIONS = 1;

    private static final int REQUEST_WELCOME = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Global.init(this);

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

	/*
    private void initDrawer() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.app_name, R.string.app_name) {
            public void onDrawerClosed(View view) {
            }

            public void onDrawerOpened(View drawerView) {
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        TextView tvApplications = (TextView) findViewById(R.id.tvApplications);
        tvApplications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCurrentFragment(MainActivity.this, FRAGMENT_APPLICATIONS, FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                drawerLayout.closeDrawer(Gravity.LEFT);
            }
        });
    }
*/

    public static void setCurrentFragment(Activity activity, int fragment, int animation) {
        Fragment newFragment;

        switch (fragment) {
            case FRAGMENT_APPLICATIONS:
                newFragment = new ApplicationListFragment();
                break;
            default:
                return;
        }

        FragmentTransaction fragmentTransaction = activity.getFragmentManager().beginTransaction();
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
