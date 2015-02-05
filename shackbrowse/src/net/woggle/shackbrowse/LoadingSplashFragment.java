package net.woggle.shackbrowse;

import android.animation.Animator;
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        randomizeTagline();
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
