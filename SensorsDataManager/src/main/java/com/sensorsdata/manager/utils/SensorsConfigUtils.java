package com.sensorsdata.manager.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class SensorsConfigUtils {
    private static final String SHARED_PREF_EDITS_FILE = "SensorsDataManagerConfig";
    private static Map<String, String> mIdsMap = new HashMap<>();

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREF_EDITS_FILE, Context.MODE_PRIVATE);
    }

    public static void putString(Context context, String key, String value) {
        try {
            mIdsMap.put(key, value);
            SharedPreferences preferences = getSharedPreferences(context);
            preferences.edit().putString(key, value).apply();
        } catch (Exception exception) {
            SALogger.printStackTrace(exception);
        }
    }

    public static String getString(Context context, String key) {
        try {
            if (mIdsMap.containsKey(key)) {
                return mIdsMap.get(key);
            }
            SharedPreferences preferences = getSharedPreferences(context);
            String value = preferences.getString(key, "");
            mIdsMap.put(key, value);
            return value;
        } catch (Exception exception) {
            SALogger.printStackTrace(exception);
        }
        return "";
    }
}
