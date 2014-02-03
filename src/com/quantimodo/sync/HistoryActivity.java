package com.quantimodo.sync;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.quantimodo.etl.ETL;
import com.quantimodo.etl.HistoryThing;
import com.quantimodo.sync.model.ApplicationData;

public class HistoryActivity extends Activity {
	
	public List<HistoryThing> variables = null;
	
	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_historylist);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		GridView listView = (GridView) findViewById(R.id.historylist);
		listView.setAdapter(new HistoryAdapter());
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			new LoadHistoryAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		else
			new LoadHistoryAsyncTask().execute();
	}
	
	public boolean onCreateOptionMenu(Menu menu) {
		
		getMenuInflater().inflate(R.menu.history, menu);
		return true;
	}
	
	public boolean onOptionItemSelected(MenuItem item) {
		
		return super.onOptionsItemSelected(item);
	}
	
	public class LoadHistoryAsyncTask extends AsyncTask<Object, String, String> {
		
		public LoadHistoryAsyncTask() {
			
		}
		
		@Override
		protected String doInBackground(Object... arg0) {
			
			ETL etl = new ETL(); 
			File cacheFile = null;
			
			String cachePath = getCacheDir().getPath();
			
			try
			{				
				String filepath = cachePath + "/" + Global.historyPackage;
				cacheFile = new File(filepath);
				
				if (!cacheFile.exists()) {		
					Log.i("No history sqlite file!!!");
					return null;
					
				}
				
				variables = Arrays.asList(etl.loadHistory(cacheFile));
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			
			return "Done";
		}
		
		@Override
		protected void onProgressUpdate(String... values) {
			
			super.onProgressUpdate(values);
		}
		
		@Override
		protected void onPostExecute(String result) {
			
			super.onPostExecute(result);
		}
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
			else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			HistoryThing entry = variables.get(position);
			
			ApplicationData application = null;
			for( ApplicationData temp : Global.applications) {
				
				if(temp.label != null && temp.label.equals(entry.label)) {
					application = temp;
					break;
				}
			}
			
			holder.label.setText(entry.label);
			
			if(application.icon != null)
				holder.appIcon.setImageDrawable(application.icon);
			else
				holder.appIcon.setImageResource(R.drawable.ic_appiconplaceholder);
			
			String syncCountString = String.format("%d measurements were synced", entry.syncCount);
			holder.syncCount.setText(syncCountString);
			if(entry.syncResult == 1) {
				holder.syncState.setTextColor(Color.parseColor("#99cc00"));
				holder.syncState.setText("successed");
			}
			else {
				holder.syncState.setTextColor(Color.RED);
				holder.syncState.setText("failed");
			}

			return convertView;
		}

		@Override
		public int getCount() {
			
			if(variables == null)
				return 0;
			
			return variables.size();
		}

		@Override
		public Object getItem(int position) {
			
			if(variables == null)
				return 0;
			
			return variables.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}		
	}
}