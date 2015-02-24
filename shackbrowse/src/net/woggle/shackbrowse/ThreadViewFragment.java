package net.woggle.shackbrowse;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.app.ListFragment;
import android.support.v4.util.LruCache;
import android.text.ClipboardManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialogCompat;
import com.nhaarman.listviewanimations.itemmanipulation.ExpandCollapseListener;

import net.woggle.CheckableTableLayout;
import net.woggle.CustomLinkMovementMethod;
import net.woggle.ExpandableListItemAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

public class ThreadViewFragment extends ListFragment
{
    public PostLoadingAdapter _adapter;
    int _rootPostId = 0;
    // int _currentPostId = 0;
    int _selectPostIdAfterLoading = 0;
    // String _currentPostAuthor;
    boolean _highlighting = false;
    public String _highlight = "";
    
    final static int FRAGMENT_ID = 20;
    final static int MENU_COPY_URL = 0;
    final static int MENU_COPY_TEXT = 1;
	
	int _lastExpanded = 0;
    
    private int _userNameHeight = 0;
    
    boolean _touchedFavoritesButton = false;
    
    MaterialDialog _progressDialog;
    
    // list view saved state while rotating
    private Parcelable _listState = null;
    private int _listPosition = 0;
    private int _itemPosition = 0;
    private int _itemChecked = ListView.INVALID_POSITION;
    
    JSONObject _lastThreadJson;
    
    private boolean _refreshRestore = false;
	private boolean _viewAvailable;
	
	private int _postYLoc = 0;
	boolean _autoFaveOnLoad = false;
	int _messageId = 0;
	String _messageSubject;
	private AsyncTask<String, Void, Void> _curTask;
	protected boolean _showFavSaved = false;
	protected boolean _showUnFavSaved = false;
	private PullToRefreshLayout ptrLayout;
	private SharedPreferences _prefs;
	private boolean _isSelectPostIdAfterLoadingIdaPQPId = false;



    public int getPostId()
    {
        return _rootPostId;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceBundle)
    {
    	
    	super.onCreate(savedInstanceBundle);
    	this.setRetainInstance(true);
    }

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {   
    	_viewAvailable = true;
        return inflater.inflate(R.layout.thread_view, null);
    }
    
    @Override
    public void onDestroyView()
    {
    	_viewAvailable = false;
    	super.onDestroyView();
    }
    
    public void loadPost(Post post)
    {        
		_adapter.add(post);
		
		// needs to be displaypost(position) not (post)
		expandAndCheckPostWithoutAnimation(_adapter.indexOf(post));
    }
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        // set list view up
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        getListView().setDividerHeight(0);
        
        //getListView().setBackgroundColor(getResources().getColor(R.color.collapsed_postbg));
        _prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        
        if (_adapter == null)
        {
        	// first launch, try to set everything up
        	Bundle args = getArguments();
        	String action = getActivity().getIntent().getAction();
        	Uri uri = getActivity().getIntent().getData();
        	
        	// instantiate adapter
        	_adapter = new PostLoadingAdapter(getActivity(), new ArrayList<Post>());
        	setListAdapter(_adapter);
        	_adapter.setAbsListView(getListView());
            _adapter.setTitleViewOnClickListener(getTitleViewOnClickListener());
            _adapter.setExpandCollapseListener(_adapter.mExpandCollapseListener);
        	_adapter.setLimit(2);
        	
        	//  only load this junk if the arguments isn't null
        	if (args != null)
        	{
        		if (args.containsKey("rootPostId"))
            	{
            		_rootPostId = args.getInt("rootPostId");
            		_itemPosition = 0;
            	}
        		if (args.containsKey("messageId"))
            	{
            		_messageId = args.getInt("messageId");
            		_messageSubject = args.getString("messageSubject");
            	}
        		if (args.containsKey("autoFaveOnLoad"))
            	{
            		_autoFaveOnLoad  = args.getBoolean("autoFaveOnLoad");
            	}
        		if (args.containsKey("selectPostIdAfterLoading"))
        		{
    	    		_selectPostIdAfterLoading = args.getInt("selectPostIdAfterLoading");
        		}
            	if (args.containsKey("content"))
            	{
            		String userName = args.getString("userName");
            		String postContent = args.getString("content");
            		Long posted = args.getLong("posted");
            		String moderation = args.containsKey("moderation") ? args.getString("moderation") : "";
            		String lastThreadJson = args.containsKey("json") ? args.getString("json") : "";
            		try {
						_lastThreadJson = new JSONObject(lastThreadJson);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            		
            		// create root post fast with no load delay
            		Post post = null;
            		if (_messageId > 0)
            			post = new Post(_messageId, userName, postContent, posted, 0, moderation, true);
            		else 
            			post = new Post(_rootPostId, userName, postContent, posted, 0, moderation, true);
            		
            		loadPost(post);
            	}
            	else if (action != null && action.equals(Intent.ACTION_VIEW) && uri != null)
            	{
            		String id = uri.getQueryParameter("id");
            		if (id == null)
            		{
            			ErrorDialog.display(getActivity(), "Error", "Invalid URL Found");
            			return;
            		}
                
            		_rootPostId = Integer.parseInt(id);
            		_itemPosition = 0;
            	}
        	}
        	_adapter.triggerLoadMore();
        }
        else    
        {
       		// user rotated the screen, try to go back to where they where
       		if (_listState != null)
       			getListView().onRestoreInstanceState(_listState);
       		
       		getListView().setSelectionFromTop(_listPosition,  _itemPosition);
       		if (_itemChecked != ListView.INVALID_POSITION)
       			expandAndCheckPostWithoutAnimation(_itemChecked);
        }
       	
       	// pull to fresh integration
        // Retrieve the PullToRefreshLayout from the content view
        ptrLayout = (PullToRefreshLayout)getView().findViewById(R.id.ptr_layout);

        // Give the PullToRefreshAttacher to the PullToRefreshLayout, along with a refresh listener.
        ptrLayout.setPullToRefreshAttacher(((MainActivity)getActivity()).getRefresher(), new PullToRefreshAttacher.OnRefreshListener(){

			@Override
			public void onRefreshStarted(View view) {
				refreshThreadReplies();
			}});
       	
        
       	updateThreadViewUi();
    }

    public void updateThreadViewUi()
    {
    	if (_viewAvailable)
    	{
    		if (getListView() != null)
    		{
    			// handle the throbbers
    			if (_adapter != null)
	    		{
		    		if (_adapter.getCount() == 1)
		    		{
		    			// single post already loaded throbber
		    			_adapter.notifyDataSetChanged();
		    		}
	    		}
	    		if (_messageId != 0)
	    		{
	    			// messages mode
	    			getView().findViewById(R.id.tview_FSIcon).setVisibility(View.GONE);
	    			_showFavSaved = false;
	    			_showUnFavSaved = false;
	    			// disable PTR for message mode
	    			((PullToRefreshLayout)getView().findViewById(R.id.ptr_layout)).setPullToRefreshAttacher(null, null);
	    		}
	    		else if (_messageId == 0)
		       	{
		       		// show the icon and start message if no threads or messages have been loaded
	    			getView().findViewById(R.id.tview_FSIcon).setVisibility((_rootPostId > 0) ? View.GONE : View.VISIBLE);
		        	
		            // and provided a way to save thread
		            if ((_lastThreadJson != null) && (_adapter != null) && (_adapter.getCount() > 0) && (_adapter.getItem(0) != null))
		    		{
		    			// determine if checked
		        		boolean set2 = ((MainActivity)getActivity()).mOffline.containsThreadId(ThreadViewFragment.this._rootPostId);
		        		
		        		_showFavSaved = set2;
		        		_showUnFavSaved = !set2;
		    		}
		            else
		            {
		            	_showFavSaved = false;
		            	_showUnFavSaved = false;
		            }
		            
		            // enable PTR because we are not in message mode
		            ((PullToRefreshLayout)getView().findViewById(R.id.ptr_layout)).setPullToRefreshAttacher(((MainActivity)getActivity()).getRefresher(), new PullToRefreshAttacher.OnRefreshListener(){

		    			@Override
		    			public void onRefreshStarted(View view) {
		    				refreshThreadReplies();
		    			}});
		       	}
	    		
	    		// handle the fullscreen throbber
	    		RelativeLayout FSLoad = (RelativeLayout)getView().findViewById(R.id.tview_FSLoad);
	    		if ((_adapter != null) && (_adapter.isCurrentlyLoading()) && (_adapter.getCount() == 0))
	    		{
	    			((PullToRefreshLayout)getView().findViewById(R.id.ptr_layout)).setVisibility(View.GONE);
	    			FSLoad.setVisibility(View.VISIBLE);
	    		}
	    		else
	    		{
	    			if ((_rootPostId > 0) || (_messageId > 0))
	    			{
	    				((PullToRefreshLayout)getView().findViewById(R.id.ptr_layout)).setVisibility(View.VISIBLE);
	    			}
	    			FSLoad.setVisibility(View.GONE);
	    		}
	    		
	    		// update options menu
	    		((MainActivity)getActivity()).invalidateOptionsMenu();
    		}
    	}
    }
    
    private void searchForPosts(String term)
    {
    	Bundle args = new Bundle();
    	args.putString("author", term);
    	((MainActivity)getActivity()).openSearch(args);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
    	super.onSaveInstanceState(outState);

    	// we should put this info into the outState, but the compatibility framework
    	// seems to swallow it somewhere   	
    	saveListState();    			
    }
    
    public void saveListState()
    {
    	if (_viewAvailable)
    	{
	    	ListView listView = getListView();
	    	_listState = listView.onSaveInstanceState();
	    	_listPosition = listView.getFirstVisiblePosition();
	    	_itemChecked = _lastExpanded;
	    	View itemView = listView.getChildAt(0);
	    	_itemPosition = itemView == null ? 0 : itemView.getTop(); 
    	}
    }
    public void ensurePostSelectedAndDisplayed ()
    {
    	ensurePostSelectedAndDisplayed(_selectPostIdAfterLoading, false);
    }
    public void ensurePostSelectedAndDisplayed(int postId, final boolean withAnimation)
    {
    	System.out.println("ENSURESELECTED " + postId);
    	
        int length = _adapter.getCount();
        for (int i = 0; i < length; i++)
        {
            Post post = _adapter.getItem(i);
            if (post != null && post.getPostId() == postId)
            {
                getListView().setSelectionFromTop(i, 0);
                //ensureVisible(i, true, false, true);
                final int pos = i;
                if (withAnimation) {
                    getListView().post(new Runnable() {
                        @Override
                        public void run() {
                            expandAndCheckPost(pos);
                        }});
                }
                else {
                    expandAndCheckPostWithoutAnimation(pos);
                }
                getListView().post(new Runnable() {
                    @Override
                    public void run() {
                        getListView().setSelectionFromTop(pos, 0);
                    }});
                System.out.println("ENSURESELECTED ECP " + postId + " " + i);
            	// dont select root posts. unnecessary
            	// i is position
        	

                
                if (_refreshRestore)
                {
                    getListView().setSelectionFromTop(_listPosition,_itemPosition);
                    getListView().post(new Runnable() {
                        @Override
                        public void run() {
                            getListView().setSelectionFromTop(_listPosition,_itemPosition);
                           // getListView().smoothScrollToPositionFromTop(_listPosition,_itemPosition);
                        }
                    });

                	_refreshRestore = false;
                }
        	
                break;
            }
        }
    }

