package com.urrecliner.mytracklogs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
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
import static com.urrecliner.mytracklogs.Vars.isWalk;
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
import static com.urrecliner.mytracklogs.Vars.speedColor;
import static com.urrecliner.mytracklogs.Vars.utils;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    final static String logID = "Main";
    final double LOW_SPEED_WALK = 50f, LOW_SPEED_DRIVE = 300f;
    final double HIGH_SPEED_WALK = 2000f, HIGH_SPEED_DRIVE = 60000f;
    private static Handler updateMarker, notifyAction;
    FloatingActionButton fabGoStop, fabPauseRestart;
    long prevLogTime, elapsedTime;
    TextView tvStartDate, tvStartTime, tvMeter, tvMinutes;
    Intent serviceIntent;
    LinearLayout llTimeInfo, llTrackInfo;
    double locSouth, locNorth, locWest, locEast;
    int mapScale = 17;

    GoogleMap mainMap;
    Polyline markLines = null;
    ArrayList<LatLng> markerLatLng;
    double startLatitude = 0, startLongitude = 0;
    double meters = 0;
    long startTime = 0, finishTime = 0, beginTime = 0, minutes = 0;
    int dbCount = 0;
    double totSpeed = 0, totDistance = 0, lowSpeed, highSpeed;
    boolean click;

    ArrayList<LatLng> latLngPos;
    ArrayList<Double> latSVs, lonSVs, latQues, lonQues;
    final int QUE_COUNT = 5;
    MainDialog mainDialog;

//    @SuppressLint("RestrictedApi")
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
        sharePrefer = getSharedPreferences("myTracks", Context.MODE_PRIVATE);

        databaseIO = new DatabaseIO();
        fabGoStop = findViewById(R.id.fabGoStop);
        fabPauseRestart = findViewById(R.id.fabPause);

        fabGoStop.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                if (modeStarted) {
                    confirmFinish();
                } else {
                    mainDialog = new MainDialog();
                    mainDialog.show(getFragmentManager(), null);
                }
            }
        });

        fabPauseRestart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseRestart_Clicked();
            }
        });
        fabPauseRestart.hide();
//        fabPauseRestart.setAlpha(0.2f);

        String blank = " ";
        tvStartDate.setText(blank); tvStartTime.setText(blank);
        tvMeter = findViewById(R.id.meter); tvMeter.setText(blank);
        tvMinutes = findViewById(R.id.nMinutes); tvMinutes.setText(blank);
        ActionBar ab = this.getSupportActionBar();
        ab.setTitle(R.string.track_recording);
        ab.setIcon(R.mipmap.my_face) ;
        ab.setDisplayUseLogoEnabled(true) ;
        ab.setDisplayShowHomeEnabled(true) ;
        updateMarker = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Boolean isActive = (msg.what == 0) ? true:false;
                LatLng latLng = (LatLng) msg.obj;
                locationChanged(isActive, latLng.latitude, latLng.longitude);
            }
        };
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
        latLngPos = new ArrayList<>(); latSVs = new ArrayList<>(); lonSVs = new ArrayList<>();
        latQues = new ArrayList<>(); lonQues = new ArrayList<>();
        startLatitude = gpsTracker.getGpsLatitude();
        startLongitude = gpsTracker.getGpsLongitude();
        for (int i = 0; i < QUE_COUNT; i++) { latSVs.add(startLatitude); lonSVs.add(startLongitude); }
        gpsTracker.stopGPSUpdate();
//        generateColor();  // generate speedColor table
    }

    public static class MainDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder mBuilder = new AlertDialog.Builder(getActivity(), R.style.Theme_MaterialComponents_Dialog_MinWidth);
            LayoutInflater mLayoutInflater = getActivity().getLayoutInflater();
            mBuilder.setView(mLayoutInflater.inflate(R.layout.dialog_walk_drive, null));
//            mBuilder.setTitle(getString(R.string.walk_drive));
            return mBuilder.create();
        }

        @Override
        public void onStop() {
            super.onStop();
        }

    }
    public void start_Walk(View v) {    // start_Walk is called by activity.xml
        isWalk = true;
        lowSpeed = LOW_SPEED_WALK; highSpeed = HIGH_SPEED_WALK;
        mainDialog.dismiss();
        go_Clicked();
    }

    public void start_Drive(View v) {
        isWalk = false;
        lowSpeed = LOW_SPEED_DRIVE; highSpeed = HIGH_SPEED_DRIVE;
        mainDialog.dismiss();
        go_Clicked();
    }

