package net.woggle.shackbrowse.notifier;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;

import net.woggle.shackbrowse.NetworkNotificationServers;


public class FCMIDReceiver extends FirebaseMessagingService {

	private static final String TAG = "MyFirebaseIIDService";

	/**
	 * Called if InstanceID token is updated. This may occur if the security of
	 * the previous token had been compromised. Note that this is called when the InstanceID token
	 * is initially generated so this is where you would retrieve the token.
	 */
	// [START refresh_token]
	@Override
	public void onNewToken(String s) {
		super.onNewToken(s);
		sendRegistrationToServer(s);
		Log.d("NEW_TOKEN",s);
	}
	// [END refresh_token]

	/**
	 * Persist token to third-party servers.
	 *
	 * Modify this method to associate the user's FCM InstanceID token with any server-side account
	 * maintained by your application.
	 *
	 * @param token The new token.
	 */
	private void sendRegistrationToServer(String token) {
		// TODO: Implement this method to send token to your app server.
		NetworkNotificationServers nns = new NetworkNotificationServers(getApplicationContext(), null);
		nns.sendRegistrationIdToBackend();
	}
}