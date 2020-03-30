package com.urrecliner.mytracklogs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.mytracklogs.Vars.databaseIO;
import static com.urrecliner.mytracklogs.Vars.decimalComma;
import static com.urrecliner.mytracklogs.Vars.gpsTracker;
import static com.urrecliner.mytracklogs.Vars.mContext;
import static com.urrecliner.mytracklogs.Vars.mainActivity;
import static com.urrecliner.mytracklogs.Vars.mapUtils;
import static com.urrecliner.mytracklogs.Vars.nowLatitude;
import static com.urrecliner.mytracklogs.Vars.nowLongitude;
import static com.urrecliner.mytracklogs.Vars.prevLatitude;
import static com.urrecliner.mytracklogs.Vars.prevLongitude;
import static com.urrecliner.mytracklogs.Vars.sdfDateDayTime;
import static com.urrecliner.mytracklogs.Vars.sharePrefer;
import static com.urrecliner.mytracklogs.Vars.utils;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    final static String logID = "Main";
    static Timer logTimer = new Timer();
    private static Handler updateMarker;
    private boolean modeStarted = false, modePaused = false;
    FloatingActionButton fabGoStop, fabWalkDrive, fabPause;
    long prevLogTime, elapsedTime;
    TextView tvStartDate, tvStartTime, tvMeter, tvMinutes;
    Intent serviceIntent;
    LinearLayout llTimeInfo, llTrackInfo;
    boolean isWalk = true;
    double locSouth, locNorth, locWest, locEast;
    int mapScale = 17;
    GoogleMap mainMap;
    Polyline markLines = null;
    ArrayList<LatLng> listLatLng;
    Marker markerStart = null, markerFinish = null, markerHere = null;
    double startLatitude = 0, startLongitude = 0, polyLatitudeT, polyLongitudeT;
    double meters = 0;
    long startTime = 0, finishTime = 0, beginTime = 0, minutes = 0, pauseTime = 0;
    int dbCount = 0;

    double totSpeed = 0;
    CustomCap endCap;

    ArrayList<LatLng> latLngs;
    ArrayList<Double> latitudes, longitudes;
    final int ARRAY_COUNT = 5;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mainActivity = this;
        setContentView(R.layout.activity_main);

        askPermission();
        utils = new Utils();
        mapUtils = new MapUtils();
        utils.log(logID,"Started");

        listLatLng = new ArrayList<LatLng>(); listLatLng.add(new LatLng(0,0)); listLatLng.add(new LatLng(0,0));
        tvStartDate = findViewById(R.id.startDate);
        tvStartTime = findViewById(R.id.startTime);
        tvMeter = findViewById(R.id.meter);
        tvMinutes = findViewById(R.id.nMinutes);
        llTimeInfo = findViewById(R.id.timeInfo); llTrackInfo = findViewById(R.id.trackInfo);
        llTimeInfo.setVisibility(View.INVISIBLE); llTrackInfo.setVisibility(View.INVISIBLE);

        gpsTracker = new GPSTracker(mContext);
        sharePrefer = getSharedPreferences("myTracks", Context.MODE_PRIVATE);
//        logInterval = sharePrefer.getInt("logInterval", 10) * 1000;
        gpsTracker.askLocation(isWalk);
        nowLatitude = gpsTracker.getLatitude();
        nowLongitude = gpsTracker.getLongitude();
        utils.log("Main", "Start app @ "+ nowLatitude +" x "+ nowLongitude);

        databaseIO = new DatabaseIO();

        fabWalkDrive = findViewById(R.id.fabWalkDrive);
        fabGoStop = findViewById(R.id.fabGoStop);
        fabPause = findViewById(R.id.fabPause);
        fabPause.setAlpha(0.2f);

        fabWalkDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isWalk = !isWalk;
                fabWalkDrive.setImageResource((isWalk)? R.mipmap.footprint : R.mipmap.drive);
            }
        });

        fabGoStop.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                if (modeStarted) {  // STOP
                    modeStarted = false;
                    modePaused= false;
                    endTrackLog();
                    fabGoStop.setImageResource(R.mipmap.button_start);
                    fabPause.setAlpha(0.2f);
                    utils.deleteOldLogFiles();
                }
                else {  // START
                    modeStarted = true;
                    modePaused = false;
                    prevLogTime = System.currentTimeMillis();
                    beginTrackLog();
                    fabGoStop.setImageResource(R.mipmap.button_stop);
                    fabPause.setAlpha(1f);
                }
            }
        });

        fabPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (modeStarted) {
                    if (modePaused) {      // RESTART
                        modePaused = false;
                        beginTimerTask();
                        beginTime = System.currentTimeMillis();
                        prevLogTime = beginTime;
                        fabPause.setImageResource(R.mipmap.button_pause);

                    } else {       // PAUSE
                        modePaused = true;
                        stopTimerTask();
                        minutes += System.currentTimeMillis() - beginTime;
                        fabPause.setImageResource(R.mipmap.button_restart);
                    }
                }
            }
        });

        showThisAreaMap();
