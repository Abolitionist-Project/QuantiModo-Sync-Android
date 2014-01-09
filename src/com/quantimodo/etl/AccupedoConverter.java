package com.quantimodo.etl;

import com.quantimodo.sdk.model.QuantimodoMeasurement;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

public class AccupedoConverter implements Converter
{
	public static final AccupedoConverter instance = new AccupedoConverter();

	private static final String[] REQUIRED_FIELD_NAMES = new String[]{"steps", "distance", "calories", "steptime", "achievement", "year", "month", "day", "hour", "minute"};
	private static final QuantimodoMeasurement[] EMPTY_RESULT = new QuantimodoMeasurement[0];

	private AccupedoConverter()
	{
	}

	public QuantimodoMeasurement[] convert(final DatabaseView databaseView)
	{
		if ((databaseView == null) || (!databaseView.hasTable("diaries")))
		{
			return null;
		}

		final Table table = databaseView.getTable("diaries");
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
			return EMPTY_RESULT;
		}

		final List<QuantimodoMeasurement> results = new ArrayList<QuantimodoMeasurement>(4 * recordCount);

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
			final long outputTime = inputTime - steppingDuration;

			if ((stepCount >= lastStepCount) && (lastDay == day) && (lastMonth == month) && (lastYear == year))
			{
				outputStepCount = stepCount - lastStepCount;
				outputDistance = distance - lastDistance;
				outputCalories = calories - lastCalories;
				outputSteppingDuration = steppingDuration - lastSteppingDuration;
				outputAchievedGoal = achievedGoal & (!lastAchievedGoal);
			}
			else
			{
				// If the person reset their steps because someone was using the phone, delete all the previous records for the day.
				if ((lastDay == day) && (lastMonth == month) && (lastYear == year))
				{
					for (int invalidRecordNumber = results.size() - 1; invalidRecordNumber >= dayStartRecord; invalidRecordNumber--)
					{
						results.remove(invalidRecordNumber);
					}
				}
				else
				{
					dayStartRecord = results.size();
				}

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
					long timestamp = (new GregorianCalendar(year, month - 1, day)).getTimeInMillis();
					//results.add(new QuantimodoMeasurement("Accupedo", "goals", "daily step count goal reached", false, true, true, 1.0, "(count)", (new GregorianCalendar(year, month - 1, day)).getTimeInMillis(), 86400));
					results.add(new QuantimodoMeasurement("Accupedo", "Daily Step Count Goal Reached", "Goals", "SUM", timestamp, 1.0, "count"));
				}
				//results.add(new QuantimodoMeasurement("Accupedo", "activity", "steps", false, true, true, outputStepCount, "steps", outputTime, (outputSteppingDuration + 500) / 1000));
				results.add(new QuantimodoMeasurement("Accupedo", "Steps", "Physical Activity", "SUM", outputTime, outputStepCount, "steps"));
				//results.add(new QuantimodoMeasurement("Accupedo", "activity", "walk/run distance", false, true, true, outputDistance, "mi", outputTime, (outputSteppingDuration + 500) / 1000));
				results.add(new QuantimodoMeasurement("Accupedo", "Walk/Run Distance", "Physical Activity", "SUM", outputTime, outputDistance, "steps"));
				//results.add(new QuantimodoMeasurement("Accupedo", "activity", "calories burned", false, false, true, outputCalories, "kcal", outputTime, (outputSteppingDuration + 500) / 1000));
				results.add(new QuantimodoMeasurement("Accupedo", "Calories Burned", "Physical Activity", "SUM", outputTime, outputCalories, "steps"));
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

		return results.toArray(EMPTY_RESULT);
	}
}
