package com.quantimodo.etl;

import java.io.IOException;

public final class TableUtil
{
	// Disable default constructor.
	private TableUtil()
	{
		throw new UnsupportedOperationException("Utility classes cannot be constructed.");
	}

	// Returns the name of the class stored in this field or "(no data)" if there was no data or "(class varies)" if the class varies.
	public static final String getFieldClassName(final Table table, final CharSequence fieldName)
	{
		return getFieldClassName(table, table.getFieldNumber(fieldName));
	}

	// Returns the name of the class stored in this field or "(no data)" if there was no data or "(class varies)" if the class varies.
	public static final String getFieldClassName(final Table table, final int field)
	{
		if ((field < 0) || (field >= table.getFieldCount()))
		{
			throw new ArrayIndexOutOfBoundsException("Field number " + field + " doesn't exist.");
		}

		String fieldClass = null;
		final int recordCount = table.getRecordCount();
		for (int record = 0; record < recordCount; record++)
		{
			final Object data = table.getData(record, field);
			if (data != null)
			{
				String thisClass = data.getClass().getCanonicalName();
				if (thisClass.startsWith("java.lang."))
				{
					thisClass = thisClass.substring(10);
				}

				if (fieldClass == null)
				{
					fieldClass = thisClass;
				}
				else if (!fieldClass.equals(thisClass))
				{
					fieldClass = "class varies";
					break;
				}
			}
		}
		return (fieldClass == null) ? "no data" : fieldClass;
	}

	public static final StringBuilder appendInfo(final StringBuilder stringBuilder, final Table table)
	{
		try
		{
			appendInfo((Appendable) stringBuilder, table);
		}
		catch (final IOException e)
		{
		}
		return stringBuilder;
	}

	public static final StringBuffer appendInfo(final StringBuffer stringBuffer, final Table table)
	{
		try
		{
			appendInfo((Appendable) stringBuffer, table);
		}
		catch (final IOException e)
		{
		}
		return stringBuffer;
	}

	public static final Appendable appendInfo(final Appendable appendable, final Table table) throws IOException
	{
		final int fieldCount = table.getFieldCount();
		final int recordCount = table.getRecordCount();

		appendable.append("Table ").append(table.getName()).append("\nFields: ");
		if (recordCount == 0)
		{
			for (int field = 0; field < fieldCount; field++)
			{
				if (field != 0)
				{
					appendable.append(", ");
				}
				appendable.append(table.getFieldName(field));
			}
		}
		else
		{
			for (int field = 0; field < fieldCount; field++)
			{
				if (field != 0)
				{
					appendable.append(", ");
				}
				appendable.append(table.getFieldName(field)).append(" (").append(getFieldClassName(table, field)).append(')');
			}
		}
		appendable.append("\n----------------------------------------");
		for (int record = 0; record < recordCount; record++)
		{
			appendable.append("\nRecord ").append(Integer.toString(record)).append(": ");
			for (int field = 0; field < fieldCount; field++)
			{
				if (field != 0)
				{
					appendable.append(", ");
				}

				final Object data = table.getData(record, field);
				appendable.append(data == null ? "[NULL]" : data.toString());
			}
		}
		return appendable.append('\n');
	}
  
  /*public static final StringBuilder appendInfoHTML(final StringBuilder stringBuilder, final Table table) {
    try { appendInfoHTML((Appendable) stringBuilder, table); } catch (final IOException e) {}
    return stringBuilder;
  }
  
  public static final StringBuffer appendInfoHTML(final StringBuffer stringBuffer, final Table table) {
    try { appendInfoHTML((Appendable) stringBuffer, table); } catch (final IOException e) {}
    return stringBuffer;
  }
  
  public static final Appendable appendInfoHTML(final Appendable appendable, final Table table) throws IOException {
    final int fieldCount  = table.getFieldCount();
    final int recordCount = table.getRecordCount();
    
    appendable.append("<table border=\"1\"><caption>Table ").append(HTMLUtil.escapeHTML(table.getName())).
        append("</caption><thead><tr><th>Field names</th>");
    for (int field = 0; field < fieldCount; field++) {
      appendable.append("<th>").append(HTMLUtil.escapeHTML(table.getFieldName(field))).append("</th>");
    }
    appendable.append("</tr><tr><th>Field types</th>");
    for (int field = 0; field < fieldCount; field++) {
      appendable.append("<th>").append(HTMLUtil.escapeHTML(getFieldClassName(table, field))).append("</th>");
    }
    appendable.append("</tr></thead><tbody>");
    for (int record = 0; record < recordCount; record++) {
      appendable.append("<tr><th>Record ").append(Integer.toString(record)).append("</th>");
      for (int field = 0; field < fieldCount; field++) {
        final Object data = table.getData(record, field);
        appendable.append("<td>").append(data == null ? "<sub><i>[NULL]</i></sub>" : HTMLUtil.escapeHTML(data.toString())).append("</td>");
      }
      appendable.append("</tr>");
    }
    return appendable.append("</tbody></table>");
  }*/
}
