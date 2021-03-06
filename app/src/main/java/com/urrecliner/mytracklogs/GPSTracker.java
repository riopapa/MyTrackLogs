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

import static com.urrecliner.mytracklogs.Vars.isWalk;
import static com.urrecliner.mytracklogs.Vars.utils;

class GPSTracker extends Service implements LocationListener {

    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    Location location = null; // Location
    double GPSLat, GPSLng;
    final String logID = "GPS";
    protected LocationManager locationManager;
    Context context;

    void startGPSUpdate(Context context) {
        this.context = context;

        float updateDistance = (isWalk) ? 5: 30;
        long updateTime = (isWalk) ? 5000: 10000;
        try {
            locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            assert locationManager != null;
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED)
                return;
            if (isGPSEnabled || isNetworkEnabled) {
                assert locationManager != null;
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, updateTime, updateDistance, this);
                if (locationManager != null) {
                    location = locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        GPSLat = location.getLatitude();
                        GPSLng = location.getLongitude();
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

    double getGPSLat() { return GPSLat; }
    double getGPSLng() { return GPSLng; }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        GPSLat = location.getLatitude();
        GPSLng = location.getLongitude();
        MainActivity.newGPSArrived(GPSLat, GPSLng);
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