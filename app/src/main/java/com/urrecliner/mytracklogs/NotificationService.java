package com.urrecliner.mytracklogs;


import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import java.util.List;

import static com.urrecliner.mytracklogs.Vars.ACTION_EXIT;
import static com.urrecliner.mytracklogs.Vars.ACTION_INIT;
import static com.urrecliner.mytracklogs.Vars.ACTION_PAUSE;
import static com.urrecliner.mytracklogs.Vars.ACTION_RESTART;
import static com.urrecliner.mytracklogs.Vars.ACTION_START;
import static com.urrecliner.mytracklogs.Vars.ACTION_STOP;
import static com.urrecliner.mytracklogs.Vars.ACTION_UPDATE;
import static com.urrecliner.mytracklogs.Vars.mainActivity;
import static com.urrecliner.mytracklogs.Vars.modeStarted;
import static com.urrecliner.mytracklogs.Vars.utils;

public class NotificationService extends Service {

    private Context mContext;
    NotificationCompat.Builder mBuilder = null;
    NotificationChannel mNotificationChannel = null;
    NotificationManager mNotificationManager;
    private RemoteViews mRemoteViews;
    private final String logID = "Notify";
    private int iconId;
    private static final int GO_STOP = 1;
    private static final int PAUSE_RESTART = 2;
    private static final int EXIT_APP = 3;
    private static final int SHOW_MAIN = 4;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        if (null != mRemoteViews) {
            mRemoteViews.removeAllViews(R.layout.notification_bar);
            mRemoteViews = null;
        }
        mRemoteViews = new RemoteViews(mContext.getPackageName(), R.layout.notification_bar);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int operation = -1;
        try {
            operation = intent.getIntExtra("operation",-1);
        } catch (Exception e) {
            utils.log(logID, "operation EXCEPTION");
            return START_STICKY;
        }
        utils.log(logID, "operation : " + operation);
        if (operation == 4) {
            showInForeground();
        }
        else if (operation != -1) {
            if (operation == 1 && modeStarted) {
                showInForeground();
                confirmFinish();
            }
            else {
                MainActivity.notificationBarTouched(operation);
                return START_STICKY;
            }
        }

        String action = intent.getStringExtra("action");
        utils.log(logID, "action "+action);
        if (action == null)
            return START_NOT_STICKY;

