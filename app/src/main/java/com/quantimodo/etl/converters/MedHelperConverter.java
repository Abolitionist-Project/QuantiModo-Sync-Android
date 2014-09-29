package com.quantimodo.etl.converters;

import com.quantimodo.android.sdk.model.Measurement;
import com.quantimodo.android.sdk.model.MeasurementSet;
import com.quantimodo.etl.DatabaseView;
import com.quantimodo.etl.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MedHelperConverter implements Converter {
    public static final MedHelperConverter instance = new MedHelperConverter();

    private static final String[] unitTypes = new String[]{"tablets", "mg", "mL", "drops", "applications", "mcg", "units", "puffs", "sprays", "capsules", "g"};

    private MedHelperConverter() {
    }

    public ArrayList<MeasurementSet> convert(final DatabaseView databaseView) {
        if ((databaseView == null) || (!databaseView.hasTable("prescription")) || (!databaseView.hasTable("doselog"))) {
            return null;
        }

        final Map<Long, String> prescriptionNames = new HashMap<Long, String>();
        final Map<Long, String> prescriptionUnits = new HashMap<Long, String>();
        {
            final Table prescriptionTable = databaseView.getTable("prescription");
            if ((!prescriptionTable.hasField("_id")) || (!prescriptionTable.hasField("name")) || (!prescriptionTable.hasField("inventorytype"))) {
                return null;
            }

            final int recordCount = prescriptionTable.getRecordCount();
            for (int recordNumber = 0; recordNumber < recordCount; recordNumber++) {
                final long id = ((Number) prescriptionTable.getData(recordNumber, "_id")).longValue();
                prescriptionNames.put(id, (String) prescriptionTable.getData(recordNumber, "name"));
                prescriptionUnits.put(id, unitTypes[((Number) prescriptionTable.getData(recordNumber, "inventorytype")).intValue()]);
            }
        }

        final Table table = databaseView.getTable("doselog");
        if ((!table.hasField("prescriptionid")) || (!table.hasField("actualtime")) || (!table.hasField("actualdosage"))) {
            return null;
        }

        final int recordCount = table.getRecordCount();
        final HashMap<String, MeasurementSet> measurementSets = new HashMap<String, MeasurementSet>();
        for (int recordNumber = 0; recordNumber < recordCount; recordNumber++) {
            final long prescriptionID = ((Number) table.getData(recordNumber, "prescriptionid")).longValue();

            String name = prescriptionNames.get(prescriptionID);
            double dose = ((Number) table.getData(recordNumber, "actualdosage")).doubleValue();
            String unit = prescriptionUnits.get(prescriptionID);
            long timestamp = ((Number) table.getData(recordNumber, "actualtime")).longValue() / 1000;

            if (!measurementSets.containsKey(name + unit)) {
                MeasurementSet newSet = new MeasurementSet(name, null, "Medications", unit, MeasurementSet.COMBINE_MEAN, "Med Helper");
                newSet.measurements.add(new Measurement(timestamp, dose));
                measurementSets.put(name + unit, newSet);
            } else {
                measurementSets.get(name + unit).measurements.add(new Measurement(timestamp, dose));
            }
        }

        return new ArrayList<MeasurementSet>(measurementSets.values());
    }
}
