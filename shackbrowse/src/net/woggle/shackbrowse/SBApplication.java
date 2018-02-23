package net.woggle.shackbrowse;

import android.app.Application;

import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.tweetui.TweetUi;

import java.lang.*;


/**
 * Created by brad on 2/17/2018.
 */

public class SBApplication extends Application
{
	public void onCreate() {
		super.onCreate();
		// Stetho.initializeWithDefaults(this);
		Twitter.initialize(this);

	}
}