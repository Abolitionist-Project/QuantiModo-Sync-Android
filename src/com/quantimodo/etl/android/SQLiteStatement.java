package com.quantimodo.etl.android;

import android.database.Cursor;

public class SQLiteStatement
{
	private Cursor cursor;

	public SQLiteStatement(Cursor cursor)
	{
		this.cursor = cursor;
	}

	public boolean step()
	{
		return cursor.moveToNext();
	}

	public String columnString(int column)
	{
		return cursor.getString(column);
	}

	public int columnCount()
	{
		return cursor.getColumnCount();
	}

	public String getColumnName(int column)
	{
		return cursor.getColumnName(column);
	}

	public Object columnValue(int column)
	{
		int type = cursor.getType(column);
		switch (type)
		{
		case Cursor.FIELD_TYPE_BLOB:
			return cursor.getBlob(column);
		case Cursor.FIELD_TYPE_FLOAT:
			return cursor.getFloat(column);
		case Cursor.FIELD_TYPE_INTEGER:
			return cursor.getLong(column);
		case Cursor.FIELD_TYPE_STRING:
			return cursor.getString(column);
		default:
			return null;
		}
	}

	public void dispose()
	{
		cursor.close();
	}
}
