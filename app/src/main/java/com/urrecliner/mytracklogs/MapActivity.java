package com.urrecliner.mytracklogs;

import android.app.Activity;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.urrecliner.mytracklogs.Vars.logLocations;
import static com.urrecliner.mytracklogs.Vars.databaseIO;
import static com.urrecliner.mytracklogs.Vars.trackAdapter;
import static com.urrecliner.mytracklogs.Vars.mContext;
import static com.urrecliner.mytracklogs.Vars.utils;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

//    public class MapActivity extends AppCompatActivity
//            implements
//            OnMapReadyCallback,
//            GoogleMap.OnPolylineClickListener,
//            GoogleMap.OnPolygonClickListener {

    private static final int POLYLINE_STROKE_WIDTH_PX = 12;
    private static final int PATTERN_DASH_LENGTH_PX = 10;
    private static final int PATTERN_GAP_LENGTH_PX = 10;
    private static final PatternItem DOT = new Dot();
    private static final PatternItem DASH = new Dash(PATTERN_DASH_LENGTH_PX);
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);

    private static final List<PatternItem> PATTERN_POLYLINE_MINE = Arrays.asList(DOT, DASH, DOT, DASH);

    private Activity mapActivity;
    private long startTime, finishTime;
    private int iMinutes, iMeters, position;
    ArrayList<LatLng> listLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mapActivity = this;
        startTime = getIntent().getLongExtra("startTime",0);
        finishTime = getIntent().getLongExtra("finishTime",0);
        iMinutes = getIntent().getIntExtra("minutes",0);
        iMeters = getIntent().getIntExtra("meters",0);
        position = getIntent().getIntExtra("position",-1);
        if (startTime == 0 || finishTime == 0) {
            Toast.makeText(mContext,"Zero Value "+startTime+" x "+finishTime, Toast.LENGTH_LONG).show();
            return;
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragMap);
        mapFragment.getMapAsync(this);

        ActionBar ab = this.getSupportActionBar();
        ab.setTitle(" 지도");
        ab.setIcon(R.mipmap.my_face) ;
        ab.setDisplayUseLogoEnabled(true) ;
        ab.setDisplayShowHomeEnabled(true) ;

    }

    GoogleMap nowMap;
    @Override
    public void onMapReady(GoogleMap googleMap) {

        nowMap = googleMap;
        String s;
        TextView tvTimeInfo = findViewById(R.id.timeInfo);
        s = utils.long2DateDay(startTime)+" "+utils.long2Time(startTime)+"~"+utils.long2DateDay(finishTime)+" "+utils.long2Time(finishTime)+"  ";
        tvTimeInfo.setText(s);
        if (iMeters > 0) {
            DecimalFormat decimalFormat = new DecimalFormat("##,###,###");
            s = utils.minute2Text(iMinutes) + "  " + decimalFormat.format(iMeters) + "m";
            TextView tvLog = findViewById(R.id.logInfo);
            tvLog.setText(s);
        }
        double locationS = 999, locationN = -999, locationW = 999, locationE = -999;
        listLatLng = new ArrayList<>();
        Cursor cursor = databaseIO.logGetFromTo(startTime, finishTime);
        if (cursor != null) {
            if (cursor.getCount() < 20) {
                Toast.makeText(mapActivity,"자료가 너무 작음("+cursor.getCount()+"), 삭제 요망",Toast.LENGTH_LONG).show();
                return;
            }
            utils.log("cursor","count ="+cursor.getCount());
            // move cursor to first row
            if (cursor.moveToFirst()) {
                do {
                    double latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
                    double longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
                    if (latitude>locationN) locationN = latitude;
                    if (latitude<locationS) locationS = latitude;
                    if (longitude>locationE) locationE = longitude;
                    if (longitude<locationW) locationW = longitude;
                    listLatLng.add(new LatLng(latitude, longitude));
                } while (cursor.moveToNext());
            }
        }
        else {
            Toast.makeText(mContext,"No log data to display ",Toast.LENGTH_LONG).show();
            utils.log("no data",utils.long2DateDay(startTime)+" "+utils.long2Time(startTime)+" ~ "+utils.long2DateDay(finishTime)+" "+utils.long2Time(finishTime));
            return;
        }
        utils.log("cursor","W"+locationW+" E"+locationE+" S"+locationS+" N"+locationN);
        Location locationPrev = new Location("");
        Location locationNow = new Location("");
        locationPrev.setLatitude(locationW);
        locationPrev.setLongitude(locationN);
        locationNow.setLatitude(locationE);
        locationNow.setLongitude(locationS);
        float distance = locationPrev.distanceTo(locationNow) * 10;
        float [] scaleMap = { 1128, 2256, 4514, 9028, 18056, 36112, 72224, 144448, 288895, 577790, 1155581, 2311162,
                4622324, 9244649, 18489298, 36978597, 73957194, 147914388, 295828775, 591657550};
        int mapScale;
        for (mapScale=0; mapScale<scaleMap.length; mapScale++) {
            if (distance<scaleMap[mapScale])
                break;
        }

        mapScale = 19 - mapScale;
        PolylineOptions polyOptions = new PolylineOptions();
        polyOptions.color(getColor(R.color.trackRoute));
        polyOptions.width(POLYLINE_STROKE_WIDTH_PX);
        polyOptions.pattern(PATTERN_POLYLINE_MINE);
        polyOptions.addAll(listLatLng);

        googleMap.addPolyline(polyOptions);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng((locationN+locationS)/2, (locationE+locationW)/2), mapScale));

        MarkerOptions makerOptions = new MarkerOptions();
        makerOptions
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker_start))
                .position(new LatLng(listLatLng.get(0).latitude, listLatLng.get(0).longitude));
        googleMap.addMarker(makerOptions);
        makerOptions
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker_finish))
                .position(new LatLng(listLatLng.get(listLatLng.size()-1).latitude, listLatLng.get(listLatLng.size()-1).longitude));
        googleMap.addMarker(makerOptions);

        // Set listeners for click events.
//        googleMap.setOnPolylineClickListener(this);
//        googleMap.setOnPolygonClickListener(this);

        googleMap.getUiSettings().setCompassEnabled(true);
//        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.getUiSettings().setTiltGesturesEnabled(false);

    }

    Menu mapMenu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mapMenu = menu;
        getMenuInflater().inflate(R.menu.map_menu, menu);
        MenuItem item = menu.findItem(R.id.deleteLog);
        item.setVisible(!(position == -1));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.deleteLog) {
            LogLocation logLocation = logLocations.get(position);
            long fromTime = logLocation.getStartTime();
            long toTime = logLocation.getFinishTime();
            logLocations.remove(position);
            trackAdapter.notifyItemRemoved(position);
            databaseIO.logDeleteFromTo(fromTime, toTime);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}
