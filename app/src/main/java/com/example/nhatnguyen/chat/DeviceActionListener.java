package com.example.nhatnguyen.chat;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by nhatnguyen on 30/11/2016.
 */

public interface DeviceActionListener {
    void showDetails(WifiP2pDevice device);

    void cancelDisconnect();

    void connect(WifiP2pConfig config);

    void disconnect();
}
