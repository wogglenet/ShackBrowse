package net.woggle.shackbrowse;

import java.util.regex.Pattern;

import net.woggle.ApiUrl;
import net.woggle.shackbrowse.imgur.ImgurAuthorization;
import net.woggle.shackbrowse.imgur.LoginActivity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

public class PreferenceView extends PreferenceFragment
{
	private SharedPreferences _prefs;
	
	protected MaterialDialog _progressDialog;
	private NetworkNotificationServers _GCMAccess;

	private CheckBoxPreference _vanityNotification;

	private CheckBoxPreference _noteEnabled;

	private CheckBoxPreference _repliesNotification;

	private boolean _Venabled;

	private Preference _keyNotification;
    private CheckBoxPreference mChattyPicsEnable;
	private boolean mLoggedIn;


	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        doOrientation(-1);
        
        _prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        
        addPreferencesFromResource(R.xml.preferences);
        
        String versionName = "App Version Unknown";
        try {
			versionName = getActivity().getApplication().getPackageManager().getPackageInfo(getActivity().getApplication().getPackageName(), 0 ).versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        Preference customPref = (Preference) findPreference("versionName");
        customPref.setTitle("Shack Browse V. " + versionName);
        final Context cont = getActivity();
        customPref.setOnPreferenceClickListener(new OnPreferenceClickListener(){

			@Override
			public boolean onPreferenceClick(Preference preference) {
				ChangeLog cl = new ChangeLog(cont);
		        cl.getFullLogDialog().show();
				return false;
			}}
        );
        /*
        Preference testNote = (Preference) findPreference("pref_testnote");
        testNote.setOnPreferenceClickListener(new OnPreferenceClickListener(){

			@Override
			public boolean onPreferenceClick(Preference preference) {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
				
				Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_old);
				NotificationCompat.Builder mBuilder =
				        new NotificationCompat.Builder(getActivity())
				        .setSmallIcon(R.drawable.note_logo)
				        .setLargeIcon(largeIcon)
				        .setContentTitle("Test")
				.setContentText("Only a test")
				.setTicker("Test Notification")
				.setAutoCancel(true);
				
				NotificationManager mNotificationManager =
					    (NotificationManager)getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
				
				PendingIntent notifyPIntent = PendingIntent.getActivity(getActivity().getApplicationContext(), 0, new Intent(), 0);
				mBuilder.setContentIntent(notifyPIntent);
				
				Notification notification = mBuilder.build();
				notification.sound = Uri.parse(prefs.getString("notificationSound", "DEFAULT_SOUND"));
				
				if (prefs.getBoolean("notificationVibrate", true))
					notification.defaults|= Notification.DEFAULT_VIBRATE;
				
				notification.flags |= Notification.FLAG_AUTO_CANCEL;
				notification.flags |= Notification.FLAG_SHOW_LIGHTS;
				
				notification.ledARGB = prefs.getInt("notificationColor", Color.GREEN);
				notification.ledOffMS = 1600;
				notification.ledOnMS = 100;
				// mId allows you to update the notification later on.
				int mId = 58401;
				mNotificationManager.notify(mId, notification);
				
				return false;
			}}
        );
        */


	    Preference imgurLogin = (Preference) findPreference("imgurLogin");
	    imgurLogin.setOnPreferenceClickListener(new OnPreferenceClickListener(){
		    @Override
		    public boolean onPreferenceClick(Preference preference) {
			    if (mLoggedIn)
			    {
				    ImgurAuthorization.getInstance().logout();
				    Toast.makeText(getActivity(), "Logged out of Imgur", Toast.LENGTH_SHORT).show();
				    setImgurLoginText();
			    }
			    else
			    {
				    startActivity(new Intent(getActivity(), LoginActivity.class));
			    }
			    return true;
		    }}
	    );



	    Preference bAutoImageZoomPref = (Preference) findPreference("openBrowserImageZoom");
        bAutoImageZoomPref.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference preference) {
                ((MainActivity)getActivity()).openBrowserZoomAdjust();
				return true;
			}}
        );


        
        Preference orientLock = (Preference)findPreference("orientLock");
        orientLock.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				doOrientation(Integer.parseInt((String)newValue));
				return true;
			}
					
		});
        
        Preference fontZoom = (Preference)findPreference("fontZoom");
        fontZoom.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		        builder.setTitle("Font Size Change");
		        builder.setMessage("Changing the font size requires an app restart.");
		        builder.setPositiveButton("Restart Now", new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int id) {
                        ((MainActivity)getActivity()).restartApp();
		            }
		        });
		        builder.setNegativeButton("Deal with Bugs", null);
		        builder.create().show();
				return true;
			}
					
		});

        Preference appColor = (Preference)findPreference("appTheme");
        appColor.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("App Theme");
                builder.setMessage("Changing the app theme requires an app restart.");
                builder.setPositiveButton("Restart Now", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ((MainActivity)getActivity()).restartApp();
                    }
                });
                builder.setNegativeButton("Later", null);
                builder.create().show();
                return true;
            }

        });

        Preference notePref = (Preference) findPreference("notifications");
        notePref.setOnPreferenceClickListener(new OnPreferenceClickListener(){

			@Override
			public boolean onPreferenceClick(Preference preference) {
                ((MainActivity)getActivity()).cleanUpViewer();
                ((MainActivity)getActivity()).setContentTo(MainActivity.CONTENT_NOTEPREFS);
		    	return false;
			}}
        );

        mChattyPicsEnable = (CheckBoxPreference)findPreference("enableChattyPics");
        mChattyPicsEnable.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                PackageManager pm = getActivity().getPackageManager();
                ComponentName compName =  new ComponentName(getActivity().getApplicationContext(), PicUploader.class);
                final Boolean checked = (Boolean)newValue;
                pm.setComponentEnabledSetting(compName, checked ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                return true;
            }
        });
        PackageManager pm = getActivity().getPackageManager();
        ComponentName compName =  new ComponentName(getActivity().getApplicationContext(), PicUploader.class);
        if ((pm.getComponentEnabledSetting(compName) == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) || (pm.getComponentEnabledSetting(compName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED))
        {
            mChattyPicsEnable.setChecked(true);
        }
        else
        {
            mChattyPicsEnable.setChecked(false);
        }

        Preference pingPref = (Preference) findPreference("pref_ping");
        pingPref.setOnPreferenceClickListener(new OnPreferenceClickListener(){

			@Override
			public boolean onPreferenceClick(Preference preference) {
				System.out.println("pinging");
		    	new PingTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		    	return false;
			}}
        );
    }

    @Override
    public void onResume()
    {
	    super.onResume();
	    setImgurLoginText();
    }

    public void setImgurLoginText() {
	    Preference imgurLogin = (Preference) findPreference("imgurLogin");
	    mLoggedIn = ImgurAuthorization.getInstance().isLoggedIn();
	    if (mLoggedIn) {
		    imgurLogin.setTitle("Click to log out");
	    }
	    else {
		    imgurLogin.setTitle("Click to log in");
	    }
	    imgurLogin.setSummary(getResources().getString(R.string.preference_imgur_login_summary) + " Currently uploading " + (mLoggedIn ? "as \"" + ImgurAuthorization.getInstance().getUsername() + "\" - uploads will appear in your Imgur account." : "anonymously - cannot delete uploads."));
    }
	
	final static int OPEN_BROWSER_ZOOM_SETUP = 37;
	final static int RESTART_APP = 38;
    public void doOrientation (int _orientLock) 
    {
    	if (_orientLock == -1)
    	{
	    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
	        _orientLock = Integer.parseInt(prefs.getString("orientLock", "0"));
    	}
        
        if (_orientLock == 0)
        	getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        if (_orientLock == 1)
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (_orientLock == 2)
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if (_orientLock == 3)
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        if (_orientLock == 4)
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    }

	/*
	 * 
	 * PING TASK
	 * 
	 */
	
    class PingTask extends AsyncTask<Void, Void, String>
	{
	    Exception _exception;
		private String _taskMode;
		
        @Override
        protected String doInBackground(Void... params)
        {
            try
            {
                getActivity().runOnUiThread(new Runnable(){
            		@Override public void run()
            		{
            			_progressDialog = MaterialProgressDialog.show(getActivity(), "Pinging", "Communicating with servers...", true, true, new OnCancelListener() {
							
							@Override
							public void onCancel(DialogInterface dialog) {
								cancel(true);
								
							}
						});
            		}
            	});
            	
            	// warm up the servers
            	ShackApi.getPosts(3000000, getActivity(), new ApiUrl(ShackApi.BASE_URL, false));
            	if (isCancelled()) 
            	    return null;
            	ShackApi.getPosts(3000000, getActivity(), new ApiUrl(ShackApi.BASE_URL_ALT, false));
            	if (isCancelled()) 
            	    return null;
            	ShackApi.getPosts(3000000, getActivity(), new ApiUrl(ShackApi.BASE_URL_ALT2, false));
            	if (isCancelled())
            	    return null;
            	
            	Long current = TimeDisplay.now();
            	ShackApi.getPosts(3000000, getActivity(), new ApiUrl(ShackApi.BASE_URL, false));
            	float hughes = (TimeDisplay.now() - current);
            	if (isCancelled()) 
            	    return null;
            	
            	current = TimeDisplay.now();
            	ShackApi.getPosts(3000000, getActivity(), new ApiUrl(ShackApi.BASE_URL_ALT, false));
            	float woggle = (TimeDisplay.now() - current);
            	if (isCancelled()) 
            	    return null;
            	
            	current = TimeDisplay.now();
            	ShackApi.getPosts(3000000, getActivity(), new ApiUrl(ShackApi.BASE_URL_ALT2, false));
            	float appspot = (TimeDisplay.now() - current);
            	if (isCancelled()) 
            	    return null;
            	
            	current = TimeDisplay.now();
            	ShackApi.getPosts(3000000, getActivity(), new ApiUrl(ShackApi.WINCHATTYV2_API, true));
            	float winchatty = (TimeDisplay.now() - current);
            	if (isCancelled()) 
            	    return null;
            	
            	current = TimeDisplay.now();
            	ShackApi.getPosts(3000000, getActivity(), new ApiUrl(ShackApi.WOGGLEV2_API, true));
            	float woggle2 = (TimeDisplay.now() - current);
            	if (isCancelled()) 
            	    return null;
            	
                return hughes + " " + woggle + " " + appspot + " " + winchatty + " " + woggle2;
                
            }
            catch (Exception e)
            {
                Log.e("shackbrowse", "Error pinging", e);
                _exception = e;
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(final String result)
        {
        	try {
        		Handler handler = new Handler();
        		handler.postDelayed(new Runnable() {
        		    public void run() {
        		    	_progressDialog.dismiss();
        		    	
        		    	if (result == null)
        		    		return;
        		    	
        		    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        		    	builder.setTitle("API Average Time for getPosts");
        		    	// Set up the input
        		    	String[] pingbits = result.split(Pattern.quote(" "));
        				builder.setMessage("arhughes: " + pingbits[0] + "\n" + "woggle: " + pingbits[1] + "\n" +"appspot: " + pingbits[2] + "\n" +"winchatty V2: " + pingbits[3] + "\n"+"woggle V2: " + pingbits[4] + "\n");
        				builder.setNegativeButton("Close", null);
        		    	AlertDialog alert = builder.create();
        		        alert.setCanceledOnTouchOutside(false);
        		        alert.show();
        		        
        		    }}, 500);  // 700 milliseconds
        	}
        	catch (Exception e)
        	{
        		
        	}
            
            if (_exception != null)
            {
                ErrorDialog.display(getActivity(), "Error", "Error pinging:\n" + _exception.getMessage());
            }
            else if (result == null)
            {
            	System.out.println("pushreg: err");
                ErrorDialog.display(getActivity(), "Error", "Unknown ping error.");
            }
        }
	}

}
