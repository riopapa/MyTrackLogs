package com.urrecliner.mytracklogs;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

import androidx.core.app.ActivityCompat;

import static com.urrecliner.mytracklogs.Vars.utils;

class GPSTracker extends Service implements LocationListener {

    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    Location location = null; // Location
    double trackerLat, trackerLng;
    final String logID = "GPS";
    private static final float MIN_DISTANCE_WALK = 5; // meters
    private static final long MIN_TIME_WALK_UPDATES = 5000; // miliSecs
    protected LocationManager locationManager;
    float updateDistance = MIN_DISTANCE_WALK;
    long updateTime = MIN_TIME_WALK_UPDATES;
    Context context;

    void startGPSUpdate(Context context) {
        this.context = context;

        try {
            locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            assert locationManager != null;
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)
                return;
            if (isGPSEnabled) {
                assert locationManager != null;
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, updateTime, updateDistance, this);
                if (locationManager != null) {
                    location = locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        trackerLat = location.getLatitude();
                        trackerLng = location.getLongitude();
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
                        trackerLat = location.getLatitude();
                        trackerLng = location.getLongitude();
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

    double getTrackerLat() { return trackerLat; }
    double getTrackerLng() { return trackerLng; }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        trackerLat = location.getLatitude();
        trackerLng = location.getLongitude();
        MainActivity.locationUpdatedByGPSTracker(trackerLat, trackerLng);
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