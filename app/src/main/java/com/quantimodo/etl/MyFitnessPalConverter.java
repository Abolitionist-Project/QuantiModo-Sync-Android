package com.quantimodo.etl;

import com.quantimodo.android.sdk.model.Measurement;
import com.quantimodo.android.sdk.model.MeasurementSet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MyFitnessPalConverter implements Converter
{
	public static final MyFitnessPalConverter instance = new MyFitnessPalConverter();

	private static final String[] unitTypes = new String[]{"serving", "serving", "serving", "serving", "serving", "serving", "serving", "serving", "serving", "serving", "serving"};

	private MyFitnessPalConverter()
	{
	}

	public ArrayList<MeasurementSet> convert(final DatabaseView databaseView)
	{
		if ((databaseView == null) || (!databaseView.hasTable("foods")) || (!databaseView.hasTable("food_entries")))
		{
			return null;
		}

		final Map<Long, String> foodsNames = new HashMap<Long, String>();
		final Map<Long, String> foodsUnits = new HashMap<Long, String>();
		{
			final Table foodsTable = databaseView.getTable("foods");
			if ((!foodsTable.hasField("id")) || (!foodsTable.hasField("description")) || (!foodsTable.hasField("food_type")))
			{
				return null;
			}

			final int recordCount = foodsTable.getRecordCount();
			for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
			{
				final long id = ((Number) foodsTable.getData(recordNumber, "id")).longValue();
				foodsNames.put(id, (String) foodsTable.getData(recordNumber, "description"));
				foodsUnits.put(id, unitTypes[((Number) foodsTable.getData(recordNumber, "food_type")).intValue()]);
			}
		}

		final Table table = databaseView.getTable("food_entries");
		if ((!table.hasField("food_id")) || (!table.hasField("entry_date")) || (!table.hasField("quantity")))
		{
			return null;
		}

		Calendar calendar = Calendar.getInstance();
		calendar.clear(Calendar.SECOND);
		calendar.clear(Calendar.MINUTE);
		calendar.clear(Calendar.MILLISECOND);

		final int recordCount = table.getRecordCount();
		final HashMap<String, MeasurementSet> measurementSets = new HashMap<String, MeasurementSet>();
		for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
		{
			final long foodsID = ((Number) table.getData(recordNumber, "food_id")).longValue();

			String name = foodsNames.get(foodsID);

			final double dose = ((Number) table.getData(recordNumber, "quantity")).doubleValue();
			final String unit = foodsUnits.get(foodsID);
			final String[] dateString = ((String) table.getData(recordNumber, "entry_date")).split("-");
			int mealId = ((Number) table.getData(recordNumber, "meal_id")).intValue();
			
			switch(mealId)
			{
				case 1: calendar.set(Calendar.HOUR_OF_DAY, 8);
				  break;
				case 2: calendar.set(Calendar.HOUR_OF_DAY, 12);
				  break;
				case 3: calendar.set(Calendar.HOUR_OF_DAY, 18);
				  break;
				case 4: calendar.set(Calendar.HOUR_OF_DAY, 15);
				  break;
			}

			calendar.set(Integer.valueOf(dateString[0]), Integer.valueOf(dateString[1]) - 1,  Integer.valueOf(dateString[2]));
			final long timestamp = calendar.getTimeInMillis() / 1000;

			if(!measurementSets.containsKey(name + unit))
			{
				MeasurementSet newSet = new MeasurementSet(name, null, "Foods", unit, MeasurementSet.COMBINE_SUM, "MyFitnessPal");
				newSet.measurements.add(new Measurement(timestamp, dose));
				measurementSets.put(name + unit, newSet);
			}
			else
			{
				measurementSets.get(name + unit).measurements.add(new Measurement(timestamp, dose));
			}
		}

		return new ArrayList<MeasurementSet>(measurementSets.values());
	}
}
