package com.zebra.webkiosk;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.SslError;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;


public class MainActivity extends AppCompatActivity implements ScannerMgr.DatawedgeListener, NetworkConnectivityReceiver.NetworkChangeListener, NetworkConnectivityReceiver.NetworkEventListener, BatteryReceiver.BatteryListener {

// ****************************************************
    private boolean CUSTOM_MULTIBARCODE = false;
// ****************************************************
 public static final String TAG = "WEBKIOSK";
    private CustomWebView mWebView;
    JavaScriptInterface jsInterface;

    public ScannerMgr mScanner = null;
    private SettingsMgr mSettingsMgr;
    private NetworkConnectivityReceiver mNetReceiver;
    private BatteryReceiver mBatteryReceiver;

    private boolean ekbShowState = true;

    private static final int FILECHOOSER_RESULTCODE = 2888;
    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI = null;
    private static final int INPUT_FILE_REQUEST_CODE = 1;

    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;


    @Override
    public void onNetworkChange(Boolean connected) {
        Log.d(TAG,"onNetworkChange connected: "+connected);
        mWebView.evaluateJavascript("javascript:if(typeof onNetworkChange === \"function\") onNetworkChange('"+ mSettingsMgr.mSettingsData.homeURL+"', "+connected+");",null);
    }

    @Override
    public void onNetworkEvent(String json) {
        json = json.replace("\"\"","\"");
        Log.d(TAG,"onNetworkEvent: "+json);
        mWebView.evaluateJavascript("javascript:if(typeof onNetworkEvent === \"function\") onNetworkEvent("+json+");",null);
    }

    @Override
    public void onDatawedgeEvent(Intent i) {
        String data = i.getStringExtra(ScannerMgr.DATA_STRING_TAG);
        String type = i.getStringExtra(ScannerMgr.LABEL_TYPE);
        String source = i.getStringExtra(ScannerMgr.SOURCE_TAG);
        String decode = "";//i.getStringExtra(ScannerMgr.DECODE_DATA_TAG);
        Log.d(TAG,"***barcode:"+data);
        String json = "{data:\""+data+"\",type:\""+type+"\", source:\""+source+"\", decode:\""+decode+"\"}";
        Log.d(TAG,"***barcode (json obj):"+json);
        mWebView.evaluateJavascript("javascript:if(typeof onDatawedgeEvent === \"function\") onDatawedgeEvent("+json+");",null);
        mWebView.evaluateJavascript("javascript:if(typeof onScan === \"function\") onScan('"+data+"');",null);
    }

    @Override
    public void onBatteryEvent(Intent intent) {
        Log.d(TAG,"MainActivity got battery event!");
        Bundle bundle = intent.getExtras();
        //extras
        JSONObject json = new JSONObject();
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            try {
                // json.put(key, bundle.get(key)); see edit below
                json.put(key, JSONObject.wrap(bundle.get(key)));
            } catch(JSONException e) {
                //Handle exception here
            }
        }

      //  Log.d(TAG,json.toString());
        mWebView.evaluateJavascript("javascript:if(typeof onBatteryEvent === \"function\") onBatteryEvent("+json.toString() +");",null);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mWebView.loadUrl(mSettingsMgr.mSettingsData.homeURL);
                    return true;
                case R.id.navigation_dashboard:

