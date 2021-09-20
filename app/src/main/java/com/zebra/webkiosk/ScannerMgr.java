package com.zebra.webkiosk;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Set;

public class ScannerMgr extends BroadcastReceiver {

    public static final String TAG = "ScannerMgr";
    public static final String APP_PACKAGE_NAME = "com.zebra.webkiosk";
    public static final String PROFILENAME = "WEBKIOSK";

    public boolean APPEND_END_CHAR = false;

    private static final long BEAM_TIMEOUT = 2000;
    protected Boolean barcodeScanned = false;
    private boolean barcodeScannedStarted = false;
    private long scanTime;

    public static final String enumeratedList = "com.symbol.datawedge.api.ACTION_ENUMERATEDSCANNERLIST";

    public static final String KEY_ENUMERATEDSCANNERLIST = "DWAPI_KEY_ENUMERATEDSCANNERLIST";
    public static final String NOTIFICATION = "com.symbol.datawedge.api.NOTIFICATION";
    public static final String NOTIFICATION_ACTION = "com.symbol.datawedge.api.NOTIFICATION_ACTION";
    public static final String NOTIFICATION_TYPE_SCANNER_STATUS = "SCANNER_STATUS";
    public static final String NOTIFICATION_TYPE_PROFILE_SWITCH = "PROFILE_SWITCH";
    public static final String NOTIFICATION_TYPE_CONFIGURATION_UPDATE = "CONFIGURATION_UPDATE";



    public static final String DATA_STRING_TAG = "com.symbol.datawedge.data_string";
    public static final String LABEL_TYPE = "com.symbol.datawedge.label_type";
    public static final String SOURCE_TAG = "com.symbol.datawedge.source";
    public static final String DECODE_DATA_TAG = "com.symbol.datawedge.decode_data";

    // http://techdocs.zebra.com/datawedge/6-5/guide/api/registerfornotification/

    DatawedgeListener mDatawedgeEvent;

    Context mContext;


    public ScannerMgr(Activity activity)  {
        mContext = activity.getApplicationContext();
        try {
            mDatawedgeEvent = (DatawedgeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(this.toString()
                    + " must implement DatawedgeListener");
        }

        registerReceiver();
    }

    public void registerReceiver(BroadcastReceiver broadcastReceiver){
        IntentFilter filter = new IntentFilter();
        filter.addAction(enumeratedList);
        filter.addAction(APP_PACKAGE_NAME);
        filter.addAction(NOTIFICATION_ACTION); // SCANNER_STATUS
        filter.addCategory("android.intent.category.DEFAULT");
        mContext.registerReceiver(broadcastReceiver, filter);
    }

    public void unregisterReceiver(BroadcastReceiver broadcastReceiver) {
        mContext.unregisterReceiver(broadcastReceiver);
    }


    public void unregisterReceiver() {
        mContext.unregisterReceiver(this);
    }

    public void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(enumeratedList);
        filter.addAction(APP_PACKAGE_NAME);
        filter.addAction(NOTIFICATION_ACTION); // SCANNER_STATUS
        filter.addCategory("android.intent.category.DEFAULT");
        mContext.registerReceiver(this, filter);
    }


    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        Log.d(TAG, "WEBKIOSK Action: " + action);

