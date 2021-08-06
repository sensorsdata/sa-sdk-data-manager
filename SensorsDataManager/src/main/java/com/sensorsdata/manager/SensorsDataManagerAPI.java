package com.sensorsdata.manager;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.sensorsdata.manager.db.DBConstant;
import com.sensorsdata.manager.utils.AppInfoUtils;
import com.sensorsdata.manager.utils.NetworkUtils;
import com.sensorsdata.manager.utils.SALogger;
import com.sensorsdata.manager.utils.SensorsConfigUtils;

import static com.sensorsdata.manager.utils.SADataHelper.assertValue;

public class SensorsDataManagerAPI {
    private static final String TAG = "SensorsDataManagerAPI";
    private final SMConfigOptions mSMConfigOptions;
    private static SensorsDataManagerAPI mInstance;
    /* 是否请求网络 */
    private boolean mEnableNetworkRequest = true;
    private final AnalyticsFlushData mFlushDataMessage;
    static boolean mIsMainProcess = false;
    private String mServerUrl;
    private Context mContext;

    public synchronized static void startWithConfigOptions(Context context, SMConfigOptions configOptions) {
        if (context == null) {
            throw new IllegalArgumentException("Context should not be null.");
        }

        if (configOptions == null) {
            throw new IllegalArgumentException("ConfigOptions should not be null.");
        }

        if (mInstance == null) {
            mInstance = new SensorsDataManagerAPI(context, configOptions);
        }
    }

    private SensorsDataManagerAPI(Context context, SMConfigOptions configOptions) {
        Context applicationContext = context.getApplicationContext();
        this.mSMConfigOptions = configOptions;
        this.mContext = applicationContext;
        SALogger.setEnableLog(configOptions.isLogEnable);
        setServerUrl(configOptions.mServerUrl);
        try {
            String mainProcessName = AppInfoUtils.getMainProcessName(applicationContext);
            if (TextUtils.isEmpty(mainProcessName)) {
                final ApplicationInfo appInfo = applicationContext.getApplicationContext().getPackageManager()
                        .getApplicationInfo(applicationContext.getPackageName(), PackageManager.GET_META_DATA);
                Bundle configBundle = appInfo.metaData;
                mainProcessName = configBundle.getString("com.sensorsdata.analytics.android.MainProcessName");
            }
            mIsMainProcess = AppInfoUtils.isMainProcess(applicationContext, mainProcessName);
        } catch (final PackageManager.NameNotFoundException e) {
            SALogger.printStackTrace(e);
        }
        mFlushDataMessage = new AnalyticsFlushData(applicationContext, configOptions);
        NetworkUtils.registerNetworkListener(applicationContext);
    }

    public static SensorsDataManagerAPI sharedInstance() throws NullPointerException {
        if (null == mInstance) {
            SALogger.i(TAG, "The static method startWithConfigOptions(Context context, ConfigOptions configOptions) should be called before calling sharedInstance()");
            throw new NullPointerException("The static method startWithConfigOptions(Context context, ConfigOptions configOptions) should be called before calling sharedInstance()");
        }
        return mInstance;
    }

    /**
     * 是否允许访问网络
     * @return 是否允许访问网络
     */
    public boolean isNetworkRequestEnable() {
        return mEnableNetworkRequest;
    }

    /**
     * 设置是否允许访问网络
     * @param isRequest 是否允许访问网咯
     */
    public void enableNetworkRequest(boolean isRequest) {
        this.mEnableNetworkRequest = isRequest;
    }

    /**
     * 获取数据库缓存大小
     * @return 缓存大小
     */
    public long getMaxCacheSize() {
        return this.mSMConfigOptions.mMaxCacheSize;
    }

    /**
     * 获取缓存条数
     * @return 缓存条数
     */
    public int getFlushSize() {
        return this.mSMConfigOptions.mFlushBulkSize;
    }

    /**
     * flush 上报数据
     */
    public void flush() {
        mFlushDataMessage.flush();
    }

    /**
     * 延迟 flush 上报数据
     */
    public void flushDelay() {
        mFlushDataMessage.flushDelay();
    }

    /**
     * 登录
     * @param loginId 用户 ID
     */
    private void login(String loginId) {
        try {
            assertValue(loginId);
            SensorsConfigUtils.putString(mContext, DBConstant.LOGIN_ID, loginId);
        } catch (Exception e) {
            SALogger.printStackTrace(e);
        }
    }

    /**
     * 设置匿名 ID
     * @param anonymousId 匿名 ID
     */
    public void identify(final String anonymousId) {
        try {
            assertValue(anonymousId);
            SensorsConfigUtils.putString(mContext, DBConstant.ANONYMOUS_ID, anonymousId);
        } catch (Exception e) {
            SALogger.printStackTrace(e);
        }
    }

    /**
     * 用户登出
     */
    private void logout() {
        try {
            SensorsConfigUtils.putString(mContext, DBConstant.LOGIN_ID, "");
        } catch (Exception e) {
            SALogger.printStackTrace(e);
        }
    }

    private void setServerUrl(String serverUrl) {
        if (TextUtils.isEmpty(serverUrl)) {
            mServerUrl = serverUrl;
            SALogger.i(TAG, "Server url is null or empty.");
            return;
        }

        Uri serverURI = Uri.parse(serverUrl);
        String hostServer = serverURI.getHost();
        if (!TextUtils.isEmpty(hostServer) && hostServer.contains("_")) {
            SALogger.i(TAG, "Server url " + serverUrl + " contains '_' is not recommend，" +
                    "see details: https://en.wikipedia.org/wiki/Hostname");
        }

        mServerUrl = serverUrl;
    }

    String getServerUrl() {
        return mServerUrl;
    }
}
