package com.singpung.my_arduino_rc_car;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;

public class ConnectThread extends Thread {
    private final BluetoothSocket btSocket;
    private final BluetoothDevice btDevice;
    private BTCommunicationThread btCommunicationThread = null;
    private static final String TAG = "My_arduino_rc_car";
    private boolean socketConnected = false;
    private Handler handler;


    public ConnectThread(BluetoothDevice device, Handler handler) {
        BluetoothSocket tmp = null;
        btDevice = device;
        this.handler = handler;

        try {
            ParcelUuid[] idArray = btDevice.getUuids();
            java.util.UUID myUUID = java.util.UUID.fromString(idArray[0].toString());
            tmp = device.createRfcommSocketToServiceRecord(myUUID);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        btSocket = tmp;
        Log.e(TAG, "Socket's create() method succeed");
    }

    public void run() {
        try {
            btSocket.connect();
        } catch (IOException conneectException) {
            try {
                btSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            Log.e(TAG, "btSocket.connect() failed");
            socketConnected = false;
            return;
        }
        btCommunicationThread = new BTCommunicationThread(btSocket, handler);
        socketConnected = true;
        Log.e(TAG, "btSocket.connect() succeeded");
    }

    public boolean getSocketConnectionStatus() {
        return socketConnected;
    }

    public void cancel() {
        try {
            btSocket.close();
            socketConnected = false;
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

    public void write(byte []bytes) {
        btCommunicationThread.write(bytes);
    };
}