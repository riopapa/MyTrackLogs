package com.urrecliner.mytracklogs;

import android.database.Cursor;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import static com.urrecliner.mytracklogs.Vars.logLocations;
import static com.urrecliner.mytracklogs.Vars.databaseIO;
import static com.urrecliner.mytracklogs.Vars.trackActivity;
import static com.urrecliner.mytracklogs.Vars.trackAdapter;
import static com.urrecliner.mytracklogs.Vars.utils;

public class TrackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.track_recycler);
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

        trackActivity = this;
        logLocations = new ArrayList<>();

        Cursor cursor = databaseIO.trackFromTo();
        if (cursor != null) {
            utils.log("cursor","count ="+cursor.getCount());
            // move cursor to first row
            if (cursor.moveToFirst()) {
                do {
                    long startTime = cursor.getLong(cursor.getColumnIndex("startTime"));
                    long finishTime = cursor.getLong(cursor.getColumnIndex("finishTime"));
                    int meters = cursor.getInt(cursor.getColumnIndex("meters"));
                    int minutes = cursor.getInt(cursor.getColumnIndex("minutes"));
                    logLocations.add(new LogLocation(startTime, finishTime, meters, minutes));
                } while (cursor.moveToNext());
            }
        }
        trackAdapter = new TrackAdapter();
        mRecyclerView.setAdapter(trackAdapter);
        ActionBar ab = this.getSupportActionBar();
        ab.setTitle(" 이동 리스트");
        ab.setIcon(R.mipmap.my_face) ;
        ab.setDisplayUseLogoEnabled(true) ;
        ab.setDisplayShowHomeEnabled(true) ;
    }

}
