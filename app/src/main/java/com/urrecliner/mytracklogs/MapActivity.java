package com.urrecliner.mytracklogs;

import android.app.Activity;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.mytracklogs.Vars.HIGH_SPEED_DRIVE;
import static com.urrecliner.mytracklogs.Vars.HIGH_SPEED_WALK;
import static com.urrecliner.mytracklogs.Vars.LOW_SPEED_DRIVE;
import static com.urrecliner.mytracklogs.Vars.LOW_SPEED_WALK;
import static com.urrecliner.mytracklogs.Vars.databaseIO;
import static com.urrecliner.mytracklogs.Vars.decimalComma;
import static com.urrecliner.mytracklogs.Vars.mContext;
import static com.urrecliner.mytracklogs.Vars.mapUtils;
import static com.urrecliner.mytracklogs.Vars.showMarker;
import static com.urrecliner.mytracklogs.Vars.speedColor;
import static com.urrecliner.mytracklogs.Vars.trackAdapter;
import static com.urrecliner.mytracklogs.Vars.trackLogs;
import static com.urrecliner.mytracklogs.Vars.trackPosition;
import static com.urrecliner.mytracklogs.Vars.utils;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int POLYLINE_STROKE_WIDTH_PX_WALK = 16;
    private static final int POLYLINE_STROKE_WIDTH_PX_DRIVE = 20;
//    private static final int PATTERN_DASH_LENGTH_PX = 6;
//    private static final int PATTERN_GAP_LENGTH_PX = 6;
//    private static final PatternItem DOT = new Dot();
//    private static final PatternItem DASH = new Dash(PATTERN_DASH_LENGTH_PX);
//    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);
//    private static final List<PatternItem> PATTERN_POLYLINE_MINE = Arrays.asList(DASH, GAP);
    GoogleMap thisMap;
    private String resultAddress;
    double locSouth, locNorth, locWest, locEast;
    private Activity mapActivity;
    private long startTime;
    private long finishTime;
    private boolean isWalk;
    private int iMinutes;
    private int iMeters;
    private int iconWidth;
    private int iconHeight;
    float lowSpeed, highSpeed, lowSqrt, highSqrt;
    ArrayList<LatLng> lineLatLng;
    ArrayList<LocLog> locLogs;
    TextView tvTimeInfo, tvLogInfo;
    SupportMapFragment mapFragment;
    ArrayList<String[]> places;
    Geocoder geocoder;
    GPS2Address gps2Address;
    int polyWidth;
    double totDistance = 0;

    static class LocLog {
        long logTime;
        double latitude, longitude, speed;

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
        int iWalkDrive = trackLog.getWalkDrive(); isWalk = iWalkDrive == 0;
        iMinutes = trackLog.getMinutes();
        iMeters = trackLog.getMeters();
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragMap);
        mapFragment.getMapAsync(this);
        tvTimeInfo =  findViewById(R.id.timeSummary);
        tvLogInfo =  findViewById(R.id.logSummary);
        gps2Address = new GPS2Address();
        ImageView iv = findViewById(R.id.smallMap);
        iv.setOnClickListener(view -> view.setVisibility(View.INVISIBLE));
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {

        thisMap = googleMap;
        places= new ArrayList<>();
        if (showMarker == null)
            showMarker = new ShowMarker();
        polyWidth = (isWalk)? POLYLINE_STROKE_WIDTH_PX_WALK : POLYLINE_STROKE_WIDTH_PX_DRIVE;
        showMarker.init(mapActivity, googleMap);
        String s;
        if (retrieveDBLog()) return;
        geocoder = new Geocoder(this, Locale.getDefault());
        double fullMapDistance = mapUtils.calcDistance(locSouth, locEast, locNorth, locWest);
        float mapScale = mapUtils.getMapScale(fullMapDistance);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng((locNorth + locSouth)/2, (locEast + locWest)/2), mapScale - 0.1f));
        s = utils.long2DateDayTime(locLogs.get(0).logTime)+" ~ "+utils.long2DateDayTime(locLogs.get(locLogs.size()-1).logTime)+"   ";
        tvTimeInfo.setText(s);
        if (iMinutes > 0) {
            s = utils.minute2Text(iMinutes) + (isWalk ? " Walk  ":" Drive  ") + decimalComma.format(iMeters) + "m";
            tvLogInfo.setText(s);
        }
        else
            tvLogInfo.setVisibility(View.INVISIBLE);
        googleMap.getUiSettings().setCompassEnabled(true);
        View v = findViewById(R.id.fragMap);
        v.post(() -> new Timer().schedule(new TimerTask() {
            public void run() {
                thisMap.snapshot(mapSaveShot);
            }
        }, 600));
