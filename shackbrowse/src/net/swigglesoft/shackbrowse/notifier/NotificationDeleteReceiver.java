package net.swigglesoft.shackbrowse.notifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class NotificationDeleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Editor edit = prefs.edit();
        if (intent != null && intent.getExtras() != null)
        {
            edit.putInt(intent.getExtras().getString("key"), 0);
            edit.commit();
        }
        System.out.println("SWIPED NOTIFICATION: deleted prefkey " +intent.getExtras().getString("key"));
    }
}