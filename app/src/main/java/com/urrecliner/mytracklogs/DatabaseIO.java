package com.urrecliner.mytracklogs;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.widget.Toast;

import static com.urrecliner.mytracklogs.Vars.dummyMap;
import static com.urrecliner.mytracklogs.Vars.mContext;
import static com.urrecliner.mytracklogs.Vars.mapUtils;
import static com.urrecliner.mytracklogs.Vars.utils;

class DatabaseIO extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "MyTracks.db";
    private static final String TABLE_LOG = "locationLog";
    private static final String TABLE_TRACK = "track";
    private static final int SCHEMA_VERSION = 1;
    private static final String logID = "dbIO";
    private static SQLiteDatabase dbIO = null;
    static final String sqlLog = "CREATE TABLE if not exists " + TABLE_LOG + " " +
            "(logTime INTEGER PRIMARY KEY AUTOINCREMENT, " +    // 0
            " latitude REAL, " + // 1
            " longitude REAL ) ;"; // 2
    static final String sqlTrack = "CREATE TABLE if not exists " + TABLE_TRACK + " " +
            "(startTime INTEGER PRIMARY KEY AUTOINCREMENT, " +    // 0
            " finishTime INTEGER, " + // 1
            " walkDrive INTEGER, " + // 2
            " meters INTEGER, " + // 3
            " minutes INTEGER, " + // 4
            " bitMap LONGTEXT, " + // 5
            " placeName TEXT );"; // 6

    DatabaseIO() {
        super(mContext, utils.getPackageDirectory().toString()+ "/" + DATABASE_NAME, null, SCHEMA_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        create_Table(db);
    }

    private void create_Table(SQLiteDatabase db) {
        db.execSQL(sqlLog);
        db.execSQL(sqlTrack);
    }

    void trackInsert(long startTime) {
        if (dbIO == null)
            dbIO = this.getWritableDatabase();
        String mapString = mapUtils.BitMapToString(dummyMap);
        ContentValues cv = new ContentValues();
        cv.put("startTime", startTime);
        cv.put("finishTime", startTime);
        cv.put("walkDrive", 0); // 0 : walk, 1: drive
        cv.put("meters", 0);
        cv.put("minutes", 0);
        cv.put("bitMap", mapString);
        dbIO.insert(TABLE_TRACK, null, cv);
    }

    void trackUpdate(long startTime, long finishTime, int walkDrive, int meters, int minutes) {
        if (dbIO == null)
            dbIO = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("startTime", startTime);
        cv.put("finishTime", finishTime);
        cv.put("walkDrive", walkDrive);
        cv.put("meters", meters);
        cv.put("minutes", minutes);
        try {
            dbIO.update(TABLE_TRACK, cv, "startTime = ?", new String[]{String.valueOf(startTime)});
        } catch (Exception e) {
            utils.logE(logID, "trackUpdate", e);
        }
    }

    void trackMapPlaceUpdate(long startTime, long totDistance, Bitmap bitmap, String placeName ) {
        if (dbIO == null)
            dbIO = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("startTime", startTime);
        cv.put("meters", totDistance);
        cv.put("bitMap", mapUtils.BitMapToString(bitmap));
        cv.put("placeName", placeName);
        try {
            dbIO.update(TABLE_TRACK, cv, "startTime = ?", new String[]{String.valueOf(startTime)});
        } catch (Exception e) {
            utils.logE(logID, "trackUpdate", e);
        }
    }

    void trackDelete(long startTime) {
        if (dbIO == null)
            dbIO = this.getWritableDatabase();
        dbIO.delete(TABLE_TRACK, "startTime = ?", new String[]{String.valueOf(startTime)});
//        utils.log("Delete",""+startTime);
    }

    Cursor trackFromTo() {
        if (dbIO == null)
            dbIO = this.getWritableDatabase();
        return dbIO.rawQuery("SELECT * FROM " + TABLE_TRACK + " ORDER BY startTime DESC", new String[] {});
    }

    void logInsert(long logTime, double latitude, double longitude) {
        if (dbIO == null)
            dbIO = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("logTime", logTime);
        cv.put("latitude", latitude);
        cv.put("longitude", longitude);
        try {
            dbIO.insert(TABLE_LOG, null, cv);
        } catch (Exception e) {
            utils.logE(logID, "logInsert error ", e);
        }
    }

    Cursor logGetFromTo(long timeFrom, long timeTo) {

        if (dbIO == null)
            dbIO = this.getWritableDatabase();
        String sqlCommand = "SELECT * FROM " + TABLE_LOG + " WHERE " +
                "logTime>=\""+timeFrom+"\" AND logTime<=\""+timeTo+"\";";
        Cursor result = null;
        try {
            result = dbIO.rawQuery(sqlCommand, null);
        } catch (Exception e) {
            utils.logE(logID, " retrievePhoto exception ", e);
        }
        return result;
    }

    void logDeleteFromTo(long timeFrom, long timeTo) {
        if (dbIO == null)
            dbIO = this.getWritableDatabase();
        int cnt = dbIO.delete(TABLE_LOG,
                "logTime>=\"" + timeFrom + "\" AND logTime<=\"" + timeTo + "\";", null);
        Toast.makeText(mContext, cnt+" logs deleted",Toast.LENGTH_LONG).show();
    }

    @Override
    public void finalize() throws Throwable {
        if (dbIO != null)
            dbIO.close();
        super.finalize();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

}