//        displayDateTime();
        String blank = " ";

        tvStartDate.setText(blank);
        tvStartTime.setText(blank);
        tvMeter = findViewById(R.id.meter);
        tvMeter.setText(blank);
        tvMinutes = findViewById(R.id.nMinutes);
        tvMinutes.setText(blank);
        ActionBar ab = this.getSupportActionBar();
        ab.setTitle(" 기록하기");
        ab.setIcon(R.mipmap.my_face) ;
        ab.setDisplayUseLogoEnabled(true) ;
        ab.setDisplayShowHomeEnabled(true) ;
        updateMarker = new Handler() {public void handleMessage(Message msg) { responseGPSLocation(); }};

        endCap = new CustomCap(
                BitmapDescriptorFactory.fromResource(R.mipmap.triangle), 12);
    }

    void beginTimerTask() {
        stopTimerTask();
        gpsTracker.askLocation(isWalk);
        prevLatitude = gpsTracker.getLatitude();
        prevLongitude = gpsTracker.getLongitude();
    }

    void stopTimerTask()
    {
        gpsTracker.stopUsingGPS();
    }

    void beginTrackLog() {
        gpsTracker.askLocation(isWalk);
        startTime = System.currentTimeMillis();
        beginTime = startTime;
        latLngs = new ArrayList<>(); latitudes = new ArrayList<>(); longitudes = new ArrayList<>();
        startLatitude = gpsTracker.getLatitude();
        startLongitude = gpsTracker.getLongitude();
        for (int i = 0; i < ARRAY_COUNT; i++) { latitudes.add(startLatitude); longitudes.add(startLongitude); }
        latLngs.add(new LatLng(startLatitude, startLongitude));
        utils.log(logID, "startLog " + startLatitude + " x " + startLongitude);
        finishTime = 0;
        dbCount = 0;
        meters = 0;
        minutes = 0;
        totSpeed = 0;
        tvStartDate.setText(utils.long2DateDay(startTime));
        tvStartTime.setText(utils.long2Time(startTime));
        tvMinutes.setText("0분");
        tvMeter.setText("0m");
        beginTimerTask();
        llTimeInfo.setVisibility(View.VISIBLE);
        llTrackInfo.setVisibility(View.VISIBLE);
        if (markLines != null) {
            markLines.remove();
            markLines = null;
        }
        markerHandler.sendEmptyMessage(MARK_START);
        markerHandler.sendEmptyMessage(MARK_HERE);
        if (markerFinish != null)
            markerFinish.remove();

        utils.log("create","NEW log "+sdfDateDayTime.format(startTime));
        databaseIO.trackInsert(startTime);

        updateNotificationBar(utils.long2DateDayTime(startTime), 0, 0, R.mipmap.button_pause);
        locSouth = startLatitude-0.0001; locNorth = startLatitude+0.0001;
        locWest = startLongitude-0.0001; locEast = startLongitude+0.0001;
    }

    void endTrackLog() {
        stopTimerTask();
        finishTime = System.currentTimeMillis();
        latitudeGPS = gpsTracker.getLatitude(); longitudeGPS = gpsTracker.getLongitude();
        responseGPSLocation();
        markerHere.remove();
        markerHandler.sendEmptyMessage(MARK_FINISH);
        calcMapScale();
        mainMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(nowLatitude, nowLongitude), mapScale));
        if (dbCount > 0) {
            minutes += System.currentTimeMillis() - beginTime;
            databaseIO.trackUpdate(startTime, finishTime, (int) meters, (int) minutes / 60000);
            utils.log("finish","NEW log "+sdfDateDayTime.format(startTime));
            dbCount = 0;
        }
        else
            databaseIO.trackDelete(startTime);
    }

    void showThisAreaMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mainMap);
        mapFragment.getMapAsync(this);
    }

    void responseGPSLocation() {

        long nowTime = System.currentTimeMillis();
        nowLatitude = latitudeGPS; nowLongitude = longitudeGPS;
        elapsedTime = minutes + nowTime - beginTime;
        String s = utils.minute2Text((int) elapsedTime / 60000);
        tvMinutes.setText(s);
        double distance = mapUtils.getShortDistance();
        long deltaTime = nowTime - prevLogTime;
        double speed = distance * (60*60) / ((double)deltaTime/1000);
        if (deltaTime > 100 && (!isWalk && speed < 2500000 && speed > 4000) || (isWalk && speed<45000 && speed>1000)) {
            markerHandler.sendEmptyMessage(MARK_HERE);
            totSpeed += speed;
            latitudes.remove(0); longitudes.remove(0);
            latitudes.add(nowLatitude); longitudes.add(nowLongitude);
            calcMidLatLng();
            nowLatitude = latitudes.get(1); nowLongitude = longitudes.get(1);
            if (nowLatitude > locNorth) locNorth = nowLatitude;
            if (nowLatitude < locSouth) locSouth = nowLatitude;
            if (nowLongitude > locEast) locEast = nowLongitude;
            if (nowLongitude < locWest) locWest = nowLongitude;
            try {
                listLatLng.set(0, new LatLng(prevLatitude, prevLongitude));
                listLatLng.set(1, new LatLng(nowLatitude, nowLongitude));;
                markerHandler.sendEmptyMessage(ONE_LINE);
            } catch (Exception e) {
                utils.log(logID, "ONE_LINE Exception "+e.toString());
                e.printStackTrace();
            }
            try {
                if (dbCount == 0)
                    markerHandler.sendEmptyMessage(MARK_START);
            } catch (Exception e) {
                utils.log(logID, "MARK_START Exception "+e.toString());
                e.printStackTrace();
            }
            distance = mapUtils.getShortDistance();
            meters += distance;
            try {
                databaseIO.logInsert(nowTime, nowLatitude, nowLongitude);
                databaseIO.trackUpdate(startTime, nowTime, (int) meters, (int) elapsedTime / 60000);
                utils.log("update", sdfDateDayTime.format(nowTime) + " distance="+distance+
                        " meter=" + meters + " elapsed=" + elapsedTime);
                s = decimalComma.format(meters) + "m /" + dbCount;
                tvMeter.setText(s);
                markerHandler.sendEmptyMessage(MARK_HERE);
            } catch (Exception e) {
                utils.log(logID, "dbCount 3 Exception "+e.toString());
                e.printStackTrace();
            }
            prevLatitude = nowLatitude;
            prevLongitude = nowLongitude;
            prevLogTime = nowTime;
            dbCount++;
            utils.log("GOOD", "dist " + distance + " speed " + speed+" time "+deltaTime+" av speed "+(totSpeed)/dbCount);
        }
        else
            utils.log("X", "BAD " + distance + " XSpeed " + speed+" XTime "+(nowTime-prevLogTime));

    }

    void calcMidLatLng() {
        latitudes.set(1, ((latitudes.get(0)+latitudes.get(2))/2+latitudes.get(1))/2);
        latitudes.set(3, ((latitudes.get(2)+latitudes.get(4))/2+latitudes.get(3))/2);
        latitudes.set(2, ((latitudes.get(1)+latitudes.get(3))/2+latitudes.get(2))/2);
        longitudes.set(1, ((longitudes.get(0)+longitudes.get(2))/2+longitudes.get(1))/2);
        longitudes.set(3, ((longitudes.get(2)+longitudes.get(4))/2+longitudes.get(3))/2);
        longitudes.set(2, ((longitudes.get(1)+longitudes.get(3))/2+longitudes.get(2))/2);
    }

    void calcMapScale() {
        double fullMapDistance = mapUtils.getFullMapDistance();
        int newScale = mapUtils.getMapScale(fullMapDistance);
        if (newScale == mapScale)
            return;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mainMap = googleMap;
        gpsTracker.askLocation(isWalk);
        nowLatitude = gpsTracker.getLatitude();
        nowLongitude = gpsTracker.getLongitude();
        calcMapScale();
        utils.log(logID, "MapReady "+ nowLatitude +" x "+ nowLongitude);
        markerHandler.sendEmptyMessage(MARK_HERE);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(nowLatitude, nowLongitude), mapScale));
    }

    static double latitudeGPS, longitudeGPS;
    static void locationUpdated(Double latitude, Double longitude) {
        latitudeGPS = latitude; longitudeGPS = longitude;
        updateMarker.sendEmptyMessage(0);
    }
    final Handler markerHandler = new Handler() {public void handleMessage(Message msg) { mapShowMarker(msg.what); }};

    void mapShowMarker (final int markerType) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                final LatLng latLng;
                switch (markerType) {
                    case MARK_START:
                        latLng = new LatLng(nowLatitude, nowLongitude);
                        if (markerStart != null)
                            markerStart.remove();
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                markerStart = mainMap.addMarker(new MarkerOptions()
                                        .zIndex(2000f)
                                        .position(latLng)
                                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker_start)));
                            }
                        });
                        break;
                    case MARK_FINISH:
                        latLng = new LatLng(nowLatitude, nowLongitude);
                        if (markerFinish != null)
                            markerFinish.remove();
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                markerFinish = mainMap.addMarker(new MarkerOptions()
                                        .zIndex(3000f)
                                        .position(latLng)
                                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker_finish)));
                            }
                        });
                        break;
                    case MARK_HERE:
                        latLng = new LatLng(latitudeGPS, longitudeGPS);
                        if (markerHere != null)
                            markerHere.remove();
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                markerHere = mainMap.addMarker(new MarkerOptions()
                                        .zIndex(10000f)
                                        .position(latLng)
                                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.my_face)));
                            }
                        });
                        break;
                    case MARK_DOT:
                        latLng = new LatLng(nowLatitude, nowLongitude);
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mainMap.addMarker(new MarkerOptions()
                                        .zIndex(1f)
                                        .position(latLng)
                                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker_dot_small)));
                            }
                        });
                        break;
                    case MARK_REFRESH:
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                mainMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
//                                        new LatLng((locNorth+locSouth)/2, (locEast+locWest)/2), mapScale));
                            }
                        });
                        break;
                    case ONE_LINE:
                        mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                PolylineOptions polyOptions = new PolylineOptions();
                                polyOptions.color(getColor(R.color.trackRoute));
                                polyOptions.width(POLYLINE_STROKE_WIDTH_PX);
                                polyOptions.endCap(endCap);
                                polyOptions.addAll(listLatLng);
                                mainMap.addPolyline(polyOptions);
                            }
                        });
                        break;
                }
            }
        });
    }

    final int MARK_START = 11, MARK_FINISH = 22, MARK_HERE = 33, MARK_DOT = 44, MARK_REFRESH = 55, ONE_LINE = 66;

    private static final int POLYLINE_STROKE_WIDTH_PX = 6;

    @Override
    public void onBackPressed() {
        finish();
        if (modeStarted)
            endTrackLog();
        new Timer().schedule(new TimerTask() {
            public void run() {
                finishAffinity();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        }, 100);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceIntent!=null) {
            stopService(serviceIntent);
            serviceIntent = null;
        }
    }

    Menu mainMenu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mainMenu = menu;
        getMenuInflater().inflate(R.menu.main_menu, menu);
