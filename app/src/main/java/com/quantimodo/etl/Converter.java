package com.quantimodo.etl;

import com.quantimodo.android.sdk.model.MeasurementSet;

import java.util.ArrayList;

public interface Converter
{
	ArrayList<MeasurementSet> convert(DatabaseView databaseView);
}

