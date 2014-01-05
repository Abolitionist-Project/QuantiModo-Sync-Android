package com.quantimodo.etl;

import com.quantimodo.sdk.model.QuantimodoMeasurement;

import java.io.File;
import java.io.IOException;

public class ETL
{
	// CSVReader must be last due to it not being able to discern CSV files properly
	public static final Reader[] readers = new Reader[]{SpreadsheetReader.instance, SQLiteReader.instance, CSVReader.instance};
	public static final Converter[] converters = new Converter[]{
			DataHabitConverter.instance, AccupedoConverter.instance, SportsTrackerConverter.instance, ZombiesRunConverter.instance,
			myFitnessCompanionConverter.instance, CardiographConverter.instance,
			MedHelperConverter.instance, MediSafeConverter.instance,
			SleepAsAndroidConverter.instance,
			GoodDayJournalConverter.instance, T2MoodTrackerConverter.instance, HowAreYouFeelingConverter.instance,
			CallRecorderConverter.instance
	};

	public QuantimodoMeasurement[] handle(final String filename) throws IOException
	{
		return handle(new File(filename), false);
	}

	public QuantimodoMeasurement[] handle(final String filename, final boolean debugPrinting) throws IOException
	{
		return handle(new File(filename), debugPrinting);
	}

	public QuantimodoMeasurement[] handle(final File file) throws IOException
	{
		return handle(file, false);
	}

	public QuantimodoMeasurement[] handle(final File file, final boolean debugPrinting) throws IOException
	{
		if (file == null)
		{
			throw new NullPointerException("ETL.handle was given null in place of a File");
		}

		// Get original data
		final DatabaseView database;
		{
			DatabaseView db = null;
			for (int i = 0; i < readers.length; i++)
			{
				try
				{
					db = readers[i].getDatabaseView(file);
					if (db != null)
					{
						break;
					}
				}
				catch (final IOException e)
				{
					if (readers[i] == SQLiteReader.instance)
					{
						throw e;
					}
				}
			}
			database = db;
		}
		if (database == null)
		{
			throw new IOException("No Reader can decipher this file format.");
		}
		if (debugPrinting)
		{
			System.out.println(database);
		}
		// System.out.println(java.util.Arrays.toString((byte []) database.getTable("records").getData(0, "recordData")));

		// Convert records
		final QuantimodoMeasurement[] outputRecords;
		{
			QuantimodoMeasurement[] recs = null;
			for (int i = 0; i < converters.length; i++)
			{
				recs = converters[i].convert(database);
				if (recs != null)
				{
					break;
				}
			}
			outputRecords = recs;
		}
		if (outputRecords == null)
		{
			throw new IOException("No Converter understands this data." + database.toHTML());
		}
		if (debugPrinting)
		{
			for (final QuantimodoMeasurement outputRecord : outputRecords)
			{
				System.out.println(outputRecord);
			}
		}

		return outputRecords;
	}
}

