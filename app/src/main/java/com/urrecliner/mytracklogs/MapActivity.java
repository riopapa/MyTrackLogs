package com.urrecliner.mytracklogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
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
import static com.urrecliner.mytracklogs.Vars.trackActivity;
import static com.urrecliner.mytracklogs.Vars.trackAdapter;
import static com.urrecliner.mytracklogs.Vars.trackLogs;
import static com.urrecliner.mytracklogs.Vars.utils;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final String logID = "Map";
    private static final int POLYLINE_STROKE_WIDTH_PX = 20;
    private static final int PATTERN_DASH_LENGTH_PX = 6;
    private static final int PATTERN_GAP_LENGTH_PX = 6;
//    private static final PatternItem DOT = new Dot();
    private static final PatternItem DASH = new Dash(PATTERN_DASH_LENGTH_PX);
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);

    private static final List<PatternItem> PATTERN_POLYLINE_MINE = Arrays.asList(DASH, GAP);
    GoogleMap thisMap;
    int reDrawCount = 0;
    private int mapScale;
    private Activity mapActivity;
    private long startTime, finishTime;
    private int iMinutes, iMeters, position;
    private Bitmap iBitmap;
    ArrayList<LatLng> lineFromToLatLng;
    ArrayList<LocLog> locLogs;
    TextView tvTimeInfo, tvLogInfo;
    SupportMapFragment mapFragment;

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

        TrackLog trackLog = getIntent().getParcelableExtra("trackLog");
        startTime = trackLog.getStartTime();
        finishTime = trackLog.getFinishTime();
        iMinutes = trackLog.getMinutes();
        iMeters = trackLog.getMeters();
        iBitmap = trackLog.getBitMap();
        position = getIntent().getIntExtra("position",-1);

        ActionBar ab = this.getSupportActionBar();
        ab.setTitle(R.string.review_map);
        ab.setIcon(R.mipmap.my_face);
        ab.setDisplayUseLogoEnabled(true);
        ab.setDisplayShowHomeEnabled(true);

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragMap);
        mapFragment.getMapAsync(this);
        tvTimeInfo =  findViewById(R.id.timeSummary);
        tvLogInfo =  findViewById(R.id.logSummary);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        thisMap = googleMap;
        showMarker.init(mapActivity, googleMap);
        String s;
        if (retrieveDBLog()) return;

        double fullMapDistance = mapUtils.getFullMapDistance();
        mapScale = mapUtils.getMapScale(fullMapDistance);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng((locNorth + locSouth)/2, (locEast + locWest)/2),
                (float) mapScale - 0.1f));
        View v = findViewById(R.id.fragMap);
        v.post(new Runnable() {
            @Override
            public void run() {
                new Timer().schedule(new TimerTask() {
                    public void run() {
                        reDrawCount = 0;
                        thisMap.snapshot(mapSnapShotCallback);
                    }
                }, 1200);
            }
        });

        s = utils.long2DateDayTime(locLogs.get(0).logTime)+" ~\n"+utils.long2DateDayTime(locLogs.get(locLogs.size()-1).logTime)+"   ";
        tvTimeInfo.setText(s);
        if (iMinutes > 0) {
            s = utils.minute2Text(iMinutes) + "  " + decimalComma.format(iMeters) + "m";
            tvLogInfo.setText(s);
        }
    }

    private void drawTrackLIne(GoogleMap googleMap) {
        lineFromToLatLng = new ArrayList<>();
        lineFromToLatLng.add(new LatLng(0,0));
        lineFromToLatLng.add(new LatLng(0,0));
        AnimatedColor animatedColor = new AnimatedColor(Color.RED, Color.BLUE);
        int color = 0;
        for (int i = 0; i < locLogs.size()-2; i++) {
            lineFromToLatLng.set(0, new LatLng(locLogs.get(i).getLatitude(), locLogs.get(i).getLongitude()));
            lineFromToLatLng.set(1, new LatLng(locLogs.get(i+1).getLatitude(), locLogs.get(i+1).getLongitude()));
            float ratio = (float) i / (float) locLogs.size();
            color = animatedColor.with(ratio);
            if (i % 3 == 0)
                color = color ^ 0x00333333;
            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.width(POLYLINE_STROKE_WIDTH_PX);
            polyOptions.addAll(lineFromToLatLng);
            polyOptions.color(color);
            googleMap.addPolyline(polyOptions);
        }

        showMarker.drawStart(locLogs.get(0).latitude, locLogs.get(0).longitude);
        showMarker.drawFinish(locLogs.get(locLogs.size()-1).latitude, locLogs.get(locLogs.size()-1).longitude);
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
//        utils.log("cursor","W"+ locWest +" E"+ locEast +" S"+ locSouth +" N"+ locNorth);
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
    GoogleMap.SnapshotReadyCallback mapSnapShotCallback = new GoogleMap.SnapshotReadyCallback() {

        Bitmap resultMap = null;
        @Override
        public void onSnapshotReady(Bitmap snapshot) {
            utils.log(logID," snapShot "+ reDrawCount);
            if (reDrawCount == 0) {
                resultMap = Bitmap.createScaledBitmap(snapshot, 240, 360, false);
//                Bitmap convertedBitmap = Bitmap.createBitmap(sBitmap.getWidth(), sBitmap.getHeight(), Bitmap.Config.ARGB_8888);
//                Canvas canvas = new Canvas(convertedBitmap);
//                Paint paint = new Paint();
//                paint.setColor(Color.BLACK);
//                canvas.drawBitmap(sBitmap, 0, 0, paint);
//                resultMap = filterBitmap(sBitmap);
                drawTrackLIne(thisMap);
//                ImageView iv = findViewById(R.id.smallMap);
//                iv.setImageBitmap(resultMap);
                reDrawCount++;
                new Timer().schedule(new TimerTask() {
                    public void run() {
                        thisMap.snapshot(mapSnapShotCallback);
                    }
                }, 2000);
            }
            else if (reDrawCount < 3) {
                utils.log(logID, "Redraw "+ reDrawCount);
                reDrawCount++;
                resultMap = filterBitmap(resultMap, Bitmap.createScaledBitmap(snapshot, 240, 360, false));
                ImageView iv = findViewById(R.id.smallMap);
                iv.setImageBitmap(resultMap);
                final TrackLog trackLog = trackLogs.get(position);
                databaseIO.trackMapUpdate(trackLog.getStartTime(), resultMap);
//                trackAdapter.notifyItemChanged(position);
                trackAdapter.notifyDataSetChanged();
                View v = findViewById(R.id.track_recycler);
                v.invalidate();
            }
        }
    };

    Bitmap filterBitmap(Bitmap bitMap, Bitmap routeMap) {
        int width = bitMap.getWidth();
        int height = bitMap.getHeight();
        int[] pixelsB = new int[width * height];
        int[] pixelsR = new int[width * height];
        bitMap.getPixels(pixelsB, 0, width, 0, 0, width, height);
        routeMap.getPixels(pixelsR, 0, width, 0, 0, width, height);
        int cnt = 0;
        for(int x = 0; x < pixelsR.length; ++x) {
            if (pixelsB[x] == pixelsR[x])
                pixelsR[x] = pixelsR[x] & 0x7FFFFFFF;
        }
        utils.log("count","cnt="+cnt+" org "+width+"x"+height+"="+(width*height));
        // create result bitmap output
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        //set pixels
        result.setPixels(pixelsR, 0, width, 0, 0, width, height);
        return result;
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
