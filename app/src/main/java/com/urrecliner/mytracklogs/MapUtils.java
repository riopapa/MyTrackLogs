package com.urrecliner.mytracklogs;

import android.location.Location;

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
        utils.log("left top", locWest+", "+locNorth);
        utils.log("right btm", locEast+", "+locSouth);
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


}
