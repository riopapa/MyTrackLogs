package com.urrecliner.mytracklogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
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
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
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
import static com.urrecliner.mytracklogs.Vars.showMarker;
import static com.urrecliner.mytracklogs.Vars.trackActivity;
import static com.urrecliner.mytracklogs.Vars.trackAdapter;
import static com.urrecliner.mytracklogs.Vars.trackLogs;
import static com.urrecliner.mytracklogs.Vars.utils;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final String logID = "Map";
    private static final int POLYLINE_STROKE_WIDTH_PX = 6;
    private static final int PATTERN_DASH_LENGTH_PX = 6;
    private static final int PATTERN_GAP_LENGTH_PX = 6;
//    private static final PatternItem DOT = new Dot();
    private static final PatternItem DASH = new Dash(PATTERN_DASH_LENGTH_PX);
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);

    private static final List<PatternItem> PATTERN_POLYLINE_MINE = Arrays.asList(DASH, GAP);

    private Activity mapActivity;
    private long startTime, finishTime;
    private int iMinutes, iMeters, position;
    ArrayList<LatLng> lineFromToLatLng;
    ArrayList<LocLog> locLogs;
    static class LocLog {
        private long logTime;
        private double latitude, longitude;

        LocLog(long logTime, double latitude, double longitude) {
            this.logTime = logTime;
            this.latitude = latitude;
            this.longitude = longitude;
        }
        double getLatitude() { return latitude; }
        double getLongitude() { return longitude; }
    }

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
        ab.setTitle(R.string.review_map);
        ab.setIcon(R.mipmap.my_face);
        ab.setDisplayUseLogoEnabled(true);
        ab.setDisplayShowHomeEnabled(true);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragMap);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        showMarker.init(mapActivity, googleMap);
        String s;
        if (retrieveDBLog()) return;

        double fullMapDistance = mapUtils.getFullMapDistance();
        int mapScale = mapUtils.getMapScale(fullMapDistance);

//        showMarker.init(mapActivity, googleMap);
        CustomCap endCap = new CustomCap(
                BitmapDescriptorFactory.fromResource(R.mipmap.triangle), 10);
        PolylineOptions polyOptions = new PolylineOptions();
        polyOptions.width(POLYLINE_STROKE_WIDTH_PX);
//        polyOptions.pattern(PATTERN_POLYLINE_MINE);
        polyOptions.color(mapActivity.getColor(R.color.trackRoute));
        polyOptions.endCap(endCap);

        lineFromToLatLng = new ArrayList<>(); lineFromToLatLng.add(new LatLng(0,0)); lineFromToLatLng.add(new LatLng(0,0));

        for (int i = 0; i < locLogs.size()-2; i++) {
            lineFromToLatLng.set(0, new LatLng(locLogs.get(i).getLatitude(), locLogs.get(i).getLongitude()));
            lineFromToLatLng.set(1, new LatLng(locLogs.get(i+1).getLatitude(), locLogs.get(i+1).getLongitude()));
//            utils.log(logID, locLogs.get(i).getLatitude()+" x "+ locLogs.get(i).getLongitude());
            showMarker.drawLine(lineFromToLatLng);
            polyOptions.addAll(lineFromToLatLng);
            googleMap.addPolyline(polyOptions);
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng((locNorth + locSouth)/2, (locEast + locWest)/2), mapScale));

        showMarker.drawStart(locLogs.get(0).latitude, locLogs.get(0).longitude);
        showMarker.drawFinish(locLogs.get(locLogs.size()-1).latitude, locLogs.get(locLogs.size()-1).longitude);

//        googleMap.getUiSettings().setCompassEnabled(true);
//        googleMap.getUiSettings().setZoomControlsEnabled(true);
//        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
//        googleMap.getUiSettings().setZoomGesturesEnabled(true);
//        googleMap.getUiSettings().setTiltGesturesEnabled(false);

        TextView tvTimeInfo = findViewById(R.id.timeInfo);
        s = utils.long2DateDayTime(locLogs.get(0).logTime)+" ~\n"+utils.long2DateDayTime(locLogs.get(locLogs.size()-1).logTime)+"   ";
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
            deleteThisLogOrNot(position);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private static void deleteThisLogOrNot(int pos) {
        final int position = pos;
        final TrackLog TrackLog = trackLogs.get(pos);
        AlertDialog.Builder builder = new AlertDialog.Builder(trackActivity);
        builder.setTitle("이동 정보 처리");
        String s = utils.long2DateDay(TrackLog.getStartTime())+" "+utils.long2Time(TrackLog.getStartTime())+"~"+
                utils.long2Time(TrackLog.getFinishTime())+"\n"+
                decimalComma.format(TrackLog.getMeters())+"m "+utils.minute2Text(TrackLog.getMinutes());
        builder.setMessage(s);
        builder.setNegativeButton("Delete",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        long fromTime = TrackLog.getStartTime();
                        long toTime = TrackLog.getFinishTime();
                        databaseIO.trackDelete(fromTime);
                        databaseIO.logDeleteFromTo(fromTime, toTime);
                        trackLogs.remove(position);
                        trackAdapter.notifyItemRemoved(position);
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
//            polyOptions.addAll(listLatLng);
//            float ratio = (float) i / (float) locLogs.size();
//            int color = animatedColor.with(ratio);
//            polyOptions.color(color);
//            utils.log(logID, ratio+" " +Integer.toHexString(color));
//            red += 0x10000; green +=0x100; blue++;
//            utils.log(logID,"new Color R:"+Integer.toHexString(red)+" G:"+Integer.toHexString(green)+" B:"+Integer.toHexString(blue)+" Result "+Integer.toHexString(0xFF000000 | red | green | blue));
//            googleMap.addPolyline(polyOptions);
//        }
//
//    private int blendColors(int from, int to, float ratio) {
//        final float inverseRatio = 1f - ratio;
//
//        final float r = Color.red(to) * ratio + Color.red(from) * inverseRatio;
//        final float g = Color.green(to) * ratio + Color.green(from) * inverseRatio;
//        final float b = Color.blue(to) * ratio + Color.blue(from) * inverseRatio;
//
//        return Color.rgb((int) r, (int) g, (int) b);
//    }
