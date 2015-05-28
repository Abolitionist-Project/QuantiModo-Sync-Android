package com.quantimodo.sync.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import com.quantimodo.sync.Global;
import com.quantimodo.sync.Log;
import com.quantimodo.sync.R;
import com.quantimodo.sync.sync.SyncHelper;
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

public class ApplicationData implements Comparable<ApplicationData> {
    public String syncStatus;

    public String label;                                // Label the user sees in his app drawer
    public String packageName;                          // Package name to identify individual apps
    public File dataFile;                         // Location of this app's useful data
    public boolean rootRequired;                        // True if root is required to export data
    public boolean isInstalled;
    public List<String> dataTypes = new ArrayList<String>(); // Types of data this app can provide
    public Drawable icon;

    private boolean isSyncEnabled;

    public ApplicationData(String label, String packageName, File dataFile, boolean rootRequired, boolean isInstalled, boolean isSyncEnabled) {
        this.label = label;
        this.packageName = packageName;
        this.dataFile = dataFile;
        this.rootRequired = rootRequired;
        this.isInstalled = isInstalled;
        this.isSyncEnabled = isSyncEnabled;
    }

    /*
     * Enable or disable whether this app gets synced.
     *
     * @return True if this app is now synced, false if it's not
     */
    public boolean setSyncEnabled(Context context, boolean isSyncEnabled) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("com.quantimodo.sync_preferences", Context.MODE_MULTI_PROCESS);
            String currentSyncingPackages = prefs.getString("syncingPackages", "");

            String thisSyncingApp = this.packageName + ",";
            Log.i("Before syncing: " + currentSyncingPackages + ":");
            if (isSyncEnabled) {
                if (!currentSyncingPackages.contains(thisSyncingApp)) {
                    if (currentSyncingPackages.length() == 0) {
                        Log.i("Enabling tracking apps sync, new app selected");
                        SyncHelper.scheduleSync(context);
                    }
                    currentSyncingPackages = currentSyncingPackages.concat(thisSyncingApp);
                }
            } else {
                if (currentSyncingPackages.contains(thisSyncingApp)) {
                    currentSyncingPackages = currentSyncingPackages.replace(thisSyncingApp, "");
                    if (currentSyncingPackages.length() == 0) {
                        Log.i("Disabling tracking apps sync, no apps selected");
                        SyncHelper.unscheduleSync(context);
                    }
                }
            }
            Log.i("After syncing: " + currentSyncingPackages + ":");
            prefs.edit().putString("syncingPackages", currentSyncingPackages).commit();

