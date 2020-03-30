package com.urrecliner.mytracklogs;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import static com.urrecliner.mytracklogs.Vars.trackAdapter;
import static com.urrecliner.mytracklogs.Vars.trackLogs;
import static com.urrecliner.mytracklogs.Vars.utils;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final String logID = "Map";
    private static final int POLYLINE_STROKE_WIDTH_PX = 6;
    private static final int PATTERN_DASH_LENGTH_PX = 6;
    private static final int PATTERN_GAP_LENGTH_PX = 6;
    private static final PatternItem DOT = new Dot();
    private static final PatternItem DASH = new Dash(PATTERN_DASH_LENGTH_PX);
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);

    private static final List<PatternItem> PATTERN_POLYLINE_MINE = Arrays.asList(DASH, GAP);

    private Activity mapActivity;
    private long startTime, finishTime, mapStartTime, mapFinishTime, thisTime;
    private int iMinutes, iMeters, position;
    ArrayList<LatLng> listLatLng;
    ArrayList<Float> listAngle;
    ArrayList<LocLog> locLogs;
    static class LocLog {
        private long logTime;
        private double latitude, longitude;

        LocLog(long logTime, double latitude, double longitude) {
            this.logTime = logTime;
            this.latitude = latitude;
            this.longitude = longitude;
        }
        long getLogTime() { return logTime; }
        double getLatitude() { return latitude; }
        double getLongitude() { return longitude; }
    }

    double distanceGap, totalDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mapActivity = this;
        startTime = getIntent().getLongExtra("startTime",0);
        finishTime = getIntent().getLongExtra("finishTime",0);
        iMinutes = getIntent().getIntExtra("minutes",0);
        iMeters = getIntent().getIntExtra("meters",0);
        position = getIntent().getIntExtra("position",-1);

        ActionBar ab = this.getSupportActionBar();
        ab.setTitle(" 지도");
        ab.setIcon(R.mipmap.my_face) ;
        ab.setDisplayUseLogoEnabled(true) ;
        ab.setDisplayShowHomeEnabled(true) ;

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragMap);
        mapFragment.getMapAsync(this);

    }

    GoogleMap nowMap;
    @Override
    public void onMapReady(GoogleMap googleMap) {

        nowMap = googleMap;
        String s;
        if (retrieveDBLog()) return;

        double fullMapDistance = mapUtils.getFullMapDistance();
        int mapScale = mapUtils.getMapScale(fullMapDistance);
        distanceGap = fullMapDistance / 100;   // to ignore mark if less than this distance
//        utils.log("distance"," Org is "+distanceGap+" total is "+totalDistance);
        distanceGap = Math.min(totalDistance/locLogs.size(), distanceGap);
//        utils.log("distance"," min max is "+distanceGap);
        utils.log(logID, " x "+(locWest-locEast)/2+" y"+(locNorth-locSouth)/2+" dist="+fullMapDistance);

        buildMarkerPositions();

        PolylineOptions polyOptions = new PolylineOptions();
        polyOptions.color(getColor(R.color.trackRoute));
        polyOptions.width(POLYLINE_STROKE_WIDTH_PX);
        polyOptions.pattern(PATTERN_POLYLINE_MINE);
        polyOptions.addAll(listLatLng);

        googleMap.addPolyline(polyOptions);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng((locNorth + locSouth)/2, (locEast + locWest)/2), mapScale));

        Bitmap spriteOriginal = BitmapFactory.decodeResource(this.getResources(), R.mipmap.triangle);
        final int markScale = 24;
        final Bitmap sprite = Bitmap.createScaledBitmap(spriteOriginal, markScale, markScale, false);
        for (int idx = 0; idx < listAngle.size(); idx++) {
            Matrix matrix = new Matrix();
            matrix.preRotate(listAngle.get(idx));///in degree
            Bitmap mBitmap = Bitmap.createBitmap(sprite, 0, 0, markScale, markScale, matrix, true);
            googleMap.addMarker(new MarkerOptions().position(listLatLng.get(idx)).icon(BitmapDescriptorFactory.fromBitmap(mBitmap)).anchor(0.5f, 0.5f));
        }

        googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(listLatLng.get(0).latitude, listLatLng.get(0).longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker_start)));
        googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(listLatLng.get(listLatLng.size()-1).latitude, listLatLng.get(listLatLng.size()-1).longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker_finish)));

        googleMap.getUiSettings().setCompassEnabled(true);
