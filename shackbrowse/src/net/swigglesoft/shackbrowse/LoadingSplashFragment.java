package net.swigglesoft.shackbrowse;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Random;

import static net.swigglesoft.shackbrowse.StatsFragment.statInc;

/**
 * Created by brad on 2/5/2015.
 */
public class LoadingSplashFragment extends Fragment {
    private boolean mViewAvailable;
    private SharedPreferences mPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    public View getParentView() { return getView(); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        mViewAvailable = true;
        return inflater.inflate(R.layout.loading_splash, null);
    }


    @Override
    public void onDestroyView()
    {
        mViewAvailable = false;
        super.onDestroyView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        getActivity().findViewById(R.id.splash_tagline).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                randomizeTagline();
                statInc(v.getContext(), "ClickedOnLoadingSplashScreen");
            }
        });
        getActivity().findViewById(R.id.splash_tagline).setBackgroundResource(MainActivity.getThemeResource(getActivity(),R.attr.colorHighlight));
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        randomizeTagline();

        showEcho();
    }

    public void showEcho()
    {
        if (getView() != null) {
            TextView echo = (TextView) ((View) getView()).findViewById(R.id.echo_chamber);
            String echostatus = "";
            String autostatus = "";
            boolean echoEnabled = mPrefs.getBoolean("echoEnabled", false);
            boolean echoPalatize = mPrefs.getBoolean("echoPalatize", false);
            if (mPrefs.getBoolean("echoChamberAuto", true) && echoEnabled) {
                autostatus = "AutoChamber " + (echoPalatize ? "palatizing" : "blocking") + ": " + ((MainActivity) getActivity()).getFancyBlockList(true);
            } else {
                autostatus = "(AutoChamber off)";
            }
            if (echoEnabled) {
                echostatus = (echoPalatize ? "palatizing" : "blocking") + ": " + ((MainActivity) getActivity()).getFancyBlockList(false);
            } else {
                echostatus = "off";
            }

            echo.setText("Echo Chamber: " + echostatus + "\r\n" + autostatus);
        }
    }
    public void randomizeTagline() {
        if (getView() != null) {
            TextView tline = (TextView) ((View) getView()).findViewById(R.id.splash_tagline);
            String[] array = getResources().getStringArray(R.array.taglines);
            tline.setText(array[randInt(0, (array.length - 1))]);

            // check if is beta or not
            String thisversion;
            try {
                thisversion = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                // TODO Auto-generated catch block
                thisversion = "unknown";
            }
            TextView sbname = (TextView) ((View) getView()).findViewById(R.id.splash_sbname);
            sbname.setText("Shack Browse" + (thisversion.toLowerCase().contains("beta") ? " Beta" : ""));
        }
    }

    public static int randInt(int min, int max) {

        // NOTE: Usually this should be a field rather than a method
        // variable so that it is not re-seeded every call.
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }

}
