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

    private Context context;
    NotificationCompat.Builder mBuilder = null;
    NotificationChannel mNotificationChannel = null;
    NotificationManager mNotificationManager;
    private RemoteViews mRemoteViews;
    String dateTimeStr, meterStr, minuteStr;
    int iconId;
    private final String logID = "Notify";

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        if (null != mRemoteViews) {
            mRemoteViews.removeAllViews(R.layout.notification_bar);
            mRemoteViews = null;
        }
        mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.notification_bar);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

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
            mBuilder = new NotificationCompat.Builder(context,"default")
                    .setSmallIcon(R.mipmap.button_pause)
                    .setContent(mRemoteViews)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(false)
                    .setOngoing(true);
        }

        Intent mainIntent = new Intent(context, MainActivity.class);
        mRemoteViews.setOnClickPendingIntent(R.id.ll_customNotification, PendingIntent.getActivity(context, 0, mainIntent, 0));

        Intent intent = new Intent(this, NotificationService.class);
        PendingIntent pi = PendingIntent.getService(context, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pi);
        mRemoteViews.setOnClickPendingIntent(R.id.nGoStop, pi);
    }

    private void updateRemoteViews(String dateTime, String meters, String minutes, int iconId) {
        final String fDateTime = dateTime, fMeters = meters, fMinutes = minutes;
        final int fIconId = iconId;
//        mainActivity.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//
//                mRemoteViews.setImageViewResource(R.id.nGoStop, fIconId);
//                mRemoteViews.setTextViewText(R.id.nDateTime, fDateTime);
//                mRemoteViews.setTextViewText(R.id.nKms, fMeters);
//                mRemoteViews.setTextViewText(R.id.nMinutes, fMinutes);
////        mRemoteViews.setTextColor(R.id.nDateTime, color);
////        mRemoteViews.setTextColor(R.id.nKms, color);
////        mRemoteViews.setTextColor(R.id.nMinutes, color);
//            }
//        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
