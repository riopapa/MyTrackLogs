package com.urrecliner.mytracklogs;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import static com.urrecliner.mytracklogs.Vars.mapUtils;
import static com.urrecliner.mytracklogs.Vars.trackLogs;
import static com.urrecliner.mytracklogs.Vars.databaseIO;
import static com.urrecliner.mytracklogs.Vars.trackActivity;
import static com.urrecliner.mytracklogs.Vars.trackAdapter;
import static com.urrecliner.mytracklogs.Vars.utils;

public class TrackActivity extends AppCompatActivity {

    final String logID = "track";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks);

        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.track_recycler);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

        trackActivity = this;
        trackLogs = new ArrayList<>();

        Cursor cursor = databaseIO.trackFromTo();
        if (cursor == null) {
            utils.log(logID, "retry to read db");
            cursor = databaseIO.trackFromTo();
        }
        if (cursor != null) {
            utils.log("cursor","count ="+cursor.getCount());
            // move cursor to first row
            if (cursor.moveToFirst()) {
                do {
                    long startTime = cursor.getLong(cursor.getColumnIndex("startTime"));
                    long finishTime = cursor.getLong(cursor.getColumnIndex("finishTime"));
                    int meters = cursor.getInt(cursor.getColumnIndex("meters"));
                    int minutes = cursor.getInt(cursor.getColumnIndex("minutes"));
                    Bitmap bitmap = mapUtils.StringToBitMap(cursor.getString(cursor.getColumnIndex("bitMap")));
                    trackLogs.add(new TrackLog(startTime, finishTime, meters, minutes, bitmap));
                } while (cursor.moveToNext());
            }
        }
        trackAdapter = new TrackAdapter();
        mRecyclerView.setAdapter(trackAdapter);
        ActionBar ab = this.getSupportActionBar();
        ab.setTitle(R.string.track_list);
        ab.setIcon(R.mipmap.my_face) ;
        ab.setDisplayUseLogoEnabled(true) ;
        ab.setDisplayShowHomeEnabled(true) ;
    }

}
