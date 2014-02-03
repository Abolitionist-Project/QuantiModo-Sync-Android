package com.quantimodo.etl;

public class HistoryThing {
	
	public String label;
	public int syncCount;
	public int syncResult;
	
	public HistoryThing(String label, int syncCount, int syncResult) {
		
		this.label = label;
		this.syncCount = syncCount;
		this.syncResult = syncResult;
	}
}