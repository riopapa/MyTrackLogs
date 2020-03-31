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

import static com.urrecliner.mytracklogs.Vars.utils;

public class NotificationService extends Service {

    private Context mContext;
    NotificationCompat.Builder mBuilder = null;
    NotificationChannel mNotificationChannel = null;
    NotificationManager mNotificationManager;
    private RemoteViews mRemoteViews;
    private final String logID = "Notify";
    private int iconId;
    private static final int GO_STOP = 10011;
    private static final int PAUSE_RESTART = 10022;
    private static final int EXIT_APP = 10033;
    @Override
    public void onCreate() {
        super.onCreate();
        if (utils == null)
            utils = new Utils();
//        createNotificationChannel();
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

        int operation = intent.getIntExtra("operation", -1);
        utils.log(logID, "operation : "+operation);
        if (operation != -1) {
            switch (operation) {
                case GO_STOP:
                    MainActivity.notificationBarTouched(0);
                    break;
                case PAUSE_RESTART:
                    MainActivity.notificationBarTouched(1);
                    break;
                case EXIT_APP:
                    MainActivity.notificationBarTouched(2);
                    break;
            }
            return START_STICKY;
        }
        int status = intent.getIntExtra("status",0);
        createNotification();
        switch (status) {
            case 0: // load at first time
                mRemoteViews.setTextViewText(R.id.nDateTime, "Ready to\nStart");
                mRemoteViews.setImageViewResource(R.id.nGoStop, R.mipmap.button_start);
                mRemoteViews.setViewVisibility(R.id.nPause, View.GONE);
                mRemoteViews.setViewVisibility(R.id.nExit, View.VISIBLE);
                break;
            case 1: // Start pressed, set Start Date
                mRemoteViews.setTextViewText(R.id.nDateTime, intent.getStringExtra("dateTime"));
                mRemoteViews.setTextViewText(R.id.nLaps,"");
                mRemoteViews.setImageViewResource(R.id.nGoStop, R.mipmap.button_stop);
                mRemoteViews.setImageViewResource(R.id.nPause, R.mipmap.button_pause);
                mRemoteViews.setViewVisibility(R.id.nPause, View.VISIBLE);
                mRemoteViews.setViewVisibility(R.id.nExit, View.GONE);
                break;
            case 2: // update distance, minutes
                mRemoteViews.setTextViewText(R.id.nLaps,intent.getStringExtra("laps"));
                break;
            case 3: // end recording
                mRemoteViews.setImageViewResource(R.id.nGoStop, R.mipmap.button_start);
                mRemoteViews.setViewVisibility(R.id.nPause, View.GONE);
                mRemoteViews.setViewVisibility(R.id.nExit, View.VISIBLE);
                break;
            case 4: // pause pressed
                mRemoteViews.setImageViewResource(R.id.nGoStop, R.mipmap.button_stop);
                mRemoteViews.setImageViewResource(R.id.nPause, R.mipmap.button_restart);
                mRemoteViews.setViewVisibility(R.id.nPause, View.VISIBLE);
                mRemoteViews.setViewVisibility(R.id.nExit, View.GONE);
                break;
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
                    .setSmallIcon(R.mipmap.button_pause)
                    .setContent(mRemoteViews)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(false)
                    .setOngoing(true);
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

    private void updateRemoteViews(String dateTime, String meters, int iconId) {
        utils.log(logID, "dateTime "+dateTime);
        mRemoteViews.setImageViewResource(R.id.nGoStop, iconId);
        mRemoteViews.setTextViewText(R.id.nDateTime, dateTime);
        mRemoteViews.setTextViewText(R.id.nLaps, meters);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
