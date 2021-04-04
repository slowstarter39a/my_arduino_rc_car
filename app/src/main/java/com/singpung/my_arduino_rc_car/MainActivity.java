package com.singpung.my_arduino_rc_car;

import android.Manifest;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;

import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;

import android.content.Intent;
import android.content.IntentFilter;

import android.widget.Button;
import android.widget.ListView;
import android.view.View;
import android.util.Log;

import android.os.Bundle;

import java.util.List;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter;
    BluetoothHeadset bluetoothHeadset;
    public final int REQUEST_ENABLE_BT = 1;
    //private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TAG = "My_arduino_rc_car";
    public enum BT_STATUS {NOT_CONNECTED, SEARCHED, CONNECTED};
    private BT_STATUS bt_status = BT_STATUS.NOT_CONNECTED;
    private Button buttonBtCtl;
    private ListView listView ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonBtCtl = (Button) findViewById(R.id.button_bt_ctl);
        listView = (ListView)findViewById(R.id.bt_device_list);

        buttonBtCtl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG, "button clicked");

                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter == null) {
                    Log.e(TAG, "Device doesn't  support Bluetooth");
                    return;
                }

                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }

                doBtDeviceDiscovery();

                // Get permission
                String[] permission_list = {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                };

                ActivityCompat.requestPermissions(MainActivity.this, permission_list,  1);
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, "onActivityResult, requestCode = " + requestCode + ", resultCode = " + resultCode);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        List<BluetoothDevice> btDevices = new ArrayList<BluetoothDevice>();
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();

                if (!isBtDeviceInList(device)) {
                    btDevices.add(device);
                    Log.e(TAG, "deviceName = " + deviceName + ", deviceHardwareAddress = " + deviceHardwareAddress);
                }
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.e(TAG, "ACTION_DISCOVERY_STARTED intent received");
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.e(TAG, "ACTION_DISCOVERY_FINISHED intent received");
            }
            else {
            }
        }
        private boolean isBtDeviceInList(BluetoothDevice btDev) {
            int size = btDevices.size();
            for (int i = 0; i < size; i++) {
                if (btDevices.get(i).equals(btDev)) {
                    return true;
                }
            }
            return false;
        }
    };

    private void doBtDeviceDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
    }





}