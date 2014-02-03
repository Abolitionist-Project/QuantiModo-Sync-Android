package com.quantimodo.sync;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
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
import com.quantimodo.sync.model.HistoryThing;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HistoryActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor>
{
	private static final int URL_HISTORYLOADER = 0;
	public List<HistoryThing> historyItems = null;

	private HistoryAdapter adapter;

	@Override 
	protected void onCreate(Bundle savedInstanceState)
	{
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_historylist);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		GridView listView = (GridView) findViewById(R.id.historylist);

		adapter = new HistoryAdapter();
		listView.setAdapter(adapter);

		getLoaderManager().initLoader(URL_HISTORYLOADER, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle)
	{
		switch(loaderId)
		{
		case URL_HISTORYLOADER:
			return new CursorLoader(HistoryActivity.this.getApplicationContext(), QuantiSyncContentProvider.CONTENT_URI_HISTORY, null, null, null, null);
		default:
			return null;
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor)
	{
		if(cursor == null)
		{
			Log.i("History cursor is null");
			return;
		}

		switch(cursorLoader.getId())
		{
		case URL_HISTORYLOADER:
			if(historyItems == null)
			{
				historyItems = new ArrayList<HistoryThing>(cursor.getCount());
			}

			int packageNameColumn = cursor.getColumnIndex(QuantiSyncDbHelper.History.PACKAGENAME);
			int packageLabelColumn = cursor.getColumnIndex(QuantiSyncDbHelper.History.PACKAGELABEL);
			int timestampColumn = cursor.getColumnIndex(QuantiSyncDbHelper.History.TIMESTAMP);
			int syncCountColumn = cursor.getColumnIndex(QuantiSyncDbHelper.History.SYNCCOUNT);
			int syncErrorColumn = cursor.getColumnIndex(QuantiSyncDbHelper.History.SYNCERROR);
			for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
			{
				historyItems.add(
						new HistoryThing(cursor.getString(packageNameColumn),
										 cursor.getString(packageLabelColumn),
										 new Date(cursor.getLong(timestampColumn)),
										 cursor.getInt(syncCountColumn),
										 cursor.getString(syncErrorColumn)));
			}
			break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader)
	{

	}

	static class ViewHolder
	{
		ImageButton appIcon;
		TextView label;
		TextView syncCount;
		TextView syncState;
	}
	
	public class HistoryAdapter extends BaseAdapter {
		
		private LayoutInflater inflater;
		
		public HistoryAdapter() {
			inflater = (LayoutInflater) HistoryActivity.this.getSystemService(HistoryActivity.LAYOUT_INFLATER_SERVICE);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			ViewHolder holder;
			if(convertView == null) {
				
				holder = new ViewHolder();
				
				convertView = inflater.inflate(R.layout.activity_historylist_row, null);
				holder.appIcon = (ImageButton) convertView.findViewById(R.id.imgAppIcon);
				holder.label = (TextView) convertView.findViewById(R.id.tvLabel);
				holder.syncCount = (TextView) convertView.findViewById(R.id.tvSyncCnt);
				holder.syncState = (TextView) convertView.findViewById(R.id.tvSyncState);
				
				convertView.setTag(holder);
			}
			else
			{
				holder = (ViewHolder) convertView.getTag();
			}
			
			HistoryThing entry = historyItems.get(position);
			
			ApplicationData application = null;
			for( ApplicationData temp : Global.applications)
			{
				if(temp.label != null && temp.label.equals(entry.packageLabel))
				{
					application = temp;
					break;
				}
			}

			if(application == null)
			{
				holder.label.setText(entry.packageLabel);
				holder.appIcon.setImageResource(R.drawable.ic_appiconplaceholder);
			}
			else
			{
				holder.label.setText(application.label);
				if(application.icon == null)
				{
					holder.appIcon.setImageResource(R.drawable.ic_appiconplaceholder);
				}
				else
				{
					holder.appIcon.setImageDrawable(application.icon);
				}
			}

			String syncCountString = String.format("%d measurements were synced", entry.syncCount);
			holder.syncCount.setText(syncCountString);
			if(entry.syncError == null)
			{
				holder.syncState.setTextColor(Color.parseColor("#99CC00"));
				holder.syncState.setText("Synced");
			}
			else
			{
				holder.syncState.setTextColor(Color.RED);
				holder.syncState.setText("Failed");
			}

			return convertView;
		}

		@Override
		public int getCount() {
			
			if(historyItems == null)
			{
				return 0;
			}
			
			return historyItems.size();
		}

		@Override
		public Object getItem(int position)
		{
			if(historyItems == null)
			{
				return 0;
			}

			return historyItems.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}		
	}
}