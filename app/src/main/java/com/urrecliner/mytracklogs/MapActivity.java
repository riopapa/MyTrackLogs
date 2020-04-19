package com.urrecliner.mytracklogs;

import android.app.Activity;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.mytracklogs.MapUtils.locEast;
import static com.urrecliner.mytracklogs.MapUtils.locNorth;
import static com.urrecliner.mytracklogs.MapUtils.locSouth;
import static com.urrecliner.mytracklogs.MapUtils.locWest;
import static com.urrecliner.mytracklogs.Vars.databaseIO;
import static com.urrecliner.mytracklogs.Vars.decimalComma;
import static com.urrecliner.mytracklogs.Vars.mContext;
import static com.urrecliner.mytracklogs.Vars.mapUtils;
import static com.urrecliner.mytracklogs.Vars.nowLatitude;
import static com.urrecliner.mytracklogs.Vars.nowLongitude;
import static com.urrecliner.mytracklogs.Vars.prevLatitude;
import static com.urrecliner.mytracklogs.Vars.prevLongitude;
import static com.urrecliner.mytracklogs.Vars.showMarker;
import static com.urrecliner.mytracklogs.Vars.trackAdapter;
import static com.urrecliner.mytracklogs.Vars.trackLogs;
import static com.urrecliner.mytracklogs.Vars.utils;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final String logID = "Map";
    private static final int POLYLINE_STROKE_WIDTH_PX = 24;
    private static final int PATTERN_DASH_LENGTH_PX = 6;
    private static final int PATTERN_GAP_LENGTH_PX = 6;
//    private static final PatternItem DOT = new Dot();
    private static final PatternItem DASH = new Dash(PATTERN_DASH_LENGTH_PX);
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);
    private String startAddress;
    private static final List<PatternItem> PATTERN_POLYLINE_MINE = Arrays.asList(DASH, GAP);
    GoogleMap thisMap;
    private Activity mapActivity;
    private long startTime, finishTime, timeBegin;
    private float timeDiff;
    private int iMinutes, iMeters, position, iconWidth, iconHeight;
    ArrayList<LatLng> lineFromToLatLng;
    ArrayList<LocLog> locLogs;
    TextView tvTimeInfo, tvLogInfo, tvPlace;
    SupportMapFragment mapFragment;

    static class LocLog {
        private long logTime;
        private double latitude, longitude;

        LocLog(long logTime, double latitude, double longitude) {
            this.logTime = logTime;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mapActivity = this;
        int orientation = this.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            iconWidth = 200; iconHeight = 360;
        } else {
            iconWidth = 360; iconHeight = 200;
        }
        TrackLog trackLog = getIntent().getParcelableExtra("trackLog");
        startTime = trackLog.getStartTime();
        finishTime = trackLog.getFinishTime();
        iMinutes = trackLog.getMinutes();
        iMeters = trackLog.getMeters();
        position = getIntent().getIntExtra("position",-1);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragMap);
        mapFragment.getMapAsync(this);
        tvTimeInfo =  findViewById(R.id.timeSummary);
        tvLogInfo =  findViewById(R.id.logSummary);
        tvPlace = findViewById(R.id.logPlace);

        ImageView iv = findViewById(R.id.smallMap);
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        thisMap = googleMap;
        if (showMarker == null)
            showMarker = new ShowMarker();
        showMarker.init(mapActivity, googleMap);
        String s;
        if (retrieveDBLog()) return;
        Geocoder geocoder = new Geocoder(this, Locale.KOREA);
        startAddress = GPS2Address.get(geocoder, locLogs.get(0).latitude, locLogs.get(0).longitude);
        String finishAddress = GPS2Address.get(geocoder, locLogs.get(locLogs.size()-1).latitude, locLogs.get(locLogs.size()-1).longitude);
        if (!startAddress.equals(finishAddress))
            startAddress += "~"+finishAddress;

        double fullMapDistance = mapUtils.getFullMapDistance();
        int mapScale = mapUtils.getMapScale(fullMapDistance);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng((locNorth + locSouth)/2, (locEast + locWest)/2),
                (float) mapScale - 0.1f));
        s = utils.long2DateDayTime(locLogs.get(0).logTime)+" ~ "+utils.long2DateDayTime(locLogs.get(locLogs.size()-1).logTime)+"   ";
        tvTimeInfo.setText(s);
        timeBegin =  locLogs.get(0).logTime;
        timeDiff = locLogs.get(locLogs.size()-1).logTime - locLogs.get(0).logTime;
        if (position != -1) {
            s = utils.minute2Text(iMinutes) + "  " + decimalComma.format(iMeters) + "m";
            tvLogInfo.setText(s);
        }
        else
            tvLogInfo.setVisibility(View.INVISIBLE);
        tvPlace.setText(startAddress);
        View v = findViewById(R.id.fragMap);
        v.post(new Runnable() {
            @Override
            public void run() {
                new Timer().schedule(new TimerTask() {
                    public void run() {
                        thisMap.snapshot(mapSaveShot);
                    }
                }, 100);
            }
        });
