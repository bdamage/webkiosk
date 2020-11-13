package com.zebra.webkiosk;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity{

    public final static String TAG = "WebKioskSettings";

    private SettingsMgr mSettingsMgr;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_settings);
        mSettingsMgr = new SettingsMgr(this.getApplicationContext());
        mSettingsMgr.onLoadSettings();
        setEditText(R.id.editTextHomeUrl, mSettingsMgr.mSettingsData.homeURL);
        setEditText(R.id.editTextPassword, mSettingsMgr.mSettingsData.settingsPassword);
        Switch switchState = findViewById(R.id.debug);
        switchState.setChecked(mSettingsMgr.mSettingsData.chromeDebugging);
        switchState = findViewById(R.id.forcePortrait);
        switchState.setChecked(mSettingsMgr.mSettingsData.forcePortrait);
        switchState = findViewById(R.id.injectJavascript);
        switchState.setChecked(mSettingsMgr.mSettingsData.injectJavascript);

        switchState = findViewById(R.id.hideNavBar);
        switchState.setChecked(mSettingsMgr.mSettingsData.hideNavbar);

        switchState = findViewById(R.id.useScannerAPI);
        switchState.setChecked(mSettingsMgr.mSettingsData.useScannerAPI);

        switchState = findViewById(R.id.allowMixedContent);
        switchState.setChecked(mSettingsMgr.mSettingsData.allowMixedContent);

        switchState = findViewById(R.id.autoStartOnBoot);
        switchState.setChecked(mSettingsMgr.mSettingsData.autoStartOnBoot);


        switchState = findViewById(R.id.controlEKBVisibility);
        switchState.setChecked(mSettingsMgr.mSettingsData.ekbFullControl);


        switchState = findViewById(R.id.useEKB);
        switchState.setChecked(mSettingsMgr.mSettingsData.useEKB);
        setEditText(R.id.editTextEKBGroup, mSettingsMgr.mSettingsData.ekbDefaultGroup);
        setEditText(R.id.editTextEKBName, mSettingsMgr.mSettingsData.ekbDefaultName);

        String webViewVersion = "Only available on Android O and newer.";
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webViewVersion = getWebViewVersion();
        }
        setTextView(R.id.textViewAppVersion,"App version: " +BuildConfig.VERSION_NAME);
        setTextView(R.id.textViewWebViewVersion,"WebView version: "+webViewVersion);
    }
    public String getWebViewVersion()
    {
        PackageInfo webViewPackageInfo = WebView.getCurrentWebViewPackage();
        Log.d(TAG, "WebView version: " + webViewPackageInfo.versionName);
        return webViewPackageInfo.versionName;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onSaveSettings();
    }



    public void onSaveSettings() {

     //   editor.putInt("interval", getEditTextInt(com.zebra.webkiosk.R.id.editTextInterval));
        mSettingsMgr.mSettingsData.homeURL = getEditText(R.id.editTextHomeUrl);
        mSettingsMgr.mSettingsData.settingsPassword = getEditText(R.id.editTextPassword);
        mSettingsMgr.mSettingsData.forcePortrait = getSwitchState(R.id.forcePortrait);
        mSettingsMgr.mSettingsData.chromeDebugging = getSwitchState(R.id.debug);
        mSettingsMgr.mSettingsData.injectJavascript = getSwitchState(R.id.injectJavascript);
        mSettingsMgr.mSettingsData.useScannerAPI = getSwitchState(R.id.useScannerAPI);
        mSettingsMgr.mSettingsData.hideNavbar = getSwitchState(R.id.hideNavBar);
        mSettingsMgr.mSettingsData.ekbFullControl = getSwitchState(R.id.controlEKBVisibility);
        mSettingsMgr.mSettingsData.useEKB = getSwitchState(R.id.useEKB);
        mSettingsMgr.mSettingsData.allowMixedContent = getSwitchState(R.id.allowMixedContent);
        mSettingsMgr.mSettingsData.autoStartOnBoot = getSwitchState(R.id.autoStartOnBoot);


        mSettingsMgr.mSettingsData.ekbDefaultGroup = getEditText(R.id.editTextEKBGroup);
        mSettingsMgr.mSettingsData.ekbDefaultName = getEditText(R.id.editTextEKBName);

        mSettingsMgr.onSaveSettings();
    }


    private void setTextView( final int editViewId, final String displayString) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView view = (TextView) findViewById(editViewId);
                view.setText(displayString);
            }
        });

    }

    private void setEditText( final int editViewId, final String displayString) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EditText view = (EditText) findViewById(editViewId);
                view.setText(displayString);
            }
        });

    }

    private String getEditText( final int editViewId) {
        EditText view = (EditText) findViewById(editViewId);
        return view.getText().toString();
    }

    private int getEditTextInt( final int editViewId) {
        EditText view = (EditText) findViewById(editViewId);
        return Integer.parseInt(view.getText().toString());
    }
    private Boolean getSwitchState( final int id) {
        Switch view = (Switch) findViewById(id);
        //Log.d(TAG,view.isChecked());

        return view.isChecked();
    }

}
