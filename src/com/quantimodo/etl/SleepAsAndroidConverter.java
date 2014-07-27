package com.quantimodo.etl;

import com.quantimodo.sdk.model.Measurement;
import com.quantimodo.sdk.model.MeasurementSet;

import java.util.ArrayList;

public class SleepAsAndroidConverter implements Converter
{
	public static final SleepAsAndroidConverter instance = new SleepAsAndroidConverter();

	private static final String[] REQUIRED_FIELD_NAMES = new String[]{"startTime", "toTime", "quality"};

	private SleepAsAndroidConverter()
	{
	}

	public ArrayList<MeasurementSet> convert(final DatabaseView databaseView)
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

		ArrayList<Measurement> durationMeasurements = new ArrayList<Measurement>(recordCount);
		ArrayList<Measurement> cyclesMeasurements = new ArrayList<Measurement>(recordCount);
		ArrayList<Measurement> noiseMeasurements = new ArrayList<Measurement>(recordCount);
		ArrayList<Measurement> ratingMeasurements = new ArrayList<Measurement>(recordCount);
		ArrayList<Measurement> qualityMeasurements = new ArrayList<Measurement>(recordCount);

		for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
		{
			long startTime = ((Number) table.getData(recordNumber, "startTime")).longValue() / 1000;
			long toTime = ((Number) table.getData(recordNumber, "toTime")).longValue() / 1000;

			int rating = ((Number) table.getData(recordNumber, "rating")).intValue();
			int cycles = ((Number) table.getData(recordNumber, "cycles")).intValue();
			double noise = ((Number) table.getData(recordNumber, "noiseLevel")).doubleValue();
			double quality = ((Number) table.getData(recordNumber, "quality")).doubleValue();

			int duration = (int) (toTime - startTime);
			float durationMinutes = duration / 60;

			durationMeasurements.add(new Measurement(toTime, durationMinutes, duration));
			cyclesMeasurements.add(new Measurement(toTime, cycles));

			if(noise >= 0 && noise <= 1)
			{
				noiseMeasurements.add(new Measurement(toTime, noise));
			}
			if(rating >= 0 && rating <= 5)
			{
				ratingMeasurements.add(new Measurement(toTime, rating));
			}
			if(quality >= 0 && quality <= 1)
			{
				qualityMeasurements.add(new Measurement(toTime, quality));
			}
		}

		ArrayList<MeasurementSet> measurementSets = new ArrayList<MeasurementSet>(5);
		if(durationMeasurements.size() != 0)
		{
			measurementSets.add(new MeasurementSet("Sleep Duration", null, "Sleep", "min", MeasurementSet.COMBINE_SUM, "Sleep as Android", durationMeasurements));
		}
		if(cyclesMeasurements.size() != 0)
		{
			measurementSets.add(new MeasurementSet("Sleep Cycles", null, "Sleep", "event", MeasurementSet.COMBINE_SUM, "Sleep as Android", cyclesMeasurements));
		}
		if(noiseMeasurements.size() != 0)
		{
			measurementSets.add(new MeasurementSet("Sleep Noise Level", null, "Sleep", "/1", MeasurementSet.COMBINE_MEAN, "Sleep as Android", noiseMeasurements));
		}
		if(ratingMeasurements.size() != 0)
		{
			measurementSets.add(new MeasurementSet("Sleep Rating", null, "Sleep", "/6", MeasurementSet.COMBINE_MEAN, "Sleep as Android", ratingMeasurements));
		}
		if(qualityMeasurements.size() != 0)
		{
			measurementSets.add(new MeasurementSet("Deep Sleep", null, "Sleep", "/1", MeasurementSet.COMBINE_MEAN, "Sleep as Android", qualityMeasurements));
		}

		return measurementSets;
	}
}
