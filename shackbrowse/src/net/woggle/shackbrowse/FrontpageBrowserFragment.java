package net.woggle.shackbrowse;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SeekBar;
import android.widget.Toast;

/**
 * Created by brad on 2/1/2015.
 */
public class FrontpageBrowserFragment extends Fragment {

    private SharedPreferences mPrefs;
    private boolean mViewAvailable;
    WebView mWebview;
    public int mState;
    final static public String TEST_IMAGE = "arrows.png";
    public static final int BROWSER = 100;
    public static final int SHOW_ZOOM_CONTROLS = 200;
    private String mFirstHref;
    public boolean mSplashSuppress = false;

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
        return inflater.inflate(R.layout.fpbrowser, null);
    }


    @Override
    public void onDestroyView()
    {
        mViewAvailable = false;
        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        mWebview = (WebView) getView().findViewById(R.id.fp_webView);

        mWebview.getSettings().setBuiltInZoomControls(false);
        mWebview.getSettings().setDisplayZoomControls(false);
        mWebview.getSettings().setJavaScriptEnabled(true);
        mWebview.getSettings().setSupportMultipleWindows(true);
        mWebview.getSettings().setAllowFileAccess(false);
        mWebview.getSettings().setDomStorageEnabled(true);
        mWebview.getSettings().setAllowFileAccessFromFileURLs(false);
        mWebview.getSettings().setAllowUniversalAccessFromFileURLs(false);
        /*
        if (getActivity() != null)
            ((MainActivity)getActivity()).showOnlyProgressBarFromPTRLibrary(false);
*/
        mWebview.setWebChromeClient(new WebChromeClient() {
                                 @Override
                                        public void onProgressChanged(WebView view, int progress) {

                                        }
                                    });
            	/*
            	if (pb != null && progress < 100)
            	{
            		pb.setVisibility(View.VISIBLE);
            		parent.setVisibility(View.VISIBLE);
            		inactive.setVisibility(View.GONE);
            		pb.bringToFront();
            		pb.setProgress(progress);
            	}
            	*/ /*
                System.out.println("prog:" + progress);
                if (progress >= 85)
                {
                    if (getActivity() != null)
                        ((MainActivity)getActivity()).showOnlyProgressBarFromPTRLibrary(false);
                }
                if (!view.getSettings().getUserAgentString().contentEquals("nothing"))
                    view.setBackgroundColor(Color.WHITE);
            }
        });
*/
        mWebview.setWebViewClient(
                new WebViewClient() {
                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        if ((getActivity() != null) && (mSplashSuppress == false)) {
                            ((MainActivity) getActivity()).showLoadingSplash();
                            ((MainActivity) getActivity()).showOnlyProgressBarFromPTRLibrary(true);
                        }
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        if ((getActivity() != null) && (mSplashSuppress == false)) {
                            ((MainActivity) getActivity()).hideLoadingSplash();
                            ((MainActivity) getActivity()).showOnlyProgressBarFromPTRLibrary(false);
                        }
                        mSplashSuppress = false;
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String _href) {
                        Uri uri = Uri.parse(_href);
                        String id = uri.getQueryParameter("id");
                        if ((uri.getHost().equalsIgnoreCase("www.shacknews.com") || uri.getHost().equalsIgnoreCase("shacknews.com")) && id != null) {
                            ((MainActivity) getActivity()).openThreadViewAndSelect(Integer.parseInt(id));
                            return true;
                        } else if ((uri.getHost().equalsIgnoreCase("www.shacknews.com") || uri.getHost().equalsIgnoreCase("shacknews.com")) && uri.getPath().toLowerCase().contains("article")) {
                            ((MainActivity) getActivity()).openInArticleViewer(uri.toString());
                    /*
                    if (getActivity() != null)
                        ((MainActivity)getActivity()).showOnlyProgressBarFromPTRLibrary(false);
                        */
                            return true;
                        }
                        return false;
                    }
                });

        // mWebview.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
        mWebview.getSettings().setUseWideViewPort(false);
        mWebview.getSettings().setLoadWithOverviewMode(false);
        mWebview.setBackgroundColor(Color.BLACK);

        mWebview.loadUrl(mFirstHref);
    }

    // reset the progress bars when we are detached from the activity
    @Override
    public void onStop()
    {
        /*
        if (getActivity() != null) {
            ((MainActivity) getActivity()).mSOPBFPTRL = true;
            ((MainActivity) getActivity()).showOnlyProgressBarFromPTRLibrary(false);
        }
        */
        super.onStop();
    }

    public void openExternal() {
        if (getActivity() != null)
        {
            Intent i = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(mWebview.getUrl()));
            getActivity().startActivity(i);
        }
    }

    public void copyURL()
    {
        if (getActivity() != null)
        {
            ClipboardManager clipboard = (ClipboardManager)((MainActivity)getActivity()).getSystemService(Activity.CLIPBOARD_SERVICE);
            clipboard.setText(getHREFText());
            Toast.makeText(getActivity(), getHREFText(), Toast.LENGTH_SHORT).show();
        }
    }
    public void shareURL()
    {
        if (getActivity() != null)
        {
            Toast.makeText(getActivity(), getHREFText(), Toast.LENGTH_SHORT).show();
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, getHREFText());
            sendIntent.setType("text/plain");
            getActivity().startActivity(Intent.createChooser(sendIntent, "Share Link"));
        }
    }
    public String getHREFText()
    {
        String copyText = mWebview.getUrl();
        return copyText;
    }

    public void open(String href) {
        mWebview.loadUrl(href);
    }

    public void setFirstOpen(String href) {
        mFirstHref = href;
    }

    public void refresh() {
        if (mWebview != null)
            mWebview.reload();
    }
}
