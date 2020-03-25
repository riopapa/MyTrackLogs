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

    static SharedPreferences sharePrefer;
    static ArrayList<LogLocation> logLocations;
    static TrackAdapter trackAdapter;

    static long logInterval;

    static DecimalFormat decimalComma = new DecimalFormat("##,###,###");
    static SimpleDateFormat sdfDateDay = new SimpleDateFormat("MM-dd(EEE)", Locale.getDefault());
    static SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
    static SimpleDateFormat sdfDateTimeLog = new SimpleDateFormat("MM-dd HH.mm.ss sss", Locale.US);
    static SimpleDateFormat sdfDate = new SimpleDateFormat("yy-MM-dd", Locale.US);

}

