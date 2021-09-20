package com.zebra.webkiosk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class RemoteIntentConfig extends BroadcastReceiver {
    public static final String TAG = "RemoteIntentConfig";
    Context mContext;
    RemoteIntentConfig.RemoteConfigListener mRemoteConfigEvent;

    public static String APP_PACKAGE_NAME = "com.zebra.webkiosk";

    final static String configRemoteUrl = ".REMOTE_CONFIG";

    public RemoteIntentConfig(Activity activity)  {
        mContext = activity.getApplicationContext();
        APP_PACKAGE_NAME = activity.getApplication().getPackageName();

        try {
            mRemoteConfigEvent = (RemoteIntentConfig.RemoteConfigListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(this.toString()
                    + " must implement RemoteConfigListener");
        }
        registerReceiver();
    }

    public void unregisterReceiver() {
        mContext.unregisterReceiver(this);
    }

    public void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        //com.zebra.webkiosk.REMOTE_CONFIG
        //com.zebra.webkiosk.REMOTE_CONFIG

        filter.addAction(APP_PACKAGE_NAME+configRemoteUrl);
        Log.d(TAG,"Register intent filter: "+APP_PACKAGE_NAME+configRemoteUrl);
      //  filter.addCategory("android.intent.category.DEFAULT");
        mContext.registerReceiver(this, filter);
    }

    // Container Activity must implement this interface
    public interface RemoteConfigListener {
        public void onRemoteConfigEvent(Intent intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Action: " + action);

        if (action.equals(RemoteIntentConfig.APP_PACKAGE_NAME+configRemoteUrl) )
            mRemoteConfigEvent.onRemoteConfigEvent(intent);

    }
}