    public boolean isPostIdInAdapter(int postId)
    {
    	if (_adapter != null)
    	{
	        int length = _adapter.getCount();
	        for (int i = 0; i < length; i++)
	        {
	            Post post = _adapter.getItem(i);
	            if (post != null && post.getPostId() == postId)
	            {
	                return true;
	            }
	        }
    	}
		return false;
    }
    
    public void showTaggers(int pos)
    {
        if (_adapter.getItem(pos) != null && _adapter.getItem(pos).getPostId() > 0)
    	{
        	final GetTaggersTask gtt = new GetTaggersTask();
        	
        	gtt.execute(_adapter.getItem(pos).getPostId());
        }
    }
    class GetTaggersTask extends AsyncTask<Integer, Void, CharSequence> {
    	protected MaterialDialog mProgressDialog;
		private CharSequence arraylistFormatter (String type, ArrayList<String>  arr)
    	{
    		if (arr.size() > 0)
    		{
    			int color = 0;
    			if (type.equals("lol")) color = getResources().getColor(R.color.shacktag_lol);
    			if (type.equals("wtf")) color = getResources().getColor(R.color.shacktag_wtf);
    			if (type.equals("inf")) color = getResources().getColor(R.color.shacktag_inf);
    			if (type.equals("tag")) color = getResources().getColor(R.color.shacktag_tag);
    			if (type.equals("ugh")) color = getResources().getColor(R.color.shacktag_ugh);
    			if (type.equals("unf")) color = getResources().getColor(R.color.shacktag_unf);
    			SpannableString header = new SpannableString(type + "\'d" + "\n");
    			header.setSpan(new ForegroundColorSpan(color), 0, header.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
    			header.setSpan(new RelativeSizeSpan(1.6f), 0, header.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
    			
    			java.util.Collections.sort(arr, Collator.getInstance());
	    		ListIterator<String> iter = arr.listIterator();
	    		String txt = "";
	    		while (iter.hasNext())
	    			txt = txt + iter.next() + "\n";
	    		
	    		SpannableString list = new SpannableString(txt);
    			list.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.userName)), 0, txt.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
    			
				return TextUtils.concat(header, list, "\n");
    		}
    		else
    			return "";
    	}
        @Override
        protected CharSequence doInBackground(Integer... params) {
        	
        	final Integer parm = params[0];
        	getActivity().runOnUiThread(new Runnable(){
        		@Override public void run()
        		{
        			mProgressDialog = MaterialProgressDialog.show(getActivity(), "Loading Taggers", "ThomW's server is a loose cannon", true, true, new OnCancelListener(){
    				@Override
    				public void onCancel(DialogInterface arg0) {
    					cancel(true);
    					System.out.println("CANCELED");
    				}});
        		}
			});
        	
        	ArrayList<String> resultslol = new ArrayList<String>();
        	ArrayList<String> resultsinf = new ArrayList<String>();
        	ArrayList<String> resultstag = new ArrayList<String>();
        	ArrayList<String> resultsunf = new ArrayList<String>();
        	ArrayList<String> resultswtf = new ArrayList<String>();
        	ArrayList<String> resultsugh = new ArrayList<String>();
        	
        	try {
				resultslol = ShackApi.getLOLTaggers(parm, "lol");
				resultsinf = ShackApi.getLOLTaggers(parm, "inf");
				resultsunf = ShackApi.getLOLTaggers(parm, "unf");
				resultstag = ShackApi.getLOLTaggers(parm, "tag");
				resultswtf = ShackApi.getLOLTaggers(parm, "wtf");
				resultsugh = ShackApi.getLOLTaggers(parm, "ugh");
			} catch (Exception e) {
				e.printStackTrace();
			}
        	
        	CharSequence txt = TextUtils.concat(
        			arraylistFormatter("lol", resultslol),
        			arraylistFormatter("inf", resultsinf),
        			arraylistFormatter("unf", resultsunf),
        			arraylistFormatter("tag", resultstag),
        			arraylistFormatter("wtf", resultswtf),
        			arraylistFormatter("ugh", resultsugh)
        			);
            return txt;
        }
        @Override
        public void onPostExecute(final CharSequence txt)
        {
        	if (getActivity () != null)
    		{
    			getActivity().runOnUiThread(new Runnable(){
	        		@Override public void run()
	        		{
                        MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(getActivity());
	                    builder.setTitle("Taggers for post");
	                    builder.setNegativeButton("OK", null);
	                    
				        ScrollView scrolly = new ScrollView(getActivity());
				        TextView content = new TextView(getActivity());
				        content.setPadding(10, 10, 10, 10);
				        content.setTextSize(TypedValue.COMPLEX_UNIT_SP, _adapter._zoom * getResources().getDimension(R.dimen.viewPostTextSize));
				        scrolly.addView(content);
				        content.setText(txt);
				        if (Build.VERSION.SDK_INT >= 11)
				        {
				        	content.setTextIsSelectable(true);
				        }
				        System.out.println("SETTING LOLLERS: " + txt);
				        builder.setView(scrolly);
				        AlertDialog alert = builder.create();
				        alert.show();
				        
				        mProgressDialog.dismiss();
	        		}
    			});
    		}
        }
    }
    
    public void shareURL(int pos)
    {
	    Intent sendIntent = new Intent();
	    sendIntent.setAction(Intent.ACTION_SEND);
	    sendIntent.putExtra(Intent.EXTRA_TEXT, createPostURL(pos));
	    sendIntent.setType("text/plain");
	    startActivity(Intent.createChooser(sendIntent, "Share Post Link"));
    }
    private String createPostURL(int pos)
    {
    	if (_adapter.getItem(pos) != null && _adapter.getItem(pos).getPostId() > 0)
    	{
    		String str = "http://www.shacknews.com/chatty?id=" + _adapter.getItem(pos).getPostId();
    		if ((_lastExpanded > 0) && (pos > 0))
    		{
    			str = "http://www.shacknews.com/chatty?id=" + _adapter.getItem(pos).getPostId();
    			str = str + "#item_" + _adapter.getItem(pos).getPostId();
    		}
    		return str;
    	}
    	return null;
    }
    public void copyURL(int pos)
    {
    	if (createPostURL(pos) != null)
    	{
    		copyString(createPostURL(pos));
    	}
    }
    public void copyPostText(int pos)
    {
    	 copyString(_adapter.getItem(pos).getCopyText());
    }
    
    public void copyString(String string)
    {
    	ClipboardManager clipboard = (ClipboardManager)getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
    	clipboard.setText(string);
    	Toast.makeText(getActivity(), string, Toast.LENGTH_SHORT).show();
    }
    
    public void refreshThreadReplies()
    {
    	 saveListState();

    	 if (_adapter != null && _adapter.getCount() > _lastExpanded  && _adapter.getItem(_lastExpanded) != null)
    		 _selectPostIdAfterLoading = _adapter.getItem(_lastExpanded).getPostId();
    	 _refreshRestore = true;

        System.out.println("LASTEXP REFR" + _lastExpanded + " " + _selectPostIdAfterLoading);

         _adapter.clear();
         _adapter.triggerLoadMore();
    }
    
    public void saveThread()
    {
    	if ((_lastThreadJson != null) && (_adapter != null) && (_adapter.getCount() > 0) && (_adapter.getItem(0) != null))
    	{
    		// false if UNsaved
    		if(((MainActivity)getActivity()).mOffline.toggleThread(_adapter.getItem(0).getPostId(), _adapter.getItem(0).getPosted(), _lastThreadJson))
    		{
    			Toast.makeText(getActivity(), "Thread added to favorites", Toast.LENGTH_SHORT).show();
    			_showUnFavSaved = false;
    			_showFavSaved = true;
    		}
    		else
    		{
    			Toast.makeText(getActivity(), "Thread removed from favorites", Toast.LENGTH_SHORT).show();
    			_showUnFavSaved = true;
    			_showFavSaved = false;
    		}
    		((MainActivity)getActivity()).invalidateOptionsMenu();
    		((MainActivity)getActivity()).mRefreshOfflineThreadsWoReplies();
    	}
    	else
    	{
    		Toast.makeText(getActivity(), "Error: could not save thread", Toast.LENGTH_SHORT).show();
    		System.out.println("TVIEW: no json to save thread with");
    	}
    }
    