            this.isSyncEnabled = isSyncEnabled;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Error saving sync pref: " + e.getMessage());
        }

        return this.isSyncEnabled;
    }

    /*
     * True if this app is being synced, false if it's not
     */
    public boolean isSyncEnabled() {
        return isSyncEnabled;
    }

    /*
     * Listener to accompany getCompatibleApplications
     */
    public interface OnCompatibleApplicationsLoaded {
        public void onComplete();
    }

    /*
     * Get all compatible applications from XML definition and load them to Global.applications
     */
    public static void getCompatibleApplications(final Context context, final Handler handler, final OnCompatibleApplicationsLoaded listener) {
        Log.i("Getting compatible apps from XML");
        Runnable run = new Runnable() {
            @Override
            public void run() {
                String filesPath = context.getFilesDir().getPath();
                final String dataLocation = filesPath.replace("/" + context.getPackageName() + "/files", "");
                final String sdDataLocation = Environment.getExternalStorageDirectory() + "/Android/data";

                final SharedPreferences prefs = context.getSharedPreferences("com.quantimodo.sync_preferences", Context.MODE_MULTI_PROCESS);
                final String currentSyncingPackages = prefs.getString("syncingPackages", "");
                final List<ApplicationData> tempApps = new ArrayList<ApplicationData>();
                final PackageManager packageManager = context.getPackageManager();

                try {
                    InputStream in = context.getResources().openRawResource(R.raw.compatible_apps);    // Open resource containing compatible app data

                    System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver"); // Init SAX parser
                    SAXParserFactory parseFactory = SAXParserFactory.newInstance();
                    SAXParser xmlParser = parseFactory.newSAXParser();

                    XMLReader xmlIn = xmlParser.getXMLReader();                             // Init XMl reader to read from asset
                    InputStreamReader xmlStream = new InputStreamReader(in);
                    BufferedReader xmlBufferedStream = new BufferedReader(xmlStream);

                    xmlIn.setContentHandler(new DefaultHandler()                            // Set handler to parse the data
                    {
                        private ApplicationData currentApp;

                        private String newSyncingPackages;

                        @Override
                        public void startDocument() throws SAXException {
                            newSyncingPackages = currentSyncingPackages;
                        }

                        @Override
                        public void endDocument() {
                            Collections.sort(tempApps);

                            if (newSyncingPackages.length() == 0) {
                                Log.i("Disabling sync, no apps selected");
                                SyncHelper.unscheduleSync(context);
                            } else {
                                Log.i("Enabling sync, some apps are selected");
                                if (!SyncHelper.isSync(context)) {
                                    SyncHelper.scheduleSync(context);
                                }
                            }

                            prefs.edit().putString("syncingPackages", newSyncingPackages).commit();

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Global.applications = tempApps;
                                    listener.onComplete();
                                }
                            });
                        }

                        public void startElement(String uri, String name, String qName, Attributes atts) {
                            if (qName.equals("App"))                                                            // This element is an app
                            {
                                String label = atts.getValue("label");                                              // Gather info
                                String packageName = atts.getValue("packageName");
                                boolean rootRequired = Boolean.valueOf(atts.getValue("rootRequired"));
                                String dataPath = atts.getValue("dataFile");
                                File dataFile;
                                if (dataPath.startsWith("/internal")) {
                                    dataFile = new File(dataPath.replace("/internal", dataLocation + "/" + packageName));
                                } else if (dataPath.startsWith("/external")) {
                                    dataFile = new File(dataPath.replace("/external", sdDataLocation + "/" + packageName));
                                } else {
                                    dataFile = new File(dataPath);
                                }
                                boolean isInstalled;
                                Drawable icon = null;

                                try {
                                    icon = packageManager.getApplicationIcon(packageManager.getApplicationInfo(packageName, 0));
                                    isInstalled = true;
                                } catch (PackageManager.NameNotFoundException e) {
                                    isInstalled = false;
                                    newSyncingPackages = newSyncingPackages.replace(packageName + ",", "");
                                }

                                String thisSyncingApp = packageName + ",";
                                boolean isSyncEnabled = currentSyncingPackages.contains(thisSyncingApp);

                                currentApp = new ApplicationData(label, packageName, dataFile, rootRequired, isInstalled, isSyncEnabled);    // Set currentApp to this app
                                currentApp.icon = icon;
                            } else if (qName.equals("DataTypes"))                                                  // This element contains datatypes for currentApp
                            {
                                for (int i = 0; i < atts.getLength(); i++)                                           // Loop through all types and store the name
                                {
                                    currentApp.dataTypes.add(atts.getValue(i));
                                }
                            }
                        }

                        public void endElement(String uri, String name, String qName) {
                            if (qName.equals("App"))                                                             // App tag closed, so add it to the array
                            {
                                tempApps.add(currentApp);
                            }
                        }
                    });
                    xmlIn.parse(new InputSource(xmlBufferedStream));
                } catch (SAXException e) {
                    Log.e("SAX Error " + e.getMessage());
                } catch (IOException e) {
                    Log.e("Input Error " + e.getMessage());
                } catch (ParserConfigurationException e) {
                    Log.e("Configuration Error " + e.getMessage());
                }
            }
        };
        Thread thread = new Thread(run);
        thread.setPriority(Thread.NORM_PRIORITY - 1);   // Lower than normal priority to prevent hindering the UI thread
        thread.start();
    }

    @Override
    public int compareTo(ApplicationData otherApp) {
        if (this.isInstalled && otherApp.isInstalled)            // Both installed
        {
            return label.compareToIgnoreCase(otherApp.label);
        } else if (!this.isInstalled && !otherApp.isInstalled)     // Neither installed
        {
            return label.compareToIgnoreCase(otherApp.label);
        } else                                                    // One installed
        {
            if (this.isInstalled) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
