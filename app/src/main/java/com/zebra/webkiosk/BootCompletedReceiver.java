package com.zebra.webkiosk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
       // adb shell am broadcast -a android.intent.action.BOOT_COMPLETED
        Log.d("WEBKIOSK","BOOT_COMPLETED");

      //  if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            i.putExtra("BOOT_COMPLETED", true);
            i.setAction("android.intent.action.MAIN");
            context.startActivity(i);
        //}

    }

}
