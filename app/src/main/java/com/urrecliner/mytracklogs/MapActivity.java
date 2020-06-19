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
import android.util.Log;
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
import static com.urrecliner.mytracklogs.Vars.speedColor;
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
    double lowSpeed, highSpeed, speed;
    ArrayList<LatLng> lineFromToLatLng;
    ArrayList<LocLog> locLogs;
    TextView tvTimeInfo, tvLogInfo, tvPlace;
    SupportMapFragment mapFragment;
    ArrayList<String[]> places;
    Geocoder geocoder;
    GPS2Address gps2Address;

    static class LocLog {
        private long logTime;
        private double latitude, longitude, speed;

        LocLog(long logTime, double latitude, double longitude, double speed) {
            this.logTime = logTime;
            this.latitude = latitude;
            this.longitude = longitude;
            this.speed = speed;
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
        gps2Address = new GPS2Address();
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
        places= new ArrayList<String[]>();
        if (showMarker == null)
            showMarker = new ShowMarker();
        showMarker.init(mapActivity, googleMap);
        String s = "";
        if (retrieveDBLog()) return;
        geocoder = new Geocoder(this, Locale.getDefault());
        double fullMapDistance = mapUtils.getFullMapDistance(locEast, locWest, locSouth, locNorth);
        int mapScale = mapUtils.getMapScale(fullMapDistance);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng((locNorth + locSouth)/2, (locEast + locWest)/2),
                (float) mapScale - 0.1f));
        s = utils.long2DateDayTime(locLogs.get(0).logTime)+" ~ "+utils.long2DateDayTime(locLogs.get(locLogs.size()-1).logTime)+"   ";
        tvTimeInfo.setText(s);
        timeBegin =  locLogs.get(0).logTime;
        timeDiff = locLogs.get(locLogs.size()-1).logTime - locLogs.get(0).logTime;
        utils.log("Veryf trackPos",trackPosition+" ```");
        if (iMinutes > 0) {
            s = utils.minute2Text(iMinutes) + "  " + decimalComma.format(iMeters) + "m";
            tvLogInfo.setText(s);
        }
        else
            tvLogInfo.setVisibility(View.INVISIBLE);
        googleMap.getUiSettings().setCompassEnabled(true);
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

    private String buildFromToAddress() {
        String result = "";
//        for (int i = 0; i < places.size(); i++)
//            Log.e("org place "+i, places.get(i)[0]+"*"+places.get(i)[1]+"*"+places.get(i)[2]+"*"+places.get(i)[3]+"*"+places.get(i)[4]+"*");
        if (places.size() == 0)
            return "";
        result = places.get(0)[0];
        if (!places.get(0)[1].equals(" ")) result += " "+places.get(0)[1];
        if (!places.get(0)[2].equals(" ")) result += " "+places.get(0)[2];
        if (!places.get(0)[3].equals(" ")) result += " "+places.get(0)[3];
        if (!places.get(0)[4].equals(" ")) result += " "+places.get(0)[4];
        for (int i = places.size()-1; i > 0; i--) {
            if (places.get(i)[0].equals(places.get(i-1)[0]))
                places.set(i, new String[]{" ", places.get(i)[1], places.get(i)[2], places.get(i)[3], places.get(i)[4]});
            if (places.get(i)[1].equals(places.get(i-1)[1]))
                places.set(i, new String[]{places.get(i)[0]," ",  places.get(i)[2], places.get(i)[3], places.get(i)[4]});
            if (places.get(i)[2].equals(places.get(i-1)[2]))
                places.set(i, new String[]{places.get(i)[0], places.get(i)[1], " ", places.get(i)[3], places.get(i)[4]});
            if (places.get(i)[3].equals(places.get(i-1)[3]))
                places.set(i, new String[]{places.get(i)[0], places.get(i)[1], places.get(i)[2], " ", places.get(i)[4]});
            if (places.get(i)[4].equals(places.get(i-1)[4]))
            places.set(i, new String[]{places.get(i)[0], places.get(i)[1], places.get(i)[2], places.get(i)[3], " "});
        }

        for (int i = 1; i < places.size(); i++) {
            String newPlace = "";
            if (!places.get(i)[0].equals(" ")) newPlace += places.get(i)[0];
            if (!places.get(i)[1].equals(" ")) newPlace += " "+places.get(i)[1];
            if (!places.get(i)[2].equals(" ")) newPlace += " "+places.get(i)[2];
            if (!places.get(i)[3].equals(" ")) newPlace += " "+places.get(i)[3];
            if (!places.get(i)[4].equals(" ")) newPlace += " "+places.get(i)[4];
            if (newPlace.length() > 0)
                result += " > " + newPlace;
        }

        return result;
    }

    private void drawTrackLIne(GoogleMap googleMap) {
        lineFromToLatLng = new ArrayList<>();
        lineFromToLatLng.add(new LatLng(0,0));
        lineFromToLatLng.add(new LatLng(0,0));
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.triangle);
        CustomCap customCap;
        int color, j = -1;
        for (int i = 0; i < locLogs.size()-2; i++) {
//            utils.log("locLogs",i+" speed="+locLogs.get(i).speed+" "+locLogs.get(i).latitude+" x "+locLogs.get(i).longitude);
            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.width(POLYLINE_STROKE_WIDTH_PX);
            if (locLogs.get(i).speed == -1) {
                utils.log("i is -1"," this is zero "+i);
                lineFromToLatLng.set(0, new LatLng(locLogs.get(i + 1).latitude, locLogs.get(i + 1).longitude));
                lineFromToLatLng.set(1, new LatLng(locLogs.get(i + 2).latitude, locLogs.get(i + 2).longitude));
                color = speedColor[0];
                customCap = new CustomCap(BitmapDescriptorFactory.fromResource(R.mipmap.marker_start), 18);
                polyOptions.startCap(customCap);
                j = i+1;
                places.add(gps2Address.getPlace(geocoder, locLogs.get(j).latitude, locLogs.get(j).longitude));
            }
            else if (locLogs.get(i+1).speed == -1) {
                utils.log("i+1 is -1"," next is zero"+i);
                lineFromToLatLng.set(0, new LatLng(locLogs.get(i-1).latitude, locLogs.get(i-1).longitude));
                lineFromToLatLng.set(1, new LatLng(locLogs.get(i).latitude, locLogs.get(i).longitude));
                color = speedColor[0];
                customCap = new CustomCap(BitmapDescriptorFactory.fromResource(R.mipmap.marker_finish), 18);
                polyOptions.endCap(customCap);
                if (j != 0) {
                    j = (i+j) / 2;
                    places.add(gps2Address.getPlace(geocoder, locLogs.get(j).latitude, locLogs.get(j).longitude));
                }
                places.add(gps2Address.getPlace(geocoder, locLogs.get(i).latitude, locLogs.get(i).longitude));
            }
            else {
                color = speedColor[(int) ((locLogs.get(i).speed - lowSpeed) / highSpeed * 100)];
                Bitmap colorBitmap = changeBitmapColor(bitmap, color);
                customCap = new CustomCap(BitmapDescriptorFactory.fromBitmap(colorBitmap), 20);
                polyOptions.endCap(customCap);
                lineFromToLatLng.set(0, new LatLng(locLogs.get(i).latitude, locLogs.get(i).longitude));
                lineFromToLatLng.set(1, new LatLng(locLogs.get(i + 1).latitude, locLogs.get(i + 1).longitude));
            }
            polyOptions.addAll(lineFromToLatLng);
            polyOptions.color(color);
            googleMap.addPolyline(polyOptions);
        }

        showMarker.drawStart(locLogs.get(0).latitude, locLogs.get(0).longitude, true);
        showMarker.drawFinish(locLogs.get(locLogs.size()-1).latitude, locLogs.get(locLogs.size()-1).longitude, true);
        places.add(gps2Address.getPlace(geocoder, locLogs.get(locLogs.size()-1).latitude, locLogs.get(locLogs.size()-1).longitude));
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

        LocLog prevLog, nowLog;
        highSpeed = 0; lowSpeed = 99999999f;
        locSouth = 999; locNorth = -999; locWest = 999; locEast = -999;
        locLogs = new ArrayList<>();

        utils.log(logID,"start of retrieve");
        Cursor cursor = databaseIO.logGetFromTo(startTime, finishTime);
        if (cursor == null) {
            Toast.makeText(mContext,"No log data to display ",Toast.LENGTH_LONG).show();
            utils.log("no data",utils.long2DateDay(startTime)+" "+utils.long2Time(startTime)+" ~ "+utils.long2DateDay(finishTime)+" "+utils.long2Time(finishTime));
            return true;
        }
        utils.log(logID, "count ="+cursor.getCount());
        if (cursor.getCount() < 5) {
            Toast.makeText(mapActivity,"자료가 너무 작음("+cursor.getCount()+"), 삭제 요망",Toast.LENGTH_LONG).show();
            return true;
        }
        if (cursor.moveToFirst()) {
            prevLog = cursor2Log(cursor);
            if (prevLog.speed == -1) {
                locLogs.add(prevLog);
                if (cursor.moveToNext())
                    prevLog = cursor2Log(cursor);
            }
            cursor.moveToNext();
            do {
                nowLog = cursor2Log(cursor);
                if (nowLog.speed == -1) {
                    locLogs.add(nowLog);
                    if(cursor.moveToNext())
                        prevLog = cursor2Log(cursor);
                }
                else {
                    locNorth = Math.max(nowLog.latitude, locNorth);
                    locSouth = Math.min(nowLog.latitude, locSouth);
                    locEast = Math.max(nowLog.longitude, locEast);
                    locWest = Math.min(nowLog.longitude, locWest);
                    long deltaTime = nowLog.logTime - prevLog.logTime;
                    double deltaDistance = mapUtils.getShortDistance(prevLog.latitude, prevLog.longitude, nowLog.latitude, nowLog.longitude);
                    double deltaSpeed = deltaDistance / (double) deltaTime * 1000f * 60f;
                    nowLog.speed = deltaSpeed;
                    locLogs.add(nowLog);
                    highSpeed = Math.max(highSpeed, deltaSpeed);
                    lowSpeed = Math.min(lowSpeed, deltaSpeed);
                    prevLog = new LocLog(nowLog.logTime, nowLog.latitude, nowLog.longitude, deltaSpeed);
                }
            } while (cursor.moveToNext());
            locLogs.add(nowLog);
            utils.log("check@@","@@ // lowSpeed="+lowSpeed+", highSpeed="+highSpeed);
        }
        return false;
    }

    private LocLog cursor2Log(Cursor cursor) {
         double lat = cursor.getDouble(cursor.getColumnIndex("latitude"));
         double lon  = cursor.getDouble(cursor.getColumnIndex("longitude"));
         long  time = cursor.getLong(cursor.getColumnIndex("logTime"));
         return new LocLog(time, lat, lon, (lat == 0 && lon == 0) ? -1:0);
    }

    Bitmap pureMap = null, trackMap = null;
    GoogleMap.SnapshotReadyCallback mapSaveShot = new GoogleMap.SnapshotReadyCallback() {

        @Override
        public void onSnapshotReady(Bitmap snapshot) {      // save map to bitmap without tracklog
//            utils.log(logID," snapShot "+ reDrawCount);
            pureMap = Bitmap.createScaledBitmap(snapshot, iconWidth, iconHeight, false);
            drawTrackLIne(thisMap);
            resultAddress = buildFromToAddress();
            TextView tvPlaces = findViewById(R.id.logPlace);
            tvPlaces.setText(resultAddress);
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
