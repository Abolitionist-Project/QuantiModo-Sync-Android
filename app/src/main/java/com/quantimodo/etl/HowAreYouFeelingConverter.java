package com.quantimodo.etl;

import com.quantimodo.android.sdk.model.Measurement;
import com.quantimodo.android.sdk.model.MeasurementSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HowAreYouFeelingConverter implements Converter
{
	public static final HowAreYouFeelingConverter instance = new HowAreYouFeelingConverter();

	private HowAreYouFeelingConverter()
	{
	}

	public ArrayList<MeasurementSet> convert(final DatabaseView databaseView)
	{
		if ((databaseView == null) || (!databaseView.hasTable("buttonconfiguration")) || (!databaseView.hasTable("buttonconfigurationlevel")) || (!databaseView.hasTable("feelinghistory")))
		{
			return null;
		}

		final Map<Integer, String> buttonScaleName = new HashMap<Integer, String>();
		final Map<Integer, String> buttonUnit = new HashMap<Integer, String>();
		final Map<Integer, Integer> buttonValue = new HashMap<Integer, Integer>();
		{
			final Table scales = databaseView.getTable("buttonconfiguration");
			if ((!scales.hasField("id")) || (!scales.hasField("selectionlabel")))
			{
				return null;
			}

			final Table buttons = databaseView.getTable("buttonconfigurationlevel");
			if ((!buttons.hasField("id")) || (!buttons.hasField("configurationid")) || (!buttons.hasField("value")))
			{
				return null;
			}

			final int scaleCount = scales.getRecordCount();
			final int buttonCount = buttons.getRecordCount();
			for (int scaleNumber = 0; scaleNumber < scaleCount; scaleNumber++)
			{
				final int scaleID = ((Number) scales.getData(scaleNumber, "id")).intValue();
				final String scaleName = (String) scales.getData(scaleNumber, "selectionlabel");

				int scaleLow = Integer.MAX_VALUE;
				int scaleHigh = Integer.MIN_VALUE;
				for (int buttonNumber = 0; buttonNumber < buttonCount; buttonNumber++)
				{
					if (scaleID == ((Number) buttons.getData(buttonNumber, "configurationid")).intValue())
					{
						int value = ((Number) buttons.getData(buttonNumber, "value")).intValue();
						buttonValue.put(buttonNumber, value);
						buttonScaleName.put(buttonNumber, scaleName);

						scaleLow = Math.min(scaleLow, value);
						scaleHigh = Math.max(scaleHigh, value);
					}
				}

				final String unit = scaleLow + " to " + scaleHigh;
				for (int buttonNumber = 0; buttonNumber < buttonCount; buttonNumber++)
				{
					if (scaleID == ((Number) buttons.getData(buttonNumber, "configurationid")).intValue())
					{
						buttonUnit.put(buttonNumber, unit);
					}
				}
			}
		}

		final Table feelings = databaseView.getTable("feelinghistory");
		if ((!feelings.hasField("time")) || (!feelings.hasField("levelid")))
		{
			return null;
		}

		final int recordCount = feelings.getRecordCount();

		HashMap<String, MeasurementSet> measurementSets = new HashMap<String, MeasurementSet>();

		for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
		{
			final int buttonID = ((Number) feelings.getData(recordNumber, "levelid")).intValue();
			final long timestamp = (Long) feelings.getData(recordNumber, "time") / 1000;

			//TODO try to standardize rating types so that they match existing variables in the quantimodo database
			String ratingType = buttonScaleName.get(buttonID);
			String unit = buttonUnit.get(buttonID);

			if(!measurementSets.containsKey(ratingType + unit))
			{
				MeasurementSet newSet = new MeasurementSet(ratingType, null, "Mood", unit, MeasurementSet.COMBINE_MEAN, "How Are You Feeling?");
				newSet.measurements.add(new Measurement(timestamp, ((Number)buttonValue.get(buttonID)).doubleValue()));
				measurementSets.put(ratingType + unit, newSet);
			}
			else
			{
				measurementSets.get(ratingType + unit).measurements.add(new Measurement(timestamp, ((Number)buttonValue.get(buttonID)).doubleValue()));
			}
		}

		return new ArrayList<MeasurementSet>(measurementSets.values());
	}
}
