package net.woggle.shackbrowse;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
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
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import static net.woggle.shackbrowse.StatsFragment.statInc;
import static net.woggle.shackbrowse.StatsFragment.statMax;

public class PostQueueService extends IntentService {

	private Context ctx;
	private PostQueueDB pdb;
	private SharedPreferences prefs;
    private boolean triggeredPRLStat = false;
	public PostQueueService() {
		super("PQPService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		ctx = getApplicationContext();
		prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		pdb = new PostQueueDB(ctx);
		
		
		if ((intent.hasExtra("appinit")) && (intent.getExtras().getBoolean("appinit", false)))
		{
			pdb.open();
			pdb.cleanUpFinalizedPosts();
			pdb.close();
		}
		else
		{
			SystemClock.sleep(200);
			doQueue();
		}
		
		
	}
	public void doQueue()
	{
		pdb.open();
		List<PostQueueObj> plist = pdb.getAllPostsInQueue(true);
		pdb.close();
		
		// default delay is 500 ms
		int sent = 0;
		long delay = 3000L;
		System.out.println("POSTQU: RUNNING");

        statMax(ctx, "MaximumItemsInQueue", plist.size());
        System.out.println("POSTQU: SIZE " + plist.size());

		while (plist.size() > 0)
		{
			
			PostQueueObj post = plist.get(0);
			{
				// DO REPLY
				JSONObject data;
					// try to post
					if (!post.isMessage())
					{
						if (post.getFinalId() == 0)
						{
							try {
								data = ShackApi.postReply(ctx, post.getReplyToId(), post.getBody(), post.isNews());
							} catch (Exception e) {
								e.printStackTrace();
								networkErrorNotify();
								// quit now, no need to keep trying and failing with network error
								return;
							}
                            // possible source of bug
							if (data != null && data.has("post_insert_id"))
							{
								// successful post
								int reply_id;
								try {
									reply_id = data.getInt("post_insert_id");
									// negative post ids are complete
									post.setFinalId(reply_id);
									post.setFinalizedTime(TimeDisplay.now());
									post.updateFinalId(ctx);
									sent++;
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
								    	notify("Post Successful", sent + " sent. " + remaining + " in queue.", 58410, reply_id);
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

										System.out.println("POSTQU: PRL ERROR, BACKING OFF");
										delay = (delay * 2);

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
									if (data.toString().toLowerCase().contains("banned".toLowerCase()))
									{
										// banned, delete post
										System.out.println("POSTQU: BANNED ERROR");
										post.commitDelete(ctx);

										if (!prefs.getBoolean("isAppForeground", false))
									    {
									    	notify("Post Failure", "You've been banned.", 58412, 0);
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
									if (data.toString().toLowerCase().contains("You must be logged in to post".toLowerCase()))
									{
										// login error, delete post
										System.out.println("POSTQU: LOGON ERROR");
										post.commitDelete(ctx);

										if (!prefs.getBoolean("isAppForeground", false))
									    {
									    	notify("Post Error", "Bad Login. Check shacknews credentials.", 58414, 0);
									    }
									}
									if (data.toString().toLowerCase().contains("Trying to post to a nuked thread!".toLowerCase()))
									{
										// login error, delete post
										System.out.println("POSTQU: NUKE ERROR");
										post.commitDelete(ctx);

										if (!prefs.getBoolean("isAppForeground", false))
									    {
									    	notify("Post Error", "Cannot post to nuked thread.", 58415, 0);
									    }

                                        statInc(ctx, "PostedToNukedThread");
									}

								}
								else
								{

                                    notify("Post Error", "Error X48. Please report this to bradsh.", 58416, 0);

									// unknown error, use exponential backoff
                                    statInc(ctx, "PQPUnknownError");
									delay = (delay * 2);
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
							return;
						}
						if (result)
						{
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
			
			// finished looking at all posts in queue, update list for while loop
			pdb.open();
			plist = pdb.getAllPostsInQueue(true);
			pdb.close();
			
			
			// sleep a little or quit
			if (plist.size() == 0)
				return;
			
			// max delay is 1 minutes
			if (delay > 45000)
				delay = 45000;
			System.out.println("POSTQU: NOW " + plist.size() + " POSTS IN QUEUE< SLEEPING " + delay + "ms");
			SystemClock.sleep(delay);
		}
	}

	public static final int icon_res = R.drawable.note_logo;
	
	private void notify(String title, String text, int mId, int postId) {
		
		Bitmap largeIcon = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_launcher);
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(ctx)
		        .setSmallIcon(icon_res)
		        .setLargeIcon(largeIcon)
		        .setContentTitle(title)
				.setContentText(text)
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
			resultIntent = new Intent();
		}
				
		PendingIntent resultPendingIntent = PendingIntent.getActivity(ctx, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
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
