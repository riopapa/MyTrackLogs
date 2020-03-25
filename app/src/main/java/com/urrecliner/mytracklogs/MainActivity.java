package com.urrecliner.mytracklogs;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.mytracklogs.Vars.databaseIO;
import static com.urrecliner.mytracklogs.Vars.gpsTracker;
import static com.urrecliner.mytracklogs.Vars.logInterval;
import static com.urrecliner.mytracklogs.Vars.mContext;
import static com.urrecliner.mytracklogs.Vars.mainActivity;
import static com.urrecliner.mytracklogs.Vars.sharePrefer;
import static com.urrecliner.mytracklogs.Vars.utils;

public class MainActivity extends AppCompatActivity {

    TextView tvMeter = null, tvMinutes = null;
    Timer logTimer = new Timer(), displayTimer = new Timer();
    int cnt = 0;
    private boolean modeStarted = false, modePaused = false;
    TimerTask logTimerTask, displayTask;
    FloatingActionButton fabGoStop;
    Location locationPrev, locationNow;
    long prevLogTime;
    TextView tvNowDate, tvNowTime, tvLogCount;
    View tvMainScreen;

    double startLatitude = 0, startLongitude = 0, prevLatitude = 0, prevLongitude = 0, meters = 0;
    long startTime = 0, finishTime = 0, beginTime = 0, minutes = 0, pauseTime = 0;
    int logCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mainActivity = this;
        askPermission();
        utils = new Utils();
        utils.log("Main","Started");
        setContentView(R.layout.activity_main);
        tvNowDate = findViewById(R.id.nowDate);
        tvNowTime = findViewById(R.id.nowTime);
        tvLogCount = findViewById(R.id.logCount);
        tvMainScreen = findViewById(R.id.mainScreen);
        final TextView tvStartDate = findViewById(R.id.startDate);
        TextView tvStartTime = findViewById(R.id.startTime);

        gpsTracker = new GPSTracker(mContext);
        sharePrefer = getSharedPreferences("myTracks", Context.MODE_PRIVATE);
//        logInterval = sharePrefer.getInt("logInterval", 10) * 1000;
        logInterval = 15*1000;
        if (gpsTracker.canGetLocation()) {
            double latitude = gpsTracker.getLatitude();
            double longitude = gpsTracker.getLongitude();
            utils.log("Main", "Start app @ "+latitude+" x "+longitude);
        } else {
            utils.log("Err","Can't get location.");
        }

        databaseIO = new DatabaseIO();

