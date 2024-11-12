// Entertainment Event Educational Point Of Interest System
package com.example.cv.eeepois;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    TextView txtLat;
    TextView txtLon;
    TextView txtAlt;
    EditText txtURL;
    ListView lvNearby;
    RadarView rvRadar;

    ArrayList<String> list;
    ArrayAdapter adapter;

    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;

    GNSS gnssData = new GNSS();
    HashMap<String, Location> hmLoc = new HashMap<>();

    public final static ParcelUuid UUID_SERVICE_DATA = new ParcelUuid(UUID.fromString("0000FEAA-0000-1000-8000-00805F9B34FB"));
    public static final String ipPref = "website";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeBluetooth();
        checkPermissions();
        setupUIComponents();
        setupListView();
        startLocationUpdates();
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

    private void checkPermission(final String permission, String title, String message) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(title);
            builder.setMessage(message);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{permission}, 1);
                }
            });
            builder.show();
        }
    }

    private void setupUIComponents() {
        rvRadar = findViewById(R.id.radar);
        txtLat = findViewById(R.id.txtLat);
        txtLon = findViewById(R.id.txtLon);
        txtAlt = findViewById(R.id.txtAlt);
        txtURL = findViewById(R.id.txtURL);
        lvNearby = findViewById(R.id.lvNearby);

        rvRadar.setup(this, hmLoc, txtLat, txtLon, txtAlt);
        onClickLoad(null);
    }

    private void setupListView() {
        list = new ArrayList<>();
        String[] values = new String[]{"Clear"};
        for (String value : values) {
            list.add(value);
        }

        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        lvNearby.setAdapter(adapter);

        lvNearby.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        String MAC = item.substring(0, 17);
                        gnssData.postToDB(txtURL.getText().toString(), MAC);
                    }
                });
            }
        });
    }

    private void startLocationUpdates() {
        gnssData.setup(this, txtLat, txtLon, txtAlt);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                gnssData.startLocationUpdates();
            }
        });
    }

    private void startBluetoothScan() {
        if (btAdapter.isDiscovering()) {
            Toast.makeText(getApplicationContext(), "Already scanning, re-launch after some time ...", Toast.LENGTH_LONG).show();
        } else {
            btScanner = btAdapter.getBluetoothLeScanner();
            btScanner.startScan(new ScanCallback() {
                @Override
                public void onScanFailed(int errorCode) {
                    Log.e("EEEPOIS", "onScanFailed() : Error = " + errorCode);
                }

                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    handleScanResult(result);
                }
            });

            rvRadar.scan();
        }
    }

    private void handleScanResult(ScanResult result) {
        ScanRecord scanRecord = result.getScanRecord();
        int rssi = result.getRssi();
        BluetoothDevice device = result.getDevice();
        String sPrefix = device.getAddress() + ": " + device.getName() + ", R: ";

        String sBytes = "";
        byte[] byteData = scanRecord.getServiceData(UUID_SERVICE_DATA);
        if (byteData != null) {
            final StringBuilder stringBuilder = new StringBuilder(byteData.length);
            for (byte byteChar : byteData) {
                stringBuilder.append(String.format("%c", byteChar));
            }
            sBytes = stringBuilder.toString();
        }

        String sText = sPrefix + rssi + ", D: " + sBytes;

        int pos = 0;
        boolean found = false;
        for (int i = 0; (i < list.size()) && !found; i++) {
            String sEle = list.get(i);
            found = sEle.startsWith(sPrefix);
            if (found) {
                pos = i;
            }
        }

        if (!found) {
            list.add(sText);
            final String sMAC = device.getAddress();
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    Location loc = gnssData.getCoordinatesFromDB(txtURL.getText().toString(), sMAC);
                    if ((loc.getLatitude() > 0) && (loc.getLongitude() > 0)) {
                        hmLoc.put(sMAC, loc);
                    }
                }
            });
        } else {
            list.add(pos, sText);
            list.remove(pos + 1);
        }

        adapter.notifyDataSetChanged();
    }

    void onClickAbout(View view) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Developer contact information");
        builder.setMessage("Thank you for using the Entertainment Event Education Point Of Interest System!\n\nFor support please email\nyashvarde@gmail.com\ncyusa@justusmtl.com\n\nOur website is at\nhttp://www.vizability-system.com");
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
            }
        });
        builder.show();
    }

    void onClickSave(View view) {
        writeToPreference(ipPref, txtURL.getText().toString());
    }

    void onClickLoad(View view) {
        String sText = getPreferenceValue(ipPref);
        if (!sText.equals("0")) {
            txtURL.setText(sText);
        }
    }

    public String getPreferenceValue(String prefName) {
        SharedPreferences sp = getSharedPreferences(prefName, 0);
        return sp.getString("EEEPOIS", "0");
    }

    public void writeToPreference(String prefName, String prefValue) {
        SharedPreferences.Editor editor = getSharedPreferences(prefName, 0).edit();
        editor.putString("EEEPOIS", prefValue);
        editor.commit();
    }
}