package com.quantimodo.etl;

import com.quantimodo.sdk.model.QuantimodoMeasurement;

import java.util.ArrayList;

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

		ArrayList<QuantimodoMeasurement> measurements = new ArrayList<QuantimodoMeasurement>(recordCount * 2);
		for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
		{
			long startTime = ((Number) table.getData(recordNumber, "startTime")).longValue();
			long toTime = ((Number) table.getData(recordNumber, "toTime")).longValue();

			int rating = ((Number) table.getData(recordNumber, "rating")).intValue();
			int cycles = ((Number) table.getData(recordNumber, "cycles")).intValue();
			double noise = ((Number) table.getData(recordNumber, "noiseLevel")).doubleValue();
			double quality = ((Number) table.getData(recordNumber, "quality")).doubleValue();

			long duration = toTime - startTime;
			float durationMinutes = (duration / 1000) / 60;

			//measurements.add(new QuantimodoMeasurement("Sleep as Android", "Bedtime", "Sleep", "SUM", startTime, startTime / 1000, "epoch"));
			//measurements.add(new QuantimodoMeasurement("Sleep as Android", "Wakeup Time", "Sleep", "SUM", toTime, toTime / 1000, "epoch"));

			measurements.add(new QuantimodoMeasurement("Sleep as Android", "Sleep Duration", "Sleep", "SUM", toTime, durationMinutes, "min"));
			measurements.add(new QuantimodoMeasurement("Sleep as Android", "Sleep Cycles", "Sleep", "SUM", toTime, cycles, "event"));

			if(noise >= 0 && noise <= 1)
			{
				measurements.add(new QuantimodoMeasurement("Sleep as Android", "Sleep Noise Level", "Sleep", "SUM", toTime, noise, "/1"));
			}
			if(rating >= 0 && rating <= 5)
			{
				measurements.add(new QuantimodoMeasurement("Sleep as Android", "Sleep Rating", "Sleep", "SUM", toTime, rating, "/6"));
			}
			if(quality >= 0 && quality <= 1)
			{
				measurements.add(new QuantimodoMeasurement("Sleep as Android", "Sleep Quality", "Sleep", "MEAN", toTime, quality, "/1"));
			}
		}

		return measurements.toArray(new QuantimodoMeasurement[measurements.size()]);
	}
}
