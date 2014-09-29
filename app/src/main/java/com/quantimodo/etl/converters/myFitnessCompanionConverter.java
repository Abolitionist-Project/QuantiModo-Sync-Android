package com.quantimodo.etl.converters;

import com.quantimodo.android.sdk.model.Measurement;
import com.quantimodo.android.sdk.model.MeasurementSet;
import com.quantimodo.etl.DatabaseView;
import com.quantimodo.etl.Table;

import java.util.ArrayList;
import java.util.GregorianCalendar;

public class myFitnessCompanionConverter implements Converter {
    public static final myFitnessCompanionConverter instance = new myFitnessCompanionConverter();

    private myFitnessCompanionConverter() {
    }

    public ArrayList<MeasurementSet> convert(final DatabaseView databaseView) {
        if ((databaseView == null) || (!databaseView.hasTable("table_sensors")) || (!databaseView.hasTable("table_readings"))) {
            return null;
        }

        //~ final Map<Long, String> devices = new HashMap<Long, String>(); {
        //~ final Table scaleTable = databaseView.getTable("table_sensors");
        //~ if ((!scaleTable.hasField("person_id")) || (!scaleTable.hasField("sensor_idmin_label")) || (!scaleTable.hasField("max_label"))) return null;

        //~ final int recordCount = scaleTable.getRecordCount();
        //~ for (int recordNumber = 0; recordNumber < recordCount; recordNumber++) {
        //~ final long id = ((Number) scaleTable.getData(recordNumber, "_id")).longValue();
        //~ scaleNames.put(id, String.format("%s/%s", (String) scaleTable.getData(recordNumber, "min_label"), (String) scaleTable.getData(recordNumber, "max_label")));
        //~ }
        //~ }

        final Table table = databaseView.getTable("table_readings");
        if ((!table.hasField("reading2")) || (!table.hasField("year")) || (!table.hasField("month")) || (!table.hasField("day")) || (!table.hasField("hour")) || (!table.hasField("minutes")) || (!table.hasField("seconds"))) {
            return null;
        }

        final int recordCount = table.getRecordCount();
        final ArrayList<Measurement> measurements = new ArrayList<Measurement>(recordCount);
        for (int recordNumber = 0; recordNumber < recordCount; recordNumber++) {
            final double value = ((Number) table.getData(recordNumber, "reading2")).doubleValue();
            final int year = ((Number) table.getData(recordNumber, "year")).intValue();
            final int month = ((Number) table.getData(recordNumber, "month")).intValue();
            final int day = ((Number) table.getData(recordNumber, "day")).intValue();
            final int hour = ((Number) table.getData(recordNumber, "hour")).intValue();
            final int minute = ((Number) table.getData(recordNumber, "minutes")).intValue();
            final int second = ((Number) table.getData(recordNumber, "seconds")).intValue();

            final long timestamp = (new GregorianCalendar(year, month - 1, day, hour, minute, second)).getTimeInMillis() / 1000;

            measurements.add(new Measurement(timestamp, value));
        }

        ArrayList<MeasurementSet> measurementSets = new ArrayList<MeasurementSet>(1);
        measurementSets.add(new MeasurementSet("Heart Rate", null, "Vital Signs", "bpm", MeasurementSet.COMBINE_MEAN, "myFitnessCompanion"));
        return measurementSets;
    }
}
