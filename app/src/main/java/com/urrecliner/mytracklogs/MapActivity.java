package com.urrecliner.mytracklogs;

import android.app.Activity;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

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
import static com.urrecliner.mytracklogs.Vars.trackPosition;
import static com.urrecliner.mytracklogs.Vars.trackView;
import static com.urrecliner.mytracklogs.Vars.utils;
import static java.lang.Math.min;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final String logID = "Map";
    private static final int POLYLINE_STROKE_WIDTH_PX = 20;
    private static final int PATTERN_DASH_LENGTH_PX = 6;
    private static final int PATTERN_GAP_LENGTH_PX = 6;
//    private static final PatternItem DOT = new Dot();
//    private static final PatternItem DASH = new Dash(PATTERN_DASH_LENGTH_PX);
//    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);
//    private static final List<PatternItem> PATTERN_POLYLINE_MINE = Arrays.asList(DASH, GAP);
    GoogleMap thisMap;
    private String resultAddress;
    double locSouth, locNorth, locWest, locEast;
    private Activity mapActivity;
    private long startTime, finishTime, timeBegin;
    private float timeDiff;
    private int iMinutes, iMeters, iconWidth, iconHeight;
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
            iconWidth = 200; iconHeight = 300;
        } else {
            iconWidth = 300; iconHeight = 200;
        }
        TrackLog trackLog = getIntent().getParcelableExtra("trackLog");
        startTime = trackLog.getStartTime();
        finishTime = trackLog.getFinishTime();
        iMinutes = trackLog.getMinutes();
        iMeters = trackLog.getMeters();
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
        String s = "";
        if (retrieveDBLog()) return;
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        String startAddress = GPS2Address.get(geocoder, locLogs.get(0).latitude, locLogs.get(0).longitude);
        String mid1Address = GPS2Address.get(geocoder, locLogs.get((locLogs.size() - 1) / 3).latitude, locLogs.get((locLogs.size() - 1) / 3).longitude);
        String mid2Address = GPS2Address.get(geocoder, locLogs.get((locLogs.size() - 1) / 3 * 2).latitude, locLogs.get((locLogs.size() - 1) / 3 * 2).longitude);
        String finishAddress = GPS2Address.get(geocoder, locLogs.get(locLogs.size() - 1).latitude, locLogs.get(locLogs.size() - 1).longitude);
        resultAddress = buildFromToAddress(startAddress, mid1Address, mid2Address, finishAddress);
        double fullMapDistance = mapUtils.getFullMapDistance(locEast, locWest, locSouth, locNorth);
        int mapScale = mapUtils.getMapScale(fullMapDistance);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng((locNorth + locSouth)/2, (locEast + locWest)/2),
                (float) mapScale - 0.1f));
        s = utils.long2DateDayTime(locLogs.get(0).logTime)+" ~ "+utils.long2DateDayTime(locLogs.get(locLogs.size()-1).logTime)+"   ";
        tvTimeInfo.setText(s);
        timeBegin =  locLogs.get(0).logTime;
        timeDiff = locLogs.get(locLogs.size()-1).logTime - locLogs.get(0).logTime;
        if (trackPosition != -1) {
            s = utils.minute2Text(iMinutes) + "  " + decimalComma.format(iMeters) + "m";
            tvLogInfo.setText(s);
        }
        else
            tvLogInfo.setVisibility(View.INVISIBLE);
        googleMap.getUiSettings().setCompassEnabled(true);
        tvPlace.setText(resultAddress);
        View v = findViewById(R.id.fragMap);
        v.post(new Runnable() {
            @Override
            public void run() {
                new Timer().schedule(new TimerTask() {
                    public void run() {
                        thisMap.snapshot(mapSaveShot);
                    }
                }, 600);
            }
        });
