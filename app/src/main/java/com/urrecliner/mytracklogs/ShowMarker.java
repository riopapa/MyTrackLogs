package com.urrecliner.mytracklogs;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.util.ArrayList;

import static com.google.android.gms.maps.model.BitmapDescriptorFactory.fromResource;
import static com.urrecliner.mytracklogs.Vars.HIGH_SPEED_DRIVE;
import static com.urrecliner.mytracklogs.Vars.HIGH_SPEED_WALK;
import static com.urrecliner.mytracklogs.Vars.LOW_SPEED_DRIVE;
import static com.urrecliner.mytracklogs.Vars.LOW_SPEED_WALK;
import static com.urrecliner.mytracklogs.Vars.isWalk;
import static com.urrecliner.mytracklogs.Vars.mContext;
import static com.urrecliner.mytracklogs.Vars.utils;

class ShowMarker {

    private GoogleMap thisMap;
    private Marker markerStart, markerFinish, markerHere;
    private Activity showActivity;
    private CustomCap endCap;
    private PolylineOptions polyOptions;
    private static final int POLYLINE_STROKE_WIDTH_PX = 14;
    private float lowSpeed, highSpeed, lowSqrt, highSqrt;
    private Bitmap bitmapWalkDrive;

    void init(Activity activity, GoogleMap map) {
        showActivity = activity;
        thisMap = map;
        lowSpeed = (float) ((isWalk) ? LOW_SPEED_WALK: LOW_SPEED_DRIVE);
        highSpeed = (float) ((isWalk) ? HIGH_SPEED_WALK: HIGH_SPEED_DRIVE);
        lowSqrt = (float) ((isWalk) ? Math.sqrt(LOW_SPEED_WALK): Math.sqrt(LOW_SPEED_DRIVE));
        highSqrt = (float) ((isWalk) ? Math.sqrt(HIGH_SPEED_WALK): Math.sqrt(HIGH_SPEED_DRIVE));

        bitmapWalkDrive = (isWalk) ? BitmapFactory.decodeResource(mContext.getResources(),R.mipmap.footprint) :
                BitmapFactory.decodeResource(mContext.getResources(),R.mipmap.drive);
        if (markerStart != null) markerStart.remove();
        if (markerFinish != null) markerFinish.remove();
        if (markerHere != null) markerHere.remove();
        markerStart = null; markerFinish = null; markerHere = null;
//        endCap = new CustomCap(
//                fromResource(R.mipmap.triangle), 10);
        polyOptions = new PolylineOptions();
        polyOptions.width(POLYLINE_STROKE_WIDTH_PX);
        polyOptions.color(showActivity.getColor(R.color.trackRoute));
//        polyOptions.endCap(endCap);
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
                                .icon(fromResource(
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
                                .icon(fromResource(
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
    void drawLine(final ArrayList<LatLng> nowLatLng, final boolean isWalk, final int colorCode) {

        final int capColor = colorCode ^ 0x222222;
        final Bitmap capColormap = utils.changeBitmapColor(bitmapWalkDrive, capColor);
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
                            endCap = new CustomCap(fromResource(R.mipmap.triangle), 20);
                            polyOptions.endCap(endCap);
                            polyOptions.addAll(prevLatLng);
                            thisMap.addPolyline(polyOptions);
                        }
                        CustomCap customCap = new CustomCap(BitmapDescriptorFactory.fromBitmap(capColormap),24); // big                         customCap = new CustomCap(BitmapDescriptorFactory.fromBitmap(capColormap),24); // big number small icon
                        polyOptions = new PolylineOptions();
                        polyOptions.width(POLYLINE_STROKE_WIDTH_PX);
                        polyOptions.endCap(customCap);
                        polyOptions.color(colorCode);
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
