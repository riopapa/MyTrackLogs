package com.urrecliner.mytracklogs;

import android.graphics.Color;

public class AnimatedColor {
    private final int startColor, endColor;
    private final float[] startHSV, endHSV;
    private float[] move = new float[3];


    AnimatedColor(int start, int end) {
        startColor = start;
        endColor = end;
        startHSV = toHSV(start);
        endHSV = toHSV(end);
    }

    int with(float delta) {
        if (delta <= 0) return startColor;
        if (delta >= 1) return endColor;
        return Color.HSVToColor(move(delta));
    }

    private float[] move(float delta) {
        move[0] = (endHSV[0] - startHSV[0]) * delta + startHSV[0];
        move[1] = (endHSV[1] - startHSV[1]) * delta + startHSV[1];
        move[2] = (endHSV[2] - startHSV[2]) * delta + startHSV[2];
        return move;
    }

    private float[] toHSV(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv;
    }
}