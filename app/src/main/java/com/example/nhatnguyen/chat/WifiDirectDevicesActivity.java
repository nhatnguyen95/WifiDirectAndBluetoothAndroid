package com.example.nhatnguyen.chat;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nhatnguyen.chat.com.example.nhatnguyen.service.WifiDirectBroadcastReceiver;

import java.util.ArrayList;
import java.util.List;

public class WifiDirectDevicesActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener,  WifiP2pManager.ChannelListener, WifiP2pManager.PeerListListener, DeviceActionListener, WifiP2pManager.ConnectionInfoListener {
    SwipeRefreshLayout swipeRefreshLayout;
    WiFiPeerListAdapter adapter;
    ListView listViewNewDevicesWifiDirect;
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();//
    IntentFilter intentFilter = new IntentFilter();
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    ProgressDialog progressDialog = null;
    View mContentView = null;
    private boolean isWifiP2pEnabled = false;
    private WifiP2pDevice device;
    private BroadcastReceiver receiver = null;
    String deviceName;
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_direct_devices);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.x = -10;
        params.height =1000;
        params.width = 700;
        params.y = -10;

        //Add intent filter
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WifiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
        this.getWindow().setAttributes(params);
        swipeRefreshLayout = (SwipeRefreshLayout)(findViewById(R.id.swipe_refresh_wifidirect_layout));
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(true);
                discoverPeers();
            }
        });
       // adapter = new ArrayAdapter<WifiP2pDevice>(WifiDirectDevicesActivity.this, R.layout.row_device_wifidirect, peers);
        adapter = new WiFiPeerListAdapter(WifiDirectDevicesActivity.this, R.layout.row_device_wifidirect, peers);
        listViewNewDevicesWifiDirect = (ListView) findViewById(R.id.listview_new_devices_wifidirect);
        listViewNewDevicesWifiDirect.setAdapter(adapter);
        discoverPeers();
        listViewNewDevicesWifiDirect.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                WifiP2pDevice device = (WifiP2pDevice) peers.get(i);
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(WifiDirectDevicesActivity.this, "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true

                );
                connect(config);
            }
        });


    }
    private static String getDeviceStatus(int deviceStatus) {
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";

        }
    }
    public void discoverPeers(){
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int i) {
                Toast.makeText(WifiDirectDevicesActivity.this, "Discovery Failed : "+ i,
                        Toast.LENGTH_SHORT).show();
            }
        });

    }
    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {

        peers.clear();
        peers.addAll(wifiP2pDeviceList.getDeviceList());
        adapter.notifyDataSetChanged();
        if (peers.size() == 0) {
            Toast.makeText(WifiDirectDevicesActivity.this,"No Devices Found", Toast.LENGTH_LONG).show();
            return;
        }
        swipeRefreshLayout.setRefreshing(false);
    }



    /*
    @Override
    protected void onResume() {
        super.onResume();
        receiver = new WifiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }
    */
    private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

        private List<WifiP2pDevice> items;
        Activity context=null;
        int textViewResourceId;
        /**
         * @param context
         * @param textViewResourceId
         * @param objects
         */
        public WiFiPeerListAdapter(Activity context, int textViewResourceId,
                                   List<WifiP2pDevice> objects) {
            super(context, textViewResourceId, objects);
            items = objects;
            this.context = context;
            this.textViewResourceId= textViewResourceId;


        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)context.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_device_wifidirect, null);
            }
            WifiP2pDevice device = items.get(position);
            if (device != null) {
                TextView textViewDevice = (TextView) v.findViewById(R.id.device_name);
                if (textViewDevice != null) {
                    textViewDevice.setText(device.deviceName);
                }
            }

            return v;

        }
    }

    public void resetData(){
        peers.clear();
        adapter.notifyDataSetChanged();
    }
    @Override
    public void onChannelDisconnected() {
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int i) {

            }
        });
    }

    @Override
    public void showDetails(WifiP2pDevice device) {

    }
    public void updateThisDevice(WifiP2pDevice device){
        deviceName= device.deviceName;

    }
    @Override
    public void cancelDisconnect() {

    }
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        WifiP2pInfo info = wifiP2pInfo;
        String hostAddress= info.groupOwnerAddress.getHostAddress();
        boolean isGroupOwner= info.isGroupOwner;
        Intent resultIntent = getIntent();
        resultIntent.putExtra("is_group_owner", isGroupOwner);
        resultIntent.putExtra("host_address", hostAddress);
        resultIntent.putExtra("device_name", deviceName);
        setResult(MainActivity.RESULT_CODE_WIFIDIRECT, resultIntent);
        finish();

    }
    @Override
    public void connect(WifiP2pConfig config) {
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }
            @Override
            public void onFailure(int i) {

            }
        });
    }

    @Override
    public void disconnect() {

    }


    @Override
    public void onRefresh() {
        discoverPeers();
    }
}
