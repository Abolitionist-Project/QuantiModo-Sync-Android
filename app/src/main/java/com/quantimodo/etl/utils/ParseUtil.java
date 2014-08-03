package com.quantimodo.etl.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class ParseUtil
{
	private static final SimpleDateFormat nanoTimeParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	// Disable default constructor.
	private ParseUtil()
	{
		throw new UnsupportedOperationException("Utility classes cannot be constructed.");
	}

	public static final Long parseNanoTime(final String nanoTime)
	{
		final int splitPosition = nanoTime.length() - 3;
		Date date = null;
		try
		{
			date = nanoTimeParser.parse(nanoTime.substring(0, splitPosition));
		}
		catch (ParseException e)
		{
			return null;
		}
		if (date == null)
		{
			return null;
		}
		return date.getTime() + ((nanoTime.charAt(splitPosition) >= '5') ? 1 : 0);
	}
}
