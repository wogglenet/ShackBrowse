package net.swigglesoft.shackbrowse.notifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import net.swigglesoft.shackbrowse.MainActivity;
import net.swigglesoft.shackbrowse.NotificationObj;
import net.swigglesoft.shackbrowse.NotificationsDB;
import net.swigglesoft.shackbrowse.PostFormatter;
import net.swigglesoft.shackbrowse.PreferenceKeys;
import net.swigglesoft.shackbrowse.R;
import net.swigglesoft.shackbrowse.StatsFragment;
import net.swigglesoft.shackbrowse.TimeDisplay;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.BigTextStyle;
import androidx.core.app.NotificationCompat.InboxStyle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;

public class NotifierReceiver extends FirebaseMessagingService
{
	public static final int icon_res = R.drawable.note_logo2018;

	private static final String TAG = "SBNOTIFIER";

	public static final String CHANNEL_NEWPOST = "sbnotechannel_newpost";
	public static final String CHANNEL_SHACKMSG = "sbnotechannel_shackmsg";
	public static final String CHANNEL_SYSTEM = "sbnotechannel_system";

	private SharedPreferences mPrefs;

	@Override
	public void onMessageReceived(RemoteMessage message){

		Log.i(TAG, "NotifierReceiver invoked, starting service");
		System.out.println("SHACK BROWSE FCM MSG RECEIVE");
		Context context = getApplicationContext();

		mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		boolean notificationsEnabled = mPrefs.getBoolean("noteEnabled", false);
		boolean repliesEnabled = mPrefs.getBoolean(PreferenceKeys.notificationOnReplies, false);
		boolean vanityEnabled = mPrefs.getBoolean(PreferenceKeys.notificationOnVanity, false);

		Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
		Map data = message.getData();

		if (!notificationsEnabled) {
			Log.w(TAG, "Skipping this notification due to notifications not being enabled.");
			return;
		}

		if (data == null || data.get("type") == null) {
			return;
		}

		Log.i(TAG, "Received notification from post for username " + data.get("username").toString() + ", title " + data.get("title").toString());
		if (isOnBlockList(data.get("username").toString()))
		{
			// do not trigger notification
			StatsFragment.statInc(context, "EchoChamberBlockNotification");
			return;
		}

		String notificationType = data.get("type").toString().toLowerCase();
		if (notificationType.equals("reply") || notificationType.equals("mention") || notificationType.equals("keyword"))
		{
			if(!repliesEnabled && notificationType.equals("reply")) {
				Log.w(TAG, "Skipping this notification due to the title starting with Reply and reply notifications are not enabled in settings.");
				return;
			}

			if(!vanityEnabled && notificationType.equals("mention")) {
				Log.w(TAG, "Skipping this notification due to the title starting with Mentioned and vanity notifications are not enabled in settings.");
				return;
			}

			processGeneralNotification(context, largeIcon, data);
		}
		else if (notificationType.equals("shackmsg"))
		{
			processShackmsgNotification(context, largeIcon, data);
		}
		else {
			Log.w("onMessageReceived", "Unknown notification type " + notificationType);
		}
   }

	private void processShackmsgNotification(Context context, Bitmap largeIcon, Map data) {
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(context, NotifierReceiver.CHANNEL_SHACKMSG)
				.setSmallIcon(icon_res)
				.setLargeIcon(largeIcon)
				.setContentTitle(data.get("username").toString() + " sent you a shackmessage")
				.setContentText(data.get("text").toString())
				.setTicker(data.get("username").toString() + " sent you a shackmessage")
				.setColor(Color.GREEN)
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

		PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE);

		mBuilder.setContentIntent(resultPendingIntent);
		Notification notification = mBuilder.build();

		int mId = 58403;
		handleNotification(notification, mId, context);

		Editor editor = mPrefs.edit();
		editor.putString("GCMShackMsgLastNotifiedId", data.get("id").toString());
		editor.commit();

