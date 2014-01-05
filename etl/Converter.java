package com.quantimodo.etl;

import com.quantimodo.sdk.model.QuantimodoMeasurement;

public interface Converter
{
	QuantimodoMeasurement[] convert(DatabaseView databaseView);
}

