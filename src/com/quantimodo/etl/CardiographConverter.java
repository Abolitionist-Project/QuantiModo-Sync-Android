package com.quantimodo.etl;

import com.quantimodo.sdk.model.Measurement;
import com.quantimodo.sdk.model.MeasurementSet;

import java.util.ArrayList;

public class CardiographConverter implements Converter
{
	public static final CardiographConverter instance = new CardiographConverter();

	private CardiographConverter()
	{
	}

	public ArrayList<MeasurementSet> convert(final DatabaseView databaseView)
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

		final ArrayList<Measurement> measurements = new ArrayList<Measurement>(recordCount);
		for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
		{
			final long timestamp = ((Number) table.getData(recordNumber, "history_date")).longValue() / 1000;
			final int bpm = ((Number) table.getData(recordNumber, "history_bpm")).intValue();

			measurements.add(new Measurement(timestamp, bpm));
			//result[recordNumber] = new QuantimodoMeasurement("CardioGraph", "Heart Rate", "Vital Signs", "MEAN", timestamp, bpm, "bmp");
		}

		ArrayList<MeasurementSet> measurementSets = new ArrayList<MeasurementSet>(1);
		measurementSets.add(new MeasurementSet("Heart Rate", "Vital Signs", "bpm",MeasurementSet.COMBINE_MEAN, "CardioGraph", measurements));
		return measurementSets;
	}
}
