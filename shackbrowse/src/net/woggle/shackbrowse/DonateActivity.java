package net.woggle.shackbrowse;

import java.util.LinkedList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;


/**
 */
public class DonateActivity extends ActionBarActivity {
    // Debug tag, for logging
    static final String TAG = "ShackBrowseDonate";

    // Does the user have the premium upgrade?
    boolean mIsUnlocked = false;
    boolean mHasChecked = false;



    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10001;

	public SharedPreferences _prefs;

	protected String _unlockData;

	protected String _unlockSign;
    private boolean _goldLime;
    private int mThemeResId;
    private boolean _quadLime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mThemeResId = MainActivity.themeApplicator(this);
        setContentView(R.layout.donate);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);



        new DonatorTask().execute();

        _donatorStatus = _prefs.getBoolean("enableLimeAccess", false);
        mIsUnlocked = _prefs.getBoolean("enableLimeAccess", false);
        mHasChecked = false;
        // Some sanity checks to see if the developer (that's you!) really followed the
        // instructions to run this sample (don't put these checks on your app!)

        
        this.findViewById(R.id.donateUnlockButton).setOnClickListener(new OnClickListener (){
			@Override
			public void onClick(View v) {
				if (mIsUnlocked)
					setScreen(2);
			}
        });
        
        this.findViewById(R.id.donateDisableLimeDisplay).setOnClickListener(new OnClickListener (){
			@Override
			public void onClick(View v) {
				boolean displayLimes = true;
				if (((CheckBox)v).isChecked())
				{
					displayLimes = false;
				}
				SharedPreferences.Editor editor = _prefs.edit();
            	editor.putBoolean("displayLimes", displayLimes);
            	editor.commit();
			}
        });
        
        
        this.findViewById(R.id.donateToggleLime).setOnClickListener(new OnClickListener (){
			@Override
			public void onClick(View v) {
				boolean verified = _prefs.getBoolean("usernameVerified", false);
		        if (!verified)
		        {
		        	LoginForm login = new LoginForm(DonateActivity.this);
		        	login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {
						@Override
						public void onSuccess() {
			                if (_limeRegistered)
			                	new LimeTask().execute("remove");
			                else
			                	new LimeTask().execute("add");
						}

						@Override
						public void onFailure() {
						}
					});
		        	return;
		        }
		        else
		        {
	                if (_limeRegistered)
	                	new LimeTask().execute("remove");
	                else
	                	new LimeTask().execute("add");
		        }
			}
        });
        
        String versionName = "Unknown";
        try {
			versionName = this.getApplication().getPackageManager().getPackageInfo(getApplication().getPackageName(), 0 ).versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        ((TextView)findViewById(R.id.donateVersion)).setText("Version " + versionName);
        
        this.findViewById(R.id.changeLog).setOnClickListener(new OnClickListener (){
			@Override
			public void onClick(View v) {
				ChangeLog cl = new ChangeLog(DonateActivity.this);
		        cl.getFullLogDialog().show();
			}
        });
        
        
        updateUi();
    }


	public MaterialDialog _progressDialog;

	private boolean _limeRegistered;

	private boolean _donatorStatus;

    
    // We're being destroyed. It's important to dispose of the helper here!
    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    // updates UI to reflect model
    public void updateUi() {
        // update the car color to reflect premium status or lack thereof
    	if (mHasChecked)
    	{
	        ((Button)findViewById(R.id.donateUnlockButton)).setEnabled(true);
	        if (mIsUnlocked)
	        {
	        	((Button)findViewById(R.id.donateUnlockButton)).setText("Access Lime Settings");
	        	Editor edit = _prefs.edit();
	        	edit.putBoolean("enableLimeAccess", true);
	        	edit.commit();
	        }
	        else
	        {
	        	((Button)findViewById(R.id.donateUnlockButton)).setText("No Lime Found in List");
                ((Button)findViewById(R.id.donateUnlockButton)).setEnabled(false);
	        }
    	}

        _goldLime = _prefs.getString("goldLimeUsers", "").contains(_prefs.getString("userName", "")) && !_prefs.getString("userName", "").equals("");
        _quadLime = _prefs.getString("quadLimeUsers", "").contains(_prefs.getString("userName", "")) && !_prefs.getString("userName", "").equals("");
        _limeRegistered = _prefs.getString("limeUsers", "").contains(_prefs.getString("userName", "")) && !_prefs.getString("userName", "").equals("") || _goldLime || _quadLime;

        
        SpannableString reg = new SpannableString("Serverside Lime Status: Registered plain lime for display next to username \"" + _prefs.getString("userName", "") + "\"");
        reg.setSpan(new ForegroundColorSpan(Color.rgb(100,255,100)), 23, reg.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString goldreg = new SpannableString("Serverside Lime Status: Registered gold lime for display next to username \"" + _prefs.getString("userName", "") + "\"");
        goldreg.setSpan(new ForegroundColorSpan(Color.rgb(255,195,0)), 23, goldreg.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString quadreg = new SpannableString("Serverside Lime Status: Registered quad damage lime for display next to username \"" + _prefs.getString("userName", "") + "\"");
        quadreg.setSpan(new ForegroundColorSpan(Color.rgb(50,50,255)), 23, goldreg.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString notreg = new SpannableString("Serverside Lime Status: Not registered for display");
        notreg.setSpan(new ForegroundColorSpan(Color.rgb(255,100,100)), 23, notreg.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView)findViewById(R.id.donateLimeStatus)).setText(((_limeRegistered) ? ((_goldLime || _quadLime) ? (_goldLime ? goldreg : quadreg) : reg) : notreg));

        _donatorStatus = _prefs.getBoolean("enableLimeAccess", false);
        SpannableString locked = new SpannableString("Lime Status: unavailable");
        locked.setSpan(new ForegroundColorSpan(Color.rgb(255,100,100)), 12, locked.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString unlocked = new SpannableString("Lime Status: unlocked, click above button to change lime related settings");
        unlocked.setSpan(new ForegroundColorSpan(Color.rgb(100,255,100)), 12, unlocked.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView)findViewById(R.id.donatorStatus)).setText(((_donatorStatus) ? unlocked : locked));
        
        ((CheckBox)findViewById(R.id.donateDisableLimeDisplay)).setChecked(!_prefs.getBoolean("displayLimes", true));

        ((TextView)findViewById(R.id.donatorText)).setText(Html.fromHtml("Although it used to be possible to unlock donator features here, all donator features have now been made public and unlocked! If you had donated in the past, you may access the ability to turn off your donator indicator icon above if your name is found in the <a href='http://woggle.net/shackbrowsedonators/list'>online donator list</a>. <br />This app utilizes APIs run on the woggle.net server. For more info on other woggle projects including the API, visit <a href='http://www.woggle.net'>woggle.net</a>."));
        ((TextView)findViewById(R.id.donatorText)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    class DonatorTask extends AsyncTask<String, Void, JSONArray>
    {
        Exception _exception;
        private String userName;

        @Override
        protected JSONArray doInBackground(String... params)
        {
            try
            {
                userName = _prefs.getString("userName", "");
                if (!userName.equals(""))
                    return ShackApi.getDonators();
                else
                    return null;
            }
            catch (Exception e)
            {
                Log.e("shackbrowse", "Error getting donators", e);
                _exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONArray result)
        {
            try {
                _progressDialog.dismiss();
            }
            catch (Exception e)
            {

            }

            if (_exception != null)
            {
                System.out.println("limechange: err");
                ErrorDialog.display(DonateActivity.this, "Error", "Error getting list of donators:\n" + _exception.getMessage());
            }
            else if (result == null)
            {
                mHasChecked = true;
                mIsUnlocked = false;
                updateUi();
            }
            else {
                System.out.println("DONATEACTIVITY: downloaded donator list: " + result);

                for (int i = 0; i < result.length(); i++) {
                    try {
                        if (result.getJSONObject(i).getString("user").equals(userName))
                        {
                            ErrorDialog.display(DonateActivity.this, "Congrats", "Found your name in the online donator list.");
                            Editor edit = _prefs.edit();
                            edit.putBoolean("enableLimeAccess", true);
                            edit.commit();
                            break;
                        }
                    }
                    catch (Exception e) {}
                }

                runOnUiThread(new Runnable(){
                    @Override public void run()
                    {
                        mHasChecked = true;
                        mIsUnlocked = _prefs.getBoolean("enableLimeAccess", false);
                        updateUi();
                    }
                });
            }
        }
    }
    
	class LimeTask extends AsyncTask<String, Void, String[]>
	{
	    Exception _exception;
		private String _taskMode;
	    
        @Override
        protected String[] doInBackground(String... params)
        {
            try
            {
                String userName = _prefs.getString("userName", "");
                
                _taskMode = params[0];
                
                // actually upload the thing
                if (params[0].equals("add"))
                {
                	runOnUiThread(new Runnable(){
                		@Override public void run()
                		{
                			_progressDialog = MaterialProgressDialog.show(DonateActivity.this, "Adding Lime", "Communicating with server...");
                		}
                	});
                	return new String[]{ShackApi.putDonator(true, userName), null};
                }
                
                if (params[0].equals("remove"))
                {
                	runOnUiThread(new Runnable(){
                		@Override public void run()
                		{
                			_progressDialog = MaterialProgressDialog.show(DonateActivity.this, "Removing Lime", "Communicating with server...");
                		}
                	});
                	return new String[]{ShackApi.putDonator(false, userName), null};
                }
                
                if (params[0].equals("get"))
                	return ShackApi.getLimeList();
                
                return null;
            }
            catch (Exception e)
            {
                Log.e("shackbrowse", "Error changing lime status", e);
                _exception = e;
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(String[] result)
        {
        	try {
        		_progressDialog.dismiss();
        	}
        	catch (Exception e)
        	{
        		
        	}
            
            if (_exception != null)
            {
            	System.out.println("limechange: err");
                ErrorDialog.display(DonateActivity.this, "Error", "Error changing lime:\n" + _exception.getMessage());
            }
            else if (result == null)
            {
            	System.out.println("limechange: err");
                ErrorDialog.display(DonateActivity.this, "Error", "Unknown lime error.");
            }
            else if (!_taskMode.equals("get"))
            {
            	runOnUiThread(new Runnable(){
            		@Override public void run()
            		{
            			new LimeTask().execute("get");
            		}
            	});
            }
            else if (_taskMode.equals("get"))
            {
            	System.out.println("DONATEACTIVITY: downloaded donator list: " + result[1]);
            	SharedPreferences.Editor editor = _prefs.edit();
                editor.putString("limeUsers", result[0]);
                editor.putString("goldLimeUsers", result[1]);
                editor.putString("quadLimeUsers", result[2]);
            	editor.commit();
            	
            	runOnUiThread(new Runnable(){
            		@Override public void run()
            		{
            			updateUi();
            		}
            	});
            }
        }
	}
    
    public String getUsername()
    {
        AccountManager manager = AccountManager.get(this); 
        Account[] accounts = manager.getAccountsByType("com.google"); 
        List<String> possibleEmails = new LinkedList<String>();

        for (Account account : accounts)
        {
          // TODO: Check possibleEmail against an email regex or treat
          // account.name as an email address only for certain account.type values.
        	possibleEmails.add(account.name);
        }

        if(!possibleEmails.isEmpty() && possibleEmails.get(0) != null)
        {
            String email = possibleEmails.get(0);
            String[] parts = email.split("@");
            if(parts.length > 0 && parts[0] != null)
                return parts[0];
            else
                return null;
        }
        else
            return null;
    }

    // Enables or disables the "please wait" screen.
    void setScreen(int screen) {
        findViewById(R.id.screen_main).setVisibility(screen == 0 ? View.VISIBLE : View.GONE);
        findViewById(R.id.screen_wait).setVisibility(screen == 1 ? View.VISIBLE : View.GONE);
        findViewById(R.id.screen_features).setVisibility(screen == 2 ? View.VISIBLE : View.GONE);
    }

    void complain(String message) {
        Log.e(TAG, "**** TrivialDrive Error: " + message);
        alert("Error: " + message);
    }

    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d(TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = new Intent(this, MainActivity.class);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is not part of the application's task, so create a new task
                    // with a synthesized back stack.
                    TaskStackBuilder.from(this)
                            .addNextIntent(upIntent)
                            .startActivities();
                    finish();
                } else {
                    // This activity is part of the application's task, so simply
                    // navigate up to the hierarchical parent activity.
                    finish();
                }
                return true;
        }
		return false;
    }
}