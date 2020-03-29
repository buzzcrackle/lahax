package com.example.lahacks;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;


/**
 * Created by pablorojas on 17/4/18.
 */

public class LocationService extends Service implements  GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    protected GoogleApiClient googleApiClient;
    protected BluetoothAdapter mBluetoothAdapter;
    protected BroadcastReceiver mReceiver;

    protected LocationRequest locationRequest;

    protected Location currentLocation;
    private Timestamp timeCurrentLocation;
    protected FirebaseFirestore db;
    private String androidId;
    private int deviceCount;


    @SuppressLint("HardwareIds")
    @Override
    public void onCreate() {
        super.onCreate();

        timeCurrentLocation = null;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        db = FirebaseFirestore.getInstance();
        androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        buildGoogleApiClient();

        googleApiClient.connect();

        if (googleApiClient.isConnected()) {
            startLocationUpdates();
        }

        return START_STICKY;

    }

    protected void startLocationUpdates() {
        try {

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient, locationRequest, this);

        } catch (SecurityException ex) {


        }
    }

    protected synchronized void buildGoogleApiClient() {

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }


    protected void createLocationRequest() {
        locationRequest = new LocationRequest();

        locationRequest.setInterval(5000);

        locationRequest.setFastestInterval(1000);

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void updateUI() {

        if (null != currentLocation) {

            StringBuilder locationData = new StringBuilder();
            locationData
                    .append("Latitude: " + currentLocation.getLatitude())
                    .append("\n")
                    .append("Longitude: " + currentLocation.getLongitude())
                    .append("\n");

            final ArrayList<String> arrList = new ArrayList<>();

            IntentFilter filter = new IntentFilter();

            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            registerReceiver(mReceiver, filter);
            mBluetoothAdapter.startDiscovery();
            mReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();

                    if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                        //discovery starts, we can show progress dialog or perform other tasks
                        deviceCount = 0;
                    } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                        //discovery finishes, dismis progress dialog
                        Map<String, Object> doc = new HashMap<>();
                        doc.put("timestamp", timeCurrentLocation);
                        doc.put("geopoint", new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude()));
                        doc.put("bt_user_count", deviceCount);

                        // Add a new document with a generated ID
                        db.collection(androidId)
                                .add(doc)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(TAG, "Error adding document", e);
                                    }
                                });
                    } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        //bluetooth device found
                        BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        deviceCount++;
                    }
                }
            };

            sendLocationBroadcast(locationData.toString());

        } else {

        }
    }

    private void sendLocationBroadcast(String locationData) {

        Intent locationIntent = new Intent();
        locationIntent.setAction("LOCATION_ACTION");
        locationIntent.putExtra("LOCATION_DATA", locationData);

        LocalBroadcastManager.getInstance(this).sendBroadcast(locationIntent);

    }


    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }


    @Override
    public void onDestroy() {

        stopLocationUpdates();

        googleApiClient.disconnect();

        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle connectionHint) throws SecurityException {
        Log.i(TAG, "Connected to GoogleApiClient");

        if (currentLocation == null) {
            currentLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            timeCurrentLocation = new Timestamp(new Date());
            updateUI();
        }

        startLocationUpdates();

    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        timeCurrentLocation = new Timestamp(new Date());
        updateUI();

    }

    @Override
    public void onConnectionSuspended(int cause) {

        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}