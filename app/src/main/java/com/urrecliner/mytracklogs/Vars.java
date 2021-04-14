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
    static void generateColor() {
        AnimatedColor animatedColor = new AnimatedColor(
                mContext.getColor(R.color.colorLow), mContext.getColor(R.color.colorHigh));
        StringBuilder s = new StringBuilder();
        s.append("\n");
        for (int i = 0; i < 100; i++) {
            float ratio = (float) i / 100;
            if (i%6 == 0)
                s.append("\n\t\t\t");
            s.append("0x").append(String.format("%06X", animatedColor.with(ratio))).append(", ");
        }
        utils.log("color",s.toString());
    }

    static int [] speedColor = {

            0xFF00FF00, 0xFF05FF00, 0xFF0AFF00, 0xFF0FFF00, 0xFF14FF00, 0xFF1AFF00,
            0xFF1FFF00, 0xFF24FF00, 0xFF29FF00, 0xFF2EFF00, 0xFF33FF00, 0xFF38FF00,
            0xFF3DFF00, 0xFF42FF00, 0xFF47FF00, 0xFF4CFF00, 0xFF52FF00, 0xFF57FF00,
            0xFF5CFF00, 0xFF61FF00, 0xFF66FF00, 0xFF6BFF00, 0xFF70FF00, 0xFF75FF00,
            0xFF7AFF00, 0xFF80FF00, 0xFF85FF00, 0xFF8AFF00, 0xFF8FFF00, 0xFF94FF00,
            0xFF99FF00, 0xFF9EFF00, 0xFFA3FF00, 0xFFA8FF00, 0xFFADFF00, 0xFFB3FF00,
            0xFFB8FF00, 0xFFBDFF00, 0xFFC2FF00, 0xFFC7FF00, 0xFFCCFF00, 0xFFD1FF00,
            0xFFD6FF00, 0xFFDBFF00, 0xFFE0FF00, 0xFFE6FF00, 0xFFEBFF00, 0xFFF0FF00,
            0xFFF5FF00, 0xFFFAFF00, 0xFFFFFF00, 0xFFFFFA00, 0xFFFFF500, 0xFFFFF000,
            0xFFFFEB00, 0xFFFFE600, 0xFFFFE000, 0xFFFFDB00, 0xFFFFD600, 0xFFFFD100,
            0xFFFFCC00, 0xFFFFC700, 0xFFFFC200, 0xFFFFBD00, 0xFFFFB800, 0xFFFFB300,
            0xFFFFAD00, 0xFFFFA800, 0xFFFFA300, 0xFFFF9E00, 0xFFFF9900, 0xFFFF9400,
            0xFFFF8F00, 0xFFFF8A00, 0xFFFF8500, 0xFFFF8000, 0xFFFF7A00, 0xFFFF7500,
            0xFFFF7000, 0xFFFF6B00, 0xFFFF6600, 0xFFFF6100, 0xFFFF5C00, 0xFFFF5700,
            0xFFFF5200, 0xFFFF4D00, 0xFFFF4700, 0xFFFF4200, 0xFFFF3D00, 0xFFFF3800,
            0xFFFF3300, 0xFFFF2E00, 0xFFFF2900, 0xFFFF2400, 0xFFFF1F00, 0xFFFF1A00,
            0xFFFF1400, 0xFFFF0F00, 0xFFFF0A00, 0xFFFF0500,
            0xFF980000};
}

