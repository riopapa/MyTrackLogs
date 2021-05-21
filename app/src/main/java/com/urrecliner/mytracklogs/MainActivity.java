package com.urrecliner.mytracklogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
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
import static com.urrecliner.mytracklogs.Vars.HIGH_DISTANCE_DRIVE;
import static com.urrecliner.mytracklogs.Vars.HIGH_DISTANCE_WALK;
import static com.urrecliner.mytracklogs.Vars.HIGH_SPEED_DRIVE;
import static com.urrecliner.mytracklogs.Vars.HIGH_SPEED_WALK;
import static com.urrecliner.mytracklogs.Vars.LOW_SPEED_DRIVE;
import static com.urrecliner.mytracklogs.Vars.LOW_SPEED_WALK;
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
import static com.urrecliner.mytracklogs.Vars.mActivity;
import static com.urrecliner.mytracklogs.Vars.mapUtils;
import static com.urrecliner.mytracklogs.Vars.sdfDateDayTime;
import static com.urrecliner.mytracklogs.Vars.sharePrefer;
import static com.urrecliner.mytracklogs.Vars.showMarker;
import static com.urrecliner.mytracklogs.Vars.speedColor;
import static com.urrecliner.mytracklogs.Vars.utils;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    final static String logID = "Main";
    private static Handler addTrackMarker, notifyAction;
    FloatingActionButton fabGoStop, fabPauseRestart;
    long prevLogTime, elapsedTime;
    TextView tvStartDate, tvStartTime, tvMeter, tvMinutes;
    Intent serviceIntent;
    LinearLayout llTimeInfo, llTrackInfo;
    double locSouth,  locNorth= -999, locWest, locEast;
    float lowSqrt, highSqrt;
    float mapScale = 17;
    int GPSUpdateCount;
    boolean modeStarted = false, modePaused = false, isWalk = true;
    double prevLatitude = -999, prevLongitude = -999, nowLatitude = 0, nowLongitude = 0;

    GoogleMap mainMap = null;
    Polyline markLines = null;
    ArrayList<LatLng> markerLatLng;
    double startLat = 0, startLng = 0;
    double meters = 0;
    long startTime = 0, finishTime = 0, beginTime = 0, minutes = 0;
    int dbCount = 0;
    double totSpeed = 0, lowSpeed, highSpeed, highDistance;
    AlertDialog mainDialog;

    ArrayList<LatLng> latLngPos;
    ArrayList<Double> latSVs, lngSVs, latQues, lonQues;
    final int QUE_COUNT = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
        mActivity = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        utils = new Utils();
        mapUtils = new MapUtils();
