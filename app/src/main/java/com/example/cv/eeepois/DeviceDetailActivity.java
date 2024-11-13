package com.example.cv.eeepois;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class DeviceDetailActivity extends AppCompatActivity {

    private BluetoothSocket btSocket;
    private InputStream btInputStream;
    private TextView txtDeviceName;
    private TextView txtDeviceAddress;
    private TextView txtDeviceMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);

        txtDeviceName = findViewById(R.id.txtDeviceName);
        txtDeviceAddress = findViewById(R.id.txtDeviceAddress);
        txtDeviceMessage = findViewById(R.id.txtDeviceMessage);

        Intent intent = getIntent();
        String deviceName = intent.getStringExtra("deviceName");
        String deviceAddress = intent.getStringExtra("deviceAddress");

        txtDeviceName.setText(deviceName);
        txtDeviceAddress.setText(deviceAddress);

        connectToDevice(deviceAddress);
    }

    private void connectToDevice(String MAC) {
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC);
        boolean connected = false;
        int attempts = 0;
        while (!connected && attempts < 3) {
            try {
                Log.d("DeviceDetailActivity", "Attempting to create RFCOMM socket...");
                btSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                Log.d("DeviceDetailActivity", "Socket created, attempting to connect...");
                btSocket.connect();
                Log.d("DeviceDetailActivity", "Connected to device, getting input stream...");
                btInputStream = btSocket.getInputStream();
                readDataFromDevice();
                connected = true;
            } catch (IOException e) {
                Log.e("DeviceDetailActivity", "Failed to connect to device, attempt " + (attempts + 1), e);
                attempts++;
                closeSocket();
                if (attempts >= 3) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(DeviceDetailActivity.this, "Failed to connect to device after 3 attempts", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }
    }
    
    private void closeSocket() {
        try {
            if (btSocket != null) {
                btSocket.close();
            }
        } catch (IOException e) {
            Log.e("DeviceDetailActivity", "Failed to close socket", e);
        }
    }

    private void readDataFromDevice() {
        new Thread(new Runnable() {
            public void run() {
                byte[] buffer = new byte[1024];
                int bytes;
                while (true) {
                    try {
                        bytes = btInputStream.read(buffer);
                        final String message = new String(buffer, 0, bytes);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txtDeviceMessage.setText(message);
                            }
                        });
                    } catch (IOException e) {
                        break;
                    }
                }
            }
        }).start();
    }
}