package net.woggle.shackbrowse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import net.woggle.CheckableLinearLayout;
import net.woggle.SwipeDismissListViewTouchListener;
import net.woggle.SwipeDismissListViewTouchListener.DismissCallbacks;
import net.woggle.shackbrowse.MainActivity.mAnimEnd;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.app.ListFragment;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialogCompat;
import com.gc.materialdesign.views.ButtonFlat;

import static net.woggle.shackbrowse.StatsFragment.statInc;

public class ThreadListFragment extends ListFragment
{
    ThreadLoadingAdapter _adapter;

    // list view saved state while rotating
    private Parcelable _listState = null;
    private int _listPosition = 0;
    private int _itemPosition = 0;
    protected int _itemChecked = ListView.INVALID_POSITION;
    
    private String POST_COUNT_CACHE_FILENAME = "post_count.cache";
    final static int POST_CACHE_HISTORY = 500;
    
    final static String COLLAPSED_CACHE_FILENAME = "collapsed3.cache";
    
    final static String COLLAPSE_FILTWORD_FILENAME = "colfiltwords.cache";
    final static String DELETE_FILTWORD_FILENAME = "delfiltwords.cache";
    final static String COLLAPSE_USER_FILTWORD_FILENAME = "coluserfiltwords.cache";
    final static String DELETE_USER_FILTWORD_FILENAME = "deluserfiltwords.cache";
    
    final static int COLLAPSED_SIZE = 500;
    public OfflineThread _offlineThread;
    protected boolean _viewAvailable = false;
    private boolean _silentLoad = false;
	protected boolean _firstLoad = true;
	long _lastThreadGetTime = 0L;
    boolean _filtering = false;
    protected MaterialDialog _progressDialog;
    
	private SharedPreferences _prefs;

	protected boolean _nextBackQuitsBecauseOpenedAppViaIntent = false;

	private SwipeDismissListViewTouchListener _touchListener;

	public int _swipecollapse = 2;

	protected boolean _preventAutoLoad = false;

	public boolean mTurboAPIAllowed = true;

	private ArrayList<Integer> mCollapsed;


	private boolean mUndoBarClosing = false;
	private boolean mSnackBarOpen = false;
	private boolean mSnackBarStuckOpen = false;
	private Stack<SnackBarQueueItem> mSnackBarQueue = new Stack<SnackBarQueueItem>();
	private long _lastResumeTimeAndPrompt = 0L;
	private boolean mGetAllThreadsMode;
	private String mSortMode = "usual";

	// handle collapsed saving
	public void onPause()
	{
		try {
			storeCollapsed(mCollapsed);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.onPause();
	}
	public void onResume()
	{
		reloadCollapsed();

		// snackbar for old thread data, prevent multiople fires in less than 20 seconds
		if (SNKVERBOSE) System.out.println("SNK ONRESUME"+(System.currentTimeMillis() - _lastResumeTimeAndPrompt) + " " + _lastResumeTimeAndPrompt);
		getListView().postDelayed(new Runnable()
		{
			@Override
			public void run()
			{
				if (
						(
								(getActivity() != null)
								&& (getActivity() instanceof MainActivity)
								&& ((System.currentTimeMillis() - _lastResumeTimeAndPrompt) > 20000L)
								&& (_lastResumeTimeAndPrompt > 0L))
								&&  (TimeDisplay.threadAgeInHours(_lastThreadGetTime) > 3d)
								&& (((MainActivity)getActivity())._currentFragmentType == MainActivity.CONTENT_THREADLIST)
								&& (
								(!(((MainActivity)getActivity()).getSliderOpen()) && !(((MainActivity)getActivity()).getDualPane()))
										|| (((MainActivity)getActivity()).getDualPane())
						)
						)
				{
			/*
			SnackBar snackbar = new SnackBar(getActivity(), "Your threads are out of date by 3 or more hours", "Refresh", new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					refreshThreads();
				}
			});
			snackbar.setDismissTimer(4000);
			snackbar.setColorButton(getResources().getColor(R.color.SBmed));
			snackbar.show();
			*/
					if (SNKVERBOSE) System.out.println("SNK: OPEN ONRESUME REFRESH");
					_lastResumeTimeAndPrompt = System.currentTimeMillis();
					openSnackBar("Your threads are out of date by 3 or more hours", "Refresh", new View.OnClickListener()
					{
						@Override
						public void onClick(View view)
						{
							refreshThreads();
							closeSnackBar(true);
						}
					});

				}
			}
		}, 1000);


		super.onResume();
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	setRetainInstance(true);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {        
        _viewAvailable = true;
        View layout = inflater.inflate(R.layout.threadlist, null);
        layout.findViewById(R.id.tlist_root).setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_app_ics));
        return layout;
    }

    @Override
    public void onStart()
    {
        if (((_adapter != null) && (_adapter.getCount() > 0)) && (getListView().getVisibility() == View.GONE))
        {
        	System.out.println(" FIXING ANIMS");
        	getListView().setVisibility(View.VISIBLE);
        }

        super.onStart();
    }
    
    @Override
    public void onDestroyView()
    {
    	_viewAvailable = false;
    	super.onDestroyView();
    }
    
    public void instantiateAdapter()
    {
    	reloadCollapsed();
    	// no adapter? must be a new view
   		_adapter = new ThreadLoadingAdapter(getActivity(), new ArrayList<Thread>());
   		setListAdapter(_adapter);
    }
    
    public boolean getDualPane()
    {
    	View singleThread = getActivity().findViewById(R.id.singleThread);
       	return singleThread != null && singleThread.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
       
       	getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
       	//getListView().setBackgroundColor(getResources().getColor(R.color.app_bg_color));
       	getListView().setTextFilterEnabled(true);
       	
       	_prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
       	_swipecollapse = Integer.parseInt(_prefs.getString("swipeCollapse", "2"));
	    mGetAllThreadsMode = _prefs.getBoolean("getAllThreadsMode", false);
	    mSortMode = _prefs.getString("getSortMode", "usual");
       	// style things on the listview
       	
       	//this.getListView().setDivider(getActivity().getResources().getDrawable(R.drawable.divider));
       	this.getListView().setDividerHeight(0);
       	// getListView().setBackgroundColor(getActivity().getResources().getColor(R.color.app_bg_color));
       	/*
       	getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
            	onListItemLongClick((ListView)arg0, arg1, pos, id);
                return true;
            }
        });
       	*/
       	if ((_adapter == null) && (savedInstanceState == null))
       	{
       		instantiateAdapter();
       	}
       	else if (savedInstanceState != null)
       	{
       		_listState = savedInstanceState.getParcelable("tlist_listState");
       		_listPosition = savedInstanceState.getInt("tlist_listPosition");
       		if ((_viewAvailable) && (_listState != null))
       			getListView().onRestoreInstanceState(_listState);
       		
       		if (savedInstanceState.getParcelableArrayList("tlist_adapterItems") != null)
       		{
	       		ArrayList<Thread> items = savedInstanceState.getParcelableArrayList("tlist_adapterItems");
	       		instantiateAdapter();
	       		_adapter._itemList = items;
	       		System.out.println("RESTORED " + items.size() + " ITEMS" + "AD" + _adapter.getCount());
	       		
	       		_adapter._pageNumber = savedInstanceState.getInt("tlist_adapterPage");
	       		_adapter._threadIds = savedInstanceState.getIntegerArrayList("tlist_threadIds");
	       		_adapter.setLastCallSuccessful(true);
	       		
	       		_adapter.notifyDataSetChanged();
       		}
       		_itemPosition = savedInstanceState.getInt("tlist_itemPosition");
       		
       		if (_viewAvailable)
       			getListView().setSelectionFromTop(_listPosition,  _itemPosition);
       		       		
       		_itemChecked = savedInstanceState.getInt("tlist_itemChecked");
       		
       		if ((_itemChecked != ListView.INVALID_POSITION) && (_viewAvailable))
       			getListView().setItemChecked(_itemChecked, true);       		
       	}
       	else
       	{
       		// user rotated the screen, try to go back to where they where
       		if (_listState != null)
       			getListView().onRestoreInstanceState(_listState);
       		    getListView().setSelectionFromTop(_listPosition,  _itemPosition);
       		
       		if (_itemChecked != ListView.INVALID_POSITION)
       			getListView().setItemChecked(_itemChecked, true);
       	}
       	
       	
       	// pull to fresh integration
       	((MainActivity)getActivity()).getRefresher().addRefreshableView(getListView(), new PullToRefreshAttacher.OnRefreshListener(){

			@Override
			public void onRefreshStarted(View view) {
				refreshThreads();
				
			}});
       	
       	// this will also fix the ontouchlistener which was setup by the PTR
       	initAutoLoader();
       	if (savedInstanceState == null)
       		showLoadingView();

	    _lastResumeTimeAndPrompt = System.currentTimeMillis();
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
    	if (this.isVisible())
    	{
    		System.out.println("SAVE STATE TLIST");
	    	ListView listView = getListView();
	    	_listState = listView.onSaveInstanceState();
	    	_listPosition = listView.getFirstVisiblePosition();
	    	View itemView = listView.getChildAt(0);
	    	_itemPosition = itemView == null ? 0 : itemView.getTop();
	    	_itemChecked = listView.getCheckedItemPosition();
	    	
	    	outState.putParcelable("tlist_listState", _listState);
	    	outState.putInt("tlist_listPosition", _listPosition);
	    	outState.putInt("tlist_itemPosition", _itemPosition);
	    	outState.putInt("tlist_itemChecked", _itemChecked);
	    	
	    	if (_adapter != null)
	    	{
		    	outState.putParcelableArrayList("tlist_adapterItems", _adapter._itemList);
		    	outState.putInt("tlist_adapterPage", _adapter._pageNumber);
		    	outState.putIntegerArrayList("tlist_threadIds", _adapter._threadIds);
	    	}
    	}
    	super.onSaveInstanceState(outState);
    }
    public void showLoadingView()
    {
    	if (_viewAvailable)
    	{
    		if ((getActivity() != null) && ((_adapter == null) || (_adapter.getCount() == 0)))
    		{
    			MainActivity act = ((MainActivity)getActivity());
    		
				View view = null;

                act.showLoadingSplash();
				
				if ((_adapter != null) && (!_adapter.isAsyncTaskLoading()) && (!_nextBackQuitsBecauseOpenedAppViaIntent))
					_adapter.triggerLoadMore();
    		}
    	}
    }


