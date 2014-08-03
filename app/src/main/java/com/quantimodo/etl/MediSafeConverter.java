package com.quantimodo.etl;

import com.quantimodo.android.sdk.model.Measurement;
import com.quantimodo.android.sdk.model.MeasurementSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MediSafeConverter implements Converter
{
	public static final MediSafeConverter instance = new MediSafeConverter();

	private static final String[] unitTypes = new String[]{"pills", "cc", "mL", "g", "mg", "drops", "pieces", "squeezes", "units"};

	private MediSafeConverter()
	{
	}

	public ArrayList<MeasurementSet> convert(final DatabaseView databaseView)
	{
		if ((databaseView == null) || (!databaseView.hasTable("medicine")) || (!databaseView.hasTable("schedulegroup")) || (!databaseView.hasTable("schedule")))
		{
			return null;
		}

		final Map<Integer, String> prescriptionNames = new HashMap<Integer, String>();
		final Map<Integer, Double> prescriptionDoses = new HashMap<Integer, Double>();
		final Map<Integer, String> prescriptionUnits = new HashMap<Integer, String>();
		{
			final Map<Integer, String> medicineNames = new HashMap<Integer, String>();
			{
				final Table medicines = databaseView.getTable("medicine");
				if ((!medicines.hasField("id")) || (!medicines.hasField("name")))
				{
					return null;
				}

				final int recordCount = medicines.getRecordCount();
				for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
				{
					medicineNames.put(((Long) medicines.getData(recordNumber, "id")).intValue(), (String) medicines.getData(recordNumber, "name"));
				}
			}

			final Table prescriptions = databaseView.getTable("schedulegroup");
			if ((!prescriptions.hasField("id")) || (!prescriptions.hasField("medicine_id")) || (!prescriptions.hasField("dose")) || (!prescriptions.hasField("type")))
			{
				return null;
			}

			final int recordCount = prescriptions.getRecordCount();
			for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
			{
				final int id = ((Number) prescriptions.getData(recordNumber, "id")).intValue();
				final int medicineID = ((Number) prescriptions.getData(recordNumber, "medicine_id")).intValue();
				final double dose = ((Number) prescriptions.getData(recordNumber, "dose")).doubleValue();
				final int unitID = ((Number) prescriptions.getData(recordNumber, "type")).intValue();

				final String medicineName = medicineNames.get(medicineID);
				final String unitName = unitTypes[unitID];

				prescriptionNames.put(id, medicineName);
				prescriptionDoses.put(id, dose);
				prescriptionUnits.put(id, unitName);
			}
		}

		final Table table = databaseView.getTable("schedule");
		if ((!table.hasField("group_id")) || (!table.hasField("actualDateTime")) || (!table.hasField("status")))
		{
			return null;
		}

		final int recordCount = table.getRecordCount();
		if (recordCount == 0)
		{
			return new ArrayList<MeasurementSet>(0);
		}

		HashMap<String, MeasurementSet> measurementSets = new HashMap<String, MeasurementSet>();

		for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
		{
			if ("taken".equals(table.getData(recordNumber, "status")))
			{
				final int id = ((Number) table.getData(recordNumber, "group_id")).intValue();

				String name = prescriptionNames.get(id);
				double dose = prescriptionDoses.get(id);
				String unit = prescriptionUnits.get(id);
				Long timestamp = ParseUtil.parseNanoTime((String) table.getData(recordNumber, "actualDateTime"));
				if (timestamp == null)
				{
					return null;
				}
				timestamp = timestamp / 1000;

				if(!measurementSets.containsKey(name + unit))
				{
					MeasurementSet newSet = new MeasurementSet(name, null, "Social Interactions", "s", MeasurementSet.COMBINE_SUM, "MediSafe");
					newSet.measurements.add(new Measurement(timestamp, dose));
					measurementSets.put(name + unit, newSet);
				}
				else
				{
					measurementSets.get(name + unit).measurements.add(new Measurement(timestamp, dose));
				}
			}
		}

		return new ArrayList<MeasurementSet>(measurementSets.values());
	}
}
