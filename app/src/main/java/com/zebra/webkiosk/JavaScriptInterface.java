package com.zebra.webkiosk;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class JavaScriptInterface {
    private Activity activity;
    public static final String TAG = "WebKiosk.JSInterface";
    public static final String PROFILENAME = "WEBKIOSK";

    public JavaScriptInterface(Activity activity) {
        this.activity = activity;
    }
    @JavascriptInterface
    public String version(){
        return(BuildConfig.VERSION_NAME);
    }

    @JavascriptInterface
    public void onSoftTriggerScan(){
        Log.d(MainActivity.TAG,"Soft trigger!");
        MainActivity ma = (MainActivity)activity;
        ma.mScanner.SoftTrigger();
    }

    @JavascriptInterface
    public void softTriggerScan(){
        Log.d(MainActivity.TAG,"Soft trigger!");
        MainActivity ma = (MainActivity)activity;
        ma.mScanner.SoftTrigger();
    }

    @JavascriptInterface
    public void enableDatawedge(boolean state){
        Log.d(MainActivity.TAG,"Enable / Disable Datawedge: "+state);
        MainActivity ma = (MainActivity)activity;
        ma.mScanner.enableDatawedge(state);
    }


    @JavascriptInterface
    public void hideSip(){
        Log.d("KIOSK", "hideSip()");

        //  Activity activity = (Activity) v.getContext();
        CustomWebView webview = (CustomWebView)activity.findViewById(R.id.activity_main_webview);
        webview.bKeyboardShowState = false;

        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);

    }

    @JavascriptInterface
    public void showSip()
    {
        Log.d("KIOSK", "showSip()");
        CustomWebView webview = (CustomWebView)activity.findViewById(R.id.activity_main_webview);
        webview.bKeyboardShowState = true;
        Activity a = (Activity) webview.getContext();

        InputMethodManager inputMethodManager = (InputMethodManager)  a.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //  inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
    }

    @JavascriptInterface
    public void setEKBLayout(String group, String name)
    {
        //       adb shell am broadcast -p com.symbol.mxmf.csp.enterprisekeyboard -a com.symbol.ekb.api.ACTION_UPDATE -e CURRENT_LAYOUT_GROUP NumericOnly -e CURRENT_LAYOUT_NAME NumericOnly
        Intent i = new Intent();
        i.setPackage("com.symbol.mxmf.csp.enterprisekeyboard");
        i.setAction("com.symbol.ekb.api.ACTION_UPDATE");
        i.putExtra("CURRENT_LAYOUT_GROUP",group);
        i.putExtra("CURRENT_LAYOUT_NAME",name);
        activity.getApplicationContext().sendBroadcast(i);
    }
    @JavascriptInterface
    public void resetEKBLayout(boolean doReset) {

        Boolean needToReset = doReset;
        Log.d(TAG,"resetEKBLayout ***" +needToReset.toString());
        Intent intent = new Intent();
        intent.setAction( "com.symbol.ekb.api.ACTION_DO" );
        intent.setPackage("com.symbol.mxmf.csp.enterprisekeyboard");
        intent.addFlags(Intent. FLAG_RECEIVER_FOREGROUND );
        intent.putExtra( "RESET_LAYOUT" , needToReset); // needToReset is a Boolean object so it can be either true or false.
        activity.sendBroadcast(intent);
    }

    @JavascriptInterface
    public void enableEKB(boolean enable) {
        Boolean needToEnable = enable;
        Intent intent = new Intent();
        intent.setAction( "com.symbol.ekb.api.ACTION_UPDATE" );
        intent.setPackage("com.symbol.mxmf.csp.enterprisekeyboard");
        intent.addFlags(Intent. FLAG_RECEIVER_FOREGROUND );
        intent.putExtra( "ENABLE" , needToEnable); // needToEnable is a Boolean object so it can be either true or false.
        //Intent responseIntent = new Intent(this, MyBroadcastReceiver.class);
        //PendingIntent piResponse = PendingIntent.getBroadcast(activity.getApplicationContext(), requestCode, responseIntent, flags);
       // intent.putExtra("CALLBACK_RESPONSE", piResponse);
        activity.sendBroadcast(intent);
    }
    @JavascriptInterface
    public void showEKB(boolean show) {
        Boolean needToEnable = show;
        Intent intent = new Intent();
        intent.setAction( "com.symbol.ekb.api.ACTION_UPDATE" );
        intent.setPackage("com.symbol.mxmf.csp.enterprisekeyboard");
        intent.addFlags(Intent. FLAG_RECEIVER_FOREGROUND );
        intent.putExtra( "SHOW" , needToEnable); // needToEnable is a Boolean object so it can be either true or false.
        activity.sendBroadcast(intent);
    }



    //scannerSetConfig("BARCODE", "aim_type","1");
    @JavascriptInterface
    public void scannerSetConfig(String pluginName,String key, String value ){
        Log.d(TAG, "scannerSetConfig: "+pluginName+", "+key+" : "+value);
        Bundle bMain = new Bundle();
        Bundle bConfig = new Bundle();
        Bundle bParams = new Bundle();
        Intent i = new Intent();

        bMain.putString("PROFILE_NAME", PROFILENAME);
        bMain.putString("CONFIG_MODE", "UPDATE");
        bConfig.putString("PLUGIN_NAME", pluginName);
        bParams.putString("scanner_selection","auto"); //!important if decoders are being set etc
        bParams.putString(key ,value);

        bConfig.putBundle("PARAM_LIST", bParams);
        bMain.putBundle("PLUGIN_CONFIG", bConfig);
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.SET_CONFIG", bMain);
        activity.getApplicationContext().sendBroadcast(i);
    }


    //scannerSetConfig("BARCODE", "aim_type","1");
    @JavascriptInterface
    public void scannerSetConfigEx(String pluginName, String config ){
        Bundle bMain = new Bundle();
        Bundle bConfig = new Bundle();
        Bundle bParams = new Bundle();
        Intent i = new Intent();

        bMain.putString("PROFILE_NAME", PROFILENAME);
        bMain.putString("CONFIG_MODE", "UPDATE");
        bConfig.putString("PLUGIN_NAME", pluginName);
        Log.d(TAG,config);
        bParams = jsonStringToBundle(config);

        bConfig.putBundle("PARAM_LIST", bParams);
        bMain.putBundle("PLUGIN_CONFIG", bConfig);
        i.setAction("com.symbol.datawedge.api.ACTION");
        i.putExtra("com.symbol.datawedge.api.SET_CONFIG", bMain);
        activity.getApplicationContext().sendBroadcast(i);
    }


    public static Bundle jsonStringToBundle(String jsonString){
        try {
            JSONObject jsonObject = toJsonObject(jsonString);
            return jsonToBundle(jsonObject);
        } catch (JSONException ignored) {

        }
        return null;
    }
    public static JSONObject toJsonObject(String jsonString) throws JSONException {
        return new JSONObject(jsonString);
    }
    public static Bundle jsonToBundle(JSONObject jsonObject) throws JSONException {
        Bundle bundle = new Bundle();
        Iterator iter = jsonObject.keys();
        while(iter.hasNext()){
            String key = (String)iter.next();
            String value = jsonObject.getString(key);
            bundle.putString(key,value);
        }
        return bundle;
    }

}