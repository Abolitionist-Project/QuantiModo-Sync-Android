package com.quantimodo.etl;

import com.quantimodo.etl.utils.DatabaseViewUtil;

public class ArrayDatabaseView implements DatabaseView {
    private final int tableCount;
    private final String[] tableNames;
    private final ArrayTable[] tables;

    // Disable default constructor.
    private ArrayDatabaseView() {
        throw new UnsupportedOperationException("The default constructor is not valid.");
    }

    // Constructs ETLData from arrays.
    public ArrayDatabaseView(final String[] tableNames, final ArrayTable[] tables) {
        if (tableNames == null) {
            throw new NullPointerException("No table names were provided");
        }
        if (tables == null) {
            throw new NullPointerException("No tables were provided.");
        }

        tableCount = tables.length;

        this.tableNames = tableNames;
        this.tables = tables;
    }

    public int getTableCount() {
        return tableCount;
    }

    // Returns the table number with a given name (or -1 if no table has that name).
    public int getTableNumber(CharSequence tableName) {
        tableName = tableName.toString();
        for (int table = 0; table < tableCount; table++) {
            if (tableName.equals(tableNames[table])) {
                return table;
            }
        }
        return -1;
    }

    // Returns the name of the given table number.
    public String getTableName(final int table) {
        if ((table < 0) || (table >= tableCount)) {
            throw new ArrayIndexOutOfBoundsException("Table number " + table + " doesn't exist.");
        }
        return tableNames[table];
    }

    // Returns whether a table is in the database.
    public Table getTable(final int table) {
        if ((table < 0) || (table >= tableCount)) {
            throw new ArrayIndexOutOfBoundsException("Table number " + table + " doesn't exist.");
        }
        return tables[table];
    }

    public Table getTable(final CharSequence tableName) {
        final int table = getTableNumber(tableName);
        if (table == -1) {
            throw new ArrayIndexOutOfBoundsException("There is no table named " + tableName + ".");
        }
        return getTable(table);
    }

    public void setTable(Table table, CharSequence tableName) {

    }

    // Returns whether a table is in the database.
    public boolean hasTable(CharSequence tableName) {
        tableName = tableName.toString();
        for (int table = 0; table < tableCount; table++) {
            if (tableName.equals(tableNames[table])) {
                return true;
            }
        }
        return false;
    }

    // Returns whether a table is in the database.
    public boolean hasTable(int tableNumber) {
        return (tableNumber < tableCount) && (tableNumber >= 0);
    }

    public String toString() {
        return DatabaseViewUtil.appendInfo(new StringBuilder(), this).toString();
    }

    public String toHTML() {
        return "Not supported on Android";
        //return DatabaseViewUtil.appendInfoHTML(new StringBuilder(), this).toString();
    }
}