//        googleMap.setMyLocationEnabled(true);
//        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    private String buildFromToAddress(String startAddress, String mid1Address, String mid2Address, String finishAddress) {
        String result;
        String [] sAddress, m1Address, m2Address, fAddress;
        int sIdx, m1Idx, m2Idx, fIdx;

        if (startAddress.equals(mid1Address))
            mid1Address = "";
        if (startAddress.equals(mid2Address))
            mid2Address = "";
        if (startAddress.equals(finishAddress))
            finishAddress = "";
        if (mid1Address.equals(mid2Address))
            mid2Address = "";
        if (mid1Address.equals(finishAddress))
            finishAddress = "";
        if (mid2Address.equals(finishAddress))
            finishAddress = "";
        sAddress = startAddress.split(" "); sIdx = sAddress.length;
        m1Address = mid1Address.split(" "); m1Idx = m1Address.length;
        m2Address = mid2Address.split(" "); m2Idx = m2Address.length;
        fAddress = finishAddress.split(" "); fIdx = fAddress.length;

        if (sIdx> 0 && fIdx > 0) {
            squeezeAddress(sAddress, fAddress); fIdx = fAddress.length;
        }
        if (sIdx> 0 && m1Idx > 0) {
            squeezeAddress(sAddress, m1Address); m1Idx = m1Address.length;
        }
        if (sIdx> 0 && m2Idx > 0) {
            squeezeAddress(sAddress, m2Address); m2Idx = m2Address.length;
        }
        if (m1Idx> 0 && m2Idx > 0) {
            squeezeAddress(m1Address, m2Address); m2Idx = m2Address.length;
        }
        if (m2Idx> 0 && fIdx > 0) {
            squeezeAddress(m2Address, fAddress);
        }
        result = "";
        startAddress = ""; for (String s:sAddress) {startAddress += s+" ";}
        mid1Address = ""; for (String s:m1Address) {mid1Address += s+" ";}
        mid2Address = ""; for (String s:m2Address) {mid2Address += s+" ";}
        finishAddress = ""; for (String s:fAddress) {finishAddress += s+" ";}
        if (startAddress.trim().length()> 0)
            result += startAddress;
        if (mid1Address.trim().length()> 0)
            result += " > " + mid1Address;
        if (mid2Address.trim().length()> 0)
            result += " > " + mid2Address;
        if (finishAddress.trim().length()> 0)
            result += " > " + finishAddress;
        return result;
    }

    private void squeezeAddress(String [] addr1, String [] addr2) {
        for (int i = 0; i < Math.min(addr1.length, addr2.length); i++) {
            if (addr1[i].equals(addr2[i]))
                addr2[i] = "";
        }
    }

    private void drawTrackLIne(GoogleMap googleMap) {
        lineFromToLatLng = new ArrayList<>();
        lineFromToLatLng.add(new LatLng(0,0));
        lineFromToLatLng.add(new LatLng(0,0));
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.triangle);
        AnimatedColor animatedColor = new AnimatedColor(Color.MAGENTA, Color.CYAN);
        int color, width;
        for (int i = 0; i < locLogs.size()-2; i++) {
            lineFromToLatLng.set(0, new LatLng(locLogs.get(i).latitude, locLogs.get(i).longitude));
            lineFromToLatLng.set(1, new LatLng(locLogs.get(i+1).latitude, locLogs.get(i+1).longitude));
            float ratio = (float) (locLogs.get(i).logTime-timeBegin) / timeDiff;
            color = animatedColor.with(ratio);
            Bitmap colorBitmap = changeBitmapColor(bitmap, color);
            width = POLYLINE_STROKE_WIDTH_PX;
            if (i % 3 == 0)
                color = color ^ 0x03333333;
//            else if (i % 4 == 0) {
//                color = 0xFF0F0F0F;
//                width = width * 2 / 3;
//            }
            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.width(width);
            CustomCap endCap = new CustomCap(BitmapDescriptorFactory.fromBitmap(colorBitmap), 20);
            polyOptions.endCap(endCap);
            polyOptions.addAll(lineFromToLatLng);
            polyOptions.color(color);
            googleMap.addPolyline(polyOptions);
        }

        showMarker.drawStart(locLogs.get(0).latitude, locLogs.get(0).longitude, true);
        showMarker.drawFinish(locLogs.get(locLogs.size()-1).latitude, locLogs.get(locLogs.size()-1).longitude, true);
    }
    private Bitmap changeBitmapColor(Bitmap sourceBitmap, int color) {

        Bitmap resultBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0,
                sourceBitmap.getWidth() - 1, sourceBitmap.getHeight() - 1);
        Paint p = new Paint();
        ColorFilter filter = new LightingColorFilter(color, color);
        p.setColorFilter(filter);
//        image.setImageBitmap(resultBitmap);

        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(resultBitmap, 0, 0, p);
        return resultBitmap;
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
            if (cursor.getCount() < 4) {
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
            if (trackPosition >= 0) {
                new Timer().schedule(new TimerTask() {
                    public void run() {
                        thisMap.snapshot(buildMapIcon);
                    }
                }, 600);
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
            TrackLog trackLog = trackLogs.get(trackPosition);
            trackLog.setBitMap(trackMap);
            trackLog.setPlaceName(resultAddress);
            trackLogs.set(trackPosition, trackLog);
            databaseIO.trackMapPlaceUpdate(trackLog.getStartTime(), trackMap, resultAddress);
            trackAdapter.notifyItemChanged(trackPosition, trackLog);
        }
    };

    Bitmap filterBitmap(Bitmap bitMap, Bitmap routeMap) {
        int width = bitMap.getWidth();
        int height = bitMap.getHeight();
        int[] pixelsB = new int[width * height];
        int[] pixelsR = new int[width * height];
        bitMap.getPixels(pixelsB, 0, width, 0, 0, width, height);
        routeMap.getPixels(pixelsR, 0, width, 0, 0, width, height);
        for (int x = 0; x < pixelsR.length; ++x) {
            if (pixelsB[x] == pixelsR[x])
                pixelsR[x] = pixelsR[x] & 0xAAFFFFFF; // 0xFFCCDDCC;       // grey out
        }
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.setPixels(pixelsR, 0, width, 0, 0, width, height);
        return result;
    }

}
