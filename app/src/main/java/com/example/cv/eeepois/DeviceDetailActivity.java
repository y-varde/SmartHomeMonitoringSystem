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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

import android.util.Log;

public class DeviceDetailActivity extends AppCompatActivity {

    private static final String TAG = "DeviceDetailActivity";
    private BluetoothGatt bluetoothGatt;
    private TextView txtDeviceName;
    private TextView txtDeviceAddress;
    private TextView txtTemperature;
    private TextView txtDeviceMessage;
    private Button btnToggleTempUnit;

    private static final UUID SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private StringBuilder messageBuilder = new StringBuilder();
    private boolean showInFahrenheit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);

        txtDeviceName = findViewById(R.id.txtDeviceName);
        txtDeviceAddress = findViewById(R.id.txtDeviceAddress);
        txtTemperature = findViewById(R.id.txtTemperature);
        txtDeviceMessage = findViewById(R.id.txtDeviceMessage);
        btnToggleTempUnit = findViewById(R.id.btnToggleTempUnit);

        String deviceName = getIntent().getStringExtra("deviceName");
        String deviceAddress = getIntent().getStringExtra("deviceAddress");

        txtDeviceName.setText(deviceName);
        txtDeviceAddress.setText(deviceAddress);

        btnToggleTempUnit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleTemperatureUnit();
            }
        });

        connectToDevice(deviceAddress);
    }

    private void connectToDevice(String MAC) {
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(MAC);
        if (device == null) {
            showToast("Remote Device Not Found");
            return;
        }

        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
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

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                updateMessage(characteristic);
            } else {
                Log.w(TAG, "Characteristic read failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            updateMessage(characteristic);
        }
    };

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

                bluetoothGatt.setCharacteristicNotification(characteristic, true);
            } else {
                Log.w(TAG, "Characteristic not found");
            }
        } else {
            Log.w(TAG, "Service not found");
        }
    }

    private void updateMessage(BluetoothGattCharacteristic characteristic) {
        final String chunk = new String(characteristic.getValue());
        Log.d(TAG, "Characteristic updated: " + chunk);

        messageBuilder.append(chunk);

        if (chunk.contains("\0")) {
            final String completeMessage = messageBuilder.toString().replace("\0", "").trim();
            messageBuilder.setLength(0);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    displayMessage(completeMessage);
                }
            });
        }
    }

    private void displayMessage(String message) {
        String[] lines = message.split("\n");
        if (lines.length > 0) {
            String temperatureLine = lines[0];
            if (showInFahrenheit) {
                temperatureLine = convertToFahrenheit(temperatureLine);
            }
            txtTemperature.setText(temperatureLine);
        }
        StringBuilder otherLines = new StringBuilder();
        for (int i = 1; i < lines.length; i++) {
            otherLines.append(lines[i]).append("\n");
        }
        txtDeviceMessage.setText(otherLines.toString().trim());
    }

    private String convertToFahrenheit(String temperatureLine) {
        if (temperatureLine.startsWith("Temperature: ")) {
            String tempStr = temperatureLine.substring("Temperature: ".length(), temperatureLine.indexOf(" C"));
            try {
                int tempCelsius = Integer.parseInt(tempStr);
                int tempFahrenheit = (int) (tempCelsius * 9.0 / 5.0 + 32);
                return "Temperature: " + tempFahrenheit + " F";
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return temperatureLine;
    }

    private void toggleTemperatureUnit() {
        showInFahrenheit = !showInFahrenheit;
        if (showInFahrenheit) {
            btnToggleTempUnit.setText("Show in Celsius");
        } else {
            btnToggleTempUnit.setText("Show in Fahrenheit");
        }

        String currentTemperature = txtTemperature.getText().toString();
        if (showInFahrenheit) {
            txtTemperature.setText(convertToFahrenheit(currentTemperature));
        } else {
            txtTemperature.setText(convertToCelsius(currentTemperature));
        }
    }

    private String convertToCelsius(String temperatureLine) {
        if (temperatureLine.startsWith("Temperature: ")) {
            String tempStr = temperatureLine.substring("Temperature: ".length(), temperatureLine.indexOf(" F"));
            try {
                int tempFahrenheit = Integer.parseInt(tempStr);
                int tempCelsius = (int) ((tempFahrenheit - 32) * 5.0 / 9.0);
                return "Temperature: " + tempCelsius + " C";
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return temperatureLine;
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