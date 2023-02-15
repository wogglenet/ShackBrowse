package net.swigglesoft.shackbrowse;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import net.swigglesoft.shackbrowse.notifier.NotifierReceiver;

import static net.swigglesoft.shackbrowse.StatsFragment.statInc;
import static net.swigglesoft.shackbrowse.StatsFragment.statMax;

public class PostQueueService extends JobIntentService
{


	private Context ctx;
	private PostQueueDB pdb;
	private SharedPreferences prefs;
    private boolean triggeredPRLStat = false;

	static final int JOB_ID = 1156;
	private List<PostQueueObj> mPlist;

	static void enqueueWork(Context context, Intent work)
	{
		enqueueWork(context, PostQueueService.class, JOB_ID, work);
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		ctx = getApplicationContext();
		prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

		pdb = new PostQueueDB(ctx);
	}

	@Override
	public boolean onStopCurrentWork()
	{
		return true;
	}

	@Override
	protected void onHandleWork(Intent intent)
	{
		if ((intent.hasExtra("appinit")) && (intent.getExtras().getBoolean("appinit", false)))
		{
			pdb.open();
			pdb.cleanUpFinalizedPosts();
			pdb.close();
		}
		else if (NetworkChangeReceiver.NetworkUtil.getConnectivityStatus(ctx) != NetworkChangeReceiver.NetworkUtil.TYPE_NOT_CONNECTED)
		{
			SystemClock.sleep(200);
			doQueue();
		}
		// if the device is not connected, dont bother trying to post
		
	}
	public void doQueue()
	{
		// get list
		pdb.open();
		mPlist = pdb.getAllPostsInQueue(true);
		pdb.close();
		
		// default delay is 500 ms
		long delay = 3000L;
		System.out.println("POSTQU: RUNNING");

        statMax(ctx, "MaximumItemsInQueue",mPlist.size());
        System.out.println("POSTQU: SIZE " +mPlist.size());

		while (mPlist.size() > 0)
		{
			int returnval = iterateOnPostQueue(ctx);

			if (returnval == 3)
			{
				delay = delay * 2L;
			}
			// sleep a little or quit
			if (mPlist.size() == 0)
				return;
			
			// max delay is 1 minutes
			if (delay > 45000L)
				delay = 45000L;
			System.out.println("POSTQU: NOW " +mPlist.size() + " POSTS IN QUEUE< SLEEPING " + delay + "ms");
			if (isStopped ())
			{
				return;
			}
			SystemClock.sleep(delay);
		}
	}



