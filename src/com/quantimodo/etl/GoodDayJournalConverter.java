package com.quantimodo.etl;

import com.quantimodo.sdk.model.QuantimodoMeasurement;

import java.util.GregorianCalendar;

public class GoodDayJournalConverter implements Converter
{
	public static final GoodDayJournalConverter instance = new GoodDayJournalConverter();

	private static final String[] REQUIRED_FIELD_NAMES = new String[]{"year", "month", "day", "rating", "note", "ismood"};

	private GoodDayJournalConverter()
	{
	}

	public QuantimodoMeasurement[] convert(final DatabaseView databaseView)
	{
		if (databaseView == null)
		{
			return null;
		}

		final Table table;
		{
			final int tableNumber = databaseView.getTableNumber("day");
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
		final QuantimodoMeasurement[] result = new QuantimodoMeasurement[recordCount];
		for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
		{
			final int year = ((Number) table.getData(recordNumber, "year")).intValue();
			final int month = ((Number) table.getData(recordNumber, "month")).intValue();
			final int day = ((Number) table.getData(recordNumber, "day")).intValue();
			final double rating = ((Number) table.getData(recordNumber, "rating")).doubleValue();

			final long timestamp = (new GregorianCalendar(year, month - 1, day)).getTimeInMillis();

			//result[recordNumber] = new QuantimodoMeasurement("Good Day Journal", "mood", "mood", false, false, false, rating, "out of 5", timestamp, 86400);
			result[recordNumber] = new QuantimodoMeasurement("Good Day Journal", "Overall Mood", "Mood", "MEAN", timestamp, rating, "/5");
		}

		return result;
	}
}