                    if(mSettingsMgr.mSettingsData.ekbFullControl){

                        ekbShowState = !ekbShowState;
                        Log.d(TAG, "EKB Show state: "+ekbShowState);

                        jsInterface.enableEKB(ekbShowState);

                        if(ekbShowState)
                            jsInterface.showEKB(true);

                        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.navigation);
                        Menu menu = bottomNavigationView.getMenu();
                        if(ekbShowState==false)
                            menu.findItem(R.id.navigation_dashboard).setIcon(R.drawable.nokeyboard);
                        else
                            menu.findItem(R.id.navigation_dashboard).setIcon(R.drawable.ic_keyboard_black_24dp);


                    }   else {
                        showSip();
                    }

                    return true;
                case R.id.navigation_notifications:
                    onSettings();
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mNetReceiver.unregisterReceiver();
        mScanner.unregisterReceiver();
        mBatteryReceiver.unregisterReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSettingsMgr.onLoadSettings();
        if(mSettingsMgr.mSettingsData.forcePortrait)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);

        mWebView.setWebContentsDebuggingEnabled(mSettingsMgr.mSettingsData.chromeDebugging);
        if(mSettingsMgr.mSettingsData.useEKB)
            jsInterface.setEKBLayout(mSettingsMgr.mSettingsData.ekbDefaultGroup, mSettingsMgr.mSettingsData.ekbDefaultName);

        if(mScanner!=null && mSettingsMgr.mSettingsData.useScannerAPI==false) {
          //  mScanner.unregisterReceiver(BroadcastReceiver);
            mScanner.unregisterReceiver();
        } else if ( mSettingsMgr.mSettingsData.useScannerAPI) {
            if(mScanner == null)
                mScanner = new ScannerMgr(this);
            if(CUSTOM_MULTIBARCODE)
                mScanner.createScannerProfile();
            else
                mScanner.createScannerProfileClean();
        }

        setBottomNavBarVisibility();
        checkWiFi();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSettingsMgr = new SettingsMgr(this);
        Intent intent = this.getIntent();
        boolean rebooted = false;

        if(intent!=null) {
            if( intent.getAction().equals("com.zebra.webkiosk.LOAD_CONFIG")) {
                String configfile = intent.getStringExtra("config");
                Log.d(TAG, "Detected to load config file: "+configfile);
                if(configfile!=null)
                    mSettingsMgr.setSettingFile(configfile);
                else
                    Log.d(TAG,"Invalid setting filename provided via intent");
            } else if( intent.getAction().equals("android.intent.action.MAIN")) {

                 rebooted = intent.getBooleanExtra("BOOT_COMPLETED", false);

            }
        }

        mSettingsMgr.onLoadSettings();

        if(rebooted == true && mSettingsMgr.mSettingsData.autoStartOnBoot == false) {
            Log.d(TAG, "No auto start needed -  shutting down app");
         //  shutdownApp();
        }

        /*********************************************************
                Set up layout
         *********************************************************/

        //Remove notification bar
        if(mSettingsMgr.mSettingsData.forcePortrait)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_main);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        /**************************************************
                Set up permissions and platform check
         **************************************************/
        checkPlatform();

        checkForPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        checkForPermission(Manifest.permission.CAMERA);
        checkForPermission(Manifest.permission.ACCESS_FINE_LOCATION);


        if(mSettingsMgr.mSettingsData.useScannerAPI) {
            mScanner = new ScannerMgr(this);
            mScanner.registerReceiver();

            if(CUSTOM_MULTIBARCODE)
                mScanner.createScannerProfile();
            else
                mScanner.createScannerProfileClean();

        }

        mNetReceiver = new NetworkConnectivityReceiver(this);
        mBatteryReceiver = new BatteryReceiver(this);

        initWebView();
    }

    public void initWebView(){

        Log.d(TAG,"initialize WebView component.");
        mWebView = (CustomWebView) findViewById(R.id.activity_main_webview);
        mWebView.setWebViewClient(new MyWebViewClient());



        // Enable Javascript
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mWebView.setWebContentsDebuggingEnabled(mSettingsMgr.mSettingsData.chromeDebugging);


        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setDatabaseEnabled(true);
        mWebView.getSettings().setAllowContentAccess(true);
        mWebView.getSettings().setAllowFileAccess(true);


        mWebView.clearHistory();
        mWebView.clearCache(true);

        mWebView.clearSslPreferences();
       // mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);


       // if(mSettingsMgr.mSettingsData.allowMixedContent)
         //   mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);


        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(mWebView,true);
        cookieManager.setAcceptFileSchemeCookies(true);

        jsInterface = new JavaScriptInterface(this);
        mWebView.addJavascriptInterface(jsInterface, "JSInterface");

        mWebView.setWebChromeClient(new MyChromeClient());
        mWebView.loadUrl(mSettingsMgr.mSettingsData.homeURL);

    }

    public void checkForPermission(String permissionType) {

        if (ContextCompat.checkSelfPermission(this,
                permissionType)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    permissionType)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                showCustomDialog("Permission", "Permission type is not granted:\n"+permissionType+"\nApp may not support all features.");
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{permissionType},
                        1001);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
            if(mSettingsMgr.mSettingsData.chromeDebugging)
                Toast.makeText(getApplicationContext(), "Permission already granted \n"+permissionType, Toast.LENGTH_SHORT).show();
        }


    }

    public void showCustomDialog(String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }


    public void checkWiFi() {
        onNetworkEvent(mNetReceiver.fetchWiFiState());
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    public void shutdownApp(){
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Shutting down app! ***");

                moveTaskToBack(false);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);

            }
        }, 3000);
    }

    public void checkPlatform() {

        if(isEmulator())
            return;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Build.BRAND.compareTo("Zebra") != 0) {
                    Log.d(TAG, "Not a valid platform!");
                    Toast.makeText(getApplicationContext(), "Invalid platform! Will close the app.", Toast.LENGTH_LONG).show();
                    shutdownApp();
                }
            }
                });


    }


    public void setBottomNavBarVisibility(){
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        if(mSettingsMgr.mSettingsData.hideNavbar)
            navigation.setVisibility(View.GONE);
        else
            navigation.setVisibility(View.VISIBLE);

    }


    @Override
    public void onBackPressed() {
        if(mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            Uri[] results = null;
            // Check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    // If there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            if (requestCode != FILECHOOSER_RESULTCODE || mUploadMessage == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            if (requestCode == FILECHOOSER_RESULTCODE) {
                if (null == this.mUploadMessage) {
                    return;
                }
                Uri result = null;
                try {
                    if (resultCode != RESULT_OK) {
                        result = null;
                    } else {
                        // retrieve from the private variable if the intent is null
                        result = data == null ? mCapturedImageURI : data.getData();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "activity :" + e,
                            Toast.LENGTH_LONG).show();
                }
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }
        }
        return;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }

    private class MyChromeClient extends WebChromeClient {

        // For Android 5.0
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, WebChromeClient.FileChooserParams fileChooserParams) {
            Log.d(TAG,"Open File chooser 5.0+");

            // Double check that we don't have any existing callbacks
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePath;

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e(TAG, "Unable to create Image File", ex);
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }
            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("image/*");
            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }
            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);


            return true;
        }

        // openFileChooser for Android 3.0+
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType){

            Log.d(TAG,"Open File chooser");
            // Update message
            mUploadMessage = uploadMsg;

            try{

                // Create AndroidExampleFolder at sdcard

                File imageStorageDir = new File(
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES)
                        , "AndroidExampleFolder");

                if (!imageStorageDir.exists()) {
                    // Create AndroidExampleFolder at sdcard
                    imageStorageDir.mkdirs();
                }

                // Create camera captured image file path and name
                File file = new File(
                        imageStorageDir + File.separator + "IMG_"
                                + String.valueOf(System.currentTimeMillis())
                                + ".jpg");

                mCapturedImageURI = Uri.fromFile(file);

                // Camera capture image intent
                final Intent captureIntent = new Intent(
                        android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");

                // Create file chooser intent
                Intent chooserIntent = Intent.createChooser(i, "Image Chooser");

                // Set camera intent to file chooser
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
                        , new Parcelable[] { captureIntent });

                // On select image call onActivityResult method of activity
                startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);

            }
            catch(Exception e){
                Toast.makeText(getBaseContext(), "Exception:"+e,
                        Toast.LENGTH_LONG).show();
            }

        }


        // The webPage has 2 filechoosers and will send a
        // console message informing what action to perform,
        // taking a photo or updating the file

        public boolean onConsoleMessage(ConsoleMessage cm) {

            onConsoleMessage(cm.message(), cm.lineNumber(), cm.sourceId());
            return true;
        }

    }

    private class MyWebViewClient extends WebViewClient {

        private Handler handler = new Handler();

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url); // load the url
            return true;
        }
        @SuppressWarnings("deprecation")
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            if(errorCode == 404){
                Log.d(TAG, "Invalid URL: "+failingUrl);
            } else if(errorCode == 500){
                Log.d(TAG, "Internal Server error: "+failingUrl);
            } else {
                Log.d(TAG, "Page load error (code: "+String.valueOf(errorCode)+") URL: "+failingUrl);
            }

            final int errorCode_ = errorCode;

            if (errorCode == WebViewClient.ERROR_HOST_LOOKUP) {

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Delayed loadURL " + errorCode_);
                        if (mWebView == null)
                            Log.d(TAG, "webview is null");

                        mWebView.setActivated(true);

                        if (mWebView != null && mWebView.isActivated()) {
                           // String url = "file:///" + Environment.getExternalStorageDirectory().getPath() + "/offline.html";
                            String url = "file:///android_asset/badurl.html";
                            mWebView.loadUrl(url);

                            Handler hdler = new Handler();

                            hdler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mWebView.evaluateJavascript("javascript:onError('" + errorCode_ + "');", null);
                                }
                                }, 500);
                        }
                    }
                }, 500);
            }
        }
           @Override
         public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
           //   handler.proceed();
              Log.d(TAG,"ssl_error: " + error.toString());
          }


        @TargetApi(android.os.Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError rerr) {
            // Redirect to deprecated method, so you can use it in all SDK versions
            onReceivedError(view, rerr.getErrorCode(), rerr.getDescription().toString(), req.getUrl().toString());
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d(TAG,"Inject JS file");
            if(mSettingsMgr.mSettingsData.injectJavascript)
                injectScriptFile(mWebView,"inject.js");

            checkWiFi();
        }

        @Override
        public boolean onRenderProcessGone(WebView view,
                                           RenderProcessGoneDetail detail) {
            if (!detail.didCrash()) {
                // Renderer was killed because the system ran out of memory.
                // The app can recover gracefully by creating a new WebView instance
                // in the foreground.
                Log.e(TAG, "System killed the WebView rendering process " +
                        "to reclaim memory. Recreating...");

                if (mWebView != null) {
                    ViewGroup webViewContainer =
                            (ViewGroup) findViewById(R.id.activity_main_webview);
                    webViewContainer.removeView(mWebView);
                    mWebView.destroy();
                    mWebView = null;
                }

                // By this point, the instance variable "mWebView" is guaranteed
                // to be null, so it's safe to reinitialize it.


                mWebView.clearCache(true);

                // Enable Javascript
                WebSettings webSettings = mWebView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                mWebView.setWebContentsDebuggingEnabled(mSettingsMgr.mSettingsData.chromeDebugging);

                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setAcceptCookie(true);
                cookieManager.setAcceptThirdPartyCookies(mWebView,true);
                cookieManager.setAcceptFileSchemeCookies(true);


                mWebView.loadUrl(mSettingsMgr.mSettingsData.homeURL);
                JavaScriptInterface jsInterface = new JavaScriptInterface(getParent());
                mWebView.addJavascriptInterface(jsInterface, "JSInterface");

                return true; // The app continues executing.
            }

            // Renderer crashed because of an internal error, such as a memory
            // access violation.
            Log.e(TAG, "The WebView rendering process crashed!");

            // In this example, the app itself crashes after detecting that the
            // renderer crashed. If you choose to handle the crash more gracefully
            // and allow your app to continue executing, you should 1) destroy the
            // current WebView instance, 2) specify logic for how the app can
            // continue executing, and 3) return "true" instead.
            return false;
        }

    }

