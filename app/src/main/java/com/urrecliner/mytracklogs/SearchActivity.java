package com.urrecliner.mytracklogs;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static com.urrecliner.mytracklogs.Vars.dummyMap;
import static com.urrecliner.mytracklogs.Vars.mainActivity;
import static com.urrecliner.mytracklogs.Vars.searchActivity;
import static com.urrecliner.mytracklogs.Vars.trackActivity;
import static com.urrecliner.mytracklogs.Vars.trackLogs;
import static com.urrecliner.mytracklogs.Vars.utils;

public class SearchActivity extends AppCompatActivity {

    DatePicker startDP, finishDP;
    TimePicker startTP, finishTP;
    long startTime, finishTime;
    int yearS, monthS, dayS, hourS, minuteS;
    int yearF, monthF, dayF, hourF, minuteF;
    Calendar calendar;
    TextView tvShowMeMap;

    private SimpleDateFormat sdfFullTime = new SimpleDateFormat("yy-MM-dd(EEE) HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        searchActivity = this;
        tvShowMeMap = findViewById(R.id.showMeMap);
        calendar = Calendar.getInstance();
        finishDP =  this.findViewById(R.id.datePickerFinish);
        startDP =  this.findViewById(R.id.datePickerStart);
        finishTP = this.findViewById(R.id.timePickerFinish);
        startTP = this.findViewById(R.id.timePickerStart);

        finishTP.setIs24HourView(true);
        startTP.setIs24HourView(true);

        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 55); calendar.set(Calendar.SECOND, 0);
        yearF = calendar.get(Calendar.YEAR); monthF = calendar.get(Calendar.MONTH); dayF = calendar.get(Calendar.DATE);
        hourF = calendar.get(Calendar.HOUR_OF_DAY); minuteF = calendar.get(Calendar.MINUTE);
        finishTime = calendar.getTimeInMillis();

        finishDP.updateDate(yearF, monthF, dayF);
        finishDP.setOnDateChangedListener(new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker datePicker, int year, int month, int dayOfMonth) {
                yearF = year; monthF = month; dayF = dayOfMonth;
                calendar.set(yearF, monthF, dayF, hourF, minuteF,0);
                finishTime = calendar.getTimeInMillis();
                if (startTime > finishTime) {
                    startTime = finishTime - 24*60*60000;
                }
                showDateTimeText();
            }
        });
        finishDP.setMaxDate(finishTime);
        finishTP.setHour(hourF); finishTP.setMinute(minuteF);
        finishTP.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker timePicker, int hour, int min) {
                hourF = hour; minuteF = min;
                calendar.set(yearF, monthF, dayF, hourF, minuteF,0);
                finishTime = calendar.getTimeInMillis();
                if (startTime > finishTime) {
                    startTime = finishTime - 24*60*60000;
                }
                showDateTimeText();
            }
        });

        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE, 0);
        yearS = calendar.get(Calendar.YEAR); monthS = calendar.get(Calendar.MONTH); dayS = calendar.get(Calendar.DATE);
        hourS = 0; minuteS = 0;
        startTime = calendar.getTimeInMillis();

        calendar.setTimeInMillis(startTime);
        startDP.updateDate(yearS, monthS, dayS);
        startDP.setOnDateChangedListener(new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker datePicker, int year, int month, int dayOfMonth) {
                yearS = year; monthS = month; dayS = dayOfMonth;
                calendar.set(yearS, monthS, dayS, hourS, minuteS,0);
                startTime = calendar.getTimeInMillis();
                if (startTime > finishTime) {
                    finishTime = startTime + 24*60*60000;
                }
                showDateTimeText();
            }
        });
        startDP.setMaxDate(finishTime);
        startTP.setHour(hourS); startTP.setMinute(minuteS);
        startTP.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker timePicker, int hour, int min) {
                hourS = hour; minuteS = min;
                calendar.set(yearS, monthS, dayS, hourS, minuteS,0);
                startTime = calendar.getTimeInMillis();
//                utils.log("start time", sdfFullTime.format(startTime));
                if (startTime > finishTime) {
                    utils.log("startTP","startTime "+startTime+" > finishTime "+finishTime);
                    finishTime = startTime + 24*60*60000;
                }
                showDateTimeText();
            }
        });

        FloatingActionButton fabSearch = findViewById(R.id.fabGoStop);

        fabSearch.setOnClickListener(new View.OnClickListener() {
            Intent intent;
            @Override
            public void onClick(View view) {
                TrackLog trackLog = new TrackLog(startTime, finishTime, -1, -1, dummyMap);
                Intent intent = new Intent(searchActivity, MapActivity.class);
                intent.putExtra("trackLog", trackLog);
                intent.putExtra("position", -1);
                startActivity(intent);
            }
        });

        showDateTimeText();
        this.getSupportActionBar().setTitle(getString(R.string.app_name)+" 시간으로 검색");

    }

    void showDateTimeText() {
        String s = sdfFullTime.format(startTime)+"~\n"+sdfFullTime.format(finishTime)+" ";
        tvShowMeMap.setText(s);
        tvShowMeMap.invalidate();
    }

}
