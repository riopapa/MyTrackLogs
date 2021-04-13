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

    /*
generate static color table speedColor index value (only if color table change is required)
 */
    void generateColor() {
        AnimatedColor animatedColor = new AnimatedColor(Color.rgb(0,152,0), Color.rgb(152,0,0));
        StringBuilder s = new StringBuilder();
        s.append("\n");
        for (int i = 0; i < 100; i++) {
            float ratio = (float) i / 100;
            if (i%8 == 0)
                s.append("\n\t\t\t");
            s.append("0x").append(String.format("%06X", animatedColor.with(ratio))).append(", ");
        }
        utils.log("color",s.toString());
    }

    static int [] speedColor = {
            0xFF009800, 0xFF039800, 0xFF069800, 0xFF099800, 0xFF0C9800, 0xFF0F9800, 0xFF129800, 0xFF159800, 0xFF189800, 0xFF1B9800,
            0xFF1E9800, 0xFF219800, 0xFF249800, 0xFF289800, 0xFF2B9800, 0xFF2E9800, 0xFF319800, 0xFF349800, 0xFF379800, 0xFF3A9800,
            0xFF3D9800, 0xFF409800, 0xFF439800, 0xFF469800, 0xFF499800, 0xFF4C9800, 0xFF4F9800, 0xFF529800, 0xFF559800, 0xFF589800,
            0xFF5B9800, 0xFF5E9800, 0xFF619800, 0xFF649800, 0xFF679800, 0xFF6A9800, 0xFF6D9800, 0xFF709800, 0xFF749800, 0xFF779800,
            0xFF7A9800, 0xFF7D9800, 0xFF809800, 0xFF839800, 0xFF869800, 0xFF899800, 0xFF8C9800, 0xFF8F9800, 0xFF929800, 0xFF959800,
            0xFF989800, 0xFF989500, 0xFF989200, 0xFF988F00, 0xFF988C00, 0xFF988900, 0xFF988600, 0xFF988300, 0xFF988000, 0xFF987D00,
            0xFF987A00, 0xFF987700, 0xFF987400, 0xFF987000, 0xFF986D00, 0xFF986A00, 0xFF986700, 0xFF986400, 0xFF986100, 0xFF985E00,
            0xFF985B00, 0xFF985800, 0xFF985500, 0xFF985200, 0xFF984F00, 0xFF984C00, 0xFF984900, 0xFF984600, 0xFF984300, 0xFF984000,
            0xFF983D00, 0xFF983A00, 0xFF983700, 0xFF983400, 0xFF983100, 0xFF982E00, 0xFF982B00, 0xFF982800, 0xFF982400, 0xFF982100,
            0xFF981E00, 0xFF981B00, 0xFF981800, 0xFF981500, 0xFF981200, 0xFF980F00, 0xFF980C00, 0xFF980900, 0xFF980600, 0xFF980300,
            0xFF980000};
}

