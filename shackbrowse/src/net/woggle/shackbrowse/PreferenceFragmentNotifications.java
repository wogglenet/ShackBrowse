package net.woggle.shackbrowse;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import net.woggle.shackbrowse.NetworkNotificationServers.OnGCMInteractListener;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import com.afollestad.materialdialogs.color.ColorChooserDialog;

public class PreferenceFragmentNotifications extends PreferenceFragment
{
    private SharedPreferences _prefs;

    protected MaterialDialog _progressDialog;
    private NetworkNotificationServers _GCMAccess;

    private CheckBoxPreference _vanityNotification;

    private CheckBoxPreference _noteEnabled;

    private CheckBoxPreference _repliesNotification;

    private boolean _Venabled;

    private Preference _keyNotification;
    private Preference _devicesNotification;
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
        SMCheckInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int newInterval = Integer.parseInt((String)newValue);

                long updateInterval = (long)newInterval; // DEFAULT 3 HR,  5 minutes 50-100mb, 10 minutes 25-50mb, 30mins 10-20mb, 1 hr 5-10mb, 3 hr 1-3mb, 6hr .5-1.5mb, 12hr .25-1mb

                PeriodicNetworkService.scheduleJob(fincon, updateInterval);

                return true;
            }

        });

        Preference testNote = (Preference) findPreference("pref_testnote");
        testNote.setOnPreferenceClickListener(new OnPreferenceClickListener(){

                                                  @Override
                                                  public boolean onPreferenceClick(Preference preference) {
                                                      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

                                                      Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
                                                      NotificationCompat.Builder mBuilder =
                                                              new NotificationCompat.Builder(getActivity())
                                                                      .setSmallIcon(R.drawable.note_logo)
                                                                      .setLargeIcon(largeIcon)
                                                                      .setContentTitle("Test")
                                                                      .setContentText("Only a test")
                                                                      .setTicker("Test Notification")
                                                                      .setAutoCancel(true);

                                                      NotificationManager mNotificationManager =
                                                              (NotificationManager)getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

                                                      PendingIntent notifyPIntent = PendingIntent.getActivity(getActivity().getApplicationContext(), 0, new Intent(), 0);
                                                      mBuilder.setContentIntent(notifyPIntent);

                                                      Notification notification = mBuilder.build();
                                                      notification.sound = Uri.parse(prefs.getString("notificationSound", "DEFAULT_SOUND"));

                                                      if (prefs.getBoolean("notificationVibrate", true))
                                                          notification.defaults|= Notification.DEFAULT_VIBRATE;

                                                      notification.flags |= Notification.FLAG_AUTO_CANCEL;
                                                      notification.flags |= Notification.FLAG_SHOW_LIGHTS;

                                                      notification.ledARGB = prefs.getInt("notificationColor", Color.GREEN);
                                                      notification.ledOffMS = Integer.parseInt(prefs.getString("LEDBlinkInMS", "2000"));
                                                      notification.ledOnMS = (int)(Integer.parseInt(prefs.getString("LEDBlinkInMS", "2000")) / 10);
                                                      // mId allows you to update the notification later on.
                                                      int mId = 58401;
                                                      mNotificationManager.notify(mId, notification);

                                                      return false;
                                                  }}
        );

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

        mGCMlistener = new OnGCMInteractListener(){
            @Override
            public void networkResult(String res) {
                System.out.println("NETWORKSERVERS RESULT" + res);
                Editor edit = _prefs.edit();
                if (res.contains("remove device"))
                {
                    edit.putBoolean("noteEnabled", false);
                    _noteEnabled.setChecked(false);
                    _vanityNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
                    _keyNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
                    _devicesNotification.setEnabled(false);
                    if (_progressDialog != null)
                    {
                        _progressDialog.dismiss();
                        _progressDialog = null;
                    }
                }
                else if (res.contains("add device"))
                {
                    edit.putBoolean("noteEnabled", true);
                    _noteEnabled.setChecked(true);
                    _GCMAccess.doUserInfoTask();
					/*
					_vanityNotification.setChecked(false);
					_repliesNotification.setChecked(true);
					edit.putBoolean("noteReplies", true);
					edit.putBoolean("noteVanity", false); */
                    _vanityNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
                    _keyNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
                }
                else if (res.contains("already exists"))
                {
                    // likely a vanity.repl update
                    _GCMAccess.doUserInfoTask();
                }
                else
                {
                    if (_progressDialog != null)
                    {
                        _progressDialog.dismiss();
                        _progressDialog = null;
                    }
                }
                edit.commit();
            }

            @Override
            public void userResult(JSONObject result) {
                Editor edit = _prefs.edit();
                try{
                    if (result == null)
                    {
                        edit.putBoolean("noteVanity", false);
                        _vanityNotification.setChecked(false);
                        edit.putBoolean("noteReplies", false);
                        _repliesNotification.setChecked(false);
                        _noteEnabled.setChecked(false);
                        _vanityNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
                        _keyNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
                        _devicesNotification.setEnabled(false);
                        mNoteKeywords = new ArrayList<String>();
                        edit.putBoolean("noteEnabled", false);
                    }
                    else
                    {
                        System.out.println("TRYING TO READ USERINFO" + result.getString("get_vanity") + result.getString("get_replies"));
                        if ("1".equals(result.getString("get_vanity")))
                        {
                            edit.putBoolean("noteVanity", true);
                            _vanityNotification.setChecked(true);
                        }
                        else
                        {
                            edit.putBoolean("noteVanity", false);
                            _vanityNotification.setChecked(false);
                        }
                        if ("1".equals(result.getString("get_replies")))
                        {
                            edit.putBoolean("noteReplies", true);
                            _repliesNotification.setChecked(true);
                        }
                        else
                        {
                            edit.putBoolean("noteReplies", false);
                            _repliesNotification.setChecked(false);
                        }
                        if (result.getJSONArray("devices").join("::").contains(_GCMAccess.getRegistrationId()))
                        {
                            edit.putBoolean("noteEnabled", true);
                            _noteEnabled.setChecked(true);
                            _vanityNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
                            _keyNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
                            _devicesNotification.setEnabled(true);
                            _devicesNotification.setTitle("Devices: " + result.getJSONArray("devices").length());
                        }
                        else
                        {
                            edit.putBoolean("noteEnabled", false);
                            _noteEnabled.setChecked(false);
                            _vanityNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
                            _keyNotification.setEnabled(_Venabled && _noteEnabled.isChecked());
                            _devicesNotification.setTitle("Devices: " + result.getJSONArray("devices").length());
                            _devicesNotification.setEnabled(false);
                        }

                        mNoteKeywords = new ArrayList<String>();
                        JSONArray keywordArr = result.getJSONArray("keywords");
                        if ((keywordArr != null) && (keywordArr.length() > 0))
                        {
                            for (int i=0;i<keywordArr.length();i++)
                            {
                                mNoteKeywords.add(keywordArr.get(i).toString());
                            }
                        }
                    }
                } catch (Exception e) {}
                edit.commit();
                if (_progressDialog != null)
                {
                    _progressDialog.dismiss();
                    _progressDialog = null;
                }
            }};

        ((Preference)findPreference("notificationEditOnline")).setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.woggle.net/notifications"));
                startActivity(browserIntent);
                return false;
            }
        });
        _devicesNotification = (Preference) findPreference("notificationDevices");
        _devicesNotification.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                new MaterialDialog.Builder(getActivity()).title("Delete all other devices?").content("Are you sure? This will prevent all of your other devices from receiving notifications, and only this device will receive them until you re-enable notifications on other devices.").positiveText("Remove other devices").negativeText("Cancel")
                        .onPositive(new MaterialDialog.SingleButtonCallback()
                        {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which)
                            {
                                new MaterialDialog.Builder(getActivity()).title("Seriously?").content("Your other devices will be unregistered and will need to be registered on each device. Only use this if your notifications are broken.").positiveText("Remove all other devices").negativeText("Cancel")
                                .onPositive(new MaterialDialog.SingleButtonCallback()
                                {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which)
                                    {
                                        _progressDialog = MaterialProgressDialog.show(getActivity(), "Adding Keyword", "Communicating with Shack Browse server...", true, true);
                                        _GCMAccess.doUserInfoTask("remallexcept", null);
                                    }
                                }).show();
                            }
                        }).show();
                return false;
            }
        });
        _devicesNotification.setTitle("Devices: #unknown");

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
                                _progressDialog = MaterialProgressDialog.show(getActivity(), "Changing Notification Status", "Communicating with Shack Browse server...", true, true);

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
                                _devicesNotification.setEnabled(false);
                            }
                        });
                    }
                    else
                    {
                        _progressDialog = MaterialProgressDialog.show(getActivity(), "Changing Notification Status", "Communicating with Shack Browse server...", true, true);

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
                    _progressDialog = MaterialProgressDialog.show(getActivity(), "Changing Notification Status", "Communicating with Shack Browse server...", true, true);
                    _GCMAccess = new NetworkNotificationServers(getActivity(), mGCMlistener);
                    if (checked)
                        _GCMAccess.updReplVan(true,_prefs.getBoolean("noteVanity", false));
                    else
                        _GCMAccess.updReplVan(false,_prefs.getBoolean("noteVanity", false));
                }
                return false;
            }
        };
        OnPreferenceChangeListener vanOnPrefListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, Object newValue) {
                if(newValue instanceof Boolean){
                    final Boolean checked = (Boolean)newValue;
                    _progressDialog = MaterialProgressDialog.show(getActivity(), "Changing Notification Status", "Communicating with Shack Browse server...", true, true);
                    _GCMAccess = new NetworkNotificationServers(getActivity(), mGCMlistener);
                    if (checked)
                        _GCMAccess.updReplVan(_prefs.getBoolean("noteReplies", true), true);
                    else
                        _GCMAccess.updReplVan(_prefs.getBoolean("noteReplies", true), false);
                }
                return false;
            }
        };
        _repliesNotification.setOnPreferenceChangeListener(replOnPrefListener);
        _vanityNotification.setOnPreferenceChangeListener(vanOnPrefListener);

    }

    /*
     * KEYWORDS
     */
    ArrayList<String> mNoteKeywords = new ArrayList<String>();
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
                mNoteKeywords.add(input.getText().toString());
                _progressDialog = MaterialProgressDialog.show(getActivity(), "Adding Keyword", "Communicating with Shack Browse server...", true, true);
                _GCMAccess.doUserInfoTask("addkeyword", input.getText().toString());
                showKeywords();
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
    public void removeKeyword(final String keyword)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Remove Notification Keyword");

        builder.setMessage("Stop notifications for " + keyword + "?");

        builder.setPositiveButton("Stop Notifying", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mNoteKeywords.remove(keyword);
                _progressDialog = MaterialProgressDialog.show(getActivity(), "Removing Keyword", "Communicating with Shack Browse server...", true, true);
                _GCMAccess.doUserInfoTask("removekeyword", keyword);
                showKeywords();
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
        final CharSequence[] items = mNoteKeywords.toArray(new CharSequence[mNoteKeywords.size()]);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                removeKeyword(mNoteKeywords.get(item));
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
        else
        {
            _progressDialog = MaterialProgressDialog.show(getActivity(), "Checking Notification Status", "Communicating with Shack Browse server...", true, true);
            _GCMAccess.doUserInfoTask();
        }
        super.onResume();
    }
}
