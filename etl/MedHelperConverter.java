package com.quantimodo.etl;

import com.quantimodo.sdk.model.QuantimodoMeasurement;

import java.util.HashMap;
import java.util.Map;

public class MedHelperConverter implements Converter
{
	public static final MedHelperConverter instance = new MedHelperConverter();

	private static final String[] unitTypes = new String[]{"tablets", "mg", "mL", "drops", "applications", "mcg", "units", "puffs", "sprays", "capsules", "g"};

	private MedHelperConverter()
	{
	}

	public QuantimodoMeasurement[] convert(final DatabaseView databaseView)
	{
		if ((databaseView == null) || (!databaseView.hasTable("prescription")) || (!databaseView.hasTable("doselog")))
		{
			return null;
		}

		final Map<Long, String> prescriptionNames = new HashMap<Long, String>();
		final Map<Long, String> prescriptionUnits = new HashMap<Long, String>();
		{
			final Table prescriptionTable = databaseView.getTable("prescription");
			if ((!prescriptionTable.hasField("_id")) || (!prescriptionTable.hasField("name")) || (!prescriptionTable.hasField("inventorytype")))
			{
				return null;
			}

			final int recordCount = prescriptionTable.getRecordCount();
			for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
			{
				final long id = ((Number) prescriptionTable.getData(recordNumber, "_id")).longValue();
				prescriptionNames.put(id, (String) prescriptionTable.getData(recordNumber, "name"));
				prescriptionUnits.put(id, unitTypes[((Number) prescriptionTable.getData(recordNumber, "inventorytype")).intValue()]);
			}
		}

		final Table table = databaseView.getTable("doselog");
		if ((!table.hasField("prescriptionid")) || (!table.hasField("actualtime")) || (!table.hasField("actualdosage")))
		{
			return null;
		}

		final int recordCount = table.getRecordCount();
		final QuantimodoMeasurement[] result = new QuantimodoMeasurement[recordCount];
		for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
		{
			final long prescriptionID = ((Number) table.getData(recordNumber, "prescriptionid")).longValue();

			String name = prescriptionNames.get(prescriptionID);

			final double dose = ((Number) table.getData(recordNumber, "actualdosage")).doubleValue();
			final String unit = prescriptionUnits.get(prescriptionID);
			final long timestamp = ((Number) table.getData(recordNumber, "actualtime")).longValue();

			//result[recordNumber] = new QuantimodoMeasurement("Med Helper", "Medication", name, true, true, true, dose, unit, timestamp, 0);
			result[recordNumber] = new QuantimodoMeasurement("Med Helper", name, "Medications", "SUM", timestamp, dose, unit);
		}

		return result;
	}
}