//        Vars.generateColor(); // only when want to generate color table from low speed to high speed
        Permission.ask(this, this);
        initiate();
    }

    void initiate() {

        markerLatLng = new ArrayList<>();
        markerLatLng.add(new LatLng(nowLatitude, nowLongitude));    // add two dummy
        markerLatLng.add(new LatLng(nowLatitude, nowLongitude));
        tvStartDate = findViewById(R.id.startDate);
        tvStartTime = findViewById(R.id.startTime);
        tvMeter = findViewById(R.id.meter);
        tvMinutes = findViewById(R.id.nMinutes);
        llTimeInfo = findViewById(R.id.timeSummary); llTrackInfo = findViewById(R.id.trackInfo);
        llTimeInfo.setVisibility(View.INVISIBLE); llTrackInfo.setVisibility(View.INVISIBLE);
        gpsTracker = new GPSTracker();
        gpsTracker.startGPSUpdate(getApplicationContext());
        sharePrefer = getSharedPreferences("myTracks", Context.MODE_PRIVATE);

        databaseIO = new DatabaseIO();
        fabGoStop = findViewById(R.id.fabGoStop);
        fabPauseRestart = findViewById(R.id.fabPause);

        fabGoStop.setOnClickListener(view -> {
            if (modeStarted) {
                confirmFinish();
            } else {
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(mActivity, R.style.Theme_MaterialComponents_Dialog_MinWidth);
                LayoutInflater mLayoutInflater = mActivity.getLayoutInflater();
                mBuilder.setView(mLayoutInflater.inflate(R.layout.dialog_walk_drive, null));
                mainDialog = mBuilder.create();
                mainDialog.show();
            }
        });

        fabPauseRestart.setOnClickListener(view -> RestartClicked());
        fabPauseRestart.hide();

        String blank = " ";
        tvStartDate.setText(blank); tvStartTime.setText(blank);
        tvMeter = findViewById(R.id.meter); tvMeter.setText(blank);
        tvMinutes = findViewById(R.id.nMinutes); tvMinutes.setText(blank);
        ActionBar ab = this.getSupportActionBar();
        ab.setTitle(R.string.track_recording);
        ab.setIcon(R.mipmap.my_face) ;
        ab.setDisplayUseLogoEnabled(true) ;
        ab.setDisplayShowHomeEnabled(true) ;
        addTrackMarker = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                LatLng latLng = (LatLng) msg.obj;
                locationChanged((msg.what == 0), latLng.latitude, latLng.longitude);
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
        while (nowLatitude == 0) {
            SystemClock.sleep(1000);
            nowLatitude = gpsTracker.getGPSLat();
            nowLongitude = gpsTracker.getGPSLng();
        }
//        gpsTracker.stopGPSUpdate();
        utils.deleteOldLogFiles();
    }

    public void startClicked(View v) {
        isWalk = v.getTag().equals("W");
        mainDialog.dismiss();
        latLngPos = new ArrayList<>(); latSVs = new ArrayList<>(); lngSVs = new ArrayList<>();
        latQues = new ArrayList<>(); lonQues = new ArrayList<>();
        startLat = gpsTracker.getGPSLat();
        startLng = gpsTracker.getGPSLng();
        for (int i = 0; i < QUE_COUNT; i++) { latSVs.add(startLat); lngSVs.add(startLng); }

        go_Clicked();
    }

    void RestartClicked() {
        if (modeStarted) {
            if (modePaused) {      // already paused, let restart
                modePaused = false;
                gpsTracker.startGPSUpdate(getApplicationContext());
                beginTime = System.currentTimeMillis();
                prevLogTime = beginTime;
                fabPauseRestart.setImageResource(R.mipmap.button_pause);
                prevLatitude = gpsTracker.getGPSLat();
                prevLongitude = gpsTracker.getGPSLng();
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

    void go_Clicked() {
        if (isWalk) {
            lowSpeed = LOW_SPEED_WALK; highSpeed = HIGH_SPEED_WALK; highDistance = HIGH_DISTANCE_WALK;
            lowSqrt = (float) Math.sqrt(LOW_SPEED_WALK); highSqrt = (float) Math.sqrt(HIGH_DISTANCE_WALK);
        } else {
            lowSpeed = LOW_SPEED_DRIVE; highSpeed = HIGH_SPEED_DRIVE; highDistance = HIGH_DISTANCE_DRIVE;
            lowSqrt = (float) Math.sqrt(LOW_SPEED_DRIVE); highSqrt = (float) Math.sqrt(HIGH_DISTANCE_DRIVE);
        }

        modeStarted = true;
        modePaused = false;

        mainMap.clear();
        gpsTracker.startGPSUpdate(getApplicationContext());
        GPSUpdateCount = 0;

        fabGoStop.setImageResource(R.mipmap.button_stop);
        fabPauseRestart.show();
        updateNotification(ACTION_START);
        highSqrt = (float) ((isWalk) ? Math.sqrt(HIGH_SPEED_WALK): Math.sqrt(HIGH_SPEED_DRIVE));
        showMarker = new ShowMarker();
        showMarker.init(mActivity, mainMap);
        showMarker.drawStart(startLat, startLng, false);
    }

    void finishTrackLog() {
        modeStarted = false;

        updateNotification(ACTION_UPDATE);
        updateNotification(ACTION_STOP);
        gpsTracker.stopGPSUpdate();

        fabGoStop.setImageResource(R.mipmap.button_start);
        fabPauseRestart.hide();
        updateNotification(ACTION_STOP);

        if (latQues.size() < 10)
            return;
        finalizeTrackUpdate();
        String s = "--- Finish Tracking ---"
                + "\navr speed:"+(totSpeed)/dbCount + " Speed=("+ sMaxSpeed +"~"+ sMinSpeed+")"
                +"\ndbCount="+dbCount + "Elapsed="+(elapsedTime/60000) ;
        utils.log(logID,s);
        utils.log2Download(logID,s);
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    private void finalizeTrackUpdate() {
        startLat = latQues.get(latQues.size() - 1);
        startLng = lonQues.get(lonQues.size() - 1);
        final double dLat = (gpsTracker.getGPSLat() - startLat) / QUE_COUNT;
        final double dLng = (gpsTracker.getGPSLng() - startLng) / QUE_COUNT;
        new CountDownTimer(180*QUE_COUNT, 100) {
            public void onTick(long millisUntilFinished) {
                startLat += dLat; startLng += dLng;
                Message msg = addTrackMarker.obtainMessage(1, new LatLng(startLat, startLng));
                addTrackMarker.sendMessage(msg);
            }
            public void onFinish() {
                utils.log(logID, "finish "+startLat+"x"+ startLng);
                finishTime = System.currentTimeMillis();
                mapScale = calcMapScale();
                mainMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng((locNorth + locSouth)/2, (locEast + locWest)/2), mapScale - 0.15f));
                elapsedTime = minutes + finishTime - beginTime;
                databaseIO.trackUpdate(startTime, finishTime, (isWalk) ? 0:1,
                        (int) meters, (int) elapsedTime / 60000);
                showMarker.drawHereOff();
                showMarker.drawFinish(startLat, startLng, false);
            }
        }.start();
    }

    double sMaxSpeed = -9999f, sMinSpeed = 99999f;
    void locationChanged(boolean isActive, double latitude, double longitude) {

        GPSUpdateCount++;
        if (GPSUpdateCount < 4) {
            startLat = latitude; startLng = longitude;
            locSouth = latitude; locNorth = latitude; locWest = longitude; locEast = longitude;
//            mainMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), mapScale));
            showMarker.drawStart(startLat, startLng, false);
            return;
        } else if (GPSUpdateCount == 4)
            initNewTrackingValues();

//        utils.log("GPS "+GPSUpdateCount,latitude+" x "+longitude);
        long nowTime = System.currentTimeMillis();
        long deltaTime = nowTime - prevLogTime;
        String s;
        if (isActive && deltaTime < 1000)
            return;
        float distance = mapUtils.calcDistance(prevLatitude, prevLongitude, latitude, longitude);
        float speed = distance / (float)deltaTime * 1000f * 60f;

        if (isActive) {
            if (speed > (highSpeed + highSpeed) || distance > highDistance) {
//            utils.log("Bad Speed", isWalk + "{Too BAD} Speed = " + speed + " dTime = " + deltaTime+", Dist = "+distance);
                prevLatitude = latitude;
                prevLongitude = longitude;
                return;
            }
            sMaxSpeed = Math.max(sMaxSpeed,speed); sMinSpeed = Math.min(sMinSpeed, speed);
            elapsedTime = minutes + nowTime - beginTime;
            s = utils.minute2Text((int) elapsedTime / 60000);
            tvMinutes.setText(s);
            totSpeed += speed;
        }
        nowLatitude = latitude; nowLongitude = longitude;
        latSVs.add(nowLatitude); lngSVs.add(nowLongitude);
        adjustPosition();
        nowLatitude = latSVs.get(0); nowLongitude = lngSVs.get(0);
        latSVs.remove(0); lngSVs.remove(0);
        if (nowLatitude > locNorth) locNorth = nowLatitude;
        if (nowLatitude < locSouth) locSouth = nowLatitude;
        if (nowLongitude > locEast) locEast = nowLongitude;
        if (nowLongitude < locWest) locWest = nowLongitude;

        latQues.add(nowLatitude); lonQues.add(nowLongitude);

        int color = (int) (Math.sqrt(speed)/highSqrt*20);
        if (color > 20) color = 20; if(color < 0) color = 0;
        if (isWalk) color = 20 - color; // red to green (drive), green to red (walk)
        int colorCode = speedColor[color];
        drawTrackLogs(colorCode);
        meters += mapUtils.calcDistance(prevLatitude, prevLongitude, nowLatitude, nowLongitude);
        if (dbCount > (QUE_COUNT-2)) {
            databaseIO.logInsert(nowTime, nowLatitude, nowLongitude);
            databaseIO.trackUpdate(startTime, nowTime, (isWalk) ? 0:1, (int) meters, (int) elapsedTime / 60000);
        }
        if (isActive) {
            s = decimalComma.format(meters) + "m / " + dbCount;
            tvMeter.setText(s);
            updateNotification(ACTION_UPDATE);
            prevLatitude = nowLatitude;
            prevLongitude = nowLongitude;
            prevLogTime = nowTime;
        }
        dbCount++;
    }

    void initNewTrackingValues() {
        startTime = System.currentTimeMillis();
        beginTime = startTime;
        prevLogTime = startTime;
        dbCount = 0;
        meters = 0; minutes = 0;totSpeed = 0;
        tvStartDate.setText(utils.long2DateDay(startTime));
        tvStartTime.setText(utils.long2Time(startTime));
        tvMinutes.setText(R.string.zero_minutes);
        tvMeter.setText(R.string.zero_meters);
        prevLatitude = gpsTracker.getGPSLat();
        prevLongitude = gpsTracker.getGPSLng();
        llTimeInfo.setVisibility(View.VISIBLE);
        llTrackInfo.setVisibility(View.VISIBLE);
        if (markLines != null) {
            markLines.remove();
            markLines = null;
        }
        showMarker.drawStart(startLat, startLng, false);
        String s = (isWalk) ? "Walk":"Drive" + " start @ "+sdfDateDayTime.format(startTime) +
                " speedCheck ("+lowSpeed+"~"+highSpeed+")";
        utils.log("Start",s);
        utils.log2Download("Start",s);
        Toast.makeText(mContext,s, Toast.LENGTH_LONG).show();
        databaseIO.trackInsert(startTime);
        databaseIO.logInsert(startTime, 0, 0);
        locSouth = prevLatitude; locNorth = prevLatitude;
        locWest = prevLongitude; locEast = prevLongitude;
    }

    void adjustPosition() {
        for (int i = 0; i < QUE_COUNT-2; i++) {
            latSVs.set(i+1,((latSVs.get(i)+ latSVs.get(i+2))/2+latSVs.get(i+1))/2);
            lngSVs.set(i+1,((lngSVs.get(i)+ lngSVs.get(i+2))/2+ lngSVs.get(i+1))/2);
        }
    }

    float calcMapScale() {
        double fullMapDistance = mapUtils.calcDistance(locSouth, locEast, locNorth, locWest);
        return mapUtils.getMapScale(fullMapDistance);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mainMap = googleMap;
        nowLatitude = gpsTracker.getGPSLat();
        nowLongitude = gpsTracker.getGPSLng();
        updateNotification(ACTION_HIDE_CONFIRM);
        mapScale = 18f;
        utils.log(logID, "MapReady "+ nowLatitude +" x "+ nowLongitude+" with scale:"+mapScale);
        locSouth = nowLatitude; locNorth = nowLatitude; locWest = nowLongitude; locEast = nowLongitude;
        mainMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(nowLatitude, nowLongitude), mapScale));
        showMarker = new ShowMarker();
        showMarker.init(mActivity, mainMap);
    }

    static void newGPSArrived(Double latitude, Double longitude) {
        Message msg = addTrackMarker.obtainMessage(0, new LatLng(latitude, longitude));
        addTrackMarker.sendMessage(msg);
    }

    static void notificationBarTouched(int buttonType) {
        notifyAction.sendEmptyMessage(buttonType);
    }

    void notificationClicked(int operation) {
        switch (operation) {
            case NOTIFICATION_BAR_GO: // GO in Walk Mode
                go_Clicked();
                break;
            case NOTIFICATION_BAR_PAUSE_RESTART: // PAUSE_RESTART
                RestartClicked();
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
                finishTrackLog();
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
        else {
            confirmFinish();
            if (!modeStarted)
                exit_Application();
        }
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
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("Confirm Finish ");
        String s = "Are you sure to finish tracking?";
        builder.setMessage(s);
        builder.setNegativeButton("Finish",
                (dialog, which) -> {
                    updateNotification(ACTION_HIDE_CONFIRM);
                    notifyAction.sendEmptyMessage(NOTIFICATION_BAR_CONFIRMED_STOP);
                });
        builder.setPositiveButton("No, Continue ",
                (dialog, which) -> {
                });
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainMap != null) {
            mapScale = calcMapScale();
            mainMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(nowLatitude, nowLongitude), mapScale));
        }
    }

}
