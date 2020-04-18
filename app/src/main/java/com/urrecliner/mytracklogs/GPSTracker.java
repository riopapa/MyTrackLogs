package com.urrecliner.mytracklogs;

import android.Manifest;
import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import static com.urrecliner.mytracklogs.Vars.gpsUpdateTime;
import static com.urrecliner.mytracklogs.Vars.mContext;
import static com.urrecliner.mytracklogs.Vars.utils;

class GPSTracker extends Service implements LocationListener {

    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    Location location = null; // Location
    double gpsLatitude, gpsLongitude;
    final String logID = "GPS";
    private static final float MIN_DISTANCE_WALK = 5; // meters
    private static final long MIN_TIME_WALK_UPDATES = 10000; // miliSecs
    protected LocationManager locationManager;

    void startGPSUpdate() {

        try {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // No network provider is enabled
            } else {
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (isGPSEnabled) {
                    assert locationManager != null;
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_WALK_UPDATES,
                            MIN_DISTANCE_WALK, this);
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (location != null) {
                            gpsLatitude = location.getLatitude();
                            gpsLongitude = location.getLongitude();
                        }
                    }
                }
                if (isNetworkEnabled) {
                    assert locationManager != null;
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_WALK_UPDATES,
                            MIN_DISTANCE_WALK, this);
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            gpsLatitude = location.getLatitude();
                            gpsLongitude = location.getLongitude();
                        }
                    }
                }
            }
        } catch (Exception e) {
            utils.logE(logID, "Start Error", e);
        }
    }

    void stopGPSUpdate() {
        if (locationManager != null) {
            locationManager.removeUpdates(GPSTracker.this);
        }
    }

    double getGpsLatitude() { return gpsLatitude; }
    double getGpsLongitude() { return gpsLongitude; }

    long prevTime;
    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        utils.log("NEW LOCATION ", location.getLatitude()+" x "+location.getLongitude());
        inform2Main();
    }

    void inform2Main() {
        if (location == null) return;
        gpsLatitude = location.getLatitude();
        gpsLongitude = location.getLongitude();
        gpsUpdateTime = System.currentTimeMillis();
        long nowTime = System.currentTimeMillis();
        utils.log("Tracker loc", gpsLatitude+" x "+gpsLongitude+" timeGap="+(nowTime-prevTime));
        prevTime = nowTime;
        MainActivity.locationUpdated(gpsLatitude, gpsLongitude);
    }

    @Override
    public void onProviderDisabled(String provider) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}