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
import android.widget.LinearLayout;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import java.util.List;

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
import static com.urrecliner.mytracklogs.Vars.NOTIFICATION_BAR_GO;
import static com.urrecliner.mytracklogs.Vars.NOTIFICATION_BAR_GO_STOP;
import static com.urrecliner.mytracklogs.Vars.NOTIFICATION_BAR_HIDE_CONFIRM;
import static com.urrecliner.mytracklogs.Vars.NOTIFICATION_BAR_NO_CONTINUE;
import static com.urrecliner.mytracklogs.Vars.NOTIFICATION_BAR_NO_ACTION;
import static com.urrecliner.mytracklogs.Vars.NOTIFICATION_BAR_PAUSE_RESTART;
import static com.urrecliner.mytracklogs.Vars.NOTIFICATION_BAR_SHOW_CONFIRM;
import static com.urrecliner.mytracklogs.Vars.NOTIFICATION_BAR_SHOW_MAIN;
import static com.urrecliner.mytracklogs.Vars.NOTIFICATION_BAR_CONFIRMED_STOP;
import static com.urrecliner.mytracklogs.Vars.NOTIFICATION_BAR_YES_STOP;
import static com.urrecliner.mytracklogs.Vars.mainActivity;
import static com.urrecliner.mytracklogs.Vars.utils;

public class NotificationService extends Service {

    private Context mContext;
    NotificationCompat.Builder mBuilder = null;
    NotificationChannel mNotificationChannel = null;
    NotificationManager mNotificationManager;
    private RemoteViews mRemoteViews;
    private final String logID = "Notify";
    private boolean yesNOShown = false, isStarted;
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

        utils.log(logID," start command");
        int operation;
        try {
            operation = intent.getIntExtra("operation", NOTIFICATION_BAR_NO_ACTION);
        } catch (Exception e) {
            utils.logE(logID, "operation EXCEPTION", e);
            return START_STICKY;
        }
        utils.log(logID, "operation : " + operation);
        switch (operation) {
            case NOTIFICATION_BAR_NO_ACTION:
                break;
            case NOTIFICATION_BAR_GO_STOP:
                if (isStarted)
                    MainActivity.notificationBarTouched(NOTIFICATION_BAR_SHOW_CONFIRM);
                else
                    MainActivity.notificationBarTouched(NOTIFICATION_BAR_GO);
                break;
            case NOTIFICATION_BAR_EXIT_APP:
            case NOTIFICATION_BAR_PAUSE_RESTART:
                MainActivity.notificationBarTouched(operation);
                break;
            case NOTIFICATION_BAR_SHOW_MAIN:
                showInForeground();
                break;
            case NOTIFICATION_BAR_YES_STOP:
                MainActivity.notificationBarTouched(NOTIFICATION_BAR_HIDE_CONFIRM);
                MainActivity.notificationBarTouched(NOTIFICATION_BAR_CONFIRMED_STOP);
                break;
            case NOTIFICATION_BAR_NO_CONTINUE:
                MainActivity.notificationBarTouched(NOTIFICATION_BAR_HIDE_CONFIRM);
                yesNOShown = false;
                break;
            default:
                utils.log(logID, "Invalid operation "+operation);
        }

        String action = intent.getStringExtra("action");
        if (action == null) {
//            utils.log(logID, "action is NULL, nothing to do");
            return START_NOT_STICKY;
        }
        else
            utils.log(logID, "action "+action);

