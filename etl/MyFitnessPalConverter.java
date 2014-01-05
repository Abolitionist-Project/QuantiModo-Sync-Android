package com.quantimodo.etl;

import com.quantimodo.sdk.model.QuantimodoMeasurement;

import java.util.HashMap;
import java.util.Map;

public class MyFitnessPalConverter implements Converter
{
	public static final MyFitnessPalConverter instance = new MyFitnessPalConverter();

	private static final String[] unitTypes = new String[]{"tablets", "mg", "mL", "drops", "applications", "mcg", "units", "puffs", "sprays", "capsules", "g"};

	private MyFitnessPalConverter()
	{
	}

	



	public QuantimodoMeasurement[] convert(final DatabaseView databaseView)
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
		final QuantimodoMeasurement[] result = new QuantimodoMeasurement[recordCount];
		for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
		{
			final long foodsID = ((Number) table.getData(recordNumber, "food_id")).longValue();

			String name = foodsNames.get(foodsID);

			final double dose = ((Number) table.getData(recordNumber, "quantity")).doubleValue();
			final String unit = foodsUnits.get(foodsID);
			final String[] dateString = ((String) foodsTable.getData(recordNumber, "entry_date")).split("-");
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

			calendar.set(Integer.valueOf(dateString[0]), Integer.valueOf(dateString[1]),  Integer.valueOf(dateString[2]));
			final long timestamp = calendar.getTimeInMillis();

			//result[recordNumber] = new QuantimodoMeasurement("MyFitnessPal", "Medication", name, true, true, true, dose, unit, timestamp, 0);
			result[recordNumber] = new QuantimodoMeasurement("MyFitnessPal", name, "Foods", "SUM", timestamp, dose, unit);
		}

		return result;
	}
}
