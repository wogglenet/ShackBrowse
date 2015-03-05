package net.woggle.shackbrowse;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * Created by brad on 3/2/2015.
 */
public class StatsFragment extends ListFragment {

    private ArrayList<StatsItem> mItems; // ListView items list

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // initialize the items list
        mItems = new ArrayList<StatsItem>();
        // initialize and set the list adapter
        setListAdapter(new StatListAdapter(getActivity(), mItems));
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // remove the dividers from the ListView of the ListFragment
        // getListView().setDivider(null);

        new CloudStats().execute("");
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // retrieve theListView item
        StatsItem item = mItems.get(position);
        // do something
        Toast.makeText(getActivity(), item.title, Toast.LENGTH_SHORT).show();
    }

    class StatsItem {
        private String title;
        private String fancyTitle;
        private String desc;
        private int num;
        private long time;

        public int getNum()
        {
            return num;
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
        public StatsItem(String title, int num, long time){
            this.title = title;
            this.num = num;
            this.time = time;
            this.fancyTitle = getResources().getString(getResources().getIdentifier(this.getTitle(), "string", getActivity().getPackageName()));
            this.desc = getResources().getString(getResources().getIdentifier(this.getTitle(), "string", getActivity().getPackageName()));
        }
    }

    class StatListAdapter extends ArrayAdapter {

        private Context context;
        private boolean useList = true;

        public StatListAdapter(Context context, List items) {
            super(context, R.layout.stats_layout, items);
            this.context = context;
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

            LayoutInflater mInflater = (LayoutInflater) context
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
            double perDay = Math.round(TimeDisplay.threadAge(item.getTime()) / 24f);
            holder.titleText.setText(item.getFancyTitle());
            holder.descText.setText(item.getNum() + " total, " + perDay + "/day, first logged " + TimeDisplay.getNiceTimeSince(item.getTime(), true));
            return viewToUse;
        }
    }

    class CloudStats extends AsyncTask<String, Void, ArrayList<StatsItem>> {
        private boolean _verbose;
        private String _verboseMsg;

        @Override
        protected ArrayList<StatsItem> doInBackground(String... params) {
            JSONObject cloudJson;
            ArrayList<StatsItem> statList = new ArrayList<StatsItem>();
            JSONArray stats = new JSONArray();
            try {
                cloudJson = ShackApi.getCloudPinned(((MainActivity) getActivity()).getCloudUsername());
                stats = cloudJson.optJSONArray("stats");
                if (stats == null)
                    stats = new JSONArray();

                // move jsonarray to arraylist

                for (int i = 0; i < stats.length(); i++) {
                    try {
                        JSONObject stat = stats.getJSONObject(i);
                        statList.add(new StatsItem(stat.getString("title"), stat.getInt("num"), stat.getLong("time")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // clean watched jsonarray

                // preferences should be
                // statsItemCheckedBlah = 1
                // statsTimeCheckedBlah = Timedisplay.now() or first-most time

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                Map<String, ?> keys = prefs.getAll();

                for (Map.Entry<String, ?> entry : keys.entrySet()) {
                    if (entry.getKey().startsWith("statsItem")) {
                        System.out.println("FDOUND STATS ITEM: " + entry.getKey().substring(9));
                        if (prefs.contains(entry.getKey()) && (prefs.getInt(entry.getKey(), 0) != 0)) {
                            // entry exists and current number is not 0
                            System.out.println("STATS SEARCH FOR ITEM: " + "statsTime" + entry.getKey().substring(9));
                            addOrUpdateStatItem(entry.getKey(), prefs.getInt(entry.getKey(), 0), prefs.getLong("statsTime" + entry.getKey().substring(9), 0L), statList);
                        }
                    }
                }

                // move arraylist to jsonarray
                JSONArray uploadList = new JSONArray();
                for (int i = 0; i < statList.size(); i++) {
                    try {
                        JSONObject stat = new JSONObject();
                        stat.put("title", statList.get(i).getTitle());
                        stat.put("num", statList.get(i).getNum());
                        stat.put("time", statList.get(i).getTime());
                        uploadList.put(stat);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                cloudJson.remove("stats");
                cloudJson.put("stats", uploadList);

                // rebuild watched jsonarray from thread keys
                String result = ShackApi.putCloudPinned(cloudJson, ((MainActivity) getActivity()).getCloudUsername());
                if ((_verbose) && (result != null)) _verboseMsg = "Stat Sync Success";


            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return statList;
        }

        @Override
        protected void onPostExecute(ArrayList<StatsItem> result) {
            if (_verbose) Toast.makeText(getActivity(), _verboseMsg, Toast.LENGTH_SHORT).show();
            _verbose = false;

            mItems = result;
            setListAdapter(new StatListAdapter(getActivity(), mItems));
        }
    }
    public void addOrUpdateStatItem(String title, int numberToAddOrInit, long time, ArrayList<StatsItem> statList) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor edit = prefs.edit();
        boolean foundItem = false;
        for (StatsItem item : statList) {
            if (item.getTitle().equalsIgnoreCase(title)) {
                item.setNum(item.getNum() + numberToAddOrInit);
                edit.putInt(title, 0); // 0 out the local pref
                if (item.getTime() < time) {
                    // item.setTime(time);
                    edit.putLong("statsTime" + title.substring(9), item.getTime());
                }
                foundItem = true;
            }
        }
        edit.commit();
        if (!foundItem)
            statList.add(new StatsItem(title, numberToAddOrInit, time));
    }
    public static void statInc(Context con, String itemName)
    {
        statInc(con, itemName, 1);
    }
    public static void statInc(Context con, String itemName, int by)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(con);
        SharedPreferences.Editor edit = prefs.edit();
        Long now = TimeDisplay.now();

        if (now <= prefs.getLong("statsTime" + itemName, now))
        {
            edit.putLong("statsTime" + itemName, now);
        }

        edit.putInt("statsItem" + itemName, prefs.getInt("statsItem" + itemName, 0) + by);
        edit.commit();
    }
}
