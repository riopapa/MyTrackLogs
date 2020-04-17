package com.urrecliner.mytracklogs;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

class TrackLog implements Parcelable {
    private long startTime, finishTime;
    private int meters, minutes;
    private Bitmap bitMap;

    TrackLog(long startTime, long finishTime, int meters, int minutes, Bitmap bitMap) {
        this.startTime = startTime;
        this.finishTime = finishTime;
        this.meters = meters;
        this.minutes = minutes;
        this.bitMap = bitMap;
    }

    long getStartTime() { return startTime; }
    long getFinishTime() { return finishTime; }
    int getMeters() { return meters; }
    int getMinutes() { return minutes; }
    Bitmap getBitMap() {return bitMap; }

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
    }

    public void readFromParcel(Parcel src) {
        this.startTime = src.readLong();
        this.finishTime = src.readLong();
        this.meters = src.readInt();
        this.minutes = src.readInt();
        this.bitMap = src.readParcelable(Bitmap.class.getClassLoader());
    }

    public static final Parcelable.Creator<TrackLog> CREATOR =new Parcelable.Creator<TrackLog>() {
        public TrackLog createFromParcel(Parcel in) {
            return new TrackLog(in);
        }

        public TrackLog[] newArray(int size) {
            return new TrackLog[size];
        }
    };

    public TrackLog(Parcel src) {
        readFromParcel(src);
    }
}
