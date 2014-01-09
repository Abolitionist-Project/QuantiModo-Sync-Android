package com.quantimodo.sync.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.quantimodo.sync.Global;
import com.quantimodo.sync.R;
import com.quantimodo.sync.model.SensorData;

public class SensorListFragment extends Fragment
{
	private static Adapter adapter;
	private static GridView gridView;

	private Activity activity;

	@Override public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		if (container == null)
		{
			return null;
		}

		View view = inflater.inflate(R.layout.fragment_applicationlist, container, false);

		initList(view);

		return view;
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		this.activity = activity;       // Workaround for failing getActivity()
	}

	public void initList(View view)
	{
		gridView = (GridView) view.findViewById(android.R.id.list);
		if (adapter == null)
		{
			adapter = new Adapter();
		}
		gridView.setAdapter(adapter);
	}

	public static void update()
	{
		if (adapter != null)
		{
			adapter.notifyDataSetChanged();
		}
	}

	/*
	 * Called when the state icon (the image at the far right of a row) is clicked
	 * This will toggle it's sync state
	 */
	View.OnClickListener onStateIconClicked = new View.OnClickListener()
	{
		@Override public void onClick(View view)
		{
			/*int position = (Integer) view.getTag();

			ApplicationData currentApp = Global.applications.get(position);
			if(currentApp.isInstalled)
			{
				boolean nowSyncing = currentApp.setSyncEnabled(activity, !currentApp.isSyncEnabled());

				int firstVisible = gridView.getFirstVisiblePosition();
				int rowFromTop = position - firstVisible;
				View child = gridView.getChildAt(rowFromTop);                                   // Updating the view
				TextView syncState = (TextView) child.findViewById(R.id.tvSyncState);
				ImageButton stateIcon = (ImageButton) child.findViewById(R.id.imStateIcon);

				if(nowSyncing)
				{
					stateIcon.setImageResource(R.drawable.ic_checked);
					syncState.setText("Sync active");
				}
				else
				{
					stateIcon.setImageResource(R.drawable.ic_unchecked);
					syncState.setText("Sync disabled");
				}
			}
			else
			{
				// Start a generic ACTION_VIEW intent, most likely captured by the Play store, there's no way of knowing whether the user installed the app here or not.
				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + currentApp.packageName)));
			}*/
		}
	};

	static class ViewHolder
	{
		TextView separator;

		ImageView sensorIcon;
		ImageButton stateIcon;
		TextView label;
		TextView syncState;
	}

	public class Adapter extends ArrayAdapter<SensorData>
	{
		private static final int TYPE_SENSOR = 0;
		private static final int TYPE_SEPARATOR = 1;
		private static final int TYPE_MAX_COUNT = 2;

		private LayoutInflater inflater;

		public Adapter()
		{
			super(activity, R.layout.fragment_sensorlist_row, Global.sensors);
			inflater = LayoutInflater.from(activity);
		}

		@Override
		public int getItemViewType(int position)
		{
			SensorData sensor = Global.sensors.get(position);

			if (sensor.separator == null)
			{
				return TYPE_SENSOR;
			}
			else
			{
				return TYPE_SEPARATOR;
			}
		}

		@Override
		public int getViewTypeCount()
		{
			return TYPE_MAX_COUNT;
		}

		@Override
		public int getCount()
		{
			return Global.sensors.size();
		}

		@Override
		public SensorData getItem(int position)
		{
			return Global.sensors.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			ViewHolder holder;
			SensorData sensor = Global.sensors.get(position);
			if (convertView == null)
			{
				holder = new ViewHolder();

				switch (getItemViewType(position))
				{
				case TYPE_SEPARATOR:
					convertView = inflater.inflate(R.layout.view_separator, null);
					holder.separator = (TextView) convertView.findViewById(R.id.separator);
					convertView.setTag(holder);
					break;
				case TYPE_SENSOR:
					convertView = inflater.inflate(R.layout.fragment_sensorlist_row, null);
					holder.label = (TextView) convertView.findViewById(R.id.tvLabel);
					holder.syncState = (TextView) convertView.findViewById(R.id.tvSyncState);
					holder.sensorIcon = (ImageView) convertView.findViewById(R.id.imSensorIcon);
					holder.stateIcon = (ImageButton) convertView.findViewById(R.id.imStateIcon);
					holder.stateIcon.setOnClickListener(onStateIconClicked);
					convertView.setTag(holder);
					break;
				}
			}
			else
			{
				holder = (ViewHolder) convertView.getTag();
			}

			if (sensor.separator != null)
			{
				holder.separator.setText(sensor.separator);
			}
			else
			{

				holder.stateIcon.setTag(position);      // This tag is used to identify the row when it was clicked

				holder.label.setText(sensor.label);
				if (sensor.icon != null)
				{
					holder.sensorIcon.setImageDrawable(sensor.icon);
				}
				else
				{
					holder.sensorIcon.setImageResource(R.drawable.ic_appiconplaceholder);   // TODO set proper icon for each sensor
				}

				if (sensor.isSyncEnabled())
				{
					holder.stateIcon.setImageResource(R.drawable.ic_launcher);
					holder.syncState.setText("Sync active");
				}
				else
				{
					holder.stateIcon.setImageResource(R.drawable.ic_launcher);
					holder.syncState.setText("Sync disabled");
				}
			}
			return convertView;
		}
	}
}
