package com.quantimodo.etl;

import com.quantimodo.sdk.model.Measurement;
import com.quantimodo.sdk.model.MeasurementSet;

import java.util.ArrayList;

public class SportsTrackerConverter implements Converter
{
	public static final SportsTrackerConverter instance = new SportsTrackerConverter();

	private static final String[] REQUIRED_FIELD_NAMES = new String[]{"distance", "date_time", "duration", "bpm", "breathing_rate", "skin_temp"};

	private SportsTrackerConverter()
	{
	}

	public ArrayList<MeasurementSet> convert(final DatabaseView databaseView)
	{
		if ((databaseView == null) || (!databaseView.hasTable("TrackPoint")))
		{
			return null;
		}

		final Table table = databaseView.getTable("TrackPoint");
		for (int requiredFieldNumber = 1; requiredFieldNumber < REQUIRED_FIELD_NAMES.length; requiredFieldNumber++)
		{
			if (!table.hasField(REQUIRED_FIELD_NAMES[requiredFieldNumber]))
			{
				return null;
			}
		}

		final int recordCount = table.getRecordCount();
		if (recordCount == 0)
		{
			return new ArrayList<MeasurementSet>(0);
		}

		ArrayList<Measurement> distanceMeasurements = new ArrayList<Measurement>(recordCount);
		ArrayList<Measurement> heartRateMeasurements = new ArrayList<Measurement>(recordCount);
		ArrayList<Measurement> breathingRateMeasurements = new ArrayList<Measurement>(recordCount);
		ArrayList<Measurement> temperatureMeasurements = new ArrayList<Measurement>(recordCount);

		int lastTrackID = Integer.MIN_VALUE;
		double lastDistance = 0.0;
		int lastHeartRate = 0;
		double lastBreathingRate = 0.0;
		double lastSkinTemperature = 0.0;
		long lastTime = Long.MIN_VALUE;
		int lastDuration = 0;
		for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
		{
			final int trackID = ((Number) table.getData(recordNumber, "track_id")).intValue();
			final double distance = ((Number) table.getData(recordNumber, "distance")).doubleValue();
			final int heartRate = ((Number) table.getData(recordNumber, "bpm")).intValue();
			final double breathingRate = ((Number) table.getData(recordNumber, "breathing_rate")).doubleValue();
			final double skinTemperature = ((Number) table.getData(recordNumber, "skin_temp")).doubleValue();
			final long timestamp = ((Number) table.getData(recordNumber, "date_time")).longValue() / 1000;  // Convert to seconds
			final int duration = ((Number) table.getData(recordNumber, "duration")).intValue();

			final long outputTime;
			final double outputDistance;
			final int outputDuration;
			if (trackID == lastTrackID)
			{
				outputTime = lastTime;
				outputDistance = distance - lastDistance;
				outputDuration = duration - lastDuration;
			}
			else
			{
				outputTime = timestamp - duration;
				outputDistance = distance;
				outputDuration = duration;
			}

			if (outputDistance > 0)
			{
				distanceMeasurements.add(new Measurement(outputTime, outputDistance, outputDuration));
			}
			if (heartRate > 0)
			{
				distanceMeasurements.add(new Measurement(outputTime, heartRate, outputDuration));
			}
			if (breathingRate > 0)
			{
				distanceMeasurements.add(new Measurement(outputTime, breathingRate / 60.0, outputDuration));
			}
			if (skinTemperature > 0)
			{
				distanceMeasurements.add(new Measurement(outputTime, skinTemperature, outputDuration));
			}

			lastTrackID = trackID;
			lastDistance = distance;
			lastHeartRate = heartRate;
			lastBreathingRate = breathingRate;
			lastSkinTemperature = skinTemperature;
			lastTime = timestamp;
			lastDuration = duration;
		}

		ArrayList<MeasurementSet> measurementSets = new ArrayList<MeasurementSet>(4);
		if(distanceMeasurements.size() != 0)
		{
			measurementSets.add(new MeasurementSet("Walk/Run Distance", null, "Physical Activity", "m", MeasurementSet.COMBINE_SUM, "SportsTracker", distanceMeasurements));
		}
		if(heartRateMeasurements.size() != 0)
		{
			measurementSets.add(new MeasurementSet("Heart Rate", null, "Vital Signs", "bpm", MeasurementSet.COMBINE_MEAN, "SportsTracker", heartRateMeasurements));
		}
		if(breathingRateMeasurements.size() != 0)
		{
			measurementSets.add(new MeasurementSet("Breathing Rate", null, "Vital Signs", "Hz", MeasurementSet.COMBINE_MEAN, "SportsTracker", breathingRateMeasurements));
		}
		if(temperatureMeasurements.size() != 0)
		{
			measurementSets.add(new MeasurementSet("Temperature", null, "Vital Signs", "°C", MeasurementSet.COMBINE_MEAN, "SportsTracker", temperatureMeasurements));
		}
		return measurementSets;
	}
}
