package net.woggle.shackbrowse;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;

import static java.lang.Thread.sleep;

public class NetworkEchoChamberServer {
    // a straightforward class to communicate with the server to manage echo chamber block lists
    Context context;
    private SharedPreferences _prefs;
    private OnEchoChamberResultListener _listener;

    public static final String ACTION_REMOVE = "remove";
    public static final String ACTION_ADD = "add";
    public static final String ACTION_GET = "get";

    public NetworkEchoChamberServer(Context activity, OnEchoChamberResultListener listener)
    {
        context = activity;
        _prefs = PreferenceManager.getDefaultSharedPreferences(context);

        _listener = listener;
    }

    public void doBlocklistTask (String preAction, String parameter)
    {
        System.out.println("GETTING USER blocklist");

        new BlocklistTask().execute(preAction, parameter);
    }
    public void doBlocklistTask (String preAction)
    {
        System.out.println("GETTING USER blocklist");
        if (preAction.equalsIgnoreCase(ACTION_GET))
        {
            new BlocklistTask().execute(preAction, "none");
        }
    }

    interface OnEchoChamberResultListener
    {
        public void networkResult(JSONArray result);
        public void addError();
    }

    public class BlocklistTask extends AsyncTask<String, Void, JSONArray>
    {
        Exception _exception;
        boolean addmode = false;

        @Override
        protected JSONArray doInBackground(String... params)
        {
            String userName = _prefs.getString("userName", "");
            boolean verified  = _prefs.getBoolean("usernameVerified", false);

            String preAction = params[0];

            String retval = "{\"b\":[]}";

            if (preAction == null)
                preAction = "0";

            if (verified)
            {
                try {
                    sleep(200);
                    if (preAction.equals(ACTION_ADD))
                    {
                        addmode = true;
                        if (ShackApi.usernameExists(params[1],context)) {
                            retval = ShackApi.blocklistAdd(userName, params[1]);
                        }
                        else return null;
                    }
                    if (preAction.equals(ACTION_REMOVE))
                    {
                        retval = ShackApi.blocklistRemove(userName, params[1]);
                    }
                    if (preAction.equals(ACTION_GET))
                    {
                        retval = ShackApi.blocklistCheck(userName);
                    }
                    JSONObject retjson = new JSONObject(retval);

                    return retjson.getJSONArray("b");
                } catch (Exception e) {
                    // json error
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONArray result)
        {
            if (result == null)
            {
                if (addmode)
                {
                    _listener.addError();
                }
            }
            try {
                _listener.networkResult(result);
            }
            catch (Exception e)
            {
            }
        }
    }
}