//        googleMap.setMyLocationEnabled(true);
//        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
    }

    private void drawTrackLIne(GoogleMap googleMap) {
        lineFromToLatLng = new ArrayList<>();
        lineFromToLatLng.add(new LatLng(0,0));
        lineFromToLatLng.add(new LatLng(0,0));
        AnimatedColor animatedColor = new AnimatedColor(Color.MAGENTA, Color.CYAN);
        int color = 0;
        for (int i = 0; i < locLogs.size()-2; i++) {
            lineFromToLatLng.set(0, new LatLng(locLogs.get(i).latitude, locLogs.get(i).longitude));
            lineFromToLatLng.set(1, new LatLng(locLogs.get(i+1).latitude, locLogs.get(i+1).longitude));
            float ratio = (float) (locLogs.get(i).logTime-timeBegin) / timeDiff;
            color = animatedColor.with(ratio);
            if (i % 3 == 0)
                color = color ^ 0x03333333;
            else if (i % 2 == 0)
                color = 0xFF0F0F0F;
            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.width(POLYLINE_STROKE_WIDTH_PX);
            polyOptions.addAll(lineFromToLatLng);
            polyOptions.color(color);
            googleMap.addPolyline(polyOptions);
        }

        showMarker.drawStart(locLogs.get(0).latitude, locLogs.get(0).longitude, true);
        showMarker.drawFinish(locLogs.get(locLogs.size()-1).latitude, locLogs.get(locLogs.size()-1).longitude, true);
    }

    private boolean retrieveDBLog() {

        utils.log(logID,"start of retrieve");
        Cursor cursor = databaseIO.logGetFromTo(startTime, finishTime);
        if (cursor != null) {
            utils.log(logID, "count ="+cursor.getCount());
            if (cursor.moveToFirst()) {
                locSouth = 999; locNorth = -999; locWest = 999; locEast = -999;
                locLogs = new ArrayList<>();
                prevLatitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
                prevLongitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
                do {
                    long thisTime = cursor.getLong(cursor.getColumnIndex("logTime"));
                    nowLatitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
                    nowLongitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
                    locLogs.add(new LocLog(thisTime, nowLatitude, nowLongitude));
                    if (nowLatitude > locNorth) locNorth = nowLatitude;
                    if (nowLatitude < locSouth) locSouth = nowLatitude;
                    if (nowLongitude > locEast) locEast = nowLongitude;
                    if (nowLongitude < locWest) locWest = nowLongitude;
                } while (cursor.moveToNext());
                locLogs.add(new LocLog(locLogs.get(locLogs.size()-1).logTime, nowLatitude, nowLongitude));
            }
            if (cursor.getCount() < 10) {
                Toast.makeText(mapActivity,"자료가 너무 작음("+cursor.getCount()+"), 삭제 요망",Toast.LENGTH_LONG).show();
                return true;
            }
        }
        else {
            Toast.makeText(mContext,"No log data to display ",Toast.LENGTH_LONG).show();
            utils.log("no data",utils.long2DateDay(startTime)+" "+utils.long2Time(startTime)+" ~ "+utils.long2DateDay(finishTime)+" "+utils.long2Time(finishTime));
            return true;
        }
        return false;
    }

    Bitmap pureMap = null, trackMap = null;
    GoogleMap.SnapshotReadyCallback mapSaveShot = new GoogleMap.SnapshotReadyCallback() {

        @Override
        public void onSnapshotReady(Bitmap snapshot) {      // save map to bitmap without tracklog
//            utils.log(logID," snapShot "+ reDrawCount);
            pureMap = Bitmap.createScaledBitmap(snapshot, iconWidth, iconHeight, false);
            drawTrackLIne(thisMap);
            if (position >= 0) {
                new Timer().schedule(new TimerTask() {
                    public void run() {
                        thisMap.snapshot(buildMapIcon);
                    }
                }, 500);
            }
            else {
                ImageView iv = findViewById(R.id.smallMap);
                iv.setVisibility(View.INVISIBLE);
            }
        }
    };

    GoogleMap.SnapshotReadyCallback buildMapIcon = new GoogleMap.SnapshotReadyCallback() {

        @Override
        public void onSnapshotReady(Bitmap snapshot) {  // save map to bitmap with trackLog
//            utils.log(logID, "Redraw "+ reDrawCount);
            trackMap = filterBitmap(pureMap, Bitmap.createScaledBitmap(snapshot, iconWidth, iconHeight, false));
            ImageView iv = findViewById(R.id.smallMap);
            iv.setImageBitmap(trackMap);
            final TrackLog trackLog = trackLogs.get(position);
            trackLog.setBitMap(trackMap);
            trackLog.setPlaceName(startAddress);
            trackLogs.set(position, trackLog);
            databaseIO.trackMapPlaceUpdate(trackLog.getStartTime(), trackMap, startAddress);
            trackAdapter.notifyItemChanged(position);
        }
    };

    Bitmap filterBitmap(Bitmap bitMap, Bitmap routeMap) {
        int width = bitMap.getWidth();
        int height = bitMap.getHeight();
        int[] pixelsB = new int[width * height];
        int[] pixelsR = new int[width * height];
        bitMap.getPixels(pixelsB, 0, width, 0, 0, width, height);
        routeMap.getPixels(pixelsR, 0, width, 0, 0, width, height);
        for(int x = 0; x < pixelsR.length; ++x) {
            if (pixelsB[x] == pixelsR[x])
                pixelsR[x] = pixelsR[x] & 0x0F0F0F0F;       // grey out
        }
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.setPixels(pixelsR, 0, width, 0, 0, width, height);
        return result;
    }
}
