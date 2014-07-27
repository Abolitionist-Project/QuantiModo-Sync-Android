package com.quantimodo.etl;

import com.quantimodo.sdk.model.Measurement;
import com.quantimodo.sdk.model.MeasurementSet;

import java.util.ArrayList;

public class ZombiesRunConverter implements Converter
{
	public static final ZombiesRunConverter instance = new ZombiesRunConverter();

	private ZombiesRunConverter()
	{
	}

	public ArrayList<MeasurementSet> convert(final DatabaseView databaseView)
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
		ArrayList<Measurement> measurements = new ArrayList<Measurement>(runCount);

		for (int runNumber = 0; runNumber < runCount; runNumber++)
		{
			final int distance = (Integer) runs.getData(runNumber, "distance");

			Long startTime = ParseUtil.parseNanoTime((String) runs.getData(runNumber, "started"));
			if (startTime == null)
			{
				return null;
			}
			startTime = startTime / 1000;

			Long endTime = ParseUtil.parseNanoTime((String) runs.getData(runNumber, "ended"));
			if (endTime == null)
			{
				return null;
			}
			endTime = startTime / 1000;

			int duration = (int) (endTime - startTime);

			measurements.add(new Measurement(startTime, distance, duration));
		}

		ArrayList<MeasurementSet> measurementSets = new ArrayList<MeasurementSet>(1);
		measurementSets.add(new MeasurementSet("Walk/Run Distance", null, "Physical Activity", "m", MeasurementSet.COMBINE_SUM, "Zombies, Run!"));
		return measurementSets;
	}
}
