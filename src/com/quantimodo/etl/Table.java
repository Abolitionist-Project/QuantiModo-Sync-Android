package com.quantimodo.etl;

public interface Table
{
	String getName();

	int getFieldCount();

	String getFieldType(int field);
	String getFieldName(int field);

	int getFieldNumber(CharSequence fieldName);

	boolean hasField(int fieldNumber);

	boolean hasField(CharSequence fieldName);

	int getRecordCount();

	Object getData(int record, int field);

	Object getData(int record, CharSequence fieldName);

	String toString();

	String toHTML();
}