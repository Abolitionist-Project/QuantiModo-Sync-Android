package com.quantimodo.etl;

public interface DatabaseView
{
	int getTableCount();

	int getTableNumber(CharSequence tableName);

	String getTableName(int table);

	boolean hasTable(CharSequence tableName);

	boolean hasTable(int tableName);

	Table getTable(int table);

	Table getTable(CharSequence tableName);

	String toString();

	String toHTML();
}
