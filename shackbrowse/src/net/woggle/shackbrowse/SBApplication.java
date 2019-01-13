package net.woggle.shackbrowse;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.tweetui.TweetUi;

import java.lang.*;


/**
 * Created by brad on 2/17/2018.
 */

public class SBApplication extends Application
{
	private static Context context;
	private static final String TAG = SBApplication.class.getSimpleName();

	public void onCreate() {
		super.onCreate();
		// Stetho.initializeWithDefaults(this);
		Twitter.initialize(this);
		SBApplication.context = getApplicationContext();
	}
	public static Context getAppContext() {
		return SBApplication.context;
	}

	public static String getVersionName() {
		try {
			return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "Package name not found.", e);
			return "1.0";
		}
	}
}