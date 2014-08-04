package com.quantimodo.sync.etl;

import com.quantimodo.android.sdk.model.MeasurementSet;
import com.quantimodo.sync.etl.converters.AccupedoConverter;
import com.quantimodo.sync.etl.converters.CallRecorderConverter;
import com.quantimodo.sync.etl.converters.CardiographConverter;
import com.quantimodo.sync.etl.converters.Converter;
import com.quantimodo.sync.etl.converters.DataHabitConverter;
import com.quantimodo.sync.etl.converters.GoodDayJournalConverter;
import com.quantimodo.sync.etl.converters.HowAreYouFeelingConverter;
import com.quantimodo.sync.etl.converters.MedHelperConverter;
import com.quantimodo.sync.etl.converters.MediSafeConverter;
import com.quantimodo.sync.etl.converters.MyFitnessPalConverter;
import com.quantimodo.sync.etl.converters.SleepAsAndroidConverter;
import com.quantimodo.sync.etl.converters.SportsTrackerConverter;
import com.quantimodo.sync.etl.converters.T2MoodTrackerConverter;
import com.quantimodo.sync.etl.converters.ZombiesRunConverter;
import com.quantimodo.sync.etl.converters.myFitnessCompanionConverter;
import com.quantimodo.sync.etl.readers.CSVReader;
import com.quantimodo.sync.etl.readers.Reader;
import com.quantimodo.sync.etl.readers.SQLiteReader;
import com.quantimodo.sync.etl.readers.SpreadsheetReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
			CallRecorderConverter.instance,
			MyFitnessPalConverter.instance
	};

	public ArrayList<MeasurementSet> handle(final String filename) throws IOException
	{
		return handle(new File(filename), false);
	}

	public ArrayList<MeasurementSet> handle(final String filename, final boolean debugPrinting) throws IOException
	{
		return handle(new File(filename), debugPrinting);
	}

	public ArrayList<MeasurementSet> handle(final File file) throws IOException
	{
		return handle(file, false);
	}

	public ArrayList<MeasurementSet> handle(final File file, final boolean debugPrinting) throws IOException
	{
		if (file == null)
		{
			throw new NullPointerException("ETL.handle was given null in place of a File");
		}

		// Get original data
		final DatabaseView database;
		{
			DatabaseView db = null;
			for (Reader reader : readers)
			{
				try
				{
					db = reader.getDatabaseView(file);
					if (db != null)
					{
						break;
					}
				}
				catch (final IOException e)
				{
					if (reader == SQLiteReader.instance)
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
		final ArrayList<MeasurementSet> outputRecords;
		{
			ArrayList<MeasurementSet> recs = null;
			for (Converter converter : converters)
			{
				recs = converter.convert(database);
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
			for (final MeasurementSet outputRecord : outputRecords)
			{
				System.out.println(outputRecord);
			}
		}

		return outputRecords;
	}
}