		StatsFragment.statInc(context, "NotifiedShackMessage");
		StatsFragment.statInc(context, "Notifications");
	}

	private void processGeneralNotification(Context context, Bitmap largeIcon, Map data) {
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(context, NotifierReceiver.CHANNEL_NEWPOST)
				.setSmallIcon(icon_res)
				.setLargeIcon(largeIcon)
				.setContentTitle(data.get("title").toString())
				.setContentText(PostFormatter.formatContent(data.get("title").toString(), data.get("text").toString(), null, false, true))
				.setTicker(data.get("title").toString())
				.setColor(Color.GREEN)
				.setAutoCancel(true);

		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(context, MainActivity.class);

		// save notification to db
		NotificationObj n = new NotificationObj(Integer.parseInt(data.get("nlsid").toString()), "general", data.get("text").toString(), data.get("title").toString(), TimeDisplay.now(), "general");
		n.commit(context);

		// check count
		int noteCount = mPrefs.getInt("GCMNoteCountGeneral", 0);
		int numNew = noteCount + 1;
		mBuilder.setNumber(numNew);

		// NLSID
		resultIntent.putExtra("notificationNLSID", Integer.parseInt(data.get("nlsid").toString()));

		// second if checks if we are replacing a notification when the last one was never clicked
		// if so must create a multi reply
		if (numNew  > 1)
		{
			// multiple notifications
			mBuilder.setContentTitle("New Notifications");
			mBuilder.setContentText("Click to show a list");
			mBuilder.setTicker(numNew + " new notifications");
			mBuilder.setStyle(getInboxStyleFor("New notifications", "general", numNew, context));
			resultIntent.putExtra("notificationOpenGList", true);
		}
		else
		{
			mBuilder.setStyle(getBigTextFor(data.get("title").toString(), data.get("text").toString()));
			resultIntent.putExtra("notificationOpenId", true);
		}

		resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);

		// stupid hack for android bug
		resultIntent.setAction(Long.toString(System.currentTimeMillis()));

		PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE);

		mBuilder.setContentIntent(resultPendingIntent);
		mBuilder.setDeleteIntent(getDeleteIntent("GCMNoteCountGeneral", context));
		Notification notification = mBuilder.build();

		// notification snooze
		if (checkIfMuted(Integer.parseInt(data.get("parentid").toString()), mPrefs))
		{
			return;
		}

		int mId = 58401;
		handleNotification(notification, mId, context);

		Editor editor = mPrefs.edit();
		editor.putInt("GCMNoteCountGeneral", numNew);
		editor.commit();

		StatsFragment.statInc(context, "NotifiedGeneral");
		StatsFragment.statInc(context, "Notifications");
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
		if (!mutedList.equals("")) {
			muted.addAll(Arrays.asList(mutedList.split(",")));
		}

		if (mutedList.contains(Integer.toString(postId))) {
			for (int i = 0; i < muted.size(); i++) {
				if (muted.get(i).contentEquals(postIdString)) {
					muted.remove(i);
				}
			}
			saveGCMNoteMuted(prefs, muted);
			return false;
		}

		muted.add(0, postIdString);
		if (muted.size() > 20) {
			muted.remove(muted.size() - 1); // remove last item
		}
		saveGCMNoteMuted(prefs, muted);
		return true;
	}

	private static void saveGCMNoteMuted(SharedPreferences prefs, ArrayList<String> muted) {
		Editor edit = prefs.edit();
		edit.putString("GCMNoteMuted", String.join(",", muted));
		edit.commit();
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
		net.swigglesoft.shackbrowse.NotificationsDB ndb = new NotificationsDB(ctx);
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
		big.bigText(PostFormatter.formatContent("w", text, null, false, true));
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
	}
	private static final String NOTIFICATION_DELETED_ACTION = "net.swigglesoft.shackbrowse.NOTIFICATION_DELETED";
	private PendingIntent getDeleteIntent(String prefKeyToZero, Context ctx)
	{
		Intent intent = new Intent(NOTIFICATION_DELETED_ACTION);
		intent.putExtra("key", prefKeyToZero);
        PendingIntent pendintIntent = PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        return pendintIntent;
	}

	/*
	ECHO CHAMBER
	 */
	public boolean isOnBlockList(String username)
	{
		if (!(mPrefs.contains("echoChamberBlockList") && mPrefs.getBoolean("echoEnabled", false)))
			return false;

		try {
			JSONArray mBlockList = new JSONArray(mPrefs.getString("echoChamberBlockList", "[]"));
			for (int i = 0; i < mBlockList.length(); i++) {
				if (mBlockList.getString(i).equalsIgnoreCase(username))
					return true;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
