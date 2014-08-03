package com.quantimodo.sync.model;

import java.util.ArrayList;
import java.util.Date;

public class HistoryGroup
{
	private final ArrayList<HistoryItem> historyItems;

	public final Date timestamp;
	public int totalMeasurements;
	public int totalApps;

	public HistoryGroup(Date timestamp, ArrayList<HistoryItem> historyItems)
	{
		this.timestamp = timestamp;
		this.historyItems = historyItems;

		totalApps = historyItems.size();
		for(HistoryItem item : historyItems)
		{
			totalMeasurements += item.syncCount;
		}
	}

	public HistoryGroup(Date timestamp, HistoryItem historyItem)
	{
		this.timestamp = timestamp;
		this.historyItems = new ArrayList<HistoryItem>();
		this.historyItems.add(historyItem);

		totalApps = 1;
		totalMeasurements = historyItem.syncCount;
	}

	public HistoryGroup(Date timestamp)
	{
		this.timestamp = timestamp;
		this.historyItems = new ArrayList<HistoryItem>();
	}

	public void addItem(HistoryItem historyItem)
	{
		totalApps++;
		totalMeasurements += historyItem.syncCount;
	}

	public HistoryItem getItem(int position)
	{
		return historyItems.get(position);
	}
}