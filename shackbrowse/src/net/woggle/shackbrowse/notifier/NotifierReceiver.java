package net.woggle.shackbrowse.notifier;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

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

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;

public class NotifierReceiver extends FirebaseMessagingService
{
	public static final int icon_res = R.drawable.note_logo;

	@Override
	public void onMessageReceived(RemoteMessage message){

		Log.i("SBNOTIFIER", "NotifierReceiver invoked, starting service");
		Context context = getApplicationContext();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);


		String from = message.getFrom();
		Map data = message.getData();



		if ((data != null) && (data.get("type") != null))
		{
			if (data.get("type").toString().equalsIgnoreCase("reply"))
			{
				NotificationCompat.Builder mBuilder =
				        new NotificationCompat.Builder(context)
				        .setSmallIcon(icon_res)
				        .setLargeIcon(largeIcon)
				        .setContentTitle(data.get("username").toString() + " replied to your post")
						.setContentText(PostFormatter.formatContent(data.get("username").toString(), data.get("text").toString(), null, false, true))
						.setTicker(data.get("username").toString() + " replied to your post")
						.setAutoCancel(true);
				
				
				// Creates an explicit intent for an Activity in your app
				Intent resultIntent = new Intent(context, MainActivity.class);
				
				// save notification to db
				NotificationObj n = new NotificationObj(Integer.parseInt(data.get("nlsid").toString()), "reply", data.get("text").toString(), data.get("username").toString(), TimeDisplay.now(), "reply");
				n.commit(context);
				
				// check count
				int noteCount = prefs.getInt("GCMNoteCountReply", 0);
				int numNew = noteCount + 1;
				mBuilder.setNumber(numNew);
				
				// NLSID
				resultIntent.putExtra("notificationNLSID", Integer.parseInt(data.get("nlsid").toString()));
	
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
					mBuilder.setStyle(getBigTextFor(data.get("username").toString() + " replied to your post", data.get("text").toString()));
					resultIntent.putExtra("notificationOpenId", true);
				}
				
				resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
				
				// stupid hack for android bug
				resultIntent.setAction(Long.toString(System.currentTimeMillis()));
				
				PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				
				mBuilder.setContentIntent(resultPendingIntent);
				mBuilder.setDeleteIntent(getDeleteIntent("GCMNoteCountReply", context));
				Notification notification = mBuilder.build();

				// notification snooze
				if (checkIfMuted(Integer.parseInt(data.get("parentid").toString()), prefs))
				{
					return;
				}
				
				int mId = 58401;
				handleNotification(notification, mId, context);
				
				Editor editor = prefs.edit();
				editor.putInt("GCMNoteCountReply", numNew);
				editor.commit();

                StatsFragment.statInc(context, "NotifiedReply");
                StatsFragment.statInc(context, "Notifications");
			}
			else if (data.get("type").toString().equalsIgnoreCase("vanity"))
			{
			  
				NotificationCompat.Builder mBuilder =
				        new NotificationCompat.Builder(context)
				        .setSmallIcon(icon_res)
				        .setLargeIcon(largeIcon)
				        .setContentTitle(data.get("username").toString() + " mentioned you in a post")
						.setContentText(PostFormatter.formatContent(data.get("username").toString(), data.get("text").toString(), null, false, true))
						.setTicker(data.get("username").toString() + " mentioned you in a post")
						.setAutoCancel(true);
				
				
				// Creates an explicit intent for an Activity in your app
				Intent resultIntent = new Intent(context, MainActivity.class);
				
				
				// save notification to db
				NotificationObj n = new NotificationObj(Integer.parseInt(data.get("nlsid").toString()), "vanity", data.get("text").toString(), data.get("username").toString(), TimeDisplay.now(), "vanity");
				n.commit(context);
				
				// check count
				int noteCount = prefs.getInt("GCMNoteCountVanity", 0);
				int numNew = noteCount + 1;
				mBuilder.setNumber(numNew);
				
				// NLSID
				resultIntent.putExtra("notificationNLSID", Integer.parseInt(data.get("nlsid").toString()));
	
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
					mBuilder.setStyle(getBigTextFor(data.get("username").toString() + " mentioned your shack name", data.get("text").toString()));
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

                StatsFragment.statInc(context, "NotifiedVanity");
                StatsFragment.statInc(context, "Notifications");
			}
			else if (data.get("type").toString().equalsIgnoreCase("keyword"))
			{
			  
				NotificationCompat.Builder mBuilder =
				        new NotificationCompat.Builder(context)
				        .setSmallIcon(icon_res)
				        .setLargeIcon(largeIcon)
				        .setContentTitle(data.get("username").toString() + " mentioned " + data.get("keyword").toString())
						.setContentText(PostFormatter.formatContent(data.get("username").toString(), data.get("text").toString(), null, false, true))
						.setTicker(data.get("username").toString() + " mentioned " + data.get("keyword").toString())
						.setAutoCancel(true);
				
				
				// Creates an explicit intent for an Activity in your app
				Intent resultIntent = new Intent(context, MainActivity.class);
				
				// save notification to db
				NotificationObj n = new NotificationObj(Integer.parseInt(data.get("nlsid").toString()), "keyword", data.get("text").toString(), data.get("username").toString(), TimeDisplay.now(), data.get("keyword").toString());
				n.commit(context);
				
				// check count
				int noteCount = prefs.getInt("GCMNoteCount" + data.get("keyword").toString().hashCode(), 0);
				int numNew = noteCount + 1;
				mBuilder.setNumber(numNew);
				
				// NLSID
				resultIntent.putExtra("notificationNLSID", Integer.parseInt(data.get("nlsid").toString()));
				resultIntent.putExtra("notificationKeyword", data.get("keyword").toString());
	
				// second if checks if we are replacing a notification when the last one was never clicked
				// if so must create a multi reply
				if (numNew > 1)
				{
					// multiple replies
					mBuilder.setContentTitle("New Mentions of " + data.get("keyword").toString());
					mBuilder.setContentText("Click to show a list");
					mBuilder.setTicker(numNew + " new mentions of "+ data.get("keyword").toString());
					mBuilder.setStyle(getInboxStyleFor("New mentions of "+ data.get("keyword").toString(), "keyword", data.get("keyword").toString(), numNew, context));
					resultIntent.putExtra("notificationOpenKList", true);
				}
				else
				{
					mBuilder.setStyle(getBigTextFor(data.get("username").toString() + " mentioned " + data.get("keyword").toString(), data.get("text").toString()));
					resultIntent.putExtra("notificationOpenKeywordId", true);
				}
				
				resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
				
				// stupid hack for android bug
				resultIntent.setAction(Long.toString(System.currentTimeMillis()));
				
				PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
				
				mBuilder.setContentIntent(resultPendingIntent);
				mBuilder.setDeleteIntent(getDeleteIntent("GCMNoteCount" + data.get("keyword").toString().hashCode(), context));
				Notification notification = mBuilder.build();
				
				int mId = data.get("keyword").toString().hashCode();
				handleNotification(notification, mId, context);
				
				Editor editor = prefs.edit();
				editor.putInt("GCMNoteCount" + data.get("keyword").toString().hashCode(), numNew);
				editor.commit();

                StatsFragment.statInc(context, "NotifiedKeyword");
                StatsFragment.statInc(context, "Notifications");
			}
			else if (data.get("type").toString().equalsIgnoreCase("shackmsg"))
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
						try {
							numNew = Integer.parseInt(data.get("text").toString());
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
				editor.putString("GCMShackMsgLastNotifiedId", data.get("id").toString());
				editor.commit();

                StatsFragment.statInc(context, "NotifiedShackMessage");
                StatsFragment.statInc(context, "Notifications");
			}
		}
   }

	public static boolean checkIfMuted(int postId, SharedPreferences prefs)
	{
		String mutedList = prefs.getString("GCMNoteMuted", "");
		ArrayList<String> muted = new ArrayList<String>(Arrays.asList(mutedList.split(",")));

		if (mutedList.contains(Integer.toString(postId)))
		{
			return true;
		}
		return false;
	}
	// returns true for muted, false for now not muted
	public static boolean toggleMuted(int postId, SharedPreferences prefs)
	{

		String postIdString = Integer.toString(postId);
		String mutedList = prefs.getString("GCMNoteMuted", "");

		ArrayList<String> muted = new ArrayList<String>();
		if (!mutedList.equals(""))
			muted.addAll(Arrays.asList(mutedList.split(",")));

		if (mutedList.contains(Integer.toString(postId)))
		{
			for (int i = 0; i < muted.size(); i++)
			{
				if (muted.get(i).contentEquals(postIdString))
				{
					muted.remove(i);
				}
			}


			Editor edit = prefs.edit();
			edit.putString("GCMNoteMuted", commaDelimitedStringFromStringArrayList(muted));
			edit.commit();
			return false;
		}
		else
		{
			muted.add(0, postIdString);
			if (muted.size() > 20)
				muted.remove(muted.size() - 1); // remove last item

			Editor edit = prefs.edit();
			edit.putString("GCMNoteMuted", commaDelimitedStringFromStringArrayList(muted));
			edit.commit();
			return true;
		}
	}

	static public String commaDelimitedStringFromStringArrayList(ArrayList<String> list)
	{
		String out = "";
		for (int i = 0; i < list.size(); i++)
		{
			out = out + (i > 0 ? ",":"") + list.get(i);
		}
		return out;
	}

	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch(NumberFormatException e) {
			return false;
		} catch(NullPointerException e) {
			return false;
		}
		// only got here if we didn't return false
		return true;
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
	public static void handleNotification(Notification notification, int mId, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		NotificationManager mNotificationManager =
		    (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		// prevents two noisy/vibey notifications within 30 seconds (now a pref)
		long spamLimit = Long.parseLong(prefs.getString("limitVibrateSpamInMS", "30000"));
		if (System.currentTimeMillis() - prefs.getLong("lastNoisyNotificationMade", 0L) > spamLimit)
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
		notification.ledARGB = prefs.getInt("notificationColor", Color.GREEN);
		notification.ledOffMS = Integer.parseInt(prefs.getString("LEDBlinkInMS", "2000"));
		notification.ledOnMS = (int)(Integer.parseInt(prefs.getString("LEDBlinkInMS", "2000")) / 10);
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