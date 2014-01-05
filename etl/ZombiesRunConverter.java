package com.quantimodo.etl;

import com.quantimodo.sdk.model.QuantimodoMeasurement;

import java.text.SimpleDateFormat;

public class ZombiesRunConverter implements Converter
{
	public static final ZombiesRunConverter instance = new ZombiesRunConverter();

	private static final SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	private ZombiesRunConverter()
	{
	}

	public QuantimodoMeasurement[] convert(final DatabaseView databaseView)
	{
		if ((databaseView == null) || (!databaseView.hasTable("runrecord")))
		{
			return null;
		}

		final Table runs = databaseView.getTable("runrecord");
		if ((!runs.hasField("distance")) || (!runs.hasField("started")) || (!runs.hasField("ended")))
		{
			return null;
		}

		final int runCount = runs.getRecordCount();
		final QuantimodoMeasurement[] results = new QuantimodoMeasurement[runCount];

		for (int runNumber = 0; runNumber < runCount; runNumber++)
		{
			final int distance = (Integer) runs.getData(runNumber, "distance");
			final Long startTime = ParseUtil.parseNanoTime((String) runs.getData(runNumber, "started"));
			if (startTime == null)
			{
				return null;
			}
			final Long endTime = ParseUtil.parseNanoTime((String) runs.getData(runNumber, "ended"));
			if (endTime == null)
			{
				return null;
			}

			//results[runNumber] = new QuantimodoMeasurement("Zombies, Run!", "activity", "walk/run distance", false, true, true, distance, "m", startTime, (int) ((endTime - startTime + 500) / 1000));
			results[runNumber] = new QuantimodoMeasurement("Zombies, Run!", "Walk/Run Distance", "Physical Activity", "SUM", startTime, distance, "m");
		}

		return results;
	}
}
