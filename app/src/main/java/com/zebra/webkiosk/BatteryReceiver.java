package com.zebra.webkiosk;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;


public class BatteryReceiver extends BroadcastReceiver {

    BatteryListener mBatteryEvent;
    Context mContext;

    public BatteryReceiver(Activity activity){
        mContext = activity.getApplicationContext();

        try {
            mBatteryEvent = (BatteryListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement NetworkChangeListener");
        }
        registerReceiver(activity);
    }

    public void registerReceiver(Activity activity) {
      /*  mContext = activity.getApplicationContext();

        try {
            mBatteryEvent = (BatteryListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement NetworkChangeListener");
        }*/


        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.BATTERY_STATS)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.BATTERY_STATS)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.BATTERY_STATS},
                        1001);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
        }

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
      //  filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        mContext.registerReceiver(this, filter);

        //getActivity().registerReceiver(this.batteryInfoReceiver, new IntentFilter();
    }

    public void unregisterReceiver() {
        mContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
     //   mNetworkChange.onNetworkChange(isConnected());
        mBatteryEvent.onBatteryEvent(intent);


    }



    // Container Activity must implement this interface
    public interface BatteryListener {
        public void onBatteryEvent(Intent intent);
    }

}
