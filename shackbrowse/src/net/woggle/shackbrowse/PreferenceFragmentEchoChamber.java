package net.woggle.shackbrowse;


import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.woggle.shackbrowse.NetworkNotificationServers.OnGCMInteractListener;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.annotation.NonNull;

import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

public class PreferenceFragmentEchoChamber extends PreferenceFragment
{
    private SharedPreferences _prefs;

    protected MaterialDialog _progressDialog;

    private CheckBoxPreference _echoNotification;
    private CheckBoxPreference _autoNotification;

    private Preference _blockNotification;

    private NetworkEchoChamberServer.OnEchoChamberResultListener mListener = new NetworkEchoChamberServer.OnEchoChamberResultListener() {
        @Override
        public void networkResult(JSONArray result) {
            if (_progressDialog != null)
            {
                _progressDialog.dismiss();
                _progressDialog = null;
            }
            ((MainActivity)getActivity()).mBlockList = result;
            Editor ed = _prefs.edit();
            ed.putString("echoChamberBlockList", result.toString());
            ed.commit();
            
            showBlocklist();
        }
    };

    private NetworkEchoChamberServer mEchoServerInteract;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());


        addPreferencesFromResource(R.xml.preferences_echochamber);

        mEchoServerInteract = new NetworkEchoChamberServer(getActivity(), mListener);
        _blockNotification = (Preference) findPreference("blockList");
        _blockNotification.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                _progressDialog = MaterialProgressDialog.show(getActivity(), "Loading Block List", "Communicating with Shack Browse server...", true, true);
                mEchoServerInteract.doBlocklistTask(NetworkEchoChamberServer.ACTION_GET);
                return true;

            }
        });

        _echoNotification = (CheckBoxPreference) findPreference("echoEnabled");
        _autoNotification = (CheckBoxPreference) findPreference("echoChamberAuto");
        _blockNotification.setEnabled(_echoNotification.isChecked());

        _echoNotification.setOnPreferenceChangeListener(

                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(final Preference preference, Object newValue) {
                        if (newValue instanceof Boolean) {
                            final Boolean checked = (Boolean) newValue;

                            boolean verified = _prefs.getBoolean("usernameVerified", false);
                            if (!verified) {
                                LoginForm login = new LoginForm(getActivity());
                                login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {

                                    @Override
                                    public void onSuccess() {
                                        _blockNotification.setEnabled(checked);
                                    }

                                    @Override
                                    public void onFailure() {
                                        _echoNotification.setChecked(false);
                                        _blockNotification.setEnabled(false);
                                    }
                                });
                            } else {
                                _blockNotification.setEnabled(checked);
                            }
                            return true;
                        }
                        return false;
                    }

                });
    }

    /*
     * BLOCKLIST
     */


    public void addBlock()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Remove from Echo Chamber");
        // Set up the input
        final LinearLayout lay = new LinearLayout(getActivity());
        lay.setOrientation(LinearLayout.VERTICAL);
        final TextView tv = new TextView(getActivity());
        tv.setText("This user will be removed from your personal echo chamber so you do not need to be offended by their views. All posts and replies to this user's posts will be removed (unless palatize option is selected). You can also click on their username in a thread and select \"block user\". Case insensitive.");
        final EditText input = new EditText(getActivity());
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        lay.addView(tv);
        lay.addView(input);
        builder.setView(lay);

        builder.setPositiveButton("Add Block", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                _progressDialog = MaterialProgressDialog.show(getActivity(), "Escorting user from Echo Chamber", "Communicating with Shack Browse server...", true, true);

                mEchoServerInteract.doBlocklistTask(NetworkEchoChamberServer.ACTION_ADD, input.getText().toString());

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showBlocklist();
            }
        });
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }
    public void removeBlock(final String keyword)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Remove Blocked User");

        builder.setMessage("Stop blocking " + keyword + "?");

        builder.setPositiveButton("Stop Blocking", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                _progressDialog = MaterialProgressDialog.show(getActivity(), "Removing Blocked User", "Communicating with Shack Browse server...", true, true);
                mEchoServerInteract.doBlocklistTask(NetworkEchoChamberServer.ACTION_REMOVE, keyword);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showBlocklist();
            }
        });
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }
    public void showBlocklist()
    {
        String empty = "";
        ArrayList<String> list = new ArrayList<String>();
        JSONArray jsonArray = (JSONArray)((MainActivity)getActivity()).mBlockList;
        if (jsonArray != null) {
            int len = jsonArray.length();
            for (int i=0;i<len;i++){
                try {
                    list.add(jsonArray.get(i).toString());
                }
                catch (Exception e) { e.printStackTrace(); }
            }
            if (len == 0)
            {
                empty = " (empty)";
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Echo Chamber" + empty);
        final CharSequence[] items = list.toArray(new CharSequence[list.size()]);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                removeBlock(list.get(item));
            }});
        builder.setNegativeButton("Close", null);
        builder.setPositiveButton("Add Block", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                addBlock();
            }
        });
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }

    @Override
    public void onActivityCreated(Bundle bundle)
    {
        super.onActivityCreated(bundle);
    }

    @Override public void onResume()
    {
        super.onResume();
    }
}