	public int iterateOnPostQueue (Context ctx)
	{
		// ret = 1 == sent item ok, ret = 0 == send failed, ret = 2 == network error, ret = 3 == PRL, ret = 4 == banned, delete queue, ret = 5 == login error, ret = 6 == post to nuked thread
		// ret = 7 = msg sent ok
		// ret == 9 = content type id error
		int ret = 0;
		if (prefs == null) { prefs = PreferenceManager.getDefaultSharedPreferences(ctx); }
		if (pdb == null) { pdb = new PostQueueDB(ctx); }
		pdb.open();
		mPlist = pdb.getAllPostsInQueue(true);
		pdb.close();

		if (mPlist.size() > 0)
		{
			PostQueueObj post = mPlist.get(0);
			{
				// DO REPLY
				JSONObject data;
				// try to post
				if (!post.isMessage())
				{
					if (post.getFinalId() == 0)
					{
						try {
							data = ShackApi.postReply(ctx, post.getReplyToId(), post.getBody(), post.getContentTypeId());
						} catch (java.io.FileNotFoundException e) {
							e.printStackTrace();
							networkErrorNotify();
							// quit now, no need to keep trying and failing with network error
							System.out.println("POSTQU: FILENOTFOUND");
							post.commitDelete(ctx);
							if (!prefs.getBoolean("isAppForeground", false))
							{
								notify("Post Failure", "FILE NOT FOUND Error?? Deleted from queue, sorry.", 58412, 0);
							}
							return 9;
						} catch (Exception e) {
							e.printStackTrace();
							networkErrorNotify();
							// quit now, no need to keep trying and failing with network error
							return 2;
						}
						// possible source of bug
						if (data != null && data.has("post_insert_id"))
						{
							// successful post
							ret = 1;
							int reply_id;
							try {
								reply_id = data.getInt("post_insert_id");
								// negative post ids are complete
								post.setFinalId(reply_id);
								post.setFinalizedTime(TimeDisplay.now());
								post.updateFinalId(ctx);

								System.out.println("POSTQU: SUCCESSFUL POST");

								pdb.open();
								int remaining = pdb.getAllPostsInQueue(true).size();
								pdb.close();

								// send info back to main app
								Intent localIntent = new Intent(MainActivity.PQPSERVICESUCCESS)
										// Puts the status into the Intent
										.putExtra("PQPId", post.getPostQueueId())
										.putExtra("finalId", reply_id)
										.putExtra("wasRootPost", (post.getReplyToId() == 0) ? true : false)
										.putExtra("isMessage", false)
										.putExtra("isPRL", false)
										.putExtra("remaining", remaining);
								// Broadcasts the Intent to receivers in this app.
								LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

								if (!prefs.getBoolean("isAppForeground", false))
								{
									notify("Post Successful", "Post sent. " + remaining + " in queue.", 58410, reply_id);
								}

								// stats
								if (post.getReplyToId() == 0)
									statInc(ctx, "PostedThread");
								else
									statInc(ctx, "PostedReply");

							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								// this should never happen. the app should crash
								throw new RuntimeException(e);
							}
						}
						else
						{
							ret = 0;
							// post failed, find out why
							if (data.has("error"))
							{
								pdb.open();
								int remaining = pdb.getAllPostsInQueue(true).size();
								pdb.close();

								if (data.toString().toLowerCase().contains("Please wait a few minutes before trying to post again".toLowerCase()))
								{
									// PRL
									if (!triggeredPRLStat) {
										statInc(ctx, "TimesPRLed");
										triggeredPRLStat = true;
									}

									ret = 3;
									System.out.println("POSTQU: PRL ERROR, BACKING OFF");

									// send info back to main app
									Intent localIntent = new Intent(MainActivity.PQPSERVICESUCCESS)
											// Puts the status into the Intent
											.putExtra("PQPId", post.getPostQueueId())
											.putExtra("isMessage", false)
											.putExtra("isPRL", true)
											.putExtra("remaining", remaining);
									// Broadcasts the Intent to receivers in this app.
									LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

									if (!prefs.getBoolean("isAppForeground", false))
									{
										notify("Post PRL'd", "Will Retry. " + remaining + " posts in queue.", 58411, 0);
									}
								}
								else if (data.toString().toLowerCase().contains("banned".toLowerCase()))
								{
									ret = 4;
									// banned, delete post
									System.out.println("POSTQU: BANNED ERROR");
									post.commitDelete(ctx);

									if (!prefs.getBoolean("isAppForeground", false))
									{
										notify("Post Failure", "You've been banned", 58412, 0);
									}
								}
	                                    /*
										if (data.toString().toLowerCase().contains("fixup_postbox_parent_for_remove(".toLowerCase()))
										{
											// server error
											System.out.println("POSTQU: SERVER ERROR");
											delay = (delay * 2);

											if (!prefs.getBoolean("isAppForeground", false))
										    {
										        notify("Post Error", "Will Retry. " + remaining + " posts in queue.", 58413, 0);
										    }
										}
										*/
								else if (data.toString().toLowerCase().contains("You must be logged in to post".toLowerCase()))
								{
									ret = 5;
									// login error, delete post
									System.out.println("POSTQU: LOGON ERROR");
									post.commitDelete(ctx);

									if (!prefs.getBoolean("isAppForeground", false))
									{
										notify("Post Error", "Bad Login. Check shacknews credentials.", 58414, 0);
									}
								}
								else if (data.toString().toLowerCase().contains("Trying to post to a nuked thread".toLowerCase()))
								{
									ret = 6;
									// login error, delete post
									System.out.println("POSTQU: NUKE ERROR");
									post.commitDelete(ctx);

									if (!prefs.getBoolean("isAppForeground", false))
									{
										notify("Post Error", "Cannot post to nuked thread.", 58415, 0);
									}

									statInc(ctx, "PostedToNukedThread");
								}
								else if (data.toString().toLowerCase().contains("content type or parent do not match"))
								{
									System.out.println("POSTQU: CONTENT TYPE ERROR");
									System.out.println("POSTQU: E: " + data.toString());
									post.commitDelete(ctx);
									notify("Post Failure", "Content ID Type Error (" + post.getContentTypeId() + "). Your post was deleted. Sorry.", 58412, 0);

									return 9;
								}
								else {
									notify("Post Error", "Error X51. Please report this. E: " + data.toString(), 58416, 0);
									System.out.println("POSTQU: E: " + data.toString());
									throw new RuntimeException(new Exception());
								}
							}
							else
							{

								notify("Post Error", "Error X48. Please report this. E: " + data.toString(), 58416, 0);
								System.out.println("POSTQU: E: " + data.toString());
								throw new RuntimeException(new Exception());
								// unknown error, use exponential backoff
								// statInc(ctx, "PQPUnknownError");

							}
						}
					}

				}
				else
				{
					// is message, send message!
					boolean result = false;
					try {
						result = ShackApi.postMessage(PostQueueService.this, post.getSubject(), post.getRecipient(), post.getBody());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						networkErrorNotify();
						// quit now, no need to keep trying and failing with network error
						return 2;
					}
					if (result)
					{
						ret = 7;
						System.out.println("POSTQU: SENT MESSAGE");
						post.commitDelete(ctx);

						// send info back to main app
						pdb.open();
						Intent localIntent = new Intent(MainActivity.PQPSERVICESUCCESS)
								// Puts the status into the Intent
								.putExtra("PQPId", post.getPostQueueId())
								.putExtra("isMessage", true)
								.putExtra("isPRL", false)
								.putExtra("remaining", pdb.getAllPostsInQueue(true).size());
						pdb.close();
						// Broadcasts the Intent to receivers in this app.
						LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

						if (!prefs.getBoolean("isAppForeground", false))
						{
							notify("ShackMessage Sent", "Your SM was successfully sent in the background.", 58415, 0);
						}

						statInc(ctx, "SentShackMessage");
					}
				}
			}
		}
		return ret;
	}

