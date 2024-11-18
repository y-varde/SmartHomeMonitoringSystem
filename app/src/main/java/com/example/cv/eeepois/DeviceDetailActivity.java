package com.example.cv.eeepois;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

import android.util.Log;

public class DeviceDetailActivity extends AppCompatActivity {

    private static final String TAG = "DeviceDetailActivity";
    private BluetoothGatt bluetoothGatt;
    private TextView txtDeviceName;
    private TextView txtDeviceAddress;
    private TextView txtDeviceMessage;

    private static final UUID SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private StringBuilder messageBuilder = new StringBuilder();

    // This method is called when the activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);

        txtDeviceName = findViewById(R.id.txtDeviceName);
        txtDeviceAddress = findViewById(R.id.txtDeviceAddress);
        txtDeviceMessage = findViewById(R.id.txtDeviceMessage);

        // Retrieve device information from the Intent
        String deviceName = getIntent().getStringExtra("deviceName");
        String deviceAddress = getIntent().getStringExtra("deviceAddress");

        txtDeviceName.setText(deviceName);
        txtDeviceAddress.setText(deviceAddress);

        connectToDevice(deviceAddress);
    }

    // This method is called when the activity is destroyed (e.g. when the user presses the back button)
    private void connectToDevice(String MAC) {
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC);
        if (device == null) {
            showToast("Remote Device Not Found");
            return;
        }

        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        // This method is called when the connection state changes (e.g. when the device connects or disconnects)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server.");
                showToast("Connected to GATT server.");
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.");
                showToast("Disconnected from GATT server.");
            }
        }

        // This method is called when services are discovered on the device (e.g. after connecting)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered.");
                showToast("Services discovered.");
                readCharacteristic();
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        // This method is called when a characteristic is read from the device
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                updateMessage(characteristic);
            } else {
                Log.w(TAG, "Characteristic read failed with status: " + status);
            }
        }

        // This method is called when a characteristic is changed on the device (e.g. when a notification is received)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            updateMessage(characteristic);
        }
    };

    // This method reads a characteristic from the device
    private void readCharacteristic() {
        if (bluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }

        BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
            if (characteristic != null) {
                boolean result = bluetoothGatt.readCharacteristic(characteristic);
                Log.d(TAG, "Read characteristic initiated: " + result);

                // Enable notifications if the characteristic supports it
                bluetoothGatt.setCharacteristicNotification(characteristic, true);
            } else {
                Log.w(TAG, "Characteristic not found");
            }
        } else {
            Log.w(TAG, "Service not found");
        }
    }

    // This method updates the message displayed on the screen
    private void updateMessage(BluetoothGattCharacteristic characteristic) {
        final String chunk = new String(characteristic.getValue());
        Log.d(TAG, "Characteristic updated: " + chunk);

        // Append the chunk to the StringBuilder
        messageBuilder.append(chunk);

        // Check if the chunk contains a newline character, indicating the end of the message
        if (chunk.contains("\n")) {
            final String completeMessage = messageBuilder.toString().trim();
            messageBuilder.setLength(0); // Clear the StringBuilder for the next message

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txtDeviceMessage.setText(completeMessage);
                }
            });
        }
    }

    // This method displays a toast message on the screen
    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DeviceDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}