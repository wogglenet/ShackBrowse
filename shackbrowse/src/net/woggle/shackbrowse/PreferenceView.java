package net.woggle.shackbrowse;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import net.woggle.ApiUrl;
import net.woggle.shackbrowse.NetworkNotificationServers.OnGCMInteractListener;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.NavUtils;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialogCompat;

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

	
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        doOrientation(-1);
        
        _prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        
        addPreferencesFromResource(R.xml.preferences);
       // getActionBar().setDisplayHomeAsUpEnabled(true);
        
        String versionName = "Unknown";
        try {
			versionName = getActivity().getApplication().getPackageManager().getPackageInfo(getActivity().getApplication().getPackageName(), 0 ).versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        Preference customPref = (Preference) findPreference("versionName");
        customPref.setTitle("Version " + versionName);
        final Context cont = getActivity();
        customPref.setOnPreferenceClickListener(new OnPreferenceClickListener(){

			@Override
			public boolean onPreferenceClick(Preference preference) {
				ChangeLog cl = new ChangeLog(cont);
		        cl.getFullLogDialog().show();
				return false;
			}}
        );
        
        Preference testNote = (Preference) findPreference("pref_testnote");
        testNote.setOnPreferenceClickListener(new OnPreferenceClickListener(){

			@Override
			public boolean onPreferenceClick(Preference preference) {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
				
				Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
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
        
        Preference bAutoImageZoomPref = (Preference) findPreference("openBrowserImageZoom");
        bAutoImageZoomPref.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference preference) {
                ((MainActivity)getActivity()).openBrowserZoomAdjust();
				return true;
			}}
        );
        
        Preference donatePref = (Preference) findPreference("pref_donate");
        donatePref.setOnPreferenceClickListener(new OnPreferenceClickListener(){

			@Override
			public boolean onPreferenceClick(Preference preference) {
				// hey, thats the same thing I just wrote!
                Intent i = new Intent(cont, DonateActivity.class);
                startActivity(i);
				return false;
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
        
        final OnGCMInteractListener GCMlistener = new OnGCMInteractListener(){
			@Override
			public void networkResult(String res) {
				System.out.println("NETWORKSERVERS RESULT" + res);
				Editor edit = _prefs.edit();
				if (res.contains("remove device"))
				{
					edit.putBoolean("noteEnabled", false);
					_noteEnabled.setChecked(false);
					_vanityNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
					_keyNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
					if (_progressDialog != null)
					{
						_progressDialog.dismiss();
						_progressDialog = null;
					}
				}
				else if (res.contains("add device"))
				{
					edit.putBoolean("noteEnabled", true);
					_noteEnabled.setChecked(true);
					_GCMAccess.doUserInfoTask();
					/*
					_vanityNotification.setChecked(false);
					_repliesNotification.setChecked(true);
					edit.putBoolean("noteReplies", true);
					edit.putBoolean("noteVanity", false); */
					_vanityNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
					_keyNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
				}
				else if (res.contains("already exists"))
				{
					// likely a vanity.repl update
					_GCMAccess.doUserInfoTask();
				}
				else
				{
					if (_progressDialog != null)
					{
						_progressDialog.dismiss();
						_progressDialog = null;
					}
				}
				edit.commit();
			}

			@Override
			public void userResult(JSONObject result) {
				Editor edit = _prefs.edit();
				try{
					if (result == null)
					{
						edit.putBoolean("noteVanity", false);
						_vanityNotification.setChecked(false);
						edit.putBoolean("noteReplies", false);
						_repliesNotification.setChecked(false);
						_noteEnabled.setChecked(false);
						_vanityNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
						_keyNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
						mNoteKeywords = new ArrayList<String>();
						edit.putBoolean("noteEnabled", false);
					}
					else
					{
						System.out.println("TRYING TO READ USERINFO" + result.getString("get_vanity") + result.getString("get_replies"));
						if ("1".equals(result.getString("get_vanity")))
						{
							edit.putBoolean("noteVanity", true);
							_vanityNotification.setChecked(true);
						}
						else
						{
							edit.putBoolean("noteVanity", false);
							_vanityNotification.setChecked(false);
						}
						if ("1".equals(result.getString("get_replies")))
						{
							edit.putBoolean("noteReplies", true);
							_repliesNotification.setChecked(true);
						}
						else
						{
							edit.putBoolean("noteReplies", false);
							_repliesNotification.setChecked(false);
						}
						if (result.getJSONArray("devices").join("::").contains(_GCMAccess.getRegistrationId()))
						{
							edit.putBoolean("noteEnabled", true);
							_noteEnabled.setChecked(true);
							_vanityNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
							_keyNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
						}
						else
						{
							edit.putBoolean("noteEnabled", false);
							_noteEnabled.setChecked(false);
							_vanityNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
							_keyNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
						}
						mNoteKeywords = new ArrayList<String>();
						JSONArray keywordArr = result.getJSONArray("keywords");
						if ((keywordArr != null) && (keywordArr.length() > 0))
						{
							for (int i=0;i<keywordArr.length();i++)
							{ 
								mNoteKeywords.add(keywordArr.get(i).toString());
							} 
						}
					}
				} catch (Exception e) {}
				edit.commit();
				if (_progressDialog != null)
				{
					_progressDialog.dismiss();
					_progressDialog = null;
				}
			}};
        
        Preference notePref = (Preference) findPreference("notifications");
        notePref.setOnPreferenceClickListener(new OnPreferenceClickListener(){

			@Override
			public boolean onPreferenceClick(Preference preference) {
	        	_progressDialog = MaterialProgressDialog.show(getActivity(), "Checking Notification Status", "Communicating with Shack Browse server...", true, true);
	        	_GCMAccess = new NetworkNotificationServers(getActivity(), GCMlistener);
	        	_GCMAccess.doUserInfoTask();
				
		    	return false;
			}}
        );

        _keyNotification = (Preference) findPreference("noteKeywords");
        _keyNotification.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference preference) {
				showKeywords();
				return true;
				
			}
        });
        
        _repliesNotification = (CheckBoxPreference)findPreference("noteReplies");
        _vanityNotification = (CheckBoxPreference)findPreference("noteVanity");
        _noteEnabled = (CheckBoxPreference)findPreference("noteEnabled");
        // "enableDonatorFeatures"
        _Venabled = true;
        _vanityNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
        _keyNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
        
			
        OnPreferenceChangeListener notificationOnPrefListener = new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(final Preference preference, Object newValue) {
		        if(newValue instanceof Boolean){
		            final Boolean checked = (Boolean)newValue;
		            
	                    boolean verified = _prefs.getBoolean("usernameVerified", false);
	    		        if (!verified)
	    		        {
	    		        	LoginForm login = new LoginForm(getActivity());
	    		        	login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {
	    						
								@Override
	    						public void onSuccess() {
	    							_progressDialog = MaterialProgressDialog.show(getActivity(), "Changing Notification Status", "Communicating with Shack Browse server...", true, true);
	    							
	    							_GCMAccess = new NetworkNotificationServers(getActivity(), GCMlistener);
	    							if (checked)
	    								_GCMAccess.doRegisterTask("reg");
	    							else
	    								_GCMAccess.doRegisterTask("unreg");
	    						}

	    						@Override
	    						public void onFailure() {
	    							_noteEnabled.setChecked(false);
	    							_vanityNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
	    							_keyNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
	    						}
	    					});
	    		        }
	    		        else
	    		        {
							_progressDialog = MaterialProgressDialog.show(getActivity(), "Changing Notification Status", "Communicating with Shack Browse server...", true, true);
							
							_GCMAccess = new NetworkNotificationServers(getActivity(), GCMlistener);
							if (checked)
								_GCMAccess.doRegisterTask("reg");
							else
								_GCMAccess.doRegisterTask("unreg");
	    		        }
		        }
		        return false;
		    }
					
		};
        
        _noteEnabled.setOnPreferenceChangeListener(notificationOnPrefListener);
        
        OnPreferenceChangeListener replOnPrefListener = new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(final Preference preference, Object newValue) {
		        if(newValue instanceof Boolean){
		            final Boolean checked = (Boolean)newValue;
							_progressDialog = MaterialProgressDialog.show(getActivity(), "Changing Notification Status", "Communicating with Shack Browse server...", true, true);
							_GCMAccess = new NetworkNotificationServers(getActivity(), GCMlistener);
							if (checked)
								_GCMAccess.updReplVan(true,_prefs.getBoolean("noteVanity", false));
							else
								_GCMAccess.updReplVan(false,_prefs.getBoolean("noteVanity", false));
		        }
		        return false;
		    }	
		};
		OnPreferenceChangeListener vanOnPrefListener = new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(final Preference preference, Object newValue) {
		        if(newValue instanceof Boolean){
		            final Boolean checked = (Boolean)newValue;
							_progressDialog = MaterialProgressDialog.show(getActivity(), "Changing Notification Status", "Communicating with Shack Browse server...", true, true);
							_GCMAccess = new NetworkNotificationServers(getActivity(), GCMlistener);
							if (checked)
								_GCMAccess.updReplVan(_prefs.getBoolean("noteReplies", true), true);
							else
								_GCMAccess.updReplVan(_prefs.getBoolean("noteReplies", true), false);
		        }
		        return false;
		    }	
		};
        _repliesNotification.setOnPreferenceChangeListener(replOnPrefListener);
        _vanityNotification.setOnPreferenceChangeListener(vanOnPrefListener);
        
        final Preference apiUrl = (Preference)findPreference("apiUrl2");
        apiUrl.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				int i = 0;
				try 
				{
					i = Integer.parseInt((String) newValue);
				}
				catch (NumberFormatException e)
				{}
				
				if (i == 2)
				{
					AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			        builder.setTitle("Custom API");
			        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_customapi, null);
			        final EditText input = (EditText) view.findViewById(R.id.apitext);
			        input.setText(_prefs.getString("apiCustom", ""));
			        builder.setView(view);
			        builder.setPositiveButton("Set API", new DialogInterface.OnClickListener() {
			            public void onClick(DialogInterface dialog, int id) {
			            	SharedPreferences.Editor editor = _prefs.edit();
			            	editor.putString("apiCustom", input.getText().toString());
			            	editor.commit();
			            }
			        });
			        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			            @SuppressLint("NewApi")
						public void onClick(DialogInterface dialog, int id) {
			            	
			            	SharedPreferences.Editor editor = apiUrl.getEditor();
			            	editor.remove("apiUrl2");
			            	editor.commit();
			            	if (android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.HONEYCOMB) {
                                // reload preference fragment
                                ((MainActivity)getActivity()).setContentTo(MainActivity.CONTENT_PREFS);
			            	}
			            }
			        });
			        AlertDialog d = builder.create();
			        d.show();
				}
				return true;
			}
					
		});
        
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
	 * KEYWORDS
	 */
	ArrayList<String> mNoteKeywords = new ArrayList<String>();
	public void addKeyword()
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setTitle("Add Keyword Notification");
    	// Set up the input
    	final LinearLayout lay = new LinearLayout(getActivity());
    	lay.setOrientation(LinearLayout.VERTICAL);
    	final TextView tv = new TextView(getActivity());
    	tv.setText("A notification will be sent to you whenever this keyword is posted by another user.");
		final EditText input = new EditText(getActivity());
		// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
		lay.addView(tv);
		lay.addView(input);
		builder.setView(lay);
		
    	builder.setPositiveButton("Add Keyword", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mNoteKeywords.add(input.getText().toString());
				_progressDialog = MaterialProgressDialog.show(getActivity(), "Adding Keyword", "Communicating with Shack Browse server...", true, true);
				_GCMAccess.doUserInfoTask("addkeyword", input.getText().toString());
				showKeywords();
			}
		});
    	builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showKeywords();
			}
		});
    	AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }
	public void removeKeyword(final String keyword)
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setTitle("Remove Notification Keyword");
    	
    	builder.setMessage("Stop notifications for " + keyword + "?");
		
    	builder.setPositiveButton("Stop Notifying", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mNoteKeywords.remove(keyword);
				_GCMAccess.doUserInfoTask("removekeyword", keyword);
			}
		});
    	builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showKeywords();
			}
		});
    	AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }
    public void showKeywords()
    {
    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Keyword Notifications");
        final CharSequence[] items = mNoteKeywords.toArray(new CharSequence[mNoteKeywords.size()]);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
            	removeKeyword(mNoteKeywords.get(item));
                }});
        builder.setNegativeButton("Close", null);
        builder.setPositiveButton("Add Keyword", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				addKeyword();
			}
		});
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
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
        		    	
        		    	MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(getActivity());
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
