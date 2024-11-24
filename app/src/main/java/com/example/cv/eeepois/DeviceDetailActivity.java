package com.example.cv.eeepois;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

import android.util.Log;

public class DeviceDetailActivity extends AppCompatActivity {

    private static final String TAG = "DeviceDetailActivity";
    private static final String PREFS_NAME = "DeviceDetailPrefs";
    private static final String KEY_ARMED_STATE = "armedState";
    private static final String KEY_TEMP_MIN_THRESHOLD = "tempMinThreshold";
    private static final String KEY_TEMP_MAX_THRESHOLD = "tempMaxThreshold";
    private static final String KEY_HUMIDITY_THRESHOLD = "humidityThreshold";
    private static final String KEY_GAS_MIN_THRESHOLD = "gasMinThreshold";
    private static final String KEY_GAS_MAX_THRESHOLD = "gasMaxThreshold";
    private static final int REQUEST_PERMISSIONS = 1;
    private BluetoothGatt bluetoothGatt;
    private TextView txtDeviceName;
    private TextView txtDeviceAddress;
    private TextView txtTemperature;
    private TextView txtHumidity;
    private TextView txtGasConcentration;
    private TextView txtSamplingRate;
    private TextView txtDeviceMessage;
    private Button btnToggleTempUnit;
    private Button btnArmSystem;
    private Button btnUpdateSamplingRate;
    private Button btnRefresh;
    private SeekBar seekBarSamplingRate;
    private Spinner modeSpinner;
    private EditText edtTempMinThreshold;
    private EditText edtTempMaxThreshold;
    private EditText edtHumidityThreshold;
    private EditText edtGasMinThreshold;
    private EditText edtGasMaxThreshold;

