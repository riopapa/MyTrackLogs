package com.urrecliner.mytracklogs;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import static com.urrecliner.mytracklogs.Vars.ACTION_EXIT;
import static com.urrecliner.mytracklogs.Vars.ACTION_INIT;
import static com.urrecliner.mytracklogs.Vars.ACTION_PAUSE;
import static com.urrecliner.mytracklogs.Vars.ACTION_RESTART;
import static com.urrecliner.mytracklogs.Vars.ACTION_START;
import static com.urrecliner.mytracklogs.Vars.ACTION_STOP;
import static com.urrecliner.mytracklogs.Vars.ACTION_UPDATE;
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
            operation = -11;
            return START_STICKY;
        }
        utils.log(logID, "operation : " + operation);
        if (operation != -1) {
            MainActivity.notificationBarTouched(operation);
            return START_STICKY;
        }
        String action = intent.getStringExtra("action");
        if (action == null)
            action = ACTION_INIT;
        utils.log(logID, "action "+action);
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

        Intent mainIntent = new Intent(mContext, MainActivity.class);
        mRemoteViews.setOnClickPendingIntent(R.id.ll_customNotification, PendingIntent.getActivity(mContext, 0, mainIntent, 0));

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
