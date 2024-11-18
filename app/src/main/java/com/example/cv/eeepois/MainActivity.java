package com.example.cv.eeepois;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    ListView lvNearby;
    DeviceListAdapter deviceListAdapter;
    ArrayList<BluetoothDevice> deviceList = new ArrayList<>();

    // This method is called when the activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeBluetooth();
        checkPermissions();
        setupUIComponents();
        startBluetoothScan();
    }

    // This method initializes the Bluetooth adapter
    private void initializeBluetooth() {
        BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 1);
        }
    }

    // This method checks for the necessary permissions
    private void checkPermissions() {
        checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, "This app needs location access", "Please grant location access so this app can detect peripherals.");
        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, "This app needs fine location access", "Please grant location access so this app can detect peripherals.");
        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, "This app needs to write to the phone storage", "Please grant write access so this app can function.");
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, "This app needs to read from the phone storage", "Please grant read access so this app can function.");
    }

    // This method sets up the UI components of the main activity
    private void setupUIComponents() {
        lvNearby = findViewById(R.id.lvNearby);
        deviceListAdapter = new DeviceListAdapter(this, deviceList);
        lvNearby.setAdapter(deviceListAdapter);
    }

    // This method starts the Bluetooth scan (discovery) process
    private void startBluetoothScan() {
        btScanner = btAdapter.getBluetoothLeScanner();
        btScanner.startScan(new ScanCallback() {
            // This method is called when a new device is discovered during the scan
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                //only add DSD TECH bluetooth devices
                if (device.getName() != null && device.getName().contains("DSD TECH") && !deviceList.contains(device)) {
                    deviceList.add(device);
                    deviceListAdapter.notifyDataSetChanged();
                }
            }

            // This method is called when the scan fails
            @Override
            public void onScanFailed(int errorCode) {
                Log.e("MainActivity", "Scan failed with error: " + errorCode);
            }
        });
    }

    // This method checks for a specific permission and requests it if necessary
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

    //This class is used to create a custom list adapter for the ListView
    private class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {
        private Context context;
        private ArrayList<BluetoothDevice> devices;

        // Constructor
        public DeviceListAdapter(Context context, ArrayList<BluetoothDevice> devices) {
            super(context, 0, devices);
            this.context = context;
            this.devices = devices;
        }

        // This method creates the view for each item in the list
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.list_item_device, parent, false);
            }

            final BluetoothDevice device = devices.get(position);
            TextView txtDeviceName = convertView.findViewById(R.id.txtDeviceName);
            Button btnConnect = convertView.findViewById(R.id.btnConnect);

            txtDeviceName.setText(device.getName() + "\n" + device.getAddress());

            // This listener is called when the "Connect" button is clicked
            btnConnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, DeviceDetailActivity.class);
                    intent.putExtra("deviceName", device.getName());
                    intent.putExtra("deviceAddress", device.getAddress());
                    startActivity(intent);
                }
            });

            return convertView;
        }
    }
}