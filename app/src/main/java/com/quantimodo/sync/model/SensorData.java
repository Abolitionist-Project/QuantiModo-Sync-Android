package com.quantimodo.sync.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.quantimodo.sync.Global;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SensorData implements Comparable<SensorData> {
    public static final int TYPE_ENVIRONMENT = 0;
    public static final int TYPE_POSITION = 1;
    public static final int TYPE_MOTION = 2;
    public String separator;                          // Populated if this is a separator and should be handled as one

    public Sensor sensor;
    public String label;                                // Label the user sees in his app drawer
    public int type;
    public Drawable icon;

    private boolean isSyncEnabled;

    public SensorData(String separator) {
        this.separator = separator;
    }

    public SensorData(Sensor sensor, String label, int type, boolean isSyncEnabled) {
        this.sensor = sensor;
        this.label = label;
        this.type = type;
        this.isSyncEnabled = isSyncEnabled;
    }

    /*
     * Enable or disable whether this sensor gets synced.
     *
     * @return True if this sensor is now synced, false if it's not
     */
    public boolean setSyncEnabled(Context context, boolean isSyncEnabled) {
        /*try
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			String currentSyncingPackages = prefs.getString("syncingPackages", "");

			String thisSyncingApp = this.packageName + ",";
			if(isSyncEnabled)
			{
				if(!currentSyncingPackages.contains(thisSyncingApp))
				{
					currentSyncingPackages = currentSyncingPackages.concat(thisSyncingApp);
				}
			}
			else
			{
				if(currentSyncingPackages.contains(thisSyncingApp))
				{
					currentSyncingPackages = currentSyncingPackages.replace(thisSyncingApp, "");
				}
			}

			prefs.edit().putString("syncingPackages", currentSyncingPackages).commit();

			this.isSyncEnabled = isSyncEnabled;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			Log.e("Error saving sync pref: " + e.getMessage());
		}*/

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
    public interface OnAvailableSensorsLoaded {
        public void onComplete();
    }

    /*
     * Get all compatible applications from asset and load them to Global.applications
     */
    public static void getAvailableSensors(final Context context, final Handler handler, final OnAvailableSensorsLoaded listener) {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                final String currentSyncingPackages = prefs.getString("syncingSensors", "");
                final List<SensorData> tempSensors = new ArrayList<SensorData>();

                SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);

                for (Sensor currentSensor : deviceSensors) {
                    SensorData currentSensorData = null;

                    int sensorType = currentSensor.getType();
                    switch (sensorType) {
                        case Sensor.TYPE_ACCELEROMETER:
                            currentSensorData = new SensorData(currentSensor, "Acceleration", TYPE_MOTION, false);  //TODO set sync enabled
                            break;
                        case Sensor.TYPE_AMBIENT_TEMPERATURE:
                            currentSensorData = new SensorData(currentSensor, "Ambient temperature", TYPE_ENVIRONMENT, false);
                            break;
                        case Sensor.TYPE_TEMPERATURE:
                            currentSensorData = new SensorData(currentSensor, "Temperature", TYPE_ENVIRONMENT, false);
                            break;
                        case Sensor.TYPE_GRAVITY:
                            currentSensorData = new SensorData(currentSensor, "Gravity", TYPE_ENVIRONMENT, false);
                            break;
                        case Sensor.TYPE_GYROSCOPE:
                            currentSensorData = new SensorData(currentSensor, "Angular motion", TYPE_MOTION, false);
                            break;
                        case Sensor.TYPE_LIGHT:
                            currentSensorData = new SensorData(currentSensor, "Light intensity", TYPE_ENVIRONMENT, false);
                            break;
                        case Sensor.TYPE_LINEAR_ACCELERATION:
                            currentSensorData = new SensorData(currentSensor, "Linear acceleration", TYPE_MOTION, false);
                            break;
                        case Sensor.TYPE_MAGNETIC_FIELD:
                            currentSensorData = new SensorData(currentSensor, "Magnetic field", TYPE_ENVIRONMENT, false);
                            break;
                        case Sensor.TYPE_PRESSURE:
                            currentSensorData = new SensorData(currentSensor, "Pressure", TYPE_ENVIRONMENT, false);
                            break;
                        case Sensor.TYPE_PROXIMITY:
                            currentSensorData = new SensorData(currentSensor, "Proximity", TYPE_POSITION, false);
                            break;
                        case Sensor.TYPE_RELATIVE_HUMIDITY:
                            currentSensorData = new SensorData(currentSensor, "Humidity", TYPE_ENVIRONMENT, false);
                            break;
                        case Sensor.TYPE_ROTATION_VECTOR:
                            currentSensorData = new SensorData(currentSensor, "Rotation", TYPE_POSITION, false);
                            break;
                        default:
                            // Unknown sensor
                            break;
                    }

                    if (currentSensorData != null) {
                        tempSensors.add(currentSensorData);
                    }
                }

                Collections.sort(tempSensors);

                int currentType = TYPE_POSITION;
                for (int i = 0; i < tempSensors.size(); i++) {
                    SensorData currentData = tempSensors.get(i);
                    if (currentData.separator == null && currentData.type != currentType) {
                        currentType = currentData.type;
                        if (currentType == TYPE_ENVIRONMENT) {
                            tempSensors.add(i, new SensorData("Environment"));
                        } else {
                            tempSensors.add(i, new SensorData("Motion"));
                        }
                    } else if (i == 0) {
                        tempSensors.add(0, new SensorData("Position"));
                    }
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Global.sensors = tempSensors;
                        listener.onComplete();
                    }
                });
            }
        };
        Thread thread = new Thread(run);
        thread.setPriority(Thread.NORM_PRIORITY - 1);   // Lower than normal priority to prevent hindering the UI thread
        thread.start();
    }

    @Override
    public int compareTo(SensorData otherApp) {
        switch (this.type) {
            case TYPE_POSITION:
                switch (otherApp.type) {
                    case TYPE_POSITION:
                        return label.compareToIgnoreCase(otherApp.label);
                    case TYPE_ENVIRONMENT:
                        return -1;
                    case TYPE_MOTION:
                        return -1;
                }
                break;
            case TYPE_ENVIRONMENT:
                switch (otherApp.type) {
                    case TYPE_POSITION:
                        return 1;
                    case TYPE_ENVIRONMENT:
                        return label.compareToIgnoreCase(otherApp.label);
                    case TYPE_MOTION:
                        return -1;
                }
                break;
            case TYPE_MOTION:
                switch (otherApp.type) {
                    case TYPE_POSITION:
                        return 1;
                    case TYPE_ENVIRONMENT:
                        return 1;
                    case TYPE_MOTION:
                        return label.compareToIgnoreCase(otherApp.label);
                }
                break;
        }

        return 0;

		/*if(this.isInstalled && otherApp.isInstalled)            // Both installed
		{
			return label.compareToIgnoreCase(otherApp.label);
		}
		else if(!this.isInstalled && !otherApp.isInstalled)     // Neither installed
		{
			return label.compareToIgnoreCase(otherApp.label);
		}
		else                                                    // One installed
		{
			if(this.isInstalled)
			{
				return -1;
			}
			else
			{
				return 1;
			}
		}*/
    }
}