//        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.getUiSettings().setTiltGesturesEnabled(false);

        TextView tvTimeInfo = findViewById(R.id.timeInfo);
        s = utils.long2DateDayTime(locLogs.get(0).logTime)+"~"+utils.long2DateDayTime(locLogs.get(locLogs.size()-1).logTime);
        tvTimeInfo.setText(s);
        if (iMinutes > 0) {
            s = utils.minute2Text(iMinutes) + "  " + decimalComma.format(iMeters) + "m";
            TextView tvLog = findViewById(R.id.logInfo);
            tvLog.setText(s);
        }
    }

    private boolean retrieveDBLog() {

        utils.log(logID,"start of retrieve");
        Cursor cursor = databaseIO.logGetFromTo(startTime, finishTime);
        if (cursor != null) {
            if (cursor.getCount() < 10) {
                Toast.makeText(mapActivity,"자료가 너무 작음("+cursor.getCount()+"), 삭제 요망",Toast.LENGTH_LONG).show();
                return true;
            }
            utils.log(logID, "count ="+cursor.getCount());
            if (cursor.moveToFirst()) {
                locSouth = 999; locNorth = -999; locWest = 999; locEast = -999;
                locLogs = new ArrayList<>();
                totalDistance = 0;

                prevLatitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
                prevLongitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
                do {
                    thisTime = cursor.getLong(cursor.getColumnIndex("logTime"));
                    nowLatitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
                    nowLongitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
                    locLogs.add(new LocLog(thisTime, nowLatitude, nowLongitude));
                    if (nowLatitude > locNorth) locNorth = nowLatitude;
                    if (nowLatitude < locSouth) locSouth = nowLatitude;
                    if (nowLongitude > locEast) locEast = nowLongitude;
                    if (nowLongitude < locWest) locWest = nowLongitude;
                    totalDistance += mapUtils.getShortDistance();
                } while (cursor.moveToNext());
            }
        }
        else {
            Toast.makeText(mContext,"No log data to display ",Toast.LENGTH_LONG).show();
            utils.log("no data",utils.long2DateDay(startTime)+" "+utils.long2Time(startTime)+" ~ "+utils.long2DateDay(finishTime)+" "+utils.long2Time(finishTime));
            return true;
        }

        utils.log("cursor","W"+ locWest +" E"+ locEast +" S"+ locSouth +" N"+ locNorth);
        return false;
    }

    private void buildMarkerPositions() {

        LocLog locLogFrom, locLogTo;
        listLatLng = new ArrayList<>();
        listAngle = new ArrayList<>();

        int dbCount = locLogs.size();
        utils.log("dbCount","="+dbCount);
        locLogFrom = locLogs.get(0);
//        locLogTo = locLogs.get(dbCount-1);

//        mapStartTime = locLogFrom.getLogTime();
//        mapFinishTime = locLogs.get(dbCount-1).logTime;
//        markInterval = (mapFinishTime - mapStartTime) / dbCount;
//        utils.log(logID, "interval = "+markInterval);
//        markInterval = Math.min(markInterval, 20*60*100);
//        long prevTime = locLogFrom.getLogTime();

        prevLatitude = locLogFrom.getLatitude();
        prevLongitude = locLogFrom.getLongitude();
        for (LocLog ll: locLogs) {
//            long thisTime = ll.logTime;
            nowLatitude = ll.latitude;
            nowLongitude = ll.longitude;
            float angle = calcDirection(prevLatitude, prevLongitude, nowLatitude, nowLongitude);
            if (!Float.isNaN(angle)) {      // if angle is valid
                double distance = mapUtils.getShortDistance();
                if (distance > distanceGap) {    // if not so near
                    listLatLng.add(new LatLng(nowLatitude, nowLongitude));
                    listAngle.add(angle);
                    prevLatitude = nowLatitude;
                    prevLongitude = nowLongitude;
                }
            }
            else
                utils.log(logID, "angle is NaN ");
        }
        utils.log(logID, "result marks = "+listAngle.size());
    }

    Menu mapMenu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mapMenu = menu;
        getMenuInflater().inflate(R.menu.map_menu, menu);
        MenuItem item = menu.findItem(R.id.deleteLog);
        item.setVisible(!(position == -1));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.deleteLog) {
            TrackLog trackLog = trackLogs.get(position);
            long fromTime = trackLog.getStartTime();
            long toTime = trackLog.getFinishTime();
            databaseIO.trackDelete(fromTime);
            databaseIO.logDeleteFromTo(fromTime, toTime);
            trackLogs.remove(position);
            trackAdapter.notifyItemRemoved(position);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private float calcDirection(double P1_latitude, double P1_longitude, double P2_latitude, double P2_longitude)
    {
        final double CONSTANT2RADIAN = (3.141592 / 180);
        final double CONSTANT2DEGREE = (180 / 3.141592);

        // 현재 위치 : 위도나 경도는 지구 중심을 기반으로 하는 각도이기 때문에 라디안 각도로 변환한다.
        double Cur_Lat_radian = P1_latitude * CONSTANT2RADIAN;
        double Cur_Lon_radian = P1_longitude * CONSTANT2RADIAN;

        // 목표 위치 : 위도나 경도는 지구 중심을 기반으로 하는 각도이기 때문에 라디안 각도로 변환한다.
        double Dest_Lat_radian = P2_latitude * CONSTANT2RADIAN;
        double Dest_Lon_radian = P2_longitude * CONSTANT2RADIAN;

        // radian distance
        double radian_distance =
                Math.acos(Math.sin(Cur_Lat_radian) * Math.sin(Dest_Lat_radian) + Math.cos(Cur_Lat_radian) * Math.cos(Dest_Lat_radian) * Math.cos(Cur_Lon_radian - Dest_Lon_radian));

        // 목적지 이동 방향을 구한다.(현재 좌표에서 다음 좌표로 이동하기 위해서는 방향을 설정해야 한다. 라디안 값임
        double radian_bearing = Math.acos((Math.sin(Dest_Lat_radian) - Math.sin(Cur_Lat_radian) * Math.cos(radian_distance)) / (Math.cos(Cur_Lat_radian) * Math.sin(radian_distance)));        // acos의 인수로 주어지는 x는 360분법의 각도가 아닌 radian(호도)값이다.

        double true_bearing;
        if (Math.sin(Dest_Lon_radian - Cur_Lon_radian) < 0)
            true_bearing = 360 - radian_bearing * CONSTANT2DEGREE;
        else
            true_bearing = radian_bearing * CONSTANT2DEGREE;
        return (float) true_bearing;
    }


}
