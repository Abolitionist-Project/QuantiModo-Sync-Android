package com.quantimodo.etl;

import com.quantimodo.app.Log;
import com.quantimodo.sdk.model.QuantimodoMeasurement;

import java.util.ArrayList;

public class AppUsageConverter implements Converter
{
	public static final AppUsageConverter instance = new AppUsageConverter();

	private static final QuantimodoMeasurement[] EMPTY_RESULT = new QuantimodoMeasurement[0];

	private AppUsageConverter()
	{
	}

	public QuantimodoMeasurement[] convert(final DatabaseView databaseView)
	{
		if ((databaseView == null) || (!databaseView.hasTable("appusage_apps")))
		{
			return null;
		}

		Table appsTable = databaseView.getTable("appusage_apps");
		if (!appsTable.hasField("PACKAGENAME"))
		{
			return EMPTY_RESULT;
		}

		int appCount = appsTable.getRecordCount();
		if (appCount == 0)
		{
			return EMPTY_RESULT;
		}

		int trackedAppsCount = appsTable.getRecordCount();
		ArrayList<QuantimodoMeasurement> records = new ArrayList<QuantimodoMeasurement>();

		for (int i = 0; i < trackedAppsCount; i++)
		{
			Log.i("Found tracked app: " + appsTable.getData(i, "PACKAGENAME"));

			String packageName = (String) appsTable.getData(i, "PACKAGENAME");

			Table currentTable = databaseView.getTable(packageName);
			int currentTableRows = currentTable.getRecordCount();
			for (int n = 0; n < currentTableRows; n++)
			{
				String variableCategory = "test";               //TODO proper var
				String variableName = packageName + " usage";
				boolean userDefinedVariable = false;
				boolean inputVariable = true;                   //TODO proper var
				boolean summableVariable = true;
				double value = Double.valueOf((String) currentTable.getData(n, "RUNTIME"));
				;
				String unitName = "milliseconds";
				long startTime = Long.valueOf((String) currentTable.getData(n, "LAUNCHTIME"));
				int duration = (int) value;

				//QuantimodoMeasurement currentRecord = new QuantimodoMeasurement("com.appeine.apusage", variableCategory, variableName, userDefinedVariable, inputVariable, summableVariable, value, unitName, startTime, duration);
			}
		}

		return records.toArray(new QuantimodoMeasurement[0]);
	}
}
