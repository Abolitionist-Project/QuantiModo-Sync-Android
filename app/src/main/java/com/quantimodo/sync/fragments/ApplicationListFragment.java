package com.quantimodo.sync.fragments;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.quantimodo.android.sdk.Quantimodo;
import com.quantimodo.sync.Global;
import com.quantimodo.sync.R;
import com.quantimodo.sync.model.ApplicationData;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class ApplicationListFragment extends Fragment {
    private static Adapter adapter;
    private static StickyListHeadersListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        if (container == null) {
            return null;
        }

        ApplicationData.getCompatibleApplications(getActivity().getApplicationContext(), new Handler(), new ApplicationData.OnCompatibleApplicationsLoaded() {
            @Override
            public void onComplete() {
                ApplicationListFragment.update();
            }
        });

        View view = inflater.inflate(R.layout.fragment_applicationlist, container, false);

        initList(view);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.applications, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_sync) {
            ContentResolver.requestSync(Quantimodo.getAccount(this.getActivity().getApplicationContext()), "com.quantimodo.sync.content-appdata", new Bundle());
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void initList(View view) {
        listView = (StickyListHeadersListView) view.findViewById(android.R.id.list);
        listView.setAreHeadersSticky(false);

        if (adapter == null) {
            adapter = new Adapter();
        }
        listView.setAdapter(adapter);
    }

    public static void update() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /*
     * Called when the state icon (the image at the far right of a row) is clicked
     * This will either toggle it's sync preference, or open the Play store to download the app
     */
    private View.OnClickListener onStateIconClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int position = (Integer) view.getTag();

            ApplicationData currentApp = Global.applications.get(position);
            if (currentApp.isInstalled) {
                currentApp.setSyncEnabled(getActivity(), !currentApp.isSyncEnabled());
                adapter.notifyDataSetChanged();
            } else {
                // Start a generic ACTION_VIEW intent, most likely captured by the Play store, there's no way of knowing whether the user installed the app here or not.
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + currentApp.packageName)));
            }
        }
    };

    /*
     * Called when the application icon is clicked
     * Opens the main activity of the selected app
     */
    private View.OnClickListener onAppIconClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int position = (Integer) view.getTag();

            ApplicationData currentApp = Global.applications.get(position);
            PackageManager packageManager = getActivity().getPackageManager();
            try {
                Intent launchIntent = packageManager.getLaunchIntentForPackage(currentApp.packageName);
                startActivity(launchIntent);
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + currentApp.packageName)));
            }
        }
    };

    public class Adapter extends ArrayAdapter<ApplicationData> implements StickyListHeadersAdapter {
        private LayoutInflater inflater;

        private class HeaderViewHolder {
            TextView separator;
        }

        private class ViewHolder {
            ImageButton appIcon;
            ImageButton stateIcon;
            TextView label;
            TextView syncState;
        }

        public Adapter() {
            super(getActivity(), R.layout.fragment_applicationlist_row, Global.applications);
            inflater = LayoutInflater.from(getActivity());
        }

        @Override
        public int getCount() {
            return Global.applications.size();
        }

        @Override
        public ApplicationData getItem(int position) {
            return Global.applications.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public long getHeaderId(int i) {
            return Global.applications.get(i).isInstalled ? 0 : 1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            ApplicationData application = Global.applications.get(position);
            if (convertView == null) {
                holder = new ViewHolder();

                convertView = inflater.inflate(R.layout.fragment_applicationlist_row, null);
                holder.label = (TextView) convertView.findViewById(R.id.tvLabel);
                holder.syncState = (TextView) convertView.findViewById(R.id.tvSyncState);
                holder.appIcon = (ImageButton) convertView.findViewById(R.id.imAppIcon);
                holder.appIcon.setOnClickListener(onAppIconClicked);
                holder.stateIcon = (ImageButton) convertView.findViewById(R.id.imStateIcon);
                holder.stateIcon.setOnClickListener(onStateIconClicked);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.stateIcon.setTag(position);    // This tag is used to identify the row when it was clicked
            holder.appIcon.setTag(position);      // Idem

            holder.label.setText(application.label);
            if (application.icon != null) {
                holder.appIcon.setImageDrawable(application.icon);
            } else {
                holder.appIcon.setImageResource(R.drawable.ic_appiconplaceholder);   // TODO dynamically load icon
            }

            if (application.isInstalled) {
                if (application.isSyncEnabled()) {
                    holder.stateIcon.setImageResource(R.drawable.ic_checked);
                    holder.syncState.setText(R.string.app_syncEnabled);
                } else {
                    holder.stateIcon.setImageResource(R.drawable.ic_unchecked);
                    holder.syncState.setText(R.string.app_syncDisabled);
                }

                if (application.syncStatus != null) {
                    holder.syncState.setText(application.syncStatus);
                }
            } else {
                holder.stateIcon.setImageResource(R.drawable.ic_playstore);
                holder.syncState.setText(R.string.app_notInstalled);
            }

            return convertView;
        }

        @Override
        public View getHeaderView(int position, View convertView, ViewGroup viewGroup) {
            HeaderViewHolder holder;
            ApplicationData application = Global.applications.get(position);
            if (convertView == null) {
                holder = new HeaderViewHolder();

                convertView = inflater.inflate(R.layout.view_separator, null);
                holder.separator = (TextView) convertView.findViewById(R.id.separator);

                convertView.setTag(holder);
            } else {
                holder = (HeaderViewHolder) convertView.getTag();
            }

            if (application.isInstalled) {
                holder.separator.setText("Installed");
            } else {
                holder.separator.setText("Compatible");
            }


            return convertView;
        }
    }
}
