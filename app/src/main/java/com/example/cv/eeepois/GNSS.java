package com.example.cv.eeepois;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;

import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Date;

import static java.lang.Math.min;

public class GNSS {
    private String curLat = "latitude";
    private String curLon = "longitude";
    private String curAlt = "altitude";

    // bunch of location related apis
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;

    AppCompatActivity mainActivity = null;

    TextView txtLat;
    TextView txtLon;
    TextView txtAlt;

    private static final String TAG = "GNSS";

    // location updates interval - 10sec
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 2000;

    // fastest updates interval - 5 sec
    // location updates will be received if another app is requesting the locations
    // than your app can handle
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = mainActivity;
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    void postToDB(String IP, String MAC)
    {
        final StringBuilder stringBuilder = new StringBuilder(1);
        stringBuilder.append(String.format("http://%s/EEEPOIS/SDL.asp", IP));

        HttpURLConnection client = null;
        try {
            SimpleDateFormat SDF = new SimpleDateFormat("M/d/y H:m:s", Locale.US);
            String curTime = SDF.format(new Date());

            final StringBuilder sb = new StringBuilder(1);
            // sb.append(String.format("CustomerID=%s&Latitude=%s&Longitude=%s", edtCustID.getText().toString(), edtLat.getText().toString(), edtLon.getText().toString()));
            sb.append("MAC=").append(URLEncoder.encode(MAC, "UTF-8"));
            sb.append("&Latitude=").append(URLEncoder.encode(curLat, "UTF-8"));
            sb.append("&Longitude=").append(URLEncoder.encode(curLon, "UTF-8"));
            sb.append("&Altitude=").append(URLEncoder.encode(curAlt, "UTF-8"));
            sb.append("&Description=").append(URLEncoder.encode("not set", "UTF-8"));
            sb.append("&NewBatteryDate=").append(URLEncoder.encode(curTime, "UTF-8"));
            sb.append("&BatteryLife=").append(URLEncoder.encode("24", "UTF-8"));
            sb.append("&Submit=Submit");

            URL url = new URL(stringBuilder.toString());
            client = (HttpURLConnection) url.openConnection();

            client.setRequestMethod("POST");
            client.setRequestProperty("User-Agent", "vizAbility");
            client.setRequestProperty("Accept-Charset", "UTF-8");
            client.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            client.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            client.setRequestProperty("Accept-Language", "en-US");
            client.setRequestProperty("Referer", "http://vizability-system.com/visability/AddInjury.html");
            client.setRequestProperty("Accept-Encoding", "gzip, deflate");
            client.setRequestProperty("Host", "vizability-system.com");
            client.setFixedLengthStreamingMode(sb.toString().getBytes().length);
            client.setUseCaches (false);
            client.setDoOutput(true);
            client.setDoInput(true);

            client.setReadTimeout(10000);
            client.setConnectTimeout(15000);

            client.connect();

            DataOutputStream outputPost = new DataOutputStream(client.getOutputStream());
            outputPost.writeBytes(sb.toString());
            outputPost.flush();
            outputPost.close();

            InputStream in = new BufferedInputStream(client.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            final String StatusString = result.toString();
            showToast(StatusString);
        }
        catch(MalformedURLException error) {
            //Handles an incorrectly entered URL
            showToast(error.getMessage());
        }
        catch(SocketTimeoutException error) {
            //Handles URL access timeout.
            showToast(error.getMessage());
        }
        catch (IOException error) {
            //Handles input and output errors
            showToast(error.getMessage());
        }
        finally {
            if (client != null) // Make sure the connection is not null.
                client.disconnect();
        }
    }

    Location getCoordinatesFromDB(String IP, String MAC)
    {
        Location loc = new Location("");
        loc.setLatitude(0.0);
        loc.setLongitude(0.0);
        loc.setAltitude(0.0);

        final StringBuilder stringBuilder = new StringBuilder(1);
        stringBuilder.append(String.format("http://%s/EEEPOIS/GDL.asp", IP));

        HttpURLConnection client = null;
        try {
            final StringBuilder sb = new StringBuilder(1);
            // sb.append(String.format("CustomerID=%s&Latitude=%s&Longitude=%s", edtCustID.getText().toString(), edtLat.getText().toString(), edtLon.getText().toString()));
            sb.append("MAC=").append(URLEncoder.encode(MAC, "UTF-8"));

            URL url = new URL(stringBuilder.toString());
            client = (HttpURLConnection) url.openConnection();

            client.setRequestMethod("POST");
            client.setRequestProperty("User-Agent", "vizAbility");
            client.setRequestProperty("Accept-Charset", "UTF-8");
            client.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            client.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            client.setRequestProperty("Accept-Language", "en-US");
            client.setRequestProperty("Referer", "http://vizability-system.com/visability/AddInjury.html");
            client.setRequestProperty("Accept-Encoding", "gzip, deflate");
            client.setRequestProperty("Host", "vizability-system.com");
            client.setFixedLengthStreamingMode(sb.toString().getBytes().length);
            client.setUseCaches (false);
            client.setDoOutput(true);
            client.setDoInput(true);

            client.setReadTimeout(10000);
            client.setConnectTimeout(15000);

            client.connect();

            DataOutputStream outputPost = new DataOutputStream(client.getOutputStream());
            outputPost.writeBytes(sb.toString());
            outputPost.flush();
            outputPost.close();

            InputStream in = new BufferedInputStream(client.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder result = new StringBuilder();
            String line;

            line = reader.readLine();
            if (line != null) {
                int wpcount = new Integer(line).intValue();

                if (wpcount == 1) {
                    line = reader.readLine();
                    if (line != null) {
                        double lat = new Double(line).doubleValue();
                        loc.setLatitude(lat);
                        result.append(line);
                    }

                    line = reader.readLine();
                    if (line != null) {
                        double lon = new Double(line).doubleValue();
                        loc.setLongitude(lon);
                        result.append(line);
                    }

                    line = reader.readLine();
                    if (line != null) {
                        double alt = new Double(line).doubleValue();
                        loc.setAltitude(alt);
                        result.append(line);
                    }
                }
            }

            showToast(result.toString());
        }
        catch(MalformedURLException error) {
            //Handles an incorrectly entered URL
            showToast(error.getMessage());
        }
        catch(SocketTimeoutException error) {
            //Handles URL access timeout.
            showToast(error.getMessage());
        }
        catch (IOException error) {
            //Handles input and output errors
            showToast(error.getMessage());
        }
        finally {
            if (client != null) // Make sure the connection is not null.
                client.disconnect();
        }

        return loc;
    }

    private void init() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mainActivity);
        mSettingsClient = LocationServices.getSettingsClient(mainActivity);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            // location is received
            mCurrentLocation = locationResult.getLastLocation();
            updateLocationUI();
            }
        };

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    /**
     * Update the UI displaying the location data
     * and toggling the buttons
     */
    private void updateLocationUI() {
        if (mCurrentLocation != null) {
            mainActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Double cLat = Double.valueOf(mCurrentLocation.getLatitude());
                    curLat = cLat.toString();
                    txtLat.setText("Y: " + curLat.toString());

                    Double cLon = Double.valueOf(mCurrentLocation.getLongitude());
                    curLon = cLon.toString();
                    txtLon.setText("X: " + curLon);

                    Double cAlt = Double.valueOf(mCurrentLocation.getAltitude());
                    curAlt = cAlt.toString();
                    txtAlt.setText("Z: " + curAlt.substring(0, min(curAlt.length(), 6)));
                }
            });
        }
    }

    /**
     * Starting location updates
     * Check whether location settings are satisfied and then
     * location updates will be requested
     */
    public void startLocationUpdates() {
        mSettingsClient
            .checkLocationSettings(mLocationSettingsRequest)
            .addOnSuccessListener(mainActivity, new OnSuccessListener<LocationSettingsResponse>() {
                @SuppressLint("MissingPermission")
                @Override
                public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                Log.i(TAG, "All location settings are satisfied.");

                showToast("Started location updates!");

                //noinspection MissingPermission
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());

                updateLocationUI();
                }
            })
            .addOnFailureListener(mainActivity, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                "location settings ");
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        String errorMessage = "Location settings are inadequate, and cannot be " +
                                "fixed here. Fix in Settings.";
                        Log.e(TAG, errorMessage);

                        showToast(errorMessage);
                }

                updateLocationUI();
                }
            });
    }

    public void stopLocationUpdates() {
        // Removing location updates
        mFusedLocationClient
            .removeLocationUpdates(mLocationCallback)
            .addOnCompleteListener(mainActivity, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                showToast("Location updates stopped!");
                }
            });
    }

    public void setup(AppCompatActivity activity, TextView T1, TextView T2, TextView T3) {
        mainActivity = activity;
        txtLat = T1;
        txtLon = T2;
        txtAlt = T3;

        init();
    }
}
