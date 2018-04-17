package net.woggle.shackbrowse;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.iid.FirebaseInstanceId;

//GCM access

public class NetworkNotificationServers
{

     static final String TAG = "GCMDemo";

     AtomicInteger msgId = new AtomicInteger();
     Context context;

     String regid = FirebaseInstanceId.getInstance().getToken();

	private SharedPreferences _prefs;
	private OnGCMInteractListener _listener;
		//private TextView mDisplay;
     
     protected boolean checkPlayServices() {
	     GoogleApiAvailability api = GoogleApiAvailability.getInstance();
	     int resultCode = api.isGooglePlayServicesAvailable(context);
	     if (resultCode == ConnectionResult.SUCCESS) {
		     return true;
	     }else{
		     //Any random request code
		     int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
		     //Google Play Services app is not available or version is not up to date. Error the
		     // error condition here

		     return false;
	     }
     }
     
     interface OnGCMInteractListener
     {
    	 public void networkResult(String result);
    	 public void userResult(JSONObject result);
     }

     public NetworkNotificationServers(Context activity, OnGCMInteractListener listener)
     {
     	
     	//mDisplay = (TextView) findViewById(R.id.GCMStatus);
     	System.out.println("GCM: INSTANTIATED");
         context = activity;
         _prefs = PreferenceManager.getDefaultSharedPreferences(context);
         regid = getRegistrationId();
         
         _listener = listener;
         

         // Check device for Play Services APK. If check succeeds, proceed with
         //  GCM registration.
     }

     /**
      * Gets the current registration id for application on GCM service.
      * <p>
      * If result is empty, the registration has failed.
      *
      * @return registration id, or empty string if the registration is not
      *         complete.
      */
     public String getRegistrationId() {
         return FirebaseInstanceId.getInstance().getToken();
     }

     public void sendRegistrationIdToBackend() {
    	if (!_prefs.contains("noteEnabled"))
     	{
    		 new RegisterPushTask().execute("reg");
     	}
	}

     public void doRegisterTask (String whatDo)
     {
    	 new RegisterPushTask().execute(whatDo);
     }
     
     public void updReplVan (boolean replies, boolean vanity)
     {
    	 new RegisterPushTask().execute("updreplyvanity", replies ? "1" : "0", vanity ? "1" : "0");
     }
     
     // SB BACKEND STUFF
    public class RegisterPushTask extends AsyncTask<String, Void, String>
 	{
 	    Exception _exception;
			
         @Override
         protected String doInBackground(String... params)
         {
             try
             {
                 String userName = _prefs.getString("userName", "");
                 boolean verified  = _prefs.getBoolean("usernameVerified", false);
                 
                 // everything but unreg is protected by username check
                 if (verified)
                 {
	                 if (params[0].equals("reg"))
	                 {
	                	if (getRegistrationId().length() > 0)
		                {
                            // "enableDonatorFeatures"
	                		boolean vEnabled = true;
	                		String replies = "1";
	                		String vanity = vEnabled ? "1" : "0";
	                		if (params.length == 3)
	                		{
	                			replies = params[1];
	                			vanity = params[2];
	                		}
	                		try
	                		{
	                			ShackApi.noteGetUser(userName);
	                			// returns non-json when doesnt exist, so should raise exception
	                		}
	                		catch (Exception e)
	                		{
	                			// user doesnt exist
	                			ShackApi.noteAddUser(userName,replies,vanity);
	                		}
	                		return ShackApi.noteReg(userName, getRegistrationId());
		                }
	                	else return "";
	                 }
	                 if (params[0].equals("updreplyvanity") && (params.length == 3))
	                 {
	                	if (getRegistrationId().length() > 0)
		                {
	                		
	                			String replies = params[1];
	                			String vanity = params[2];
	                		
	                		ShackApi.noteAddUser(userName,replies,vanity);
	                		return ShackApi.noteReg(userName, getRegistrationId());
		                }
	                	else return "";
	                 }
                 }
                 if (params[0].equals("unreg"))
                 {
                	 if (getRegistrationId().length() > 0)
	                {
	                	return ShackApi.noteUnreg(userName, getRegistrationId());
	                }
                	else return "";
                 }
                 return null;
             }
             catch (Exception e)
             {
                 Log.e("shackbrowse", "Error changing push status", e);
                 _exception = e;
                 return null;
             }
         }
         @Override
         protected void onPostExecute(String result)
         {
             try {
				_listener.networkResult(result);
             }
             catch (Exception e)
             {
             }
         }
 	}
    
    public void doUserInfoTask ()
    {
    	doUserInfoTask(null, null);
    }
    
    public void doUserInfoTask (String preAction, String parameter)
    {
    	System.out.println("GETTING USER INFO");
   	    new GetUserInfoTask().execute(preAction, parameter);
    }
    
    class GetUserInfoTask extends AsyncTask<String, Void, JSONObject>
 	{
 	    Exception _exception;
			
         @Override
         protected JSONObject doInBackground(String... params)
         {
        	 String userName = _prefs.getString("userName", "");
             boolean verified  = _prefs.getBoolean("usernameVerified", false);
             
             String preAction = params[0];
             if (preAction == null)
            	 preAction = "0";
             
             // everything but unreg is protected by username check
             if (verified)
             {
            	 try {
            		 if (preAction.equals("addkeyword"))
            		 {
            			 ShackApi.noteAddKeyword(userName, params[1]);
            		 }
            		 if (preAction.equals("removekeyword"))
            		 {
            			 ShackApi.noteRemoveKeyword(userName, params[1]);
            		 }
		             if (preAction.equals("remallexcept"))
		             {
			             if (getRegistrationId().length() > 0)
			             {
				             ShackApi.noteUnregEverythingBut(userName, getRegistrationId());
			             }
		             }
					 return ShackApi.noteGetUser(userName);
				} catch (Exception e) {
					// username doesnt exist, add it then retry
                     // "enableDonatorFeatures"
					boolean vEnabled = true;
            		String replies = "1";
            		String vanity = vEnabled ? "1" : "0";
					try {
						ShackApi.noteAddUser(userName,replies,vanity);
						if (preAction.equals("addkeyword"))
	            		 {
	            			 ShackApi.noteAddKeyword(userName, params[1]);
	            		 }
	            		 if (preAction.equals("removekeyword"))
	            		 {
	            			 ShackApi.noteRemoveKeyword(userName, params[1]);
	            		 }
						 return ShackApi.noteGetUser(userName);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					e.printStackTrace();
				}
             }
             return null;
         }
         
         @Override
         protected void onPostExecute(JSONObject result)
         {
             try {
            	 _listener.userResult(result);
             }
             catch (Exception e)
             {
             }
         }
 	}
}