        fabGoStop = findViewById(R.id.fabGoStop);
        fabGoStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delayButtonClick();
                if (modeStarted) {  // already started
                    if (modePaused) {       // restart after pause
                        modePaused = false;
                        startTimerTask();
                        prevLogTime = System.currentTimeMillis();
                        beginTime = System.currentTimeMillis();
                        fabGoStop.setImageResource(R.mipmap.button_pause);
                        tvMainScreen.setBackgroundColor(getResources().getColor(R.color.trackActive,mContext.getTheme()));
                        Toast.makeText(mContext,"Recording continued\n\n",Toast.LENGTH_LONG).show();
                    }
                    else {
                        modePaused = true;
                        stopTimerTask();
                        minutes += System.currentTimeMillis() - beginTime;
                        fabGoStop.setImageResource(R.mipmap.button_restart);
                        tvMainScreen.setBackgroundColor(getResources().getColor(R.color.design_default_color_primary,mContext.getTheme()));
                        Toast.makeText(mContext,"Recording PAUSED\n\n",Toast.LENGTH_LONG).show();
                    }
                }
                else {  // then newly start
                    modeStarted = true;
                    modePaused = false;
                    prevLogTime = System.currentTimeMillis();
                    tvMainScreen.setBackgroundColor(getResources().getColor(R.color.trackActive,mContext.getTheme()));
                    startLog();
                    delayButtonClick();
                    fabGoStop.setImageResource(R.mipmap.button_pause);
                    Toast.makeText(mContext,"Start Recording\n\n",Toast.LENGTH_LONG).show();
                }
                utils.deleteOldLogFiles();
            }
        });

        fabGoStop.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (modeStarted) {
                    modeStarted = false;
                    modePaused= false;
                    finishLog();
                    fabGoStop.setImageResource(R.mipmap.button_start);
                    tvMainScreen.setBackgroundColor(getResources().getColor(R.color.design_default_color_primary,mContext.getTheme()));
                    Toast.makeText(mContext,"Stop Recording\n\n",Toast.LENGTH_LONG).show();
                    delayButtonClick();
                }
                return false;
            }
        });

        displayDatTime();
        locationPrev = new Location("");
        locationNow = new Location("");
        String blank = " ";
        tvStartDate.setText(blank);
        tvStartTime.setText(blank);
        tvMeter = findViewById(R.id.meter);
        tvMeter.setText(blank);
        tvMinutes = findViewById(R.id.minutes);
        tvMinutes.setText(blank);
        ActionBar ab = this.getSupportActionBar();
        ab.setTitle(" 기록하기");
        ab.setIcon(R.mipmap.my_face) ;
        ab.setDisplayUseLogoEnabled(true) ;
        ab.setDisplayShowHomeEnabled(true) ;

    }

    private void displayDatTime() {
        displayTask = new TimerTask() {     // show now date & time
            @Override
            public void run() {
                long nowTime = System.currentTimeMillis();
                tvNowDate.setText(utils.long2DateDay(nowTime));
                tvNowTime.setText(utils.long2Time(nowTime));
                finishTime = System.currentTimeMillis();
                if (modeStarted) {
                    long dur = (minutes == 0) ? finishTime - startTime:minutes + finishTime - beginTime;
                    databaseIO.trackUpdate(startTime, finishTime, (int) meters, (int) (dur / 60000));
                }
            }
        };
        displayTimer.schedule(displayTask,100 ,60*1000);
    }

    void delayButtonClick() {

        fabGoStop.setEnabled(false);
        new Timer().schedule(new TimerTask() {  // autoStart
            @Override
            public void run() {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fabGoStop.setEnabled(true);
                    }
                });
            }
        }, 3000);
    }

    final static double distanceDelta = 0.000003;
    DecimalFormat decimalFormat = new DecimalFormat("##,###,###");
    void startTimerTask()
    {
        stopTimerTask();
        gpsTracker.getLocation();
        prevLatitude = gpsTracker.getLatitude();
        prevLongitude = gpsTracker.getLongitude();
        logTimerTask = new TimerTask() {
            @Override
            public void run() {
                long nowTime = System.currentTimeMillis();
                TextView tvDuration = findViewById(R.id.minutes);
                long dur = minutes + nowTime - beginTime;
                String s = ""+ (dur / 60000);
                tvDuration.setText(s);

                gpsTracker.getLocation();
                double latitude = gpsTracker.getLatitude();
                double longitude = gpsTracker.getLongitude();
                if (Math.abs(prevLongitude-longitude) > distanceDelta || Math.abs(prevLatitude-latitude) > distanceDelta) {
                    locationPrev.setLatitude(prevLatitude);
                    locationPrev.setLongitude(prevLongitude);
                    locationNow.setLatitude(latitude);
                    locationNow.setLongitude(longitude);
                    double distance = locationPrev.distanceTo(locationNow);
                    double speed = distance * 60 * 60 / (double) ((System.currentTimeMillis() - prevLogTime)/1000);
                    if (speed > 7200)
                        utils.log("time","fast distance="+distance+", speed = "+speed);
                    else
                        utils.log("time","dist="+distance+", speed= "+speed);

                    meters += distance;
                    tvMeter.setText(decimalFormat.format(meters));
                    databaseIO.logInsert(nowTime, latitude, longitude);
                    s = ""+logCount++;
                    tvLogCount.setText(s);
                    prevLatitude = latitude;
                    prevLongitude = longitude;
                    prevLogTime = System.currentTimeMillis();
                }
                cnt++;
            }
        };
        gpsTracker.getLocation();
        logTimer.schedule(logTimerTask,5000 ,logInterval);
    }

    void stopTimerTask()
    {
        delayButtonClick();
        if(logTimerTask != null) {
            logTimerTask.cancel();
            logTimerTask = null;
        }
        gpsTracker.stopUsingGPS();
    }

    void startLog() {
        startTime = System.currentTimeMillis();
        beginTime = startTime;
        gpsTracker.getLocation();
        startLatitude = gpsTracker.getLatitude();
        startLongitude = gpsTracker.getLongitude();
        finishTime = 0;
        meters = 0;
        minutes = 0;
        databaseIO.logInsert(startTime, startLatitude, startLongitude);
        databaseIO.trackInsert(startTime);
        TextView tvStartDate = findViewById(R.id.startDate);
        TextView tvStartTime = findViewById(R.id.startTime);
        TextView tvDuration = findViewById(R.id.minutes);
        tvStartDate.setText(utils.long2DateDay(startTime));
        tvStartTime.setText(utils.long2Time(startTime));
        tvDuration.setText("0");
        startTimerTask();
        MenuItem item = mainMenu.findItem(R.id.showNowMap);
        item.setVisible(true);
    }

    void finishLog() {
        stopTimerTask();
        minutes += System.currentTimeMillis() - beginTime;
        databaseIO.trackUpdate(startTime, finishTime, (int)meters, (int)minutes/60000);
    }

    @Override
    public void onBackPressed() {
        finish();
        finishLog();
    }

    Menu mainMenu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mainMenu = menu;
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem item = menu.findItem(R.id.showNowMap);
        item.setVisible(modeStarted);
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

            case R.id.showNowMap:
                if (minutes == 0)
                    duration = finishTime - startTime;
                else {
                    duration = minutes + finishTime - beginTime;
                }
                intent = new Intent(MainActivity.this, MapActivity.class);
                intent.putExtra("startTime", startTime);
                intent.putExtra("finishTime", System.currentTimeMillis());
                intent.putExtra("minutes", (int) duration / 60000);
                intent.putExtra("meters", (int) meters);
                startActivity(intent);
                return true;

            case R.id.log_view:
                intent = new Intent(MainActivity.this, TrackActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
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
