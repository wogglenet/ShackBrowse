package net.swigglesoft.shackbrowse;

import org.json.JSONArray;
import org.json.JSONException;

import net.swigglesoft.shackbrowse.NetworkNotificationServers.OnGCMInteractListener;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.net.Uri;
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

import androidx.appcompat.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import com.afollestad.materialdialogs.color.ColorChooserDialog;

public class PreferenceFragmentNotifications extends PreferenceFragment
{
    private static final String TAG = "PreferenceFragmentNotifications";
    private SharedPreferences _prefs;

    protected MaterialDialog _progressDialog;
    private NetworkNotificationServers _GCMAccess;

    private CheckBoxPreference _vanityNotification;

    private CheckBoxPreference _noteEnabled;

    private CheckBoxPreference _repliesNotification;

    private boolean _Venabled;

    private Preference _keyNotification;
    private OnGCMInteractListener mGCMlistener;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        _prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        addPreferencesFromResource(R.xml.preferences_notifications);

        final Context fincon = getActivity();
        final AppCompatActivity activ = (AppCompatActivity)getActivity();
        final PreferenceFragment thisFrag = this;
        Preference SMCheckInterval = (Preference)findPreference("PeriodicNetworkServicePeriod");
        try {
            mNoteKeywords = new JSONArray(_prefs.getString(PreferenceKeys.notificationKeywords, "[]"));
        } catch(JSONException e) {
            Log.e(TAG, "Error reading mNoteKeywords", e);
        }

        SMCheckInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int newInterval = Integer.parseInt((String)newValue);

                long updateInterval = (long)newInterval; // DEFAULT 3 HR,  5 minutes 50-100mb, 10 minutes 25-50mb, 30mins 10-20mb, 1 hr 5-10mb, 3 hr 1-3mb, 6hr .5-1.5mb, 12hr .25-1mb

                PeriodicNetworkService.scheduleJob(fincon, updateInterval);

