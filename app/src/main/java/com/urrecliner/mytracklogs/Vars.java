package com.urrecliner.mytracklogs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

class Vars {
    static Activity mActivity, trackActivity;
    static Context mContext;

    static Utils utils;
    static DatabaseIO databaseIO;
    static GPSTracker gpsTracker;
    static MapUtils mapUtils;
    static ShowMarker showMarker;
    static RecyclerView trackView;
    static SharedPreferences sharePrefer;
    static ArrayList<TrackLog> trackLogs;
    static TrackAdapter trackAdapter;
    static int trackPosition = -1;
    static SearchActivity searchActivity;
    static boolean modeStarted = false, modePaused = false, isWalk = true;

    static double prevLatitude = -999, prevLongitude = -999, nowLatitude = 0, nowLongitude = 0;
    static Bitmap dummyMap;
    static DecimalFormat decimalComma = new DecimalFormat("##,###,###");
    static SimpleDateFormat sdfDateDay = new SimpleDateFormat("MM-dd(EEE)", Locale.getDefault());
    static SimpleDateFormat sdfDateDayTime = new SimpleDateFormat("MM-dd(EEE) HH:mm", Locale.getDefault());
    static SimpleDateFormat sdfTimeOnly = new SimpleDateFormat("HH:mm", Locale.getDefault());
    static SimpleDateFormat sdfDateTimeLog = new SimpleDateFormat("MM-dd HH.mm.ss SSS", Locale.US);
    static SimpleDateFormat sdfDate = new SimpleDateFormat("yy-MM-dd", Locale.US);

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

    static final double LOW_SPEED_WALK = 20f, HIGH_SPEED_WALK = 400f;
    static final double LOW_SPEED_DRIVE = 100f, HIGH_SPEED_DRIVE = 2500f;
    static final double HIGH_DISTANCE_WALK = 200f, HIGH_DISTANCE_DRIVE = 10000f;

    /*
        generate static color table speedColor index value (only if color table change is required)
     */
    static void generateColor() {
        AnimatedColor animatedColor = new AnimatedColor(
                mContext.getColor(R.color.colorLow), mContext.getColor(R.color.colorHigh));
        StringBuilder s = new StringBuilder();
        s.append("\n");
        for (int i = 0; i < 20; i++) {
            float ratio = (float) i * 5 / 100;
            if (i%6 == 0)
                s.append("\n\t\t\t");
            s.append("0x").append(String.format("%06X", animatedColor.with(ratio))).append(", ");
        }
        utils.log("color",s.toString());
    }

    static int [] speedColor = {
            0xFF6C0505, 0xFF731005, 0xFF7A1D05, 0xFF812B06, 0xFF883A06, 0xFF8F4A06,
            0xFF955C06, 0xFF9C6F06, 0xFFA38406, 0xFFAA9A07, 0xFFB1B107, 0xFFA6B807,
            0xFF9ABF07, 0xFF8CC607, 0xFF7ECD07, 0xFF6DD407, 0xFF5CDA07, 0xFF48E107,
            0xFF34E807, 0xFF1EEF07,
            0xFF07F607 };
}

