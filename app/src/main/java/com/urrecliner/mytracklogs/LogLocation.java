package com.urrecliner.mytracklogs;

class LogLocation {
    private long startTime, finishTime;
    private int meters, minutes;

    LogLocation(long startTime, long finishTime, int meters, int minutes ) {
        this.startTime = startTime;
        this.finishTime = finishTime;
        this.meters = meters;
        this.minutes = minutes;
    }

    long getStartTime() { return startTime; }

    long getFinishTime() { return finishTime; }

    int getMeters() { return meters; }

    int getMinutes() { return minutes; }

}
