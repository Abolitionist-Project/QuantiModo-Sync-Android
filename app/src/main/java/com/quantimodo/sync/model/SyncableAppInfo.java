package com.quantimodo.sync.model;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;

import com.quantimodo.android.sdk.Quantimodo;
import com.quantimodo.sync.Global;
import com.quantimodo.sync.Log;
import com.quantimodo.sync.R;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Holds static application data supplied by converters.
 */
public class SyncableAppInfo
{
	public String label;                          // Label the user sees in his app drawer
	public String packageName;                    // Package name to identify individual apps
	public File dataFile;                         // Location of this app's useful data
	public boolean rootRequired;                  // True if root is required to export data

	public SyncableAppInfo(String label, String packageName, File dataFile, boolean rootRequired)
	{
		this.label = label;
		this.packageName = packageName;
		this.dataFile = dataFile;
		this.rootRequired = rootRequired;
	}
}
