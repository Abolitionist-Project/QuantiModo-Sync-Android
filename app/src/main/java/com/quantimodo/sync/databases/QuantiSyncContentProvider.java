package com.quantimodo.sync.databases;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class QuantiSyncContentProvider extends ContentProvider
{
	private static final String AUTHORITY = "com.quantimodo.sync.provider";

	public static final String HISTORY_PATH = "history";
	public static final int HISTORY_CODE = 1;

	public static Uri CONTENT_URI_HISTORY;

	private UriMatcher uriMatcher;
	private QuantiSyncDbHelper dbHelper;

	@Override
	public boolean onCreate()
	{
		dbHelper = new QuantiSyncDbHelper(getContext());

		// Note: forces db creation
		dbHelper.getWritableDatabase();

		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, HISTORY_PATH, HISTORY_CODE);

		CONTENT_URI_HISTORY = Uri.parse("content://" + AUTHORITY + "/" + HISTORY_PATH);

		return true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int result = 0;

		int code = uriMatcher.match(uri);
		switch (code)
		{
		case HISTORY_CODE:
			result = db.delete(QuantiSyncDbHelper.History.TABLE_NAME, selection, selectionArgs);
			break;
		}
		return result;
	}

	@Override
	public String getType(Uri uri)
	{
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		Uri result = null;

		int code = uriMatcher.match(uri);
		switch (code)
		{
		case HISTORY_CODE:
			long historyId = db.insert(QuantiSyncDbHelper.History.TABLE_NAME, null, values);
			break;
		}
		return result;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
	{
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		Cursor result;

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		int code = uriMatcher.match(uri);

		switch (code)
		{
		case HISTORY_CODE:
			qb.setTables(QuantiSyncDbHelper.History.TABLE_NAME);
			break;
		}

		result = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

		if (result != null)
		{
			result.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int result = 0;

		int code = uriMatcher.match(uri);
		switch (code)
		{
		case HISTORY_CODE:
			result = db.update(QuantiSyncDbHelper.History.TABLE_NAME, values, selection, selectionArgs);
			break;
		}
		if(result > 0) {
			 getContext().getContentResolver().notifyChange(uri, null);
		}
		return result;
	}

}
