package net.swigglesoft.shackbrowse;

import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

//GCM access

public class NetworkNotificationServers
{

     static final String TAG = "GCMDemo";

     AtomicInteger msgId = new AtomicInteger();
     Context context;

     protected static String regid = null;

	 public static void setRegId(String newRegId) {
		 regid = newRegId;
	 }

	 public void registerDeviceOnStartup()
	 {
		 FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
			 setRegId(regid);
			 doRegisterTask("reg");
		 });
	 }

	 static public void getRegToken() {
		 FirebaseMessaging.getInstance().getToken()
				 .addOnCompleteListener(new OnCompleteListener<String>() {
					 @Override
					 public void onComplete(@NonNull Task<String> task) {
						 if (!task.isSuccessful()) {
							 Log.w(TAG, "Fetching FCM registration token failed", task.getException());
							 return;
						 }

						 // Get new FCM registration token
						 regid = task.getResult();
						 Log.d(TAG, "FCM REG ID IS: " + regid);
					 }
				 });
	 }

	private SharedPreferences _prefs;
	private OnGCMInteractListener _listener;

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
     	System.out.println("GCM: INSTANTIATED");
         context = activity;
         _prefs = PreferenceManager.getDefaultSharedPreferences(context);
         _listener = listener;

         // Check device for Play Services APK. If check succeeds, proceed with
         // GCM registration.
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
         return regid;
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
				 JSONArray keywords = null;
				 try {
					 keywords = new JSONArray(_prefs.getString(PreferenceKeys.notificationKeywords, ""));
				 } catch(JSONException e) {
					 Log.e(TAG, "Error reading mNoteKeywords", e);
				 }
                 
                 // everything but unreg is protected by username check
                 if (verified)
                 {
	                 if (params[0].equals("reg"))
	                 {
						 String regId = getRegistrationId();
	                 	 Log.d(TAG, "FCM ID: " + regId);
	                	 if (regId.length() > 0)
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
//	                		try
//	                		{
//	                			ShackApi.noteGetUser(userName);
//	                			// returns non-json when doesnt exist, so should raise exception
//	                		}
//	                		catch (Exception e)
//	                		{
//	                			// user doesnt exist
//	                			ShackApi.noteAddUser(userName,replies,vanity);
//	                		}
							ShackApi.noteAddUser(userName, keywords);
							boolean result = ShackApi.noteReg(userName, getRegistrationId());
	                		return result ? "add device" : "";
		                }
	                	else return "";
	                 }
	                 if (params[0].equals("updreplyvanity") && (params.length == 3))
	                 {
	                	if (getRegistrationId().length() > 0)
		                {

	                			String replies = params[1];
	                			String vanity = params[2];
	                		
	                		ShackApi.noteAddUser(userName,keywords);
							boolean result = ShackApi.noteReg(userName, getRegistrationId());
							return result ? "add device" : "";
		                }
	                	else return "";
	                 }
                 }
                 if (params[0].equals("unreg"))
                 {
                	 if (getRegistrationId().length() > 0)
	                {
	                	return ShackApi.noteUnreg(userName, getRegistrationId()) ? "remove device" : "";
	                }
                	else return "";
                 }
                 return "";
             }
             catch (Exception e)
             {
                 Log.e("shackbrowse", "Error changing push status", e);
                 _exception = e;
                 return "";
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
				 Log.e("shackbrowse", "Error changing push status in onPostExecute", e);
             }
         }
 	}
    
//    public void doUserInfoTask ()
//    {
//    	doUserInfoTask(null, null);
//    }
//
//    public void doUserInfoTask (String preAction, String parameter)
//    {
//    	System.out.println("GETTING USER INFO");
//   	    new GetUserInfoTask().execute(preAction, parameter);
//    }
//
//    class GetUserInfoTask extends AsyncTask<String, Void, JSONObject>
// 	{
// 	    Exception _exception;
//
//         @Override
//         protected JSONObject doInBackground(String... params)
//         {
//        	 String userName = _prefs.getString("userName", "");
//             boolean verified  = _prefs.getBoolean("usernameVerified", false);
//
//             String preAction = params[0];
//             if (preAction == null)
//            	 preAction = "0";
//
//             // everything but unreg is protected by username check
//             if (verified)
//             {
//            	 try {
//            		 if (preAction.equals("addkeyword"))
//            		 {
//            			 ShackApi.noteAddKeyword(userName, params[1]);
//            		 }
//            		 if (preAction.equals("removekeyword"))
//            		 {
//            			 ShackApi.noteRemoveKeyword(userName, params[1]);
//            		 }
//		             if (preAction.equals("remallexcept"))
//		             {
//			             if (getRegistrationId().length() > 0)
//			             {
//				             ShackApi.noteUnregEverythingBut(userName, getRegistrationId());
//			             }
//		             }
//					 return ShackApi.noteGetUser(userName);
//				} catch (Exception e) {
//					// username doesnt exist, add it then retry
//                     // "enableDonatorFeatures"
//					boolean vEnabled = true;
//            		String replies = "1";
//            		String vanity = vEnabled ? "1" : "0";
//					try {
//						ShackApi.noteAddUser(userName,replies,vanity);
//						if (preAction.equals("addkeyword"))
//	            		 {
//	            			 ShackApi.noteAddKeyword(userName, params[1]);
//	            		 }
//	            		 if (preAction.equals("removekeyword"))
//	            		 {
//	            			 ShackApi.noteRemoveKeyword(userName, params[1]);
//	            		 }
//						 return ShackApi.noteGetUser(userName);
//					} catch (Exception e1) {
//						// TODO Auto-generated catch block
//						e1.printStackTrace();
//					}
//					e.printStackTrace();
//				}
//             }
//             return null;
//         }
//
//         @Override
//         protected void onPostExecute(JSONObject result)
//         {
//             try {
//            	 _listener.userResult(result);
//             }
//             catch (Exception e)
//             {
//             }
//         }
// 	}
}