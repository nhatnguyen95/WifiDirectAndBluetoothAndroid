package com.example.nhatnguyen.chat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.Set;

public class BluetoothDevicesActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    public static final String USING_BLUETOOTH= "using_bluetooth";
    private SwipeRefreshLayout swipeRefreshLayout;
    private final IntentFilter intentFilter = new IntentFilter();
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_devices);
        // Set size of dialog
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.x = -10;
        params.height =1000;
        params.width = 700;
        params.y = -10;
        this.getWindow().setAttributes(params);

        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        ListView listViewNewDevicesBluetooth = (ListView)findViewById(R.id.listview_new_devices_bluetooth);
        listViewNewDevicesBluetooth.setAdapter(mNewDevicesArrayAdapter);
        listViewNewDevicesBluetooth.setOnItemClickListener(mNewDeviceClickListener);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_bluetooth_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(true);
                discoveryBluetooth();

            }
        });
        makeDiscoverable(3600);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        discoveryBluetooth();
    }

    private void discoveryBluetooth() {
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                boolean deviceExist=false;
                for(int i=0; i< mNewDevicesArrayAdapter.getCount();i++)
                {
                    if((device.getName()+ "\n" + device.getAddress()).equals(mNewDevicesArrayAdapter.getItem(i).toString()) )  deviceExist=true;
                }
                if(deviceExist== false) mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
        setProgressBarIndeterminateVisibility(true);
        setTitle("Tìm thiết bị bluetooth");
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        mBtAdapter.startDiscovery();
        swipeRefreshLayout.setRefreshing(false);
    }

    private AdapterView.OnItemClickListener mNewDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
         //   Toast.makeText(BluetoothDevicesActivity.this, address,Toast.LENGTH_SHORT).show();
            Intent resultIntent = getIntent();
          //  bundle.putString(MainActivity.RESULT_CODE_BLUETOOTH,address);
            resultIntent.putExtra("bluetooth_address", address);
            setResult(MainActivity.RESULT_CODE_BLUETOOTH, resultIntent);
            finish();
            // Create the result Intent and include the MAC address
            /*
            Intent intent = new Intent(BluetoothDevicesActivity.this, ChatActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(EXTRA_DEVICE_ADDRESS,address);
            bundle.putBoolean(USING_BLUETOOTH,true);
            // Set result and finish this Activity
            intent.putExtra("BluetoothBundle", bundle);
            startActivity(intent);
            */
        }
    };

    public void makeDiscoverable (int timeOut){
        Class <?> baClass = BluetoothAdapter.class;
        Method[] methods = baClass.getDeclaredMethods();
        Method mSetScanMode = methods[44];
        try {
            mSetScanMode.invoke(mBtAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, timeOut);
        } catch (Exception e) {
            Log.e("discoverable", e.getMessage());
        }
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    boolean deviceExist=false;
                    for(int i=0; i< mNewDevicesArrayAdapter.getCount();i++)
                    {
                        if((device.getName()+ "\n" + device.getAddress()).equals(mNewDevicesArrayAdapter.getItem(i).toString()) )  deviceExist=true;
                    }
                    if(deviceExist== false) mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);


            }
        }
    };
    @Override
    public void onRefresh() {
        discoveryBluetooth();
    }
}
