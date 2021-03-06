package com.urrecliner.mytracklogs;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;

import static com.urrecliner.mytracklogs.Vars.mapUtils;
import static com.urrecliner.mytracklogs.Vars.trackLogs;
import static com.urrecliner.mytracklogs.Vars.databaseIO;
import static com.urrecliner.mytracklogs.Vars.trackActivity;
import static com.urrecliner.mytracklogs.Vars.trackAdapter;
import static com.urrecliner.mytracklogs.Vars.trackPosition;
import static com.urrecliner.mytracklogs.Vars.trackView;
import static com.urrecliner.mytracklogs.Vars.utils;

public class TrackActivity extends AppCompatActivity {

    Cursor cursor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks);
        Log.w("track"," start //");
        trackView = findViewById(R.id.track_recycler);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);

        trackView.setLayoutManager(mLinearLayoutManager);
        trackActivity = this;
        trackLogs = new ArrayList<>();
//        databaseIO = new DatabaseIO();

        try {
            cursor = databaseIO.trackFromTo();
        } catch (Exception e) {
            if (cursor == null) {
                Log.e("track"," cursor null //");
                databaseIO = new DatabaseIO();
//                utils.log(logID, "retry to read db");
                cursor = databaseIO.trackFromTo();
            }
        }
        trackAdapter = new TrackAdapter();
        trackView.setAdapter(trackAdapter);

        ActionBar ab = this.getSupportActionBar();
        ab.setTitle(R.string.track_list);
        ab.setIcon(R.mipmap.my_face) ;
        ab.setDisplayUseLogoEnabled(true) ;
        ab.setDisplayShowHomeEnabled(true) ;

        if (cursor != null) {
            utils.log("cursor","count ="+cursor.getCount());
            // move cursor to first row
            if (cursor.moveToFirst()) {

                do {
                    long startTime = cursor.getLong(cursor.getColumnIndex("startTime"));
                    long finishTime = cursor.getLong(cursor.getColumnIndex("finishTime"));
                    int walkDrive = cursor.getInt(cursor.getColumnIndex("walkDrive"));
                    int meters = cursor.getInt(cursor.getColumnIndex("meters"));
                    int minutes = cursor.getInt(cursor.getColumnIndex("minutes"));
                    Bitmap bitmap = mapUtils.StringToBitMap(cursor.getString(cursor.getColumnIndex("bitMap")));
                    String placeName = cursor.getString(cursor.getColumnIndex("placeName"));
                    trackLogs.add(new TrackLog(startTime, finishTime, walkDrive, meters, minutes, bitmap, placeName));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        if (trackPosition != -1) {
            trackView.scrollToPosition(trackPosition);
            trackPosition = -1;
        }
    }
}
