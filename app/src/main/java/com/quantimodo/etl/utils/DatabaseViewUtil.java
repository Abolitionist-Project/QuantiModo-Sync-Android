package com.quantimodo.etl.utils;

import com.quantimodo.etl.DatabaseView;
import com.quantimodo.etl.utils.TableUtil;

import java.io.IOException;

public final class DatabaseViewUtil
{
	// Disable default constructor.
	private DatabaseViewUtil()
	{
		throw new UnsupportedOperationException("Utility classes cannot be constructed.");
	}

	public static final StringBuilder appendInfo(final StringBuilder stringBuilder, final DatabaseView database)
	{
		try
		{
			appendInfo((Appendable) stringBuilder, database);
		}
		catch (final IOException e)
		{
		}
		return stringBuilder;
	}

	public static final StringBuffer appendInfo(final StringBuffer stringBuffer, final DatabaseView database)
	{
		try
		{
			appendInfo((Appendable) stringBuffer, database);
		}
		catch (final IOException e)
		{
		}
		return stringBuffer;
	}

	public static final Appendable appendInfo(final Appendable appendable, final DatabaseView database) throws IOException
	{
		final int tableCount = database.getTableCount();

		appendable.append("Database has ").append(Integer.toString(tableCount)).append(tableCount == 1 ? " table:\n" : " tables:\n");
		for (int table = 0; table < tableCount; table++)
		{
			appendable.append("========================================\n");
			TableUtil.appendInfo(appendable, database.getTable(table));
		}
		return appendable.append("========================================\n");
	}
  
  /*public static final StringBuilder appendInfoHTML(final StringBuilder stringBuilder, final DatabaseView database) {
    try { appendInfoHTML((Appendable) stringBuilder, database); } catch (final IOException e) {}
    return stringBuilder;
  }
  
  public static final StringBuffer appendInfoHTML(final StringBuffer stringBuffer, final DatabaseView database) {
    try { appendInfoHTML((Appendable) stringBuffer, database); } catch (final IOException e) {}
    return stringBuffer;
  }
  
    public static final Appendable appendInfoHTML(final Appendable appendable, final DatabaseView database) throws IOException {
    final int tableCount = database.getTableCount();
    
    appendable.append("<h2>Database has ").append(Integer.toString(tableCount)).append(tableCount == 1 ? " tables</h2>" : " tables</h2>");
    for (int table = 0; table < tableCount; table++) {
      appendable.append("<hr />");
      TableUtil.appendInfoHTML(appendable, database.getTable(table));
    }
    return appendable.append("<hr />");
  }*/
}