        createNotification();
        switch (action) {
            case ACTION_UPDATE:
                mRemoteViews.setTextViewText(R.id.nLaps,intent.getStringExtra("laps"));
                break;
            case ACTION_INIT:
                mBuilder.setSmallIcon(R.mipmap.my_track_log_small);
                mRemoteViews.setTextViewText(R.id.nDateTime, getString(R.string.press_start));
                mRemoteViews.setTextViewText(R.id.nLaps,"");
                mRemoteViews.setImageViewResource(R.id.nGoStop, R.mipmap.button_start);
                mRemoteViews.setViewVisibility(R.id.nPause, View.GONE);
                mRemoteViews.setViewVisibility(R.id.nExit, View.VISIBLE);
                mRemoteViews.setViewVisibility(R.id.nConfirm, View.GONE);
                isStarted = false;
                break;
            case ACTION_START:
            case ACTION_RESTART:
                mBuilder.setSmallIcon(R.mipmap.button_start);
                mRemoteViews.setImageViewResource(R.id.nGoStop, R.mipmap.button_stop);
                mRemoteViews.setImageViewResource(R.id.nPause, R.mipmap.button_pause);
                mRemoteViews.setViewVisibility(R.id.nPause, View.VISIBLE);
                mRemoteViews.setViewVisibility(R.id.nExit, View.GONE);
                if (action.equals(ACTION_START)) {
                    mRemoteViews.setTextViewText(R.id.nDateTime,
                            utils.long2DateDay(System.currentTimeMillis()) + "\n" + utils.long2Time(System.currentTimeMillis()));
                    mRemoteViews.setTextViewText(R.id.nLaps, "");
                }
                isStarted = true;
                break;
            case ACTION_STOP:
                mBuilder.setSmallIcon(R.mipmap.button_stop);
                mRemoteViews.setImageViewResource(R.id.nGoStop, R.mipmap.button_start);
                mRemoteViews.setViewVisibility(R.id.nPause, View.GONE);
                mRemoteViews.setViewVisibility(R.id.nExit, View.VISIBLE);
                isStarted = false;
                break;
            case ACTION_PAUSE:
                mBuilder.setSmallIcon(R.mipmap.button_pause);
                mRemoteViews.setImageViewResource(R.id.nGoStop, R.mipmap.button_stop);
                mRemoteViews.setImageViewResource(R.id.nPause, R.mipmap.button_restart);
                mRemoteViews.setViewVisibility(R.id.nPause, View.VISIBLE);
                mRemoteViews.setViewVisibility(R.id.nExit, View.GONE);
                break;
            case ACTION_SHOW_CONFIRM:
                mRemoteViews.setViewVisibility(R.id.nConfirm, View.VISIBLE);
                yesNOShown = true;
                break;
            case ACTION_HIDE_CONFIRM:
                mRemoteViews.setViewVisibility(R.id.nConfirm, View.GONE);
                yesNOShown = false;
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
        List<ActivityManager.RunningTaskInfo> tasks =am.getRunningTasks(5); //얻어올 task갯수 원하는 만큼의 수를 입력하면 된다.
        if(!tasks.isEmpty()) {
            int tasksSize = tasks.size();
//            utils.log(logID, "tasksSize "+tasksSize);
            for(int i = 0; i < tasksSize;  i++) {
                ActivityManager.RunningTaskInfo taskInfo = tasks.get(i);
//                utils.log(logID, taskInfo.topActivity.getPackageName()+" vs "+ mContext.getPackageName());
                if(taskInfo.topActivity.getPackageName().equals(mContext.getPackageName())) {
//                    utils.log(logID, taskInfo.topActivity.getPackageName()+" EQUALS "+ mContext.getPackageName());
                    am.moveTaskToFront(taskInfo.id, 0);
                }
            }
        }
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
        Intent intent = new Intent(this, NotificationService.class);

        intent.putExtra("operation", NOTIFICATION_BAR_YES_STOP);
        PendingIntent pi = PendingIntent.getService(mContext, NOTIFICATION_BAR_YES_STOP, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pi);
        mRemoteViews.setOnClickPendingIntent(R.id.nYes, pi);

        intent.putExtra("operation", NOTIFICATION_BAR_NO_CONTINUE);
        pi = PendingIntent.getService(mContext, NOTIFICATION_BAR_NO_CONTINUE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pi);
        mRemoteViews.setOnClickPendingIntent(R.id.nNo, pi);

        intent.putExtra("operation", NOTIFICATION_BAR_SHOW_MAIN);
        pi = PendingIntent.getService(mContext, NOTIFICATION_BAR_SHOW_MAIN, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pi);
        mRemoteViews.setOnClickPendingIntent(R.id.ll_customNotification, pi);

        intent.putExtra("operation", NOTIFICATION_BAR_GO_STOP);
        pi = PendingIntent.getService(mContext, NOTIFICATION_BAR_GO_STOP, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pi);
        mRemoteViews.setOnClickPendingIntent(R.id.nGoStop, pi);

        intent.putExtra("operation", NOTIFICATION_BAR_PAUSE_RESTART);
        pi = PendingIntent.getService(mContext, NOTIFICATION_BAR_PAUSE_RESTART, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pi);
        mRemoteViews.setOnClickPendingIntent(R.id.nPause, pi);

        intent.putExtra("operation", NOTIFICATION_BAR_EXIT_APP);
        pi = PendingIntent.getService(mContext, NOTIFICATION_BAR_EXIT_APP, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pi);
        mRemoteViews.setOnClickPendingIntent(R.id.nExit, pi);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
