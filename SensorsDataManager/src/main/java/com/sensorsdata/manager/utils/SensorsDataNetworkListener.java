package com.sensorsdata.manager.utils;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

import com.sensorsdata.manager.SensorsDataManagerAPI;

public class SensorsDataNetworkListener {
    private final static String TAG = "SA.SensorsDataNetworkListener";

    static class SABroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                SensorsDataManagerAPI.sharedInstance().flush();
                SALogger.i(TAG, "SABroadcastReceiver is receiving ConnectivityManager.CONNECTIVITY_ACTION broadcast");
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    static class SANetworkCallbackImpl extends ConnectivityManager.NetworkCallback {

        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            SensorsDataManagerAPI.sharedInstance().flush();
            SALogger.i(TAG, "onAvailable is calling");
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            SALogger.i(TAG, "onCapabilitiesChanged is calling");
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            SALogger.i(TAG, "onLost is calling");
        }
    }

}