                return true;
            }

        });

        Preference colorNote = (Preference) findPreference("notificationColor2");
        colorNote.setOnPreferenceClickListener(new OnPreferenceClickListener(){

            @Override
            public boolean onPreferenceClick(Preference preference) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                new ColorChooserDialog.Builder(fincon, R.string.notepref_color)

                        .allowUserColorInputAlpha(false)
                        .titleSub(R.string.notepref_colorsub)  // title of dialog when viewing shades of a color
                        .accentMode(false)  // when true, will display accent palette instead of primary palette
                        .doneButton(R.string.md_done_label)  // changes label of the done button
                        .cancelButton(R.string.md_cancel_label)  // changes label of the cancel button
                        .backButton(R.string.md_back_label)  // changes label of the back button
                        .preselect(prefs.getInt("notificationColor", Color.GREEN))  // optionally preselects a color
                        .dynamicButtonColor(true)  // defaults to true, false will disable changing action buttons' color to currently selected color
                        .show((AppCompatActivity)getActivity()); // an AppCompatActivity which implements ColorCallback
                return false;
            }}
        );

        mGCMlistener = new OnGCMInteractListener() {
            @Override
            public void networkResult(String res) {
                Log.d(TAG, "NETWORKSERVERS RESULT" + res);
                Editor edit = _prefs.edit();
                if (res.contains("remove device")) {
                    edit.putBoolean("noteEnabled", false);
                    _noteEnabled.setChecked(false);
                    _vanityNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
                    _keyNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
                    ErrorDialog.display(getContext(), "Deregistration OK", "Deregistered with the Push Notification server.");
                } else if (res.contains("add device")) {
                    edit.putBoolean("noteEnabled", true);
                    _noteEnabled.setChecked(true);
                    _vanityNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
                    _keyNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
                    ErrorDialog.display(getContext(), "Registration OK", "Registered with the Push Notification server.");
                } else if (res.toLowerCase().contains("error")) {
                    ErrorDialog.display(getContext(), "Error registering with Push Notification server", res);
                }
                if (_progressDialog != null) {
                    _progressDialog.dismiss();
                    _progressDialog = null;
                }
                edit.commit();
            }
        };

        _keyNotification = (Preference) findPreference("noteKeywords");
        _keyNotification.setOnPreferenceClickListener(new OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showKeywords();
                return true;

            }
        });

        _repliesNotification = (CheckBoxPreference)findPreference("noteReplies");
        _vanityNotification = (CheckBoxPreference)findPreference("noteVanity");
        _noteEnabled = (CheckBoxPreference)findPreference("noteEnabled");
        // "enableDonatorFeatures"
        _Venabled = true;
        _vanityNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
        _keyNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
        _repliesNotification.setChecked(_prefs.getBoolean(PreferenceKeys.notificationOnReplies, false));
        _vanityNotification.setChecked(_prefs.getBoolean(PreferenceKeys.notificationOnVanity, false));

        OnPreferenceChangeListener notificationOnPrefListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, Object newValue) {
                if(newValue instanceof Boolean){
                    final Boolean checked = (Boolean)newValue;

                    boolean verified = _prefs.getBoolean("usernameVerified", false);
                    if (!verified)
                    {
                        LoginForm login = new LoginForm(getActivity());
                        login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {

                            @Override
                            public void onSuccess() {
                                showChangingNotificationsProgress();

                                _GCMAccess = new NetworkNotificationServers(getActivity(), mGCMlistener);
                                if (checked)
                                    _GCMAccess.doRegisterTask("reg");
                                else
                                    _GCMAccess.doRegisterTask("unreg");
                            }

                            @Override
                            public void onFailure() {
                                _noteEnabled.setChecked(false);
                                _vanityNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
                                _keyNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
                            }
                        });
                    }
                    else
                    {
                        showChangingNotificationsProgress();

                        _GCMAccess = new NetworkNotificationServers(getActivity(), mGCMlistener);
                        if (checked)
                            _GCMAccess.doRegisterTask("reg");
                        else
                            _GCMAccess.doRegisterTask("unreg");
                    }
                }
                return false;
            }

        };

        _noteEnabled.setOnPreferenceChangeListener(notificationOnPrefListener);

        OnPreferenceChangeListener replOnPrefListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, Object newValue) {
                if(newValue instanceof Boolean){
                    final Boolean checked = (Boolean)newValue;
                    Editor edit = _prefs.edit();
                    edit.putBoolean(PreferenceKeys.notificationOnReplies, checked);
                    edit.commit();
                    _repliesNotification.setChecked(checked);
                }
                return false;
            }
        };
        OnPreferenceChangeListener vanOnPrefListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, Object newValue) {
                if(newValue instanceof Boolean){
                    final Boolean checked = (Boolean)newValue;
                    Editor edit = _prefs.edit();
                    edit.putBoolean(PreferenceKeys.notificationOnVanity, checked);
                    edit.commit();
                    _vanityNotification.setChecked(checked);
                    showChangingNotificationsProgress();
                    _GCMAccess.doRegisterTask("reg");
                }
                return false;
            }
        };
        _repliesNotification.setOnPreferenceChangeListener(replOnPrefListener);
        _vanityNotification.setOnPreferenceChangeListener(vanOnPrefListener);

        Preference channelNote = (Preference) findPreference("notificationChannels");
        Preference vibeNote = (Preference) findPreference("notificationVibrate");
        Preference soundNote = (Preference) findPreference("notificationSound");
        Preference blinkspdNote = (Preference) findPreference("LEDBlinkInMS");
        Preference vibespdNote = (Preference) findPreference("limitVibrateSpamInMS");
        channelNote.setOnPreferenceClickListener(new OnPreferenceClickListener()
                                                 {
                                                     @Override
                                                     public boolean onPreferenceClick(Preference preference)
                                                     {
                                                         Intent intent = new Intent();
                                                         if(android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1){
                                                             intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                                                             intent.putExtra("android.provider.extra.APP_PACKAGE", getActivity().getPackageName());
                                                         }else if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                                                             intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                                                             intent.putExtra("app_package", getActivity().getPackageName());
                                                             intent.putExtra("app_uid", getActivity().getApplicationInfo().uid);
                                                         }else {
                                                             intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                             intent.addCategory(Intent.CATEGORY_DEFAULT);
                                                             intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
                                                         }

                                                         getActivity().startActivity(intent);
                                                         return false;
                                                     }
                                                 });
                PreferenceCategory pCategory = (PreferenceCategory) findPreference("notificationCat");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            pCategory.removePreference(colorNote);
            pCategory.removePreference(vibeNote);
            pCategory.removePreference(soundNote);
            pCategory.removePreference(blinkspdNote);
            pCategory.removePreference(vibespdNote);
        }
        else
        {
            pCategory.removePreference(channelNote);
        }

    }

    private void showProgressDialog(String title) {
        _progressDialog = MaterialProgressDialog.show(getActivity(), title, "Communicating with Shack Browse server...", true, true);
    }

    private void showChangingNotificationsProgress() {
        showProgressDialog("Changing Notification Status");
    }

    private void showAddKeywordProgress() {
        showProgressDialog("Adding Keyword");
    }

    private void showRemoveKeywordProgress() {
        showProgressDialog("Removing Keyword");
    }

    /*
     * KEYWORDS
     */
    JSONArray mNoteKeywords = new JSONArray();
    public void addKeyword()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Add Keyword Notification");
        // Set up the input
        final LinearLayout lay = new LinearLayout(getActivity());
        lay.setOrientation(LinearLayout.VERTICAL);
        final TextView tv = new TextView(getActivity());
        tv.setText("A notification will be sent to you whenever this keyword is posted by another user.");
        final EditText input = new EditText(getActivity());
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        lay.addView(tv);
        lay.addView(input);
        builder.setView(lay);

        builder.setPositiveButton("Add Keyword", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mNoteKeywords.put(input.getText().toString());
                showAddKeywordProgress();
                updateKeywords();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showKeywords();
            }
        });
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }

    private void updateKeywords() {
        Editor ed = _prefs.edit();
        ed.putString(PreferenceKeys.notificationKeywords, mNoteKeywords.toString());
        ed.commit();
        showKeywords();
        try {
            _GCMAccess.doRegisterTask("reg");
        } catch(Exception e) {
            Log.e(TAG, "Exception in updateKeywords", e);
        }
    }

    public void removeKeyword(final int item)
    {
        String keyword = "";
        try {
            keyword = mNoteKeywords.getString(item);
        } catch (JSONException e) {
            Log.e(TAG, "Exception in removeKeyword", e);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Remove Notification Keyword");

        builder.setMessage("Stop notifications for " + keyword + "?");

        builder.setPositiveButton("Stop Notifying", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mNoteKeywords.remove(item);
                showRemoveKeywordProgress();
                updateKeywords();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showKeywords();
            }
        });
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }
    public void showKeywords()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Keyword Notifications");
        final CharSequence[] items = new CharSequence[mNoteKeywords.length()];
        for(int i = 0; i < mNoteKeywords.length(); i++) {
            try {
                items[i] = mNoteKeywords.getString(i);
            } catch (JSONException e) {
                Log.e(TAG, "Exception in showKeywords", e);
            }
        }
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                removeKeyword(item);
            }});
        builder.setNegativeButton("Close", null);
        builder.setPositiveButton("Add Keyword", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                addKeyword();
            }
        });
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }

    @Override
    public void onActivityCreated(Bundle bundle)
    {
        _GCMAccess = new NetworkNotificationServers(getActivity(), mGCMlistener);

        super.onActivityCreated(bundle);
    }

    @Override public void onResume()
    {

        if (!_GCMAccess.checkPlayServices()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("No Play Services");
            builder.setMessage("You need to have Google Play Services installed and updated to receive notifications.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    ((MainActivity)getActivity()).setContentTo(MainActivity.CONTENT_PREFS);
                }
            });
            builder.setNeutralButton("Update", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    String appPackageName = "com.google.android.gms";
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                    } catch (android.content.ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                    }
                }
            });
            builder.show();
        }
        super.onResume();
    }
}
