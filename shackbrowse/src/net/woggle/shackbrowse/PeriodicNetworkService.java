package net.woggle.shackbrowse;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.TaskParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * Created by brad on 6/21/2016.
 */
public class PeriodicNetworkService  extends GcmTaskService
{

    private static final String TAG = "MyTaskService";

    public static final String ACTION_DONE = "GcmTaskService#ACTION_DONE";
    public static final String EXTRA_TAG = "extra_tag";
    public static final String EXTRA_RESULT = "extra_result";
    private GcmNetworkManager mGcmNetworkManager;


    @Override
    public void onInitializeTasks() {
        // When your package is removed or updated, all of its network tasks are cleared by
        // the GcmNetworkManager. You can override this method to reschedule them in the case of
        // an updated package. This is not called when your application is first installed.
        //
        // This is called on your application's main thread.
        // TODO: enable or disable based on preferences

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    System.out.println("SMCHK: init tasks");
        long updateInterval = Long.parseLong(prefs.getString("PeriodicNetworkServicePeriod", "10800")); // DEFAULT 3 HR 10800L,  5 minutes 50-100mb, 10 minutes 25-50mb, 30mins 10-20mb, 1 hr 5-10mb, 3 hr 1-3mb, 6hr .5-1.5mb, 12hr .25-1mb

        ScheduleService(this, updateInterval);
        // TODO(developer): In a real app, this should be implemented to re-schedule important tasks.
        super.onInitializeTasks();
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        Log.d(TAG, "onRunTask: " + taskParams.getTag());
	    System.out.println("SMCHK: RUNNING GCM TASK");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    long updateInterval = Long.parseLong(prefs.getString("PeriodicNetworkServicePeriod", "10800")); // DEFAULT 3 HR 10800L,  5 minutes 50-100mb, 10 minutes 25-50mb, 30mins 10-20mb, 1 hr 5-10mb, 3 hr 1-3mb, 6hr .5-1.5mb, 12hr .25-1mb
	    String userName = prefs.getString("userName", "");
        boolean verified = prefs.getBoolean("usernameVerified", false);
        int result = GcmNetworkManager.RESULT_SUCCESS;

        // go no further if it wont work
        if ((!prefs.getBoolean("SMAutoCheckEnabled", true)) || (!verified) || (userName.contentEquals("")) || (updateInterval == 0L))
        {
            mGcmNetworkManager = GcmNetworkManager.getInstance(this);
            mGcmNetworkManager.cancelTask("ShackBrowseWidgetAndSM", PeriodicNetworkService.class);
            return result;
        }
        String tag = taskParams.getTag();

        // Default result is success.



	    /*
	     * THE FOLLOWING IS FOR USE ON A POSSIBLE FUTURE WIDGET
	     *//*
        JSONObject json = new JSONObject();
        try
        {
            json = ShackApi.getThreads(1, userName, this, prefs.getBoolean("useTurboAPI", true));
        
            // process these threads and remove collapsed
            ArrayList<Thread> new_threads = ShackApi.processThreads(json, false, new ArrayList<Integer>(), this);

            HashMap<String, HashMap<String, LolObj>> shackloldata = new HashMap<String, HashMap<String, LolObj>>();;
            boolean getLols = prefs.getBoolean("getLols", true);
            boolean lolsContained = prefs.getBoolean("showThreadLolsThreadList", true);
            boolean showShackTags = prefs.getBoolean("showShackTagsInThreadList", true);
            boolean stripNewLines = prefs.getBoolean("previewStripNewLines", false);
            if (getLols)
                shackloldata = ShackApi.getLols(this);

            WidgetDB wDB = new WidgetDB(this);
	        wDB.open();
	        wDB.deleteAll();

            for (Thread t: new_threads)
            {
                // load lols
                HashMap<String, LolObj> threadlols = shackloldata.get(Integer.toString(t.getThreadId()));
                if ((threadlols != null) && (lolsContained) && (getLols))
                {
                    LolObj lolobj = shackloldata
                            .get(Integer.toString(t.getThreadId()))
                            .get("totalLols");
                    if (lolobj != null) { t.setLolObj(lolobj); }
                }

                wDB.createWidgetDBFromThread(t);
            }
	        wDB.close();
			result = GcmNetworkManager.RESULT_SUCCESS;
        }
        catch (IOException e)
        {
            e.printStackTrace();
	        result = GcmNetworkManager.RESULT_FAILURE;
        } catch (JSONException e)
        {
            e.printStackTrace();
	        result = GcmNetworkManager.RESULT_FAILURE;
        }
*/
	    System.out.println("SMCHK: start frugal check");
	    ShackMessageCheck SMC = new ShackMessageCheck(this);
		SMC.frugalSMCheck();

        // Create Intent to broadcast the task information.
        Intent intent = new Intent();
        intent.setAction(ACTION_DONE);
        intent.putExtra(EXTRA_TAG, tag);
        intent.putExtra(EXTRA_RESULT, result);

        // Send local broadcast, running Activities will be notified about the task.
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(intent);

        // Return one of RESULT_SUCCESS, RESULT_FAILURE, or RESULT_RESCHEDULE
        return result;
    }

    public static void ScheduleService (Context fincon, long updateInterval) {
        GcmNetworkManager mGcmNetworkManager = GcmNetworkManager.getInstance(fincon);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(fincon);
        // long updateInterval = prefs.getLong("PeriodicNetworkServicePeriod", 10800L);
        String userName = prefs.getString("userName", "");
        boolean verified = prefs.getBoolean("usernameVerified", false);


        // go no further if it wont work
        if ((!prefs.getBoolean("SMAutoCheckEnabled", true)) || (!verified) || (userName.contentEquals("")) || (updateInterval == 0L))
        {
            mGcmNetworkManager = GcmNetworkManager.getInstance(fincon);
            mGcmNetworkManager.cancelTask("ShackBrowseWidgetAndSM", PeriodicNetworkService.class);
        }
        else
        {
            PeriodicTask task = new PeriodicTask.Builder()
                    .setService(PeriodicNetworkService.class)
                    .setPeriod(updateInterval)
                    .setTag("ShackBrowseWidgetAndSM")
                    .setPersisted(true)
                    .setFlex((long) (updateInterval * 0.25f))
                    .setUpdateCurrent(true)
                    .build();
            mGcmNetworkManager.schedule(task);
        }
    }
}