	public static final int icon_res = R.drawable.note_logo2018;

	private void notify(String title, String text, int mId, int postId) {

		Bitmap largeIcon = BitmapFactory.decodeResource(ctx.getResources(), R.mipmap.ic_launcher);
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(ctx, NotifierReceiver.CHANNEL_SYSTEM)
						.setSmallIcon(icon_res)
						.setLargeIcon(largeIcon)
						.setContentTitle(title)
						.setContentText(text)
						.setColor(Color.GREEN)
						.setTicker(title +": "+ text)
						.setAutoCancel(true);

		NotificationManager mNotificationManager = (NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);

		// Creates an explicit intent for an Activity in your app
		Intent resultIntent;
		if (postId > 0)
		{
			resultIntent = new Intent(ctx, MainActivity.class);
			resultIntent.setAction(Intent.ACTION_VIEW);
			Uri data = Uri.parse("http://www.shacknews.com/chatty?id="+postId);
			resultIntent.setData(data);
		}
		else
		{
			resultIntent = new Intent(ctx, MainActivity.class);
			resultIntent.putExtra("notificationOpenPostQueue", true);
		}

		PendingIntent resultPendingIntent = PendingIntent.getActivity(ctx, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE);
		mBuilder.setContentIntent(resultPendingIntent);

		Notification notification = mBuilder.build();

		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		notification.ledARGB = Color.GREEN;
		notification.ledOffMS = 1500;
		notification.ledOnMS = 100;
		// mId allows you to update the notification later on.



		mNotificationManager.notify(mId, notification);
	}
	private void networkErrorNotify()
	{
		// TODO Auto-generated catch block
		// likely a network error
		System.out.println("POSTQU: NETWORK ERROR");

		if (!prefs.getBoolean("isAppForeground", false))
		{
			pdb.open();
			notify("Network Error Posting", "Will retry when reconnected. " + pdb.getAllPostsInQueue(true).size() + " posts in queue.", 58416, 0);
			pdb.close();
		}

		// just quit. will call service when network is back
	}
}
