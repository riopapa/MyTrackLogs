package com.urrecliner.mytracklogs;

import android.app.Activity;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

class ShowMarker {

    private GoogleMap thisMap;
    private Marker markerStart, markerFinish, markerHere;
    private Activity showActivity;
    private CustomCap endCap;
    private PolylineOptions polyOptions;
    private static final int POLYLINE_STROKE_WIDTH_PX = 14;

    void init(Activity activity, GoogleMap map) {
        showActivity = activity;
        thisMap = map;

        if (markerStart != null) markerStart.remove();
        if (markerFinish != null) markerFinish.remove();
        if (markerHere != null) markerHere.remove();
        markerStart = null; markerFinish = null; markerHere = null;
        endCap = new CustomCap(
                BitmapDescriptorFactory.fromResource(R.mipmap.triangle), 10);
        polyOptions = new PolylineOptions();
        polyOptions.width(POLYLINE_STROKE_WIDTH_PX);
        polyOptions.color(showActivity.getColor(R.color.trackRoute));
        polyOptions.endCap(endCap);
    }

    void drawStart (final double latitude, final double longitude, final boolean big) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (markerStart != null)
                    markerStart.remove();
                showActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        markerStart = thisMap.addMarker(new MarkerOptions()
                                .zIndex(200f)
                                .position(new LatLng(latitude, longitude))
                                .icon(BitmapDescriptorFactory.fromResource(
                                        (big)? R.mipmap.marker_start_big :R.mipmap.marker_start)));
                    }
                });
            }
        });
    }

    void drawFinish (final double latitude, final double longitude, final boolean big) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (markerFinish != null)
                    markerFinish.remove();
                showActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        markerFinish = thisMap.addMarker(new MarkerOptions()
                                .zIndex(300f)
                                .position(new LatLng(latitude, longitude))
                                .icon(BitmapDescriptorFactory.fromResource(
                                        (big) ? R.mipmap.marker_finish_big:R.mipmap.marker_finish)));
                    }
                });
            }
        });
    }
//
//    void drawHere (final double latitude, final double longitude) {
//        new Handler(Looper.getMainLooper()).post(new Runnable() {
//            @Override
//            public void run() {
//                if (markerHere != null)
//                    markerHere.remove();
//                showActivity.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        markerHere = thisMap.addMarker(new MarkerOptions()
//                                .zIndex(10000f)
//                                .position(new LatLng(latitude, longitude))
//                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.my_face)));
//                    }
//                });
//            }
//        });
//    }

    void drawHereOff() {
        if (markerHere != null)
            markerHere.remove();
    }

    private Polyline polyline = null;
    private ArrayList<LatLng> prevLatLng = null;
    void drawLine(final ArrayList<LatLng> nowLatLng, final boolean isWalk) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                showActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (polyline != null) {
                            polyline.remove();
                            polyOptions = new PolylineOptions();
                            polyOptions.width(POLYLINE_STROKE_WIDTH_PX);
                            polyOptions.color(showActivity.getColor(R.color.trackRoute));
                            endCap = new CustomCap(BitmapDescriptorFactory.fromResource(R.mipmap.triangle), 16);
                            polyOptions.endCap(endCap);
                            polyOptions.addAll(prevLatLng);
                            thisMap.addPolyline(polyOptions);
                        }
                        endCap = new CustomCap(
                                (isWalk) ? BitmapDescriptorFactory.fromResource(R.mipmap.foot_mini) :
                                        BitmapDescriptorFactory.fromResource(R.mipmap.drive_mini), 6);
                        polyOptions = new PolylineOptions();
                        polyOptions.width(POLYLINE_STROKE_WIDTH_PX);
                        polyOptions.color(showActivity.getColor(R.color.trackRoute));
                        polyOptions.endCap(endCap);
                        polyOptions.addAll(nowLatLng);
                        polyline = thisMap.addPolyline(polyOptions);
                        prevLatLng = new ArrayList<>();
                        prevLatLng.addAll(nowLatLng);
                    }
                });
            }
        });
    }
}
