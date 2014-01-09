package com.quantimodo.sync.fragments;

import android.accounts.Account;
import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import com.quantimodo.sdk.QuantimodoClient;
import com.quantimodo.sync.Global;
import com.quantimodo.sync.R;
import com.quantimodo.sync.model.ApplicationData;

public class ApplicationListFragment extends Fragment
{
	private static Adapter adapter;
	private static GridView gridView;

	private Activity activity;

	@Override public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		if (container == null)
		{
			return null;
		}

		ApplicationData.getCompatibleApplications(activity.getApplicationContext(), new Handler(), new ApplicationData.OnCompatibleApplicationsLoaded()
		{
			@Override
			public void onComplete()
			{
				ApplicationListFragment.update();
			}
		});

		View view = inflater.inflate(R.layout.fragment_applicationlist, container, false);

		QuantimodoClient qmClient = QuantimodoClient.getInstance();
		Account qmAccount = qmClient.getAccount(activity);

		initList(view);

		return view;
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		this.activity = activity;       // Workaround for failing getActivity()
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.applications, menu);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.action_sync:
			QuantimodoClient qmClient = QuantimodoClient.getInstance();
			ContentResolver.requestSync(qmClient.getAccount(this.getActivity()), "com.quantimodo.sync.content-appdata", new Bundle());
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void initList(View view)
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
	 * This will either toggle it's sync preference, or open the Play store to download the app
	 */
	private View.OnClickListener onStateIconClicked = new View.OnClickListener()
	{
		@Override public void onClick(View view)
		{
			int position = (Integer) view.getTag();

			ApplicationData currentApp = Global.applications.get(position);
			if (currentApp.isInstalled)
			{
				boolean nowSyncing = currentApp.setSyncEnabled(activity, !currentApp.isSyncEnabled());

				int firstVisible = gridView.getFirstVisiblePosition();
				int rowFromTop = position - firstVisible;
				View child = gridView.getChildAt(rowFromTop);                                   // Updating the view
				TextView syncState = (TextView) child.findViewById(R.id.tvSyncState);
				ImageButton stateIcon = (ImageButton) child.findViewById(R.id.imStateIcon);

				if (nowSyncing)
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
			}
		}
	};

	/*
	 * Called when the application icon is clicked
	 * Opens the main activity of the selected app
	 */
	private View.OnClickListener onAppIconClicked = new View.OnClickListener()
	{
		@Override public void onClick(View view)
		{
			int position = (Integer) view.getTag();

			ApplicationData currentApp = Global.applications.get(position);
			PackageManager packageManager = activity.getPackageManager();
			try
			{
				Intent launchIntent = packageManager.getLaunchIntentForPackage(currentApp.packageName);
				activity.startActivity(launchIntent);
			}
			catch (Exception e)
			{
				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + currentApp.packageName)));
			}
		}
	};

	static class ViewHolder
	{
		TextView separator;

		ImageButton appIcon;
		ImageButton stateIcon;
		TextView label;
		TextView syncState;
	}

	public class Adapter extends ArrayAdapter<ApplicationData>
	{
		private static final int TYPE_APPLICATION = 0;
		private static final int TYPE_SEPARATOR = 1;
		private static final int TYPE_MAX_COUNT = 2;

		private LayoutInflater inflater;

		public Adapter()
		{
			super(activity, R.layout.fragment_applicationlist_row, Global.applications);
			inflater = LayoutInflater.from(activity);
		}

		@Override
		public int getItemViewType(int position)
		{
			ApplicationData application = Global.applications.get(position);

			if (application.separator == null)
			{
				return TYPE_APPLICATION;
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
			return Global.applications.size();
		}

		@Override
		public ApplicationData getItem(int position)
		{
			return Global.applications.get(position);
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
			ApplicationData application = Global.applications.get(position);
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
				case TYPE_APPLICATION:
					convertView = inflater.inflate(R.layout.fragment_applicationlist_row, null);
					holder.label = (TextView) convertView.findViewById(R.id.tvLabel);
					holder.syncState = (TextView) convertView.findViewById(R.id.tvSyncState);
					holder.appIcon = (ImageButton) convertView.findViewById(R.id.imAppIcon);
					holder.appIcon.setOnClickListener(onAppIconClicked);
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

			if (application.separator != null)
			{
				holder.separator.setText(application.separator);
			}
			else
			{

				holder.stateIcon.setTag(position);      // This tag is used to identify the row when it was clicked
				holder.appIcon.setTag(position);      // Idem

				holder.label.setText(application.label);
				if (application.icon != null)
				{
					holder.appIcon.setImageDrawable(application.icon);
				}
				else
				{
					holder.appIcon.setImageResource(R.drawable.ic_appiconplaceholder);   // TODO dynamically load icon
				}

				if (application.isInstalled)
				{
					if (application.isSyncEnabled())
					{
						holder.stateIcon.setImageResource(R.drawable.ic_checked);
						holder.syncState.setText(R.string.app_syncEnabled);
					}
					else
					{
						holder.stateIcon.setImageResource(R.drawable.ic_unchecked);
						holder.syncState.setText(R.string.app_syncDisabled);
					}

					if (application.syncStatus != null)
					{
						holder.syncState.setText(application.syncStatus);
					}
				}
				else
				{
					holder.stateIcon.setImageResource(R.drawable.ic_playstore);
					holder.syncState.setText(R.string.app_notInstalled);
				}
			}

			return convertView;
		}
	}
}
