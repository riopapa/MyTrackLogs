package com.urrecliner.mytracklogs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.mytracklogs.Vars.ACTION_EXIT;
import static com.urrecliner.mytracklogs.Vars.ACTION_INIT;
import static com.urrecliner.mytracklogs.Vars.ACTION_PAUSE;
import static com.urrecliner.mytracklogs.Vars.ACTION_RESTART;
import static com.urrecliner.mytracklogs.Vars.ACTION_START;
import static com.urrecliner.mytracklogs.Vars.ACTION_STOP;
import static com.urrecliner.mytracklogs.Vars.ACTION_UPDATE;
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
import static com.urrecliner.mytracklogs.Vars.showMarker;
import static com.urrecliner.mytracklogs.Vars.utils;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    final static String logID = "Main";
    private static Handler updateMarker, notifyAction;
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
    double startLatitude = 0, startLongitude = 0;
    double meters = 0;
    long startTime = 0, finishTime = 0, beginTime = 0, minutes = 0;
    int dbCount = 0;

    double totSpeed = 0;

    ArrayList<LatLng> latLngs;
    ArrayList<Double> latitudes, longitudes;
    final int ARRAY_COUNT = 5;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
        mainActivity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        utils = new Utils();
        mapUtils = new MapUtils();
        showMarker = new ShowMarker();

        askPermission();

        utils.log(logID,"Started");

        listLatLng = new ArrayList<>(); listLatLng.add(new LatLng(0,0)); listLatLng.add(new LatLng(0,0));
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
                goStop_Clicked();
            }
        });

        fabPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseRestart_Clicked();
            }
        });

        showThisAreaMap();
        String blank = " ";

        tvStartDate.setText(blank);
        tvStartTime.setText(blank);
        tvMeter = findViewById(R.id.meter);
        tvMeter.setText(blank);
        tvMinutes = findViewById(R.id.nMinutes);
        tvMinutes.setText(blank);
        ActionBar ab = this.getSupportActionBar();
        ab.setTitle(R.string.track_recording);
        ab.setIcon(R.mipmap.my_face) ;
        ab.setDisplayUseLogoEnabled(true) ;
        ab.setDisplayShowHomeEnabled(true) ;
        updateMarker = new Handler() {public void handleMessage(Message msg) { responseGPSLocation(); }};
        notifyAction = new Handler() {public void handleMessage(Message msg) { notificationClicked(msg.what); }};

        new Timer().schedule(new TimerTask() {
            public void run () {
                Intent updateIntent = new Intent(MainActivity.this, NotificationService.class);
                updateIntent.putExtra("status", "init");
                startService(updateIntent);
            }
        }, 100);
    }

    void goStop_Clicked() {
        if (modeStarted) {  // STOP
            confirmFinish();
        }
        else {  // START
            modeStarted = true;
            modePaused = false;
            prevLogTime = System.currentTimeMillis();
            beginTrackLog();
            fabGoStop.setImageResource(R.mipmap.button_stop);
            fabPause.setAlpha(1f);
            updateNotification(ACTION_START);
        }
    }

    void finish_tracking() {
        modeStarted = false;
        modePaused = false;
        endTrackLog();
        fabGoStop.setImageResource(R.mipmap.button_start);
        fabPause.setAlpha(0.2f);
        utils.deleteOldLogFiles();
        updateNotification(ACTION_STOP);
    }


    void pauseRestart_Clicked() {
        if (modeStarted) {
            if (modePaused) {      // already paused, let restart
                modePaused = false;
                beginTimerTask();
                beginTime = System.currentTimeMillis();
                prevLogTime = beginTime;
                fabPause.setImageResource(R.mipmap.button_pause);
                updateNotification(ACTION_RESTART);
            } else {       // make paused
                modePaused = true;
                stopTimerTask();
                minutes += System.currentTimeMillis() - beginTime;
                fabPause.setImageResource(R.mipmap.button_restart);
                updateNotification(ACTION_PAUSE);
            }
        }
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
        tvMinutes.setText(R.string.zero_minutes);
        tvMeter.setText(R.string.zero_meters);
        beginTimerTask();
        llTimeInfo.setVisibility(View.VISIBLE);
        llTrackInfo.setVisibility(View.VISIBLE);
        if (markLines != null) {
            markLines.remove();
            markLines = null;
        }
        showMarker.drawStart(startLatitude, startLongitude);

        utils.log("create","NEW log "+sdfDateDayTime.format(startTime));
        databaseIO.trackInsert(startTime);
        locSouth = startLatitude-0.01; locNorth = startLatitude+0.01;
        locWest = startLongitude-0.01; locEast = startLongitude+0.01;
    }

    void endTrackLog() {
        stopTimerTask();
        finishTime = System.currentTimeMillis();
        latitudeGPS = gpsTracker.getLatitude(); longitudeGPS = gpsTracker.getLongitude();
        responseGPSLocation();
//        markerHere.remove();
        showMarker.drawHere(nowLatitude, nowLongitude);
        showMarker.drawFinish(nowLatitude, nowLongitude);
//        markerHandler.sendEmptyMessage(MARK_FINISH);
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
        updateNotification(ACTION_UPDATE);
        updateNotification(ACTION_STOP);
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
        double speed = distance * (60*60) / ((double)deltaTime/1000);   // 269649 car
        if (deltaTime > 1000 && (!isWalk && speed < 600000 && speed > 4000) || (isWalk && speed<45000 && speed>1000)) {
//            markerHandler.sendEmptyMessage(MARK_HERE);
            showMarker.drawHere(nowLatitude, nowLongitude);
            totSpeed += speed;
            latitudes.remove(0); longitudes.remove(0);
            latitudes.add(nowLatitude); longitudes.add(nowLongitude);
            calcMidLatLng();
            nowLatitude = latitudes.get(1); nowLongitude = longitudes.get(1);
            if (nowLatitude > locNorth) locNorth = nowLatitude;
            if (nowLatitude < locSouth) locSouth = nowLatitude;
            if (nowLongitude > locEast) locEast = nowLongitude;
            if (nowLongitude < locWest) locWest = nowLongitude;
            listLatLng.set(0, new LatLng(prevLatitude, prevLongitude));
            listLatLng.set(1, new LatLng(nowLatitude, nowLongitude));
            showMarker.drawLine(listLatLng);
//            markerHandler.sendEmptyMessage(ONE_LINE);
            try {
                if (dbCount == 0)
                    showMarker.drawStart(startLatitude, startLongitude);
//                    markerHandler.sendEmptyMessage(MARK_START);
            } catch (Exception e) {
                utils.log(logID, "MARK_START Exception "+e.toString());
                e.printStackTrace();
            }
            distance = mapUtils.getShortDistance();
            meters += distance;
            try {
                if (dbCount % 3 == 0) {
                    databaseIO.logInsert(nowTime, nowLatitude, nowLongitude);
                    databaseIO.trackUpdate(startTime, nowTime, (int) meters, (int) elapsedTime / 60000);
                    s = decimalComma.format(meters) + "m /" + dbCount;
                    tvMeter.setText(s);
                    showMarker.drawHere(nowLatitude, nowLongitude);
//                    markerHandler.sendEmptyMessage(MARK_HERE);
                    updateNotification(ACTION_UPDATE);
                }
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
//        else
//            utils.log("X", "BAD " + distance + " XSpeed " + speed+" XTime "+(nowTime-prevLogTime));

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
        mapScale = mapUtils.getMapScale(fullMapDistance);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mainMap = googleMap;
        gpsTracker.askLocation(isWalk);
        showMarker.init(mainActivity, googleMap);
        nowLatitude = gpsTracker.getLatitude();
        nowLongitude = gpsTracker.getLongitude();
        calcMapScale();
        utils.log(logID, "MapReady "+ nowLatitude +" x "+ nowLongitude);
        showMarker.drawHere(nowLatitude, nowLongitude);
//        markerHandler.sendEmptyMessage(MARK_HERE);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(nowLatitude, nowLongitude), mapScale));
    }

    static double latitudeGPS, longitudeGPS;
    static void locationUpdated(Double latitude, Double longitude) {
        latitudeGPS = latitude; longitudeGPS = longitude;
        updateMarker.sendEmptyMessage(0);
    }

    static void notificationBarTouched(int buttonType) {
        utils.log(logID, "touch Button "+buttonType);
        notifyAction.sendEmptyMessage(buttonType);
    }

    void notificationClicked(int operation) {
        utils.log(logID, "notificationClicked "+operation);
        switch (operation) {
            case 1: // GO_STOP
                goStop_Clicked();
                break;
            case 2: // PAUSE_RESTART
                pauseRestart_Clicked();
                break;
            case 3: // EXIT
                exit_Application();
                break;
            case 9: // FINISH CONFIRMED
                finish_tracking();
                break;
            default:
                utils.log(logID, "Touch Code error "+operation);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
        exit_Application();
    }

    void exit_Application() {
        if (modeStarted)
            endTrackLog();
        updateNotification(ACTION_EXIT);
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

    void updateNotification(String action) {
        utils.log(logID, "updateNotification *** "+action);
        Intent updateIntent = new Intent(MainActivity.this, NotificationService.class);
        updateIntent.putExtra("action", action);
        switch (action) {
            case ACTION_UPDATE:
                String s = utils.minute2Text( (int) (minutes / 60000))+"\n"+decimalComma.format(meters) + "m";
                updateIntent.putExtra("laps", s);
                break;
            case ACTION_INIT:
            case ACTION_START:
            case ACTION_PAUSE:
            case ACTION_STOP:
            case ACTION_RESTART:
                break;
            default:
        }
        startService(updateIntent);
    }

    private void confirmFinish() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Confirm Finish ");
        String s = "Are you sure to finish tracking?";
        builder.setMessage(s);
        builder.setNegativeButton("Yes, Finish",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        notifyAction.sendEmptyMessage(9);
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
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
