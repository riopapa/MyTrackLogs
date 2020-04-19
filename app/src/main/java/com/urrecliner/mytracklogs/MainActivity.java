package com.urrecliner.mytracklogs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import static com.urrecliner.mytracklogs.Vars.ACTION_HIDE_CONFIRM;
import static com.urrecliner.mytracklogs.Vars.ACTION_INIT;
import static com.urrecliner.mytracklogs.Vars.ACTION_PAUSE;
import static com.urrecliner.mytracklogs.Vars.ACTION_RESTART;
import static com.urrecliner.mytracklogs.Vars.ACTION_SHOW_CONFIRM;
import static com.urrecliner.mytracklogs.Vars.ACTION_START;
import static com.urrecliner.mytracklogs.Vars.ACTION_STOP;
import static com.urrecliner.mytracklogs.Vars.ACTION_UPDATE;
import static com.urrecliner.mytracklogs.Vars.NOTIFICATION_BAR_EXIT_APP;
import static com.urrecliner.mytracklogs.Vars.NOTIFICATION_BAR_CONFIRMED_STOP;
import static com.urrecliner.mytracklogs.Vars.NOTIFICATION_BAR_GO;
import static com.urrecliner.mytracklogs.Vars.NOTIFICATION_BAR_HIDE_CONFIRM;
import static com.urrecliner.mytracklogs.Vars.NOTIFICATION_BAR_PAUSE_RESTART;
import static com.urrecliner.mytracklogs.Vars.NOTIFICATION_BAR_SHOW_CONFIRM;
import static com.urrecliner.mytracklogs.Vars.databaseIO;
import static com.urrecliner.mytracklogs.Vars.decimalComma;
import static com.urrecliner.mytracklogs.Vars.dummyMap;
import static com.urrecliner.mytracklogs.Vars.gpsTracker;
import static com.urrecliner.mytracklogs.Vars.mContext;
import static com.urrecliner.mytracklogs.Vars.mainActivity;
import static com.urrecliner.mytracklogs.Vars.mapUtils;
import static com.urrecliner.mytracklogs.Vars.modePaused;
import static com.urrecliner.mytracklogs.Vars.modeStarted;
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
    FloatingActionButton fabGoStop, fabWalkDrive, fabPauseRestart;
    long prevLogTime, elapsedTime;
    TextView tvStartDate, tvStartTime, tvMeter, tvMinutes;
    Intent serviceIntent;
    LinearLayout llTimeInfo, llTrackInfo;
    boolean isWalk = true;
    double locSouth, locNorth, locWest, locEast;
    int mapScale = 17;
    GoogleMap mainMap;
    Polyline markLines = null;
    ArrayList<LatLng> markerLatLng;
    double startLatitude = 0, startLongitude = 0;
    double meters = 0;
    long startTime = 0, finishTime = 0, beginTime = 0, minutes = 0;
    int dbCount = 0;
    double totSpeed = 0;
    final double mapDiff = 0.01f;

    ArrayList<LatLng> latLngPos;
    ArrayList<Double> latitudeSaves, longitudeSaves, latitudeQues, longitudeQues;
    final int QUE_COUNT = 4;

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

        markerLatLng = new ArrayList<>(); markerLatLng.add(new LatLng(0,0)); markerLatLng.add(new LatLng(0,0));
        tvStartDate = findViewById(R.id.startDate);
        tvStartTime = findViewById(R.id.startTime);
        tvMeter = findViewById(R.id.meter);
        tvMinutes = findViewById(R.id.nMinutes);
        llTimeInfo = findViewById(R.id.timeSummary); llTrackInfo = findViewById(R.id.trackInfo);
        llTimeInfo.setVisibility(View.INVISIBLE); llTrackInfo.setVisibility(View.INVISIBLE);

        gpsTracker = new GPSTracker();
        gpsTracker.startGPSUpdate();
        nowLatitude = gpsTracker.getGpsLatitude();
        nowLongitude = gpsTracker.getGpsLongitude();
        utils.log("Main", "Start app @ "+ nowLatitude +" x "+ nowLongitude);
        sharePrefer = getSharedPreferences("myTracks", Context.MODE_PRIVATE);

        databaseIO = new DatabaseIO();
        fabWalkDrive = findViewById(R.id.fabWalkDrive);
        fabGoStop = findViewById(R.id.fabGoStop);
        fabPauseRestart = findViewById(R.id.fabPause);
        fabPauseRestart.setAlpha(0.2f);
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
        fabPauseRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseRestart_Clicked();
            }
        });

        String blank = " ";
        tvStartDate.setText(blank); tvStartTime.setText(blank);
        tvMeter = findViewById(R.id.meter); tvMeter.setText(blank);
        tvMinutes = findViewById(R.id.nMinutes); tvMinutes.setText(blank);
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
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mainMap);
        mapFragment.getMapAsync(this);
        updateNotification(ACTION_INIT);
        dummyMap = mapUtils.StringToBitMap(mapUtils.BitMapToString(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)));
    }

    void goStop_Clicked() {
        if (modeStarted) {
            confirmFinish();
        }
        else {
            go_Clicked();
        }
    }

    void go_Clicked() {
        modeStarted = true;
        modePaused = false;
        beginTrackLog();
        fabGoStop.setImageResource(R.mipmap.button_stop);
        fabPauseRestart.setAlpha(1f);
        updateNotification(ACTION_START);
    }

    void pauseRestart_Clicked() {
        if (modeStarted) {
            if (modePaused) {      // already paused, let restart
                modePaused = false;
                gpsTracker.startGPSUpdate();
                beginTime = System.currentTimeMillis();
                prevLogTime = beginTime;
                fabPauseRestart.setImageResource(R.mipmap.button_pause);
                prevLatitude = gpsTracker.getGpsLatitude();
                prevLongitude = gpsTracker.getGpsLongitude();
                updateNotification(ACTION_RESTART);
            } else {
                modePaused = true;
                gpsTracker.stopGPSUpdate();
                minutes += System.currentTimeMillis() - beginTime;
                fabPauseRestart.setImageResource(R.mipmap.button_restart);
                updateNotification(ACTION_PAUSE);
            }
        }
    }

    void finish_tracking() {
        modeStarted = false;
        modePaused = false;
        utils.log(logID,"Finish Summary average speed:"+(totSpeed)/dbCount+" dbCount="+dbCount+" Elapsed="+elapsedTime/60000);
        endTrackLog();
        fabGoStop.setImageResource(R.mipmap.button_start);
        fabPauseRestart.setAlpha(0.2f);
        utils.deleteOldLogFiles();
        updateNotification(ACTION_STOP);
    }

    void beginTrackLog() {
        mainMap.clear();
        gpsTracker.startGPSUpdate();
        startTime = System.currentTimeMillis();
        beginTime = startTime;
        prevLogTime = startTime;
        latLngPos = new ArrayList<>(); latitudeSaves = new ArrayList<>(); longitudeSaves = new ArrayList<>();
        latitudeQues = new ArrayList<>(); longitudeQues = new ArrayList<>();
        startLatitude = gpsTracker.getGpsLatitude();
        startLongitude = gpsTracker.getGpsLongitude();
        for (int i = 0; i < QUE_COUNT; i++) { latitudeSaves.add(startLatitude); longitudeSaves.add(startLongitude); }
        latLngPos.add(new LatLng(startLatitude, startLongitude));
        utils.log(logID, "startLog " + startLatitude + " x " + startLongitude);
        latitudeQues.add(startLatitude); longitudeQues.add(startLongitude);
        finishTime = 0;
        dbCount = 0;
        meters = 0;
        minutes = 0;
        totSpeed = 0;
        tvStartDate.setText(utils.long2DateDay(startTime));
        tvStartTime.setText(utils.long2Time(startTime));
        tvMinutes.setText(R.string.zero_minutes);
        tvMeter.setText(R.string.zero_meters);
        prevLatitude = gpsTracker.getGpsLatitude();
        prevLongitude = gpsTracker.getGpsLongitude();
        llTimeInfo.setVisibility(View.VISIBLE);
        llTrackInfo.setVisibility(View.VISIBLE);
        if (markLines != null) {
            markLines.remove();
            markLines = null;
        }
        showMarker.drawStart(startLatitude, startLongitude, false);

        utils.log("create","NEW log "+sdfDateDayTime.format(startTime));
        databaseIO.trackInsert(startTime);
        locSouth = 999; locNorth = -999; locWest = 999; locEast = -999;
    }

    void endTrackLog() {
        finishTime = System.currentTimeMillis();
        latitudeGPS = gpsTracker.getGpsLatitude(); longitudeGPS = gpsTracker.getGpsLongitude();
        responseGPSLocation(); responseGPSLocation(); responseGPSLocation();
        gpsTracker.stopGPSUpdate();
        calcMapScale();
        utils.log(logID,"Final map scale is "+mapScale);
        mainMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitudeGPS, longitudeGPS), mapScale));
        elapsedTime = minutes + finishTime - beginTime;
        databaseIO.trackUpdate(startTime, finishTime, (int) meters, (int) elapsedTime / 60000);
