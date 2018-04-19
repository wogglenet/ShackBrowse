package net.woggle.shackbrowse.notifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.woggle.shackbrowse.PeriodicNetworkService;

public class BootReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			PeriodicNetworkService pns = new PeriodicNetworkService();
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			long updateIntervalSeconds = Long.parseLong(prefs.getString("PeriodicNetworkServicePeriod", "10800")); // DEFAULT 3 HR 10800L,  5 minutes 50-100mb, 10 minutes 25-50mb, 30mins 10-20mb, 1 hr 5-10mb, 3 hr 1-3mb, 6hr .5-1.5mb, 12hr .25-1mb

			pns.scheduleJob(context, updateIntervalSeconds);
		}
	}
}