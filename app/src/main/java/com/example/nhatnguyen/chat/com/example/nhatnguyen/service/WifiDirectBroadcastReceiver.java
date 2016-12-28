package com.example.nhatnguyen.chat.com.example.nhatnguyen.service;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.example.nhatnguyen.chat.MainActivity;
import com.example.nhatnguyen.chat.WifiDirectDevicesActivity;

/**
 * Created by nhatnguyen on 30/11/2016.
 */

public class WifiDirectBroadcastReceiver extends BroadcastReceiver {
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private WifiDirectDevicesActivity activity;


    public WifiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       WifiDirectDevicesActivity activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {


            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {

                activity.setIsWifiP2pEnabled(true);
            } else {
                activity.setIsWifiP2pEnabled(false);
                activity.resetData();

            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            if (manager != null) {
                manager.requestPeers(channel, activity);

            }

        }else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                manager.requestConnectionInfo(channel, activity);

            } else {

                activity.resetData();
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

            activity.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));


        }
    }
}