	public void initAutoLoader ()
    {        
    	_offlineThread = ((MainActivity)getActivity()).mOffline;
    	
    	_touchListener =
                new SwipeDismissListViewTouchListener(
                        getListView(),
                        new DismissCallbacks() {
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    collapseAtPosition(position);
                                }
                                _adapter.notifyDataSetChanged();
                            }

							@Override
							public boolean canDismiss(int position) {
								return true;
							}
                        });
        getListView().setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				((MainActivity)getActivity()).getRefresher().onTouch(v, event);
				if (_swipecollapse > 0)
					_touchListener.onTouch(v, event);
				return false;
			}
		});
        
        // swipe directional pref
        if (_swipecollapse == 1)
        {
        	_touchListener.setAllowRightSwipe(false);
        }
        else
        {
        	_touchListener.setAllowRightSwipe(true);
        }
        
    	// set listview so it loads more when you hit 3/4 the way down
        getListView().setOnScrollListener(new OnScrollListener() {

			@Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				if (_adapter != null)
				{
					if (mSnackBarOpen)
					{
						if (SNKVERBOSE) System.out.println("SNK: CLOSE SCROLL");
						closeSnackBar(false);
					}
					if ((++firstVisibleItem + visibleItemCount > (int)(totalItemCount * .9)) && (!mGetAllThreadsMode)) {
						// make sure we did not open via external intent, and we are not currently loading, and last call was successful
						if (
								(!_nextBackQuitsBecauseOpenedAppViaIntent)
								&&
								(!_adapter.isAsyncTaskLoading()) 
								&&
								(_adapter.wasLastCallSuccessful())
								&&
								(!_preventAutoLoad) 
							)
						{
						    // if so, download more content
							System.out.println("THREADLISTFRAG: reached 3/4 down, loading more: " + _adapter.getCount() +"]"+ totalItemCount + (_filtering) + _adapter._pageNumber + ((_adapter._pageNumber == 0) && (totalItemCount == 0)) + ((_adapter._pageNumber > 0) && (totalItemCount > 0)));

							_silentLoad = _adapter.getCount() > 0;
							
							_adapter.triggerLoadMore();
						}
					}
				}
            }

			@Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                _touchListener.setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL && scrollState != AbsListView.OnScrollListener.SCROLL_STATE_FLING);
            }
        });
        
    }
	
    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
    	Thread t = (Thread) getListView().getItemAtPosition(position);
    	if (t != null)
    	{
    		openThreadView(position);
    	}
    }

    public void collapseAtPosition (int position)
    {
    	final Thread t = (Thread) getListView().getItemAtPosition(position);
    	if (t != null)
    	{
            statInc(getActivity(), "PostCollapsed");
			addCollapsed(t.getThreadId());
			openUndoBar(t, _adapter.getPosition(t));
			_adapter.remove(t);
			_adapter.notifyDataSetChanged();
    	}
    }



	public void openUndoBar(final Thread t, final int pos)
	{
		if (SNKVERBOSE) System.out.println("SNK: OPEN TCOLAPSE");
		openSnackBar(t.getUserName() + " Thread Collapsed", "Undo", new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				undoCollapse(t, pos);
			}
		});
	}
	
	class SnackBarQueueItem
	{
		String mPrompt;
		String mButton;
		View.OnClickListener mOnc;
		SnackBarQueueItem(String prompt, String button, View.OnClickListener onc) { mPrompt = prompt; mButton = button; mOnc = onc; }
	}

	private static final boolean SNKVERBOSE = false;
	private Handler mSnackBarCloseHandler = new Handler();
	private Runnable mSnackBarCloseRunnable = new Runnable(){
		@Override
		public void run() {
			if (SNKVERBOSE) System.out.println("SNK: CLOSE TIMEOUT");
			closeSnackBar(true);
		}};
	private Handler mSnackBarStuckHandler = new Handler();
	private Runnable mSnackBarStuckRunnable = new Runnable(){
		@Override
		public void run() {
			mSnackBarStuckOpen = false;
			if (SNKVERBOSE) System.out.println("SNK: NO LONGER STUCK");
		}};

	public void openSnackBar(SnackBarQueueItem snqi) { System.out.println("SNK: OPEN QUEUE"); openSnackBar(snqi.mPrompt, snqi.mButton, snqi.mOnc); }
    public void openSnackBar(final String promptText, final String buttonText, final View.OnClickListener onc)
    {
	    if (!mSnackBarOpen)
	    {
		    if (SNKVERBOSE) System.out.println("SNK: OPENING SET OPEN SET STUCK" + promptText);
		    mUndoBarClosing = false;
		    mSnackBarOpen = true;
		    mSnackBarStuckOpen = true;

		    // reset timers
		    if (mSnackBarCloseRunnable != null)
			    mSnackBarCloseHandler.removeCallbacks(mSnackBarCloseRunnable);
		    if (mSnackBarStuckRunnable != null)
			    mSnackBarStuckHandler.removeCallbacks(mSnackBarStuckRunnable);

		    View undoBar = getActivity().findViewById(R.id.tlist_snackBar);
		    ((TextView) getActivity().findViewById(R.id.tlist_snackBarText)).setText(promptText);
		    ((ButtonFlat) getActivity().findViewById(R.id.tlist_snackBarButton)).setText(buttonText);
		    MainActivity.anim anim = ((MainActivity) getActivity()).new anim(undoBar).toVisible();
		    getActivity().findViewById(R.id.tlist_snackBarButton).setOnClickListener(onc);

		    // make new timers
		    mSnackBarCloseHandler.postDelayed(mSnackBarCloseRunnable, 6000);
		    mSnackBarStuckHandler.postDelayed(mSnackBarStuckRunnable, 3000);
	    }
	    else
	    {
		    if (SNKVERBOSE) System.out.println("SNK: ALREADY OPEN CREATING QUEUE ITEM" + promptText);
		    mSnackBarQueue.add(0, new SnackBarQueueItem(promptText,buttonText,onc));
	    }
    }
    public void undoCollapse(Thread t, int pos)
    {
    	if (t != null)
    	{
            statInc(getActivity(), "PostUndoCollapsed");
	    	removeCollapsed(t.getThreadId());
	    	_adapter.insert(t, pos);
	    	_adapter.notifyDataSetChanged();
		    if (SNKVERBOSE) System.out.println("SNK: CLOSE BUTTON");
	    	closeSnackBar(true);
    	}
    }
    public void closeSnackBar(boolean override)
    {
	    if (SNKVERBOSE) System.out.println("SNK: CLOSE CHK ACTIVITY");
    	if (getActivity() != null)
    	{
		    if (SNKVERBOSE) System.out.println("SNK: CLOSE ACT OK" + override + mSnackBarStuckOpen + mSnackBarOpen);
		    if ((mSnackBarOpen) && ((!mSnackBarStuckOpen) || (override)))
		    {
			    if (SNKVERBOSE) System.out.println("SNK: CLOSE OK GO // NOT STUCK OR OVERRIDE");
			    final View undoBar = getActivity().findViewById(R.id.tlist_snackBar);
			    mUndoBarClosing = true;

			    final View view = getActivity().getWindow().getDecorView();
			    MainActivity.anim anim = ((MainActivity) getActivity()).new anim(undoBar).toInvisible().setEndCall(new mAnimEnd()
			    {
				    @Override
				    public void end()
				    {
					    mUndoBarClosing = false;
					    mSnackBarOpen = false;
					    if (SNKVERBOSE) System.out.println("SNK: mSBO false");
					    if (!mSnackBarQueue.isEmpty())
					    {
						    if (SNKVERBOSE) System.out.println("SNK: QUE SIZE" + mSnackBarQueue.size());
						    view.postDelayed(new Runnable()
						    {
							    @Override
							    public void run()
							    {
								    if (!mSnackBarQueue.isEmpty())
								    {
									    if (SNKVERBOSE) System.out.println("SNK: QUEUE OPEN");
									    openSnackBar(mSnackBarQueue.pop());
								    }
							    }
						    }, 300);
					    }
				    }
			    });
		    }
    	}
    }
    protected void toggleFavThread(Thread thread) {
    	if (((MainActivity)getActivity()).mOffline.toggleThread(thread.getThreadId(), thread.getPosted(), thread.getJson()))
 		{
			Toast.makeText(getActivity(), "Thread added to favorites", Toast.LENGTH_SHORT).show();
            statInc(getActivity(), "FavoritedAThread");
		}
		else
		{
			Toast.makeText(getActivity(), "Thread removed from favorites", Toast.LENGTH_SHORT).show();
		}
 		((MainActivity)getActivity()).mRefreshOfflineThreadsWoReplies();
	}
    
    

	void refreshThreads()
    {
        if (_viewAvailable) {
            _preventAutoLoad = true;
            if (_adapter != null)
                _adapter.cancel();
            _silentLoad = false;
    	
    	/*
        getListView().clearChoices();
        _adapter.clear();
        _adapter.notifyDataSetChanged();
        if (_adapter.updatePrefs())
        {
        	System.out.println("zoom or other pref changed, redraw listview");
        	getListView().invalidate();
        	_adapter.notifyDataSetChanged();
        	
        }
        
        &&
							( 
									// prevent getting more than one page at a time due to empty listview when not filtering
									(_filtering)
									||
									((_adapter._pageNumber == 0) && (totalItemCount == 0))
									||
									((_adapter._pageNumber > 0) && (totalItemCount > 0))
							)
							getListView().setOnScrollListener(null);
    	getListView().setOnTouchListener(null);
        */

            statInc(getActivity(), "RefreshedThreadList");

            MainActivity act = ((MainActivity) getActivity());
            act.showLoadingSplash();

            getView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    System.out.println("REFRESHL DOIN THINGS");
                    if (getListView() != null)
                        getListView().clearChoices();
                    if (_adapter != null) {
                        _adapter.clear();
                        _adapter.notifyDataSetChanged();
                        _adapter.triggerLoadMore();
                        _preventAutoLoad = false;
                        if (_adapter.updatePrefs()) {
                            System.out.println("zoom or other pref changed, redraw listview");
                            getListView().invalidate();
                            _adapter.notifyDataSetChanged();

                        }
                    }
                }
            }, 500);
        }
    }
    
    public static final int POST_NEW_THREAD = 430;
    public static final int OPEN_THREAD_VIEW = 1290;
    public static final int OPEN_PREFS = 2117;
    
    public void showFilters()
    {
        boolean _showInformative = _prefs.getBoolean("showInformative", true);
        boolean _showTangent = _prefs.getBoolean("showTangent", true);
        boolean _showStupid = _prefs.getBoolean("showStupid", true);
        boolean _showNWS = _prefs.getBoolean("showNWS", false);
        boolean _showPolitical = _prefs.getBoolean("showPolitical", false);
        boolean _showOntopic = _prefs.getBoolean("showOntopic", true);

        MaterialDialog.Builder build = new MaterialDialog.Builder(getActivity());
        build.title("Choose which to show");
        final String[] items = { "tangent","informative","nws","stupid","political","ontopic"};
        ArrayList<Integer> index = new ArrayList<Integer>();
        if (_showTangent) index.add(new Integer(0)); if (_showInformative) index.add(new Integer(1)); if (_showNWS) index.add(new Integer(2)); if (_showStupid) index.add(new Integer(3)); if (_showPolitical) index.add(new Integer(4)); if (_showOntopic) index.add(new Integer(5));
        final Integer[] checkedItems = index.toArray(new Integer[]{});
        // final Integer[] checkedItems = { _showTangent ? 0 : null ,_showInformative ? 1 : null,_showNWS  ? 2 : null,_showStupid ? 3 : null,_showPolitical ? 4 : null, _showOntopic ? 5 : null};
        build.items(items).itemsCallbackMultiChoice(checkedItems, new MaterialDialog.ListCallbackMulti() {
            @Override
            public void onSelection(MaterialDialog materialDialog, Integer[] integers, CharSequence[] charSequences) {
                return;
            }
        })
        .cancelable(true)
        .negativeText("Cancel").positiveText("Update Filters")
        .callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                Integer[] index = dialog.getSelectedIndices();
                ArrayList<Integer> checkedIndices = new ArrayList<Integer>(Arrays.asList(index));
                Editor edit = _prefs.edit();
                if (checkedIndices.contains(0) == true)
                    edit.putBoolean("showTangent", true);
                else
                    edit.putBoolean("showTangent", false);

                if (checkedIndices.contains(1) == true)
                    edit.putBoolean("showInformative", true);
                else
                    edit.putBoolean("showInformative", false);

                if (checkedIndices.contains(2) == true)
                    edit.putBoolean("showNWS", true);
                else
                    edit.putBoolean("showNWS", false);

                if (checkedIndices.contains(3) == true)
                    edit.putBoolean("showStupid", true);
                else
                    edit.putBoolean("showStupid", false);

                if (checkedIndices.contains(4) == true)
                    edit.putBoolean("showPolitical", true);
                else
                    edit.putBoolean("showPolitical", false);

                if (checkedIndices.contains(5) == true)
                    edit.putBoolean("showOntopic", true);
                else
                    edit.putBoolean("showOntopic", false);

                edit.commit();
                refreshThreads();
            }

            @Override
            public void onNegative(MaterialDialog dialog) {
            }
        }).show();
