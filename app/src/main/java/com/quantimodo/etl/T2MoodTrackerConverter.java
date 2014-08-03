package com.quantimodo.etl;

import com.quantimodo.android.sdk.model.Measurement;
import com.quantimodo.android.sdk.model.MeasurementSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class T2MoodTrackerConverter implements Converter
{
	public static final T2MoodTrackerConverter instance = new T2MoodTrackerConverter();

	private T2MoodTrackerConverter()
	{
	}

	public ArrayList<MeasurementSet> convert(final DatabaseView databaseView)
	{
		if ((databaseView == null) || (!databaseView.hasTable("scale")) || (!databaseView.hasTable("result")))
		{
			return null;
		}

		final Table table = databaseView.getTable("result");
		if ((!table.hasField("scale_id")) || (!table.hasField("timestamp")) || (!table.hasField("value")))
		{
			return null;
		}

		final Map<Long, String> scaleNames = new HashMap<Long, String>();
		{
			final Table scaleTable = databaseView.getTable("scale");
			if ((!scaleTable.hasField("_id")) || (!scaleTable.hasField("min_label")) || (!scaleTable.hasField("max_label")))
			{
				return null;
			}

			final int recordCount = scaleTable.getRecordCount();
			for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
			{
				final long id = ((Number) scaleTable.getData(recordNumber, "_id")).longValue();
				scaleNames.put(id, String.format("%s/%s", (String) scaleTable.getData(recordNumber, "min_label"), (String) scaleTable.getData(recordNumber, "max_label")));
			}
		}

		final int recordCount = table.getRecordCount();
		ArrayList<Measurement> measurements = new ArrayList<Measurement>(recordCount);
		for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
		{
			final long scaleID = ((Number) table.getData(recordNumber, "scale_id")).longValue();

			final String name = scaleNames.get(scaleID);    //TODO Track other aspects individually just like "How Are You Feeling" converter
			final byte value = ((Number) table.getData(recordNumber, "value")).byteValue();
			final long timestamp = ((Number) table.getData(recordNumber, "timestamp")).longValue() / 1000;

			measurements.add(new Measurement(timestamp, value));
		}

		ArrayList<MeasurementSet> measurementSets = new ArrayList<MeasurementSet>(1);
		measurementSets.add(new MeasurementSet("Overall Mood", null, "Mood", "%", MeasurementSet.COMBINE_MEAN, "T2 Mood Tracker"));
		return measurementSets;
	}
}
