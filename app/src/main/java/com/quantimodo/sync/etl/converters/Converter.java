package com.quantimodo.sync.etl.converters;

import com.quantimodo.android.sdk.model.MeasurementSet;
import com.quantimodo.sync.etl.DatabaseView;
import com.quantimodo.sync.model.SyncableAppInfo;

import java.util.ArrayList;

public interface Converter
{
	ArrayList<MeasurementSet> convert(DatabaseView databaseView);

    SyncableAppInfo getAppInfo();
}

