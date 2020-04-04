package com.urrecliner.mytracklogs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

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
    static MapUtils mapUtils;
    static ShowMarker showMarker;

    static SharedPreferences sharePrefer;
    static ArrayList<TrackLog> trackLogs;
    static TrackAdapter trackAdapter;
    static boolean modeStarted = false, modePaused = false;

    static double prevLatitude, prevLongitude, nowLatitude, nowLongitude;

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
    final static String ACTION_EXIT = "exit";

}

