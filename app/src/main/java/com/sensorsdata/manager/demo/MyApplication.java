package com.sensorsdata.manager.demo;

import android.app.Application;

import com.sensorsdata.manager.SMConfigOptions;
import com.sensorsdata.manager.SensorsDataManagerAPI;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SensorsDataManagerAPI.startWithConfigOptions(this,
                new SMConfigOptions()
                        .setServerUrl("https://newsdktest.datasink.sensorsdata.cn/sa?project=dengshiwei&token=5a394d2405c147ca")
                        .enableLog());
    }
}
