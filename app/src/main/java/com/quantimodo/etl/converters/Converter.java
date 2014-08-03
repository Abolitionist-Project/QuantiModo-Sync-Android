package com.quantimodo.etl.converters;

import com.quantimodo.android.sdk.model.MeasurementSet;
import com.quantimodo.etl.DatabaseView;

import java.util.ArrayList;

public interface Converter
{
	ArrayList<MeasurementSet> convert(DatabaseView databaseView);
}

