/*
 * Created by dengshiwei on 2020/05/11.
 * Copyright 2015－2020 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensorsdata.manager.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import java.util.List;

public class AppInfoUtils {
    /**
     * 获取应用名称
     *
     * @param context Context
     * @return 应用名称
     */
    public static CharSequence getAppName(Context context) {
        if (context == null) return "";
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            return appInfo.loadLabel(packageManager);
        } catch (Exception e) {
            SALogger.printStackTrace(e);
        }
        return "";
    }

    /**
     * 获取 App 的 ApplicationId
     *
     * @param context Context
     * @return ApplicationId
     */
    public static String getProcessName(Context context) {
        if (context == null) return "";
        try {
            return context.getApplicationInfo().processName;
        } catch (Exception ex) {
            SALogger.printStackTrace(ex);
        }
        return "";
    }

    /**
     * 获取 App 版本号
     *
     * @param context Context
     * @return App 的版本号
     */
    public static String getAppVersionName(Context context) {
        if (context == null) return "";
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            SALogger.printStackTrace(e);
        }
        return "";
    }

    /**
     * 获取主进程的名称
     *
     * @param context Context
     * @return 主进程名称
     */
    public static String getMainProcessName(Context context) {
        if (context == null) {
            return "";
        }
        try {
            return context.getApplicationContext().getApplicationInfo().processName;
        } catch (Exception ex) {
            SALogger.printStackTrace(ex);
        }
        return "";
    }

    /**
     * 判断当前进程名称是否为主进程
     *
     * @param context Context
     * @param mainProcessName 进程名
     * @return 是否主进程
     */
    public static boolean isMainProcess(Context context, String mainProcessName) {
        if (context == null) {
            return false;
        }
        if (TextUtils.isEmpty(mainProcessName)) {
            return true;
        }

        String currentProcess = getCurrentProcessName(context.getApplicationContext());
        return TextUtils.isEmpty(currentProcess) || mainProcessName.equals(currentProcess);
    }

    /**
     * 获得当前进程的名字
     *
     * @param context Context
     * @return 进程名称
     */
    private static String getCurrentProcessName(Context context) {
        try {
            int pid = android.os.Process.myPid();
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) {
                return null;
            }

            List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfoList = activityManager.getRunningAppProcesses();
            if (runningAppProcessInfoList != null) {
                for (ActivityManager.RunningAppProcessInfo appProcess : runningAppProcessInfoList) {
                    if (appProcess != null) {
                        if (appProcess.pid == pid) {
                            return appProcess.processName;
                        }
                    }
                }
            }
        } catch (Exception e) {
            SALogger.printStackTrace(e);
        }
        return null;
    }
}
