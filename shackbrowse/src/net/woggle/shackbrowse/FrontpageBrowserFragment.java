package net.woggle.shackbrowse;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.ClipboardManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by brad on 2/1/2015.
 */
public class FrontpageBrowserFragment extends Fragment {

    private SharedPreferences mPrefs;
    private boolean mViewAvailable;
    WebView mWebview;
    public int mState;
    public static final int BROWSER = 100;
    public static final int SHOW_ZOOM_CONTROLS = 200;
    private String mFirstHref;
    public boolean mSplashSuppress = false;

    public boolean mLoading = true;

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
                                            if ((progress > 88) && (mLoading == true))
                                            {
                                                if ((getActivity() != null) && (mSplashSuppress == false)) {
                                                    ((MainActivity) getActivity()).hideLoadingSplash();
                                                    ((MainActivity) getActivity()).showOnlyProgressBarFromPTRLibrary(false);
                                                }
                                                mSplashSuppress = false;
                                                mLoading = false;
                                            }
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
                    public WebResourceResponse shouldInterceptRequest (final WebView view, String url) {
                        ArrayList<String> list = new ArrayList<String>();
                        URL res = null;
                        //if (true) return null;
                        try {
                                list = new ArrayList<String>(Arrays.asList(getActivity().getAssets().list("frontpage")));
                                res = new URL(url);
                        }catch(Exception e){
                                e.printStackTrace();
                                return null; // give up here if there is exception
                        }

                        boolean contains = false;
                        String asset = null;
                        for (String assetTest: list)
                        {
                            if (assetTest.contains(getLastBitFromUrl(res.getPath())))
                            {
                                System.out.println("INTERCEPTED!" + assetTest);
                                asset = assetTest;
                                contains = true;
                                break;
                            }
                        }
                        
                        if (contains) {
                            // present local resource for loading
                            WebResourceResponse response;
                            if (getFileExtension(getLastBitFromUrl(res.getPath())).equals("css"))
                            {
                                response = getWebResourceResponseFromAsset(asset, "text/css");
                            }
                            else if (getFileExtension(getLastBitFromUrl(res.getPath())).equals("js"))
                            {
                                response = getWebResourceResponseFromAsset(asset, "text/javascript");
                            }
                            else {
                                // otherwise dont intercept
                                return null;
                            }
                            System.out.println("INTERCEPTED SENT!" + asset);
                            return response;
                        } else {
                            // otherwise dont intercept
                            return null; //super.shouldInterceptRequest(view, url);
                        }
                    }

                    /**
                     * Return WebResourceResponse with CSS markup from an asset (e.g. "assets/style.css").
                     */
                    private WebResourceResponse getWebResourceResponseFromAsset(String asset, String mimeType) {
                        try {
                            return new WebResourceResponse(mimeType, "UTF-8",getActivity().getAssets().open("frontpage/" + asset));
                        } catch (IOException e) {
                            System.out.println("EXCEOPTIOON" + asset);
                            e.printStackTrace();
                            return null;
                        }
                    }

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        if ((getActivity() != null) && (mSplashSuppress == false)) {
                            ((MainActivity) getActivity()).showLoadingSplash();
                            ((MainActivity) getActivity()).showOnlyProgressBarFromPTRLibrary(true);
                            mLoading = true;
                        }
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {

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
    public static String getLastBitFromUrl(final String url){
        // return url.replaceFirst("[^?]*/(.*?)(?:\\?.*)","$1);" <-- incorrect
        return url.replaceFirst(".*/([^/?]+).*", "$1");
    }

    public static String getFileExtension(String path)
    {
        int i = path.lastIndexOf('.');
        if (i >= 0) {
            return path.substring(i+1);
        }
        else return path;
    }
    public String getHREFText()
    {
        String copyText = mWebview.getUrl();
        return copyText;
    }

    public void open(String href) {
        mWebview.loadUrl(href); mLoading = true;
    }

    public void setFirstOpen(String href) {
        mFirstHref = href;
    }

    public void refresh() {
        if (mWebview != null)
            mWebview.reload();
    }
}
