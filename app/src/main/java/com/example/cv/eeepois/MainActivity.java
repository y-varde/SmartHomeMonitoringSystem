package com.example.cv.eeepois;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import java.util.ArrayList;
import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    ListView lvNearby;
    ArrayAdapter<String> deviceListAdapter;
    ArrayList<BluetoothDevice> deviceList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeBluetooth();
        checkPermissions();
        setupUIComponents();
        startBluetoothScan();
    }

    private void initializeBluetooth() {
        BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 1);
        }
    }

    private void checkPermissions() {
        checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, "This app needs location access", "Please grant location access so this app can detect peripherals.");
        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, "This app needs fine location access", "Please grant location access so this app can detect peripherals.");
        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, "This app needs to write to the phone storage", "Please grant write access so this app can function.");
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, "This app needs to read from the phone storage", "Please grant read access so this app can function.");
    }

    private void setupUIComponents() {
        lvNearby = findViewById(R.id.lvNearby);
        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        lvNearby.setAdapter(deviceListAdapter);

        lvNearby.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice selectedDevice = deviceList.get(position);
                Intent intent = new Intent(MainActivity.this, DeviceDetailActivity.class);
                intent.putExtra("deviceName", selectedDevice.getName());
                intent.putExtra("deviceAddress", selectedDevice.getAddress());
                startActivity(intent);
            }
        });
    }

    private void startBluetoothScan() {
        btScanner = btAdapter.getBluetoothLeScanner();
        btScanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                if (!deviceList.contains(device)) {
                    deviceList.add(device);
                    deviceListAdapter.add(device.getName() + "\n" + device.getAddress());
                    deviceListAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("EEEPOIS", "Scan failed with error: " + errorCode);
            }
        });
    }

    private void checkPermission(final String permission, String rationaleTitle, String rationaleMessage) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                new AlertDialog.Builder(this)
                        .setTitle(rationaleTitle)
                        .setMessage(rationaleMessage)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, 1);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, 1);
            }
        }
    }
}