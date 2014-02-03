package com.quantimodo.sync.model;

import java.util.Date;

public class HistoryThing
{
	public String packageName;
	public String packageLabel;
	public Date timestamp;
	public int syncCount;
	public String syncError;
	
	public HistoryThing(String packageName, String packageLabel, Date timestamp, int syncCount, String syncError)
	{
		this.packageName = packageName;
		this.packageLabel = packageLabel;
		this.timestamp = timestamp;
		this.syncCount = syncCount;
		this.syncError = syncError;
	}
}