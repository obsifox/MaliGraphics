package dev.mali.graphics.android;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import java.util.Locale;

/**
 * Device detection (mission §22). Runtime capabilities are NOT inferred from
 * model names — GL strings come from the created context (GlesCapabilities),
 * everything else from the framework. Pure data collector.
 */
public final class AndroidDeviceInfo {

    public final String manufacturer;
    public final String model;
    public final String androidVersion;
    public final int sdkInt;
    public final String kernelVersion;
    public final String abi;
    public final long ramBytes;
    public final int screenWidth, screenHeight;
    public final float refreshHz;

    private AndroidDeviceInfo(String manufacturer, String model, String androidVersion, int sdkInt,
                              String kernelVersion, String abi, long ramBytes,
                              int screenWidth, int screenHeight, float refreshHz) {
        this.manufacturer = manufacturer;
        this.model = model;
        this.androidVersion = androidVersion;
        this.sdkInt = sdkInt;
        this.kernelVersion = kernelVersion;
        this.abi = abi;
        this.ramBytes = ramBytes;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.refreshHz = refreshHz;
    }

    public static AndroidDeviceInfo probe(Context ctx) {
        String kernel;
        try {
            kernel = System.getProperty("os.version");
        } catch (Throwable t) {
            kernel = "unknown";
        }
        long ram = 0;
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            if (am != null) {
                am.getMemoryInfo(mi);
                ram = mi.totalMem;
            }
        } catch (Throwable ignored) {}
        int w = 0, h = 0;
        float hz = 60f;
        try {
            WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            Display d = wm.getDefaultDisplay();
            android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
            d.getRealMetrics(dm);
            w = dm.widthPixels;
            h = dm.heightPixels;
            hz = d.getRefreshRate();
        } catch (Throwable ignored) {}
        String abi0;
        try {
            abi0 = Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0
                    ? Build.SUPPORTED_ABIS[0] : "unknown";
        } catch (Throwable t) {
            abi0 = "unknown";
        }
        return new AndroidDeviceInfo(
                Build.MANUFACTURER == null ? "unknown" : Build.MANUFACTURER,
                Build.MODEL == null ? "unknown" : Build.MODEL,
                Build.VERSION.RELEASE == null ? "unknown" : Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT,
                kernel == null ? "unknown" : kernel,
                abi0,
                ram,
                w, h, hz);
    }

    public String summary() {
        return String.format(Locale.US,
                "%s %s | Android %s (API %d) | %s | RAM %.1f GB | %dx%d @ %.0f Hz",
                manufacturer, model, androidVersion, sdkInt, abi,
                ramBytes / 1e9, screenWidth, screenHeight, refreshHz);
    }
}
