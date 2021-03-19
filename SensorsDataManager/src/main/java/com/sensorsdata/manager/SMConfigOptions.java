package com.sensorsdata.manager;

import javax.net.ssl.SSLSocketFactory;

public class SMConfigOptions {
    /**
     * 数据上报服务器地址
     */
    String mServerUrl;

    /**
     * 两次数据发送的最小时间间隔，单位毫秒
     */
    int mFlushInterval = 15 * 1000;

    /**
     * 本地缓存日志的最大条目数
     */
    int mFlushBulkSize = 100;

    /**
     * 本地缓存上限值，单位 byte，默认为 32MB：32 * 1024 * 1024
     */
    long mMaxCacheSize = 32 * 1024 * 1024L;

    /**
     * 网络上传策略
     */
    int mNetworkTypePolicy = NetworkType.TYPE_3G | NetworkType.TYPE_4G | NetworkType.TYPE_WIFI | NetworkType.TYPE_5G;

    /**
     * HTTPS 证书
     */
    SSLSocketFactory mSSLSocketFactory;

    boolean isLogEnable;

    /**
     * 设置数据上报地址
     *
     * @param serverUrl，数据上报地址
     * @return ConfigOptions
     */
    public SMConfigOptions setServerUrl(String serverUrl) {
        this.mServerUrl = serverUrl;
        return this;
    }

    /**
     * 设置两次数据发送的最小时间间隔，最小值 5 秒
     *
     * @param flushInterval 时间间隔，单位毫秒
     * @return ConfigOptions
     */
    public SMConfigOptions setFlushInterval(int flushInterval) {
        this.mFlushInterval = Math.max(5 * 1000, flushInterval);
        return this;
    }

    /**
     * 设置本地缓存日志的最大条目数
     *
     * @param flushBulkSize 缓存数目
     * @return ConfigOptions
     */
    public SMConfigOptions setFlushBulkSize(int flushBulkSize) {
        this.mFlushBulkSize = Math.max(50, flushBulkSize);
        return this;
    }

    /**
     * 设置本地缓存上限值，单位 byte，默认为 32MB：32 * 1024 * 1024，最小 16MB：16 * 1024 * 1024，若小于 16MB，则按 16MB 处理。
     *
     * @param maxCacheSize 单位 byte
     * @return ConfigOptions
     */
    public SMConfigOptions setMaxCacheSize(long maxCacheSize) {
        this.mMaxCacheSize = Math.max(16 * 1024 * 1024, maxCacheSize);
        return this;
    }

    /**
     * 设置数据的网络上传策略
     *
     * @param networkTypePolicy 数据的网络上传策略
     * @return ConfigOptions
     */
    public SMConfigOptions setNetworkTypePolicy(int networkTypePolicy) {
        this.mNetworkTypePolicy = networkTypePolicy;
        return this;
    }

    /**
     * 设置 SSLSocketFactory
     * @param sf SSLSocketFactory
     * @return ConfigOptions
     */
    public SMConfigOptions setSSLSocketFactory(SSLSocketFactory sf) {
        this.mSSLSocketFactory = sf;
        return this;
    }

    public SMConfigOptions enableLog() {
        this.isLogEnable = true;
        return this;
    }

    public interface NetworkType {
        // NULL
        int TYPE_NONE = 0;
        // 2G
        int TYPE_2G = 1;
        // 3G
        int TYPE_3G = 1 << 1;
        // 4G
        int TYPE_4G = 1 << 2;
        // WIFI
        int TYPE_WIFI = 1 << 3;
        // 5G
        int TYPE_5G = 1 << 4;
        // ALL
        int TYPE_ALL = 0xFF;
    }
}
