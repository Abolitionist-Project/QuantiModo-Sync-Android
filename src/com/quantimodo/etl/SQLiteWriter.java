package com.quantimodo.etl;

import java.io.File;

import com.quantimodo.etl.android.SQLiteConnection;
import com.quantimodo.sync.Log;

public class SQLiteWriter {
	
	public static final SQLiteWriter instance = new SQLiteWriter();
	
	public SQLiteWriter() {
		
	}
	
	public void setDatabaseView(final String filepath, DatabaseView databaseview) {
		
		SQLiteConnection database = null;
		
		File cacheFile = new File(filepath);
		
		if (cacheFile.exists()) {
			Log.i("loading the old history file...");
			
			database = new SQLiteConnection(cacheFile);
			database.openWrite();
			
		}
		else {
			Log.i("creating the new history file...");
			
			database = new SQLiteConnection(filepath);
			database.create();
			
			//create tables
			for( int tablenumber = 0; tablenumber < databaseview.getTableCount(); tablenumber++ ) {
				
				String query = "CREATE TABLE IF NOT EXISTS " + databaseview.getTable(tablenumber).getName();
				
				for( int columnnumber = 0; columnnumber < databaseview.getTable(tablenumber).getFieldCount(); columnnumber++) {
					
					String part = "";
					
					if(columnnumber == 0)
						part = " (" + databaseview.getTable(tablenumber).getFieldName(columnnumber) + 
							   " "  + databaseview.getTable(tablenumber).getFieldType(columnnumber) + 
							   ",";
					else if(columnnumber == databaseview.getTable(tablenumber).getFieldCount() - 1)
						part = " " + databaseview.getTable(tablenumber).getFieldName(columnnumber) + 
							   " " + databaseview.getTable(tablenumber).getFieldType(columnnumber) + 
							   ");";
					else
						part = " " + databaseview.getTable(tablenumber).getFieldName(columnnumber) + 
						   	   " " + databaseview.getTable(tablenumber).getFieldType(columnnumber) + 
						   	   ",";
					
					query += part;
				}
				
				database.exec(query);
			}
		}
		
		//insert values
		for( int tablenumber = 0; tablenumber < databaseview.getTableCount(); tablenumber++ ) {			
			
			for( int rawnumber = 0; rawnumber < databaseview.getTable(tablenumber).getRecordCount(); rawnumber++) {				
				
				String query = "INSERT INTO " + databaseview.getTable(tablenumber).getName() + " Values";
				
				for( int columnnumber = 0; columnnumber < databaseview.getTable(tablenumber).getFieldCount(); columnnumber++) {
					
					String part = "";
					
					String subpart = "" + databaseview.getTable(tablenumber).getData(rawnumber, columnnumber);					
					if(databaseview.getTable(tablenumber).getFieldType(columnnumber) == "VARCHAR")
						subpart = "'" + databaseview.getTable(tablenumber).getData(rawnumber, columnnumber) + "'";
					
					if(columnnumber == 0)
						part = " (" + subpart + ",";
					else if(columnnumber == databaseview.getTable(tablenumber).getFieldCount() - 1)
						part = " " + subpart + ");";
					else
						part = " " + subpart + ",";
					
					query += part;
				}
				
				database.exec(query);				
			}
		}
		
		database.dispose();
	}
}