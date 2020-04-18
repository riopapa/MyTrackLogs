package com.urrecliner.mytracklogs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

class Vars {
    static Activity mainActivity, trackActivity;
    static Context mContext;

    static Utils utils;
    static DatabaseIO databaseIO;
    static GPSTracker gpsTracker;
    static long gpsUpdateTime = 0;
    static MapUtils mapUtils;
    static ShowMarker showMarker;

    static SharedPreferences sharePrefer;
    static ArrayList<TrackLog> trackLogs;
    static TrackAdapter trackAdapter;
    static SearchActivity searchActivity;
    static boolean modeStarted = false, modePaused = false;

    static double prevLatitude, prevLongitude, nowLatitude, nowLongitude;
    static Bitmap dummyMap;
    static DecimalFormat decimalComma = new DecimalFormat("##,###,###");
    static SimpleDateFormat sdfDateDay = new SimpleDateFormat("MM-dd(EEE)", Locale.getDefault());
    static SimpleDateFormat sdfDateDayTime = new SimpleDateFormat("MM-dd(EEE) HH:mm", Locale.getDefault());
    static SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
    static SimpleDateFormat sdfDateTimeLog = new SimpleDateFormat("MM-dd HH.mm.ss sss", Locale.US);
    static SimpleDateFormat sdfDate = new SimpleDateFormat("yy-MM-dd", Locale.US);

    final static String ACTION_NONE = "none";
    final static String ACTION_INIT = "init";
    final static String ACTION_START = "start";
    final static String ACTION_PAUSE = "pause";
    final static String ACTION_STOP = "stop";
    final static String ACTION_RESTART = "restart";
    final static String ACTION_UPDATE = "update";
    final static String ACTION_SHOW_CONFIRM = "confirm";
    final static String ACTION_HIDE_CONFIRM = "hide";
    final static String ACTION_EXIT = "exit";

    static final int NOTIFICATION_BAR_NO_ACTION = -11;
    static final int NOTIFICATION_BAR_GO_STOP = 11;
    static final int NOTIFICATION_BAR_GO = 22;
    static final int NOTIFICATION_BAR_PAUSE_RESTART = 33;
    static final int NOTIFICATION_BAR_EXIT_APP = 44;
    static final int NOTIFICATION_BAR_SHOW_MAIN = 55;
    static final int NOTIFICATION_BAR_CONFIRMED_STOP = 66;
    static final int NOTIFICATION_BAR_YES_STOP = 77;
    static final int NOTIFICATION_BAR_NO_CONTINUE = 88;
    static final int NOTIFICATION_BAR_SHOW_CONFIRM = 90;
    static final int NOTIFICATION_BAR_HIDE_CONFIRM = 91;

}

