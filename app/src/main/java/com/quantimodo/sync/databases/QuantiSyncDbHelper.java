package com.quantimodo.sync.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Arrays;

public class QuantiSyncDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "quantisync.sqlite";
    private static final int DB_VERSION = 1;

    public QuantiSyncDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(History.create());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public static final class History {
        public static final String TABLE_NAME = "history";

        public static final String ID = "_id";
        public static final String PACKAGENAME = "package";
        public static final String PACKAGELABEL = "label";
        public static final String TIMESTAMP = "timestamp";
        public static final String SYNCCOUNT = "synccount";
        public static final String SYNCERROR = "syncerror";

        public static final String[] COLUMNS = new String[]{ID, PACKAGENAME, PACKAGELABEL, TIMESTAMP, SYNCCOUNT, SYNCERROR};

        public static String create() {
            String create = getCreateStatement(TABLE_NAME, ID, Arrays.copyOfRange(COLUMNS, 1, COLUMNS.length));
            return create;
        }
    }

    private static String getCreateStatement(String tableName, String idColumn, String[] columns) {
        String create = "CREATE TABLE " + tableName + "(";
        if (idColumn != null) {
            create += idColumn + " INTEGER PRIMARY KEY AUTOINCREMENT, ";
        }
        return create += combine(columns, ",") + ")";
    }

    private static String combine(String[] s, String glue) {
        int k = s.length;
        if (k == 0)
            return null;
        StringBuilder out = new StringBuilder();
        out.append(s[0]);
        for (int x = 1; x < k; ++x)
            out.append(glue).append(s[x]);
        return out.toString();
    }
}
