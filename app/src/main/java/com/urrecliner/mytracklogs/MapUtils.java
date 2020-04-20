package com.urrecliner.mytracklogs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

import static com.urrecliner.mytracklogs.Vars.nowLatitude;
import static com.urrecliner.mytracklogs.Vars.nowLongitude;
import static com.urrecliner.mytracklogs.Vars.prevLatitude;
import static com.urrecliner.mytracklogs.Vars.prevLongitude;
import static com.urrecliner.mytracklogs.Vars.utils;

class MapUtils {

    private Location locationPrev = new Location("");
    private Location locationNow = new Location("");
    static double locSouth, locNorth, locWest, locEast;

    double getFullMapDistance() {
//        utils.log("left top", locWest+", "+locNorth);
//        utils.log("right btm", locEast+", "+locSouth);
        locationPrev.setLatitude(locWest);
        locationPrev.setLongitude(locNorth);
        locationNow.setLatitude(locEast);
        locationNow.setLongitude(locSouth);
        return locationPrev.distanceTo(locationNow);
    }

    double getShortDistance() {
        locationPrev.setLatitude(prevLatitude);
        locationPrev.setLongitude(prevLongitude);
        locationNow.setLatitude(nowLatitude);
        locationNow.setLongitude(nowLongitude);
        return locationPrev.distanceTo(locationNow);
    }

    int getMapScale(double fullMapDistance) {
        double [] scaleMap = { 1128, 2256, 4514, 9028, 18056, 36112, 72224, 144448, 288895, 577790, 1155581, 2311162,
                4622324, 9244649, 18489298, 36978597, 73957194, 147914388, 295828775, 591657550};
        int mapScale;
        for (mapScale=0; mapScale<scaleMap.length; mapScale++) {
            if (fullMapDistance * 10 < scaleMap[mapScale])
                break;
        }
        return (mapScale < 2) ? 18 : (19-mapScale);
    }

    Bitmap StringToBitMap(String encodedString){
        byte [] encodeByte= Base64.decode(encodedString,Base64.DEFAULT);
        Bitmap bitmap= BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
        return bitmap;
    }

    String BitMapToString(Bitmap bitmap){
        ByteArrayOutputStream baos= new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b=baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

}