/*
    // now we need a broadcast receiver
     public android.content.BroadcastReceiver BroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "WEBKIOSK Action: " + action);
            if (action.equals(ScannerMgr.APP_PACKAGE_NAME)) {

                handleDecodeData(intent);
            } else if (action.equals(ScannerMgr.NOTIFICATION_ACTION)) {
                if (intent.hasExtra("com.symbol.datawedge.api.NOTIFICATION")) {
                    Bundle b = intent.getBundleExtra("com.symbol.datawedge.api.NOTIFICATION");
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
                                    if(barcodeScannedStarted && CUSTOM_MULTIBARCODE)
                                        mWebView.evaluateJavascript("javascript:onScan('|');",null);
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
    };
*/
/*
    private void handleDecodeData(Intent i) {
        String data = i.getStringExtra(ScannerMgr.DATA_STRING_TAG);
        String type = i.getStringExtra(ScannerMgr.LABEL_TYPE);
        Log.d(TAG,"***barcode:"+data);
        mWebView.evaluateJavascript("javascript:onScan('"+data+"');",null);
    }
*/
    public void showSip(){
        Log.d(TAG, "showSip()");
        mWebView.bKeyboardShowState = true;
        Activity a = (Activity) this;

        InputMethodManager inputMethodManager = (InputMethodManager)  a.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //  inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);

    }

    private void injectScriptFile(WebView view, String scriptFile) {
        InputStream input;
        FileInputStream fis;
        try {
            File f = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/inject.js");
            fis = new FileInputStream(f); //getAssets().open(scriptFile);
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();

            // String-ify the script byte-array using BASE64 encoding !!!
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            view.loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var script = document.createElement('script');" +
                    "script.type = 'text/javascript';" +
                    // Tell the browser to BASE64-decode the string into your script !!!
                    "script.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(script)" +
                    "})()");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public void onSettings() {


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please enter password for settings");

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String m_Text = "";

                m_Text = input.getText().toString();

                if (m_Text.compareTo(mSettingsMgr.mSettingsData.settingsPassword) == 0) {
                    Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                    startActivity(intent);
                } else {

                    Toast.makeText(getApplicationContext(),"Invalid password", Toast.LENGTH_LONG).show();
                }

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }


}
