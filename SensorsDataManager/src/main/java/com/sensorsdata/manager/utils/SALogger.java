package com.sensorsdata.manager.utils;

import android.util.Log;

public class SALogger {
    private static final String TAG_REGEX = "SA.";
    private static boolean enableLog;

    public static void d(String tag, String msg) {
        if (enableLog) {
            info(tag, msg, null);
        }
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (enableLog) {
            info(tag, msg, tr);
        }
    }

    public static void i(String tag, String msg) {
        if (enableLog) {
            info(tag, msg, null);
        }
    }

    public static void i(String tag, Throwable tr) {
        if (enableLog) {
            info(tag, "", tr);
        }
    }

    public static void i(String tag, String msg, Throwable tr) {
        if (enableLog) {
            info(tag, msg, tr);
        }
    }

    public static void info(String tag, String msg, Throwable tr) {
        try {
            Log.i(TAG_REGEX + tag, msg, tr);
        } catch (Exception e) {
            printStackTrace(e);
        }
    }

    public static void printStackTrace(Exception e) {
        if (enableLog && e != null) {
            e.printStackTrace();
        }
    }

    public static void setEnableLog(boolean isEnableLog) {
        enableLog = isEnableLog;
    }

    public static boolean isLogEnabled() {
        return enableLog;
    }
}
