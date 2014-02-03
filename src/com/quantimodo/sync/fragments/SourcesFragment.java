package com.quantimodo.sync.fragments;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.*;
import com.quantimodo.sync.R;
import com.quantimodo.sync.sync.AppDataSyncService;

public class SourcesFragment extends Fragment
{
	public static final String[] PAGER_TITLES = {"APPLICATIONS"};

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

		View view = inflater.inflate(R.layout.fragment_sources, container, false);

		initActionBar();
		//initViewPager(view);

		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.sources, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();
		
		if(id == R.id.action_sync) {
			Intent newIntent = new Intent(getActivity(), AppDataSyncService.class);
			getActivity().startService(newIntent);
			return true;
		}
		else {
			return super.onOptionsItemSelected(item);
		}
	}

	public void initActionBar()
	{
		ActionBar actionBar = getActivity().getActionBar();
		actionBar.setTitle(R.string.title_sources);
	}

	/*
	 *  Init ViewPager related items, such as the PagerAdapter and tabs.

	private void initViewPager(final View view)
	{
		ViewPager viewPager = (ViewPager) view.findViewById(R.id.viewPager);
		viewPager.setAdapter(new PagerAdapter());

		PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) view.findViewById(R.id.viewPagerTabs);
		tabs.setUnderlineColorResource(R.color.tabs_underline);
		tabs.setIndicatorColorResource(R.color.tabs_slider);
		tabs.setTextColorResource(R.color.tabs_text);
		tabs.setViewPager(viewPager);
	}*/

	public class PagerAdapter extends FragmentPagerAdapter
	{
		public PagerAdapter()
		{
			super(getChildFragmentManager());
		}

		public CharSequence getPageTitle(int position)
		{
			return PAGER_TITLES[position];
		}

		@Override
		public int getCount()
		{
			return PAGER_TITLES.length;
		}

		@Override
		public Fragment getItem(int position)
		{
			switch (position)
			{
			case 0:
				return new ApplicationListFragment();
			case 1:
				return new SensorListFragment();
			}
			return null;
		}
	}
}
