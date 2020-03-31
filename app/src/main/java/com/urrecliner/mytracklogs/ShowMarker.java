package com.urrecliner.mytracklogs;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

class ShowMarker {

    private double lat, lng;
    private GoogleMap mainMap;
    private Marker markerStart, markerFinish, markerHere;
    private Activity showActivity;
    private LatLng latLng;
    private CustomCap endCap;

    void init(Activity activity, GoogleMap map) {
        if (markerStart != null) markerStart.remove();
        if (markerFinish != null) markerFinish.remove();
        if (markerHere != null) markerHere.remove();
        markerStart = null; markerFinish = null; markerHere = null;
        showActivity = activity;
        mainMap = map;
        endCap = new CustomCap(
                BitmapDescriptorFactory.fromResource(R.mipmap.triangle), 12);
    }

    void drawStart (double latitude, double longitude) {
        latLng = new LatLng(latitude, longitude);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (markerStart != null)
                    markerStart.remove();
                showActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        markerStart = mainMap.addMarker(new MarkerOptions()
                                .zIndex(2000f)
                                .position(latLng)
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker_start)));
                    }
                });
            }
        });
    }

    void drawFinish (double latitude, double longitude) {
        latLng = new LatLng(latitude, longitude);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (markerFinish != null)
                    markerFinish.remove();
                showActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        markerFinish = mainMap.addMarker(new MarkerOptions()
                                .zIndex(3000f)
                                .position(latLng)
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker_finish)));
                    }
                });
            }
        });
    }

    void drawHere (double latitude, double longitude) {
        latLng = new LatLng(latitude, longitude);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (markerHere != null)
                    markerHere.remove();
                showActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        markerHere = mainMap.addMarker(new MarkerOptions()
                                .zIndex(10000f)
                                .position(latLng)
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.my_face)));
                    }
                });
            }
        });
    }

    private static final int POLYLINE_STROKE_WIDTH_PX = 6;

    void drawPoly (final ArrayList<LatLng> listLatLng) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                showActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PolylineOptions polyOptions = new PolylineOptions();
                        polyOptions.color(showActivity.getColor(R.color.trackRoute));
                        polyOptions.width(POLYLINE_STROKE_WIDTH_PX);
                        polyOptions.endCap(endCap);
                        polyOptions.addAll(listLatLng);
                        mainMap.addPolyline(polyOptions);
                    }
                });
            }
        });
    }

}
