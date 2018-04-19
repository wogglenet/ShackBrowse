package net.woggle.shackbrowse;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;

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
public class PeriodicNetworkService  extends JobService
{

    public static final String IDTAG = "sbsmcheck";

    public static void scheduleJob(Context context, long updateIntervalSeconds)
    {
        Log.d("startuptest", "StartUpBootReceiver BOOT_COMPLETED");

        System.out.println("SHACKBROWSE PERIODIC NETWORK MESSAGE CHECK STARTUP");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        System.out.println("SMCHK: init tasks");
        long flex = Math.round((double)updateIntervalSeconds * (1f/3f));

        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher firebaseJobDispatcher = new FirebaseJobDispatcher(driver);

        String userName = prefs.getString("userName", "");
        boolean verified = prefs.getBoolean("usernameVerified", false);


        // go no further if it wont work
        if ((!prefs.getBoolean("SMAutoCheckEnabled", true)) || (!verified) || (userName.contentEquals("")) || (updateIntervalSeconds == 0L))
        {
            firebaseJobDispatcher.cancel(IDTAG);
        }
        else
        {
            Job smCheckJob = firebaseJobDispatcher.newJobBuilder()
                    .setService(PeriodicNetworkService.class)
                    .setTag(IDTAG)
                    .setRecurring(true)
                    .setConstraints(Constraint.ON_ANY_NETWORK)
                    .setLifetime(Lifetime.FOREVER)
                    .setRecurring(true)
                    .setTrigger(Trigger.executionWindow(
                            (int) updateIntervalSeconds,
                            (int) (updateIntervalSeconds + flex)
                    ))
                    .setReplaceCurrent(true)
                    .build();

            firebaseJobDispatcher.schedule(smCheckJob);
        }
    }

    @Override
    public boolean onStartJob(JobParameters job)
    {
        Log.d(IDTAG, "onRunTask: " + job.getTag());
        System.out.println("SMCHK: RUNNING GCM TASK");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        long updateInterval = Long.parseLong(prefs.getString("PeriodicNetworkServicePeriod", "10800")); // DEFAULT 3 HR 10800L,  5 minutes 50-100mb, 10 minutes 25-50mb, 30mins 10-20mb, 1 hr 5-10mb, 3 hr 1-3mb, 6hr .5-1.5mb, 12hr .25-1mb
        String userName = prefs.getString("userName", "");
        boolean verified = prefs.getBoolean("usernameVerified", false);

        Driver driver = new GooglePlayDriver(this);
        FirebaseJobDispatcher firebaseJobDispatcher = new FirebaseJobDispatcher(driver);


        // go no further if it wont work
        if ((!prefs.getBoolean("SMAutoCheckEnabled", true)) || (!verified) || (userName.contentEquals("")) || (updateInterval == 0L))
        {
            firebaseJobDispatcher.cancel(IDTAG);
            return false;
        }


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

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters job)
    {
        return false;
    }
}