package net.woggle.shackbrowse;

import net.woggle.CustomClickableSpan;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.ClipboardManager;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.webkit.WebView;
import android.widget.Toast;

import java.util.regex.Pattern;

import static net.woggle.shackbrowse.StatsFragment.statInc;

public class CustomURLSpan extends CustomClickableSpan implements OnLongClickListener {
	private String href;


	public CustomURLSpan(String href) {
		// TODO Auto-generated constructor stub
		super();
		this.href = addHttp(href);
	}


	public static Dialog dialog;
	
	public String getURL()
	{
		return href;
	}

    public String addHttp(String url) {
        if (!Pattern.compile("^(?:f|ht)tps?://.*", Pattern.DOTALL).matcher(url).matches()) {
            url = "http://" + url;
        }

        return url;
    }
	/*
	 * DEPRECATED 
	 * LONGCLICK NOT ACTUALLY USED
	 * 
	 */
	@Override
	public boolean onLongClick (View v)
	{
		final View view = v;
		final FragmentActivity activ = ((FragmentActivity)v.getContext());
		v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
		activ.runOnUiThread(new Runnable(){
    		@Override public void run()
    		{
                AlertDialog.Builder builder = new AlertDialog.Builder(activ);
    			builder.setTitle("Choose Link Action");
    	        final CharSequence[] items = { "Copy URL","Share Link","Open Externally", "Open in popup browser"};
    	        builder.setItems(items, new DialogInterface.OnClickListener() {
    	            public void onClick(DialogInterface dialog, int item) {
    	                if (item == 0)
    	                {
    	                	ClipboardManager clipboard = (ClipboardManager)activ.getSystemService(Activity.CLIPBOARD_SERVICE);
    	                	clipboard.setText(href);
    	                	Toast.makeText(activ, href, Toast.LENGTH_SHORT).show();
    	                }
    	                if (item == 1)
    	                {
    	                	Intent sendIntent = new Intent();
    	            	    sendIntent.setAction(Intent.ACTION_SEND);
    	            	    sendIntent.putExtra(Intent.EXTRA_TEXT, href);
    	            	    sendIntent.setType("text/plain");
    	            	    activ.startActivity(Intent.createChooser(sendIntent, "Share Link"));
    	                }
    	                if (item == 2)
    	                {
    	                	Intent i = new Intent(Intent.ACTION_VIEW, 
    	             		       Uri.parse(href));
    	             		activ.startActivity(i);
    	                }
    	                if (item == 3)
    	                {
    	                	((MainActivity)view.getContext()).openBrowser(href);
    	                }
    	                }});
    	        AlertDialog alert = builder.create();
    	        alert.setCanceledOnTouchOutside(true);
    	        alert.show();
    		}
		});
		return true;
	}

	// fix for support library sucking
	private Activity getActivityFromView(View v) {
		Context context = v.getContext();
		while (context instanceof ContextWrapper) {
			if (context instanceof Activity) {
				return (Activity)context;
			}
			context = ((ContextWrapper)context).getBaseContext();
		}
		return null;
	}

	@Override
	public void onClick (View v) 
	{
		Activity test = getActivityFromView(v);

		if (test instanceof MainActivity)
		{
			MainActivity mAct = (MainActivity)test;

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mAct);
			statInc(mAct, "ClickedLink");
			int _useBrowser = Integer.parseInt(prefs.getString("usePopupBrowser2", "1"));

			// see if we can address URL internally
			int newId = 0;
			if (
					href.contains("://www.shacknews.com/chatty?id=")
							|| href.contains("://shacknews.com/chatty?id=")
							|| href.contains("://www.shacknews.com/chatty/laryn.x?id=")
							|| href.contains("://shacknews.com/chatty/laryn.x?id=")
							|| href.contains("://www.shacknews.com/laryn.x?id=")
							|| href.contains("://shacknews.com/laryn.x?id=")
							|| href.contains("://www.shacknews.com/chatty/ja.zz?id=")
							|| href.contains("://shacknews.com/chatty/ja.zz?id=")
							|| href.contains("://www.shacknews.com/chatty/funk.y?id=")
							|| href.contains("://shacknews.com/chatty/funk.y?id=")
							|| (href.contains("shacknews.com/article") && ((_useBrowser == 1) || (_useBrowser == 0)))
					)
			{
				if (href.contains("shacknews.com/article")) // simple removal of article viewer
				{
					if (mAct.getSliderOpen() && !mAct.getDualPane())
					{
						mAct._tviewFrame.closeLayer(true);
					} else if (mAct._sresFrame.isOpened())
					{
						mAct._sresFrame.closeLayer(true);
					}
					mAct.openInArticleViewer(href);
					return;
				} else
				{
					Uri uri = Uri.parse(getURL());
					try
					{
						newId = Integer.valueOf(uri.getQueryParameter("id").trim());
					} catch (NumberFormatException e)
					{
						Toast.makeText(mAct, "Invalid URL, could not open thread internally", Toast.LENGTH_SHORT).show();
					}
				}
			}

			// fix youtube app not handling their own youtu.be url shortener
			if (href.contains("youtu.be"))
			{
				String[] splt = href.split("youtu.be/");
				if (splt.length > 1)
				{
					href = "http://www.youtube.com/watch?v=" + splt[1];
				}
			}

			if (newId > 0)
			{
				System.out.println("opening new thread: " + newId);

				int currentPostId = (mAct)._threadView._adapter.getItem(mAct._threadView._lastExpanded).getPostId();
				mAct.addToThreadIdBackStack(currentPostId);
				mAct.openThreadViewAndSelectWithBackStack(newId);

			} else if (((_useBrowser == 0) && (!href.contains("play.google"))) || ((_useBrowser == 1) && (!href.contains("youtu.be")) && (!href.contains("youtube")) && (!href.contains("twitter")) && (!href.contains("play.google"))) || ((_useBrowser == 2) && (PopupBrowserFragment.isImage(href))))
			{
				mAct.openBrowser(href);

			} else
			{
				Uri u = Uri.parse(this.getURL());
				if (u.getScheme() == null)
				{
					u = Uri.parse("http://" + href);
				}
				Intent i = new Intent(Intent.ACTION_VIEW, u);
				mAct.startActivity(i);
			}
		}
	}
}
