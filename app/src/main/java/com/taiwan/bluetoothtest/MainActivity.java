package com.taiwan.bluetoothtest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import java.io.IOException;
import java.io.OutputStream;
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




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(!bluetoothAdapter.isEnabled()){
            //�u�X��ܤ�����ܨϥΪ̬O�ᥴ�}
            Intent enabler = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enabler, REQUEST_ENABLE);
            //�������ܡA�j�楴�}
            //mAdapter.enable();
            Toast.makeText(getApplicationContext(),"Bluetooth open now.",Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(getApplicationContext(),"Bluetooth is opened.",Toast.LENGTH_LONG).show();
        }
        IntentFilter filter=new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        // ��U�@��BroadcastReceiver�A�Ψӱ����j�M��˸m���z
        registerReceiver(mReceiver, filter);

        // Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.btn_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
            }
        });



    }

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
            Toast.makeText(getApplicationContext(),"Connecting with unit: "+address,Toast.LENGTH_LONG).show();
            connect(device);
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

    private void connect(BluetoothDevice device){

        final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        try {
            // �@�i�Ӥ@�w�n����j�M
            bluetoothAdapter.cancelDiscovery();

            // �s����Ӹ˸m
            mBluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
            mBluetoothSocket.connect();
            Log.d("BlueToothTestActivity", "开始连接...");

            // ��ooutputstream
            //mOutputStream = mBluetoothSocket.getOutputStream();

            // �e�X�T��
            //String message = "hello";
            //mOutputStream.write(message.getBytes());

        } catch (IOException e) {

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
