package com.quantimodo.sync;

import android.content.res.Resources;
import android.util.DisplayMetrics;

public class Utils {
    public static int convertDpToPixel(float dp, Resources res) {
        DisplayMetrics metrics = res.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return (int) px;
    }

    public static int convertPixelsToDp(float px, Resources res) {
        DisplayMetrics metrics = res.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return (int) dp;
    }

    public static float convertSpToPixels(float px, Resources res) {
        float scaledDensity = res.getDisplayMetrics().scaledDensity;
        return px * scaledDensity;
    }
}
