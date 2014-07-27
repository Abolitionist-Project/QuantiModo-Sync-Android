package com.quantimodo.etl;

import com.quantimodo.sdk.model.Measurement;
import com.quantimodo.sdk.model.MeasurementSet;

import java.util.ArrayList;
import java.util.GregorianCalendar;

public class AccupedoConverter implements Converter
{
	public static final AccupedoConverter instance = new AccupedoConverter();

	private static final String[] REQUIRED_FIELD_NAMES = new String[]{"steps", "distance", "calories", "steptime", "achievement", "year", "month", "day", "hour", "minute"};

	private AccupedoConverter()
	{
	}
	
	public ArrayList<MeasurementSet> convert(final DatabaseView databaseView)
	{
		if ((databaseView == null) || (!databaseView.hasTable("diaries")))
		{
			return null;
		}

		final Table table = databaseView.getTable("diaries");
		for (int requiredFieldNumber = 0; requiredFieldNumber < REQUIRED_FIELD_NAMES.length; requiredFieldNumber++)
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

		final ArrayList<Measurement> stepGoalMeasurements = new ArrayList<Measurement>(recordCount);
		final ArrayList<Measurement> stepsMeasurements = new ArrayList<Measurement>(recordCount);
		final ArrayList<Measurement> distanceMeasurements = new ArrayList<Measurement>(recordCount);
		final ArrayList<Measurement> caloriesMeasurements = new ArrayList<Measurement>(recordCount);

		int dayStartRecord = 0;
		int lastStepCount = 0;
		double lastDistance = 0.0;
		double lastCalories = 0.0;
		int lastSteppingDuration = 0;
		boolean lastAchievedGoal = false;
		int lastYear = Integer.MIN_VALUE;
		long lastMonth = Integer.MIN_VALUE;
		long lastDay = Integer.MIN_VALUE;
		for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
		{
			final int stepCount = ((Number) table.getData(recordNumber, "steps")).intValue();
			final double distance = ((Number) table.getData(recordNumber, "distance")).doubleValue();
			final double calories = ((Number) table.getData(recordNumber, "calories")).doubleValue();
			final int steppingDuration = ((Number) table.getData(recordNumber, "steptime")).intValue();
			final boolean achievedGoal = ((Number) table.getData(recordNumber, "achievement")).intValue() == 1;
			final int year = ((Number) table.getData(recordNumber, "year")).intValue();
			final int month = ((Number) table.getData(recordNumber, "month")).intValue();
			final int day = ((Number) table.getData(recordNumber, "day")).intValue();
			final int hour = ((Number) table.getData(recordNumber, "hour")).intValue();
			final int minute = ((Number) table.getData(recordNumber, "minute")).intValue();

			final long inputTime = (new GregorianCalendar(year, month - 1, day, hour, minute, 0)).getTimeInMillis();

			final int outputStepCount;
			final double outputDistance;
			final double outputCalories;
			final int outputSteppingDuration;
			final boolean outputAchievedGoal;
			final long outputTime = (inputTime - steppingDuration) / 1000; // Convert to seconds;

			if ((stepCount >= lastStepCount) && (lastDay == day) && (lastMonth == month) && (lastYear == year))
			{
				outputStepCount = stepCount - lastStepCount;
				outputDistance = distance - lastDistance;
				outputCalories = calories - lastCalories;
				outputSteppingDuration = (steppingDuration - lastSteppingDuration) / 1000;  // Convert to seconds
				outputAchievedGoal = achievedGoal & (!lastAchievedGoal);
			}
			else
			{
				// If the person reset their steps because someone was using the phone, delete all the previous records for the day.
				//TODO rewrite this to use timestamp instead of record number
				/*if ((lastDay == day) && (lastMonth == month) && (lastYear == year))
				{
					for (int invalidRecordNumber = stepsMeasurements.size() - 1; invalidRecordNumber >= dayStartRecord; invalidRecordNumber--)
					{
						results.remove(invalidRecordNumber);
					}
				}
				else
				{
					dayStartRecord = stepsMeasurements.size();
				}*/

				outputStepCount = stepCount;
				outputDistance = distance;
				outputCalories = calories;
				outputSteppingDuration = steppingDuration;
				outputAchievedGoal = achievedGoal;
			}

			if (outputStepCount > 0)
			{
				if (outputAchievedGoal)
				{
					long timestamp = (new GregorianCalendar(year, month - 1, day)).getTimeInMillis() / 1000;
					stepGoalMeasurements.add(new Measurement(timestamp, 1));
					//results.add(new QuantimodoMeasurement("Accupedo", "Daily Step Count Goal Reached", "Goals", "SUM", timestamp, 1, "count"));
				}

				stepsMeasurements.add(new Measurement(outputTime, outputStepCount, steppingDuration));
				distanceMeasurements.add(new Measurement(outputTime, outputDistance, steppingDuration));
				caloriesMeasurements.add(new Measurement(outputTime, outputCalories, steppingDuration));

				//results.add(new QuantimodoMeasurement("Accupedo", "activity", "steps", false, true, true, outputStepCount, "steps", outputTime, (outputSteppingDuration + 500) / 1000));
				//results.add(new QuantimodoMeasurement("Accupedo", "activity", "walk/run distance", false, true, true, outputDistance, "mi", outputTime, (outputSteppingDuration + 500) / 1000));
				//results.add(new QuantimodoMeasurement("Accupedo", "activity", "calories burned", false, false, true, outputCalories, "kcal", outputTime, (outputSteppingDuration + 500) / 1000));
			}

			lastStepCount = stepCount;
			lastDistance = distance;
			lastCalories = calories;
			lastSteppingDuration = steppingDuration;
			lastAchievedGoal = achievedGoal;
			lastYear = year;
			lastMonth = month;
			lastDay = day;
		}

		ArrayList<MeasurementSet> measurementSets = new ArrayList<MeasurementSet>(4);
		if(stepGoalMeasurements.size() != 0)
		{
			measurementSets.add(new MeasurementSet("Daily Step Count Goal Reached", null, "Goals", "count", MeasurementSet.COMBINE_SUM, "Accupedo", stepGoalMeasurements));
		}
		if(stepsMeasurements.size() != 0)
		{
			measurementSets.add(new MeasurementSet("Steps", null, "Physical Activity", "steps", MeasurementSet.COMBINE_SUM, "Accupedo", stepsMeasurements));
		}
		if(distanceMeasurements.size() != 0)
		{
			measurementSets.add(new MeasurementSet("Walk/Run Distance", null, "Goals", "count", MeasurementSet.COMBINE_SUM, "Accupedo", distanceMeasurements));
		}
		if(caloriesMeasurements.size() != 0)
		{
			measurementSets.add(new MeasurementSet("Calories Burned", null, "Goals", "cal", MeasurementSet.COMBINE_SUM, "Accupedo", caloriesMeasurements));
		}
		return measurementSets;
	}
}
