package com.quantimodo.etl;

import com.quantimodo.sdk.model.QuantimodoMeasurement;

public class CardiographConverter implements Converter
{
	public static final CardiographConverter instance = new CardiographConverter();

	private CardiographConverter()
	{
	}

	public QuantimodoMeasurement[] convert(final DatabaseView databaseView)
	{
		if ((databaseView == null) || (!databaseView.hasTable("history")))
		{
			return null;
		}

		final Table table = databaseView.getTable("history");
		if ((!table.hasField("history_date")) || (!table.hasField("history_bpm")))
		{
			return null;
		}

		final int recordCount = table.getRecordCount();
		final QuantimodoMeasurement[] result = new QuantimodoMeasurement[recordCount];
		for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
		{
			final long timestamp = ((Number) table.getData(recordNumber, "history_date")).longValue();
			final int bpm = ((Number) table.getData(recordNumber, "history_bpm")).intValue();

			//result[recordNumber] = new QuantimodoMeasurement("Cardiograph", "vital sign", "heart rate", false, false, false, bpm, "bpm", timestamp, 0);
			result[recordNumber] = new QuantimodoMeasurement("CardioGraph", "Heart Rate", "Vital Signs", "MEAN", timestamp, bpm, "bmp");
		}

		return result;
	}
}
