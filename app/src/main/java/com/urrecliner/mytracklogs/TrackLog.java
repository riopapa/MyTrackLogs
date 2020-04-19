package com.urrecliner.mytracklogs;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

class TrackLog implements Parcelable {
    private long startTime, finishTime;
    private int meters, minutes;
    private Bitmap bitMap;
    private String placeName;

    TrackLog(long startTime, long finishTime, int meters, int minutes, Bitmap bitMap, String placeName) {
        this.startTime = startTime;
        this.finishTime = finishTime;
        this.meters = meters;
        this.minutes = minutes;
        this.bitMap = bitMap;
        this.placeName = placeName;
    }

    long getStartTime() { return startTime; }
    long getFinishTime() { return finishTime; }
    int getMeters() { return meters; }
    int getMinutes() { return minutes; }
    Bitmap getBitMap() {return bitMap; }
    void setBitMap (Bitmap bitMap) {this.bitMap = bitMap;}
    String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(startTime);
        dest.writeLong(finishTime);
        dest.writeInt(meters);
        dest.writeInt(minutes);
        dest.writeParcelable(bitMap, flags);
        dest.writeString(placeName);
    }

    private void readFromParcel(Parcel src) {
        this.startTime = src.readLong();
        this.finishTime = src.readLong();
        this.meters = src.readInt();
        this.minutes = src.readInt();
        this.bitMap = src.readParcelable(Bitmap.class.getClassLoader());
        this.placeName = src.readString();
    }

    public static final Parcelable.Creator<TrackLog> CREATOR =new Parcelable.Creator<TrackLog>() {
        public TrackLog createFromParcel(Parcel in) {
            return new TrackLog(in);
        }
        public TrackLog[] newArray(int size) {
            return new TrackLog[size];
        }
    };

    private TrackLog(Parcel src) {
        readFromParcel(src);
    }
}
