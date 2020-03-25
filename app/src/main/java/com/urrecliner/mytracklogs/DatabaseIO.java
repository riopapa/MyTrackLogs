package com.urrecliner.mytracklogs;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import static com.urrecliner.mytracklogs.Vars.mContext;
import static com.urrecliner.mytracklogs.Vars.utils;

public class DatabaseIO extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "My Tracks.db";
    private static final String TABLE_LOG = "locationLog";
    private static final String TABLE_TRACK = "track";
    private static final int SCHEMA_VERSION = 1;
    private static String logID = "dbIO";
    private static SQLiteDatabase dbIO = null;
    static final String sqlLog = "CREATE TABLE if not exists " + TABLE_LOG + " " +
            "(logTime INTEGER PRIMARY KEY AUTOINCREMENT, " +    // 0
            " latitude REAL, " + // 1
            " longitude REAL ) ;"; // 2
    static final String sqlTrack = "CREATE TABLE if not exists " + TABLE_TRACK + " " +
            "(startTime INTEGER PRIMARY KEY AUTOINCREMENT, " +    // 0
            " finishTime INTEGER, " + // 1
            " meters INTEGER, " + // 2
            " minutes INTEGER ) ;"; // 3

    DatabaseIO() {
        super(mContext, utils.getPackageDirectory().toString()+ "/" + DATABASE_NAME, null, SCHEMA_VERSION);
//        utils.log("Start","DatabaseIO");
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
        ContentValues cv = new ContentValues();
        cv.put("startTime", startTime);
        cv.put("finishTime", 0);
        cv.put("meters", 0);
        cv.put("minutes", 0);
        dbIO.insert(TABLE_TRACK, null, cv);
    }

    void trackUpdate(long startTime, long finishTime, int meters, int minutes) {
        if (dbIO == null)
            dbIO = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("startTime", startTime);
        cv.put("finishTime", finishTime);
        cv.put("meters", meters);
        cv.put("minutes", minutes);
        dbIO.update(TABLE_TRACK, cv, "startTime = ?", new String[]{String.valueOf(startTime)});
    }

    void trackDelete(long startTime) {
        if (dbIO == null)
            dbIO = this.getWritableDatabase();
        dbIO.delete(TABLE_TRACK, "startTime = ?", new String[]{String.valueOf(startTime)});
        utils.log("Delete",""+startTime);
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
        dbIO.insert(TABLE_LOG, null, cv);
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
            utils.log(logID, " retrievePhoto exception "+e.toString());
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

    public void close() {
        if (dbIO != null)
            dbIO.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    @Override
    protected void finalize() throws Throwable { this.close(); super.finalize(); }

}

