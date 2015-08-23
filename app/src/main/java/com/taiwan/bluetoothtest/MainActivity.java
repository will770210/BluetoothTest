package com.taiwan.bluetoothtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.taiwan.bluetoothtest.common.constants.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {
    private final int REQUEST_ENABLE = 1;
    private final String TAG = "Blutooth";
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private ListView pairedListView;
    private ListView newDevicesListView;
    //SPP use to sent data
    private final  UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //To use to connection two device
    private static BluetoothSocket mBluetoothSocket = null;
    private static OutputStream mOutputStream = null;
    private static InputStream mInputStream = null;
    public BluetoothChatService mChatService =null;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mInputStringBuffer;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        IntentFilter filter=new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        // 註冊BroadcastReceiver
        registerReceiver(mReceiver, filter);

        // Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.btn_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
            }
        });

        Button sendButton = (Button) findViewById(R.id.btn_send);
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendMessage("Hello!!");
            }

        });




    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult

        if(!bluetoothAdapter.isEnabled()){
            //詢問使用者是否要開啟藍芽
            Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enabler, REQUEST_ENABLE);
            //直接開啟藍芽
            //mAdapter.enable();
            Toast.makeText(getApplicationContext(),"Bluetooth open now.",Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(getApplicationContext(),"Bluetooth is opened.",Toast.LENGTH_LONG).show();
        }

        if (mChatService == null) {
            setupChat();
        }


    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "--------------------->");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                Log.d(TAG,"1.Start mChatService--------------------->");
                // Start the Bluetooth chat services
                mChatService.start();
                Log.d(TAG, "2.Start mChatService--------------------->");
            }
        }



    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat..................");

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getApplicationContext(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
//        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
//            Toast.makeText(this, "Your device is not connected.", Toast.LENGTH_SHORT).show();
//            return;
//        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG,Integer.toString(msg.what));
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:

                            break;
                        case BluetoothChatService.STATE_CONNECTING:

                            break;
                        case BluetoothChatService.STATE_LISTEN:

                        case BluetoothChatService.STATE_NONE:

                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Toast.makeText(getApplicationContext(),writeMessage,Toast.LENGTH_LONG).show();
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Toast.makeText(getApplicationContext(),readMessage,Toast.LENGTH_LONG).show();
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
//                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
//                    if (null != activity) {
//                        Toast.makeText(activity, "Connected to "
//                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
//                    }
                    break;
                case Constants.MESSAGE_TOAST:
//                    if (null != get) {
//                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
//                                Toast.LENGTH_SHORT).show();
//                    }
                    break;
            }
        }
    };

    /** 搜尋藍芽裝置*/

    private void doDiscovery(){
        Log.d(TAG, "doDiscovery()");

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);


        // If we're already discovering, stop it
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        bluetoothAdapter.startDiscovery();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // one for newly discovered devices
        ArrayAdapter<String> pairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(null);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(null);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                //sendMessage(device);
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedDevicesArrayAdapter.add("No Device");
        }
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            bluetoothAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            Toast.makeText(getApplicationContext(),"BluetoothSocket is connect to Device ["+device.getName()+"]....",Toast.LENGTH_SHORT).show();
            mChatService.connect(device,false);
            //connect(device);
            doDiscovery();



            // Create the result Intent and include the MAC address
            //Intent intent = new Intent();
            //intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            //setResult(Activity.RESULT_OK, intent);
            //finish();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {//���]��
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "find device:" + device.getName() + device.getAddress());
                    mNewDevicesArrayAdapter.add(device.getName() + ":  " + device.getAddress());
                    //sendMessage(device);
                }
            }else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {//�j������
                setTitle("Discovery finish.");
                Log.d(TAG, "find over");
                setProgressBarIndeterminateVisibility(false);
                if (mNewDevicesArrayAdapter.getCount()== 0) {
                    mNewDevicesArrayAdapter.add("No device");
                    Log.v(TAG, "find over");
                }
            }
        }
    };

    private void connect(BluetoothDevice device) {

        final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        try {
            // �@�i�Ӥ@�w�n����j�M
            bluetoothAdapter.cancelDiscovery();


//            if (device.getBondState() == BluetoothDevice.BOND_NONE) {
//                //利用反射方法调用
//                BluetoothDevice.createBond(BluetoothDevice device);
////                Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
////                Log.d("BlueToothTestActivity", "开始配对");
////                boolean returnValue = (Boolean) createBondMethod.invoke(device);
//            } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
//
//            }


            // �s����Ӹ˸m
            if (mBluetoothSocket != null && mBluetoothSocket.getRemoteDevice().equals(device)) {
                if (mBluetoothSocket.isConnected()) {
                    byte[] buffer = new byte[1024];
                    int i = 0;
                    mInputStream = mBluetoothSocket.getInputStream();
                    while ((i = mInputStream.read()) != -1) {
                        Log.d(TAG, Integer.toString(i));
                    }


                    // ooutputstream
                    mOutputStream = mBluetoothSocket.getOutputStream();

                    //
                    String message = "hello " + device.getName();
                    mOutputStream.write(message.getBytes());
                    Toast.makeText(getApplicationContext(), "Device [" + device.getName() + "] BluetoothSocket is connected", Toast.LENGTH_SHORT).show();
                }
            } else {
                mBluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                mBluetoothSocket.connect();
                Toast.makeText(getApplicationContext(), "Device [" + device.getName() + "] BluetoothSocket is creating...", Toast.LENGTH_SHORT).show();
            }


        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }


        @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
