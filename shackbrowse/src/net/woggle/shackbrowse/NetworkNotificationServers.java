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
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

//GCM access

public class NetworkNotificationServers
{
	 public static final String EXTRA_MESSAGE = "message";
     public static final String PROPERTY_REG_ID = "GCMregistration_id";
     private static final String PROPERTY_APP_VERSION = "GCMappVersion";
     private static final String PROPERTY_ON_SERVER_EXPIRATION_TIME = "GCMonServerExpirationTimeMs";
     
     /**
      * Default lifespan (7 days) of a reservation until it is considered expired.
      */
     public static final long REGISTRATION_EXPIRY_TIME_MS = 1000 * 3600 * 24 * 7;
     /**
      * You must use your own project ID instead.
      */
     String SENDER_ID = "1047530033396";

     /**
      * Tag used on log messages.
      */
     static final String TAG = "GCMDemo";

     GoogleCloudMessaging gcm;
     AtomicInteger msgId = new AtomicInteger();
     Activity context;

     String regid;
	private SharedPreferences _prefs;
	private OnGCMInteractListener _listener;
		//private TextView mDisplay;
     
     protected boolean checkPlayServices() {
         int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
         if (resultCode != ConnectionResult.SUCCESS) {
             if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {

             } else {
                 Log.i(TAG, "This device is not supported.");
             }
             return false;
         }
         return true;
     }
     
     interface OnGCMInteractListener
     {
    	 public void networkResult(String result);
    	 public void userResult(JSONObject result);
     }

     NetworkNotificationServers (Activity activity, OnGCMInteractListener listener)
     {
     	
     	//mDisplay = (TextView) findViewById(R.id.GCMStatus);
     	System.out.println("GCM: INSTANTIATED");
         context = activity;
         _prefs = PreferenceManager.getDefaultSharedPreferences(context);
         regid = getRegistrationId();
         
         _listener = listener;
         

         // Check device for Play Services APK. If check succeeds, proceed with
         //  GCM registration.
         if (checkPlayServices()) {
             gcm = GoogleCloudMessaging.getInstance(context);
             regid = getRegistrationId();

             if (regid.length() == 0) {
                 registerBackground();
             }
         } else {
             Log.i(TAG, "No valid Google Play Services APK found.");
         }

         
     }

     /**
      * Stores the registration id, app versionCode, and expiration time in the application's
      * {@code SharedPreferences}.
      *
      * @param context application's context.
      * @param regId registration id
      */
     private void setRegistrationId(Context context, String regId) {
         final SharedPreferences prefs = getGCMPreferences(context);
         int appVersion = getAppVersion(context);
         Log.v(TAG, "Saving regId on app version " + appVersion);
         SharedPreferences.Editor editor = prefs.edit();
         editor.putString(PROPERTY_REG_ID, regId);
         editor.putInt(PROPERTY_APP_VERSION, appVersion);
         long expirationTime = System.currentTimeMillis() + REGISTRATION_EXPIRY_TIME_MS;

         Log.v(TAG, "Setting registration expiry time to " +
                 new Timestamp(expirationTime));
         editor.putLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, expirationTime);
         editor.commit();
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
         final SharedPreferences prefs = getGCMPreferences(context);
         String registrationId = prefs.getString(PROPERTY_REG_ID, "");
         if (registrationId.length() == 0) {
             Log.v(TAG, "Registration not found.");
             return "";
         }
         // check if app was updated; if so, it must clear registration id to
         // avoid a race condition if GCM sends a message
         int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
         int currentVersion = getAppVersion(context);
         if (registeredVersion != currentVersion || isRegistrationExpired()) {
             Log.v(TAG, "App version changed or registration expired.");
             return "";
         }
         return registrationId;
     }

     /**
      * Registers the application with GCM servers asynchronously.
      * <p>
      * Stores the registration id, app versionCode, and expiration time in the application's
      * shared preferences.
      */
     private void registerBackground() {
         new AsyncTask<Void, Void, String>() {
             @Override
             protected String doInBackground(Void... params) {
                 String msg = "";
                 try {

                     if (gcm == null) {
                         gcm = GoogleCloudMessaging.getInstance(context);
                     }
                     regid = gcm.register(SENDER_ID);
                     msg = "Device registered, registration id=" + regid;

                     // You should send the registration ID to your server over HTTP, so it
                     // can use GCM/HTTP or CCS to send messages to your app.
                     sendRegistrationIdToBackend();

                     // For this demo: we don't need to send it because the device will send
                     // upstream messages to a server that echo back the message using the
                     // 'from' address in the message.

                     // Save the regid - no need to register again.
                     setRegistrationId(context, regid);
                 } catch (IOException ex) {
                     msg = "Error :" + ex.getMessage();
                 }
                 return msg;
             }

             @Override
             protected void onPostExecute(String msg) {
                 //mDisplay.append(msg + "\n");

             }
         }.execute(null, null, null);
     }

     protected void sendRegistrationIdToBackend() {
    	if (!_prefs.contains("noteEnabled"))
     	{
    		 new RegisterPushTask().execute("reg");
     	}
	}

	/**
      * @return Application's version code from the {@code PackageManager}.
      */
     private int getAppVersion(Context context) {
         try {
             PackageInfo packageInfo = context.getPackageManager()
                     .getPackageInfo(context.getPackageName(), 0);
             return packageInfo.versionCode;
         } catch (NameNotFoundException e) {
             // should never happen
             throw new RuntimeException("Could not get package name: " + e);
         }
     }

     /**
      * @return Application's {@code SharedPreferences}.
      */
     private SharedPreferences getGCMPreferences(Context context) {
         return _prefs;
     }

     /**
      * Checks if the registration has expired.
      *
      * <p>To avoid the scenario where the device sends the registration to the
      * server but the server loses it, the app developer may choose to re-register
      * after REGISTRATION_EXPIRY_TIME_MS.
      *
      * @return true if the registration has expired.
      */
     private boolean isRegistrationExpired() {
         final SharedPreferences prefs = getGCMPreferences(context);
         // checks if the information is not stale
         long expirationTime =
                 prefs.getLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, -1);
         return System.currentTimeMillis() > expirationTime;
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
    class RegisterPushTask extends AsyncTask<String, Void, String>
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