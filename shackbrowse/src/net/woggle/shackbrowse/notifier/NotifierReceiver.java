package net.woggle.shackbrowse.notifier;

import java.util.ArrayList;

import net.woggle.shackbrowse.MainActivity;
import net.woggle.shackbrowse.NotificationObj;
import net.woggle.shackbrowse.NotificationsDB;
import net.woggle.shackbrowse.PostFormatter;
import net.woggle.shackbrowse.R;
import net.woggle.shackbrowse.StatsFragment;
import net.woggle.shackbrowse.TimeDisplay;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.InboxStyle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

public class NotifierReceiver extends BroadcastReceiver {

	public static final int icon_res = R.drawable.note_logo;
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("SBNOTIFIER", "NotifierReceiver invoked, starting service");

      
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
      /*
      AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
      PendingIntent pendingIntent =
               PendingIntent.getBroadcast(context, 0, new Intent(context, NotifierReceiver.class), 0);

      // use inexact repeating which is easier on battery (system can phase events and not wake at exact times)
      alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0,
    		  AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent);
      */
		Bundle data = intent.getExtras();
		if (data != null)
		{
			if (data.getString("type").equalsIgnoreCase("reply"))
			{
			  
				NotificationCompat.Builder mBuilder =
				        new NotificationCompat.Builder(context)
				        .setSmallIcon(icon_res)
				        .setLargeIcon(largeIcon)
				        .setContentTitle(data.getString("username") + " replied to your post")
						.setContentText(PostFormatter.formatContent(data.getString("username"), data.getString("text"), null, false, true))
						.setTicker(data.getString("username") + " replied to your post")
						.setAutoCancel(true);
				
				
				// Creates an explicit intent for an Activity in your app
				Intent resultIntent = new Intent(context, MainActivity.class);
				
				// save notification to db
				NotificationObj n = new NotificationObj(Integer.parseInt(data.getString("nlsid")), "reply", data.getString("text"), data.getString("username"), TimeDisplay.now(), "reply");
				n.commit(context);
				
				// check count
				int noteCount = prefs.getInt("GCMNoteCountReply", 0);
				int numNew = noteCount + 1;
				mBuilder.setNumber(numNew);
				
				// NLSID
				resultIntent.putExtra("notificationNLSID", Integer.parseInt(data.getString("nlsid")));
	
				// second if checks if we are replacing a notification when the last one was never clicked
				// if so must create a multi reply
				if (numNew  > 1)
				{
					// multiple replies
					mBuilder.setContentTitle("New Replies");
					mBuilder.setContentText("Click to show a list");
					mBuilder.setTicker(numNew + " new replies to your posts");
			        mBuilder.setStyle(getInboxStyleFor("New replies to your posts", "reply", numNew, context));
					resultIntent.putExtra("notificationOpenRList", true);
				}
				else
				{
					mBuilder.setStyle(getBigTextFor(data.getString("username") + " replied to your post", data.getString("text")));
					resultIntent.putExtra("notificationOpenId", true);
				}
				
				resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
				
				// stupid hack for android bug
				resultIntent.setAction(Long.toString(System.currentTimeMillis()));
				
				PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				
				mBuilder.setContentIntent(resultPendingIntent);
				mBuilder.setDeleteIntent(getDeleteIntent("GCMNoteCountReply", context));
				Notification notification = mBuilder.build();
				
				int mId = 58401;
				handleNotification(notification, mId, context);
				
				Editor editor = prefs.edit();
				editor.putInt("GCMNoteCountReply", numNew);
				editor.commit();

                StatsFragment.statInc(context, "NotifiedReply", numNew);
                StatsFragment.statInc(context, "Notifications", numNew);
			}
			else if (data.getString("type").equalsIgnoreCase("vanity"))
			{
			  
				NotificationCompat.Builder mBuilder =
				        new NotificationCompat.Builder(context)
				        .setSmallIcon(icon_res)
				        .setLargeIcon(largeIcon)
				        .setContentTitle(data.getString("username") + " mentioned you in a post")
						.setContentText(PostFormatter.formatContent(data.getString("username"), data.getString("text"), null, false, true))
						.setTicker(data.getString("username") + " mentioned you in a post")
						.setAutoCancel(true);
				
				
				// Creates an explicit intent for an Activity in your app
				Intent resultIntent = new Intent(context, MainActivity.class);
				
				
				// save notification to db
				NotificationObj n = new NotificationObj(Integer.parseInt(data.getString("nlsid")), "vanity", data.getString("text"), data.getString("username"), TimeDisplay.now(), "vanity");
				n.commit(context);
				
				// check count
				int noteCount = prefs.getInt("GCMNoteCountVanity", 0);
				int numNew = noteCount + 1;
				mBuilder.setNumber(numNew);
				
				// NLSID
				resultIntent.putExtra("notificationNLSID", Integer.parseInt(data.getString("nlsid")));
	
				// second if checks if we are replacing a notification when the last one was never clicked
				// if so must create a multi reply
				if (numNew > 1)
				{
					// multiple replies
					mBuilder.setContentTitle("New Mentions of You");
					mBuilder.setContentText("Click to show a list");
					
					mBuilder.setTicker(numNew + " new mentions of your shack name");
					mBuilder.setStyle(getInboxStyleFor("New mentions of your name", "vanity", numNew, context));
					resultIntent.putExtra("notificationOpenVList", true);
				}
				else
				{
					mBuilder.setStyle(getBigTextFor(data.getString("username") + " mentioned your shack name", data.getString("text")));
					resultIntent.putExtra("notificationOpenVanityId", true);
				}
				
				resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
				
				// stupid hack for android bug
				resultIntent.setAction(Long.toString(System.currentTimeMillis()));
				
				PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				
				mBuilder.setContentIntent(resultPendingIntent);
				mBuilder.setDeleteIntent(getDeleteIntent("GCMNoteCountVanity", context));
				Notification notification = mBuilder.build();
				
				int mId = 58402;
				handleNotification(notification, mId, context);
				
				Editor editor = prefs.edit();
				editor.putInt("GCMNoteCountVanity", numNew);
				editor.commit();

                StatsFragment.statInc(context, "NotifiedVanity", numNew);
                StatsFragment.statInc(context, "Notifications", numNew);
			}
			else if (data.getString("type").equalsIgnoreCase("keyword"))
			{
			  
				NotificationCompat.Builder mBuilder =
				        new NotificationCompat.Builder(context)
				        .setSmallIcon(icon_res)
				        .setLargeIcon(largeIcon)
				        .setContentTitle(data.getString("username") + " mentioned " + data.getString("keyword"))
						.setContentText(PostFormatter.formatContent(data.getString("username"), data.getString("text"), null, false, true))
						.setTicker(data.getString("username") + " mentioned " + data.getString("keyword"))
						.setAutoCancel(true);
				
				
				// Creates an explicit intent for an Activity in your app
				Intent resultIntent = new Intent(context, MainActivity.class);
				
				// save notification to db
				NotificationObj n = new NotificationObj(Integer.parseInt(data.getString("nlsid")), "keyword", data.getString("text"), data.getString("username"), TimeDisplay.now(), data.getString("keyword"));
				n.commit(context);
				
				// check count
				int noteCount = prefs.getInt("GCMNoteCount" + data.getString("keyword").hashCode(), 0);
				int numNew = noteCount + 1;
				mBuilder.setNumber(numNew);
				
				// NLSID
				resultIntent.putExtra("notificationNLSID", Integer.parseInt(data.getString("nlsid")));
				resultIntent.putExtra("notificationKeyword", data.getString("keyword"));
	
				// second if checks if we are replacing a notification when the last one was never clicked
				// if so must create a multi reply
				if (numNew > 1)
				{
					// multiple replies
					mBuilder.setContentTitle("New Mentions of " + data.getString("keyword"));
					mBuilder.setContentText("Click to show a list");
					mBuilder.setTicker(numNew + " new mentions of "+ data.getString("keyword"));
					mBuilder.setStyle(getInboxStyleFor("New mentions of "+ data.getString("keyword"), "keyword", data.getString("keyword"), numNew, context));
					resultIntent.putExtra("notificationOpenKList", true);
				}
				else
				{
					mBuilder.setStyle(getBigTextFor(data.getString("username") + " mentioned " + data.getString("keyword"), data.getString("text")));
					resultIntent.putExtra("notificationOpenKeywordId", true);
				}
				
				resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
				
				// stupid hack for android bug
				resultIntent.setAction(Long.toString(System.currentTimeMillis()));
				
				PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				
				mBuilder.setContentIntent(resultPendingIntent);
				mBuilder.setDeleteIntent(getDeleteIntent("GCMNoteCount" + data.getString("keyword").hashCode(), context));
				Notification notification = mBuilder.build();
				
				int mId = data.getString("keyword").hashCode();
				handleNotification(notification, mId, context);
				
				Editor editor = prefs.edit();
				editor.putInt("GCMNoteCount" + data.getString("keyword").hashCode(), numNew);
				editor.commit();

                StatsFragment.statInc(context, "NotifiedKeyword", numNew);
                StatsFragment.statInc(context, "Notifications", numNew);
			}
			else if (data.getString("type").equalsIgnoreCase("shackmsg"))
			{
			  
				NotificationCompat.Builder mBuilder =
				        new NotificationCompat.Builder(context)
				        .setSmallIcon(icon_res)
				        .setLargeIcon(largeIcon)
				        .setContentTitle(data.getString("username") + " sent you a shackmessage")
						.setContentText(data.getString("text"))
						.setTicker(data.getString("username") + " sent you a shackmessage")
						.setAutoCancel(true);
				
				
				// Creates an explicit intent for an Activity in your app
				Intent resultIntent = new Intent(context, MainActivity.class);
				
				resultIntent.putExtra("notificationOpenMessages", true);
				resultIntent.putExtra("notificationNLSID", Integer.parseInt(data.getString("nlsid")));
                int numNew = 0;
				if (data.getString("username").equals("multiple"))
				{
					// multiple replies
					mBuilder.setContentTitle("New shackmessages");
					mBuilder.setContentText("Click to show a list");

					if (data.getString("username").equals("multiple"))
					{
						try {
							numNew = Integer.parseInt(data.getString("text"));
						}
						catch (Exception e)
						{
							
						}
						mBuilder.setNumber(numNew);
					}
					mBuilder.setTicker(numNew + " new shackmessages");
					
					resultIntent.putExtra("notificationOpenMessages", true);
					
				}
				
				resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
				
				// stupid hack for android bug
				resultIntent.setAction(Long.toString(System.currentTimeMillis()));
				
				PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				
				mBuilder.setContentIntent(resultPendingIntent);
				Notification notification = mBuilder.build();
				
				int mId = 58403;
				handleNotification(notification, mId, context);
				
				Editor editor = prefs.edit();
				editor.putString("GCMShackMsgLastNotifiedId", data.getString("id"));
				editor.commit();

                StatsFragment.statInc(context, "NotifiedShackMessage", numNew);
                StatsFragment.statInc(context, "Notifications", numNew);
			}
		}
   }
	private NotificationCompat.InboxStyle getInboxStyleFor(String title, String type, int num, Context ctx)
	{
		return getInboxStyleFor(title, type, null, num, ctx);
	}
	private NotificationCompat.InboxStyle getInboxStyleFor(String title, String type, String keyword, int num, Context ctx)
	{
		int overflow = 0;
		if (num > 5)
		{
			overflow = num - 5;
			num = 5;
		}
		net.woggle.shackbrowse.NotificationsDB ndb = new NotificationsDB(ctx);
		ndb.open();
		ArrayList<NotificationObj> new_notes = new ArrayList<NotificationObj>(ndb.getNew(type, keyword, num));
		ndb.close();
		InboxStyle inbox = new NotificationCompat.InboxStyle();
		for(NotificationObj n: new_notes)
		{
			inbox.addLine(TextUtils.concat(formatUser(n.getAuthor(), ctx),": ",PostFormatter.formatContent(n.getAuthor(), n.getBody(), null, false, true)));
		}
		inbox.setBigContentTitle(title);
		if (overflow > 0)
			inbox.setSummaryText(" + " + overflow + " more");
		return inbox;
	}
	private NotificationCompat.BigTextStyle getBigTextFor(String title, String text)
	{
		BigTextStyle big = new NotificationCompat.BigTextStyle();
		big.bigText(PostFormatter.formatContent("bradsh", text, null, false, true));
		big.setBigContentTitle(title);
		return big;
	}
	SpannableString formatUser(String user, Context ctx)
	{
		SpannableString form = new SpannableString(user);
		form.setSpan(new ForegroundColorSpan(ctx.getResources().getColor(R.color.userName)), 0, user.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
		return form;
	}
	private void handleNotification(Notification notification, int mId, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		NotificationManager mNotificationManager =
		    (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		// prevents two noisy/vibey notifications within 30 seconds
		if (System.currentTimeMillis() - prefs.getLong("lastNoisyNotificationMade", 0L) > 30000)
		{
			notification.sound = Uri.parse(prefs.getString("notificationSound", "DEFAULT_SOUND"));
		
			if (prefs.getBoolean("notificationVibrate", true))
			{
				notification.defaults|= Notification.DEFAULT_VIBRATE;
			}
			
			Editor editor = prefs.edit();
			editor.putLong("lastNoisyNotificationMade", System.currentTimeMillis());
			editor.commit();
		}
		
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		notification.ledARGB = Color.GREEN;
		notification.ledOffMS = 1500;
		notification.ledOnMS = 100;
		// mId allows you to update the notification later on.
		
		mNotificationManager.notify(mId, notification);
            	
		// WakefulIntentService.sendWakefulWork(context, new Intent(context, NotifierService.class));
		//setResultCode(Activity.RESULT_OK);
	}
	private static final String NOTIFICATION_DELETED_ACTION = "net.woggle.shackbrowse.NOTIFICATION_DELETED";
	private PendingIntent getDeleteIntent(String prefKeyToZero, Context ctx)
	{
		Intent intent = new Intent(NOTIFICATION_DELETED_ACTION);
		intent.putExtra("key", prefKeyToZero);
        PendingIntent pendintIntent = PendingIntent.getBroadcast(ctx, 0, intent, 0);
        return pendintIntent;
	}
}