package com.quantimodo.etl.readers;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.quantimodo.etl.ArrayDatabaseView;
import com.quantimodo.etl.ArrayTable;
import com.quantimodo.etl.DatabaseView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SQLiteReader implements Reader {
    public static final SQLiteReader instance = new SQLiteReader();

    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final Object[][] EMPTY_DATA_MATRIX = new Object[0][];

    // Disable default constructor.
    private SQLiteReader() {
    }

    public DatabaseView getDatabaseView(final CharSequence filename) throws IOException {
        return getDatabaseView(new File(filename.toString()));
    }

    public DatabaseView getDatabaseView(final File file) throws IOException {

        final SQLiteConnection database = new SQLiteConnection(file);
        SQLiteStatement statement = null;
        try {
            database.openReadonly();

            final String[] tableNames;
            statement = database.prepare("SELECT name FROM sqlite_master WHERE type='table'");
            {
                final List<String> tableNameList = new ArrayList<String>();
                while (statement.step()) {
                    tableNameList.add(statement.columnString(0));
                }
                statement.dispose();
                tableNames = tableNameList.toArray(EMPTY_STRING_ARRAY);
            }

            final ArrayTable[] tables = new ArrayTable[tableNames.length];
            for (int tableNumber = 0; tableNumber < tableNames.length; tableNumber++) {
                final String tableName = tableNames[tableNumber];
                statement = database.prepare("SELECT * FROM [" + tableName + "]");

                final int columnCount = statement.columnCount();
                final String[] columnNames = new String[columnCount];
                for (int columnNumber = 0; columnNumber < columnCount; columnNumber++) {
                    columnNames[columnNumber] = statement.getColumnName(columnNumber);
                }

                final Object[][] tableData;
                {
                    final List<Object[]> tableDataList = new ArrayList<Object[]>();
                    while (statement.step()) {
                        final Object[] rowData = new Object[columnCount];
                        for (int columnNumber = 0; columnNumber < columnCount; columnNumber++) {
                            rowData[columnNumber] = statement.columnValue(columnNumber);
                        }
                        tableDataList.add(rowData);
                    }
                    tableData = tableDataList.toArray(EMPTY_DATA_MATRIX);
                }
                tables[tableNumber] = new ArrayTable(tableName, columnNames, tableData);

                statement.dispose();
            }

            return new ArrayDatabaseView(tableNames, tables);
        } catch (final SQLiteException e) {
            throw new IOException(e);
        } finally {
            if (statement != null) {
                statement.dispose();
            }
            database.dispose();
        }
    }

    private class SQLiteConnection {
        File databaseFile;
        String databasePath;
        SQLiteDatabase database;

        public SQLiteConnection(File databaseFile) {
            this.databaseFile = databaseFile;
        }

        public SQLiteConnection(String filePath) {

            this.databasePath = filePath;
        }

        public void openReadonly() {
            database = SQLiteDatabase.openDatabase(databaseFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
        }

        public SQLiteStatement prepare(String query) {
            return new SQLiteStatement(database.rawQuery(query, null));
        }

        public void dispose() {
            database.close();
        }
    }

    private class SQLiteStatement {
        private Cursor cursor;

        public SQLiteStatement(Cursor cursor) {
            this.cursor = cursor;
        }

        public boolean step() {
            return cursor.moveToNext();
        }

        public String columnString(int column) {
            return cursor.getString(column);
        }

        public int columnCount() {
            return cursor.getColumnCount();
        }

        public String getColumnName(int column) {
            return cursor.getColumnName(column);
        }

        public Object columnValue(int column) {
            int type = cursor.getType(column);
            switch (type) {
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

        public void dispose() {
            cursor.close();
        }
    }
}