//        googleMap.setMyLocationEnabled(true);
//        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    private String buildFromToAddress() {
//        for (int i = 0; i < places.size(); i++)
//            Log.e("org place "+i, places.get(i)[0]+"*"+places.get(i)[1]+"*"+places.get(i)[2]+"*"+places.get(i)[3]+"*"+places.get(i)[4]+"*");
        if (places.size() == 0)
            return "";

        StringBuilder result = new StringBuilder(places.get(0)[0]);
        if (!places.get(0)[1].equals(" ")) result.append(" ").append(places.get(0)[1]);
        if (!places.get(0)[2].equals(" ")) result.append(" ").append(places.get(0)[2]);
        if (!places.get(0)[3].equals(" ")) result.append(" ").append(places.get(0)[3]);
        if (!places.get(0)[4].equals(" ")) result.append(" ").append(places.get(0)[4]);
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
            StringBuilder newPlace = new StringBuilder();
            if (!places.get(i)[0].equals(" ")) newPlace.append(places.get(i)[0]);
            if (!places.get(i)[1].equals(" ")) newPlace.append(" ").append(places.get(i)[1]);
            if (!places.get(i)[2].equals(" ")) newPlace.append(" ").append(places.get(i)[2]);
            if (!places.get(i)[3].equals(" ")) newPlace.append(" ").append(places.get(i)[3]);
            if (!places.get(i)[4].equals(" ")) newPlace.append(" ").append(places.get(i)[4]);
            if (newPlace.length() > 0)
                result.append(" > ").append(newPlace);
        }

        return result.toString();
    }

    private void drawTrackLIne(GoogleMap googleMap) {
        lineLatLng = new ArrayList<>();
        lineLatLng.add(new LatLng(0,0));
        lineLatLng.add(new LatLng(0,0));
        Bitmap triangleMap = BitmapFactory.decodeResource(getResources(), R.mipmap.triangle);
        CustomCap customCap;
        totDistance = 0;
        double distance, maxDistance = 0, maxSpeed = 0;
        int j = -1;
        lowSpeed = (float) ((isWalk) ? LOW_SPEED_WALK: LOW_SPEED_DRIVE);
        highSpeed = (float) ((isWalk) ? HIGH_SPEED_WALK: HIGH_SPEED_DRIVE);
        lowSqrt = (float) ((isWalk) ? Math.sqrt(LOW_SPEED_WALK): Math.sqrt(LOW_SPEED_DRIVE));
        highSqrt = (float) ((isWalk) ? Math.sqrt(HIGH_SPEED_WALK): Math.sqrt(HIGH_SPEED_DRIVE));

        for (int i = 0; i < locLogs.size()-2; i++) {
            if (i == 0)
                distance = 0;
            else {
                distance = mapUtils.calcDistance(locLogs.get(i).latitude, locLogs.get(i).longitude, locLogs.get(i + 1).latitude, locLogs.get(i + 1).longitude);
                totDistance += distance;
                if (maxDistance < distance)
                    maxDistance = distance;
            }
            double speed = distance / (double)(locLogs.get(i+1).logTime-locLogs.get(i).logTime) * 60000f;
            if (speed > maxSpeed)
                maxSpeed = speed;
//            utils.log("totDistance",i+""+totDistance+" maxDist="+maxDistance+" maxSpeed="+maxSpeed);
//            utils.log("locLogs^",i+", "+locLogs.get(i).speed+", "+speed+", "+distance+", "+locLogs.get(i).latitude+", "+locLogs.get(i).longitude);
            int color = (int) (Math.sqrt(speed)/highSqrt*20);
            if (color > 20) color = 20; if(color < 0) color = 0;
            if (isWalk) color = 20 - color; // red to green (drive), green to red (walk)
            int colorCode = speedColor[color];
            int capColor = colorCode ^ 0x222222;
            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.width(polyWidth);
            if (locLogs.get(i).speed == -1) {
//                utils.log("i is -1"," this is zero "+i);
                lineLatLng.set(0, new LatLng(locLogs.get(i + 1).latitude, locLogs.get(i + 1).longitude));
                lineLatLng.set(1, new LatLng(locLogs.get(i + 2).latitude, locLogs.get(i + 2).longitude));
                customCap = new CustomCap(BitmapDescriptorFactory.fromResource(R.mipmap.marker_start), 18);
                polyOptions.startCap(customCap);
                j = i+1;
                places.add(gps2Address.getPlace(geocoder, locLogs.get(j).latitude, locLogs.get(j).longitude));
            }
            else if (locLogs.get(i+1).speed == -1) {
                utils.log("i+1 is -1"," next is zero"+i);
                lineLatLng.set(0, new LatLng(locLogs.get(i-1).latitude, locLogs.get(i-1).longitude));
                lineLatLng.set(1, new LatLng(locLogs.get(i).latitude, locLogs.get(i).longitude));
                customCap = new CustomCap(BitmapDescriptorFactory.fromResource(R.mipmap.marker_finish), 12);
                polyOptions.endCap(customCap);
                if (j != 0) {
                    j = (i+j) / 2;
                    places.add(gps2Address.getPlace(geocoder, locLogs.get(j).latitude, locLogs.get(j).longitude));
                }
                places.add(gps2Address.getPlace(geocoder, locLogs.get(i).latitude, locLogs.get(i).longitude));
            }
            else {
                Bitmap capColormap = utils.changeBitmapColor(triangleMap, capColor);
                customCap = new CustomCap(BitmapDescriptorFactory.fromBitmap(capColormap),24); // big number small icon
                polyOptions.endCap(customCap);
                lineLatLng.set(0, new LatLng(locLogs.get(i).latitude, locLogs.get(i).longitude));
                lineLatLng.set(1, new LatLng(locLogs.get(i+1).latitude, locLogs.get(i+1).longitude));
            }
            polyOptions.addAll(lineLatLng);
            polyOptions.color(colorCode);
            googleMap.addPolyline(polyOptions);
        }
        utils.log("RESULT","MaxDistance is "+maxDistance+" maxspeed = "+maxSpeed);

        showMarker.drawStart(locLogs.get(0).latitude, locLogs.get(0).longitude, false);
        showMarker.drawFinish(locLogs.get(locLogs.size()-1).latitude, locLogs.get(locLogs.size()-1).longitude, false);
        places.add(gps2Address.getPlace(geocoder, locLogs.get(locLogs.size()-1).latitude, locLogs.get(locLogs.size()-1).longitude));
    }

    private boolean retrieveDBLog() {

        LocLog prevLog, nowLog;
        highSpeed = 0; lowSpeed = 99999999f;
        locSouth = 999; locNorth = -999; locWest = 999; locEast = -999;
        locLogs = new ArrayList<>();

        String logID = "Map";
        utils.log(logID,"start of retrieve");
        Cursor cursor = databaseIO.logGetFromTo(startTime, finishTime);
        if (cursor == null) {
            Toast.makeText(mContext,"No log data to display ",Toast.LENGTH_LONG).show();
            utils.log("no data",utils.long2DateDay(startTime)+" "+utils.long2Time(startTime)+" ~ "+utils.long2DateDay(finishTime)+" "+utils.long2Time(finishTime));
            return true;
        }
        utils.log(logID, "count ="+cursor.getCount());
        if (cursor.getCount() < 10) {
            Toast.makeText(mapActivity,"자료가 너무 작음("+cursor.getCount()+"), 삭제 요망",Toast.LENGTH_LONG).show();
            return true;
        }
        if (cursor.moveToFirst()) {
            prevLog = cursor2LocLog(cursor);
            if (prevLog.speed == -1) {
                locLogs.add(prevLog);
                if (cursor.moveToNext())
                    prevLog = cursor2LocLog(cursor);
            }
            cursor.moveToNext();
            do {
                nowLog = cursor2LocLog(cursor);
                if (nowLog.speed == -1) {
                    locLogs.add(nowLog);
                    if(cursor.moveToNext())
                        prevLog = cursor2LocLog(cursor);
                }
                else {
                    locNorth = Math.max(nowLog.latitude, locNorth);
                    locSouth = Math.min(nowLog.latitude, locSouth);
                    locEast = Math.max(nowLog.longitude, locEast);
                    locWest = Math.min(nowLog.longitude, locWest);
                    long deltaTime = nowLog.logTime - prevLog.logTime;
                    float deltaDistance = mapUtils.calcDistance(prevLog.latitude, prevLog.longitude, nowLog.latitude, nowLog.longitude);
                    float deltaSpeed = deltaDistance / (float) deltaTime * 1000f * 60f;
                    nowLog.speed = deltaSpeed;
                    locLogs.add(nowLog);
                    highSpeed = Math.max(highSpeed, deltaSpeed);
                    lowSpeed = Math.min(lowSpeed, deltaSpeed);
                    prevLog = new LocLog(nowLog.logTime, nowLog.latitude, nowLog.longitude, deltaSpeed);
                }
            } while (cursor.moveToNext());
            locLogs.add(nowLog);
            utils.log("check@@","@@ after read all db // lowSpeed="+lowSpeed+", highSpeed="+highSpeed);
        }
        return false;
    }

    private LocLog cursor2LocLog(Cursor cursor) {
         double lat = cursor.getDouble(cursor.getColumnIndex("latitude"));
         double lng  = cursor.getDouble(cursor.getColumnIndex("longitude"));
         long  time = cursor.getLong(cursor.getColumnIndex("logTime"));
         return new LocLog(time, lat, lng, (lat == 0 && lng == 0) ? -1:0);
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
            String s = utils.minute2Text(iMinutes) + (isWalk ? " Walk  ":" Drive  ") +
                    decimalComma.format((int) totDistance) + "m / "+locLogs.size();
            tvLogInfo.setText(s);
            TrackLog trackLog = trackLogs.get(trackPosition);
            trackLog.setBitMap(trackMap);
            trackLog.setPlaceName(resultAddress);
            trackLog.setMeters((int) totDistance);
            trackLogs.set(trackPosition, trackLog);
            databaseIO.trackMapPlaceUpdate(trackLog.getStartTime(), (int) totDistance, trackMap, resultAddress);
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
