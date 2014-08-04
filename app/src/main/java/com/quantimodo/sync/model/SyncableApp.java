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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SyncableApp implements Comparable<SyncableApp>
{
    // Static application data, supplied by converters
    public SyncableAppInfo appInfo;

	public boolean isInstalled;
    private boolean isSyncEnabled;
    public String syncStatus;
	public Drawable icon;

	public SyncableApp(SyncableAppInfo appInfo, boolean isInstalled, boolean isSyncEnabled)
	{
		this.appInfo = appInfo;
		this.isInstalled = isInstalled;
		this.isSyncEnabled = isSyncEnabled;
	}

	/*
	 * Enable or disable whether this app gets synced.
	 *
	 * @return True if this app is now synced, false if it's not
	 */
	public boolean setSyncEnabled(Context context, boolean isSyncEnabled)
	{
		try
		{
			SharedPreferences prefs = context.getSharedPreferences("com.quantimodo.sync_preferences", Context.MODE_MULTI_PROCESS);
			String currentSyncingPackages = prefs.getString("syncingPackages", "");

			String thisSyncingApp = this.appInfo.packageName + ",";
			Log.i("Before syncing: " + currentSyncingPackages + ":");
			if (isSyncEnabled)
			{
				if (!currentSyncingPackages.contains(thisSyncingApp))
				{
					if (currentSyncingPackages.length() == 0)
					{
						Log.i("Enabling tracking apps sync, new app selected");
						ContentResolver.setSyncAutomatically(Quantimodo.getAccount(context), "com.quantimodo.sync.content-appdata", true);
					}
					currentSyncingPackages = currentSyncingPackages.concat(thisSyncingApp);
				}
			}
			else
			{
				if (currentSyncingPackages.contains(thisSyncingApp))
				{
					currentSyncingPackages = currentSyncingPackages.replace(thisSyncingApp, "");
					if (currentSyncingPackages.length() == 0)
					{
						Log.i("Disabling tracking apps sync, no apps selected");
						ContentResolver.setSyncAutomatically(Quantimodo.getAccount(context), "com.quantimodo.sync.content-appdata", false);
					}
				}
			}
			Log.i("After syncing: " + currentSyncingPackages + ":");
			prefs.edit().putString("syncingPackages", currentSyncingPackages).commit();

			this.isSyncEnabled = isSyncEnabled;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Log.e("Error saving sync pref: " + e.getMessage());
		}

		return this.isSyncEnabled;
	}

	/*
	 * True if this app is being synced, false if it's not
	 */
	public boolean isSyncEnabled()
	{
		return isSyncEnabled;
	}

	/*
	 * Listener to accompany getCompatibleApplications
	 */
	public interface OnCompatibleApplicationsLoaded
	{
		public void onComplete();
	}

	/*
	 * Get all compatible applications from XML definition and load them to Global.applications
	 */
	public static void getCompatibleApplications(final Context context, final Handler handler, final OnCompatibleApplicationsLoaded listener)
	{
		Log.i("Getting compatible apps from XML");
		Runnable run = new Runnable()
		{
			@Override public void run()
			{
				String filesPath = context.getFilesDir().getPath();
				final String dataLocation = filesPath.replace("/" + context.getPackageName() + "/files", "");
				final String sdDataLocation = Environment.getExternalStorageDirectory() + "/Android/data";

				final SharedPreferences prefs = context.getSharedPreferences("com.quantimodo.sync_preferences", Context.MODE_MULTI_PROCESS);
				final String currentSyncingPackages = prefs.getString("syncingPackages", "");
				final List<SyncableApp> tempApps = new ArrayList<SyncableApp>();
				final PackageManager packageManager = context.getPackageManager();

				try
				{
					InputStream in =  context.getResources().openRawResource(R.raw.compatible_apps);    // Open resource containing compatible app data

					System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver"); // Init SAX parser
					SAXParserFactory parseFactory = SAXParserFactory.newInstance();
					SAXParser xmlParser = parseFactory.newSAXParser();

					XMLReader xmlIn = xmlParser.getXMLReader();                             // Init XMl reader to read from asset
					InputStreamReader xmlStream = new InputStreamReader(in);
					BufferedReader xmlBufferedStream = new BufferedReader(xmlStream);

					xmlIn.setContentHandler(new DefaultHandler()                            // Set handler to parse the data
					{
						private SyncableApp currentApp;

						private String newSyncingPackages;

						@Override
						public void startDocument() throws SAXException
						{
							newSyncingPackages = currentSyncingPackages;
						}

						@Override
						public void endDocument()
						{
							Collections.sort(tempApps);

							Account account = Quantimodo.getAccount(context);
							if (newSyncingPackages.length() == 0)
							{
								Log.i("Disabling sync, no apps selected");
								ContentResolver.setSyncAutomatically(account, "com.quantimodo.sync.content-appdata", false);
							}
							else
							{
								Log.i("Enabling sync, some apps are selected");
								ContentResolver.setSyncAutomatically(account, "com.quantimodo.sync.content-appdata", true);
							}

							prefs.edit().putString("syncingPackages", newSyncingPackages).commit();

							handler.post(new Runnable()
							{
								@Override public void run()
								{
									Global.applications = tempApps;
									listener.onComplete();
								}
							});
						}

						public void startElement(String uri, String name, String qName, Attributes atts)
						{
							if (qName.equals("App"))                                                            // This element is an app
							{
								String label = atts.getValue("label");                                              // Gather info
								String packageName = atts.getValue("packageName");
								boolean rootRequired = Boolean.valueOf(atts.getValue("rootRequired"));
								String dataPath = atts.getValue("dataFile");
								File dataFile;
								if (dataPath.startsWith("/internal"))
								{
									dataFile = new File(dataPath.replace("/internal", dataLocation + "/" + packageName));
								}
								else if (dataPath.startsWith("/external"))
								{
									dataFile = new File(dataPath.replace("/external", sdDataLocation + "/" + packageName));
								}
								else
								{
									dataFile = new File(dataPath);
								}
								boolean isInstalled;
								Drawable icon = null;

								try
								{
									icon = packageManager.getApplicationIcon(packageManager.getApplicationInfo(packageName, 0));
									isInstalled = true;
								}
								catch (PackageManager.NameNotFoundException e)
								{
									isInstalled = false;
									newSyncingPackages = newSyncingPackages.replace(packageName + ",", "");
								}

								String thisSyncingApp = packageName + ",";
								boolean isSyncEnabled = currentSyncingPackages.contains(thisSyncingApp);

                                SyncableAppInfo appInfo = new SyncableAppInfo(label, packageName, dataFile, rootRequired);
								currentApp = new SyncableApp(appInfo, isInstalled, isSyncEnabled);    // Set currentApp to this app
								currentApp.icon = icon;
							}
						}

						public void endElement(String uri, String name, String qName)
						{
							if (qName.equals("App"))                                                             // App tag closed, so add it to the array
							{
								tempApps.add(currentApp);
							}
						}
					});
					xmlIn.parse(new InputSource(xmlBufferedStream));
				}
				catch (SAXException e)
				{
					Log.e("SAX Error " + e.getMessage());
				}
				catch (IOException e)
				{
					Log.e("Input Error " + e.getMessage());
				}
				catch (ParserConfigurationException e)
				{
					Log.e("Configuration Error " + e.getMessage());
				}
			}
		};
		Thread thread = new Thread(run);
		thread.setPriority(Thread.NORM_PRIORITY - 1);   // Lower than normal priority to prevent hindering the UI thread
		thread.start();
	}

	@Override
	public int compareTo(SyncableApp otherApp)
	{
		if (this.isInstalled && otherApp.isInstalled)            // Both installed
		{
			return appInfo.label.compareToIgnoreCase(otherApp.appInfo.label);
		}
		else if (!this.isInstalled && !otherApp.isInstalled)     // Neither installed
		{
			return appInfo.label.compareToIgnoreCase(otherApp.appInfo.label);
		}
		else                                                    // One installed
		{
			if (this.isInstalled)
			{
				return -1;
			}
			else
			{
				return 1;
			}
		}
	}
}