/*
        MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(getActivity());
        builder.setTitle("Choose which to show");
        final String[] items = { "tangent","informative","nws","stupid","political","ontopic"};
        final boolean[] checkedItems = { _showTangent,_showInformative,_showNWS,_showStupid,_showPolitical, _showOntopic};
        builder.setMultiChoiceItems(items, checkedItems, new
                DialogInterface.OnMultiChoiceClickListener()
        		{

				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					// TODO Auto-generated method stub
					
				}
		});
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				
			}});
        builder.setPositiveButton("Update Filters", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				Editor edit = _prefs.edit();
				
				ListView lw = ((AlertDialog)dialog).getListView();
				SparseBooleanArray checkedItems = lw.getCheckedItemPositions();
				
				if (checkedItems.get(0) == true)
					edit.putBoolean("showTangent", true);
				else
					edit.putBoolean("showTangent", false);
				
				if (checkedItems.get(1) == true)
					edit.putBoolean("showInformative", true);
				else
					edit.putBoolean("showInformative", false);
				
				if (checkedItems.get(2) == true)
					edit.putBoolean("showNWS", true);
				else
					edit.putBoolean("showNWS", false);
				
				if (checkedItems.get(3) == true)
					edit.putBoolean("showStupid", true);
				else
					edit.putBoolean("showStupid", false);
				
				if (checkedItems.get(4) == true)
					edit.putBoolean("showPolitical", true);
				else
					edit.putBoolean("showPolitical", false);
				
				if (checkedItems.get(5) == true)
					edit.putBoolean("showOntopic", true);
				else
					edit.putBoolean("showOntopic", false);
				
				edit.commit();
				refreshThreads();
				
			}});
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(true);
        alert.show();
        */
    }
    
    void openThreadView(int index)
    {
    	// -1 for PULL TO REFRESH INTEGRATION
        Thread thread = _adapter.getItem(index);
        
        // probably clicked the "Loading..." or something
        if (thread == null)
            return;
        
        markFavoriteAsRead(thread);
        
        _itemChecked = index -1;
        _adapter.notifyDataSetChanged();
        getListView().setItemChecked(index, true);
        
        //  because we preload the root post into the threadview and loldata is generated prior to list display, we must pass this loldata
        LolObj lol = new LolObj();
        // do we even have loldata?
        if (_adapter._shackloldata != null)
        {
        	// is this thread funny?
	        HashMap<String, LolObj> threadlols = _adapter._shackloldata.get(Integer.toString(thread.getThreadId()));
	    	if ((threadlols != null))
	    	{
	    		// get root post lol data
	    		lol = _adapter._shackloldata
	        		.get(Integer.toString(thread.getThreadId()))
	        		.get(Integer.toString(thread.getThreadId()));
	    	}
        }
        
        ((MainActivity)getActivity()).openThreadView(thread.getThreadId(), thread, lol);
    }
    
    public void markFavoriteAsRead (Thread thread)
    {
    	OfflineThread offline = ((MainActivity)getActivity()).mOffline;
        if (offline.containsThreadId(thread.getThreadId()))
        {
        	// this is a favorite, must update unread counts when we open
        	thread.setReplyCountPrevious(thread.getReplyCount());
            
            // update data for last viewed count, this creates the info for how many are unread.
        	offline.updateRecordedReplyCountPrev(thread.getThreadId(), thread.getReplyCount());
            
            // update "previous" count
        	offline.updateRecordedReplyCount(thread.getThreadId(), thread.getReplyCount());
        	offline.flushThreadsToDiskTask();
        }
    }
    
    public void markFavoriteAsRead (int threadId, int replyCount)
    {
    	if  (_adapter != null)
    	{
        	// this is a favorite, must update unread counts when we open
        	for (int i = 0; i < _adapter.getCount(); i++)
        	{
        		// find the thread we are looking for
        		if (_adapter.getItem(i).getThreadId() == threadId)
        		{
        			_adapter.getItem(i).setReplyCount(replyCount);
        			_adapter.getItem(i).setReplyCountPrevious(replyCount);
        			// done looking
        			break;
        		}
        	}
    	}
    }

    // COLLAPSED
    	 
    void addCollapsed(int threadId)
    {
    	if (!mCollapsed.contains(threadId))
        {
            mCollapsed.add(threadId);
    	}
    }
    void removeCollapsed(int threadId)
    {
    	if (mCollapsed.contains(threadId))
        {
            mCollapsed.remove(Integer.valueOf(threadId));
    	}
    }
    public void reloadCollapsed()
    {
    	mCollapsed = getCollapsed();
    }
    protected ArrayList<Integer> getCollapsed()
    {
    	String filename = COLLAPSED_CACHE_FILENAME;
    	new ArrayList<Integer>();
    	ArrayList<Integer> collapsed = new ArrayList<Integer>();

        if (getActivity().getFileStreamPath(filename).exists())
        {
            // look at that, we got a file
            try {
                FileInputStream input = getActivity().openFileInput(filename);
                try
                {
                    DataInputStream in = new DataInputStream(input);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line = reader.readLine();
                    while (line != null)
                    {
                        if (line.length() > 0)
                        {
                        	try
                        	{
                        		collapsed.add(Integer.parseInt(line));
                        	}
                        	catch (NumberFormatException e)
                        	{ System.out.println("COLLAPSED: ERROR READING "); }
                        }
                        line = reader.readLine();
                    }
                }
                finally
                {
                    input.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return collapsed;
    }
    private void storeCollapsed(ArrayList<Integer> collapsed) throws IOException
    {
    	String filename = COLLAPSED_CACHE_FILENAME;
    	List<Integer> postIds = new ArrayList<Integer>();
    	for (int i = 0; i < collapsed.size(); i++)
    	{
    		postIds.add(collapsed.get(i));
    	}
    	
    	// trim to last 1000 posts
        Collections.sort(postIds);
        if (postIds.size() > COLLAPSED_SIZE)
            postIds.subList(postIds.size() - COLLAPSED_SIZE, postIds.size() - 1);

        FileOutputStream output = getActivity().openFileOutput(filename, Activity.MODE_PRIVATE);
        try
        {
            DataOutputStream out = new DataOutputStream(output);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

            for (Integer postId : postIds)
            {
            	System.out.println("COLLAPSED: wrote "+postId);
            	writer.write(postId.toString());
                writer.newLine();
            }
            writer.flush();
        }
        finally
        {
            output.close();
        }
    }
    
    public void addFiltWord(final int type)
    {
		String thing = "Keyword";
    	if (type == 1)
    		thing = "Username";

        MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(getActivity());
    	builder.setTitle("Add Keyword Filter");
    	// Set up the input
		final EditText input = new EditText(getActivity());
		// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
		builder.setView(input);
		
    	builder.setPositiveButton("Remove Threads w/ " + thing, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Filtword fw = null;
				if (type == 1)
					fw = new Filtword(DELETE_USER_FILTWORD_FILENAME);
				else
					fw = new Filtword(DELETE_FILTWORD_FILENAME);
				
				fw.add(input.getText().toString());
				showFiltWordList();
			}
		});
    	builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showFiltWordList();
			}
		});
    	AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }
    public void removeFiltWord(final String keyword, final String filename)
    {
        MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(getActivity());
    	builder.setTitle("Remove Filter Keyword");
    	String type = null;
    	if (filename.contains(DELETE_FILTWORD_FILENAME))
	    	type = "removing threads w/ keyword";
    	else if (filename.contains(COLLAPSE_FILTWORD_FILENAME))
    		type = "doing nothing to keyword";
    	else if (filename.contains(DELETE_USER_FILTWORD_FILENAME))
	    	type = "removing threads w/ username";
    	else if (filename.contains(COLLAPSE_USER_FILTWORD_FILENAME))
    		type = "doing nothing to username";
    	
    	builder.setMessage("Stop "+type+" \""+keyword+"\"?");
		
    	builder.setPositiveButton("Remove Filter", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Filtword fw = new Filtword(filename);
				fw.remove(keyword);
				showFiltWordList();
			}
		});
    	builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showFiltWordList();
			}
		});
    	AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }
    public void showFiltWordList()
    {
        MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(getActivity());
        builder.setTitle("Keyword Filters");
        final Filtword cfwords = new Filtword(COLLAPSE_FILTWORD_FILENAME);
        final Filtword dfwords = new Filtword(DELETE_FILTWORD_FILENAME);
        final Filtword cfuwords = new Filtword(COLLAPSE_USER_FILTWORD_FILENAME);
        final Filtword dfuwords = new Filtword(DELETE_USER_FILTWORD_FILENAME);
        final ArrayList<CharSequence> list = new ArrayList<CharSequence>();
        list.addAll(cfwords.getWithPrefix());
        list.addAll(dfwords.getWithPrefix());
        list.addAll(cfuwords.getWithPrefix());
        list.addAll(dfuwords.getWithPrefix());
        final ArrayList<CharSequence> cleanlist = new ArrayList<CharSequence>();
        cleanlist.addAll(cfwords.get());
        cleanlist.addAll(dfwords.get());
        cleanlist.addAll(cfuwords.get());
        cleanlist.addAll(dfuwords.get());
        final CharSequence[] items = list.toArray(new CharSequence[list.size()]);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
            	if (item >= (cfwords.get().size() + dfwords.get().size() + cfuwords.get().size()))
            		removeFiltWord(cleanlist.get(item).toString(), DELETE_USER_FILTWORD_FILENAME);
            	else if (item >= (cfwords.get().size() + dfwords.get().size()))
            		removeFiltWord(cleanlist.get(item).toString(), COLLAPSE_USER_FILTWORD_FILENAME);
            	else if (item >= (cfwords.get().size()))
            		removeFiltWord(cleanlist.get(item).toString(), DELETE_FILTWORD_FILENAME);
            	else
            		removeFiltWord(cleanlist.get(item).toString(), COLLAPSE_FILTWORD_FILENAME);
                }});
        builder.setNegativeButton("Close", null);
        builder.setNeutralButton("Add Username", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				addFiltWord(1);
			}
		});
        builder.setPositiveButton("Add Keyword", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				addFiltWord(0);
			}
		});
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }
	// FILTERABLE KEYWORDS
    class Filtword {
    	private String _filename;
    	public Filtword (String filename)
    	{
    		_filename = filename;
    	}
	    void remove(String keyword)
	    {
	    	// update storage
	    	ArrayList<String> filtwords = get();
	    	filtwords.remove(keyword);
	    	
	    	try { store(filtwords); } catch (IOException e) { e.printStackTrace(); }
	    }
	    void add(String keyword)
	    {
	    	// update storage
	    	ArrayList<String> filtwords = get();
	    	if (!filtwords.contains(keyword))
	            filtwords.add(keyword);
	    	
	    	try { store(filtwords); } catch (IOException e) { e.printStackTrace(); }
	    }
	    ArrayList<Thread> update(ArrayList<Thread> threads)
	    {
	        // set the number of replies that are new
	    	ArrayList<String> collapsefws = get(COLLAPSE_FILTWORD_FILENAME, false);
	    	ArrayList<String> deletefws = get(DELETE_FILTWORD_FILENAME, false);
	    	ArrayList<String> collapseufws = get(COLLAPSE_USER_FILTWORD_FILENAME, false);
	    	ArrayList<String> deleteufws = get(DELETE_USER_FILTWORD_FILENAME, false);
	    	
	        // actually mark as read here
	    	Iterator<Thread> iter = threads.iterator();
	    	for (int i = 0; i < threads.size(); i++)
	    	{
	    		Thread t = iter.next();
	    		for (int j = 0; j < deletefws.size(); j++)
	    		{
	    			if (t.getFilterable().toLowerCase().contains(deletefws.get(j).toLowerCase()))
	    			{
	    				System.out.println("DELETING: " + deletefws.get(j).toLowerCase());
	    				// delete thread

	    				iter.remove();
                        statInc(getActivity(), "CollapsedDueToKeywordFilter");
	    				break;
	    			}
	    		}
	    		for (int j = 0; j < deleteufws.size(); j++)
	    		{
	    			if (t.getUserName().toLowerCase().contains(deleteufws.get(j).toLowerCase()))
	    			{
	    				// delete thread
	    				System.out.println("DELETING U: " + deleteufws.get(j).toLowerCase());
	    				iter.remove();
                        statInc(getActivity(), "CollapsedDueToUsernameFilter");
	    				break;
	    			}
	    		}
	    	}
			return threads;
	    }
	    protected ArrayList<String> get()
	    {
	    	return get(_filename, false);
	    }
	    protected ArrayList<String> getWithPrefix()
	    {
	    	return get(_filename, true);
	    }
	    protected ArrayList<String> get(String filename, boolean withPrefix)
	    {
	    	ArrayList<String> fwlist = new ArrayList<String>();
	
	        if (getActivity().getFileStreamPath(filename).exists())
	        {
	            try {
	                FileInputStream input = getActivity().openFileInput(filename);
	                try
	                {
	                    DataInputStream in = new DataInputStream(input); BufferedReader reader = new BufferedReader(new InputStreamReader(in)); String line = reader.readLine();
	                    while (line != null) {
	                    	if (line.length() > 0) {
	                    		if (withPrefix)
	                    		{
	                    			String prefix = null;
	                    			if (filename.equals(COLLAPSE_FILTWORD_FILENAME))
	                    				prefix = "DEPRECATED (does nothing): ";
	                    			else if (filename.equals(DELETE_FILTWORD_FILENAME))
	                    				prefix = "collapse: ";
	                    			else if (filename.equals(COLLAPSE_USER_FILTWORD_FILENAME))
	                    				prefix = "DEPRECATED (does nothing): ";
	                    			else if (filename.equals(DELETE_USER_FILTWORD_FILENAME))
	                    				prefix = "collapse user: ";
	                    			fwlist.add(prefix + line);
	                    		}
	                    		else
	                    			fwlist.add(line);
	                        }
	                        line = reader.readLine();
	                    }
	                }
	                finally { input.close(); }
	            } catch (IOException e) { e.printStackTrace(); }
	        }
	        return fwlist;
	    }
	    private void store(ArrayList<String> fwlist) throws IOException
	    {
	        FileOutputStream output = getActivity().openFileOutput(_filename, Activity.MODE_PRIVATE);
	        try {
	        	DataOutputStream out = new DataOutputStream(output); BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
	            for (String filtword : fwlist)
	            {
	            	writer.write(filtword);
	                writer.newLine();
	            }
	            writer.flush();
	        }
	        finally { output.close(); }
	    }
    }
    
    // POST COUNTS
    void updatePostCounts(ArrayList<Thread> threads)
    {
        // set the number of replies that are new
        Hashtable<Integer, Integer> counts = getPostCounts();
        // actually mark as read here
        for (Thread t : threads)
        {
            if (counts.containsKey(t.getThreadId()))
            {
                t.setReplyCountPrevious(counts.get(t.getThreadId()));
            }
        }

        // store all post counts
        try
        {
        	// storing postid=getreplycount
            storePostCounts(counts, threads);
        } catch (IOException e)
        {
            // yeah, who cares
            Log.e("ThreadView", "Error storing post counts.", e);
        }
    }
    protected Hashtable<Integer, Integer> getPostCounts()
    {
        Hashtable<Integer, Integer> counts = new Hashtable<Integer, Integer>();

        if (getActivity() != null && getActivity().getFileStreamPath(POST_COUNT_CACHE_FILENAME) != null && getActivity().getFileStreamPath(POST_COUNT_CACHE_FILENAME).exists())
        {
            // look at that, we got a file
            try {
                FileInputStream input = getActivity().openFileInput(POST_COUNT_CACHE_FILENAME);
                try
                {
                    DataInputStream in = new DataInputStream(input);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line = reader.readLine();
                    while (line != null)
                    {
                        if (line.length() > 0)
                        {
                        	if (line.contains("="))
                        	{
	                            String[] parts = line.split("=");
	                            if (parts.length > 0)
	                            {
	                            	try
	                            	{
	                            		counts.put(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
	                            	}
	                            	catch (NumberFormatException e)
	                            	{ }
	                            }
                        	}
                        }
                        line = reader.readLine();
                    }
                }
                finally
                {
                    input.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return counts;
    }
    private void storePostCounts(Hashtable<Integer, Integer> counts, ArrayList<Thread> threads) throws IOException
    {
    	if (getActivity() != null)
    	{
	        // update post counts for threads viewing right now
	        for (Thread t : threads)
	            counts.put(t.getThreadId(), t.getReplyCount());
	
	        List<Integer> postIds = Collections.list(counts.keys());
	        Collections.sort(postIds);
	        
	
	        // trim to last 1000 posts
	        if (postIds.size() > POST_CACHE_HISTORY)
	            postIds.subList(postIds.size() - POST_CACHE_HISTORY, postIds.size() - 1);
	
	        FileOutputStream output = getActivity().openFileOutput(POST_COUNT_CACHE_FILENAME, Activity.MODE_PRIVATE);
	        try
	        {
	            DataOutputStream out = new DataOutputStream(output);
	            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
	
	            for (Integer postId : postIds)
	            {
	            	writer.write(postId + "=" + counts.get(postId));
	                writer.newLine();
	            }
	            writer.flush();
	        }
	        finally
	        {
	            output.close();
	        }
    	}
    }
    
    public class ThreadLoadingAdapter extends LoadingAdapter<Thread> implements Filterable
    {
    	protected ArrayList<Integer> _threadIds = new ArrayList<Integer>();
    	protected HashMap<String, HashMap<String, LolObj>> _shackloldata = new HashMap<String, HashMap<String, LolObj>>();
        protected int _pageNumber = 0;
        private Boolean _showShackTags;
        private Boolean _showModTags;
        private Boolean _stripNewLines;
        private int _previewLines;
        
        boolean _getLols = true;
        boolean _lolsContained = false;
        private String _userName = "";
        float _zoom = 1.0f;
        boolean _showOntopic = true;
		private boolean _showHoursSince = true;
		private boolean _showPinnedInTL = true;
		private Bitmap _favBMP;
		private Bitmap _unfavBMP;
		
		private Filter mFilter;
		private ArrayList<Thread> _filteredItemList = new ArrayList<Thread>();
		
	    @Override
	    public int getCount() {
	    	synchronized (mLock)
        	{
		    	if ((!_filtering) || (_filteredItemList == null))
		    		return _itemList.size();
		        return _filteredItemList.size();
        	}
	    }
	    @Override
	    public Thread getItem(int position) {
	    	synchronized (mLock)
        	{
		    	if ((!_filtering) || (_filteredItemList == null))
		    		return _itemList.get(position);
		        return _filteredItemList.get(position);
        	}
	    }
	    @Override
	    public int getPosition(Thread item) {
	    	if ((!_filtering) || (_filteredItemList == null))
	    		return _itemList.indexOf(item);
	        return _filteredItemList.indexOf(item);
	    }
	    @Override
	    public long getItemId(int position) {
	        return position;
	    }
        @Override
        public void remove(Thread item)
        {
	    	_itemList.remove(item);
        }
        
        @Override
        public void insert(Thread t, int index)
        {
        	_itemList.add(index, t);
        }
        
        public ThreadLoadingAdapter(Context context, ArrayList<Thread> items)
        {
            super(context, items);
            setShowTags();
            
            // prefs
            _zoom = Float.parseFloat(_prefs.getString("fontZoom", "1.0"));
            _lolsContained = _prefs.getBoolean("showThreadLolsThreadList", true);
            _getLols = _prefs.getBoolean("getLols", true);
            _showOntopic = _prefs.getBoolean("showOntopic", true);
            _showHoursSince = _prefs.getBoolean("showHoursSince", true);
            _userName = _prefs.getString("userName", "").trim();
            _showPinnedInTL = _prefs.getBoolean("showPinnedInTL", true);
            _swipecollapse = Integer.parseInt(_prefs.getString("swipeCollapse", "2"));
            
            // this is an optimization. using alpha is bad
            _favBMP = imgDecolor(R.drawable.ic_toggle_star, 0xFF777777);
            _unfavBMP = imgDecolor(R.drawable.ic_toggle_star_outline, 0xFF222222);
            
            // collapse button no longer used
            // _collapseBMP = imgDecolor(R.drawable.content_remove, 0xFF222222);
            
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {       
            if (convertView == null)
                convertView = _inflater.inflate(R.layout.row, null);
            
            return createView(position, convertView, parent);
        }
        
        public Bitmap imgDecolor(int id, int mask)
        {
        	Bitmap sourceBitmap = BitmapFactory.decodeResource(getResources(), id);
	        // 0 x ARGB
	        // 0xFF999999
	        LightingColorFilter colorFilter = new LightingColorFilter(mask, 0xFF101010);
	        Paint paint = new Paint();
	        paint.setColorFilter(colorFilter);
	        Bitmap resultBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true);        
	
	        Canvas canvas = new Canvas(resultBitmap);
	        canvas.drawBitmap(resultBitmap, 0, 0, paint);
	        return resultBitmap;
        }
        
        @Override
        public void clear()
        {
            _pageNumber = 0;
            _threadIds.clear();
            setShowTags();
            super.clear();
            
            System.out.println("TLIST REFRESH " + _adapter.wasLastCallSuccessful() + _adapter.isCurrentlyLoading());
        }
        
        @Override
        public void setCurrentlyLoading (final boolean set)
        {
        	super.setCurrentlyLoading(set);
        	if (getActivity() != null)
        	{
        		final MainActivity act = (MainActivity) getActivity();
	        	if (set)
	        	{
	        		act.runOnUiThread(new Runnable(){
	            		@Override public void run()
	            		{
	            			if (_viewAvailable)
	            			{
	            				act.showOnlyProgressBarFromPTRLibrary(set);
		        			}
	            		}
	            	});
	        	}
	        	else
	        	{
	        		act.runOnUiThread(new Runnable(){
	            		@Override public void run()
	            		{
	            			if (_viewAvailable)
	            			{
	            				act.showOnlyProgressBarFromPTRLibrary(set);
	            			}
	            		}
	            	});
	        	}
        	}
        }
        
        public boolean updatePrefs()
        {
        	// prefs
        	boolean changed = false;
        	
            if (_zoom != Float.parseFloat(_prefs.getString("fontZoom", "1.0")))
            {
            	_zoom = Float.parseFloat(_prefs.getString("fontZoom", "1.0"));
            	changed = true;
            }
            if (_swipecollapse != Integer.parseInt(_prefs.getString("swipeCollapse", "2")))
            {
            	_swipecollapse = Integer.parseInt(_prefs.getString("swipeCollapse", "2"));
            	initAutoLoader();
            	changed = true;
            }
            if (_lolsContained != _prefs.getBoolean("showThreadLolsThreadList", true))
            {
            	_lolsContained = _prefs.getBoolean("showThreadLolsThreadList", true);
            	changed = true;
            }
            if (_getLols != _prefs.getBoolean("getLols", true))
            {
            	_getLols = _prefs.getBoolean("getLols", true);
            	changed = true;
            }
            if (_showOntopic != _prefs.getBoolean("showOntopic", true))
            {
            	_showOntopic = _prefs.getBoolean("showOntopic", true);
            	changed = true;
            }
            if (_showHoursSince != _prefs.getBoolean("showHoursSince", true))
            {
	            _showHoursSince = _prefs.getBoolean("showHoursSince", true);
	        	changed = true;
            }
            if (!_userName.equals(_prefs.getString("userName", "").trim()))
            {
            	_userName = _prefs.getString("userName", "").trim();
            	changed = true;
            }
            if (_showPinnedInTL != _prefs.getBoolean("showPinnedInTL", true))
            {
            	_showPinnedInTL = _prefs.getBoolean("showPinnedInTL", true);
            	changed = true;
            }
	        if (mGetAllThreadsMode != _prefs.getBoolean("getAllThreadsMode", false))
	        {
		        mGetAllThreadsMode = _prefs.getBoolean("getAllThreadsMode", false);
		        changed = true;
	        }
	        if (mSortMode != _prefs.getString("getSortMode", "usual"))
	        {
	            mSortMode = _prefs.getString("getSortMode", "usual");
		        changed = true;
	        }

            return changed;
        }
        void setShowTags()
        {
            Activity activity = ThreadListFragment.this.getActivity();
            if (activity != null)
            {
                _showModTags = _prefs.getBoolean("showModTagsInThreadList", true);
                _showShackTags = _prefs.getBoolean("showShackTagsInThreadList", true);
                _stripNewLines = _prefs.getBoolean("previewStripNewLines", false);
                _previewLines = Integer.parseInt(_prefs.getString("previewLineCount", "7"));
            }
        }
        
        @Override
        protected View createView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder holder = (ViewHolder)convertView.getTag();
            if (holder == null)
            {
                holder = new ViewHolder();
                holder.container = (CheckableLinearLayout)convertView.findViewById(R.id.threadlistContainer);
                holder.moderation = (TextView)convertView.findViewById(R.id.threadModeration);
                holder.userName = (TextView)convertView.findViewById(R.id.textUserName);
                holder.content = (TextView)convertView.findViewById(R.id.textContent);
                holder.posted = (TextView)convertView.findViewById(R.id.textPostedTime);
                holder.replyCount = (TextView)convertView.findViewById(R.id.textReplyCount);
                holder.defaultTimeColor = holder.posted.getTextColors().getDefaultColor();
                holder.fav = (ImageButton)convertView.findViewById(R.id.tlist_favorite);
                // holder.collapse = (ImageButton)convertView.findViewById(R.id.tlist_collapse);
                
                holder.firstRow = (LinearLayout)convertView.findViewById(R.id.firstRow);
                
                
                holder.containsLols = (TextView)convertView.findViewById(R.id.textContainsLols);
                
               
                // zoom
                //holder.moderation.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.moderation.getTextSize() * _zoom);
                holder.content.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.content.getTextSize() * _zoom);
                holder.userName.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.userName.getTextSize() * _zoom);
                holder.posted.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.posted.getTextSize() * _zoom);
                holder.replyCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.replyCount.getTextSize() * _zoom);
                holder.containsLols.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.containsLols.getTextSize() * _zoom);
                
                // fav button zoom
                LayoutParams favparams = holder.fav.getLayoutParams();
                favparams.height = (int) (favparams.height *  _zoom);
                favparams.width = (int) (favparams.width *  _zoom);
                holder.fav.setLayoutParams(favparams);
                
                /*
                favparams = holder.collapse.getLayoutParams();
                favparams.height = (int) (favparams.height *  _zoom);
                favparams.width = (int) (favparams.width *  _zoom);
                holder.collapse.setLayoutParams(favparams);
                holder.collapse.setImageBitmap(_collapseBMP);
                */
                
                // make room for incresed fav  button size
                favparams = holder.content.getLayoutParams();
                holder.content.setPadding(holder.content.getPaddingLeft(), holder.content.getPaddingTop(), holder.content.getPaddingRight(), (int) (holder.content.getPaddingBottom() * _zoom * 1.1f));
                holder.firstRow.setPadding(holder.firstRow.getPaddingLeft(), holder.firstRow.getPaddingTop(), (int) (holder.firstRow.getPaddingRight() * _zoom * 1.1f), holder.firstRow.getPaddingBottom());
                
                
                convertView.setTag(holder);
            }
            
            
            // get the thread to display and populate all the data into the layout
            Thread t = getItem(position);
    
        	// buttons
        	if (((MainActivity)getActivity()).mOffline.containsThreadId(t.getThreadId()))
        		holder.fav.setImageBitmap(_favBMP);
            else
            	holder.fav.setImageBitmap(_unfavBMP);
            
        	final Thread finT = t;
            final ImageButton fav = holder.fav;
            fav.setVisibility(View.VISIBLE);
            holder.fav.setOnClickListener(new View.OnClickListener(){
            	@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					toggleFavThread(finT);
					if (((MainActivity)getActivity()).mOffline.containsThreadId(finT.getThreadId()))
						fav.setImageBitmap(_favBMP);
		            else
		            	fav.setImageBitmap(_unfavBMP);
				}});
            /*
            holder.collapse.setVisibility(View.VISIBLE);
            holder.collapse.setOnClickListener(new View.OnClickListener(){
            	@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					toggleCollapsed(finT);
					_adapter.notifyDataSetChanged();
				}});
            */
            
        	// mod tags
            if (_showModTags)
            {
            	holder.moderation.setVisibility(View.VISIBLE);
            	holder.container.setModTagsFalse();
	            	
	            if (t.getModeration().equalsIgnoreCase("nws"))
	            {
	            	holder.container.setNWS(true);
	                holder.moderation.setTextColor(getResources().getColor(R.color.modtag_nws));
	                holder.moderation.setText("nws");
	            }
	            else if (t.getModeration().equalsIgnoreCase("offtopic"))
	            {
	            	holder.container.setTangent(true);
	                holder.moderation.setTextColor(getResources().getColor(R.color.modtag_tangent));
	                holder.moderation.setText("tangent");
	            }
	            else if (t.getModeration().equalsIgnoreCase("informative"))
	            {
	            	holder.container.setInf(true);
	                holder.moderation.setTextColor(getResources().getColor(R.color.modtag_inf));
	                holder.moderation.setText("informative");
	            }
	            else if (t.getModeration().equalsIgnoreCase("stupid"))
	            {
	            	holder.container.setStupid(true);
	                holder.moderation.setTextColor(getResources().getColor(R.color.modtag_stupid));
	                holder.moderation.setText("stupid");
	            }
	            else if (t.getModeration().equalsIgnoreCase("political"))
	            {
	            	holder.container.setPolitical(true);
	                holder.moderation.setTextColor(getResources().getColor(R.color.modtag_political));
	                holder.moderation.setText("political");
	            }
	            else
	            {
	            	holder.container.refreshDrawableState(); // needed because setmodtagsfalse does not do this
	                holder.moderation.setVisibility(View.GONE);
	            }
            }
            else
            {
                holder.moderation.setVisibility(View.GONE);
            }
            
            // highlight for filter text
        	if ((((ThreadFilter)getFilter()).lastFilterString != null) && (((ThreadFilter)getFilter()).lastFilterString.length() > 0))
        	{
        		Spannable text = t.getPreview(_showShackTags, _stripNewLines);
        		String txtplain = text.toString().toLowerCase();
        		int color = getResources().getColor(R.color.modtag_political);
        		Spannable highlighted = new SpannableString(text);
        		int startSpan = 0, endSpan = 0;
        		String target = ((ThreadFilter)getFilter()).lastFilterString;
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
        		holder.content.setText(highlighted);
        	}
        	else
        	{
        		holder.content.setText(t.getPreview(_showShackTags, _stripNewLines));
        	}
            holder.content.setTextColor(getResources().getColor(R.color.nonpreview_post_text_color));
            

            holder.content.setLinkTextColor(getResources().getColor(R.color.linkColor));
            holder.content.setMaxLines(_previewLines);
            //holder.content.setEllipsize(TextUtils.TruncateAt.END);
            
            final double threadAge = TimeDisplay.threadAgeInHours(t.getPosted());
            if (threadAge > 16f)
            {
            	holder.posted.setTextColor(getResources().getColor(R.color.threadLifeBad));
            }
            else if (threadAge > 12f)
            {
                holder.posted.setTextColor(getResources().getColor(R.color.threadLifeOk));
            }
            else
            {
                holder.posted.setTextColor(getResources().getColor(R.color.threadLifeGood));
            }

            holder.posted.setText(TimeDisplay.getNiceTimeSince(t.getPosted(), _showHoursSince));
            /*
            // threadage > 8760 == one year. optimization to prevent getyear from being run on every thread
            if (threadAgeInHours > 8760f && !TimeDisplay.getYear(TimeDisplay.now()).equals(TimeDisplay.getYear(t.getPosted())))
        		holder.posted.setText(TimeDisplay.convTime(t.getPosted(), "MMM dd, yyyy h:mma zzz"));
        	else
        	{
	            if ((!_showHoursSince) || (threadAgeInHours > 24f))
	            {
	            	if (threadAgeInHours > 96f)
	            		holder.posted.setText(TimeDisplay.convertTimeLong(t.getPosted()));
	            	else
	            		holder.posted.setText(TimeDisplay.convertTime(t.getPosted()));
	            }
	            else
	            	holder.posted.setText(TimeDisplay.doubleThreadAgeToString(threadAgeInHours));
        	}
        	*/
            
            // reply count formatting
            holder.replyCount.setText(formatReplyCount(t));
            
            // reset these
            holder.containsLols.setText("");
            holder.containsLols.setVisibility(View.GONE);
            
            // get tags contained in thread
            // optimization
            
            if ((_lolsContained) && (_getLols))
            {
            	LolObj lolobj = null;
	            if (t.getLolObj() != null)
	            {
		            lolobj = t.getLolObj();
	            }
	            else
	            {
		            HashMap<String, LolObj> threadlols = this._shackloldata.get(Integer.toString(t.getThreadId()));
		        	if (threadlols != null)
		        	{
		        		System.out.println("THREADLISTFRAG: HAD TO GET LOLOBJ THE SLOW WAY");
		        		lolobj = this._shackloldata
		            		.get(Integer.toString(t.getThreadId()))
		            		.get("totalLols");
		        	}
	            }
	            
	            if (lolobj != null)
	            {
	            	/*
	            	 *  when restoring Thread from a serialized state after activity is restored from a parcel, 
	            	 *  lolobj tagspans are not regenerated as the lolobj must be recreated from a serialized 
	            	 *  string in a contextless environment. Therefore it might need to be generated.
	            	 */
	            	if (lolobj.getTagSpan() == null)
	            	{
	            		System.out.println("THREADLISTFRAG: HAD TO GENTAGSPAN LOLOBJ THE SLOW WAY");
		        		
	            		lolobj.genTagSpan(getActivity());
	            	}
	            	
	            	holder.containsLols.setText(lolobj.getTagSpan()); holder.containsLols.setVisibility(View.VISIBLE);
		        }
            }
        	
        	// collapsed posts dont get a bg
            // special highlight for shacknews posts, hopefully the thread_selector color thing will
            // reset the background to transparent when scrolling
            if (t.getUserName().equalsIgnoreCase("Shacknews"))
                convertView.setBackgroundResource(R.color.selector_snthreadrow);
            else
                convertView.setBackgroundResource(R.color.selector_threadrow);
            
            
            // USERNAME
            holder.userName.setText(t.getUserName());

            // special highlight for employee and mod names
            if (t.getUserName().equalsIgnoreCase(_userName))
            {
                // highlight your own posts
                holder.userName.setTextColor(getResources().getColor(R.color.selfUserName));
            }
    		else if (User.isEmployee(t.getUserName()))
    		{
    			holder.userName.setTextColor(getResources().getColor(R.color.emplUserName));
    		}
    		else if (User.isModerator(t.getUserName()))
    		{
    			holder.userName.setTextColor(getResources().getColor(R.color.modUserName));
    		}
    		else
    		{
    			holder.userName.setTextColor(getResources().getColor(R.color.userName));
    		}
            
            return convertView;
        }
        
        public Spanned formatReplyCount(Thread thread)
        { return formatReplyCount(thread, true); }
        public Spanned formatReplyCount(Thread thread, boolean showNew)
        {
        	// the -1s in this change reply count from counting all posts to not counting root post as a reply
            String first = " " + (thread.getReplyCount() -1) + " ";
            String second = "";

            if (getNewReplies(thread) > 0)
            {
            	if (thread.getReplyCount() == getNewReplies(thread))
            	{
            		second = " new ";
            		if (thread.getPinned())
            			second = " cloud ";
            	}
            	else
            	{
            		second = " +" + getNewReplies(thread) + " ";
            		if (thread.getPinned())
            			second = " " + getNewReplies(thread) + " unread ";
            	}
            }
            if (thread.getReplied())
            {
            	first = first + "\u2022";
            }
            
            SpannableString formatted = null;
            if (showNew)
            	formatted = new SpannableString(first + second);
            else
            	formatted = new SpannableString(first);
            
            int replyBgColor = R.color.replyCountBg;
            // change color for user participated
            if (thread.getReplied())
            {
            	replyBgColor = R.color.user_paricipated;
                formatted.setSpan(new StyleSpan(Typeface.BOLD), 0, first.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            formatted.setSpan(new ForegroundColorSpan(getResources().getColor(replyBgColor)), 0, first.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            if ((second.length() > 0) && (showNew))
                formatted.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.replyCountNewBg)), first.length(), first.length() + second.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            return formatted;
        }
        
        protected int getNewReplies(Thread thread)
        {
        	if (thread.getReplyCount() > thread.getReplyCountPrevious())
            {
                return (thread.getReplyCount() - thread.getReplyCountPrevious());
            }
        	else return 0;
        }
        
        boolean _wasLastThreadGetSuccessful = false;
		public final Object mLock = new Object();
		
		public void silentUpdatePinned() {
			System.out.println("THREADLIST: UPDATING OT THREADS SILENTLY");
			if ((_showPinnedInTL) && (_adapter != null))
			{
				
				ArrayList<Thread> ot_threads = null;
				try {
					ot_threads = ShackApi.processThreads(_offlineThread.getThreadsAsJson(true, true), true, getActivity());
					
					// replace cloud data
					for (int i = 0; i < _adapter.getCount(); i++)
					{
						Thread nt = _adapter.getItem(i);
						// cloud data which has not been loaded is tagged with a date of 10L
						if ((nt != null) && (nt.getPosted() == 10L))
						{
							Iterator<Thread> iter = ot_threads.iterator();
							while (iter.hasNext())
							{
								Thread ot = iter.next();
								if (ot.getThreadId() == nt.getThreadId())
								{
									nt = ot;
								}
							}
						}
						// prevent iterating whole list
						if (!nt.getPinned())
						{
							break;
						}
					}
					
					_adapter.notifyDataSetChanged();
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
        public ArrayList<Thread> getThreadData() throws IOException, JSONException
        {
        	String userName = _prefs.getString("userName", "");

	        JSONObject json;
	        if (mGetAllThreadsMode)
		        json = ShackApi.getAllThreads(userName, this.getContext());
	        else
                json = ShackApi.getThreads(_pageNumber + 1, userName, this.getContext(), _prefs.getBoolean("useTurboAPI", true));
        	
            // winchatty uses "rootPosts" instead of "comments" // get array of threads
            boolean is_winchatty = json.has("rootPosts");
            JSONArray comments = json.getJSONArray(is_winchatty ? "rootPosts" : "comments");
            _wasLastThreadGetSuccessful = comments.length() > 0;
	        _lastThreadGetTime = System.currentTimeMillis();

            // process these threads and remove collapsed
            ArrayList<Thread> new_threads = ShackApi.processThreads(json, false, mCollapsed, getActivity());

	        mSortMode = "usual";
	        // sort fresh threads
	        if (mSortMode != "usual")
	        {
		        Collections.sort(new_threads, new Comparator<Thread>()
		        {
			        @Override
			        public int compare(Thread t1, Thread t2)
			        {
				        if (mSortMode == "top")
				        {
					        if (t1.getReplyCount() > t2.getReplyCount())
					        {
						        return -1;
					        }
					        else if (t1.getReplyCount() == t2.getReplyCount())
					        {
						        return 0;
					        }
					        return 1;
				        }
				        else // hot
				        {
					        if ((long)((t1.getReplyCount() / TimeDisplay.threadAgeInHours(t1.getPosted())) * 100f) > (long)((t2.getReplyCount() / TimeDisplay.threadAgeInHours(t2.getPosted())) * 100f))
					        {
						        return -1;
					        }
					        else if ((long)((t1.getReplyCount() / TimeDisplay.threadAgeInHours(t1.getPosted())) * 100f) == (long)((t2.getReplyCount() / TimeDisplay.threadAgeInHours(t2.getPosted())) * 100f))
					        {
						        return 0;
					        }
					        return 1;
				        }
			        }
		        });
	        }

        	try
        	{
        		if (_getLols)
        			_shackloldata = ShackApi.getLols(getActivity());
        	}
        	catch (Exception e)
        	{
        		getActivity().runOnUiThread(new Runnable(){
            		@Override public void run()
            		{
        		ErrorDialog.display(getActivity(), "Error", "ShackLOL Related error");
            		}
        		});
        	}
             
             
             // update the "new" post counts
             updatePostCounts(new_threads);
             
             // filter keywords
             Filtword fw = new Filtword(null);
             fw.update(new_threads);
             
             // offlinethreads at top
             if (_showPinnedInTL && _pageNumber == 0)
             {
            	ArrayList<Thread> ot_threads = null;
            	Iterator<Thread> iter = null;
				try
				{
					ot_threads = ShackApi.processThreadsAndUpdReplyCounts(_offlineThread.getThreadsAsJson(true, _prefs.getBoolean("onlyFavoritesFromLast18Hours", true)), getActivity());
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
				if (ot_threads != null)
				{
					// update all of the favorites in their file
					iter = ot_threads.iterator();
					while (iter.hasNext())
					{
						Thread t = iter.next();
						_offlineThread.updateRecordedReplyCount(t.getThreadId(), t.getReplyCount());
						// add any threadIds that dont exist
						if (!_threadIds.contains(t.getThreadId()))
							_threadIds.add(t.getThreadId());
					}	
					_offlineThread.flushThreadsToDiskTask();
				}
				
				// remove threads that are pinned or already displayed
	            iter = new_threads.iterator();
	            while (iter.hasNext())
				{
					int threadToCheck = iter.next().getThreadId();
					if (_threadIds.contains(threadToCheck))
						iter.remove();
					else
						_threadIds.add(threadToCheck);
				}
	             
	            if (ot_threads != null)
				{
		            // add threads to top
		            ListIterator<Thread> iter2 = ot_threads.listIterator(ot_threads.size());
					while (iter2.hasPrevious())
						new_threads.add(0, iter2.previous());
				}
             }
             else
             {
				// remove threads already displayed
				Iterator<Thread> iter = new_threads.iterator();
				while (iter.hasNext())
				{
					int threadToCheck = iter.next().getThreadId();
					if (_threadIds.contains(threadToCheck))
						iter.remove();
					else
						_threadIds.add(threadToCheck);
				}
             }
             
             _pageNumber++;
             
             
             // optimization
             for (Thread t: new_threads)
             {
            	// load lols
            	HashMap<String, LolObj> threadlols = this._shackloldata.get(Integer.toString(t.getThreadId()));
             	if ((threadlols != null) && (_lolsContained) && (_getLols))
             	{
             		LolObj lolobj = this._shackloldata
                 		.get(Integer.toString(t.getThreadId()))
                 		.get("totalLols");
             		if (lolobj != null) { lolobj.genTagSpan(getActivity()); t.setLolObj(lolobj); }
             	}
	            	
        	 	t.getPreview(_showShackTags, _stripNewLines);
             }
             
             return new_threads;
        }
        
        @Override
        protected ArrayList<Thread> loadData() throws Exception
        {
            // grab threads from the api
            return getThreadData();
        }
        
        @SuppressLint("NewApi")
		@Override
        protected void afterDisplay()
        {
        	// pull to refresh integration
        	if (getActivity() != null)
        		((MainActivity)getActivity()).getRefresher().setRefreshComplete();
        	
        	if (_viewAvailable)
        		getFilter().filter(((ThreadFilter)getFilter()).lastFilterString);
        	
        	setLastCallSuccessful(_wasLastThreadGetSuccessful);
        	
        	// all prefs are on, this is not a "next page" load, and the view is available
        	/*
        	 * SCROLLPASTPINNED PREFERENCE IS DEPRECATED
        	 */
        	/*
        	if ((_showPinnedInTL) && (_scrollPastPinned) && (_viewAvailable) && ((!_silentLoad) || (_silentLoad)))
            {
        		boolean prevWasPinned = false;
        		// start at 1 due to PULL TO REFRESH INTEGRATION
        		for (int i = 0; i < getListView().getCount(); i++)
        		{
        			Thread t = ((Thread)getListView().getItemAtPosition(i));
        			if (t != null)
        			{
	        			if (t.getPinned())
	        			{
	        				prevWasPinned = true;
	        			}
	        			else if (prevWasPinned == true)
	        			{
	        				getListView().setSelection(i);
	    			        break;
	        			}
        			}
        		}
            }
        	*/
        	if ((!_silentLoad) && (getActivity() != null))
        	{

        		MainActivity act = ((MainActivity)getActivity());
                act.hideLoadingSplash();
                /*
        		View view = null;
				if (((View)ThreadListFragment.this.getView() != null) && (((View)ThreadListFragment.this.getView()).findViewById(R.id.tlist_FSLoad).getVisibility() == View.VISIBLE))
				{
					view = ((View)ThreadListFragment.this.getView()).findViewById(R.id.tlist_FSLoad);
					if (view != null)
						act.new anim(view).toInvisible();
				}
				if (((View)ThreadListFragment.this.getView() != null) && (((View)ThreadListFragment.this.getView()).findViewById(R.id.tlist_FSLoadSplash).getVisibility() == View.VISIBLE))
				{
					view = ((View)ThreadListFragment.this.getView()).findViewById(R.id.tlist_FSLoadSplash);
					if (view != null)
						act.new anim(view).toInvisible();
				}
				
				act.new anim(getListView()).toVisible();
				*/
				/* getListView().post(new Runnable() {
                    @Override
                    public void run() {
                        getListView().setSelection(0);
                    }
                });
*/
                System.out.println("TLIST: SETSEL0");
                getListView().setAdapter(null);
                getListView().setAdapter(_adapter);
                getListView().setSelection(0);
				
				_silentLoad = true;
        	}
        	
        	// first refresh, or user hit refresh button
        	if (!_silentLoad)
        	{
        		_silentLoad = true;
        	}
        }
        private class ViewHolder
        {
            public LinearLayout firstRow;
			public ImageButton collapse;
			public ImageButton fav;
			public CheckableLinearLayout container;
			TextView containsLols;
			TextView moderation;
            TextView userName;
            TextView content;
            TextView posted;
            TextView replyCount;
            int defaultTimeColor;
        }
        
        @Override
        public Filter getFilter() {
            if (mFilter == null) {
                mFilter = new ThreadFilter();
            }
            return mFilter;
        }
        
        
        /**
         * Custom Filter implementation for the items adapter.
         *
         */
        protected class ThreadFilter extends Filter
        {
        	public String lastFilterString;
			protected FilterResults performFiltering(CharSequence prefix)
            {
                // Initiate our results object
                FilterResults results = new FilterResults();
                // If the adapter array is empty, check the actual items array and use it

                // No prefix is sent to filter by so we're going to send back the original array
                if (prefix == null || prefix.length() == 0) {
                	
                	synchronized (mLock)
                	{
	                    results.values = _itemList;
	                    results.count = _itemList.size();
	                    lastFilterString = "";
                	}
                }
                else
                {
                	synchronized (mLock)
                	{
	                    // Compare lower case strings
	                    String prefixString = prefix.toString().toLowerCase();
	                    lastFilterString = prefixString;
	                    // Local to here so we're not changing actual array
	                    final ArrayList<Thread> items = _itemList;
	                    final int count = items.size();
	                    final ArrayList<Thread> newItems = new ArrayList<Thread>(count);
	                    for (int i = 0; i < count; i++) {
	                        final Thread item = items.get(i);
	                        final String itemName = item.getFilterable();
	                        // First match against the whole, non-splitted value
	                        if (itemName.contains(prefixString)) {
	                            newItems.add(item);
	                        }
	                    }
	                    // Set and return
	                    results.values = newItems;
	                    results.count = newItems.size();
                	}
                }
                return results;
            }
			
            protected void publishResults(CharSequence prefix, FilterResults results)
            {
            	synchronized (mLock)
            	{
            		final FilterResults res = results;
            		if (getActivity() != null)
            		{
	            		getActivity().runOnUiThread(new Runnable(){
	                		@Override public void run()
	                		{
				                //noinspection unchecked
			            		
				                _filteredItemList = (ArrayList<Thread>) res.values;
				                // Let the adapter know about the updated list
				                
				                if (res.count > 0) {
				                    notifyDataSetChanged();
				                } else {
				                    notifyDataSetInvalidated();
				                }
	                		}
	            		});
            		}
            	}
            }
        }
        
    }

    // back in threadlist fragment
    
    public void adjustSelected(int movement)
    {
    	if (_viewAvailable)
    	{
    		// INTEGRATION PULL TO REFRESH +1
	        int index = getListView().getCheckedItemPosition() + movement;
	        if (index >= 0 && index < getListView().getCount())
	        {
	        	getListView().setItemChecked(index, true);
	            ensureVisible(index, 0);
	        }
    	}
    }
    
    void ensureVisible(int position, int minPos)
    {
    	ListView view = getListView();

        
        if (position < minPos || position >= view.getCount())
            return;
        
        int first = view.getFirstVisiblePosition();
        int last = view.getLastVisiblePosition();
        int destination = 0;
        
        if (position < first)
            destination = position;
        else if (position >= last)
            destination = (position - (last - first));
        
        if ((position < first) || (position >= last))
        {
        	view.setSelection(destination);
        }
        
        // keep the child view on screen
		final int pos = position;
		final ListView listView = view;
		listView.post(new Runnable() {
		    @Override
		    public void run() {
		    	View betterView = getListViewChildAtPosition(pos, listView);
		    	if (betterView != null)
		    	{
		     		listView.requestChildRectangleOnScreen(betterView, new Rect(0, 0, betterView.getRight(), betterView.getHeight()), false);
		     	}
		     	else
		            listView.smoothScrollToPosition(pos);
		     }
		});
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
}

