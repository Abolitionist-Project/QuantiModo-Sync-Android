package com.quantimodo.sync;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.quantimodo.sync.databases.QuantiSyncContentProvider;
import com.quantimodo.sync.databases.QuantiSyncDbHelper;
import com.quantimodo.sync.model.ApplicationData;
import com.quantimodo.sync.model.HistoryGroup;
import com.quantimodo.sync.model.HistoryItem;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class HistoryActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int URL_HISTORYLOADER = 0;
    private static final int NUM_HISTORY_PRELOAD = 5;   // Load entries from five syncs at once

    private LoaderManager loaderManager;

    public List<HistoryItem> historyItems = null;
    public List<HistoryGroup> historyGroups = null;

    private HistoryItemListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_historylist);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        GridView listView = (GridView) findViewById(R.id.historylist);

        if (adapter == null) {
            adapter = new HistoryItemListAdapter();
        }

        listView.setAdapter(adapter);

        loaderManager = getLoaderManager();
        loaderManager.initLoader(URL_HISTORYLOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
        switch (loaderId) {
            case URL_HISTORYLOADER:
                return new CursorLoader(HistoryActivity.this.getApplicationContext(), QuantiSyncContentProvider.CONTENT_URI_HISTORY, null, null, null, null);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor == null) {
            Log.i("Cursor is null");
            return;
        }

        switch (cursorLoader.getId()) {
            case URL_HISTORYLOADER:
                if (historyItems == null) {
                    historyItems = new ArrayList<HistoryItem>(cursor.getCount());
                    cursor.moveToLast();
                }

                int newSyncsAdded = 0;  // Count number of historyGroups we loaded

                HashMap<Long, HistoryGroup> newHistoryGroups = new HashMap<Long, HistoryGroup>();

                int packageNameColumn = cursor.getColumnIndex(QuantiSyncDbHelper.History.PACKAGENAME);
                int packageLabelColumn = cursor.getColumnIndex(QuantiSyncDbHelper.History.PACKAGELABEL);
                int timestampColumn = cursor.getColumnIndex(QuantiSyncDbHelper.History.TIMESTAMP);
                int syncCountColumn = cursor.getColumnIndex(QuantiSyncDbHelper.History.SYNCCOUNT);
                int syncErrorColumn = cursor.getColumnIndex(QuantiSyncDbHelper.History.SYNCERROR);

                for (; !cursor.isBeforeFirst(); cursor.moveToPrevious()) {
                    long timestamp = cursor.getLong(timestampColumn);
                    Date timestampDate = new Date(timestamp);
                    HistoryItem newHistoryItem = new HistoryItem(cursor.getString(packageNameColumn),
                            cursor.getString(packageLabelColumn),
                            timestampDate,
                            cursor.getInt(syncCountColumn),
                            cursor.getString(syncErrorColumn));

                    historyItems.add(newHistoryItem);

                    if (newHistoryGroups.containsKey(timestamp)) {
                        newSyncsAdded++;
                        if (newSyncsAdded == NUM_HISTORY_PRELOAD)    // If we loaded enough groups we break out. We have to move the cursor to the next element manually
                        {
                            cursor.moveToPrevious();
                            break;
                        }

                        newHistoryGroups.get(timestamp).addItem(newHistoryItem);
                    } else {
                        newHistoryGroups.put(timestamp, new HistoryGroup(timestampDate, newHistoryItem));
                    }
                }

                break;
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        historyItems = null;
    }

    static class ViewHolder {
        ImageButton imAppIcon;
        View vwIndicator;
        TextView tvAppLabel;
        TextView tvSyncDate;
        TextView tvSyncDescription;
    }

    public class HistoryItemListAdapter extends BaseAdapter {
        private final DateFormat dateFormat;
        private final DateFormat timeFormat;
        private final LayoutInflater inflater;

        private int lastUpdatePosition = 0; // The position at which we last requested an update

        public HistoryItemListAdapter() {
            inflater = (LayoutInflater) HistoryActivity.this.getSystemService(HistoryActivity.LAYOUT_INFLATER_SERVICE);
            dateFormat = android.text.format.DateFormat.getLongDateFormat(getApplicationContext());
            timeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {


            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();

                convertView = inflater.inflate(R.layout.activity_historylist_row, null);
                holder.imAppIcon = (ImageButton) convertView.findViewById(R.id.imAppIcon);
                holder.tvAppLabel = (TextView) convertView.findViewById(R.id.tvAppLabel);
                holder.tvSyncDate = (TextView) convertView.findViewById(R.id.tvSyncDate);
                holder.tvSyncDescription = (TextView) convertView.findViewById(R.id.tvSyncDescription);
                holder.vwIndicator = convertView.findViewById(R.id.vwIndicator);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            HistoryItem entry = historyItems.get(position);

            ApplicationData application = null;
            for (ApplicationData temp : Global.applications) {
                if (temp.label != null && temp.label.equals(entry.packageLabel)) {
                    application = temp;
                    break;
                }
            }

            if (application == null) {
                holder.tvAppLabel.setText(entry.packageLabel);
                holder.imAppIcon.setImageResource(R.drawable.ic_appiconplaceholder);
            } else {
                holder.tvAppLabel.setText(application.label);
                if (application.icon == null) {
                    holder.imAppIcon.setImageResource(R.drawable.ic_appiconplaceholder);
                } else {
                    holder.imAppIcon.setImageDrawable(application.icon);
                }
            }

            if (entry.syncError == null) {
                holder.tvSyncDescription.setText(String.format("%d measurements were synced", entry.syncCount));
                holder.vwIndicator.setBackgroundResource(R.color.indicator_success);
            } else {
                holder.tvSyncDescription.setText(entry.syncError);
                holder.vwIndicator.setBackgroundResource(R.color.indicator_faillure);
            }

            holder.tvSyncDate.setText(dateFormat.format(entry.timestamp) + ", " + timeFormat.format(entry.timestamp));

            if (position != lastUpdatePosition && position == getCount() - 1) {
                loaderManager.initLoader(URL_HISTORYLOADER, null, HistoryActivity.this);
                lastUpdatePosition = position;
            }

            return convertView;
        }

        @Override
        public int getCount() {

            if (historyItems == null) {
                return 0;
            }

            return historyItems.size();
        }

        @Override
        public Object getItem(int position) {
            if (historyItems == null) {
                return 0;
            }

            return historyItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }
}