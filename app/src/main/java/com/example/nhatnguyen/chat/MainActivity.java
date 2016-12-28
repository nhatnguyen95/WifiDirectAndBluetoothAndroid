package com.example.nhatnguyen.chat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.example.nhatnguyen.chat.com.example.nhatnguyen.service.BluetoothChatService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements WifiP2pManager.ConnectionInfoListener {
    public static final int REQUEST_CODE_BLUETOOTH=101;
    public static final int RESULT_CODE_BLUETOOTH=102;
    public static final int REQUEST_CODE_WIFIDIRECT=103;
    public static final int RESULT_CODE_WIFIDIRECT=104;
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME=4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final int PORT = 8988;
    public ListView listViewChat;
    public EditText editTextChat;
    public Button buttonSendMessage;
    public ArrayList<String> arrayListMessage;
    public ArrayAdapter<String> adapter;
    public String addressBluetooth;
    public static String addressGroupOwnerWifidirect;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothChatService mChatService = null;
    private String mConnectedDeviceName = null;
    private String mydevicename;
    private String deviceNameWifidirect;
    private StringBuffer mOutStringBuffer;
    public static ObjectInputStream ois;
    public static ObjectOutputStream oos;
    public static ServerSocket serverSocket = null;
    public  static Socket socket = null;
    public android.os.Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createWidget();
        createBluetooth();
        mydevicename= Build.MODEL;
    }
    public void createWidget(){
        editTextChat = (EditText) findViewById(R.id.edit_text_message);
        listViewChat = (ListView) findViewById(R.id.list_view_chat);
        buttonSendMessage = (Button) findViewById(R.id.button_send_message);
        arrayListMessage = new ArrayList<>();
        listViewChat = (ListView) findViewById(R.id.list_view_chat);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1, arrayListMessage);
        listViewChat.setAdapter(adapter);
    }
    private final android.os.Handler mHandler = new android.os.Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    arrayListMessage.add("Me: "+writeMessage);
                    adapter.notifyDataSetChanged();
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    arrayListMessage.add(readMessage);

                    adapter.notifyDataSetChanged();
                    if(oos !=null){
                        new SendMessageAsyncTask(MainActivity.this).execute(readMessage);
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;

            }
        }
    };
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.atn_discover_bluetooth:
                Intent intentBluetooth = new Intent(MainActivity.this,BluetoothDevicesActivity.class);
                startActivityForResult(intentBluetooth,REQUEST_CODE_BLUETOOTH);
                return true;

            case R.id.atn_discover_wifidirect:
                Intent intentWifiDirect = new Intent(MainActivity.this,WifiDirectDevicesActivity.class);
                startActivityForResult(intentWifiDirect, REQUEST_CODE_WIFIDIRECT);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode== REQUEST_CODE_BLUETOOTH) {
            if(resultCode == RESULT_CODE_BLUETOOTH){
                addressBluetooth = data.getStringExtra("bluetooth_address");
                Toast.makeText(MainActivity.this, addressBluetooth,Toast.LENGTH_SHORT).show();
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(addressBluetooth);
                setupChat();
                // Attempt to connect to the device
                mChatService.connect(device);
                return;
            }
        }
        if(requestCode== REQUEST_CODE_WIFIDIRECT){
            if(resultCode== RESULT_CODE_WIFIDIRECT){

                boolean isGroupOwner = data.getBooleanExtra("is_group_owner",false);
                addressGroupOwnerWifidirect = data.getStringExtra("host_address");
                deviceNameWifidirect = data.getStringExtra("device_name");


                CreateWifiChat();

                if(isGroupOwner){

                    Thread server=  new ServerThread();
                    server.start();


                }else{

                    Thread Client=  new ClientThread();
                    Client.start();


                }
                new ReceivedMessageThread().start();
                setupChat();
            }
        }
    }
    public void createBluetooth(){
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    private void sendMessageBluetooth(String message) {

       if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {

            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            editTextChat.setText("");

        }
    }

    private void setupChat() {
        buttonSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String Message =mydevicename+": "+ editTextChat.getText().toString();
                if(oos != null) {
                    //new SendMessageAsyncTask().execute(sendMessage);
                    new SendMessageAsyncTask(MainActivity.this).execute(Message);
                    // ;
                    editTextChat.setText("");
                }


                sendMessageBluetooth(Message);
             //   editTextChat.setText("");
            }
        });
        if(mChatService == null) mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mChatService != null) mChatService.stop();
    }
    // Wifi Direct
    public void CreateWifiChat(){
        handler = new android.os.Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                arrayListMessage.add((String)msg.obj);
                adapter.notifyDataSetChanged();
            }
        };
    }
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        WifiP2pInfo info = wifiP2pInfo;
        String hostAddress= info.groupOwnerAddress.getHostAddress();
        Toast.makeText(MainActivity.this, hostAddress, Toast.LENGTH_LONG).show();
    }
    public static class ServerThread extends Thread{

        @Override
        public void run() {
            super.run();
            while (oos == null && ois == null) {


                try {
                    serverSocket = new ServerSocket(PORT);
                    Socket socket = serverSocket.accept();
                    oos = new ObjectOutputStream(socket.getOutputStream());
                    ois = new ObjectInputStream(socket.getInputStream());


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
    public static class ClientThread extends Thread{
        @Override
        public void run() {
            super.run();
            while (oos == null && ois == null) {

                try {
                    socket = new Socket(addressGroupOwnerWifidirect, PORT);
                    oos = new ObjectOutputStream(socket.getOutputStream());
                    ois = new ObjectInputStream(socket.getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public class SendMessageAsyncTask extends AsyncTask<String, String, Void> {
        Activity context;
        public SendMessageAsyncTask(Activity context){
            this.context = context;
        }
        @Override
        protected Void doInBackground(String... strings) {
            try {
                oos.writeObject(strings[0]);
                oos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            publishProgress(strings[0]);
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //Toast.makeText(context,values[0],Toast.LENGTH_LONG).show();
            if(mChatService.getState() != BluetoothChatService.STATE_CONNECTED) arrayListMessage.add("Me: "+values[0]);
            adapter.notifyDataSetChanged();
        }
    }
    public class ReceivedMessageThread extends Thread{
        @Override
        public void run() {
            super.run();

            try {
                while (true) {
                    String receiveMessage = "";
                    if(ois!=null)
                    {
                        receiveMessage= (String)ois.readObject();
                        Message msg = handler.obtainMessage();
                        msg.obj = receiveMessage;
                        if(mBluetoothAdapter != null){
                            if (receiveMessage.length() > 0) {
                                // Get the message bytes and tell the BluetoothChatService to write
                                byte[] send = receiveMessage.getBytes();
                                mChatService.write(send);
                            }
                        }
                        handler.sendMessage(msg);
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            // publishProgress(receiveMessage);
        }
    }
}
