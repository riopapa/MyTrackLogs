package com.urrecliner.mytracklogs;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

public class NotificationService extends Service {

    private Context mContext;
    NotificationCompat.Builder mBuilder = null;
    NotificationChannel mNotificationChannel = null;
    NotificationManager mNotificationManager;
    private RemoteViews mRemoteViews;
    String dateTimeStr, meterStr, minuteStr;
    int iconId;
    private final String logID = "Notify";
    private static final int STOP_SAY = 10011;
    private static final int RE_LOAD = 10022;

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

//        dateTimeStr = intent.getStringExtra("dateTime");
//        meterStr = decimalComma.format(intent.getIntExtra("meters",0))+"m ";
//        minuteStr = utils.minute2Text(intent.getIntExtra("minutes",0));
//        iconId = intent.getIntExtra("iconId",0);
//        utils.log(logID, "service date time "+dateTimeStr);"
        dateTimeStr = "dateTimeStr";
        meterStr = "meterStr";
        minuteStr = "minuteStr";
        iconId = R.mipmap.button_pause;
        createNotification();
        updateRemoteViews(dateTimeStr, meterStr, minuteStr, iconId);
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

        Intent reloadIntent = new Intent(this, NotificationService.class);
        reloadIntent.putExtra("operation", RE_LOAD);
        reloadIntent.putExtra("isFromNotification", true);
        PendingIntent reloadPi = PendingIntent.getService(mContext, 1, reloadIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(reloadPi);
        mRemoteViews.setOnClickPendingIntent(R.id.nGoStop, reloadPi);

    }

    private void updateRemoteViews(String dateTime, String meters, String minutes, int iconId) {
        mRemoteViews.setImageViewResource(R.id.nGoStop, iconId);
        mRemoteViews.setTextViewText(R.id.nDateTime, dateTime);
        mRemoteViews.setTextViewText(R.id.nKms, meters);
        mRemoteViews.setTextViewText(R.id.nMinutes, minutes);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
