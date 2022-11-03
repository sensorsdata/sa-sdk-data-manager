package com.sensorsdata.manager;

public class EmptyDataManagerAPI extends SensorsDataManagerAPI{

    public EmptyDataManagerAPI() {
    }

    @Override
    public boolean isNetworkRequestEnable() {
        return false;
    }

    @Override
    public void enableNetworkRequest(boolean isRequest) {
    }

    @Override
    public long getMaxCacheSize() {
        return 32 * 1024 * 1024L;
    }

    @Override
    public int getFlushSize() {
        return 100;
    }

    @Override
    public void flush() {
    }

    @Override
    public void flushDelay() {
    }

    @Override
    public void identify(String anonymousId) {
    }

    @Override
    public void setServerUrl(String serverUrl) {
    }

    @Override
    public String getServerUrl() {
        return "";
    }
}
