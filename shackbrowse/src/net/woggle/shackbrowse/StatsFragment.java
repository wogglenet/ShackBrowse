package net.woggle.shackbrowse;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by brad on 3/2/2015.
 */
public class StatsFragment extends ListFragment {

    private ArrayList<StatsItem> mItems; // ListView items list
    protected long mFirstTime = TimeDisplay.now();
    private Activity mContext;
    private String mCloudUserName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // initialize the items list
        mItems = new ArrayList<StatsItem>();
        setContext(getActivity());
        // initialize and set the list adapter
        // setListAdapter(new StatListAdapter(getActivity(), mItems));

    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // remove the dividers from the ListView of the ListFragment
        // getListView().setDivider(null);
        statInc(getActivity(), "TimesViewedStats");
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                setContext(getActivity());
                new CloudStats().execute("");
            }
        }, 100);

    }

    public void wipeStats()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Delete Stats?");
        builder.setMessage("This will clear stats both on this device and in the cloud for your username. Are you SURE?");
        builder.setCancelable(false);
        builder.setPositiveButton("Clear Stats Everywhere", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                setListAdapter(null);
                setContext(getActivity());
                new CloudStats().execute("wipe");
            }
        });
        builder.setNegativeButton("No, DO NOT DELETE", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        builder.create().show();

    }

    public void optOutDialog()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        new MaterialDialog.Builder(getActivity())
                .title("Select Opt Out Option")
                .items(R.array.preference_optoutstats)
                .itemsCallbackSingleChoice(prefs.getInt("optOutFromStats", 0), new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                        SharedPreferences.Editor edit = prefs.edit();
                        edit.putInt("optOutFromStats", which);
                        edit.commit();
                        mCloudUserName = ((MainActivity)getActivity()).getCloudUsername();

                        // paranoids get a wipe of local
                        if (which == 2)
                            new CloudStats().execute("wipe");
                        else
                            new CloudStats().execute("");

                        if ((which > 0) && (mCloudUserName != null)) {
                            new MaterialDialog.Builder(getActivity())
                                    .title("Nullify Cloud Data")
                                    .content("You can choose at this time to remove all cloud data for user " + mCloudUserName + ". This cannot be undone. Would you like to delete cloud stats?")
                                    .negativeText("No, do not delete")
                                    .positiveText("Delete all cloud stat data for " +mCloudUserName)
                                    .callback(new MaterialDialog.ButtonCallback() {
                                        @Override
                                        public void onPositive(MaterialDialog dialog) {
                                            super.onPositive(dialog);
                                            nullifyRemoteStats(getActivity());
                                        }
                                    })
                                    .show();
                        }

                        return true;
                    }
                })
                .positiveText("Confirm")
                .show();
    }

    public void nullifyRemoteStats(Activity context)
    {
        setContext(context);
        new CloudStats().execute("remotestatremove");
    }

    public void blindStatSync(Activity context)
    {
        setContext(context);
        new CloudStats().execute("blind");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View layout = inflater.inflate(R.layout.stats_layout, null);
        return layout;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // retrieve theListView item
        StatsItem item = mItems.get(position);
        // do something
        String string = item.getFancyTitle() + ": " + item.getItemDesc();
        Toast.makeText(getActivity(), "Copied to clipboard: " + string, Toast.LENGTH_SHORT).show();
        ClipboardManager clipboard = (ClipboardManager)getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
        clipboard.setText(string);
    }

    class StatsItem implements Comparable<StatsItem> {
        private String title;
        private String fancyTitle;
        private String desc;
        private int num;
        private long time;
        private long lastTime;
        public int getNum()
        {
            return num;
        }
        public long getLastTime()
        {
            return lastTime;
        }
        public long getTime()
        {
            return time;
        }
        public String getItemDesc() {
            return desc;
        }

        public String getTitle() {
            return title;
        }

        public String getFancyTitle() {
            return fancyTitle;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setDesc(String itemDesc) {
            this.desc = itemDesc;
        }

        public void setNum(int num) {
            this.num = num;
        }
        public void setTime(long time) {
            this.time = time;
        }
        public void setLastTime(long time) {
            this.lastTime = time;
        }
        public StatsItem(String titleWithoutUsernameButIncludingStatsItemPrefix, int num, long time, long lastTime){
            this.title = titleWithoutUsernameButIncludingStatsItemPrefix;
            this.fancyTitle = titleWithoutUsernameButIncludingStatsItemPrefix.substring(9);
            this.num = num;
            this.time = time;
            this.lastTime = lastTime;
            System.out.println("GETR RES:" + this.getTitle());
            try {
                this.fancyTitle = getResources().getString(getResources().getIdentifier(this.getTitle(), "string", getActivity().getPackageName()));
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        public StatsItem calcDesc()
        {
            long timeToUse = this.getTime();
            if (mFirstTime < timeToUse) timeToUse = mFirstTime;

            if (this.title == "notTurnedOn")
            {
                this.desc = "Statistics are turned off";
            }
            else if (this.time == 0L && this.title != "statsItemFirstStatRecord")
            {
                this.desc = "Not yet logged";
            }
            else if (this.title == "statsItemFirstStatRecord")
            {
                this.desc = TimeDisplay.getNiceTimeSince(mFirstTime, true);
            }
            else if (this.title.contains("Max"))
            {
                this.desc = this.getNum() + "";
            }
            else if (this.title.contains("TimeIn"))
            {

                double daysSince = TimeDisplay.threadAgeInHours(timeToUse) / 24d;
                daysSince = Math.ceil(daysSince);
                int perDay = (int)(this.getNum() / daysSince);
                this.desc = TimeDisplay.secondsToNiceTime(this.getNum()) + ", " + TimeDisplay.secondsToNiceTime(perDay) + "/day, since " + TimeDisplay.getNiceTimeSince(time, true);
            }
            else {
                double daysSince = TimeDisplay.threadAgeInHours(timeToUse) / 24d;
                daysSince = Math.ceil(daysSince);
                double perDay = this.getNum() / daysSince;
                BigDecimal perDayF = new BigDecimal(String.valueOf(perDay)).setScale(1, BigDecimal.ROUND_HALF_UP);
                this.desc = num + " total, " + perDayF.toString() + "/day last updated " + TimeDisplay.getNiceTimeSince(lastTime, true);
            }
            return this;
        }

        @Override
        public int compareTo(StatsItem another) {
            return ((Integer.compare(another.getNum(), this.getNum() ) != 0) ? Integer.compare(another.getNum(), this.getNum()) : this.fancyTitle.toLowerCase().compareTo(another.fancyTitle.toLowerCase()));
        }
    }

    class StatListAdapter extends ArrayAdapter {
        private boolean useList = true;

        public StatListAdapter(Context context, List items) {
            super(context, R.layout.stats_layout, items);
        }

        /**
         * Holder for the list items.
         */
        private class ViewHolder {
            TextView titleText;
            TextView descText;
        }

        /**
         * @param position
         * @param convertView
         * @param parent
         * @return
         */
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            StatsItem item = (StatsItem) getItem(position);
            View viewToUse = null;

            LayoutInflater mInflater = (LayoutInflater) mContext
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            if (convertView == null) {
                viewToUse = mInflater.inflate(R.layout.stats_row, null);
                holder = new ViewHolder();
                holder.titleText = (TextView) viewToUse.findViewById(R.id.statText1);
                holder.descText = (TextView) viewToUse.findViewById(R.id.statText2);
                viewToUse.setTag(holder);
            } else {
                viewToUse = convertView;
                holder = (ViewHolder) viewToUse.getTag();
            }
            holder.titleText.setText(item.getFancyTitle());
            holder.descText.setText(item.getItemDesc());

            return viewToUse;
        }
    }

    public void setContext(Activity context)
    {
        mContext = context;
    }
    class CloudStats extends AsyncTask<String, Void, ArrayList<StatsItem>> {
        private boolean _verbose;
        private String _verboseMsg;
        private boolean mBlind = false;

        @Override
        protected ArrayList<StatsItem> doInBackground(String... params) {
            JSONObject cloudJson = new JSONObject();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            int localOnly = prefs.getInt("optOutFromStats", 0);
            ArrayList<StatsItem> remoteStatList = new ArrayList<StatsItem>();
            JSONArray stats = new JSONArray();
            mCloudUserName = ((MainActivity)mContext).getCloudUsername();
            if (mCloudUserName == null || mCloudUserName.equals(""))
                return null;

            if (params[0].equalsIgnoreCase("remotestatremove"))
            {
                try {
                    cloudJson = ShackApi.getCloudPinned(mCloudUserName);
                    cloudJson.remove("stats");
                    String result = ShackApi.putCloudPinned(cloudJson, mCloudUserName);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            if (localOnly == 2)
            {
                if (params[0].equalsIgnoreCase("wipe"))
                {
                    clearLocalPrefs(prefs);
                    mFirstTime = TimeDisplay.now();
                }
                remoteStatList.add(new StatsItem("notTurnedOn", 0, 0L, 0L).calcDesc());
                // calcdesc will tell them everything is off

            }
            // local only
            else if (localOnly == 1)
            {
                if (params[0].equalsIgnoreCase("blind"))
                {
                    // nothing to do
                }
                else if (params[0].equalsIgnoreCase("wipe"))
                {
                    clearLocalPrefs(prefs);
                    mFirstTime = TimeDisplay.now();
                }

                loadLocalStatsIntoRemoteStatList(prefs, remoteStatList);
                // set up first time variable
                for (int i = 0; i < remoteStatList.size(); i++) {
                    if (remoteStatList.get(i).getTime() < mFirstTime) {
                        mFirstTime = remoteStatList.get(i).getTime();
                    }
                }
            }
            else if (localOnly == 0) {
                try {
                    cloudJson = ShackApi.getCloudPinned(mCloudUserName);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                try {
                    // watched should always exist. if it doesnt, data may be corrupted server side
                    JSONArray watched = cloudJson.optJSONArray("watched");
                    stats = cloudJson.optJSONArray("stats");
                    if (stats == null)
                        stats = new JSONArray();

                    // move jsonarray to arraylist

                    // create a crap upload and do not retain data from download
                    if (params[0].equalsIgnoreCase("blind")) {
                        mBlind = true;
                    }
                    if (params[0].equalsIgnoreCase("wipe")) {
                        clearLocalPrefs(prefs);

                        // so that its not totally empty..
                        remoteStatList.add(new StatsItem("statsItemTimesViewedStats", 1, TimeDisplay.now(), TimeDisplay.now()));
                        mFirstTime = TimeDisplay.now();
                    }
                    // load data from download into statlist, sync local data
                    else {
                        for (int i = 0; i < stats.length(); i++) {
                            try {
                                JSONObject stat = stats.getJSONObject(i);
                                remoteStatList.add(new StatsItem(stat.getString("title"), stat.getInt("num"), stat.getLong("time"), stat.getLong("lastTime")));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }


                        // preferences should be
                        // statsItemCheckedBlah = 1
                        // statsTimeCheckedBlah = Timedisplay.now() or first-most time
                        // statsLastTimeCheckedBlah = Timedisplay.now() or last-most time

                        // cycle through local data and sync with cloud data

                        Map<String, ?> keys = prefs.getAll();
                        for (Map.Entry<String, ?> entry : keys.entrySet()) {
                            // local prefs use item.getTitle() + cloudName as pref key
                            if (entry.getKey().startsWith("statsItem") && entry.getKey().endsWith(mCloudUserName)) {
                                System.out.println("FDOUND STATS ITEM: " + entry.getKey().substring(9));
                                String itemRootNameWithUsername = entry.getKey().substring(9);
                                if (prefs.contains(entry.getKey()) && (prefs.getInt(entry.getKey(), 0) != 0)) {
                                    // entry exists and current number is not 0
                                    // substring 9 gives us the name of the item, without the "statsItem" prefix
                                    addOrUpdateStatItem(entry.getKey(), prefs.getInt(entry.getKey(), 0), prefs.getLong("statsTime" + itemRootNameWithUsername, 0L), prefs.getLong("statsLastTime" + itemRootNameWithUsername, 0L), remoteStatList);
                                }
                            }
                        }
                    }

                    // move arraylist to jsonarray
                    // also use this loop to determine earliest initialized stat mFirstTime
                    JSONArray uploadList = new JSONArray();
                    for (int i = 0; i < remoteStatList.size(); i++) {
                        if (remoteStatList.get(i).getTime() < mFirstTime) {
                            System.out.println("Setting FirstTime based on " + remoteStatList.get(i).getTitle());
                            mFirstTime = remoteStatList.get(i).getTime();
                        }
                        try {
                            JSONObject stat = new JSONObject();
                            stat.put("title", remoteStatList.get(i).getTitle());
                            stat.put("num", remoteStatList.get(i).getNum());
                            stat.put("time", remoteStatList.get(i).getTime());
                            stat.put("lastTime", remoteStatList.get(i).getLastTime());
                            uploadList.put(stat);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    cloudJson.remove("stats");
                    cloudJson.put("stats", uploadList);

                    // rebuild watched jsonarray from thread keys
                    String result = ShackApi.putCloudPinned(cloudJson, mCloudUserName);
                    if ((_verbose) && (result != null)) _verboseMsg = "Stat Sync Success";

                    // sync is complete, now safe to 0 local data
                    // 0 out the local pref, as it has now been added to the cloud data and reuploaded safely. no data loss, otherwise an exception would have occurred
                    Map<String, ?> keys = prefs.getAll();
                    SharedPreferences.Editor edit = prefs.edit();
                    for (Map.Entry<String, ?> entry : keys.entrySet()) {
                        // at this point times have already been updated to cloud stats, only need to edit #s
                        // Max stats not 0'd
                        if (entry.getKey().startsWith("statsItem") && !entry.getKey().contains("Max") && entry.getKey().endsWith(mCloudUserName)) {
                            if (prefs.contains(entry.getKey()) && (prefs.getInt(entry.getKey(), 0) != 0)) {
                                edit.putInt(entry.getKey(), 0);
                            }
                        }
                    }
                    edit.commit();


                } catch (ClientProtocolException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return remoteStatList;
        }

        private void loadLocalStatsIntoRemoteStatList(SharedPreferences prefs, ArrayList<StatsItem> remoteStatList) {
            Map<String, ?> keys = prefs.getAll();
            for (Map.Entry<String, ?> entry : keys.entrySet()) {
                // local prefs use item.getTitle() + cloudName as pref key
                if (entry.getKey().startsWith("statsItem") && entry.getKey().endsWith(mCloudUserName)) {
                    System.out.println("FDOUND STATS ITEM: " + entry.getKey().substring(9));
                    String prefKeyWithoutStatsItemPrefix = entry.getKey().substring(9);
                    String prefKeyWithoutUsername = entry.getKey().substring(0, entry.getKey().length() - mCloudUserName.length());
                    if (prefs.contains(entry.getKey()) && (prefs.getInt(entry.getKey(), 0) != 0)) {
                        // entry exists and current number is not 0
                        // substring 9 gives us the name of the item, without the "statsItem" prefix
                        remoteStatList.add(new StatsItem(prefKeyWithoutUsername, prefs.getInt(entry.getKey(), 0), prefs.getLong("statsTime" + prefKeyWithoutStatsItemPrefix, 0L), prefs.getLong("statsLastTime" + prefKeyWithoutStatsItemPrefix, 0L)));
                    }
                }
            }
        }

        private void clearLocalPrefs(SharedPreferences prefs) {
            // clear local prefs
            SharedPreferences.Editor edit = prefs.edit();
            Map<String, ?> keys = prefs.getAll();
            for (Map.Entry<String, ?> entry : keys.entrySet()) {
                if (entry.getKey().startsWith("stats") && entry.getKey().endsWith(mCloudUserName)) {
                    edit.remove(entry.getKey());
                }
            }
            edit.commit();

            // add one back
            statInc(mContext, "TimesViewedStats");
        }

        @Override
        protected void onPostExecute(ArrayList<StatsItem> result) {
            if (result == null)
                return;

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            int localOnly = prefs.getInt("optOutFromStats", 0);
            // no stats mode
            if (localOnly == 2)
            {
                mItems = result;
                setListAdapter(new StatListAdapter(mContext, mItems));
            }
            // local or remote stats
            else {
                if (!mBlind) {
                    if (_verbose) Toast.makeText(mContext, _verboseMsg, Toast.LENGTH_SHORT).show();
                    _verbose = false;

                    mItems = result;

                    statMax(mContext, "MaxNumberOfStatsRecorded", mItems.size());

                    addInNonExistingStats(mItems);

                    Collections.sort(mItems);

                    // calcDesc does the actual calculation on this, the item just needs to be in the array for display
                    mItems.add(0, new StatsItem("statsItemFirstStatRecord", 0, 0L, 0L).calcDesc());

                    setListAdapter(new StatListAdapter(mContext, mItems));
                }
            }
        }

        private void addInNonExistingStats(ArrayList<StatsItem> statList) {
            String[] array = mContext.getResources().getStringArray(R.array.statsItems);
            ArrayList<String> statNameList = new ArrayList<String>();
            statNameList.addAll(Arrays.asList(array));

            // if the statlist currently has a record of an item in the name list, remove from name list to prevent dupes
            // also use this loop to calculate descriptions based on mFirstTime which was calculated in earlier loop (search for "mFirstTime")
            for (StatsItem item : statList) {
                item.calcDesc();
                if (statNameList.contains(item.getTitle()))
                    statNameList.remove(item.getTitle());
            }

            // add in any stat in the name list that was not already represented
            for (String missingStat : statNameList)
            {
                statList.add(new StatsItem(missingStat, 0, 0L, 0L).calcDesc());
            }
        }

        // statList has only cloud items in it at this point. this function adds local items into the array. local prefs are the first 4 inputs, cloud data is the array
        public void addOrUpdateStatItem(String prefKey, int numberToAddOrInit, long time, long lastTime, ArrayList<StatsItem> statList) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor edit = prefs.edit();
            String prefKeyWithoutUsername = prefKey.substring(0, prefKey.length() - mCloudUserName.length());
            boolean foundItem = false;
            // prefKey INCLUDES username
            // remote item names dont have username on them
            // look to see if cloud data already had an entry for the local item
            for (StatsItem remoteItem : statList) {
                // found the cloud item which matches the local pref, and pref is not a max
                if (remoteItem.getTitle().equalsIgnoreCase(prefKeyWithoutUsername) && !prefKeyWithoutUsername.contains("Max")) {

                    // add local to cloud. later, local pref will be 0'd
                    remoteItem.setNum(remoteItem.getNum() + numberToAddOrInit);

                    // check to see if cloud data predates local time or supersedes
                    if (remoteItem.getTime() < time) {
                        edit.putLong("statsTime" + prefKey.substring(9), remoteItem.getTime());
                    }
                    if (remoteItem.getTime() > time) {
                        remoteItem.setTime(time);
                    }
                    if (remoteItem.getLastTime() > lastTime) {
                        edit.putLong("statsLastTime" + prefKey.substring(9), remoteItem.getLastTime());
                    }
                    if (remoteItem.getLastTime() < lastTime) {
                        remoteItem.setLastTime(lastTime);
                    }
                    foundItem = true;
                }
                // max items
                else if (remoteItem.getTitle().equalsIgnoreCase(prefKeyWithoutUsername) && prefKeyWithoutUsername.contains("Max")) {
                    // is cloud item more than local?
                    if (remoteItem.getNum() > numberToAddOrInit)
                    {
                        edit.putInt(prefKey, remoteItem.getNum());
                    }
                    // is cloud item less than local?
                    else if (remoteItem.getNum() < numberToAddOrInit)
                    {
                        remoteItem.setNum(numberToAddOrInit);
                    }
                    // check to see if cloud data predates local time or supersedes
                    if (remoteItem.getTime() < time) {
                        edit.putLong("statsTime" + prefKey.substring(9), remoteItem.getTime());
                    }
                    if (remoteItem.getTime() > time) {
                        remoteItem.setTime(time);
                    }
                    if (remoteItem.getLastTime() > lastTime) {
                        edit.putLong("statsLastTime" + prefKey.substring(9), remoteItem.getLastTime());
                    }
                    if (remoteItem.getLastTime() < lastTime) {
                        remoteItem.setLastTime(lastTime);
                    }
                    foundItem = true;
                }
            }
            edit.commit();

            // no entry in cloud data for local item, so add local item to cloud array which will be uploaded now that its synched
            if (!foundItem)
                statList.add(new StatsItem(prefKeyWithoutUsername, numberToAddOrInit, time, TimeDisplay.now()));
        }
    }


    public static void statInc(Context con, String itemName)
    {
        statInc(con, itemName, 1);
    }

    public static void statInc(Context con, String itemName, int by)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(con);
        SharedPreferences.Editor edit = prefs.edit();

        int localOnly = prefs.getInt("optOutFromStats", 0);
        String cloudUsername = getCloudUsername(con);

        if (cloudUsername == null)
            return;

        // no stats mode
        if (localOnly == 2)
            return;

        Long now = TimeDisplay.now();

        if (now <= prefs.getLong("statsTime" + itemName + cloudUsername, now))
        {
            edit.putLong("statsTime" + itemName + cloudUsername, now);
        }
        if (now >= prefs.getLong("statsLastTime" + itemName + cloudUsername, now))
        {
            edit.putLong("statsLastTime" + itemName + cloudUsername, now);
        }

        edit.putInt("statsItem" + itemName + cloudUsername, prefs.getInt("statsItem" + itemName + cloudUsername, 0) + by);
        edit.apply();
    }
    public static String getCloudUsername(Context con)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(con);
        String userName = prefs.getString("userName", "");
        if ((userName.length() == 0) || !prefs.getBoolean("usernameVerified", false))
        {
            return null;
        }
        return userName.trim();
    }
    public static void statMax(Context con, String itemName, int max) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(con);
        SharedPreferences.Editor edit = prefs.edit();
        String cloudUsername = getCloudUsername(con);
        int localOnly = prefs.getInt("optOutFromStats", 0);

        if (cloudUsername == null)
            return;

        // no stats mode
        if (localOnly == 2)
            return;

        Long now = TimeDisplay.now();

        if (now <= prefs.getLong("statsTime" + itemName + cloudUsername, now)) {
            edit.putLong("statsTime" + itemName + cloudUsername, now);
        }
        if (now >= prefs.getLong("statsLastTime" + itemName + cloudUsername, now))
        {
            edit.putLong("statsLastTime" + itemName + cloudUsername, now);
        }
        if (max > prefs.getInt("statsItem" + itemName + cloudUsername, 0)) {
            edit.putInt("statsItem" + itemName + cloudUsername, max);
        }
        edit.apply();
    }
}
