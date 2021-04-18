package com.urrecliner.mytracklogs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

class MapUtils {

    float calcDistance(double lat1, double lng1, double lat2, double lng2) {
        Location locationPrev = new Location("");
        Location locationNow = new Location("");
        locationPrev.setLatitude(lat1);
        locationPrev.setLongitude(lng1);
        locationNow.setLatitude(lat2);
        locationNow.setLongitude(lng2);
        return locationPrev.distanceTo(locationNow) * 1.1f;
    }

    float getMapScale(double fullMapDistance) {
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
        return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
    }

    String BitMapToString(Bitmap bitmap){
        ByteArrayOutputStream byteOut= new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, byteOut);
        byte [] b=byteOut.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

}
