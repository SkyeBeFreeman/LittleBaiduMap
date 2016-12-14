package com.zhtian.experimentten;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import java.security.Security;

/**
 * Created by zhtian on 2016/11/30.
 */

public class SensorUtils {

    private static SensorUtils sensorUtilsInstance;
    private SensorManager mSensorManager;
    private Sensor mMagneticSensor;
    private Sensor mAccelerometerSensor;
    private LocationManager mLocationManager;
    private Location mCurrentLocation;
    private float mCurrentRotation;
    private MyListener mMyListener;
    private Toast mToast;
    private static final int TWO_MINUTES = 1000 * 60 * 2;

    public SensorUtils() {
        mSensorManager = (SensorManager) BaseApplication.getContext().getSystemService(Context.SENSOR_SERVICE);
        mLocationManager = (LocationManager) BaseApplication.getContext().getSystemService(Context.LOCATION_SERVICE);
        mMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mCurrentRotation = 0;
    }

    public static SensorUtils getSensorUtilsInstance() {
        if (sensorUtilsInstance == null) {
            sensorUtilsInstance = new SensorUtils();
        }
        return sensorUtilsInstance;
    }

    public void registerSensors() {
        mSensorManager.registerListener(mSensorEventListener, mMagneticSensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(mSensorEventListener, mAccelerometerSensor, SensorManager.SENSOR_DELAY_GAME);

        if (ActivityCompat.checkSelfPermission(BaseApplication.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(BaseApplication.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mGPSListener);
        }
        if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mNetworkListener);
        }

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        String provider = mLocationManager.getBestProvider(criteria, true);
        mLocationManager.getLastKnownLocation(provider);
        mCurrentLocation = mLocationManager.getLastKnownLocation(provider);
    }

    public void unregisterSensors() {
        mSensorManager.unregisterListener(mSensorEventListener);
    }

    // sensor event listener
    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        float[] accValues = null;
        float[] magValues = null;
        long lastRotation = 0;

        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    accValues = event.values.clone();
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    magValues = event.values.clone();
                    break;
                default:
                    break;
            }


            if (accValues != null && magValues != null) {
                float[] R = new float[9];
                float[] values = new float[3];

                SensorManager.getRotationMatrix(R, null, accValues, magValues);
                SensorManager.getOrientation(R, values);
                float newRotationDegree = (float) Math.toDegrees(values[0]);

                if (Math.abs(mCurrentRotation - newRotationDegree) > 1) {
                    mCurrentRotation = newRotationDegree;
                }

                long temp = System.currentTimeMillis();
                if (temp - lastRotation > 1000 && mMyListener != null) {
                    lastRotation = temp;
                    mMyListener.updateInTime(mCurrentLocation, mCurrentRotation, 1);
                }
                // 判断摇动
                float xValue = Math.abs(accValues[0]);
                float yValue = Math.abs(accValues[1]);
                float zValue = Math.abs(accValues[2]);
                if (xValue > 15 || yValue > 15 || zValue > 15) {
                    // 认为用户摇动了手机，触发摇一摇逻辑
                    if (mMyListener != null) {
                        mMyListener.updateInTime(mCurrentLocation, mCurrentRotation, 2);
                    }
                }


            }


        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    // GPS 监听器
    private LocationListener mGPSListener = new LocationListener() {
        private boolean isRemove = false;//判断网络监听是否移除

        @Override
        public void onLocationChanged(Location location) {
            if (isBetterLocation(location, mCurrentLocation)) {
                mCurrentLocation = location;
                mMyListener.updateInTime(mCurrentLocation, mCurrentRotation, 1);
            }
            // 获得GPS服务后，移除network监听
            if (location != null && !isRemove
                    && ActivityCompat.checkSelfPermission(BaseApplication.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(BaseApplication.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                mLocationManager.removeUpdates(mNetworkListener);
                isRemove = true;
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (LocationProvider.OUT_OF_SERVICE == status) {
                showToast("GPS服务丢失,切换至网络定位");
                if (ActivityCompat.checkSelfPermission(BaseApplication.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(BaseApplication.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mLocationManager
                        .requestLocationUpdates(
                                LocationManager.NETWORK_PROVIDER, 0, 0,
                                mNetworkListener);
            }
        }

        public void onProviderEnabled(String provider) {
            showToast("GPS Provider Enabled");
        }

        public void onProviderDisabled(String provider) {
            showToast("GPS Provider Disabled");
        }
    };

    // Network 监听器
    private LocationListener mNetworkListener = new LocationListener() {

        private boolean isRemove = false;//判断GPS监听是否移除

        public void onLocationChanged(Location location) {
            if (isBetterLocation(location, mCurrentLocation)) {
                mCurrentLocation = location;
                mMyListener.updateInTime(mCurrentLocation, mCurrentRotation, 1);
            }
            // 获得GPS服务后，移除network监听
            if (location != null && !isRemove
                    && ActivityCompat.checkSelfPermission(BaseApplication.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(BaseApplication.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationManager.removeUpdates(mNetworkListener);
                isRemove = true;
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) throws SecurityException {
            if (LocationProvider.OUT_OF_SERVICE == status) {
                showToast("网络服务丢失,切换至GPS定位");
                if (ActivityCompat.checkSelfPermission(BaseApplication.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(BaseApplication.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mLocationManager
                        .requestLocationUpdates(
                                LocationManager.GPS_PROVIDER, 0, 0,
                                mGPSListener);
            }
        }

        public void onProviderEnabled(String provider) {
            showToast("Network Provider Enabled");
        }

        public void onProviderDisabled(String provider) {
            showToast("Network Provider Disabled");
        }
    };


    /**
     * from https://developer.android.com/guide/topics/location/strategies.html
     * <p>
     * Determines whether one Location reading is better than the current Location fix
     *
     * @param location            The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * from https://developer.android.com/guide/topics/location/strategies.html
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    public Location getCurrentLocation() {
        return mCurrentLocation;
    }

    public float getCurrentRotation() {
        return mCurrentRotation;
    }

    public static abstract interface MyListener {
        public abstract void updateInTime(Location location, float rotation, int type);
    }

    public void setMyListener(MyListener myListener) {
        mMyListener = myListener;
    }

    public void removeMyListener(MyListener myListener) {
        if (myListener != null)
            myListener = null;
    }

    public void showToast(String str) {
        if (mToast == null) {
            mToast = Toast.makeText(BaseApplication.getContext(), str, Toast.LENGTH_SHORT);
            mToast.show();
        } else {
            mToast.setText(str);
            mToast.show();
        }
    }

}