        createNotification();
        switch (action) {
            case ACTION_UPDATE:
                mRemoteViews.setTextViewText(R.id.nLaps,intent.getStringExtra("laps"));
                break;
            case ACTION_INIT:
                mBuilder.setSmallIcon(R.mipmap.my_track_log_small);
                mRemoteViews.setTextViewText(R.id.nDateTime, "Ready to\nStart");
                mRemoteViews.setTextViewText(R.id.nLaps,"");
                mRemoteViews.setImageViewResource(R.id.nGoStop, R.mipmap.button_start);
                mRemoteViews.setViewVisibility(R.id.nPause, View.GONE);
                mRemoteViews.setViewVisibility(R.id.nExit, View.VISIBLE);
                break;
            case ACTION_START:
                mBuilder.setSmallIcon(R.mipmap.button_start);
                mRemoteViews.setTextViewText(R.id.nDateTime,
                        utils.long2DateDay(System.currentTimeMillis())+"\n"+utils.long2Time(System.currentTimeMillis()));
                mRemoteViews.setTextViewText(R.id.nLaps,"");
                mRemoteViews.setImageViewResource(R.id.nGoStop, R.mipmap.button_stop);
                mRemoteViews.setImageViewResource(R.id.nPause, R.mipmap.button_pause);
                mRemoteViews.setViewVisibility(R.id.nPause, View.VISIBLE);
                mRemoteViews.setViewVisibility(R.id.nExit, View.GONE);
                break;
            case ACTION_STOP:
                mBuilder.setSmallIcon(R.mipmap.button_stop);
                mRemoteViews.setImageViewResource(R.id.nGoStop, R.mipmap.button_start);
                mRemoteViews.setViewVisibility(R.id.nPause, View.GONE);
                mRemoteViews.setViewVisibility(R.id.nExit, View.VISIBLE);
                break;
            case ACTION_PAUSE:
                mBuilder.setSmallIcon(R.mipmap.button_pause);
                mRemoteViews.setImageViewResource(R.id.nGoStop, R.mipmap.button_stop);
                mRemoteViews.setImageViewResource(R.id.nPause, R.mipmap.button_restart);
                mRemoteViews.setViewVisibility(R.id.nPause, View.VISIBLE);
                mRemoteViews.setViewVisibility(R.id.nExit, View.GONE);
                break;
            case ACTION_RESTART:
                mBuilder.setSmallIcon(R.mipmap.button_start);
                mRemoteViews.setImageViewResource(R.id.nGoStop, R.mipmap.button_stop);
                mRemoteViews.setImageViewResource(R.id.nPause, R.mipmap.button_pause);
                mRemoteViews.setViewVisibility(R.id.nPause, View.VISIBLE);
                mRemoteViews.setViewVisibility(R.id.nExit, View.GONE);
                break;
            case ACTION_EXIT:
                mBuilder = null;
                mRemoteViews = null;
                return START_NOT_STICKY;
        }
        startForeground(111, mBuilder.build());
        return START_STICKY;
    }

    private void showInForeground() {
        ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks =am.getRunningTasks(10); //얻어올 task갯수 원하는 만큼의 수를 입력하면 된다.
        if(!tasks.isEmpty()) {
            int tasksSize = tasks.size();
            utils.log(logID, "tasksSize "+tasksSize);
            for(int i = 0; i < tasksSize;  i++) {
                ActivityManager.RunningTaskInfo taskInfo = tasks.get(i);
                utils.log(logID, taskInfo.topActivity.getPackageName()+" vs "+ mContext.getPackageName());
                if(taskInfo.topActivity.getPackageName().equals(mContext.getPackageName())) {
                    utils.log(logID, taskInfo.topActivity.getPackageName()+" EQUALS "+ mContext.getPackageName());
                    am.moveTaskToFront(taskInfo.id, 0);
                }
            }
        }

    }

    private void confirmFinish() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle("Confirm Finish ");
        String s = "Are you sure to finish tracking?";
        builder.setMessage(s);
        builder.setNegativeButton("Yes, Finish",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.notificationBarTouched(9);
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setAllCaps(false);
    }


    private void createNotification() {

        if (null == mNotificationChannel) {
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotificationChannel = new NotificationChannel("default","default", NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(mNotificationChannel);
        }
        if (null == mBuilder) {
            mBuilder = new NotificationCompat.Builder(mContext,"default")
                    .setSmallIcon(R.mipmap.my_track_log_small)
                    .setContent(mRemoteViews)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .setOngoing(false);
        }

//        Intent mainIntent = new Intent(mContext, MainActivity.class);
//        mRemoteViews.setOnClickPendingIntent(R.id.ll_customNotification, PendingIntent.getActivity(mContext, 0, mainIntent, 0));

        Intent barIntent = new Intent(this, NotificationService.class);
        barIntent.putExtra("operation", SHOW_MAIN);
        PendingIntent barPI = PendingIntent.getService(mContext, 4, barIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(barPI);
        mRemoteViews.setOnClickPendingIntent(R.id.ll_customNotification, barPI);

        Intent goStopIntent = new Intent(this, NotificationService.class);
        goStopIntent.putExtra("operation", GO_STOP);
        PendingIntent goStopPI = PendingIntent.getService(mContext, 1, goStopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(goStopPI);
        mRemoteViews.setOnClickPendingIntent(R.id.nGoStop, goStopPI);

        Intent pauseIntent = new Intent(this, NotificationService.class);
        pauseIntent.putExtra("operation", PAUSE_RESTART);
        PendingIntent pausePI = PendingIntent.getService(mContext, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pausePI);
        mRemoteViews.setOnClickPendingIntent(R.id.nPause, pausePI);

        Intent exitIntent = new Intent(this, NotificationService.class);
        exitIntent.putExtra("operation", EXIT_APP);
        PendingIntent exitPI = PendingIntent.getService(mContext, 3, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(exitPI);
        mRemoteViews.setOnClickPendingIntent(R.id.nExit, exitPI);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
