package net.swigglesoft.shackbrowse;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import net.swigglesoft.shackbrowse.notifier.NotifierReceiver;

public class PushNotificationSetup {
    public static void SetupNotificationChannels(Context context) {
        // Set up android oreo notification channels
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

			NotificationChannel channel = new NotificationChannel(NotifierReceiver.CHANNEL_NEWPOST, "New Post Notifications", NotificationManager.IMPORTANCE_DEFAULT);
			channel.setDescription("Notifications when new posts come in that are replies, mentions or keyword matches"); channel.enableLights(true); channel.setLightColor(Color.GREEN); channel.enableVibration(true);
			notificationManager.createNotificationChannel(channel);
			channel = new NotificationChannel(NotifierReceiver.CHANNEL_SHACKMSG, "Shack Message Notifications", NotificationManager.IMPORTANCE_HIGH);
			channel.setDescription("Notifications when someone sends you a private message"); channel.enableLights(true); channel.setLightColor(Color.GREEN); channel.enableVibration(true);
			notificationManager.createNotificationChannel(channel);
			channel = new NotificationChannel(NotifierReceiver.CHANNEL_SYSTEM, "System Notifications", NotificationManager.IMPORTANCE_LOW);
			channel.setDescription("Notifications from the app, such as post queue notifications"); channel.enableLights(true); channel.setLightColor(Color.GREEN); channel.enableVibration(false);
			notificationManager.createNotificationChannel(channel);
		}
    }
}