    public static final int POST_REPLY = 937;
    public static final int POST_MESSAGE = 947;
    void postReply(final Post parentPost)
    {
        boolean verified = _prefs.getBoolean("usernameVerified", false);
        if (!verified)
        {
        	LoginForm login = new LoginForm(getActivity());
        	login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {
				@Override
				public void onSuccess() {
					postReply(parentPost);
				}

				@Override
				public void onFailure() {
				}
			});
        	return;
        }
        else if (_messageId == 0)
        {
	    	boolean isNewsItem = _adapter.getItem(0).getUserName().equalsIgnoreCase("shacknews");
		    ((MainActivity)getActivity()).openComposerForReply(POST_REPLY, parentPost, isNewsItem);
        }
        else if (_rootPostId == 0)
        {
        	((MainActivity)getActivity()).openComposerForMessageReply(POST_MESSAGE, parentPost, "Re: " + _messageSubject);
        }
        else
        {
        	 ErrorDialog.display(getActivity(), "Error", "Error determining message TYPE for reply.");
        }
    }
    
    public void shackmessageTo (String username)
    {
    	((MainActivity)getActivity()).openNewMessagePromptForSubject(username);
    }
    
    public void lolChoose(int pos, final boolean isFromQuickLOL)
    {
        final int finpos = pos;
        
        boolean verified = _prefs.getBoolean("usernameVerified", false);
        if (!verified)
        {
        	LoginForm login = new LoginForm(getActivity());
        	login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {
				@Override
				public void onSuccess() {
					lolChoose(finpos, isFromQuickLOL);
				}

				@Override
				public void onFailure() {
				}
			});
        	return;
        }

        MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(getActivity());
        builder.setTitle("Choose LOL tag");
        final CharSequence[] items = { "lol","inf","unf","tag","wtf","ugh"};
        final CharSequence[] itemsQuick = { "See who tagged", "lol","inf","unf","tag","wtf","ugh"};
        if (isFromQuickLOL)
        {
        builder.setItems(itemsQuick, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
            	if (item == 0)
            	{
            		new GetTaggersTask().execute(_adapter.getItem(finpos).getPostId());
            	}
            	else
            		lolPost((String)itemsQuick[item], finpos);
                }});
        }
        else
        {
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                lolPost((String)items[item], finpos);
                }});
        }
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(true);
        alert.show();
    }
    
    private void lolPost(final String tag, final int pos)
    {
        String userName = _prefs.getString("userName", "");
        
        boolean verified = _prefs.getBoolean("usernameVerified", false);
        if (!verified)
        {
        	LoginForm login = new LoginForm(getActivity());
        	login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {
				@Override
				public void onSuccess() {
					lolPost(tag, pos);
				}

				@Override
				public void onFailure() {
				}
			});
        	return;
        }
        
        _curTask = new LolTask().execute(userName, tag, Integer.toString(pos));
        if (_adapter != null) {
            _adapter.getItem(pos).setIsWorking(true);
            _adapter.notifyDataSetChanged();
        }
    }
    
    public void modChoose(final int pos)
    {
        MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(getActivity());
        builder.setTitle("Choose mod tag");
        final CharSequence[] items = { "interesting","nws","stupid","tangent","ontopic","political" };
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                modPost((String)items[item], pos);
                }});
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(true);
        alert.show();
    }
    
    private void modPost(final String moderation, final int pos)
    {
        String userName = _prefs.getString("userName", "");
        String password = _prefs.getString("password", "");
        
        boolean verified = _prefs.getBoolean("usernameVerified", false);
        if (!verified)
        {
        	LoginForm login = new LoginForm(getActivity());
        	login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {
				@Override
				public void onSuccess() {
					modPost(moderation, pos);
				}

				@Override
				public void onFailure() {
				}
			});
        	return;
        }
        
        new ModTask().execute(userName, password, moderation, Integer.toString(pos));
        _progressDialog = MaterialProgressDialog.show(getActivity(), "Please wait", "Laying down the ban hammer...");
    }
    
    class LolTask extends AsyncTask<String, Void, Void>
    {
        Exception _exception;
        private int pos;
        private int postId;
        private String tag;

        @Override
        protected Void doInBackground(String... params)
        {
            String userName = params[0];
            tag = params[1];
            pos = Integer.parseInt(params[2]);
            postId = _adapter.getItem(pos).getPostId();

            try
            {
                ShackApi.tagPost(postId, tag, userName);
            }
            catch (Exception ex)
            {
                Log.e("shackbrowse", "Error tagging post", ex);
                _exception = ex;
            }
            
            return null;
        }
        
        @Override
        protected void onPostExecute(Void result)
        {
            if (_adapter != null && _adapter.getItem(pos) != null && _adapter.getItem(pos).getPostId() == postId)
            {
                LolObj updLol = _adapter.getItem(pos).getLolObj();
                if (updLol == null)
                    updLol = new LolObj();

                if (tag.equalsIgnoreCase("lol")) updLol.incLol();
                if (tag.equalsIgnoreCase("tag")) updLol.incTag();
                if (tag.equalsIgnoreCase("ugh")) updLol.incUgh();
                if (tag.equalsIgnoreCase("wtf")) updLol.incWtf();
                if (tag.equalsIgnoreCase("inf")) updLol.incInf();
                if (tag.equalsIgnoreCase("unf")) updLol.incUnf();

                updLol.genTagSpan(getActivity());
                _adapter.getItem(pos).setLolObj(updLol);
                _adapter.getItem(pos).setIsWorking(false);
                _adapter.notifyDataSetChanged();
            }
            if (_exception != null)
               ErrorDialog.display(getActivity(), "Error", "Error tagging post:\n" + _exception.getMessage()); 
        }
    }
    
    class ModTask extends AsyncTask<String, Void, String>
    {
        Exception _exception;
        
        @Override
        protected String doInBackground(String... params)
        {
            String userName = params[0];
            String password = params[1];
            String moderation = params[2];
            int pos = Integer.parseInt(params[3]);
            
            try
            {
                int rootPost = _adapter.getItem(0).getPostId();
                String result = ShackApi.modPost(userName, password, rootPost, _adapter.getItem(pos).getPostId(), moderation);
                return result;
            }
            catch (Exception e)
            {
                _exception = e;
                Log.e("shackbrowse", "Error modding post", e);
            }
            
            return null;
        }
        
        @Override
        protected void onPostExecute(String result)
        {
            _progressDialog.dismiss();
            if (_exception != null)
                ErrorDialog.display(getActivity(), "Error", "Error occured modding post."); 
            else if (result != null)
                ErrorDialog.display(getActivity(), "Moderation", result);
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
        	// returning from compose post view
            case POST_REPLY:
                if (resultCode == Activity.RESULT_OK)
                {
                    // read the resulting thread id from the post
                	// this is either the id of your new post or the id of the post your replied to
                    int PQPId = (int) data.getExtras().getLong("PQPId");
                    int parentPostId = data.getExtras().getInt("parentPostId");
                    _selectPostIdAfterLoading = PQPId;
                    _isSelectPostIdAfterLoadingIdaPQPId  = true;
                    
                    if (_adapter != null)
                    {
                    	_adapter.fakePostRemoveinator();
                    	_adapter.fakePostAddinator(_adapter.getAll());
                    	_adapter.createThreadTree(_adapter.getAll());
                    	_adapter.notifyDataSetChanged();
                    	ensurePostSelectedAndDisplayed();
                    }
                    //System.out.println("RECV PR: " + _selectPostIdAfterLoading);
                   // _rootPostId = parentPostId; // should cause the thread to load, then _selectpqpid will select the fakepost
                    //_adapter.clear();
                    //_adapter.triggerLoadMore();
                }
                break;
            default:
                break;
        }
    } 

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        // displayPost(v);
    }

    private ExpandableListItemAdapter.titleViewOnClick getTitleViewOnClickListener() {
        return new ExpandableListItemAdapter.titleViewOnClick() {
            @Override
            public void OnClick(View contentParent) {
                // do not collapse root posts
                if (_adapter.findPositionForId((Long)contentParent.getTag()) == 0)
                    return;
                if (_adapter.mAnimSpeed == 0f)
                {
                    // no animation
                    displayPost((View)contentParent.getParent());
                }
                else
                {
                    // animations
                    _lastExpanded = _adapter.findPositionForId((Long)contentParent.getTag());
                    _adapter.toggle(contentParent);
                }
            }
        };
    }


    // the following function is a remnant from olden times. it is only used when animations are turned off.
    private void displayPost(View v)
    {
    	if (getListView().getPositionForView(v) != ListView.INVALID_POSITION)
    	{
    		this._postYLoc = v.getTop();
    		
    		expandAndCheckPostWithoutAnimation(v);
    		
    		// calculate sizes
            Display display = ((WindowManager) v.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            DisplayMetrics displaymetrics = new DisplayMetrics();
            display.getMetrics(displaymetrics);
            
            // move things around to prevent weird scrolls
    		getListView().setSelectionFromTop(getListView().getPositionForView(v), _postYLoc - (_userNameHeight + (int)(30*(displaymetrics.ydpi / 160))));
    		
    		// keep the child view on screen
    		final View view = v;
    		final int pos = getListView().getPositionForView(view);
            final ListView listView = getListView();
            listView.post(new Runnable() {

                @Override
                public void run() {
                	View betterView = getListViewChildAtPosition(pos, listView);
                	if (betterView != null)
                	{
                		listView.requestChildRectangleOnScreen(betterView, new Rect(0, 0, betterView.getRight(), betterView.getHeight()), false);
                	}
                	else
                		listView.requestChildRectangleOnScreen(view,
                            new Rect(0, 0, view.getRight(), view.getHeight()), false);
                }
            });
    	}
    }
    private View getListViewChildAtPosition(int position, ListView listView)
    {
    	int wantedPosition = position; // Whatever position you're looking for
    	int firstPosition = listView.getFirstVisiblePosition() - listView.getHeaderViewsCount(); // This is the same as child #0
    	int wantedChild = wantedPosition - firstPosition;
    	// Say, first visible position is 8, you want position 10, wantedChild will now be 2
    	// So that means your view is child #2 in the ViewGroup:
    	if (wantedChild < 0 || wantedChild >= listView.getChildCount()) {
    	  Log.w("SB3", "Unable to get view for desired position, because it's not being displayed on screen.");
    	  return null;
    	}
    	// Could also check if wantedPosition is between listView.getFirstVisiblePosition() and listView.getLastVisiblePosition() instead.
    	View wantedView = listView.getChildAt(wantedChild);
    	return wantedView;
    }
    
    private void expandAndCheckPostWithoutAnimation(View v)
    {
    	expandAndCheckPostWithoutAnimation(getListView().getPositionForView(v));
    }
    private void expandAndCheckPostWithoutAnimation(int listviewposition)
    {
		_adapter.expandWithoutAnimation(listviewposition);
        System.out.println("EXPANDING " + listviewposition);
    	/*
    	Post post = null;
    	
    	int adapterposition = listviewposition;
    
    	// sanity check
    	if (adapterposition >= 0 && adapterposition < _adapter.getCount())
        {
    		post = _adapter.getItem(adapterposition);
        }
        
        // user clicked "Loading..."
        if (post == null)
            return;
        
        getListView().setItemChecked(listviewposition, true);
        
        // never unexpand the root post
        if ((_lastExpanded > 0) && (_lastExpanded <= (_adapter.getCount())) && (_lastExpanded != listviewposition))
        {
        	Post oldpost = _adapter.getItem(this._lastExpanded);
        	oldpost.setExpanded(false);
        	getListView().setItemChecked(_lastExpanded, false);
        	
        	// scroll helper
        }
        */
        _lastExpanded = listviewposition;
        /*
        post.setExpanded(true);
       	
       	_adapter.notifyDataSetChanged();
       	*/
    }
    private void expandAndCheckPost(int listviewposition)
    {
		_adapter.expand(listviewposition);
        _lastExpanded = listviewposition;
    }
    
    class PostLoadingAdapter extends ExpandableLoadingAdapter<Post>
    {
        boolean _lolsInPost = true;
        boolean _getLols = true;
        float _zoom = 1.0f;
        int _maxWidth = 400;
        int _bulletWidth = 20;
        int _maxBullets = 8;
        String _donatorList = "";
        private String _donatorGoldList = "";
        private String _donatorQuadList = "";
        boolean _showModTools = false;
		private boolean _showHoursSince = true;
        private String _userName = "";
		private String _OPuserName = "";
        private HashMap<String, HashMap<String, LolObj>> _shackloldata = new HashMap<String, HashMap<String, LolObj>>();
        
        private Bitmap _bulletBlank;
        private Bitmap _bulletEnd;
        private Bitmap _bulletExtendPast;
        private Bitmap _bulletBranch;
        private Bitmap _bulletCollapse;
		private BitmapDrawable _donatorIcon;
        private BitmapDrawable _donatorGoldIcon;
        private BitmapDrawable _donatorQuadIcon;
        private BitmapDrawable _briefcaseIcon;
		private boolean _displayLimes = true;
		private boolean _displayLolButton = false;
		private HashMap<String, LolObj> _threadloldata;
		private Bitmap _bulletEndNew;
		private Bitmap _bulletExtendPastNew;
		private Bitmap _bulletBranchNew;
		private LruCache<String, Bitmap> mMemoryCache;
		private boolean _fastScroll = true;

        public ExpandCollapseListener mExpandCollapseListener = new ExpandCollapseListener() {
            @Override
            public void onItemExpanded(int i) {
                View v = _adapter.getTitleView(i);
                if (v != null)
                {
                    TextView userName = (TextView) v.findViewById(R.id.textPreviewUserName);
                    userName.setOnClickListener(_adapter.getUserNameClickListenerForPosition(i,v));
                    userName.setClickable(true);
                }
            }

            @Override
            public void onItemCollapsed(int i) {
                View v = _adapter.getTitleParent(i);
                if (v != null)
                {
                    TextView userName = (TextView) v.findViewById(R.id.textPreviewUserName);
                    userName.setOnClickListener(null);
                    userName.setClickable(false);
                }
            }
        };

        public View.OnClickListener getUserNameClickListenerForPosition(int pos, View v)
        {
            final String unamefinal = getItem(pos).getUserName();
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu usrpop = new PopupMenu(getContext(), v);
                    usrpop.getMenu().add(Menu.NONE, 0, Menu.NONE, "Shack Message " + unamefinal);
                    usrpop.getMenu().add(Menu.NONE, 1, Menu.NONE, "Search for posts by " + unamefinal);
                    usrpop.getMenu().add(Menu.NONE, 2, Menu.NONE, "Highlight " + unamefinal + " in thread");

                    usrpop.setOnMenuItemClickListener(new OnMenuItemClickListener(){
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (item.getItemId() == 0)
                                shackmessageTo(unamefinal);
                            if (item.getItemId() == 1)
                                searchForPosts(unamefinal);
                            if (item.getItemId() == 2)
                                ((MainActivity)getActivity()).openHighlighter(unamefinal);
                            return true;
                        }});
                    usrpop.show();
                }
            };
        }

        public PostLoadingAdapter(Context context, ArrayList<Post> items)
        {
            super(context, items);
            loadPrefs();
            
            // Get max available VM memory, exceeding this amount will throw an
            // OutOfMemory exception. Stored in kilobytes as LruCache takes an
            // int in its constructor.
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

            // Use 1/3th of the available memory for this memory cache.
            final int cacheSize = maxMemory / 3;

            mMemoryCache = new LruCache<String, Bitmap>(cacheSize)
            {
                @Override
                protected int sizeOf(String key, Bitmap bitmap)
                {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.

                	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
                        return ((bitmap.getRowBytes() * bitmap.getHeight()) / 1024) / 1024;
                    } else {
                    	return bitmap.getByteCount() / 1024;
                    }
                }
            };

        }
        
        public void addBitmapToMemoryCache(String key, Bitmap bitmap)
        {
            if ((bitmap != null) && (getBitmapFromMemCache(key) == null))
            {
                mMemoryCache.put(key, bitmap);
            }
        }

        public Bitmap getBitmapFromMemCache(String key) {
            return mMemoryCache.get(key);
        }
        public void clearBitmapMemCache()
        {
        	mMemoryCache.evictAll();
        }
        
        @Override
        public void clear()
        {
        	// reload preferences
            loadPrefs();
            
            // not used now that memcache is based on depthstring keys
            // clearBitmapMemCache();
            
            
            _lastExpanded = 0;
            
            // clear any errant checkeds
            for (int i=0; i < this.getCount(); i++) {
                getListView().setItemChecked(i, false);
            }
            
            super.clear();
        }
        
        void loadPrefs()
        {
            _userName = _prefs.getString("userName", "").trim();
            _lolsInPost = _prefs.getBoolean("showPostLolsThreadView", true);
            _getLols = _prefs.getBoolean("getLols", true);
            _zoom = Float.parseFloat(_prefs.getString("fontZoom", "1.0"));
            _showModTools = _prefs.getBoolean("showModTools", false);
            _showHoursSince  = _prefs.getBoolean("showHoursSince", true);
            _fastScroll   = _prefs.getBoolean("fastScroll", true);
            _donatorList = _prefs.getString("limeUsers", "");
            _donatorGoldList = _prefs.getString("goldLimeUsers", "");
            _donatorQuadList = _prefs.getString("quadLimeUsers", "");
            _displayLimes  = _prefs.getBoolean("displayLimes", true);
            // "enableDonatorFeatures"
            _displayLolButton  = true;
            setDurationPref();
            
            // fast scroll on mega threads
        	if (getActivity () != null)
    		{
        		boolean set = false;
        		if ((getCount() > 300))
        			set = true;
        		final boolean set2 = set && _fastScroll;
        		
    			getActivity().runOnUiThread(new Runnable(){
	        		@Override public void run()
	        		{
	        			if (_viewAvailable)
	                	{
	        				if (getListView() != null)
	                    	{
	        					getListView().setFastScrollEnabled(set2);
	                    	}
	                	}
	        		}
    			});
    		}
            
            // calculate sizes for deep threads
            Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            DisplayMetrics displaymetrics = new DisplayMetrics();
            display.getMetrics(displaymetrics);
            _maxWidth = (int) (displaymetrics.widthPixels * (.7));
            createAllBullets();
        	_maxBullets = (int) Math.floor(_maxWidth / _bulletWidth);
        	// failsafe
        	if (_maxBullets < 8) _maxBullets = 8;
        }
        
        @Override
		protected void setCurrentlyLoading(final boolean set)
        {
        	super.setCurrentlyLoading(set);
        	
    		if (getActivity () != null)
    		{
    			getActivity().runOnUiThread(new Runnable(){
	        		@Override public void run()
	        		{
	        			if (_viewAvailable)
	                	{
	        	        	updateThreadViewUi();
	        	        	
	        	        	((MainActivity)getActivity()).showOnlyProgressBarFromPTRLibrary(set);
	                	}
	        		}
    			});
    		}
        }
        
        

		@Override
		public View getContentView(int position, View convertView, ViewGroup parent) {
			if (convertView == null)
        	{
	        		convertView = LayoutInflater.from(getActivity()).inflate(R.layout.thread_row_expanded, parent, false);
        	}
            
            return createView(position, convertView, parent, true);
		}

		@Override
		public View getTitleView(int position, View convertView, ViewGroup parent) {
			if (convertView == null)
        	{
	        		convertView = LayoutInflater.from(getActivity()).inflate(R.layout.thread_row_preview, parent, false);
        	}
            
            return createView(position, convertView, parent, false);
		}

        @Override
        public void loadExpandedViewDataIntoView(int position, View convertView)
        {
            if (convertView != null && convertView.getTag() != null) {
                ViewHolder holder = (ViewHolder) convertView.getTag();
                Post p = getItem(position);

                // load expanded data

                holder.expandedView.setBackgroundColor(getResources().getColor(R.color.selected_highlight_postbg));

                // set content text color
                holder.content.setTextColor(getResources().getColor(R.color.nonpreview_post_text_color));
                holder.content.setText(applyExtLink((Spannable) applyHighlight(p.getFormattedContent()), holder.content), BufferType.SPANNABLE);

                // set lol tags
                if (p.getLolObj() != null) {
                    holder.expLolCounts.setText(p.getLolObj().getTagSpan());
                } else {
                    holder.expLolCounts.setText("");
                }


                // links stuff
                holder.content.setLinkTextColor(getResources().getColor(R.color.linkColor));
                holder.content.setTextIsSelectable(true);
                holder.content.setMovementMethod(new CustomLinkMovementMethod());
                StyleCallback cb = new StyleCallback();
                cb.setTextView(holder.content);
                holder.content.setCustomSelectionActionModeCallback(cb);
                // now we cant click the list item so fix that
            	 /* holder.content.setOnClickListener(new View.OnClickListener() {
            	    @Override
            	    public void onClick(View v) {
            	        // displayPost(v,false);
            	    }
            	}); */

                final int pos = position;
                final String unamefinal = p.getUserName();

                holder.buttonReply.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        postReply(_adapter.getItem(pos));
                    }
                });

                final ImageButton btnlol = holder.buttonLol;
                holder.buttonLol.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu lolpop = new PopupMenu(getContext(), btnlol);
                        lolpop.getMenu().add(Menu.NONE, 6, Menu.NONE, "Who Tagged?");
                        lolpop.getMenu().add(Menu.NONE, 0, Menu.NONE, "lol");
                        lolpop.getMenu().add(Menu.NONE, 1, Menu.NONE, "inf");
                        lolpop.getMenu().add(Menu.NONE, 2, Menu.NONE, "unf");
                        SubMenu sub = lolpop.getMenu().addSubMenu(Menu.NONE, 3, Menu.NONE, "More...");
                        sub.add(Menu.NONE, 4, Menu.NONE, "ugh");
                        sub.add(Menu.NONE, 5, Menu.NONE, "wtf");
                        sub.add(Menu.NONE, 7, Menu.NONE, "tag");
                        lolpop.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                if (item.getItemId() == 3)
                                    return false;
                                if (item.getItemId() == 6)
                                    new GetTaggersTask().execute(_adapter.getItem(pos).getPostId());
                                else
                                    lolPost((String) item.getTitle(), pos);
                                return true;
                            }
                        });
                        lolpop.show();
                    }
                });

                // open all images button
                final CustomURLSpan[] urlSpans = ((SpannableString) holder.content.getText()).getSpans(0, holder.content.getText().length(), CustomURLSpan.class);
                if (urlSpans.length > 1) {
                    String _href;
                    ArrayList<String> hrefs = new ArrayList<String>();
                    for (int i = 0; i < urlSpans.length; i++) {
                        _href = urlSpans[i].getURL().trim();
                        if (PopupBrowserFragment.isImage(_href)) {
                            hrefs.add(_href);
                        }
                    }
                    String[] hrefs2 = new String[hrefs.size()];
                    hrefs.toArray(hrefs2);
                    final String[] hrefs3 = hrefs2;
                    holder.buttonAllImages.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((MainActivity) getActivity()).openBrowser(hrefs3);
                        }
                    });
                    if (hrefs2.length > 1)
                        holder.buttonAllImages.setVisibility(View.VISIBLE);
                    else
                        holder.buttonAllImages.setVisibility(View.GONE);

                } else {
                    holder.buttonAllImages.setVisibility(View.GONE);
                }


                final ImageButton btnothr = holder.buttonOther;
                holder.buttonOther.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu extpop = new PopupMenu(getContext(), btnothr);
                        SubMenu sub = extpop.getMenu().addSubMenu(Menu.NONE, 0, Menu.NONE, unamefinal + " Actions");
                        sub.add(Menu.NONE, 3, Menu.NONE, "Shack Message " + unamefinal);
                        sub.add(Menu.NONE, 4, Menu.NONE, "Search for posts by " + unamefinal);
                        sub.add(Menu.NONE, 16, Menu.NONE, "Highlight " + unamefinal + " in thread");
                        SubMenu sub2 = extpop.getMenu().addSubMenu(Menu.NONE, 1, Menu.NONE, "Share/Copy Post");
                        sub2.add(Menu.NONE, 5, Menu.NONE, "Copy Post Text");
                        if (_messageId == 0) {
                            // not a message
                            sub2.add(Menu.NONE, 6, Menu.NONE, "Copy URL of Post");
                            sub2.add(Menu.NONE, 7, Menu.NONE, "Share Link to Post");
                        }
                        SubMenu sub3 = extpop.getMenu().addSubMenu(Menu.NONE, 2, Menu.NONE, "LOLtag Post");
                        sub3.add(Menu.NONE, 8, Menu.NONE, "lol");
                        sub3.add(Menu.NONE, 9, Menu.NONE, "inf");
                        sub3.add(Menu.NONE, 10, Menu.NONE, "unf");
                        sub3.add(Menu.NONE, 11, Menu.NONE, "ugh");
                        sub3.add(Menu.NONE, 12, Menu.NONE, "wtf");
                        sub3.add(Menu.NONE, 13, Menu.NONE, "tag");
                        extpop.getMenu().add(Menu.NONE, 14, Menu.NONE, "Check LOL Taggers");
                        if ((_showModTools) && (_rootPostId != 0)) {
                            extpop.getMenu().add(Menu.NONE, 15, Menu.NONE, "Mod Tools");
                        }
                        extpop.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                if (item.getItemId() <= 2)
                                    return false;
                                switch (item.getItemId()) {
                                    case 3:
                                        shackmessageTo(unamefinal);
                                        break;
                                    case 4:
                                        searchForPosts(unamefinal);
                                        break;
                                    case 5:
                                        copyPostText(pos);
                                        break;
                                    case 6:
                                        copyURL(pos);
                                        break;
                                    case 7:
                                        shareURL(pos);
                                        break;
                                    case 8:
                                    case 9:
                                    case 10:
                                    case 11:
                                    case 12:
                                    case 13:
                                        lolPost((String) item.getTitle(), pos);
                                        break;
                                    case 14:
                                        new GetTaggersTask().execute(_adapter.getItem(pos).getPostId());
                                        break;
                                    case 15:
                                        modChoose(pos);
                                        break;
                                    case 16:
                                        ((MainActivity) getActivity()).openHighlighter(unamefinal);
                                        break;
                                }
                                return true;
                            }
                        });
                        extpop.show();
                    }
                });


                // donator lol button
                if ((_displayLolButton) && (_messageId == 0))
                    holder.buttonLol.setVisibility(View.VISIBLE);
                else
                    holder.buttonLol.setVisibility(View.GONE);


                // this must be done for recycled views, setmodtagsfalse doesnt handle loading
                if (!_adapter.isCurrentlyLoading()) {
                    //holder.rowtype.setLoading(false);
                    holder.loading.setVisibility(View.GONE);
                }

                // check if root post and loading
                if (((p.getPostId() == _rootPostId) && (_adapter.isCurrentlyLoading()))) {
                    holder.expandedView2.setVisibility(View.VISIBLE);
                    holder.loading.setVisibility(View.VISIBLE);
                    //holder.rowtype.setLoading(true);
                } else {
                    holder.expandedView2.setVisibility(View.GONE);
                    holder.loading.setVisibility(View.GONE);
                    //holder.rowtype.setLoading(false);
                }

                // hide buttons on pqp posts
                if (p.isPQP()) {
                    holder.buttonLol.setVisibility(View.GONE);
                    holder.buttonOther.setVisibility(View.GONE);
                    holder.buttonReply.setVisibility(View.GONE);
                    //holder.rowtype.setLoading(false);
                } else {
                    holder.buttonLol.setVisibility(View.VISIBLE);
                    holder.buttonOther.setVisibility(View.VISIBLE);
                    holder.buttonReply.setVisibility(View.VISIBLE);
                }
            }
        }
        protected View createView(int position, View convertView, ViewGroup parent, boolean isExpanded)
        {

        	// get the thread to display and populate all the data into the layout
            Post p = getItem(position);

            ViewHolder holder = (ViewHolder)convertView.getTag();
            if ((holder == null) && (!isExpanded))
            {
                holder = new ViewHolder();
                
                // preview items
                holder.previewView = (CheckableTableLayout)convertView.findViewById(R.id.previewView);
                holder.previewRow = (TableRow) convertView.findViewById(R.id.previewRow);
                
				holder.treeIcon = (ImageView)convertView.findViewById(R.id.treeIcon); 
                holder.postingThrobber = (ProgressBar)convertView.findViewById(R.id.postingThrobber);
               
                holder.preview = (TextView)convertView.findViewById(R.id.textPreview);
                holder.previewLolCounts = (TextView)convertView.findViewById(R.id.textPostLolCounts);
                holder.previewUsernameHolder = (LinearLayout)convertView.findViewById(R.id.previewUNHolder);
                holder.previewUsername = (TextView)convertView.findViewById(R.id.textPreviewUserName);
                holder.previewLimeHolder = (ImageView)convertView.findViewById(R.id.previewLimeHolder);
                
                // first row expanded
                holder.rowtype = holder.previewView; // (CheckableLinearLayout)convertView.findViewById(R.id.rowType);
                // holder.username = (TextView)convertView.findViewById(R.id.textUserName);
                holder.postedtime = (TextView)convertView.findViewById(R.id.textPostedTime);
                
                
                // zoom for preview.. needs to only be done ONCE, when holder is first created
                holder.preview.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.preview.getTextSize() * _zoom);
                holder.previewLolCounts.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.previewLolCounts.getTextSize() * _zoom);

                // holder.username.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.username.getTextSize() * _zoom);
                holder.postedtime.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.postedtime.getTextSize() * _zoom);
                 
                
                if (_userNameHeight == 0)
                {
                	_userNameHeight = (int) (holder.previewUsername.getTextSize() * _zoom);
                	setOriginalUsernameHeight(_userNameHeight);
                }
                holder.previewUsername.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.previewUsername.getTextSize() * _zoom);
                
                
                convertView.setTag(holder);
            }
            
            // expanded post
            if (holder == null && (isExpanded))
            {
            	holder = new ViewHolder();
                holder.containerExp = (LinearLayout)convertView.findViewById(R.id.rowLayoutExp);
                
                holder.expandedView = (View)convertView.findViewById(R.id.expandedView);
                holder.expandedView2 = (View)convertView.findViewById(R.id.expandedView2);

                
                // expanded items
                holder.content = (TextView)convertView.findViewById(R.id.textContent);
                
                holder.loading = (ProgressBar)convertView.findViewById(R.id.tview_loadSpinner);
                
                holder.expLolCounts = (TextView)convertView.findViewById(R.id.textExpPostLolCounts);
                
                holder.buttonOther = (ImageButton)convertView.findViewById(R.id.buttonPostOpt);
                holder.buttonReply = (ImageButton)convertView.findViewById(R.id.buttonReplyPost);
                holder.buttonAllImages = (ImageButton)convertView.findViewById(R.id.buttonOpenAllImages);
                holder.buttonLol = (ImageButton)convertView.findViewById(R.id.buttonPostLOL);
                
                // zoom for expanded
                holder.content.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.content.getTextSize() * _zoom);
                holder.expLolCounts.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.expLolCounts.getTextSize() * _zoom);
                /*
                holder.buttonReply.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.buttonReply.getTextSize() * _zoom);
                holder.buttonAllImages.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.buttonAllImages.getTextSize() * _zoom);
                holder.buttonLol.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.buttonLol.getTextSize() * _zoom);
                */
                
                
                // buttons are already as small as they can be
                if (_zoom >= 0.9)
                {
                	ViewGroup.LayoutParams buttonLayout = holder.buttonOther.getLayoutParams();
	                buttonLayout.height = (int)Math.floor(buttonLayout.height * _zoom);
	                buttonLayout.width = (int)Math.floor(buttonLayout.width * _zoom);
	                holder.buttonOther.setLayoutParams(buttonLayout);
	                
	                buttonLayout = holder.buttonReply.getLayoutParams();
	                buttonLayout.height = (int)Math.floor(buttonLayout.height * _zoom);
	                buttonLayout.width = (int)Math.floor(buttonLayout.width * _zoom);
	                holder.buttonReply.setLayoutParams(buttonLayout);
	                
	                buttonLayout = holder.buttonAllImages.getLayoutParams();
	                buttonLayout.height = (int)Math.floor(buttonLayout.height * _zoom);
	                buttonLayout.width = (int)Math.floor(buttonLayout.width * _zoom);
	                holder.buttonAllImages.setLayoutParams(buttonLayout);
	                
	                buttonLayout = holder.buttonLol.getLayoutParams();
	                buttonLayout.height = (int)Math.floor(buttonLayout.height * _zoom);
	                buttonLayout.width = (int)Math.floor(buttonLayout.width * _zoom);
	                holder.buttonLol.setLayoutParams(buttonLayout);
                }
                convertView.setTag(holder);
            }


            // preview titleview
            if (!isExpanded)
            {


            	// this is created by the animator, have to remove it or recycled views get weird
                holder.previewRow.setLayoutTransition(null);

            	// reset container modifiers
            	holder.rowtype.setModTagsFalse();
            	holder.rowtype.setChecked(isExpanded(position));
            	if (p.isNWS())
                {
                	holder.rowtype.setNWS(true);
                }
                else if (p.isINF())
                {
                	holder.rowtype.setInf(true);
                }
                else if (p.isPolitical())
                {
                	holder.rowtype.setPolitical(true);
                }
                else
                {
                	holder.rowtype.refreshDrawableState(); // needed because setmodtagsfalse does not do this
                }

            	final double threadAge = TimeDisplay.threadAge(p.getPosted());
            	// set posted time
                if (threadAge <= 24f && _showHoursSince)
                {
                    // this is actually the same as the final else below, but this is the most common result
                    holder.postedtime.setText(TimeDisplay.doubleThreadAgeToString(threadAge));
                }
                else {
                    // check if this post is so old its not even the same year
                    // threadage > 8760 == one year. optimization to prevent getyear from being run on every thread
                    if (threadAge > 8760f && !TimeDisplay.getYear(TimeDisplay.now()).equals(TimeDisplay.getYear(p.getPosted())))
                    {
                        // older than one year
                        holder.postedtime.setText(TimeDisplay.convTime(p.getPosted(), "MMM dd, yyyy h:mma zzz"));
                    }
                    else
                    {
                        if ((!_showHoursSince) || (threadAge > 24f)) {
                            if (TimeDisplay.threadAge(p.getPosted()) > 96f) {
                                // default readout for !showsince or > 96h, has month
                                holder.postedtime.setText(TimeDisplay.convertTimeLong(p.getPosted()));
                            }
                            else {
                                // has only day of week
                                holder.postedtime.setText(TimeDisplay.convertTime(p.getPosted()));
                            }
                        } else {
                            // standard less than 24h with showtimesince... this will actually always be caught by the first if as an optimization
                            holder.postedtime.setText(TimeDisplay.doubleThreadAgeToString(threadAge));
                        }
                    }
                }

            	// 5L is used by the postqueue system to indicate the post hasnt been posted yet
            	if (p.getPosted() == 5L)
            		holder.postedtime.setText("Posting...");

            	// queued posts
            	if (p.isPQP() || p.isWorking())
        		{
            		holder.postingThrobber.setVisibility(View.VISIBLE);
        		}
        		else
        		{
        			holder.postingThrobber.setVisibility(View.GONE);
        		}
            	
            	// support highlight
            	holder.preview.setText(applyHighlight(p.getPreview()));
            	holder.preview.setLinkTextColor(getResources().getColor(R.color.linkColor));
            	
                holder.previewUsername.setText(applyHighlight(p.getUserName()));

                if (p.getUserName().equalsIgnoreCase(_userName))
                {
                    // highlight your own posts
                    holder.previewUsername.setTextColor(getResources().getColor(R.color.selfUserName));
                }
                else if (p.getUserName().equalsIgnoreCase(_OPuserName) && (position != 0))
                {
                    holder.previewUsername.setTextColor(getResources().getColor(R.color.OPUserName));
                }
                else if (p.isFromEmployee())
                {
                    holder.previewUsername.setTextColor(getResources().getColor(R.color.emplUserName));
                }
                else if (p.isFromModerator())
                {
                    holder.previewUsername.setTextColor(getResources().getColor(R.color.modUserName));
                }
                else
                {
                    holder.previewUsername.setTextColor(getResources().getColor(R.color.userName));
                }

                // donator icon
                holder.previewLimeHolder.setImageResource(android.R.color.transparent);
                holder.previewLimeHolder.setOnClickListener(null);
                holder.previewLimeHolder.setClickable(false);
                if (_displayLimes)
                {
                    if (_donatorList.contains(":" + p.getUserName().toLowerCase() + ";"))
                    {
                        holder.previewLimeHolder.setImageDrawable(_donatorIcon);
                    }
                    if (_donatorGoldList.contains(":" + p.getUserName().toLowerCase() + ";"))
                    {
                        holder.previewLimeHolder.setImageDrawable(_donatorGoldIcon);
                    }
                    if (_donatorQuadList.contains(":" + p.getUserName().toLowerCase() + ";"))
                    {
                        holder.previewLimeHolder.setImageDrawable(_donatorQuadIcon);
                        // easter egg
                        holder.previewLimeHolder.setClickable(true);
                        holder.previewLimeHolder.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                AnimationDrawable quad = (AnimationDrawable) getActivity().getResources().getDrawable(R.drawable.quaddamage);
                                ((ImageView)v).setImageDrawable(quad);
                                ((ImageView)v).setOnClickListener(null);
                                v.setClickable(false);
                                quad.start();
                            }
                        });
                        /*

                        */
                    }
                    if (p.getUserName().toLowerCase().equals("the man with the briefcase"))
                    {
                        holder.previewLimeHolder.setImageDrawable(_briefcaseIcon);
                    }
                }

            	// tree branch
            	buildTreeBranches(p, position, holder.treeIcon);


                // highlight newer posts
                int color = 255 - (12 * Math.min(p.getOrder(), 10));
                holder.preview.setTextColor(Color.argb(255, color, color, color));

                // has the title view been swapped?
                if (isExpanded(position))
            	{
            		// do things to change preview row
        			holder.preview.setVisibility(View.GONE);
        			holder.previewLolCounts.setVisibility(View.GONE);
        			holder.postedtime.setVisibility(View.VISIBLE);
                    holder.previewUsername.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.previewUserNameSizeBig));

                    final TextView txtusr = holder.previewUsername;
                    holder.previewUsername.setOnClickListener(getUserNameClickListenerForPosition(position, holder.previewUsername));
                    holder.previewUsername.setClickable(true);
            	}
            	else
            	{
            		//  preview mode
                    holder.previewUsername.setOnClickListener(null);
                    holder.previewUsername.setClickable(false);
        			holder.preview.setVisibility(View.VISIBLE);
                    holder.previewUsername.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.previewUserNameSize));
        			holder.previewLolCounts.setVisibility(View.VISIBLE);
        			holder.postedtime.setVisibility(View.GONE);
            	}

            	if ((p.getLolObj() != null) && !isExpanded(position)) {
            		holder.previewLolCounts.setVisibility(View.VISIBLE);
            		holder.previewLolCounts.setText(p.getLolObj().getTagSpan());
            	}
            	else { holder.previewLolCounts.setVisibility(View.GONE); }


            }


            return convertView;
        }
        
        private CharSequence applyExtLink(Spannable text, TextView t) {
        	// this thing puts little world buttons at the end of links to provide link actions
        	CustomURLSpan[] list = text.getSpans(0, text.length(), CustomURLSpan.class);
        	SpannableStringBuilder builder = new SpannableStringBuilder(text);
        	for (CustomURLSpan target : list)
        	{
        		Drawable iSpan = getActivity().getResources().getDrawable(R.drawable.ic_action_action_launch);
        		iSpan.setBounds(0, 0, t.getLineHeight(),t.getLineHeight());
        		builder.insert(text.getSpanEnd(target), " o");
				builder.setSpan(new ImageSpan(iSpan, DynamicDrawableSpan.ALIGN_BOTTOM) , text.getSpanEnd(target) +1, text.getSpanEnd(target)+2,   Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				final Context activ = getContext();
				final String href = target.getURL();
				ClickableSpan clickspan = new ClickableSpan(){
					@Override
					public void onClick(View widget) {
                        /*
                        PopupMenu hrefpop = new PopupMenu(getContext(), widget);
                        hrefpop.getMenu().add(Menu.NONE, 0, Menu.NONE, "Open Externally");
                        hrefpop.getMenu().add(Menu.NONE, 1, Menu.NONE, "Open in Popup Browser");
                        hrefpop.getMenu().add(Menu.NONE, 2, Menu.NONE, "Copy URL");
                        hrefpop.getMenu().add(Menu.NONE, 3, Menu.NONE, "Share Link");
                        hrefpop.getMenu().add(Menu.NONE, 4, Menu.NONE, "Change Default Action");
                        hrefpop.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                if (item.getItemId() == 2)
                                {
                                    ClipboardManager clipboard = (ClipboardManager)activ.getSystemService(Activity.CLIPBOARD_SERVICE);
                                    clipboard.setText(href);
                                    Toast.makeText(activ, href, Toast.LENGTH_SHORT).show();
                                    return true;
                                }
                                if (item.getItemId() == 3)
                                {
                                    Intent sendIntent = new Intent();
                                    sendIntent.setAction(Intent.ACTION_SEND);
                                    sendIntent.putExtra(Intent.EXTRA_TEXT, href);
                                    sendIntent.setType("text/plain");
                                    activ.startActivity(Intent.createChooser(sendIntent, "Share Link"));
                                    return true;
                                }
                                if (item.getItemId() == 0)
                                {
                                    Uri u = Uri.parse(href);
                                    if (u.getScheme() == null)
                                    {
                                        u = Uri.parse("http://" + href);
                                    }
                                    Intent i = new Intent(Intent.ACTION_VIEW,
                                            u);
                                    activ.startActivity(i);
                                    return true;
                                }
                                if (item.getItemId() == 1)
                                {
                                    ((MainActivity)activ).openBrowser(href);
                                    return true;
                                }
                                if (item.getItemId() == 4)
                                {
                                    Intent i = new Intent(getActivity(), PreferenceView.class);
                                    i.putExtra("pscreenkey", "popupbrowser");
                                    startActivityForResult(i, ThreadListFragment.OPEN_PREFS);
                                    return true;
                                }
                                return false;
                            }
                        });
                        hrefpop.show();
                        */
                        MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(activ);
		    			builder.setTitle(href);
		    	        final CharSequence[] items = { "Open Externally", "Open in Popup Browser","Copy URL","Share Link", "Change Default Action"};
		    	        builder.setItems(items, new DialogInterface.OnClickListener() {
		    	            public void onClick(DialogInterface dialog, int item) {
		    	                if (item == 2)
		    	                {
		    	                	ClipboardManager clipboard = (ClipboardManager)activ.getSystemService(Activity.CLIPBOARD_SERVICE);
		    	                	clipboard.setText(href);
		    	                	Toast.makeText(activ, href, Toast.LENGTH_SHORT).show();
		    	                }
		    	                if (item == 3)
		    	                {
		    	                	Intent sendIntent = new Intent();
		    	            	    sendIntent.setAction(Intent.ACTION_SEND);
		    	            	    sendIntent.putExtra(Intent.EXTRA_TEXT, href);
		    	            	    sendIntent.setType("text/plain");
		    	            	    activ.startActivity(Intent.createChooser(sendIntent, "Share Link"));
		    	                }
		    	                if (item == 0)
		    	                {
		    	                	Uri u = Uri.parse(href);
		    	                	if (u.getScheme() == null)
		    	                	{
		    	                		u = Uri.parse("http://" + href);
		    	                	}
		    	                	Intent i = new Intent(Intent.ACTION_VIEW,
		    	             		       u);
		    	             		activ.startActivity(i);
		    	                }
		    	                if (item == 1)
		    	                {
		    	                	((MainActivity)activ).openBrowser(href);
		    	                }
		    	                if (item == 4)
		    	                {
		    	                	Intent i = new Intent(getActivity(), PreferenceView.class);
		    		                i.putExtra("pscreenkey", "popupbrowser");
		    		                startActivityForResult(i, ThreadListFragment.OPEN_PREFS);
		    	                }
		    	                }});
		    	        AlertDialog alert = builder.create();
		    	        alert.setCanceledOnTouchOutside(true);
		    	        alert.show();

		    		}
				};
				builder.setSpan(clickspan , text.getSpanEnd(target) +1, text.getSpanEnd(target)+2,   Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        	}
        	
			return builder;
		}

		private CharSequence applyHighlight(String preview)
        {
            if ((_highlight != null) && (_highlight.length() > 0))
            {
                return applyHighlight(new SpannableString(preview));
            }
            else {
                return preview;
            }
        }
        private CharSequence applyHighlight(Spannable preview) {
        	if ((_highlight != null) && (_highlight.length() > 0))
        	{
        		Spannable text = preview;
        		String txtplain = text.toString().toLowerCase();
        		int color = getResources().getColor(R.color.modtag_political);
        		Spannable highlighted = new SpannableString(text);
        		int startSpan = 0, endSpan = 0;
        		String target = _highlight;
        		while (true) {
                    startSpan = txtplain.indexOf(target, endSpan);
                    BackgroundColorSpan foreColour = new BackgroundColorSpan(color);
                    // Need a NEW span object every loop, else it just moves the span
                    if (startSpan < 0)
                        break;
                    endSpan = startSpan + target.length();
                    highlighted.setSpan(foreColour, startSpan, endSpan,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
        		return highlighted;
        	}
        	else
        	{
        		return preview;
        	}
		}

		private ImageView buildTreeBranches(Post t, int position, ImageView imageView)
        {
        	StringBuilder depthStr = new StringBuilder(t.getDepthString());

        	if (depthStr.length() > 0)
        	{
        		final String imageKey = t.getDepthString();

        	    final Bitmap bitmap = getBitmapFromMemCache(imageKey);
        	    if (bitmap == null) {
        	    	System.out.println("HAD TO REBUILD CACHE");
        	    	imageView.setImageBitmap(buildBranchesForPost(t));
        	    }
	        	else
	        	{
	        		imageView.setImageBitmap(bitmap);
	        	}
        	}
        	else
        	{
                // this is to enforce row height, otherwise the view will collapse and look weird on root posts which do not have bullets
        		imageView.setImageResource(R.drawable.bullet_spacer);
        	}
        	imageView.forceLayout();
        	imageView.setAdjustViewBounds(true);
        	return imageView;
        }
        
        
        private Bitmap buildBranchesForPost(Post t)
        {
        	int bulletWidth = _bulletBlank.getWidth();
        	StringBuilder depthStr = new StringBuilder(t.getDepthString());
        	
        	if (depthStr.length() > 0)
        	{
	        	Bitmap big = Bitmap.createBitmap(bulletWidth * depthStr.length(), _bulletBlank.getHeight(), Bitmap.Config.ARGB_4444);
	        	Canvas canvas = new Canvas(big);
	        	
	        	for (int i = 0; i < depthStr.length(); i++)
	        	{
	        		
	            	if (depthStr.charAt(i) == "L".charAt(0))
	            		canvas.drawBitmap(_bulletEnd, bulletWidth * i, 0, null);
	            	if (depthStr.charAt(i) == "T".charAt(0))
	            		canvas.drawBitmap(_bulletBranch, bulletWidth * i, 0, null);
	            	if (depthStr.charAt(i) == "|".charAt(0))
	            		canvas.drawBitmap(_bulletExtendPast, bulletWidth * i, 0, null);
	            	if (depthStr.charAt(i) == "[".charAt(0))
	            		canvas.drawBitmap(_bulletEndNew, bulletWidth * i, 0, null);
	            	if (depthStr.charAt(i) == "+".charAt(0))
	            		canvas.drawBitmap(_bulletBranchNew, bulletWidth * i, 0, null);
	            	if (depthStr.charAt(i) == "!".charAt(0))
	            		canvas.drawBitmap(_bulletExtendPastNew, bulletWidth * i, 0, null);
	            	if (depthStr.charAt(i) == "0".charAt(0))
	            		canvas.drawBitmap(_bulletBlank, bulletWidth * i, 0, null);
	            	if (depthStr.charAt(i) == "C".charAt(0))
	            		canvas.drawBitmap(_bulletCollapse, bulletWidth * i, 0, null);
	            	
	            	
	        	}
	        	addBitmapToMemoryCache(t.getDepthString(), big);
	        	return big;
        	}
        	return null;
        }
        
        private Bitmap createBullet(int id)
        {
        	Bitmap bm = BitmapFactory.decodeResource(getResources(), id);
        	if (_zoom != 1.0f)
        	{
	        	int newH = (int)(bm.getHeight() * _zoom);
	        	int newW = (int)(bm.getWidth() * _zoom);
	        	bm = Bitmap.createScaledBitmap(bm, newW, newH, false);
        	}
        	return bm;
        }
        
        private void createAllBullets()
        {
        	_bulletBlank = createBullet(R.drawable.bullet_blank);
        	_bulletEnd = createBullet(R.drawable.bullet_end);
        	_bulletExtendPast = createBullet(R.drawable.bullet_extendpast);
        	_bulletBranch = createBullet(R.drawable.bullet_branch);
        	_bulletEndNew = createBullet(R.drawable.bullet_endnew);
        	_bulletExtendPastNew = createBullet(R.drawable.bullet_extendpastnew);
        	_bulletBranchNew = createBullet(R.drawable.bullet_branchnew);
        	_bulletCollapse = createBullet(R.drawable.bullet_collapse);
        	_bulletWidth = _bulletCollapse.getWidth();
        	
        	// donator icon
        	int size =  (int) (_bulletCollapse.getHeight() * 0.75);
        	BitmapDrawable bm = new BitmapDrawable(getContext().getResources(), Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher), size, size, false));
        	/*ColorMatrix cm = new ColorMatrix();
        	cm.setSaturation(0.8f);
        	final float m[] = cm.getArray();
        	final float c = 0.8f;
        	final float bright = 0.6f;
        	cm.set(new float[] { 
        	        m[ 0] * c, m[ 1] * c, m[ 2] * c, m[ 3] * c, m[ 4] * c + bright, 
        	        m[ 5] * c, m[ 6] * c, m[ 7] * c, m[ 8] * c, m[ 9] * c + bright, 
        	        m[10] * c, m[11] * c, m[12] * c, m[13] * c, m[14] * c + bright, 
        	        m[15]    , m[16]    , m[17]    , m[18]    , m[19] }); 
        	        */
        	bm.setColorFilter(new LightingColorFilter(Color.argb(1, 175, 175, 175), 0));
            // bm.setColorFilter(new ColorMatrixColorFilter(cm));
        	_donatorIcon = bm;

            bm = new BitmapDrawable(getContext().getResources(), Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.limegold), size, size, false));
            bm.setColorFilter(new LightingColorFilter(Color.argb(1, 175, 175, 175), 0));
            _donatorGoldIcon = bm;

            bm = new BitmapDrawable(getContext().getResources(), Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.iconpowerup_quad), size, size, false));
            bm.setColorFilter(new LightingColorFilter(Color.argb(1, 175, 175, 175), 0));
            _donatorQuadIcon = bm;

            bm = new BitmapDrawable(getContext().getResources(), Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.briefcaseicon), size, size, false));
            bm.setColorFilter(new LightingColorFilter(Color.argb(1, 175, 175, 175), 0));
            _briefcaseIcon = bm;
        }

        @Override
        protected ArrayList<Post> loadData() throws Exception
        {
        	if ((_rootPostId > 0) && (_messageId == 0))
        	{
	            ArrayList<Post> posts = ShackApi.processPosts(ShackApi.getPosts(_rootPostId, this.getContext(), ShackApi.getBaseUrl(getActivity())), _rootPostId, _maxBullets, getActivity());
	            _shackloldata = ShackApi.getLols(getActivity());
	            if (_shackloldata.size() != 0)
	            {
	            	// check if this thread has shacklol data
	            	if (_shackloldata.containsKey(Integer.toString(_rootPostId)))
	            	{
	            		_threadloldata = _shackloldata.get(Integer.toString(_rootPostId));
	            	}
	            }
	            
	            if (posts.size() > 0)
	            {
	            	// set op username for hilights
	            	_OPuserName = posts.get(0).getUserName();
	            }
	/*
	            if ((this.getCount() > 0) && (posts.size() > 0))
	            {
	            	// make sure the first items of both arent dupes from preloading the root post
	            	if (this.getItem(0).getPostId() == posts.get(0).getPostId())
	            	{
	            		posts.remove(0);
	            	}
	            }
*/
	            for (Post p: posts)
	            {
	            	//

	            	// load lols
	            	if ((_getLols) && (_lolsInPost) && (_threadloldata != null) && (_threadloldata.containsKey(Integer.toString(p.getPostId()))))
	            	{
	            		p.setLolObj(_threadloldata.get(Integer.toString(p.getPostId())));
	            	}
	            }
	            
	            // fast scroll on mega threads
            	if (getActivity () != null)
        		{
            		boolean set = false;
            		if ((posts.size() > 300))
            			set = true;
            		final boolean set2 = set && _fastScroll;
            		
        			getActivity().runOnUiThread(new Runnable(){
    	        		@Override public void run()
    	        		{
    	        			if (_viewAvailable)
    	                	{
    	        				if (getListView() != null)
    	                    	{
    	        					getListView().setFastScrollEnabled(set2);
    	                    	}
    	                	}
    	        		}
        			});
        		}
            	// reduce the number of post images which must be generated on the fly


                Long timer = TimeDisplay.now();
                fakePostAddinator(posts);
                System.out.println("TIMER: FPA: " + (TimeDisplay.now() - timer)); timer = TimeDisplay.now();
                createThreadTree(posts);
                System.out.println("TIMER: CTT: " + (TimeDisplay.now() - timer)); timer = TimeDisplay.now();
                prePreBuildTreeCache(posts);

	            return posts;
        	}
        	else
        		return null;
        }
        @Override
        public void beforeClear()
        {
            setListAdapter(null);
        }

    	@Override
    	public void afterDisplay()
    	{

            Long timer = TimeDisplay.now();

            expandAndCheckPostWithoutAnimation(0);

            // pull to refresh integration
            if (getActivity() != null)
                ((MainActivity)getActivity()).getRefresher().setRefreshComplete();
            System.out.println("TIMER: SRC: " + (TimeDisplay.now() - timer)); timer = TimeDisplay.now();

            // attempt at creating json so thread can be saved saveThread()
            // this is for when we receive a loading command via intent without a thread json at the same time
            boolean hasReplied = false;
            String userName = _prefs.getString("userName", "");

            if (userName.length() > 0)
            {
                for (int i = 0; i < _adapter.getCount(); i++)
                {
                    if (_adapter.getItem(i).getUserName().equals(userName))
                        hasReplied = true;
                }
            }
            System.out.println("TIMER: hasReplied: " + (TimeDisplay.now() - timer)); timer = TimeDisplay.now();
            Thread t = null;
            if ((_adapter != null) && (_adapter.getCount() > 0) && (_messageId == 0))
            {
                // create fake thread for fav saving
                t = new Thread(_adapter.getItem(0).getPostId(), _adapter.getItem(0).getUserName(), _adapter.getItem(0).getContent(), _adapter.getItem(0).getPosted(), _adapter.getCount(), "ontopic", hasReplied, ((MainActivity)getActivity()).mOffline.containsThreadId(_rootPostId));
                if (_adapter.getItem(0) != null)
                {
                    _rootPostId = _adapter.getItem(0).getPostId();
                }
            }

            if (t != null)
            {
                // create data for fav saving
                if (t.getJson() != null)
                    _lastThreadJson = t.getJson();
            }
            System.out.println("TIMER: fakeJson: " + (TimeDisplay.now() - timer)); timer = TimeDisplay.now();
    		// autofave. TODO: MAKE WORK WITH POSTQUEUE
            if (_autoFaveOnLoad)
            {
            	saveThread();
            	_autoFaveOnLoad = false;
            }
            
            // mark as read if in favs
            if (((MainActivity)getActivity()).mOffline.containsThreadId(_rootPostId))
            {
            	((MainActivity)getActivity()).markFavoriteAsRead(_rootPostId, _adapter.getCount());
            }
            System.out.println("TIMER: markRead: " + (TimeDisplay.now() - timer)); timer = TimeDisplay.now();



    		// select posts for _selectpostidafterloading
            setListAdapter(this);
            ensurePostSelectedAndDisplayed();

            System.out.println("TIMER: EPS: " + (TimeDisplay.now() - timer)); timer = TimeDisplay.now();
            updateThreadViewUi();
            System.out.println("TIMER: updUI: " + (TimeDisplay.now() - timer)); timer = TimeDisplay.now();
    	}
        
        private class ViewHolder
        {
			public LinearLayout previewUsernameHolder;
			public LinearLayout containerExp;
			public ProgressBar postingThrobber;
			public ImageButton buttonLol;
			public ImageView previewLimeHolder;
			public View anchor;
			public int currentPostId;
			public ImageButton buttonAllImages;
			public View expandedView2;
			public TableRow previewRow;
			public ProgressBar loading;
			public CheckableTableLayout rowtype;
			
			ImageView treeIcon;
			
			/* OLD WAY TO DO BRANCHES
			LinearLayout treeIcon;
			public LinearLayout treeIconExp;
			*/
			public TextView previewUsername;
			public ImageButton buttonRefresh;
			public ImageButton buttonOther;
			public ImageButton buttonReply;
			public TextView expLolCounts;
			public TextView previewLolCounts;
			public View expandedView;
			public CheckableTableLayout previewView;
			
            TextView content;
            TextView preview;
            //TextView username;
            TextView postedtime;
        }
        
        public ArrayList<Post> fakePostAddinator(ArrayList<Post> posts)
        {
        	String selfUserName = _prefs.getString("userName", "default user");
            // get postqueueposts
            PostQueueDB pqpdb = new PostQueueDB(getActivity());
            pqpdb.open();
            ArrayList<PostQueueObj> pqplist = (ArrayList<PostQueueObj>) pqpdb.getAllPostsInQueue(false);
            pqpdb.close();
            
            ArrayList<Integer> parentIds = new ArrayList<Integer>();
            
            for (PostQueueObj pqo : pqplist)
            {
            	parentIds.add(pqo.getReplyToId());
            }
            
            ArrayList<Integer> postIds = new ArrayList<Integer>();
            for (int i = 0; i < posts.size(); i++)
            {
            	if (!posts.get(i).isPQP())
            		postIds.add(posts.get(i).getPostId());
            }
            
	        for (int i = 0; i < posts.size(); i++)
	        {
	        	Post curPost = posts.get(i);
	        	
	        	// add a fake reply if we have a matching post in the adapter that has a replytoid of this postid
	            if (parentIds.contains(curPost.getPostId()))
	            {
	            	for (PostQueueObj pqo : pqplist)
	            	{
	            		// System.out.println("ADDINATOR: replytoid : curpostid " + pqo.getReplyToId() + ":"+ curPost.getPostId());
	            		// a parent is found and the real post with the same finalid as this postqueuedpost doesnt yet exist
	            		if ((pqo.getReplyToId() == curPost.getPostId()) && (!postIds.contains(pqo.getFinalId())))
	            		{
	            			String body = ComposePostView.getPreviewFromHTML(pqo.getBody());
			            	Post fakePost = new Post((pqo.getFinalId() == 0) ? Integer.parseInt(Long.toString(pqo.getPostQueueId())) : pqo.getFinalId(), selfUserName, body, (pqo.getFinalId() == 0) ? 5L : TimeDisplay.now(), curPost.getLevel() + 1, "ontopic", false, false, (pqo.getFinalId() == 0) ? true : false);
			            	// copy the depth string from parent
			            	//fakePost.setDepthString(curPost.getDepthString().substring(0, curPost.getDepthString().length() - 1) + " " + "L");
			            	
			            	//if (getBitmapFromMemCache(fakePost.getDepthString()) == null)
			            	//	addBitmapToMemoryCache(fakePost.getDepthString(), buildBranchesForPost(fakePost));
			            	
			            	fakePost.setOrder(0);
			            	
			            	// find right place
			            	int place = 1;
			            	for (int j = 1; true; j++)
			            	{
			            		if ((posts.size() > i+j) && (posts.get(i + j).getLevel() < curPost.getLevel() + 1))
			            		{
			            			place = i+j;
			            			break;
			            		}
			            		if (posts.size() <= i+j)
			            		{
			            			place = i+j;
			            			break;
			            		}
			            	}
			            	// System.out.println("ADDINATOR: adding pqo" + pqo.getPostQueueId()+ " to place" + place + " " + fakePost.getContent() + " " + fakePost.getPostId());
			            	posts.add(place, fakePost);
	            		}
	            	}
	            }
	        }
            return posts;
		}
        
        public void fakePostRemoveinator() {
    		System.out.println("THREADVIEW POSTQU: Got signal to remove fakeposts");
    		if (_adapter != null)
    		{
    	        for (int i = 0; i < _adapter.getCount(); i++)
    	        {
    	            Post post = _adapter.getItem(i);
    	            if (post.isPQP())
    	            {
    	            	_adapter.remove(post);
    	            	i--;
    	            }
    	        }
    		}
        }
        
        public ArrayList<Post> createThreadTree(ArrayList<Post> posts)
        {
            // create depthstrings
            for (int i = 0; i < posts.size(); i++)
            {
            	int j = i -1;
            	while ((j > 0) && (posts.get(j).getLevel() >= posts.get(i).getLevel()))
            	{
            		StringBuilder jDString = new StringBuilder(posts.get(j).getDepthString());
            		
            		// L is a seen reply |_, [ is unseen green line |_
            		if ((jDString.charAt(posts.get(i).getLevel()-1) == "L".charAt(0)) && (posts.get(i).getLevel() == posts.get(j).getLevel()))
            		{
            			jDString.setCharAt(posts.get(i).getLevel()-1, "T".charAt(0));
            		}
            		if ((jDString.charAt(posts.get(i).getLevel()-1) == "[".charAt(0)) && (posts.get(i).getLevel() == posts.get(j).getLevel()))
            		{
            			jDString.setCharAt(posts.get(i).getLevel()-1, "+".charAt(0));
            		}
            		// 0 denotes blank space
            		if (jDString.charAt(posts.get(i).getLevel()-1) == "0".charAt(0))
            		{
            			// ! denotes green line, | denotes gray
            			if (posts.get(i).getSeen())
            				jDString.setCharAt(posts.get(i).getLevel()-1, "|".charAt(0));
            			else
            				jDString.setCharAt(posts.get(i).getLevel()-1, "!".charAt(0));
            		}

                    posts.get(j).setDepthString(jDString.toString());
            		j--;
            	}
            }
            
            // collapser for deep threads
            for (int i = 0; i < posts.size(); i++)
            {
	            StringBuilder depthStr = new StringBuilder(posts.get(i).getDepthString());
	        	
	        	// collapser for deep threads
	        	if (depthStr.length() >= _maxBullets)
	        	{
		        	int j = 0;
		        	String depthStr2 = depthStr.toString();
		        	while (depthStr2.length() > _maxBullets)
		        	{
		        		// the higher this number the higher the chunking of the collapser. higher numbers are arguably better to a point
		        		depthStr2 = depthStr2.substring(Math.round((float)(_maxBullets * 0.75f)));
		        		j++;
		        	}
		        	if (j > 0)
		        	{
			        	String repeated = new String(new char[j]).replace("\0", "C");
			        	// String repeated2 = new String(new char[(depthStr2.length() - 1)]).replace("\0", "0");
			        	String repeated2 = depthStr2.substring(j);
			        	depthStr = new StringBuilder(repeated + repeated2);
		        	}
	        	}

                posts.get(i).setDepthString(depthStr.toString());
            }
            return posts;
        }
        
        // used on posts 
        public void prePreBuildTreeCache(ArrayList<Post> posts)
        {
            // prebuild tree branches (optimization) during loading instead of when scrolling
            for (int i = 0; i < posts.size(); i++)
            {
            	Post p = posts.get(i);
            	if (getBitmapFromMemCache(p.getDepthString()) == null)
            		addBitmapToMemoryCache(p.getDepthString(), buildBranchesForPost(p));
            }
        }

    }

    public void adjustSelected(int movement)
    {
    	if (_viewAvailable)
    	{
    		if (_lastExpanded != _adapter.getExpandedPosition() && _adapter.getExpandedPosition() != -1)
    			_lastExpanded = _adapter.getExpandedPosition();
	        final int index = _lastExpanded + movement;
	        if (index >= 0 && index < getListView().getCount())
	        {
	        	ensureVisible(index, true, true, false);
	            
	        }
    	}
    }
    
    void ensureVisible(int position, boolean withPostMove, final boolean withExpansion, final boolean forcePostToTop)
    {
    	ListView view = getListView();

        if (view != null) {

            if (position < 0 || position >= view.getCount())
                return;

            int firstPositionVisible = view.getFirstVisiblePosition();
            int lastPositionVisible = view.getLastVisiblePosition();
            int destination = 0;

            if (position < firstPositionVisible)
                destination = position;
            else if (position >= lastPositionVisible)
                destination = (position - (lastPositionVisible - firstPositionVisible));

            if ((position < firstPositionVisible) || forcePostToTop) {
                view.setSelectionFromTop(destination, 0);
            } else if (position >= lastPositionVisible) {
                System.out.println("STUFF:L " + (lastPositionVisible - firstPositionVisible) + " gvt:" + view.getChildCount());
                view.setSelectionFromTop(lastPositionVisible + 1, view.getChildAt(lastPositionVisible - firstPositionVisible).getBottom() - 5);
            }
            if (withPostMove) {
                // keep the child view on screen
                final int pos = position;
                final ListView listView = view;
                listView.post(new Runnable() {

                    @Override
                    public void run() {
                        View betterView = getListViewChildAtPosition(pos, listView);
                        if (betterView != null) {
                            listView.requestChildRectangleOnScreen(betterView, new Rect(0, 0, betterView.getRight(), betterView.getHeight()), false);
                        } else
                            listView.smoothScrollToPosition(pos);

                        if (withExpansion) {
                            listView.post(new Runnable() {

                                @Override
                                public void run() {
                                    _adapter.expand(pos);
                                }
                            });
                        }
                    }
                });
            }
        }
    }
    
    @Override
	public void onResume()
    {
    	super.onResume();
    	
    	//  update all PQPs
    	if ((_adapter != null) && (getActivity() != null))
    	{
    		for (int i = 0; i < _adapter.getCount(); i++)
    		{
    			if (_adapter.getItem(i).isPQP())
    			{
    				System.out.println("getting db backed pqo from id " + _adapter.getItem(i).getPostId());
    				PostQueueObj pqo = PostQueueObj.FromDB(_adapter.getItem(i).getPostId(), getActivity());
    				if ((pqo != null) && (pqo.getFinalId() != 0))
    				{
    					_adapter.getItem(i).setPostId(pqo.getFinalId());
    					_adapter.getItem(i).setIsPQP(false);
    					_adapter.getItem(i).setPosted(TimeDisplay.now());
    				}
    			}
    		}
    		_adapter.notifyDataSetChanged();
    	}
    }

	public void updatePQPostIdToFinal(int PQPId, int finalId) {
		System.out.println("THREADVIEW POSTQU: Got signal to update PQPid");
		if (_adapter != null)
		{
			boolean found = false;
			int length = _adapter.getCount();
	        for (int i = 0; i < length; i++)
	        {
	            Post post = _adapter.getItem(i);
	            if ((post.isPQP()) && (post.getPostId() == PQPId))
	            {
	            	post.setPostId(finalId);
	            	post.setIsPQP(false);
	            	post.setPosted(TimeDisplay.now());
	            	found = true;
	            	break;
	            }
	        }
	        // update this too
	        if (_selectPostIdAfterLoading == PQPId)
	        {
	        	System.out.println("SPIALID UPDATED FROM PQPID TO REALID");
	        	_selectPostIdAfterLoading = finalId;
	        	_isSelectPostIdAfterLoadingIdaPQPId  = false;
	        }
	        
	        if (found)
	        	_adapter.notifyDataSetChanged();
		}
		
	}
	
	class StyleCallback implements ActionMode.Callback {

	    private TextView mTextView;

		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
	        MenuInflater inflater = mode.getMenuInflater();
	        inflater.inflate(R.menu.text_selection, menu);
	        
	        return true;
	    }

	    public void setTextView(TextView content) {
			mTextView = content;
		}

		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
	    	menu.findItem(R.id.menu_textSelectSearch).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    		if (menu.findItem(android.R.id.selectAll) != null)
    			menu.findItem(android.R.id.selectAll).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

	    	return false;
	    }

	    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
	        CharacterStyle cs;
	        TextView bodyView = mTextView;
	        int start = bodyView.getSelectionStart();
	        int end = bodyView.getSelectionEnd();
	        SpannableStringBuilder ssb = new SpannableStringBuilder(bodyView.getText());

	        Bundle args;
			switch(item.getItemId()) {

	        case R.id.menu_textSelectSearch:
	        	args = new Bundle();
	    		args.putString("terms", bodyView.getText().subSequence(start, end).toString());
	            ((MainActivity)getActivity()).openSearch(args);
	            return true;
	        }
	        return false;
	    }

	    public void onDestroyActionMode(ActionMode mode) {
	    }
	}
}
