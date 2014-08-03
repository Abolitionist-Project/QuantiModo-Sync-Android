package com.quantimodo.etl;

import com.quantimodo.android.sdk.model.Measurement;
import com.quantimodo.android.sdk.model.MeasurementSet;

import java.util.ArrayList;
import java.util.HashMap;

public class CallRecorderConverter implements Converter
{
	public static final CallRecorderConverter instance = new CallRecorderConverter();

	private static final String[] REQUIRED_FIELD_NAMES = new String[]{"PhoneNumber", "Date", "Duration"};

	private CallRecorderConverter()
	{
	}

	public ArrayList<MeasurementSet> convert(final DatabaseView databaseView)
	{
		if (databaseView == null)
		{
			return null;
		}

		final Table table;
		{
			final int tableNumber = databaseView.getTableNumber("recordings");
			table = databaseView.getTable((tableNumber == -1) ? 0 : tableNumber);
		}

		for (int requiredFieldNumber = 1; requiredFieldNumber < REQUIRED_FIELD_NAMES.length; requiredFieldNumber++)
		{
			if (!table.hasField(REQUIRED_FIELD_NAMES[requiredFieldNumber]))
			{
				return null;
			}
		}

		final int recordCount = table.getRecordCount();
		final HashMap<String, MeasurementSet> measurementSets = new HashMap<String, MeasurementSet>();
		for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
		{
			final String phoneNumber = (String) table.getData(recordNumber, "PhoneNumber");
			final long timestamp = ((Number) table.getData(recordNumber, "Date")).longValue() / 1000;
			final int duration;
			try
			{
				final String durationText = (String) table.getData(recordNumber, "Duration");
				final int durationLength = durationText.length();
				final int hours = Integer.parseInt(durationText.substring(0, durationLength - 6));
				final int minutes = Integer.parseInt(durationText.substring(durationLength - 5, durationLength - 3));
				final int seconds = Integer.parseInt(durationText.substring(durationLength - 2));
				duration = seconds + 60 * (minutes + 60 * hours);
			}
			catch (final NumberFormatException e)
			{
				return null;
			}

			if(!measurementSets.containsKey(phoneNumber))
			{
				MeasurementSet newSet = new MeasurementSet("Telephone Call With " + phoneNumber, null, "Social Interactions", "s", MeasurementSet.COMBINE_SUM, "Call Recorder");
				newSet.measurements.add(new Measurement(timestamp, duration, duration));
				measurementSets.put(phoneNumber, newSet);
			}
			else
			{
				measurementSets.get(phoneNumber).measurements.add(new Measurement(timestamp, duration, duration));
			}

			//result[recordNumber] = new QuantimodoMeasurement("Call Recorder", "telephone call", "telephone call with " + phoneNumber, true, true, true, duration, "s", timestamp, duration);
		}

		// Convert the hashmap to an arraylist and return it
		return new ArrayList<MeasurementSet>(measurementSets.values());
	}
}
