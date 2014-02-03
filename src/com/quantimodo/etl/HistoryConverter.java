package com.quantimodo.etl;

import java.util.ArrayList;
import java.util.List;

public class HistoryConverter {
	
	public static final HistoryConverter instance = new HistoryConverter();
	
	private static final HistoryThing[] EMPTY_RESULT = new HistoryThing[0];
	
	private static final String[] REQUIRED_FIELD_TYPES = new String[]{"VARCHAR", "INT", "INT(1)"};
	private static final String[] REQUIRED_FIELD_NAMES = new String[]{"package", "synccount", "syncstate"};
	
	private static final Object[][] EMPTY_DATA_MATRIX = new Object[0][];
	
	public DatabaseView convert(HistoryThing[] historyArray) {
	
		final String[] tableNames = { "history" };
		
		final ArrayTable[] tables = new ArrayTable[1];		
		final int columnCount = REQUIRED_FIELD_NAMES.length;
		
		final Object[][] tableData;
		{
			final List<Object[]> tableDataList = new ArrayList<Object[]>();
			
			for(int rowNumber = 0; rowNumber < historyArray.length; rowNumber++) {
				
				final Object[] rowData = new Object[columnCount];
				rowData[0] = historyArray[rowNumber].label;
				rowData[1] = historyArray[rowNumber].syncCount;
				rowData[2] = historyArray[rowNumber].syncResult;
				
				tableDataList.add(rowData);
			}
			
			tableData = tableDataList.toArray(EMPTY_DATA_MATRIX);
		}
		
		tables[0] = new ArrayTable(tableNames[0], REQUIRED_FIELD_TYPES, REQUIRED_FIELD_NAMES, tableData);
		
		return new ArrayDatabaseView(tableNames, tables);
	}
	
	public HistoryThing[] convert(DatabaseView databaseView) {
		
		if ((databaseView == null) || (!databaseView.hasTable("history")))
			return null;
		
		final Table table = databaseView.getTable("history");
		for (int requiredFieldNumber = 0; requiredFieldNumber < REQUIRED_FIELD_NAMES.length; requiredFieldNumber++)
		{
			if (!table.hasField(REQUIRED_FIELD_NAMES[requiredFieldNumber]))
				return null;
		}
		
		final int recordCount = table.getRecordCount();
		if (recordCount == 0)
			return EMPTY_RESULT;
		
		final List<HistoryThing> results = new ArrayList<HistoryThing>(recordCount);
		
		for (int recordNumber = 0; recordNumber < recordCount; recordNumber++) {
			
			final String label = ((String) table.getData(recordNumber, "package")).toString();
			final int syncCount = ((Number) table.getData(recordNumber, "synccount")).intValue();
			final int syncState = ((Number) table.getData(recordNumber, "syncstate")).intValue();
			
			results.add(new HistoryThing(label, syncCount, syncState));
		}
		
		return results.toArray(EMPTY_RESULT);
	}
}