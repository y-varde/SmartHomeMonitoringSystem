package com.example.cv.eeepois;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import android.util.Log;

public class DeviceDetailActivity extends AppCompatActivity {

    private static final String DEVICE_ADDRESS = "6C:79:B8:C6:FE:44"; // Hardcoded Bluetooth address
    private static final UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // SPP UUID
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

        txtDeviceName.setText("Hardcoded Device");
        txtDeviceAddress.setText(DEVICE_ADDRESS);

        connectToDevice(DEVICE_ADDRESS);
    }

    private void connectToDevice(String MAC) {
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC);
        if (device == null) {
            showToast("Remote Device Not Found");
            return;
        }

        UUID[] uuids = fetchUuids(device);
        if (uuids == null || uuids.length == 0) {
            showToast("No UUIDs found for device, using default UUID");
            uuids = new UUID[]{DEFAULT_UUID};
        }

        boolean connected = attemptConnection(device, uuids);
        if (!connected) {
            showToast("Failed to connect to device with any UUID");
        }
    }

    private UUID[] fetchUuids(BluetoothDevice device) {
        device.fetchUuidsWithSdp();
        ParcelUuid[] parcelUuids = device.getUuids();
        if (parcelUuids != null) {
            UUID[] uuids = new UUID[parcelUuids.length];
            for (int i = 0; i < parcelUuids.length; i++) {
                uuids[i] = parcelUuids[i].getUuid();
            }
            return uuids;
        }
        return null;
    }

    private boolean attemptConnection(BluetoothDevice device, UUID[] uuids) {
        boolean connected = false;
        for (UUID uuid : uuids) {
            if (tryConnect(device, uuid)) {
                connected = true;
                break;
            }
        }
        return connected;
    }

    private boolean tryConnect(BluetoothDevice device, UUID uuid) {
        boolean connected = false;
        int attempts = 0;
        while (!connected && attempts < 3) {
            try {
                Log.d("DeviceDetailActivity", "Attempting to create RFCOMM socket with UUID: " + uuid);
                btSocket = device.createRfcommSocketToServiceRecord(uuid);
                Log.d("DeviceDetailActivity", "Socket created, attempting to connect...");
                btSocket.connect();
                Log.d("DeviceDetailActivity", "Connected to device, getting input stream...");
                btInputStream = btSocket.getInputStream();
                startReadingData();
                connected = true;
            } catch (IOException e) {
                Log.e("DeviceDetailActivity", "Failed to connect to device with UUID: " + uuid + ", attempt " + (attempts + 1), e);
                attempts++;
                closeSocket();
                if (attempts >= 3) {
                    showToast("Failed to connect to device after 3 attempts with UUID: " + uuid);
                }
            }
        }
        return connected;
    }

    private void startReadingData() {
        new Thread(new Runnable() {
            public void run() {
                readDataFromDevice();
            }
        }).start();
    }

    private void readDataFromDevice() {
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

    private void closeSocket() {
        try {
            if (btSocket != null) {
                btSocket.close();
            }
        } catch (IOException e) {
            Log.e("DeviceDetailActivity", "Failed to close socket", e);
        }
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DeviceDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}