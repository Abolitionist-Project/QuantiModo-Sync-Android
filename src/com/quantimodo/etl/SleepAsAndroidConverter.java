package com.quantimodo.etl;

import com.quantimodo.sdk.model.QuantimodoMeasurement;

public class SleepAsAndroidConverter implements Converter
{
	public static final SleepAsAndroidConverter instance = new SleepAsAndroidConverter();

	private static final String[] REQUIRED_FIELD_NAMES = new String[]{"startTime", "toTime", "quality"};

	private SleepAsAndroidConverter()
	{
	}

	public QuantimodoMeasurement[] convert(final DatabaseView databaseView)
	{
		if (databaseView == null)
		{
			return null;
		}
		if (!databaseView.hasTable("records"))
		{
			return null;
		}

		final Table table = databaseView.getTable("records");

		for (int requiredFieldNumber = 1; requiredFieldNumber < REQUIRED_FIELD_NAMES.length; requiredFieldNumber++)
		{
			if (!table.hasField(REQUIRED_FIELD_NAMES[requiredFieldNumber]))
			{
				return null;
			}
		}

		int recordCount = table.getRecordCount();
		int resultCount = recordCount * 2;

		QuantimodoMeasurement[] result = new QuantimodoMeasurement[resultCount];

		for (int recordNumber = 0, resultNumber = 0; recordNumber < recordCount; recordNumber++, resultNumber = resultNumber + 2)
		{
			long startTime = ((Number) table.getData(recordNumber, "startTime")).longValue();
			long toTime = ((Number) table.getData(recordNumber, "toTime")).longValue();

			long duration = toTime - startTime;
			float durationMinutes = (duration / 1000) / 60f;

			double quality = ((Number) table.getData(recordNumber, "quality")).doubleValue();

			result[resultNumber] = new QuantimodoMeasurement("Sleep as Android", "Sleep Duration", "Sleep", "SUM", startTime, durationMinutes, "min");
			result[resultNumber + 1] = new QuantimodoMeasurement("Sleep as Android", "Sleep Quality", "Sleep", "MEAN", startTime, quality, "/1");
		}

		return result;
	}
}
