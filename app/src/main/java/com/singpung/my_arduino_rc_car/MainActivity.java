package com.singpung.my_arduino_rc_car;

import android.Manifest;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;

import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.view.View;
import android.util.Log;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.TextView;

import android.os.Bundle;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private Activity activity;
    private View view;
    BluetoothAdapter bluetoothAdapter;
    BluetoothHeadset bluetoothHeadset;
    public final int REQUEST_ENABLE_BT = 1;
    //private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TAG = "My_arduino_rc_car";

    public enum BT_STATUS {NOT_CONNECTED, SEARCHED, CONNECTED}

    private BT_STATUS bt_status = BT_STATUS.NOT_CONNECTED;
    private Button buttonBtCtl;
    private ListView listView;
    private BTDeviceItemAdapter adapter;
    ProgressDialog dialog;
    private boolean isActionFound = false;
    List<BluetoothDevice> btDevices;
    ConnectThread btConnectThread = null;
    public TextView statusTextView;
    BluetoothDevice curBtDevice;

    TimerTask timerTask;
    Timer timer = new Timer();
    ImageView imageView;

    float baseX;
    float baseY;
    int   imgHalfWidth;
    int   imgHalfHeight;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
           // String temp_str = "Hello_Arduino";

            switch(msg.what) {
                case BTCommunicationThread.MessageConstants.MESSAGE_READ:
                    //btConnectThread.write(temp_str.getBytes());
                    break;
                case BTCommunicationThread.MessageConstants.MESSAGE_WRITE:
                    break;
                case BTCommunicationThread.MessageConstants.MESSAGE_TOAST:
                    break;
                default:
                    break;
            }
        }
    };

    private void startTimerTask()
    {
        stopTimerTask();

        timerTask = new TimerTask()
        {
            float curX = imageView.getX();
            float curY = imageView.getY();
            float dx = curX - baseX;
            float dy = curY - baseY;

            float newPosX = 0;
            float newPosY = 0;
            int i = 0;

            @Override
            public void run()
            {
                if ( i < 10) {
                    newPosX = curX - i * dx/10;
                    newPosY = curY - i * dy/10;

                    imageView.setX(newPosX);
                    imageView.setY(newPosY);

                    i++;
                }
                else {
                    imageView.setX(baseX);
                    imageView.setY(baseY);
                    stopTimerTask();
                }
            }
        };
        timer.schedule(timerTask,0 ,30);
    }

    private void stopTimerTask()
    {
        if(timerTask != null)
        {
            timerTask.cancel();
            timerTask = null;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;
        dialog = new ProgressDialog(this);

        btDevices = new ArrayList<BluetoothDevice>();

        buttonBtCtl = (Button) findViewById(R.id.button_bt_ctl);
        adapter = new BTDeviceItemAdapter(activity);

        imageView = (ImageView)findViewById(R.id.imageView);
        imageView.setImageResource(R.drawable.circle_car_control);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        imageView.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        imgHalfWidth = imageView.getWidth()/2;
                        imgHalfHeight = imageView.getHeight()/2;

                        baseX = width/2 - imgHalfWidth;
                        baseY = height - height/2;
                        imageView.setX(baseX);
                        imageView.setY(baseY);

                        imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });

        view = findViewById(R.id.main);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    stopTimerTask();
                }
                else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    imageView.setX(motionEvent.getX() - imgHalfWidth);
                    imageView.setY(motionEvent.getY() - imgHalfHeight);
                }
                else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    startTimerTask();
                }
                return true;
            }
        });


        buttonBtCtl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e(TAG, "button clicked");

                if (bt_status == BT_STATUS.CONNECTED) {
                    btConnectThread.cancel();
                    return;
                }

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
                AlertDialog alertDialog = listViewDialog.create();
                alertDialog.show();

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        BluetoothDevice btDevice = getBtDeviceFromBTDeviceList(parent.getAdapter().getItem(position).toString());
                        btConnectThread = new ConnectThread(btDevice, handler);

                        btConnectThread.start();
                        try {
                            btConnectThread.join();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (btConnectThread.getSocketConnectionStatus()) {
                            curBtDevice = btDevice;
                            alertDialog.dismiss();
                        }
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

                queryPairedDevices();
                doBtDeviceDiscovery();

                String[] permission_list = {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                };

                ActivityCompat.requestPermissions(MainActivity.this, permission_list, 1);
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        registerReceiver(receiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, "onActivityResult, requestCode = " + requestCode + ", resultCode = " + resultCode);
    }

    public void updateBtStatus(final BT_STATUS bt_status) {
        statusTextView = (TextView) findViewById(R.id.status);

        if (bt_status == BT_STATUS.CONNECTED) {
            statusTextView.setText("Connected (" + curBtDevice.getName() + " " + curBtDevice.getAddress() + ")");
            buttonBtCtl.setText("Disconnect");

        } else {
            statusTextView.setText("Not connected");
            buttonBtCtl.setText("Search BT Devices");
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.e(TAG, "action " + action);

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
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.e(TAG, "ACTION_DISCOVERY_STARTED intent received");
                dialog.setCancelable(false);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setMessage("Searching BT devices. Please wait...");
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        bluetoothAdapter.cancelDiscovery();
                    }
                });
                dialog.show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.e(TAG, "ACTION_DISCOVERY_FINISHED intent received");
                if (dialog != null) {
                    dialog.dismiss();
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Log.e(TAG, "ACTION_ACL_CONNECTED");
                bt_status = BT_STATUS.CONNECTED;
                updateBtStatus(bt_status);
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Log.e(TAG, "ACTION_ACL_DISCONNECTED");
                curBtDevice = null;
                bt_status = BT_STATUS.NOT_CONNECTED;
                updateBtStatus(bt_status);
            } else {
            }

            adapter.notifyDataSetChanged();
        }
    };

    private boolean isBtDeviceInList(BluetoothDevice btDev) {
        int size = btDevices.size();
        for (int i = 0; i < size; i++) {
            if (btDevices.get(i).equals(btDev)) {
                return true;
            }
        }
        return false;
    }

    private BluetoothDevice getBtDeviceFromBTDeviceList(String str) {
        if ((str == null) || (str == "")) {
            return null;
        }

        int size = btDevices.size();
        for (int i = 0; i < size; i++) {
            if ((str.contains(btDevices.get(i).getName()) &&
                    str.contains(btDevices.get(i).getAddress()))) {
                return btDevices.get(i);
            }
        }
        return null;
    }

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