//    @SuppressLint("RestrictedApi")
    void go_Clicked() {
        modeStarted = true;
        modePaused = false;
        gpsTracker.stopGPSUpdate();

        beginTrackLog();
        fabGoStop.setImageResource(R.mipmap.button_stop);
        fabPauseRestart.show();
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
        utils.log(logID,"--- Finish Tracking ---");
        utils.log(logID,"avrDist="+(totDistance)/dbCount+", avr speed:"+(totSpeed)/dbCount+" dbCount="+dbCount+" Elapsed="+elapsedTime/60000);
        utils.log("minMax", " sMaxSpeed="+ sMaxSpeed +" sMinSpeed="+ sMinSpeed);
        stopTrackLog();
        fabGoStop.setImageResource(R.mipmap.button_start);
        fabPauseRestart.hide();
        utils.deleteOldLogFiles();
        updateNotification(ACTION_STOP);
    }

    void beginTrackLog() {
        mainMap.clear();
        gpsTracker.startGPSUpdate();
        startTime = System.currentTimeMillis();
        beginTime = startTime;
        prevLogTime = startTime;
//        latLngPos.add(new LatLng(startLatitude, startLongitude));
//        utils.log(logID, "startLog " + startLatitude + " x " + startLongitude);
//        latQues.add(startLatitude); lonQues.add(startLongitude);
        dbCount = 0;
        meters = 0; minutes = 0;
        totSpeed = 0; totDistance = 0;
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
        utils.log("SPEED range","lowSpeed="+lowSpeed+" hiSeed="+highSpeed);
        databaseIO.trackInsert(startTime);
        databaseIO.logInsert(startTime, 0, 0);
        locSouth = 999; locNorth = -999; locWest = 999; locEast = -999;
    }

    double startLat, startLon;
    void stopTrackLog() {
        startLat = latQues.get(latQues.size()-1); startLon = lonQues.get(lonQues.size()-1);
        final double smallLat = (gpsTracker.getGpsLatitude() - startLat) / QUE_COUNT;
        final double smallLon = (gpsTracker.getGpsLongitude() - startLon) / QUE_COUNT;
        gpsTracker.stopGPSUpdate();
        new CountDownTimer(280*QUE_COUNT, 300) {
            public void onTick(long millisUntilFinished) {
                startLat += smallLat; startLon += smallLon;
//                utils.log(logID+millisUntilFinished, "queue "+startLat+"x"+startLon);
                Message msg = updateMarker.obtainMessage(1, new LatLng(startLat, startLon));
                updateMarker.sendMessage(msg);
            }
            public void onFinish() {
                utils.log(logID, "finish "+startLat+"x"+startLon);
                finishTime = System.currentTimeMillis();
                calcMapScale();
                mainMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng((locNorth + locSouth)/2, (locEast + locWest)/2),
                        (float) mapScale - 0.15f));
                elapsedTime = minutes + finishTime - beginTime;
                databaseIO.trackUpdate(startTime, finishTime, (int) meters, (int) elapsedTime / 60000);
//                databaseIO.logInsert(finishTime, startLat, startLon);
                showMarker.drawHereOff();
                showMarker.drawFinish(startLat, startLon, false);
                updateNotification(ACTION_UPDATE);
                updateNotification(ACTION_STOP);
            }
        }.start();
    }

    double sMaxSpeed = -9999f, sMinSpeed = 99999f;
    int resetCount = 0;
    void locationChanged(boolean isActive, double latitude, double longitude) {

        nowLatitude = latitude; nowLongitude = longitude;
        long nowTime = System.currentTimeMillis();
        long deltaTime = nowTime - prevLogTime;
        if (isActive && deltaTime < 250)
            return;
        double distance = mapUtils.getShortDistance(prevLatitude, prevLongitude, nowLatitude, nowLongitude);
        double speed = distance / (double)deltaTime * 1000f * 60f;
        if (isActive) {
            if (speed < lowSpeed || speed > highSpeed) {
                if (resetCount++ < 5) {
                    utils.log("Count " + resetCount, isWalk + "{BAD} " + resetCount + " Speed " + speed + " XTime " + deltaTime);
                    return;
                }
            }
        }
        else
            utils.log("final ", isWalk+ "{forced} Speed " + speed+" XTime "+deltaTime);
        sMaxSpeed = Math.max(sMaxSpeed,speed); sMinSpeed = Math.min(sMinSpeed, speed);
        resetCount = 0;
        elapsedTime = minutes + nowTime - beginTime;
        String s = utils.minute2Text((int) elapsedTime / 60000);
        tvMinutes.setText(s);
        totSpeed += speed;
        totDistance += distance;
        latSVs.add(nowLatitude); lonSVs.add(nowLongitude);
        adjustPosition();
        nowLatitude = latSVs.get(0); nowLongitude = lonSVs.get(0);
        latSVs.remove(0); lonSVs.remove(0);
        if (nowLatitude > locNorth) locNorth = nowLatitude;
        if (nowLatitude < locSouth) locSouth = nowLatitude;
        if (nowLongitude > locEast) locEast = nowLongitude;
        if (nowLongitude < locWest) locWest = nowLongitude;

        latQues.add(nowLatitude); lonQues.add(nowLongitude);
        int color = (int) ((speed-lowSpeed)/highSpeed*100);
        utils.log(logID, "cidx="+color+" speed="+speed+" dist="+distance+" delta="+deltaTime);
        if (color > 100) color = 100; if(color < 0) color = 0;
        color = speedColor[color];
        drawTrackLogs(color);
        meters += mapUtils.getShortDistance(prevLatitude, prevLongitude, nowLatitude, nowLongitude) * 1.1f;
        if (dbCount > (QUE_COUNT-2)) {
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
//        utils.log("GOOD "+isWalk, " speed " + speed+" time "+deltaTime+" avspeed "+(totSpeed)/dbCount);
    }

    void adjustPosition() {
//        int k = QUE_COUNT-2;
//        latSVs.set(k,(latSVs.get(k)+ latSVs.get(k-1))/2);
//        lonSVs.set(k,(lonSVs.get(k)+ lonSVs.get(k-1))/2);
        for (int i = 0; i < QUE_COUNT-2; i++) {
            latSVs.set(i+1,((latSVs.get(i)+ latSVs.get(i+2))/2+latSVs.get(i+1))/2);
            lonSVs.set(i+1,((lonSVs.get(i)+ lonSVs.get(i+2))/2+lonSVs.get(i+1))/2);
        }
    }

    void calcMapScale() {
        double fullMapDistance = mapUtils.getFullMapDistance(locEast, locWest, locSouth, locNorth);
//        utils.log(logID," initial distance :"+fullMapDistance);
        mapScale = mapUtils.getMapScale(fullMapDistance);
//        utils.log(logID, "mapScale >> "+mapScale);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mainMap = googleMap;
        showMarker.init(mainActivity, googleMap);
        nowLatitude = gpsTracker.getGpsLatitude();
        nowLongitude = gpsTracker.getGpsLongitude();
        utils.log(logID, "MapReady "+ nowLatitude +" x "+ nowLongitude+" with scale:"+mapScale);
        mapScale = 18;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(nowLatitude, nowLongitude), mapScale));
        updateNotification(ACTION_HIDE_CONFIRM);
    }

    static double latitudeGPS, longitudeGPS;
    static void locationUpdatedByGPSTracker(Double latitude, Double longitude) {
        Message msg = updateMarker.obtainMessage(0, new LatLng(latitude, longitude));
        updateMarker.sendMessage(msg);
    }

    static void notificationBarTouched(int buttonType) {
        notifyAction.sendEmptyMessage(buttonType);
    }

    void notificationClicked(int operation) {
        switch (operation) {
            case NOTIFICATION_BAR_GO: // GO in Walk Mode
                isWalk = true;
                lowSpeed = LOW_SPEED_WALK; highSpeed = HIGH_SPEED_WALK;
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

    void drawTrackLogs(int color) {
        while (latQues.size() > 2) {
            markerLatLng.set(0, new LatLng(latQues.get(0), lonQues.get(0)));
            markerLatLng.set(1, new LatLng(latQues.get(1), lonQues.get(1)));
            showMarker.drawLine(markerLatLng, isWalk, color);
            latQues.remove(0); lonQues.remove(0);
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
        try {
            databaseIO.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
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

    /*
    generate static color table speedColor index value (only if color table change is required)
     */
    void generateColor() {
        AnimatedColor animatedColor = new AnimatedColor(Color.rgb(0,152,0), Color.rgb(152,0,0));
        StringBuilder s = new StringBuilder();
        s.append("\n");
        for (int i = 0; i < 100; i++) {
            float ratio = (float) i / 100;
            if (i%10 == 0)
                s.append("\n\t\t\t");
            s.append("0x"+String.format("%06X", animatedColor.with(ratio))+", ");
        }
        utils.log(logID,s.toString());
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
