package com.ivanmedic.toplinskamapa;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

public class LocationService extends Service{

    public LocationManager locationManager;
    Intent intent;
    public static final String BROADCAST_ACTION = "LOCATION_UPDATE";
    public locationListener listener;


    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new locationListener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, listener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v("STOP_SERVICE", "DONE");
        locationManager.removeUpdates(listener);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class locationListener implements LocationListener {

        public void onLocationChanged(final Location loc) {
            loc.getLatitude();
            loc.getLongitude();
            intent.putExtra("Latitude", loc.getLatitude());
            intent.putExtra("Longitude", loc.getLongitude());
            sendBroadcast(intent);
        }

        public void onProviderDisabled(String provider) {
            Toast.makeText( getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT ).show();
        }

        public void onProviderEnabled(String provider) {
            Toast.makeText( getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }


        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    }
}
