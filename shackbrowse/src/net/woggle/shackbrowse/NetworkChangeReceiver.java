package net.woggle.shackbrowse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkChangeReceiver extends BroadcastReceiver {
	 
    @Override
    public void onReceive(final Context context, final Intent intent) {
 
        if (
        		(NetworkUtil.getConnectivityStatus(context) == NetworkUtil.TYPE_MOBILE)
        		||
        		(NetworkUtil.getConnectivityStatus(context) == NetworkUtil.TYPE_WIFI)
        	)
        {
        	// start the postqueue service
    	    Intent msgIntent = new Intent(context, PostQueueService.class);
    	    context.startService(msgIntent);
    	    System.out.println("POSTQU: SENDING START MSG");
        }
    }
    
    static class NetworkUtil {
        
        public static final int TYPE_WIFI = 1;
        public static final int TYPE_MOBILE = 2;
        public static final int TYPE_NOT_CONNECTED = 0;
         
         
        public static int getConnectivityStatus(Context context) {
            ConnectivityManager cm = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
     
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (null != activeNetwork) {
                if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                    return TYPE_WIFI;
                 
                if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                    return TYPE_MOBILE;
            }
            return TYPE_NOT_CONNECTED;
        }
         
        public static String getConnectivityStatusString(Context context) {
            int conn = NetworkUtil.getConnectivityStatus(context);
            String status = null;
            if (conn == NetworkUtil.TYPE_WIFI) {
                status = "Wifi enabled";
            } else if (conn == NetworkUtil.TYPE_MOBILE) {
                status = "Mobile data enabled";
            } else if (conn == NetworkUtil.TYPE_NOT_CONNECTED) {
                status = "Not connected to Internet";
            }
            return status;
        }
    }
}