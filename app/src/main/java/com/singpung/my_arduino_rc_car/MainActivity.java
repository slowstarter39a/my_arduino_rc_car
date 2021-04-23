package com.singpung.my_arduino_rc_car;

import android.Manifest;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;

import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;

import android.content.Intent;
import android.content.IntentFilter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.view.View;
import android.util.Log;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.widget.Toast;

import android.os.Bundle;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private Activity activity;
    private View view;
    BluetoothAdapter bluetoothAdapter;
    BluetoothHeadset bluetoothHeadset;
    public final int REQUEST_ENABLE_BT = 1;
    //private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TAG = "My_arduino_rc_car";
    public enum BT_STATUS {NOT_CONNECTED, SEARCHED, CONNECTED};
    private BT_STATUS bt_status = BT_STATUS.NOT_CONNECTED;
    private Button buttonBtCtl;
    private ListView listView ;
    private BTDeviceItemAdapter adapter;
    ProgressDialog dialog;
    private boolean isActionFound = false;
    List<BluetoothDevice> btDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;
        dialog = new ProgressDialog(this);
        btDevices = new ArrayList<BluetoothDevice>();

        buttonBtCtl = (Button) findViewById(R.id.button_bt_ctl);
        adapter = new BTDeviceItemAdapter(activity);

        buttonBtCtl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG, "button clicked");

                view = activity.getLayoutInflater().inflate(R.layout.dialog_listview_layout, null);
                listView = (ListView)view.findViewById(R.id.listview);
                listView.setAdapter(adapter);

                btDevices.clear();
                adapter.clearAll();
                adapter.notifyDataSetChanged();
                isActionFound = false;

                AlertDialog.Builder listViewDialog = new AlertDialog.Builder(activity);
                listViewDialog.setView(view);
                listViewDialog.setPositiveButton("Ok", null);
                listViewDialog.setNegativeButton("Cancel", null);
                listViewDialog.setTitle("BT Device list");

                listViewDialog.show();
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Title")
                                .setMessage("Position clicked: " + position)
                                .create()
                                .show();
                    }
                });

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
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();

                if (isActionFound == false) {
                    adapter.addItem(new String("* Available devices"));
                    isActionFound = true;
                }

                if (!isBtDeviceInList(device)) {
                    btDevices.add(device);
                    adapter.addItem(new String(deviceName + "\n" + deviceHardwareAddress));

                    Log.e(TAG, "deviceName = " + deviceName + ", deviceHardwareAddress = " + deviceHardwareAddress);
                }
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.e(TAG, "ACTION_DISCOVERY_STARTED intent received");
                dialog.setCancelable(false);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setMessage("Searching BT devices. Please wait...");
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                dialog.show();
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.e(TAG, "ACTION_DISCOVERY_FINISHED intent received");
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
            else {
            }

            adapter.notifyDataSetChanged();
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

    private void queryPairedDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            adapter.addItem(new String("* Paired devices"));

            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();

                btDevices.add(device);
                adapter.addItem(new String(deviceName + "\n" + deviceHardwareAddress));

                Log.e(TAG, "deviceName = " + deviceName + ", deviceHardwareAddress = " + deviceHardwareAddress);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
    }
}