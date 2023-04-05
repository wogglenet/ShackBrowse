package net.swigglesoft.shackbrowse;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

/**
 * Created by brad on 6/21/2016 with FirebaseJobDispatcher. Modified to WorkManager as of 2023-03-31
 */
public class PeriodicNetworkService extends Worker
{
    private Context context;
    public static final String IDTAG = "sbsmcheck";

    public PeriodicNetworkService(@NonNull Context appContext, @NonNull WorkerParameters params) {
        super(appContext, params);
        this.context = appContext;
    }

    public static void scheduleJob(Context context, long updateIntervalSeconds)
    {
        Log.d("startuptest", "StartUpBootReceiver BOOT_COMPLETED");

        System.out.println("SHACKBROWSE PERIODIC NETWORK MESSAGE CHECK STARTUP");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        System.out.println("SMCHK: init tasks");

        String userName = prefs.getString("userName", "");
        boolean verified = prefs.getBoolean("usernameVerified", false);


        // go no further if it wont work
        if ((!prefs.getBoolean("SMAutoCheckEnabled", true)) || (!verified) || (userName.contentEquals("")) || (updateIntervalSeconds == 0L))
        {
            WorkManager.getInstance(context).cancelUniqueWork(IDTAG);
        }
        else
        {
            Data input = new Data.Builder().build();
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            PeriodicWorkRequest request =
                    new PeriodicWorkRequest.Builder(PeriodicNetworkService.class, updateIntervalSeconds, TimeUnit.SECONDS)
                            .setInputData(input)
                            .setConstraints(constraints)
                            .build();

            WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(IDTAG, ExistingPeriodicWorkPolicy.REPLACE, request);
        }
    }

    @Override
    public Result doWork() {
        Log.d(IDTAG, "onRunTask: " + IDTAG);
        System.out.println("SMCHK: RUNNING GCM TASK");
        Context appContext = this.context;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        long updateInterval = Long.parseLong(prefs.getString("PeriodicNetworkServicePeriod", "10800")); // DEFAULT 3 HR 10800L,  5 minutes 50-100mb, 10 minutes 25-50mb, 30mins 10-20mb, 1 hr 5-10mb, 3 hr 1-3mb, 6hr .5-1.5mb, 12hr .25-1mb
        String userName = prefs.getString("userName", "");
        boolean verified = prefs.getBoolean("usernameVerified", false);

        // go no further if it wont work
        if ((!prefs.getBoolean("SMAutoCheckEnabled", true)) || (!verified) || (userName.contentEquals("")) || (updateInterval == 0L))
        {
            WorkManager.getInstance(appContext).cancelUniqueWork(IDTAG);
            return Result.success();
        }

        ShackMessageCheck SMC = new ShackMessageCheck(appContext);
        SMC.syncCheckAPIForSMS();

        return Result.success();
    }

    @Override
    public void  onStopped() {
        // Nothing to do on stopped
    }
}