//        MenuItem item = menu.findItem(R.id.showNowMap);
//        item.setVisible(modeStarted);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        long duration;
        switch (item.getItemId()) {
            case R.id.time_search:
                intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
                return true;

            case R.id.log_view:
                intent = new Intent(MainActivity.this, TrackActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void updateNotificationBar(String dateTime, int meters, int minutes, int iconId) {
        Intent updateIntent = new Intent(MainActivity.this, NotificationService.class);
//        updateIntent.putExtra("dateTime", dateTime);
//        updateIntent.putExtra("meters", meters);
//        updateIntent.putExtra("minutes", minutes);
        updateIntent.putExtra("iconId", iconId);
        startService(updateIntent);
    }

    // ↓ ↓ ↓ P E R M I S S I O N    RELATED /////// ↓ ↓ ↓ ↓
    ArrayList<String> permissions = new ArrayList<>();
    private final static int ALL_PERMISSIONS_RESULT = 101;
    ArrayList permissionsToRequest;
    ArrayList<String> permissionsRejected = new ArrayList<>();

    private void askPermission() {
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.ACCESS_NOTIFICATION_POLICY);
        permissions.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);
        permissionsToRequest = findUnAskedPermissions(permissions);
        if (permissionsToRequest.size() != 0) {
            requestPermissions((String[]) permissionsToRequest.toArray(new String[0]),
                    ALL_PERMISSIONS_RESULT);
        }
    }

    private ArrayList findUnAskedPermissions(@NonNull ArrayList<String> wanted) {
        ArrayList <String> result = new ArrayList<>();
        for (String perm : wanted) if (hasPermission(perm)) result.add(perm);
        return result;
    }
    private boolean hasPermission(@NonNull String permission) {
        return (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED);
    }

    //    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ALL_PERMISSIONS_RESULT) {
            for (Object perms : permissionsToRequest) {
                if (hasPermission((String) perms)) {
                    permissionsRejected.add((String) perms);
                }
            }
            if (permissionsRejected.size() > 0) {
                if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                    String msg = "These permissions are mandatory for the application. Please allow access.";
                    showDialog(msg);
                }
            }
            else
                Toast.makeText(mContext, "Permissions not granted.", Toast.LENGTH_LONG).show();
        }
    }
    private void showDialog(String msg) {
        showMessageOKCancel(msg,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(permissionsRejected.toArray(
                                new String[0]), ALL_PERMISSIONS_RESULT);
                    }
                });
    }
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new android.app.AlertDialog.Builder(mainActivity)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
// ↑ ↑ ↑ ↑ P E R M I S S I O N    RELATED /////// ↑ ↑ ↑
}
