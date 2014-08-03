package com.quantimodo.etl;

import com.quantimodo.etl.utils.TableUtil;

import java.util.List;

// A simple record store. Stores all records directly in RAM.
public final class ArrayTable implements Table
{
	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final Object[][] EMPTY_DATA_MATRIX = new Object[0][];

	private final String name;

	private String[] fieldTypes = null;
	private final String[] fieldNames;
	private final Object[][] records;

	private final int recordCount;
	private final int fieldCount;

	// Disable default constructor.
	public ArrayTable()
	{
		throw new UnsupportedOperationException("The default constructor is not valid.");
	}
	
	// Constructs ETLData from arrays.
	public ArrayTable(final String name, final String[] fieldNames, final Object[][] records)
	{
		this.name = name;

		if (fieldNames == null)
		{
			throw new NullPointerException("No field names were provided");
		}
		if (records == null)
		{
			throw new NullPointerException("No records were provided.");
		}

		recordCount = records.length;
		fieldCount = fieldNames.length;

		for (int i = 0; i < recordCount; i++)
		{
			if (records[i].length != fieldCount)
			{
				throw new IllegalArgumentException("Record " + i + " doesn't have " + fieldCount + " fields.");
			}
		}

		this.fieldNames = fieldNames;
		this.records = records;
	}
	
	public ArrayTable(final String name, final String[] fieldTypes, final String[] fieldNames, final Object[][] records) {
		
		this(name, fieldNames, records);
		this.fieldTypes = fieldTypes;		
	}

	// Constructs ETLData from java.util.Lists.
	public ArrayTable(final String name, final List<String> fieldNames, final List<? extends List<? extends Object>> records)
	{
		this(name, fieldNames.toArray(EMPTY_STRING_ARRAY), toMatrix(records));
	}

	private static final Object[][] toMatrix(List<? extends List<? extends Object>> records)
	{
		final int recordCount = records.size();
		final Object[][] result = new Object[recordCount][];
		for (int i = 0; i < recordCount; i++)
		{
			result[i] = records.get(i).toArray();
		}
		return result;
	}

	public static ArrayTable makeEmptyTable(final String name)
	{
		return new ArrayTable(name, EMPTY_STRING_ARRAY, EMPTY_DATA_MATRIX);
	}

	// Returns the table name.
	public String getName()
	{
		return name;
	}

	// Returns the number of records.
	public int getRecordCount()
	{
		return recordCount;
	}

	// Returns the number of fields.
	public int getFieldCount()
	{
		return fieldCount;
	}

	// Returns the field number with a given name (or -1 if no field has that name).
	public int getFieldNumber(CharSequence fieldName)
	{
		fieldName = fieldName.toString();
		for (int field = 0; field < fieldCount; field++)
		{
			if (fieldName.equals(fieldNames[field]))
			{
				return field;
			}
		}
		return -1;
	}
	
	//Returns the type of the given field number.
	public String getFieldType(final int field) {
		
		if(fieldTypes == null)
			throw new ArrayIndexOutOfBoundsException("Field type of the number " + field + " is null.");
		
		if((field < 0 || field >= fieldCount))
			throw new ArrayIndexOutOfBoundsException("Field number " + field + " doesn't exist.");
		
		return fieldTypes[field];
	}

	// Returns the name of the given field number.
	public String getFieldName(final int field)
	{
		if ((field < 0) || (field >= fieldCount))
		{
			throw new ArrayIndexOutOfBoundsException("Field number " + field + " doesn't exist.");
		}
		return fieldNames[field];
	}

	// Returns whether a field is in the database.
	public boolean hasField(CharSequence fieldName)
	{
		fieldName = fieldName.toString();
		for (int field = 0; field < fieldCount; field++)
		{
			if (fieldName.equals(fieldNames[field]))
			{
				return true;
			}
		}
		return false;
	}

	// Returns whether a field is in the database.
	public boolean hasField(int fieldNumber)
	{
		return (fieldNumber < fieldCount) && (fieldNumber >= 0);
	}

	// Returns the cell at the specified record and field.
	public Object getData(final int record, final int field)
	{
		if ((record < 0) || (record >= recordCount))
		{
			throw new ArrayIndexOutOfBoundsException("Record number " + record + " doesn't exist.");
		}
		if ((field < 0) || (field >= fieldCount))
		{
			throw new ArrayIndexOutOfBoundsException("Field number " + field + " doesn't exist.");
		}
		return records[record][field];
	}

	// Returns the cell at the specified record and field.
	public Object getData(final int record, final CharSequence fieldName)
	{
		final int field = getFieldNumber(fieldName);
		if (field == -1)
		{
			throw new ArrayIndexOutOfBoundsException("There is no field named " + fieldName + '.');
		}
		return getData(record, field);
	}

	public String toString()
	{
		return TableUtil.appendInfo(new StringBuilder(), this).toString();
	}

	public String toHTML()
	{
		return "Not supported on Android";
		// return TableUtil.appendInfoHTML(new StringBuilder(), this).toString();
	}
}