//        utils.log("finish","NEW log "+sdfDateDayTime.format(startTime));/
        showMarker.drawHereOff();
        showMarker.drawFinish(latitudeGPS, longitudeGPS, false);
        updateNotification(ACTION_UPDATE);
        updateNotification(ACTION_STOP);
    }

    void responseGPSLocation() {

        nowLatitude = latitudeGPS; nowLongitude = longitudeGPS;
        long nowTime = System.currentTimeMillis();
        long deltaTime = nowTime - prevLogTime;
        if (deltaTime < 200)
            return;
        double distance = mapUtils.getShortDistance();
        double speed = distance / (double)deltaTime * 1000f / 60f;
        utils.log("Source", "distance "+distance+" speed " + speed+" time "+deltaTime);
        if (isWalk && (speed>50f || speed < 0.001f)) {
            utils.log("Walk", "BAD " + " XSpeed " + speed+" XTime "+deltaTime);
            return;
        }
        elapsedTime = minutes + nowTime - beginTime;
        String s = utils.minute2Text((int) elapsedTime / 60000);
        tvMinutes.setText(s);
        totSpeed += speed;
        latitudeSaves.remove(0); longitudeSaves.remove(0);
        latitudeSaves.add(nowLatitude); longitudeSaves.add(nowLongitude);
        adjustPosition();
        nowLatitude = latitudeSaves.get(0); nowLongitude = longitudeSaves.get(0);
        if (nowLatitude > locNorth) locNorth = nowLatitude;
        if (nowLatitude < locSouth) locSouth = nowLatitude;
        if (nowLongitude > locEast) locEast = nowLongitude;
        if (nowLongitude < locWest) locWest = nowLongitude;

        latitudeQues.add(nowLatitude); longitudeQues.add(nowLongitude);
        drawTrackLogs();
        distance = mapUtils.getShortDistance();
        meters += distance; // * 1.1f;
        if (dbCount % 2 == 0) {
            databaseIO.logInsert(nowTime, nowLatitude, nowLongitude);
            databaseIO.trackUpdate(startTime, nowTime, (int) meters, (int) elapsedTime / 60000);
        }
        s = decimalComma.format(meters) + "m /" + dbCount;
        tvMeter.setText(s);
        updateNotification(ACTION_UPDATE);
        prevLatitude = nowLatitude;
        prevLongitude = nowLongitude;
        prevLogTime = nowTime;
        dbCount++;
        utils.log("GOOD", " speed " + speed+" time "+deltaTime+" avspeed "+(totSpeed)/dbCount);
    }

    void adjustPosition() {
        latitudeSaves.set(1, ((latitudeSaves.get(0)+ latitudeSaves.get(2))/2+ latitudeSaves.get(1))/2);
        latitudeSaves.set(2, ((latitudeSaves.get(1)+ latitudeSaves.get(3))/2+ latitudeSaves.get(2))/2);
        longitudeSaves.set(1, ((longitudeSaves.get(0)+ longitudeSaves.get(2))/2+ longitudeSaves.get(1))/2);
        longitudeSaves.set(2, ((longitudeSaves.get(1)+ longitudeSaves.get(3))/2+ longitudeSaves.get(2))/2);
    }

    void calcMapScale() {
        double fullMapDistance = mapUtils.getFullMapDistance();
        utils.log(logID," initial distance :"+fullMapDistance);
        mapScale = mapUtils.getMapScale(fullMapDistance);
        utils.log(logID, "mapScale >> "+mapScale);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mainMap = googleMap;
        showMarker.init(mainActivity, googleMap);
        nowLatitude = gpsTracker.getGpsLatitude();
        nowLongitude = gpsTracker.getGpsLongitude();
        showMarker.drawHere(nowLatitude, nowLongitude);
        utils.log(logID, "MapReady "+ nowLatitude +" x "+ nowLongitude+" with scale:"+mapScale);
        locSouth = startLatitude-mapDiff; locNorth = startLatitude+mapDiff;
        locWest = startLongitude-mapDiff; locEast = startLongitude+mapDiff;
        utils.log(logID," initial distance :"+mapUtils.getFullMapDistance());
        calcMapScale();
        mapScale = 18;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(nowLatitude, nowLongitude), mapScale));
        updateNotification(ACTION_HIDE_CONFIRM);
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
            case NOTIFICATION_BAR_GO: // GO_STOP
                go_Clicked();
                break;
            case NOTIFICATION_BAR_PAUSE_RESTART: // PAUSE_RESTART
                pauseRestart_Clicked();
                break;
            case NOTIFICATION_BAR_SHOW_CONFIRM:
                updateNotification(ACTION_SHOW_CONFIRM);
                break;
            case NOTIFICATION_BAR_HIDE_CONFIRM:
                updateNotification(ACTION_HIDE_CONFIRM);
                break;
            case NOTIFICATION_BAR_EXIT_APP: // EXIT
                if (!modeStarted)
                    exit_Application();
                break;
            case NOTIFICATION_BAR_CONFIRMED_STOP:
                updateNotification(ACTION_INIT);
                finish_tracking();
                break;
            default:
                utils.log(logID, "/// Touch Code error "+operation);
        }
    }

    void drawTrackLogs() {
//        utils.log(logID, "draw size="+latitudeQues.size());
        while (latitudeQues.size() > 2) {
//            utils.log(logID+latitudeQues.size(), "Drawing "+latitudeQues.get(0)+" x "+longitudeQues.get(0));
            markerLatLng.set(0, new LatLng(latitudeQues.get(0), longitudeQues.get(0)));
            markerLatLng.set(1, new LatLng(latitudeQues.get(1), longitudeQues.get(1)));
            showMarker.drawLine(markerLatLng);
            showMarker.drawHere(latitudeQues.get(1), longitudeQues.get(1));
            latitudeQues.remove(0); longitudeQues.remove(0);
        }
    }
    @Override
    public void onBackPressed() {
        if (!modeStarted)
            exit_Application();
        else
            confirmFinish();
    }

    void exit_Application() {

        updateNotification(ACTION_EXIT);
        finish();
        new Timer().schedule(new TimerTask() {
            public void run() {
                finishAffinity();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        }, 2000);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceIntent != null) {
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
//        utils.log(logID, " /// "+action+" ///");
        Intent updateIntent = new Intent(MainActivity.this, NotificationService.class);
        updateIntent.putExtra("action", action);
        switch (action) {
            case ACTION_UPDATE:
                String s = utils.minute2Text( (int) (elapsedTime / 60000))+"\n"+decimalComma.format(meters) + "m";
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
        builder.setNegativeButton("Finish",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        updateNotification(ACTION_HIDE_CONFIRM);
                        notifyAction.sendEmptyMessage(NOTIFICATION_BAR_CONFIRMED_STOP);
                    }
                });
        builder.setPositiveButton("No, Continue ",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
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
        permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
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