        if (action.equals(ScannerMgr.APP_PACKAGE_NAME) ) {
            mDatawedgeEvent.onDatawedgeEvent(intent);
        } else if (action.equals(ScannerMgr.NOTIFICATION_ACTION)) {
            if (intent.hasExtra(NOTIFICATION)) {
                Bundle b = intent.getBundleExtra(NOTIFICATION);
                String NOTIFICATION_TYPE = b.getString("NOTIFICATION_TYPE");
                if (NOTIFICATION_TYPE != null) {
                    switch (NOTIFICATION_TYPE) {
                        case ScannerMgr.NOTIFICATION_TYPE_SCANNER_STATUS:

                            Log.d(TAG, "SCANNER_STATUS: status: " + b.getString("STATUS") + ", profileName: " + b.getString("PROFILE_NAME"));
                            String scanner_status = b.getString("STATUS");
                            if (scanner_status.equalsIgnoreCase("WAITING")) {
                                // check if barcode scan was started and timed out
                                if (!barcodeScanned && barcodeScannedStarted && (System.currentTimeMillis() - scanTime >= BEAM_TIMEOUT)) {
                                    //Toast.makeText(getApplicationContext(), "SCAN TIMEOUT", Toast.LENGTH_SHORT).show();
                                    //   mWebView.evaluateJavascript("javascript:onScan('|*|');",null);

                                }
                                if(barcodeScannedStarted && APPEND_END_CHAR) {
                                    Intent i = new Intent();
                                    i.putExtra(ScannerMgr.DATA_STRING_TAG,"|");

                                  //  String data = i.getStringExtra(ScannerMgr.DATA_STRING_TAG);
                                    //   mWebView.evaluateJavascript("javascript:onScan('|');",null);
                                    mDatawedgeEvent.onDatawedgeEvent(i);
                                }
                                barcodeScannedStarted = false;
                            }
                            if (scanner_status.equalsIgnoreCase("SCANNING")) {
                                barcodeScanned = false;
                                barcodeScannedStarted = true;
                                scanTime = System.currentTimeMillis();

                            }
                            break;

                        case ScannerMgr.NOTIFICATION_TYPE_PROFILE_SWITCH:
                            Log.d(TAG, "PROFILE_SWITCH: profileName: " + b.getString("PROFILE_NAME") + ", profileEnabled: " + b.getBoolean("PROFILE_ENABLED"));
                            break;

                        case ScannerMgr.NOTIFICATION_TYPE_CONFIGURATION_UPDATE:
                            break;
                    }
                }
            }
        }


    }


    public void getConfig(){
        Bundle bMain = new Bundle();
        bMain.putString("PROFILE_NAME", PROFILENAME);
        Bundle bConfig = new Bundle();
        ArrayList<Bundle> pluginName = new ArrayList<>();

        Bundle pluginInternal = new Bundle();
        pluginInternal.putString("PLUGIN_NAME", "BARCODE");//can put a list "ADF,BDF"
        pluginInternal.putString("OUTPUT_PLUGIN_NAME","BARCODE");
        pluginName.add(pluginInternal);
        bConfig.putParcelableArrayList("PROCESS_PLUGIN_NAME", pluginName);
        bMain.putBundle("PLUGIN_CONFIG", bConfig);

        Intent i = new Intent();
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.GET_CONFIG", bMain);
        mContext.sendBroadcast(i);
    }

    public void createScannerProfile() {

        Bundle bMain = new Bundle();
        bMain.putString("PROFILE_NAME", PROFILENAME);
        bMain.putString("PROFILE_ENABLED", "true");
        bMain.putString("CONFIG_MODE", "CREATE_IF_NOT_EXIST");
        Bundle bConfig = new Bundle();
        bConfig.putString("PLUGIN_NAME", "INTENT");
        // bConfig.putString("RESET_CONFIG","true");
        Bundle bParams = new Bundle();
        bParams.putString("intent_output_enabled", "true");
        bParams.putString("intent_action", APP_PACKAGE_NAME);
        bParams.putString("intent_category", "android.intent.category.DEFAULT");
        bParams.putString("intent_delivery", "2");
        bConfig.putBundle("PARAM_LIST", bParams);
        //PUT bConfig into bMain
        bMain.putBundle("PLUGIN_CONFIG", bConfig);
        Intent i = new Intent();
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.SET_CONFIG", bMain);
        // i.putExtra("com.symbol.datawedge.api.GET_VERSION_INFO", "");

        mContext.sendBroadcast(i);

        bMain.putString("PROFILE_NAME", PROFILENAME);
        bMain.putString("CONFIG_MODE", "UPDATE");
        bConfig.putString("PLUGIN_NAME", "BARCODE");
        bParams.putString("scanner_selection","auto");   //!important if decoders are being set etc
        bParams.putString("scanner_input_enabled","true");
        bParams.putString("scanning_mode","3");
        bParams.putString("multi_barcode_count","6");
        bParams.putString("instant_reporting_enable","true");

        bParams.putString("aim_type", "0");
       // bParams.putString("decoder_microqr", "true");
       // bParams.putString("digimarc_decoding", "false");
        bConfig.putBundle("PARAM_LIST", bParams);
        bMain.putBundle("PLUGIN_CONFIG", bConfig);
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.SET_CONFIG", bMain);
        mContext.sendBroadcast(i);


        // Disable keystroke
        bMain.putString("PROFILE_NAME", PROFILENAME);
        bMain.putString("CONFIG_MODE", "UPDATE");
        bConfig.putString("PLUGIN_NAME", "KEYSTROKE");
        bParams.putString("keystroke_output_enabled","false");
        bConfig.putBundle("PARAM_LIST", bParams);


        Bundle bundleApp = new Bundle();
        bundleApp.putString("PACKAGE_NAME",APP_PACKAGE_NAME);
        bundleApp.putStringArray("ACTIVITY_LIST", new String[]{"*"});

// NEXT APP_LIST BUNDLE(S) INTO THE MAIN BUNDLE
        bMain.putParcelableArray("APP_LIST", new Bundle[]{
                bundleApp
        });

        //PUT bConfig into bMain
        bMain.putBundle("PLUGIN_CONFIG", bConfig);
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.SET_CONFIG", bMain);
        mContext.sendBroadcast(i);

        // Register for notifications - SCANNER_STATUS
        Bundle b = new Bundle();
        b.putString("com.symbol.datawedge.api.APPLICATION_NAME", APP_PACKAGE_NAME);
        b.putString("com.symbol.datawedge.api.NOTIFICATION_TYPE", "SCANNER_STATUS");
        i = new Intent();
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION", b);//(1)
        mContext.sendBroadcast(i);

        APPEND_END_CHAR = true;
    }

    public void createScannerProfileClean() {

        Bundle bMain = new Bundle();
        bMain.putString("PROFILE_NAME", PROFILENAME);
        bMain.putString("PROFILE_ENABLED", "true");
        bMain.putString("CONFIG_MODE", "CREATE_IF_NOT_EXIST");
        Bundle bConfig = new Bundle();
        bConfig.putString("PLUGIN_NAME", "INTENT");
        // bConfig.putString("RESET_CONFIG","true");
        Bundle bParams = new Bundle();
        bParams.putString("intent_output_enabled", "true");
        bParams.putString("intent_action", APP_PACKAGE_NAME);
        bParams.putString("intent_category", "android.intent.category.DEFAULT");
        bParams.putString("intent_delivery", "2");
        bConfig.putBundle("PARAM_LIST", bParams);
        bMain.putBundle("PLUGIN_CONFIG", bConfig);

        Intent i = new Intent();
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.SET_CONFIG", bMain);
        mContext.sendBroadcast(i);


        bMain.putString("PROFILE_NAME", PROFILENAME);
        bMain.putString("CONFIG_MODE", "UPDATE");
        bConfig.putString("PLUGIN_NAME", "BARCODE");
        bParams.putString("scanner_selection","auto");   //!important if decoders are being set etc
        bParams.putString("scanner_input_enabled","true");
        bParams.putString("scanning_mode","1");

        bConfig.putBundle("PARAM_LIST", bParams);
        bMain.putBundle("PLUGIN_CONFIG", bConfig);
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.SET_CONFIG", bMain);
        mContext.sendBroadcast(i);

        // Disable keystroke
        bMain.putString("PROFILE_NAME", PROFILENAME);
        bMain.putString("CONFIG_MODE", "UPDATE");
        bConfig.putString("PLUGIN_NAME", "KEYSTROKE");
        bParams.putString("keystroke_output_enabled","false");
        bConfig.putBundle("PARAM_LIST", bParams);



        Bundle bundleApp = new Bundle();
        bundleApp.putString("PACKAGE_NAME",APP_PACKAGE_NAME);
        bundleApp.putStringArray("ACTIVITY_LIST", new String[]{"*"});

// NEXT APP_LIST BUNDLE(S) INTO THE MAIN BUNDLE
        bMain.putParcelableArray("APP_LIST", new Bundle[]{
                bundleApp
        });

        //PUT bConfig into bMain
        bMain.putBundle("PLUGIN_CONFIG", bConfig);
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.SET_CONFIG", bMain);
        mContext.sendBroadcast(i);

        // Register for notifications - SCANNER_STATUS
        Bundle b = new Bundle();
        b.putString("com.symbol.datawedge.api.APPLICATION_NAME", APP_PACKAGE_NAME);
        b.putString("com.symbol.datawedge.api.NOTIFICATION_TYPE", "SCANNER_STATUS");
        i = new Intent();
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION", b);//(1)
        mContext.sendBroadcast(i);
    }

    void StopSoftTrigger(){
        // define action and data strings
        String softScanTrigger = "com.symbol.datawedge.api.ACTION";
        String extraData = "com.symbol.datawedge.api.SOFT_SCAN_TRIGGER";
        Intent i = new Intent();
        i.setAction(softScanTrigger);
        i.putExtra(extraData, "STOP_SCANNING");
        mContext.sendBroadcast(i);
    }



    void SoftTrigger(){
        // define action and data strings
        String softScanTrigger = "com.symbol.datawedge.api.ACTION";
        String extraData = "com.symbol.datawedge.api.SOFT_SCAN_TRIGGER";
        Intent i = new Intent();
        i.setAction(softScanTrigger);
        i.putExtra(extraData, "START_SCANNING");
        mContext.sendBroadcast(i);
    }


    public void enableDatawedge(boolean state) {
        Intent i = new Intent();
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.ENABLE_DATAWEDGE", state);
        mContext.sendBroadcast(i);
    }

    // Container Activity must implement this interface
    public interface DatawedgeListener {
        public void onDatawedgeEvent(Intent intent);
    }


}
