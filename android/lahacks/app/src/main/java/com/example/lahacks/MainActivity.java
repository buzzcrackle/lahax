package com.example.lahacks;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private final int LOC_FINE_REQUEST = 6969;
    private final int BLU_REQUEST = 420420;

    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOC_FINE_REQUEST);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }



        sp = getSharedPreferences("usermac", MODE_PRIVATE);


        Intent serviceIntent = new Intent(this, LocationService.class);
        startService(serviceIntent);
        Log.i("WHAT", "started service?");

        final TextView tv = findViewById(R.id.textview);

        final String androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String str = intent.getStringExtra("LOCATION_DATA");
                        tv.setText(str + androidId);
                    }
                }, new IntentFilter("LOCATION_ACTION")
        );


    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case LOC_FINE_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            case BLU_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    String mac = getBluetoothMac(this);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("address", mac);
                    editor.apply();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private String getBluetoothMac(final Context context) {

        String result = null;
        if (context.checkCallingOrSelfPermission(Manifest.permission.BLUETOOTH)
                == PackageManager.PERMISSION_GRANTED) {
//            Log.i("mac", "1");
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                // Hardware ID are restricted in Android 6+
//                // https://developer.android.com/about/versions/marshmallow/android-6.0-changes.html#behavior-hardware-id
//                // Getting bluetooth mac via reflection for devices with Android 6+
//                Log.i("mac", "2: " + Build.VERSION.SDK_INT);
//                result = android.provider.Settings.Secure.getString(context.getContentResolver(),
//                        "bluetooth_address");
//            } else {
            Log.i("mac", "3");
            BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
            result = bta != null ? bta.getAddress() : "";
//            }
        }
        return result;
    }
}
