package net.woggle.shackbrowse;

import android.app.Application;

import com.facebook.stetho.Stetho;

/**
 * Created by brad on 2/17/2018.
 */

public class SBApplication extends Application
{
	public void onCreate() {
		super.onCreate();
		// Stetho.initializeWithDefaults(this);
	}
}