package com.quantimodo.sync.etl.readers;

import com.quantimodo.sync.etl.ArrayDatabaseView;
import com.quantimodo.sync.etl.ArrayTable;
import com.quantimodo.sync.etl.DatabaseView;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CSVReader implements Reader
{
	public static final CSVReader instance = new CSVReader();

	// Disable default constructor.
	private CSVReader()
	{
	}

	private static Object parse(String data)
	{
		data = data.trim();

		if (data.length() == 0)
		{
			return null;
		}
		if (data.equalsIgnoreCase("null"))
		{
			return null;
		}
		try
		{
			return Integer.valueOf(data);
		}
		catch (final NumberFormatException e)
		{
		}
		try
		{
			return Long.valueOf(data);
		}
		catch (final NumberFormatException e)
		{
		}
		try
		{
			return new BigInteger(data);
		}
		catch (final NumberFormatException e)
		{
		}
		try
		{
			return Double.valueOf(data);
		}
		catch (final NumberFormatException e)
		{
		}

		final SimpleDateFormat dateParser = new SimpleDateFormat();
		Date result;

		try
		{
			dateParser.applyPattern("yyyyMMdd");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("dd-MM-yyyy");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("yyyy-MM-dd");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("MM/dd/yyyy");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("yyyy/MM/dd");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("dd MMM yyyy");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("dd MMMM yyyy");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("yyyyMMddHHmm");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("yyyyMMdd HHmm");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("dd-MM-yyyy HH:mm");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("yyyy-MM-dd HH:mm");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("MM/dd/yyyy HH:mm");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("yyyy/MM/dd HH:mm");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("dd MMM yyyy HH:mm");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("dd MMMM yyyy HH:mm");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("yyyyMMddHHmmss");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("yyyyMMdd HHmmss");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("dd-MM-yyyy HH:mm:ss");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("yyyy-MM-dd HH:mm:ss");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("MM/dd/yyyy HH:mm:ss");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("yyyy/MM/dd HH:mm:ss");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("dd MMM yyyy HH:mm:ss");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("dd MMMM yyyy HH:mm:ss");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		try
		{
			dateParser.applyPattern("MMM dd, yyyy  HH:mm aa");
			return dateParser.parse(data);
		}
		catch (final ParseException e)
		{
		}
		return data;
	}

	public DatabaseView getDatabaseView(final CharSequence filename) throws IOException
	{
		return getDatabaseView(new File(filename.toString()));
	}

	public DatabaseView getDatabaseView(final File file) throws IOException
	{
		final String tableName = file.getName();

		final List<String[]> stringRows;
		{
			final au.com.bytecode.opencsv.CSVReader reader = new au.com.bytecode.opencsv.CSVReader(new FileReader(file));
			stringRows = reader.readAll();
			reader.close();
		}

		final int fieldCount;
		{
			int count = 0;
			for (final String[] row : stringRows)
			{
				count = Math.max(count, row.length);
			}
			fieldCount = count;
		}

		final boolean hasHeaderRow;
		{
			final String[] topRow = stringRows.get(0);
			if (topRow.length != fieldCount)
			{
				hasHeaderRow = false;
			}
			else
			{
				boolean hasHeader = true;
				for (final String cell : topRow)
				{
					if (!(parse(cell) instanceof String))
					{
						hasHeader = false;
						break;
					}
				}
				hasHeaderRow = hasHeader;
			}
		}

		final int recordCount;
		final String[] fieldNames = new String[fieldCount];
		final Object[][] records;
		if (hasHeaderRow)
		{
			recordCount = stringRows.size() - 1;
			final String[] topRow = stringRows.get(0);
			for (int fieldNumber = 0; fieldNumber < fieldCount; fieldNumber++)
			{
				fieldNames[fieldNumber] = (String) parse(topRow[fieldNumber]);
			}
			records = new Object[recordCount][fieldCount];
			for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
			{
				final String[] inputRecord = stringRows.get(recordNumber + 1);
				final Object[] outputRecord = records[recordNumber];
				for (int fieldNumber = 0; fieldNumber < fieldCount; fieldNumber++)
				{
					outputRecord[fieldNumber] = (fieldNumber >= inputRecord.length) ? null : parse(inputRecord[fieldNumber]);
				}
			}
		}
		else
		{
			recordCount = stringRows.size();
			for (int fieldNumber = 0; fieldNumber < fieldCount; fieldNumber++)
			{
				fieldNames[fieldNumber] = "";
			}
			records = new Object[recordCount][fieldCount];
			for (int recordNumber = 0; recordNumber < recordCount; recordNumber++)
			{
				final String[] inputRecord = stringRows.get(recordNumber);
				final Object[] outputRecord = records[recordNumber];
				for (int fieldNumber = 0; fieldNumber < inputRecord.length; fieldNumber++)
				{
					outputRecord[fieldNumber] = parse(inputRecord[fieldNumber]);
				}
			}
		}

		return new ArrayDatabaseView(new String[]{tableName}, new ArrayTable[]{new ArrayTable(tableName, fieldNames, records)});
	}
}