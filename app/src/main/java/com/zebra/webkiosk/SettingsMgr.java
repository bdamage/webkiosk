package com.zebra.webkiosk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class SettingsMgr {
    public SettingsData mSettingsData = new SettingsData();
    public final static String TAG = "WEBKIOSK_SETTINGS";
   // public final static String SCOPE = "webkiosk";
    private String mSettingsFile = "config.txt";
    private Context mContext;

    private String mPath;

    public SettingsMgr(Context context) {
        mContext = context;
        //Log.d(TAG,Environment.getExternalStorageDirectory().getAbsoluteFile().toString());
        //Legacy path
        mPath = Environment.getExternalStorageDirectory() + File.separator  + "webkiosk/";

        //Android 10+ friendly
        mPath = mContext.getExternalFilesDir(null).getPath();

    }

    public void onSaveSettings() {
        Log.v(TAG, "Save settings.");
/*
        SharedPreferences sharedPref = mContext.getSharedPreferences(SCOPE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();
        //   editor.putInt("interval", getEditTextInt(com.zebra.webkiosk.R.id.editTextInterval));
        editor.putString("homeURL", mSettingsData.homeURL);
        editor.putBoolean("chromeDebugging",  mSettingsData.chromeDebugging);
        editor.putBoolean("forcePortrait", mSettingsData.forcePortrait);
        editor.putBoolean("injectJavascript", mSettingsData.injectJavascript);
        editor.putString("settingsPassword", mSettingsData.settingsPassword);
        editor.putString("ekbDefaultGroup", mSettingsData.ekbDefaultGroup);
        editor.putString("ekbDefaultName", mSettingsData.ekbDefaultName);
        editor.putBoolean("useEKB", mSettingsData.useEKB);
        editor.putBoolean("hideNavbar", mSettingsData.hideNavbar);
        editor.putBoolean("useScannerAPI", mSettingsData.useScannerAPI);
        //  Log.v(TAG, "New interval: "+getEditTextInt(com.zebra.webkiosk.R.id.editTextInterval));
        editor.commit();
  */
        writeSettingFile();

    }


    public void setSettingFile(String filename) {
        mSettingsFile = filename;
        Log.d(TAG,"Changed setting file to: "+mSettingsFile);
    }

    public void writeSettingFile(){

        Gson gson = new Gson();
        String json = gson.toJson(mSettingsData);
        Log.d(TAG,json);
        File fPath;
        fPath = new File(mPath);
        if(fPath.isDirectory() == false) {
            Log.d(TAG,"Path does not exist: " + fPath.getPath());
           boolean success =  fPath.mkdirs();
           Log.d(TAG,"Path created = "+ success);
        }
        Log.d(TAG,"Config save path: "+mPath);
        final File file = new File(mPath, mSettingsFile);

        // Save your stream, don't forget to flush() it before closing it.

        try
        {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(json);
            myOutWriter.close();
            fOut.flush();
            fOut.close();
        }
        catch (IOException e)  {
            Log.e(TAG, "File write failed: " + e.toString());
        }
    }

    public void onLoadSettings() {
        Log.v(TAG, "Load settings.");

      /*       SharedPreferences sharedPref = mContext.getSharedPreferences(SCOPE, Context.MODE_PRIVATE);
        //String interval = Integer.toString(sharedPref.getInt("interval", 10));
   mSettingsData.homeURL = sharedPref.getString("homeURL", "http://www.html5test.com");
        mSettingsData.injectJavascript = sharedPref.getBoolean("injectJavascript", false);
        mSettingsData.useScannerAPI = sharedPref.getBoolean("useScannerAPI", true);
        mSettingsData.forcePortrait = sharedPref.getBoolean("forcePortrait", true);
        mSettingsData.chromeDebugging = sharedPref.getBoolean("chromeDebugging", false);
        mSettingsData.hideNavbar = sharedPref.getBoolean("hideNavbar", false);
        mSettingsData.settingsPassword = sharedPref.getString("settingsPassword", "zebra");
        mSettingsData.useEKB = sharedPref.getBoolean("useEKB", false);
        mSettingsData.ekbDefaultGroup = sharedPref.getString("ekbDefaultGroup", "NumericOnly");
        mSettingsData.ekbDefaultName = sharedPref.getString("ekbDefaultName", "NumericOnly");
*/
        readSettingFile();
    }

    public void readSettingFile(){

        File fPath;
        fPath = new File(mPath);
        if(fPath.isDirectory() == false) {
            Log.d(TAG,"Path does not exist: " + fPath.getPath());
            boolean success =  fPath.mkdirs();
            Log.d(TAG,"Path created = "+ success);
        }

        final File file = new File(mPath, mSettingsFile);

        // Save your stream, don't forget to flush() it before closing it.
        if (file.exists() ) {
            try {

                FileInputStream fis;
                fis = new FileInputStream(file);
                StringBuffer fileContent = new StringBuffer();

                byte[] buffer = new byte[fis.available()];
                int n;
                while ((n = fis.read(buffer)) != -1) {
                    fileContent.append(new String(buffer, 0, n));
                }

                Log.d(TAG,"String buffer: "+fileContent.toString());
                Gson gson = new Gson();

                mSettingsData = gson.fromJson(""+fileContent, mSettingsData.getClass());

            } catch (IOException e) {
                Log.e(TAG, "File read failed: " + e.toString());
            }
        } else {
            Log.e(TAG, "Config doesn't exist.");
            writeSettingFile(); //write default settings
        }
    }

    public String getPath() {
        return mPath;
    }

}
