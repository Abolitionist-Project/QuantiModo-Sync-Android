package com.quantimodo.etl.android;

import android.database.sqlite.SQLiteDatabase;

import java.io.File;

public class SQLiteConnection
{
	File databaseFile;
	String databasePath;
	SQLiteDatabase database;

	public SQLiteConnection(File databaseFile)
	{
		this.databaseFile = databaseFile;
	}
	
	public SQLiteConnection(String filePath) {
		
		this.databasePath = filePath;
	}

	public void create() {
		
		database = SQLiteDatabase.openOrCreateDatabase(databasePath, null);
	}
	
	public void openReadonly()
	{
		database = SQLiteDatabase.openDatabase(databaseFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
	}
	
	public void openWrite() {
		
		database = SQLiteDatabase.openDatabase(databaseFile.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
	}

	public SQLiteStatement prepare(String query)
	{
		return new SQLiteStatement(database.rawQuery(query, null));
	}
	
	public void exec(String query) {
		
		database.execSQL(query);
	}

	public void dispose()
	{
		database.close();
	}
}
