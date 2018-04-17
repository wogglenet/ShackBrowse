package net.woggle.shackbrowse.notifier;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;

import net.woggle.shackbrowse.MainActivity;
import net.woggle.shackbrowse.R;
import net.woggle.shackbrowse.StatsFragment;

public class ShackMessageNotifierReceiver extends BroadcastReceiver
{
	public static final int icon_res = R.drawable.note_logo;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.i("SBNOTIFIER", "NotifierReceiver invoked, starting service");

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);

		Bundle data = intent.getExtras();
		if ((data != null) && (data.getString("type") != null))
		{
			if (data.getString("type").equalsIgnoreCase("shackmsg"))
			{

				NotificationCompat.Builder mBuilder =
						new NotificationCompat.Builder(context)
								.setSmallIcon(icon_res)
								.setLargeIcon(largeIcon)
								.setContentTitle(data.get("username").toString() + " sent you a shackmessage")
								.setContentText(data.get("text").toString())
								.setTicker(data.get("username").toString() + " sent you a shackmessage")
								.setAutoCancel(true);


				// Creates an explicit intent for an Activity in your app
				Intent resultIntent = new Intent(context, MainActivity.class);

				resultIntent.putExtra("notificationOpenMessages", true);
				resultIntent.putExtra("notificationNLSID", Integer.parseInt(data.get("nlsid").toString()));
				int numNew = 0;
				if (data.get("username").toString().equals("multiple"))
				{
					// multiple replies
					mBuilder.setContentTitle("New shackmessages");
					mBuilder.setContentText("Click to show a list");

					if (data.get("username").toString().equals("multiple"))
					{
						try
						{
							numNew = Integer.parseInt(data.get("text").toString());
						} catch (Exception e)
						{

						}
						mBuilder.setNumber(numNew);
					}
					mBuilder.setTicker(numNew + " new shackmessages");

					resultIntent.putExtra("notificationOpenMessages", true);

				}

				resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

				// stupid hack for android bug
				resultIntent.setAction(Long.toString(System.currentTimeMillis()));

				PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

				mBuilder.setContentIntent(resultPendingIntent);
				Notification notification = mBuilder.build();

				int mId = 58403;
				NotifierReceiver.handleNotification(notification, mId, context);

				SharedPreferences.Editor editor = prefs.edit();
				editor.putString("GCMShackMsgLastNotifiedId", data.get("nlsid").toString());
				editor.commit();

				StatsFragment.statInc(context, "NotifiedShackMessage");
				StatsFragment.statInc(context, "Notifications");
			}
		}
	}
}
