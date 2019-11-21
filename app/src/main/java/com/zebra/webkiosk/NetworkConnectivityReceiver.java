package com.zebra.webkiosk;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class NetworkConnectivityReceiver extends BroadcastReceiver {

    NetworkChangeListener mNetworkChange;
    NetworkEventListener mNetworkEvent;
    Context mContext;

    public NetworkConnectivityReceiver(Activity activity){

        mContext = activity.getApplicationContext();

        try {
            mNetworkChange = (NetworkChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement NetworkChangeListener");
        }
        try {
            mNetworkEvent = (NetworkEventListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement NetworkEventListener");
        }

        registerReceiver(activity);
    }

    public void registerReceiver(Activity activity) {



        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.CHANGE_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.CHANGE_NETWORK_STATE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.CHANGE_NETWORK_STATE},
                        1001);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }


        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_NETWORK_STATE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.ACCESS_NETWORK_STATE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_NETWORK_STATE},
                        1001);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        mContext.registerReceiver(this, filter);


    }

    public void unregisterReceiver() {
        mContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {


        mNetworkEvent.onNetworkEvent(fetchWiFiState());

        mNetworkChange.onNetworkChange(isConnected());
    }

    public String fetchWiFiState()
    {

        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        int numberOfLevels = 5;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), numberOfLevels);


        int rssi = wifiInfo.getRssi();
        String signal = "???";
        if (rssi >= -50) {
            signal = "Excellent";
        } else if (rssi < -50 && rssi >= -60) {
            signal = "Good";
        } else if (rssi < -60 && rssi >= -70) {
            signal = "Fair";
        } else if (rssi < -70 && rssi >= -100) {
            signal = "Weak";
        } else {
            signal = "no signal";
        }

        String json = "{rssi:"+wifiInfo.getRssi()+", ip:"+wifiInfo.getIpAddress()+", level:"+level+",ssid:\""+wifiInfo.getSSID()+"\",signal:\""+signal+"\",linkspeed:"+wifiInfo.getLinkSpeed()+", clientMAC:\""+wifiInfo.getMacAddress()+"\"}";

        return json;
    }

    public Boolean isConnected() {
           ConnectivityManager cm = (ConnectivityManager)
                    mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    // Container Activity must implement this interface
    public interface NetworkChangeListener {
        public void onNetworkChange(Boolean connected);
    }

    public interface NetworkEventListener {
        public void onNetworkEvent(String json);
    }

}