    private static final UUID SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private StringBuilder messageBuilder = new StringBuilder();
    private boolean showInFahrenheit = false;
    private boolean isArmed = false;
    private int samplingRate = 1; // Default sampling rate in seconds
    private int tempMinThreshold = 0; // Default minimum temperature threshold
    private int tempMaxThreshold = 43; // Default maximum temperature threshold
    private int humidityThreshold = 70; // Default humidity threshold
    private int gasMinThreshold = 0; // Default minimum gas concentration threshold
    private int gasMaxThreshold = 400; // Default maximum gas concentration threshold

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);

        txtDeviceName = findViewById(R.id.txtDeviceName);
        txtDeviceAddress = findViewById(R.id.txtDeviceAddress);
        txtTemperature = findViewById(R.id.txtTemperature);
        txtHumidity = findViewById(R.id.txtHumidity);
        txtGasConcentration = findViewById(R.id.txtGasConcentration);
        txtSamplingRate = findViewById(R.id.txtSamplingRate);
        txtDeviceMessage = findViewById(R.id.txtDeviceMessage);
        btnToggleTempUnit = findViewById(R.id.btnToggleTempUnit);
        btnArmSystem = findViewById(R.id.btnArmSystem);
        btnUpdateSamplingRate = findViewById(R.id.btnUpdateSamplingRate);
        btnRefresh = findViewById(R.id.btnRefresh);
        seekBarSamplingRate = findViewById(R.id.seekBarSamplingRate);
        modeSpinner = findViewById(R.id.mode_spinner);
        edtTempMinThreshold = findViewById(R.id.edtTempMinThreshold);
        edtTempMaxThreshold = findViewById(R.id.edtTempMaxThreshold);
        edtHumidityThreshold = findViewById(R.id.edtHumidityThreshold);
        edtGasMinThreshold = findViewById(R.id.edtGasMinThreshold);
        edtGasMaxThreshold = findViewById(R.id.edtGasMaxThreshold);

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

        btnArmSystem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleArmSystem();
            }
        });

        btnUpdateSamplingRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessageToDevice("S" + samplingRate);
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshActivity();
            }
        });

        seekBarSamplingRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                samplingRate = progress + 1; // Sampling rate in seconds
                txtSamplingRate.setText("Sampling Rate: " + samplingRate + "s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.mode_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSpinner.setAdapter(adapter);

        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedMode = parent.getItemAtPosition(position).toString();
                handleModeChange(selectedMode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Load the saved armed state and thresholds
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isArmed = preferences.getBoolean(KEY_ARMED_STATE, false);
        tempMinThreshold = preferences.getInt(KEY_TEMP_MIN_THRESHOLD, tempMinThreshold);
        tempMaxThreshold = preferences.getInt(KEY_TEMP_MAX_THRESHOLD, tempMaxThreshold);
        humidityThreshold = preferences.getInt(KEY_HUMIDITY_THRESHOLD, humidityThreshold);
        gasMinThreshold = preferences.getInt(KEY_GAS_MIN_THRESHOLD, gasMinThreshold);
        gasMaxThreshold = preferences.getInt(KEY_GAS_MAX_THRESHOLD, gasMaxThreshold);

        // Set the threshold EditText fields with the loaded values
        edtTempMinThreshold.setText(String.valueOf(tempMinThreshold));
        edtTempMaxThreshold.setText(String.valueOf(tempMaxThreshold));
        edtHumidityThreshold.setText(String.valueOf(humidityThreshold));
        edtGasMinThreshold.setText(String.valueOf(gasMinThreshold));
        edtGasMaxThreshold.setText(String.valueOf(gasMaxThreshold));

        updateArmButton();

        // Check and request permissions
        checkPermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Save the thresholds and armed state when the activity is paused
        saveThresholds();
    }
    
    private void saveThresholds() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_TEMP_MIN_THRESHOLD, Integer.parseInt(edtTempMinThreshold.getText().toString()));
        editor.putInt(KEY_TEMP_MAX_THRESHOLD, Integer.parseInt(edtTempMaxThreshold.getText().toString()));
        editor.putInt(KEY_HUMIDITY_THRESHOLD, Integer.parseInt(edtHumidityThreshold.getText().toString()));
        editor.putInt(KEY_GAS_MIN_THRESHOLD, Integer.parseInt(edtGasMinThreshold.getText().toString()));
        editor.putInt(KEY_GAS_MAX_THRESHOLD, Integer.parseInt(edtGasMaxThreshold.getText().toString()));
        editor.putBoolean(KEY_ARMED_STATE, isArmed); // Save the armed state
        editor.apply();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_PERMISSIONS);
        } else {
            // Permissions already granted, proceed with Bluetooth setup
            connectToDevice(getIntent().getStringExtra("deviceAddress"));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, proceed with Bluetooth setup
                connectToDevice(getIntent().getStringExtra("deviceAddress"));
            } else {
                showToast("Permissions required for Bluetooth scanning");
            }
        }
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
            checkThreshold(txtTemperature, temperatureLine, tempMinThreshold, tempMaxThreshold);
        }
        if (lines.length > 1) {
            txtHumidity.setText(lines[1]);
            checkThreshold(txtHumidity, lines[1], humidityThreshold);
        }
        if (lines.length > 2) {
            txtGasConcentration.setText(lines[2]);
            checkThreshold(txtGasConcentration, lines[2], gasMinThreshold, gasMaxThreshold);
        }
        StringBuilder otherLines = new StringBuilder();
        for (int i = 3; i < lines.length; i++) {
            otherLines.append(lines[i]).append("\n");
        }
        txtDeviceMessage.setText(otherLines.toString().trim());
    }

    private void checkThreshold(TextView textView, String value, int minThreshold, int maxThreshold) {
        try {
            int intValue = Integer.parseInt(value.replaceAll("[^0-9]", ""));
            if (intValue < minThreshold || intValue > maxThreshold) {
                textView.setTextColor(Color.RED);
            } else {
                textView.setTextColor(Color.BLACK);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void checkThreshold(TextView textView, String value, int threshold) {
        try {
            int intValue = Integer.parseInt(value.replaceAll("[^0-9]", ""));
            if (intValue > threshold) {
                textView.setTextColor(Color.RED);
            } else {
                textView.setTextColor(Color.BLACK);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
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

    private void handleModeChange(String selectedMode) {
        if ("Admin Mode".equals(selectedMode)) {
            btnArmSystem.setVisibility(View.VISIBLE);
            txtSamplingRate.setVisibility(View.VISIBLE);
            seekBarSamplingRate.setVisibility(View.VISIBLE);
            btnUpdateSamplingRate.setVisibility(View.VISIBLE);
            edtTempMinThreshold.setVisibility(View.VISIBLE);
            edtTempMaxThreshold.setVisibility(View.VISIBLE);
            edtHumidityThreshold.setVisibility(View.VISIBLE);
            edtGasMinThreshold.setVisibility(View.VISIBLE);
            edtGasMaxThreshold.setVisibility(View.VISIBLE);
        } else {
            btnArmSystem.setVisibility(View.GONE);
            txtSamplingRate.setVisibility(View.GONE);
            seekBarSamplingRate.setVisibility(View.GONE);
            btnUpdateSamplingRate.setVisibility(View.GONE);
            edtTempMinThreshold.setVisibility(View.GONE);
            edtTempMaxThreshold.setVisibility(View.GONE);
            edtHumidityThreshold.setVisibility(View.GONE);
            edtGasMinThreshold.setVisibility(View.GONE);
            edtGasMaxThreshold.setVisibility(View.GONE);
        }
    }

    private void toggleArmSystem() {
        isArmed = !isArmed;
        updateArmButton();

        // Send the appropriate message to the Bluetooth device
        if (isArmed) {
            sendMessageToDevice("A");
        } else {
            sendMessageToDevice("D");
        }
    }

    private void updateArmButton() {
        if (isArmed) {
            btnArmSystem.setText("DISARM SYSTEM");
        } else {
            btnArmSystem.setText("ARM SYSTEM");
        }
    }

    private void sendMessageToDevice(String message) {
        if (bluetoothGatt != null) {
            BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                if (characteristic != null) {
                    characteristic.setValue(message);
                    bluetoothGatt.writeCharacteristic(characteristic);
                }
            }
        }
    }

    private void refreshActivity() {
        finish();
        startActivity(getIntent());
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