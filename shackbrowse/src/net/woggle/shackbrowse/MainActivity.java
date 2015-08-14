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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;

import net.woggle.shackbrowse.ChangeLog.onChangeLogCloseListener;
import net.woggle.shackbrowse.NetworkNotificationServers.OnGCMInteractListener;
import net.woggle.shackbrowse.PullToRefreshAttacher.Options;
import net.woggle.shackbrowse.notifier.NotifierReceiver;

import org.json.JSONObject;


import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialogCompat;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;

import static net.woggle.shackbrowse.StatsFragment.statInc;

public class MainActivity extends ActionBarActivity
{
	static final String PQPSERVICESUCCESS = "net.woggle.PQPServiceSuccess";
    FrameLayout mFrame;
    OfflineThread mOffline;
    
    public SearchResultFragment _searchResults;
    public ThreadViewFragment _threadView;
    public FrontpageBrowserFragment _fpBrowser;
    int _splitView = 1;
    boolean _dualPane = false;
	private int _orientLock = 0;
	SharedPreferences _prefs;
	private ArrayList<Integer> _threadIdBackStack = new ArrayList<Integer>();

	SlideFrame _tviewFrame;
	SlideFrame _sresFrame;
	public boolean _analytics = true;
	
	public Boolean _messagesGetInbox = true;
	protected boolean _nextBackQuitsBecauseOpenedAppViaIntent = false;
	private float _zoom = 1.0f;
	private AppMenu _appMenu;
	boolean _showPinnedInTL = true;
	private long _lastOpenedThreadViewEpochSeconds = 0l;
	
	boolean _swappedSplit = false;
	public boolean _enableDonatorFeatures = false;
	Seen _seen;
	NetworkNotificationServers _GCMAccess;
	private ThreadListFragment _threadList;
	private MessageFragment _messageList;
	int _currentFragmentType;
	private String mTitle;
	private String mDrawerTitle;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	private FrameLayout _menuFrame;
	PullToRefreshAttacher mPullToRefreshAttacher;
	private boolean mPopupBrowserOpen = false;
	private FrameLayout mBrowserFrame;
	private PopupBrowserFragment mPBfragment;
	private int mShortAnimationDuration;
	private MenuItem mFinder;
	private boolean mBrowserIsClosing = false;
	private MenuItem mHighlighter;
	private int onPostResume = OnPostResume.DO_NOTHING;
	private PQPServiceReceiver mPQPServiceReceiver;
	private NetworkConnectivityReceiver mNetworkConnectivityReceiver;
    public boolean mSOPBFPTRL;
    public FrontpageBrowserFragment _articleViewer;
    private boolean mArticleViewerIsOpen = false;
    private boolean mSplashOpen;
    public LoadingSplashFragment _loadingSplash;
    private Fragment mCurrentFragment;
    private boolean mActivityAvailable = false;
    protected int mThemeResId = R.style.AppTheme;
    private Long mLastResumeTime = TimeDisplay.now();
	protected boolean isBeta = false;
	protected String mVersion = "none";

    public PullToRefreshAttacher getRefresher()
	{
		return mPullToRefreshAttacher;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);


        _prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // sets theme
        mThemeResId = MainActivity.themeApplicator(this);

        // app open stat
        StatsFragment.statInc(this, "AppOpenedFresh");

		// get version data
		// check if is beta or not
		String thisversion;
		try {
			thisversion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			// TODO Auto-generated catch block
			thisversion = "unknown";
		}
		mVersion = thisversion;
		isBeta = thisversion.toLowerCase().contains("beta");

		// enforce overflow menu
		try {
	        ViewConfiguration config = ViewConfiguration.get(this);
	        Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
	        if(menuKeyField != null) {
	            menuKeyField.setAccessible(true);
	            menuKeyField.setBoolean(config, false);
	        }
	    } catch (Exception ex) {
	        // Ignore
	    }
		
		this.setContentView(R.layout.main_splitview);
		
		// set up persistent fragments
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
					
		if (fm.findFragmentByTag("tview") != null)
		{
			_threadView = (ThreadViewFragment) fm.findFragmentByTag("tview");
		}
		else
			_threadView = new ThreadViewFragment();
		
		if (fm.findFragmentByTag("sres") != null)
			_searchResults = (SearchResultFragment) fm.findFragmentByTag("sres");
		else
		{
			_searchResults = new SearchResultFragment();
		}

        if (fm.findFragmentByTag("pbfrag") != null)
            mPBfragment = (PopupBrowserFragment) fm.findFragmentByTag("pbfrag");
        else
        {
            mPBfragment = new PopupBrowserFragment();
        }
		
		if (fm.findFragmentByTag(Integer.toString(CONTENT_THREADLIST)) != null)
		{
			_threadList = (ThreadListFragment)fm.findFragmentByTag(Integer.toString(CONTENT_THREADLIST));
		}
		else
		{
			_threadList = new ThreadListFragment();
		}

        if (fm.findFragmentByTag(Integer.toString(CONTENT_FRONTPAGE)) != null)
        {
            _fpBrowser = (FrontpageBrowserFragment)fm.findFragmentByTag(Integer.toString(CONTENT_FRONTPAGE));
        }
        else
        {
            _fpBrowser = new FrontpageBrowserFragment();
        }
		
		_messageList = new MessageFragment();

        _loadingSplash = new LoadingSplashFragment();
		
		if (fm.findFragmentById(R.id.menu_frame) != null)
			_appMenu = (AppMenu) fm.findFragmentById(R.id.menu_frame);
		else
		{
			_appMenu = new AppMenu();
			// menu setup
	  		ft = getFragmentManager().beginTransaction();
	  		ft.attach(_appMenu);
	  		ft.replace(R.id.menu_frame, _appMenu, "appmenu");
	  		ft.commit();
		}
					
		if (savedInstanceState == null)
		{
			// only have to attach when starting new
			ft = getFragmentManager().beginTransaction();
			ft.replace(R.id.singleThread, _threadView, "tview");
			ft.attach(_threadView);
			ft.commit();
			
			ft = fm.beginTransaction();
			ft.replace(R.id.searchResults, _searchResults, "sres");
			ft.attach(_searchResults);
			ft.commit();
			
			ft = fm.beginTransaction();
			// ft.replace(R.id.content_frame, _threadList, Integer.toString(CONTENT_THREADLIST));
			ft.attach(_threadList);
			ft.commit();

            ft = fm.beginTransaction();
            ft.attach(_fpBrowser);
            ft.commit();
			
			ft = fm.beginTransaction();
			ft.attach(_messageList);
			ft.commit();


            ft = fm.beginTransaction();
            ft.attach(mPBfragment);
            ft.commit();

		}

		mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime) * 1;

		getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

		// this is for upgraders, a check to upgrade donator prefs
		upgradeDonatorPreferences();
				
		// set up preferences
        reloadPrefs();
		
		// notifications registrator, works mostly automatically
		OnGCMInteractListener GCMlistener = new OnGCMInteractListener(){@Override	public void networkResult(String res) {
			// this allows the check mark to be placed when push notifications are automatically enabled if the setting has never been touched
			Editor edit = _prefs.edit();
			if (res.contains("ok"))
			{
				edit.putBoolean("noteEnabled", true);
				_appMenu.updateMenuUi();
				System.out.println("PUSHREG: registered");
			}
			edit.apply();
		}

		@Override
		public void userResult(JSONObject result) {
			// TODO Auto-generated method stub
			
		}};
		_GCMAccess = new NetworkNotificationServers(this, GCMlistener);
		// this pref is OPT OUT
		if (!_prefs.contains("noteEnabled"))
    	{
			_GCMAccess.doRegisterTask("reg");
    	}
		
		if (_prefs.contains("noteEnabled"))
		{
			// notifications are enabled. sync server with local settings
			if (_prefs.getBoolean("noteEnabled", false))
			{
				_GCMAccess.updReplVan(_prefs.getBoolean("noteReplies", true),_prefs.getBoolean("noteVanity", false));
			}
		}
		
		
		
		// seen
		_seen = new Seen();

		// set up landscape mode navigation
		evaluateDualPane(getResources().getConfiguration());
		
		
		// setup drawerlayou
        mTitle = mDrawerTitle = (String) getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerContainer);
        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {
                if (_appMenu != null) {
                    _appMenu.updateMenuUi();
                }
            }

            @Override
            public void onDrawerClosed(View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.LEFT);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                 R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
            	setTitleContextually();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                setTitleContextually();
            }
        };

        // popup frame
        mBrowserFrame = (FrameLayout)findViewById(R.id.browser_frame);
        
        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // set up pull to refresh
        // Create a PullToRefreshAttacher instance
        Options ptro = new PullToRefreshAttacher.Options();
        ptro.headerLayout = R.layout.ptr_header;
        ptro.refreshScrollDistance = 0.4f;
        mPullToRefreshAttacher = PullToRefreshAttacher.get(this, ptro);
        
        mFrame = (FrameLayout)findViewById(R.id.content_frame);

        // set up favorites class
        mOffline = new OfflineThread(this);
        
        
        // set up change log
        ChangeLog cl = new ChangeLog(this);
        boolean clIsShowing = false;
        if (cl.firstRun())
        {
        	clIsShowing = true;
            cl.getLogDialog().show();
            cl.setChangeLogCloseListener(new onChangeLogCloseListener() {
				
				@Override
				public void onClose() {
					annoyBrowserZoomDialog();
				}
			});
            StatsFragment.statInc(this, "AppUpgradedToNewVersion");
        }
        
        // set up donator icons
        new LimeTask().execute();

        // sync stats
        StatsFragment sfrag = new StatsFragment();
        sfrag.blindStatSync(this);
        
        // clean up postqueue
	    Intent msgIntent = new Intent(this, PostQueueService.class);
	    msgIntent.putExtra("appinit", true);
	    startService(msgIntent);
        
        // initialize slide frame handle
        _tviewFrame = ((SlideFrame)findViewById(R.id.singleThread));
        _sresFrame = ((SlideFrame)findViewById(R.id.searchResults));
        _menuFrame = ((FrameLayout)findViewById(R.id.menu_frame));
        
        _tviewFrame.setOnInteractListener(new SlideFrame.OnInteractListener() {
			
			@Override
			public void onOpened() {
				// TODO Auto-generated method stub
				if (!getDualPane()) {
                    mFrame.setVisibility(View.GONE);
                    _sresFrame.setVisibility(View.GONE);
                }
				
				setTitleContextually();
			}
			
			@Override
			public void onOpen() {
			}
			
			@Override
			public void onClosed() {
				setTitleContextually();
			}
			
			@Override
			public void onClose() {
				// user closed thread slider, so doesnt want to go back to previous app
				_nextBackQuitsBecauseOpenedAppViaIntent = false;
				_threadList._nextBackQuitsBecauseOpenedAppViaIntent = false;
				
				if (_sresFrame.isOpened())
					mFrame.setVisibility(View.GONE);
				else
					mFrame.setVisibility(View.VISIBLE);
				_sresFrame.setVisibility(View.VISIBLE);
			}
			public void onDrag()
			{
				if (_sresFrame.isOpened())
					mFrame.setVisibility(View.GONE);
				else
					mFrame.setVisibility(View.VISIBLE);
				_sresFrame.setVisibility(View.VISIBLE);
			}

            public void onXChange(float x){
                slideContentFrameBasedOnTView(x, true);

            };

		});
        _sresFrame.setOnInteractListener(new SlideFrame.OnInteractListener() {
			
			@Override
			public void onOpened() {
				setTitleContextually();
				
				if (!getDualPane())
					mFrame.setVisibility(View.GONE);
				else
					mFrame.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onOpen() {
			}
			
			@Override
			public void onClosed() {
				setTitleContextually();
			}
			
			@Override
			public void onClose()
			{
				mFrame.setVisibility(View.VISIBLE);
			}
			public void onDrag()
			{
				mFrame.setVisibility(View.VISIBLE);
			}

            public void onXChange(float x){
                slideContentFrameBasedOnTView(x, false);

            };
		});
        
        _sresFrame.setSlidingEnabled(true);
        
        // default content setting
        setContentTo(Integer.parseInt(_prefs.getString("APP_DEFAULTPANE", Integer.toString(CONTENT_THREADLIST))));
        
        // check for wifi connection
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        // check for shack messages
        if (mWifi.isConnected() || _prefs.getBoolean("SMCheckOnCellNotification", false)) {
        	new CheckForSMTask().execute();
        }
        
        // check versions
        new VersionTask().execute();
        
        // external intent handling
        Intent intent = getIntent();
        if ((intent == null) || (canHandleIntent(intent) == CANNOTHANDLEINTENT))
        {
        	if (!clIsShowing)
        	{
	        	// no external intent, do annoyance dialogs
	        	annoyBrowserZoomDialog();
	        }
        }
        else if (canHandleIntent(intent) != CANNOTHANDLEINTENT)
        {
        	onPostResumeIntent = intent;
        	onPostResume = OnPostResume.HANDLE_INTENT;
        	if (canHandleIntent(intent) == CANHANDLEINTENTANDMUSTSETNBQBAOVI)
        	{
        		if (_threadList != null)
        			_threadList._nextBackQuitsBecauseOpenedAppViaIntent = true;
        	}
        }
	}

    public static int themeApplicator(Activity context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String appTheme =  prefs.getString("appTheme", "0");
        int themeId;
        int statusBarColor;
        if (appTheme.equals("1")) {
            themeId = R.style.AppThemeDark;
            statusBarColor = R.color.selected_postbg;
        }
        else {
            themeId = R.style.AppTheme;
            statusBarColor = R.color.SBvdark;
        }

        context.setTheme(themeId);

        //We need to manually change statusbar color, otherwise, it remains green.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            context.getWindow().setStatusBarColor(context.getResources().getColor(statusBarColor));
        }

        return themeId;
    }

    protected void setTitleContextually() {
        
		if (mDrawerLayout.isDrawerOpen(_menuFrame))
		{
        	getSupportActionBar().setTitle(mDrawerTitle);
        	mDrawerToggle.setDrawerIndicatorEnabled(true);
		}
		else if (mPopupBrowserOpen)
		{
			boolean browserZoomMode = false; boolean browserPhotoMode = false;
	        if ((mPBfragment != null) && (mPBfragment.mState == mPBfragment.SHOW_ZOOM_CONTROLS))
				browserZoomMode = true;

			if ((mPBfragment != null) && (mPBfragment.mState == mPBfragment.SHOW_PHOTO_VIEW))
				browserPhotoMode = true;
	        
	        if (!browserZoomMode && !browserPhotoMode)
	        	getSupportActionBar().setTitle(getResources().getString(R.string.browser_title));
			else if (browserPhotoMode)
				getSupportActionBar().setTitle(getResources().getString(R.string.browser_photo_title));
	        else
	        	getSupportActionBar().setTitle(getResources().getString(R.string.browserZoom_title));
			mDrawerToggle.setDrawerIndicatorEnabled(false);
		}
		else if (_tviewFrame.isOpened() && (_currentFragmentType == CONTENT_MESSAGES) && !getDualPane())
		{
        	getSupportActionBar().setTitle(getResources().getString(R.string.message_title));
        	mDrawerToggle.setDrawerIndicatorEnabled(false);
		}
        else if (_tviewFrame.isOpened() && (_currentFragmentType == CONTENT_FRONTPAGE || _currentFragmentType == CONTENT_NOTIFICATIONS || _currentFragmentType == CONTENT_THREADLIST || _currentFragmentType == CONTENT_FAVORITES) && !getDualPane())
        {
        	getSupportActionBar().setTitle(getResources().getString(R.string.thread_title));
        	mDrawerToggle.setDrawerIndicatorEnabled(false);
        }
        else if (_sresFrame.isOpened())
        {
        	getSupportActionBar().setTitle(_searchResults._title);
        	mDrawerToggle.setDrawerIndicatorEnabled(false);
        }
        else
        {
        	getSupportActionBar().setTitle(mTitle);
        	mDrawerToggle.setDrawerIndicatorEnabled(true);
        }

		invalidateOptionsMenu();
	}

	@Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

	
	// actionbar menus
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
        	return true;
        }
        
        OfflineThreadFragment otf = null;
        if (_currentFragmentType == CONTENT_FAVORITES)
        {
        	otf = (OfflineThreadFragment)getFragmentManager().findFragmentByTag(Integer.toString(CONTENT_FAVORITES));
        }
        StatsFragment stf = null;
        if (_currentFragmentType == CONTENT_STATS)
        {
            stf = (StatsFragment)getFragmentManager().findFragmentByTag(Integer.toString(CONTENT_STATS));
        }
        SearchViewFragment svf = null;
        if (_currentFragmentType == CONTENT_SEARCHVIEW)
        {
        	svf = (SearchViewFragment)getFragmentManager().findFragmentByTag(Integer.toString(CONTENT_SEARCHVIEW));
        }
        NotificationFragment nf = null;
        if (_currentFragmentType == CONTENT_NOTIFICATIONS)
        {
        	nf = (NotificationFragment)getFragmentManager().findFragmentByTag(Integer.toString(CONTENT_NOTIFICATIONS));
        }

        mPBfragment = (PopupBrowserFragment)getFragment("pbfrag");
        
        NotificationsDB ndb;
		switch (item.getItemId())
	    {
	        case android.R.id.home:
	        	if (mPopupBrowserOpen)
	        	{
	        		closeBrowser();
	        	}
	        	else if (_tviewFrame.isOpened() && !getDualPane()) {
                    _tviewFrame.closeLayer(true);
                    annoyThreadViewClose();
                }
	        	else if (_sresFrame.isOpened())
	        		_sresFrame.closeLayer(true);
	        	break;
	        case R.id.menu_refreshThreads:
	        	_threadList.refreshThreads();
	        	break;
	        case R.id.menu_newPost:
	        	newPost();
	        	break;
	        case R.id.menu_cloudOptions:
	        	cloudChoose();
	        	break;
	        case R.id.menu_keywordFilter:
	        	showKeywords();
	        	break;
	        case R.id.menu_modtagFilter:
	        	showFilters();
	        	break;
	        case R.id.menu_restoreCollapsed:
	        	restoreCollapsed();
	        	break;
	        case R.id.menu_refreshReplies:
	        	_threadView.refreshThreadReplies();
	        	break;
	        case R.id.menu_unfavThread:
	        	_threadView.saveThread();
	        	break;
	        case R.id.menu_favThread:
	        	_threadView.saveThread();
	        	break;
	        case R.id.menu_searchGo:
	        	svf.searchGo();
	        	break;
	        case R.id.menu_searchSave:
	        	svf.saveSearch();
	        	break;
	        case R.id.menu_searchDel:
	        	svf.deleteSearches();
	        	break;
	        case R.id.menu_newMsg:
	        	_messageList.promptRecipient();
	        	break;
	        case R.id.menu_replyMsg:
	        	_threadView.postReply(_threadView._adapter.getItem(0));
	        	break;
	        case R.id.menu_refreshMsg:
	        	_messageList.refreshMessages();
	        	break;
	        case R.id.menu_switchToInbox:
	        	switchMessageType();
	        	break;
	        case R.id.menu_switchToSent:
	        	switchMessageType();
	        	break;
	        case R.id.menu_refreshFav:
	        	if (otf != null)
	        		otf.refreshOfflineThreads();
	        	break;
	        case R.id.menu_discardFav:
	        	if (otf != null)
	        		otf.clearAllOfflineThreads();
	        	break;
	        case R.id.menu_browserClose:
	        	closeBrowser();
	        	break;
	        case R.id.menu_browserOpenExt:
	        	if (mPBfragment != null)
	        		mPBfragment.openExternal();
	        	break;
	        case R.id.menu_browserCopyURL:
	        	if (mPBfragment != null)
	        		mPBfragment.copyURL();
	        	break;
	        case R.id.menu_browserShare:
	        	if (mPBfragment != null)
	        		mPBfragment.shareURL();
	        	break;
	        case R.id.menu_browserSettings:
	        	mAnimEnd onEnd = new mAnimEnd() {
					
					@Override
					public void end() {
                        setContentTo(CONTENT_PREFS);
                        /*
						Intent i = new Intent(MainActivity.this, PreferenceView.class);
		                i.putExtra("pscreenkey", "popupbrowser");
		                startActivityForResult(i, ThreadListFragment.OPEN_PREFS); */
					}
				};
                closeBrowser(true,onEnd, false);
                break;
	        case R.id.menu_browserChangeZoom:
	        	if (mPopupBrowserOpen)
	        	{
	        		restartBrowserWithZoom();
	        	}
                break;
	        case R.id.menu_retrySearch:
	        	_searchResults.retrySearch();
	        	break;
	        case R.id.menu_editSearch:
	        	_searchResults.editSearch();
	        	break;
	        case R.id.menu_notesDel:
	        	ndb = new NotificationsDB(this);
	        	ndb.open();
	        	ndb.deleteAll();
	        	ndb.close();
	        	nf.refreshNotes();
	        	break;
	        case R.id.menu_refreshNotes:
	        	nf.refreshNotes();
	        	break;
            case R.id.menu_fpbrowserCopyURL:
                if ((_fpBrowser != null) && (!isArticleOpen()))
                    _fpBrowser.copyURL();
                if ((_articleViewer != null) && (isArticleOpen()))
                    _articleViewer.copyURL();
                break;
            case R.id.menu_fpbrowserShare:
                if ((_fpBrowser != null) && (!isArticleOpen()))
                    _fpBrowser.shareURL();
                if ((_articleViewer != null) && (isArticleOpen()))
                    _articleViewer.shareURL();
                break;
            case R.id.menu_fprefresh:
                if ((_fpBrowser != null) && (!isArticleOpen()))
                    _fpBrowser.refresh();
                if ((_articleViewer != null) && (isArticleOpen()))
                    _articleViewer.refresh();
                break;
            case R.id.menu_statsTrash:
                if (stf != null)
                    stf.wipeStats();
                break;
            case R.id.menu_statsOptOut:
                if (stf != null)
                    stf.optOutDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

	public void openHighlighter(String query)
	{
		mHighlighter.expandActionView();
		SearchView sview = (SearchView)mHighlighter.getActionView();
		sview.setQuery(query, true);
		// hideKeyboard();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);

	    mHighlighter = menu.findItem(R.id.menu_findInThread);
        final SearchView sview = (SearchView)MenuItemCompat.getActionView(mHighlighter);
        if (sview != null) {
            sview.setOnSearchClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ((_threadView != null) && (_threadView._adapter != null)) {
                        _threadView._highlighting = true;
                    }
                }
            });
            sview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

                @Override
                public boolean onQueryTextChange(String newText) {
                    if ((_threadView != null) && (_threadView._adapter != null)) {
                        _threadView._highlight = newText;
                        _threadView._adapter.notifyDataSetChanged();
                    }
                    return false;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {
                    // used to hide the keyboard
                    sview.setVisibility(View.INVISIBLE);
                    sview.setVisibility(View.VISIBLE);
                    return false;
                }
            });
        }
        MenuItemCompat.setOnActionExpandListener(mHighlighter,new MenuItemCompat.OnActionExpandListener(){
			@Override
			public boolean onMenuItemActionCollapse(MenuItem arg0) {
				if ((_threadView != null) && (_threadView._adapter != null))
				{
					_threadView._highlighting = false;
					_threadView._highlight = "";
					_threadView._adapter.notifyDataSetChanged();
				}
				return true;
			}

			@Override
			public boolean onMenuItemActionExpand(MenuItem arg0) {
				if ((_threadView != null) && (_threadView._adapter != null))
				{
					_threadView._highlighting = true;
				}
				return true;
			}});


	    
	    mFinder = menu.findItem(R.id.menu_findOnPage);
        MenuItemCompat.setOnActionExpandListener(mFinder, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem arg0) {
                if ((_threadList != null) && (_threadList._adapter != null)) {
                    _threadList._filtering = false;
                    _threadList._adapter.getFilter().filter("");
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem arg0) {
                if ((_threadList != null) && (_threadList._adapter != null)) {
                    _threadList._filtering = true;
                }
                return true;
            }
        });
	    SearchView sview2 = (SearchView)mFinder.getActionView();
	    sview2.setOnSearchClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				if ((_threadList != null) && (_threadList._adapter != null))
				{
					_threadList._filtering = true;
				}
			}});
	    sview2.setOnQueryTextListener(new SearchView.OnQueryTextListener(){

			@Override
			public boolean onQueryTextChange(String newText) {
				if ((_threadList != null) && (_threadList._adapter != null))
				{
					_threadList._adapter.getFilter().filter(newText);
				}
				return false;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				// TODO Auto-generated method stub
				return false;
			}});
	    
	    return super.onCreateOptionsMenu(menu);
	}
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);

        boolean isMenuOpen = mDrawerLayout.isDrawerOpen(_menuFrame);
        boolean isResultsOpen = _sresFrame.isOpened();
        boolean isRepliesOpen = _tviewFrame.isOpened();
        boolean areSlidersOpen = (isMenuOpen || isResultsOpen || isRepliesOpen);
        boolean dualPane = getDualPane();
        
        boolean showTListItems = ((_currentFragmentType == CONTENT_THREADLIST) && (dualPane || !areSlidersOpen)) && (!mPopupBrowserOpen) && (!isResultsOpen);
        boolean showFavItems = ((_currentFragmentType == CONTENT_FAVORITES) && (dualPane || !areSlidersOpen)) && (!mPopupBrowserOpen) && (!isResultsOpen);
        boolean showReplyViewItems = (dualPane || isRepliesOpen) && (!mPopupBrowserOpen) && (!isMenuOpen);
        
        boolean showSearchItems = (_currentFragmentType == CONTENT_SEARCHVIEW) && (dualPane || !areSlidersOpen) && (!mPopupBrowserOpen) && (!isResultsOpen);
        
        boolean showMessageItems = (_currentFragmentType == CONTENT_MESSAGES) && (!mPopupBrowserOpen) && (!isMenuOpen) && (!isResultsOpen);
        boolean showNoteItems = (_currentFragmentType == CONTENT_NOTIFICATIONS) && (!mPopupBrowserOpen) && (!isMenuOpen) && (!isResultsOpen);

        boolean showFPBrowserItems = ((_currentFragmentType == CONTENT_FRONTPAGE) && (dualPane || !areSlidersOpen)) && (!mPopupBrowserOpen) && (!isMenuOpen) && (!isResultsOpen);
        
        boolean browserZoomMode = false;
        if ((mPBfragment != null) && (mPBfragment.mState == mPBfragment.SHOW_ZOOM_CONTROLS))
        	browserZoomMode = true;
        
        menu.findItem(R.id.menu_refreshFav).setVisible(showFavItems && (!isMenuOpen));
        menu.findItem(R.id.menu_discardFav).setVisible(showFavItems && (!isMenuOpen));
        
        menu.findItem(R.id.menu_refreshThreads).setVisible(showTListItems);
        menu.findItem(R.id.menu_cloudOptions).setVisible(showTListItems || showFavItems);
        menu.findItem(R.id.menu_findOnPage).setVisible(showTListItems);
        if ((!showTListItems) && (mFinder.isActionViewExpanded()))
        	mFinder.collapseActionView();
        menu.findItem(R.id.menu_keywordFilter).setVisible(showTListItems);
        menu.findItem(R.id.menu_modtagFilter).setVisible(showTListItems);
        menu.findItem(R.id.menu_newPost).setVisible(showTListItems);
        menu.findItem(R.id.menu_restoreCollapsed).setVisible(showTListItems);
        
        menu.findItem(R.id.menu_searchGo).setVisible(showSearchItems);
        menu.findItem(R.id.menu_searchDel).setVisible(showSearchItems && _enableDonatorFeatures);
        menu.findItem(R.id.menu_searchSave).setVisible(showSearchItems && _enableDonatorFeatures);
        
        menu.findItem(R.id.menu_replyMsg).setVisible(showMessageItems && (_threadView._messageId != 0) && (dualPane || areSlidersOpen));
        menu.findItem(R.id.menu_newMsg).setVisible(showMessageItems);
        menu.findItem(R.id.menu_refreshMsg).setVisible(showMessageItems && (dualPane || !areSlidersOpen));
        menu.findItem(R.id.menu_switchToSent).setVisible(showMessageItems && _messagesGetInbox && (dualPane || !areSlidersOpen));
        menu.findItem(R.id.menu_switchToInbox).setVisible(showMessageItems && !_messagesGetInbox && (dualPane || !areSlidersOpen));
        
        menu.findItem(R.id.menu_retrySearch).setVisible(isResultsOpen && (!isRepliesOpen || dualPane) && (!mPopupBrowserOpen) && (!isMenuOpen));
        menu.findItem(R.id.menu_editSearch).setVisible(isResultsOpen && (!isRepliesOpen || dualPane) && (!mPopupBrowserOpen) && (!isMenuOpen));
        
        menu.findItem(R.id.menu_findInThread).setVisible(showReplyViewItems);
        if ((!showReplyViewItems) && (mHighlighter.isActionViewExpanded()))
        	mHighlighter.collapseActionView();
        menu.findItem(R.id.menu_refreshReplies).setVisible(showReplyViewItems && !showMessageItems);
        if (dualPane)
        	menu.findItem(R.id.menu_refreshReplies).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        else
        	menu.findItem(R.id.menu_refreshReplies).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        
        // these two are so complicated they are managed in the actual fragment
        menu.findItem(R.id.menu_favThread).setVisible((_threadView._showFavSaved) && showReplyViewItems);
        menu.findItem(R.id.menu_unfavThread).setVisible((_threadView._showUnFavSaved) && showReplyViewItems);
        
        menu.findItem(R.id.menu_browserClose).setVisible(mPopupBrowserOpen);
        menu.findItem(R.id.menu_browserOpenExt).setVisible(mPopupBrowserOpen && !browserZoomMode);
        menu.findItem(R.id.menu_browserSettings).setVisible(mPopupBrowserOpen);
        menu.findItem(R.id.menu_browserShare).setVisible(mPopupBrowserOpen && !browserZoomMode);
        menu.findItem(R.id.menu_browserChangeZoom).setVisible(mPopupBrowserOpen && !browserZoomMode);
        menu.findItem(R.id.menu_browserCopyURL).setVisible(mPopupBrowserOpen && !browserZoomMode);
        
        menu.findItem(R.id.menu_refreshNotes).setVisible(showNoteItems && (dualPane || !areSlidersOpen));
        menu.findItem(R.id.menu_notesDel).setVisible(showNoteItems && (dualPane || !areSlidersOpen));

        menu.findItem(R.id.menu_fpbrowserCopyURL).setVisible(showFPBrowserItems);
        menu.findItem(R.id.menu_fpbrowserShare).setVisible(showFPBrowserItems);
        menu.findItem(R.id.menu_fprefresh).setVisible(showFPBrowserItems);

        menu.findItem(R.id.menu_statsTrash).setVisible(_currentFragmentType == CONTENT_STATS ? true : false);
        menu.findItem(R.id.menu_statsOptOut).setVisible(_currentFragmentType == CONTENT_STATS ? true : false);
		return true;
    }

	// this is how we switch views
	public static final int CONTENT_THREADLIST = 0;
	public static final int CONTENT_MESSAGES = 1;
	public static final int CONTENT_SEARCHVIEW = 2;
	public static final int CONTENT_FAVORITES = 3;
	public static final int CONTENT_NOTIFICATIONS = 4;
    public static final int CONTENT_PREFS = 5;
    public static final int CONTENT_FRONTPAGE = 6;
    public static final int CONTENT_STATS = 7;
    public static final int CONTENT_NOTEPREFS = 8;
	
	void setContentTo(int type)
	{
		setContentTo(type, null);
	}
	void setContentTo(int type, Bundle bundle) {
		Fragment fragment = null;
		if (bundle == null)
			bundle = new Bundle();
		
		if (type == CONTENT_THREADLIST)
		{
			mTitle = "Latest Chatty";
			if (isBeta)
				mTitle = "Beta " + mVersion;
			fragment = _threadList;
		}
		if (type == CONTENT_MESSAGES)
		{
			mTitle = "Shack Messages";
			fragment = _messageList;
		}
		if (type == CONTENT_NOTIFICATIONS)
		{
			mTitle = "Notifications";
			fragment = (NotificationFragment)Fragment.instantiate(getApplicationContext(), NotificationFragment.class.getName(), new Bundle());
		}
		if (type == CONTENT_SEARCHVIEW)
		{
			mTitle = "Search";
			fragment = (SearchViewFragment)Fragment.instantiate(getApplicationContext(), SearchViewFragment.class.getName(), bundle);
			}
		if (type == CONTENT_FAVORITES)
		{
			mTitle = "Favorites";
			fragment = (OfflineThreadFragment)Fragment.instantiate(getApplicationContext(), OfflineThreadFragment.class.getName(), new Bundle());
		}
        if (type == CONTENT_PREFS)
        {
            mTitle = "Settings";
            fragment = (PreferenceFragment)Fragment.instantiate(getApplicationContext(), PreferenceView.class.getName(), new Bundle());
        }
        if (type == CONTENT_STATS)
        {
            mTitle = "Statistics";
            fragment = (StatsFragment)Fragment.instantiate(getApplicationContext(), StatsFragment.class.getName(), new Bundle());
            statInc(this, "TimeInApp", TimeDisplay.secondsSince(mLastResumeTime)); mLastResumeTime = TimeDisplay.now();
        }
        if (type == CONTENT_NOTEPREFS)
        {
            mTitle = "Notification Preferences";
            fragment = (PreferenceFragmentNotifications)Fragment.instantiate(getApplicationContext(), PreferenceFragmentNotifications.class.getName(), new Bundle());
        }
        if (type == CONTENT_FRONTPAGE)
        {
            mTitle = "Frontpage";
            fragment = _fpBrowser;
            _articleViewer = (FrontpageBrowserFragment) Fragment.instantiate(getApplicationContext(), FrontpageBrowserFragment.class.getName(), new Bundle());
        }
		

		
		// turn off any refresher bars so the new fragment can work
		getRefresher().setRefreshComplete();
        mSOPBFPTRL = true;
		showOnlyProgressBarFromPTRLibrary(false);
		
		FragmentManager fragmentManager = getFragmentManager();


        // clear all fragments from id.content_frame
        Fragment toBeDeleted = (Fragment) getFragmentManager().findFragmentById(R.id.content_frame);
        while (toBeDeleted != null)
        {
            System.out.println("DELETING A FRAG");
            fragmentManager.beginTransaction()
                    .remove(toBeDeleted)
                    .commit();
            fragmentManager.executePendingTransactions();
            toBeDeleted = (Fragment) getFragmentManager().findFragmentById(R.id.content_frame);
        }




	    fragmentManager.beginTransaction()
	                   .add(R.id.content_frame, fragment, Integer.toString(type))
	                   .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
	                   .commit();

        mCurrentFragment = fragment;

        if (type == CONTENT_FRONTPAGE)
        {
            fragmentManager.beginTransaction()
                    .add(R.id.content_frame, _articleViewer, "article")
                    .hide(_articleViewer)
                    .commit();

            _fpBrowser.setFirstOpen("http://www.shacknews.com/topic/news");
        }

        fragmentManager.beginTransaction()
                .add(R.id.content_frame, _loadingSplash, "splash")
                .hide(_loadingSplash)
                .commit();

        // clean up for outgoing fragment
        if (_currentFragmentType == CONTENT_FRONTPAGE)
        {
            // kill weird ad crap running in background
            _fpBrowser.mWebview.loadData("", "text/html", null);
        }
        if (_currentFragmentType == CONTENT_PREFS)
        {
            reloadPrefs();
        }

	    _currentFragmentType = type;

        evaluateDualPane(getResources().getConfiguration());

	    _sresFrame.closeLayer(true);
		if (_tviewFrame.isOpened() && !getDualPane())
    		_tviewFrame.closeLayer(true);
		if (mPopupBrowserOpen)
    	{
    		closeBrowser();
    	}

        setTitleContextually();

        _appMenu.updateMenuUi();
		trackScreen(mTitle);
	    
	}


	// used for sending fake notifications to the notifierreceiver. check for sms locally, then send notification!
	private void sendSMBroadcast(String username, String text, int nlsid, boolean multiple, int howMany) {
		NotifierReceiver receiver = new NotifierReceiver();
		registerReceiver( receiver, new IntentFilter( "net.woggle.fakenotification" ) );
		
		Intent broadcast = new Intent();
		broadcast.putExtra("type", "shackmsg");
		broadcast.putExtra("username", (multiple) ? "multiple" : username);
		broadcast.putExtra("text", (multiple) ? Integer.toString(howMany) : text);
		broadcast.putExtra("nlsid", Integer.toString(nlsid));
        broadcast.setAction("net.woggle.fakenotification");
        sendBroadcast(broadcast);
        
       // unregisterReceiver(receiver);
	}

	protected void hideKeyboard() {
		final InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mFrame.getWindowToken(), 0);
	}

	@Override
	protected void onDestroy () {
		
	    super.onDestroy();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		mOffline.endCloudUpdates();
		
		// unregister receiver for pqpservice
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mPQPServiceReceiver);
		
		// unreg ncr
        try {
            unregisterReceiver(mNetworkConnectivityReceiver);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
		
		// set pref that activity is not foreground. for postqueueservice
		Editor ed = _prefs.edit();
		ed.putBoolean("isAppForeground", false);
		ed.commit();

        mActivityAvailable = false;

        statInc(this, "TimeInApp", TimeDisplay.secondsSince(mLastResumeTime));
	}
	
	@Override
	protected void onResume()
	{
		
		super.onResume();

        StatsFragment.statInc(this, "AppOpened");
        mLastResumeTime = TimeDisplay.now();

		mOffline.startCloudUpdates();
		
		// register to receive information from PQPService
		IntentFilter filter = new IntentFilter(PQPSERVICESUCCESS);
        mPQPServiceReceiver = new PQPServiceReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(
        		mPQPServiceReceiver,
                filter);
        
        // connectivity changes
        mNetworkConnectivityReceiver = new NetworkConnectivityReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(mNetworkConnectivityReceiver, intentFilter);
        
        // set pref that activity is not foreground. use by postqueueservice
        Editor ed = _prefs.edit();
		ed.putBoolean("isAppForeground", true);
		ed.apply();

        mActivityAvailable = true;

        if (!mPopupBrowserOpen)
            // make sure the browser is closed
            closeBrowser(true, null, true);
	}
	
	public void toggleMenu() {
		if (!mDrawerLayout.isDrawerOpen(_menuFrame))
			openMenu();
		else
			closeMenu();
	}
	
	public void openMenu() {
		if (!mDrawerLayout.isDrawerOpen(_menuFrame))
		{
			_appMenu.updateMenuUi();
			mDrawerLayout.openDrawer(_menuFrame);
		}
	}
	public void updateAppMenu()
	{
		_appMenu.updateMenuUi();
	}
	public boolean isMenuOpen() {
		if (mDrawerLayout.isDrawerOpen(_menuFrame))
		{
			return true;
		}
		else return false;
	}
	
	public void closeMenu() {
		mDrawerLayout.closeDrawer(_menuFrame);
	}

	// FRAGMENT COMMAND SENDING AREA
	public void openComposer (int returnResultType, String preText)
	{
		Intent i = new Intent(this, ComposePostView.class);
		if (preText != null)
			{ i.putExtra("preText", preText); }
        startActivityForResult(i, returnResultType);
	}
	public void openComposerAndUploadImage (int returnResultType, Uri preImage)
	{
		Intent i = new Intent(this, ComposePostView.class);
		i.putExtra("preImage", preImage);
        startActivityForResult(i, returnResultType);
	}
	final static String THREAD_ID = "threadid";
	final static String IS_NEWS_ITEM = "isnewsitem";

	public void openComposerForReply (int returnResultType, Post parentPost, boolean isNewsItem)
	{
		Intent i = new Intent(this, ComposePostView.class);
        i.putExtra(THREAD_ID, parentPost.getPostId());
        i.putExtra("parentAuthor", parentPost.getUserName());
        i.putExtra("parentContent", parentPost.getContent());
        i.putExtra("parentDate", parentPost.getPosted());
        i.putExtra(IS_NEWS_ITEM, isNewsItem);
        startActivityForResult(i, returnResultType);
	}
	
	public void openComposerForMessageReply (int returnResultType, Post parentPost, String messageSubject)
	{
		Intent i = new Intent(this, ComposePostView.class);
        i.putExtra("mode", "message");
        i.putExtra("parentAuthor", parentPost.getUserName());
        i.putExtra("parentContent", parentPost.getCopyText());
        i.putExtra("messageSubject", messageSubject);
        startActivityForResult(i, returnResultType);
	}
	public void openNewMessagePromptForSubject (final String username)
	{
    	boolean verified = _prefs.getBoolean("usernameVerified", false);
        if (!verified)
        {
        	LoginForm login = new LoginForm(this);
        	login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {
				@Override
				public void onSuccess() {
					openNewMessagePromptForSubject(username);
				}

				@Override
				public void onFailure() {
				}
			});
        	return;
        }
        MaterialDialogCompat.Builder alert = new MaterialDialogCompat.Builder(this);
    	alert.setTitle("Shackmessage to " + username);
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(5,5,5,5);
        TextView tv = new TextView(this);
        tv.setText("Subject:");
        int padding_in_dp = 5;  // 6 dps
        final float scale = getResources().getDisplayMetrics().density;
        int padding_in_px = (int) (padding_in_dp * scale + 0.5f);
        tv.setPadding(padding_in_px, padding_in_px, padding_in_px, padding_in_px);
        tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        layout.addView(tv);
    	final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        layout.addView(input);
    	alert.setView(layout);
    	alert.setPositiveButton("Next", new DialogInterface.OnClickListener() {
    	public void onClick(DialogInterface dialog, int whichButton) {
			Editable value = input.getText();
			Post post = new Post(0, username, "", null, 0, "", false);
			openComposerForMessageReply(ThreadViewFragment.POST_MESSAGE, post, value.toString());
    	}});
    	alert.setNegativeButton("Cancel", null);
    	alert.show().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        input.requestFocus();
	}
	
	public void openThreadView(int threadId) { openThreadView(threadId, null, 0, null, false, 0, null, false, false); }
	public void openThreadView(int threadId, Thread thread, LolObj lol) {
		Post post = Post.fromThread(thread);
		post.setLolObj(lol);
		openThreadView(threadId, post, 0, thread.getJson().toString(), false, 0, null, false, false);
	}
	public void openThreadViewAndSelect(int selectPostIdAfterLoading) {	openThreadView(selectPostIdAfterLoading, null, selectPostIdAfterLoading, null, false, 0, null, false, true); }
	public void openThreadViewAndSelectWithBackStack(int selectPostIdAfterLoading) {
		openThreadView(selectPostIdAfterLoading, null, selectPostIdAfterLoading, null, false, 0, null, true, true);
	}
	public void openThreadViewAndFave(int faveThreadId)	{ openThreadView(faveThreadId, null, 0, null, true, 0, null, false, false); }
	public void openMessageView(int messageId, Message message)	{ openThreadView(0, Post.fromMessage(message), 0, null, false, messageId, message.getSubject(), false, false); }
	public void openThreadView(int threadId, Post post, int selectPostIdAfterLoading, String json, boolean autoFaveOnLoad, int messageId, String messageSubject, boolean preserveBackStack, boolean doesntExpire)
	{
        StatsFragment.statInc(this, "ThreadOpened");

		boolean expired = false;
		long current = (System.currentTimeMillis() / 1000);
		
		// threadview data expires after 2 minute
		if ((_lastOpenedThreadViewEpochSeconds > 0) && ((current - _lastOpenedThreadViewEpochSeconds) > 120))
			expired = true;
		
		if (doesntExpire)
			expired = false;
		
		_lastOpenedThreadViewEpochSeconds = current;
		hideKeyboard();
        ThreadViewFragment view = _threadView;
        
        
        if ((!view.isPostIdInAdapter(threadId) || expired) || (view._messageId != messageId))
        {
        	view._rootPostId = threadId;
        	view._messageId = messageId;
        	view._selectPostIdAfterLoading = selectPostIdAfterLoading;
        	view._autoFaveOnLoad = autoFaveOnLoad;
        	view._messageSubject = messageSubject;
        	
        	if (view._adapter != null)
        	{
	        	view._adapter.clear();
	        	view._adapter.triggerLoadMore();
        	}
        	
        	if (post != null)
        		view.loadPost(post);
        	
            if (json != null)
            {
            	try {
					view._lastThreadJson = new JSONObject(json);
				} catch (Exception e) {
					e.printStackTrace();
				}
            }
            else
            	view._lastThreadJson = null;
        	
            view.updateThreadViewUi();
        }
        else if (view.isPostIdInAdapter(threadId))
        {
        	view.ensurePostSelectedAndDisplayed(threadId, true);
        }
        if (!preserveBackStack)
        {
        	this.resetThreadIdBackStack();
        }
        
        _tviewFrame.post(new Runnable() {
            @Override
            public void run() {
                _tviewFrame.openLayer(true);
            }
        });
        _appMenu.updateMenuUi();
	}
	
	public void mRefreshOfflineThreads()
    {
		if (_currentFragmentType == CONTENT_FAVORITES)
		{
			OfflineThreadFragment otf = (OfflineThreadFragment)getFragmentManager().findFragmentByTag(Integer.toString(CONTENT_FAVORITES));
			otf.refreshOfflineThreads();
    	}
    }
	
    public void mRefreshOfflineThreadsWoReplies()
    {
    	if (_currentFragmentType == CONTENT_FAVORITES)
		{
			OfflineThreadFragment otf = (OfflineThreadFragment)getFragmentManager().findFragmentByTag(Integer.toString(CONTENT_FAVORITES));
			otf.updateThreadsWithoutUpdateReplies();
		}
    	
		if (_threadList._adapter != null)
			_threadList._adapter.notifyDataSetChanged();
    }

	public void attemptToUpdateReplyCountInThreadListTo(int rootId, int replies, boolean replied)
	{
		if (_threadList._adapter != null)
		{
			System.out.println("UpdateReplies: TRYING" + rootId);
			int checkIndexFirst = _threadList._itemChecked + 1; // should almost always work, will be faster than looping
			if (_threadList._adapter.getItem(checkIndexFirst).getThreadId() == rootId)
			{
				_threadList._adapter.getItem(checkIndexFirst).setReplyCount(replies);
				_threadList._adapter.getItem(checkIndexFirst).setReplied(replied);
				System.out.println("UpdateReplies: FOUND INDEX THE EASY WAY");
			}
			else {
				int count = _threadList._adapter.getCount();
				for (int i = 0; i < count; i++) {
					if (_threadList._adapter.getItem(i).getThreadId() == rootId) {
						_threadList._adapter.getItem(i).setReplyCount(replies);
						_threadList._adapter.getItem(i).setReplied(replied);
						System.out.println("UpdateReplies: FOUND INDEX THE HARD WAY");
						break;
					}
				}
			}
			// its possible neither of these will be successful for instance if the threadview is in an old thread. awell.
			_threadList._adapter.notifyDataSetChanged();
		}

	}
    
    public void markFavoriteAsRead(int _rootPostId, int count) {
        if ((mOffline != null) && (mOffline.containsThreadId(_rootPostId)))
        {
	    	if (_currentFragmentType == CONTENT_FAVORITES)
			{
				OfflineThreadFragment otf = (OfflineThreadFragment)getFragmentManager().findFragmentByTag(Integer.toString(CONTENT_FAVORITES));
				otf.markFavoriteAsRead(_rootPostId, count);
			}
	    	if (_showPinnedInTL)
	    	{
				_threadList.markFavoriteAsRead(_rootPostId, count);
		    }
	    	
            // update data for last viewed count, this creates the info for how many are unread.
	    	mOffline.updateRecordedReplyCountPrev(_rootPostId, count);
            
            // update "previous" count
	    	mOffline.updateRecordedReplyCount(_rootPostId, count);
	    	mOffline.flushThreadsToDiskTask();
        }
    	mRefreshOfflineThreadsWoReplies();
	}
    
    public void mOfflineThreadsNotifyAdapter()
    {
    	if (_currentFragmentType == CONTENT_FAVORITES)
		{
			OfflineThreadFragment otf = (OfflineThreadFragment)getFragmentManager().findFragmentByTag(Integer.toString(CONTENT_FAVORITES));
			otf._doNotGetReplies = true;
			if (otf._adapter != null)
				otf._adapter.triggerLoadMore();
		}
    	if (_showPinnedInTL)
    	{
			if (_threadList._adapter != null)
				_threadList._adapter.silentUpdatePinned();
	    }
    }
    
    public void switchMessageType()
    {
    	_messagesGetInbox = !_messagesGetInbox;
    	_messageList.refreshMessages();
    }
    public boolean getMessageType()
    {
    	return _messagesGetInbox;
    }
    public MessageFragment getMessageFragment()
    {
		return _messageList;
    }
    public void refreshMessages()
    {
    	getMessageFragment()._adapter.triggerLoadMore();
    }
    public void clearMessages()
    {
    	getMessageFragment()._adapter.clear();
    }
    public void openSearch(Bundle args)
	{
        statInc(this, "SearchedForPosts");
    	if (_tviewFrame.isOpened() && !getDualPane())
    		_tviewFrame.closeLayer(true);
    	
    	mDrawerLayout.closeDrawer(_menuFrame);
    	_sresFrame.openLayer(true);
    	_appMenu.updateMenuUi();
    	_searchResults.openSearch(args, this);
		System.out.println("recvd call for search");
		setTitleContextually();
		hideKeyboard();
	}
	public void openSearchLOL(Bundle args)
	{
        statInc(this, "SearchedForLOLs");
		if (_tviewFrame.isOpened() && !getDualPane())
    		_tviewFrame.closeLayer(true);
		
		mDrawerLayout.closeDrawer(_menuFrame);
		_sresFrame.openLayer(true);
		_appMenu.updateMenuUi();
    	_searchResults.openSearchLOL(args, this);
		System.out.println("recvd call for searchlol");
		hideKeyboard();
		setTitleContextually();
	}
	public void openSearchDrafts()
	{
        statInc(this, "LookedAtDrafts");
		_sresFrame.openLayer(true);
		_appMenu.updateMenuUi();
    	_searchResults.openSearchDrafts(this);
	}
	
	// HANDLE ORIENTATION
	
	@Override
	public void onConfigurationChanged (Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		evaluateDualPane(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
		setTitleContextually();
	}
	
	public void setThreadViewFullScreen (boolean set)
	{
		if (getDualPane())
		{
			this.findViewById(R.id.content_frame).setVisibility((set) ? View.GONE : View.VISIBLE);
			if (set)
				((RelativeLayout.LayoutParams)_tviewFrame.getLayoutParams()).width = this.getScreenWidth();
			else
				setDualPane(true);
		}
		
	}
	public boolean getThreadViewFullScreen()
	{
		return (findViewById(R.id.drawerContainer).getVisibility() == View.GONE);
	}
	public void evaluateDualPane(Configuration conf)
	{
        _splitView = Integer.parseInt(_prefs.getString("splitView", "1"));
       
        if (_orientLock != Integer.parseInt(_prefs.getString("orientLock", "0")))
        {
        	_orientLock = Integer.parseInt(_prefs.getString("orientLock", "0"));
	        if (_orientLock == 0)
	        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
	        if (_orientLock == 1)
	        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	        if (_orientLock == 2)
	        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	        if (_orientLock == 3)
	        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
	        if (_orientLock == 4)
	        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        }
    
        if (_splitView == 0 || _currentFragmentType == CONTENT_FRONTPAGE || _currentFragmentType == CONTENT_PREFS || _currentFragmentType == CONTENT_STATS || _currentFragmentType == CONTENT_NOTEPREFS)
        {
        	setThreadViewFullScreen(false);
        	setDualPane(false);
        }
        else if (((_splitView == 1) && (conf.orientation == Configuration.ORIENTATION_LANDSCAPE)) || (_splitView == 2))
        {
        	setDualPane(true);
        }
        else
        {
        	setThreadViewFullScreen(false);
        	setDualPane(false);
        }
	}
	public boolean getDualPane () { return _dualPane; }
	public boolean getSliderOpen () { return _tviewFrame.isOpened(); }

    private void slideContentFrameBasedOnTView(float x, boolean isTView) {
        FrameLayout contentframe = (FrameLayout)findViewById(R.id.content_frame);

        // doesnt slide in dual pane
        if ((!getDualPane() && contentframe.getVisibility() == View.VISIBLE) || (!isTView && contentframe.getVisibility() == View.VISIBLE))
        {
           // System.out.println("X:" +x + " s" + (-1f - x) * 40f);
           contentframe.setTranslationX((1f - x) * (-.2f * contentframe.getWidth()));
        }
        if (!getDualPane() && _sresFrame.isOpened() && isTView)
        {
            _sresFrame.setTranslationX((1f - x) * (-.2f * contentframe.getWidth()));
        }
    }
	
	public void setDualPane (boolean dualPane)
	{
		SlideFrame slide = (SlideFrame)findViewById(R.id.singleThread);
		SlideFrame sres = (SlideFrame)findViewById(R.id.searchResults);
		FrameLayout contentframe = (FrameLayout)findViewById(R.id.content_frame);
		//RelativeLayout contentCont = (RelativeLayout)findViewById(R.id.contentContainer);
		
		if (!dualPane)
		{
			// CHANGE TO NON DUAL PANE MODE 
			
			// sresults slider
			((RelativeLayout.LayoutParams)sres.getLayoutParams()).width = getScreenWidth();
			((RelativeLayout.LayoutParams)sres.getLayoutParams()).addRule(RelativeLayout.RIGHT_OF, 0);
    		

			((RelativeLayout.LayoutParams)contentframe.getLayoutParams()).width = getScreenWidth();
			((RelativeLayout.LayoutParams)contentframe.getLayoutParams()).addRule(RelativeLayout.RIGHT_OF,0);
			contentframe.requestLayout();
    		
    		// changee slider layer

    		((RelativeLayout.LayoutParams)slide.getLayoutParams()).width = getScreenWidth();
    		slide.requestLayout();
    		((RelativeLayout.LayoutParams)slide.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    		((RelativeLayout.LayoutParams)slide.getLayoutParams()).addRule(RelativeLayout.RIGHT_OF,0);

    		if (_threadView._rootPostId == 0 && _threadView._messageId == 0)
    		{
    			contentframe.setVisibility(View.VISIBLE);
    			slide.closeLayer(false);
    		}
    		else
    			contentframe.setVisibility(View.VISIBLE);
    			
    		if (_prefs.getBoolean("swipeDismiss", true)) 
    			{ slide.setSlidingEnabled(true); }
    		else
    			{ slide.setSlidingEnabled(false); }
    		
    		((RelativeLayout)contentframe.getParent()).requestLayout();
		}
		else
		{
			// DUAL PANE SETUP
			
			// sresults slider
			((RelativeLayout.LayoutParams)sres.getLayoutParams()).width = (int)(getScreenWidth() * (1f / 3f));
			
			// threadview slider
    		((RelativeLayout.LayoutParams)contentframe.getLayoutParams()).width = (int)(getScreenWidth() * (1f / 3f));
    		
    		((RelativeLayout.LayoutParams)slide.getLayoutParams()).width = (int)(getScreenWidth() * (2f / 3f)) + 1;
    		
    		if (_swappedSplit)
    		{
	       		((RelativeLayout.LayoutParams)slide.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_LEFT);
	       		((RelativeLayout.LayoutParams)slide.getLayoutParams()).addRule(RelativeLayout.RIGHT_OF,0);
	    		((RelativeLayout.LayoutParams)contentframe.getLayoutParams()).addRule(RelativeLayout.RIGHT_OF, R.id.singleThread);
	    		((RelativeLayout.LayoutParams)sres.getLayoutParams()).addRule(RelativeLayout.RIGHT_OF, R.id.singleThread);
    		}
    		else
    		{
	       		((RelativeLayout.LayoutParams)slide.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
	    		((RelativeLayout.LayoutParams)slide.getLayoutParams()).addRule(RelativeLayout.RIGHT_OF, R.id.content_frame);
	    		((RelativeLayout.LayoutParams)contentframe.getLayoutParams()).addRule(RelativeLayout.RIGHT_OF, 0);
	    		((RelativeLayout.LayoutParams)sres.getLayoutParams()).addRule(RelativeLayout.RIGHT_OF, 0);
    		}
    		sres.requestLayout();
    		slide.requestLayout();
    		slide.setSlidingEnabled(false);
    		slide.openLayer(false);
    		contentframe.setVisibility(View.VISIBLE);
    		((RelativeLayout)contentframe.getParent()).requestLayout();
    		
    		if (sres.isOpened())
    		{
    			sres.openLayer(false);
    			// correct for overdraw optimization where this is hidden when occluded
    			sres.setVisibility(View.VISIBLE);
    		}
		}
        contentframe.setTranslationX(0f);
        sres.setTranslationX(0f);

		_dualPane = dualPane;
		_threadView.updateThreadViewUi();
		
		track("ui_action", "dual_pane", Boolean.toString(dualPane));
		if (_appMenu != null)
			_appMenu.updateMenuUi();
	}
	

	// HANDLE CLOSED ACTIVITIES RETURNING TO MAIN
    
    private int getScreenWidth() {
    	// calculate sizes
        Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        DisplayMetrics displaymetrics = new DisplayMetrics();
        display.getMetrics(displaymetrics);
		return (displaymetrics.widthPixels);
	}

    public void openThreadByIDDialog() {
        MainActivity _context = this;
        MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(_context);
        builder.setTitle("Enter Thread ID");
        final View view = _context.getLayoutInflater().inflate(R.layout.dialog_openthreadid, null);
        final EditText tid = (EditText) view.findViewById(R.id.openIDText);
        final TextView header = (TextView) view.findViewById(R.id.openIDHeader);
        header.setText("/chatty?id=");
        builder.setView(view);
        builder.setPositiveButton("Open", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                openThreadViewAndSelect(Integer.parseInt(tid.getText().toString()));
            }
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog d = builder.create();
        d.show();
    }


    static private class OnPostResume
    {
		static final int DO_NOTHING = 0;
    	static final int OPEN_BROWSER_ZOOM_SETUP = 1;
    	static final int HANDLE_INTENT = 2;
    }
    private Intent onPostResumeIntent = null;
    @Override
    protected void onPostResume()
    {
    	super.onPostResume();
    	// some things can't be done in oncreate, or else they will cause a crash due to trying to load fragments before the activity is ready
    	// for those things, i use onpostresume
    	if (onPostResume == OnPostResume.OPEN_BROWSER_ZOOM_SETUP)
    	{
    		openBrowserZoomAdjust();
    	}
    	if (onPostResume == OnPostResume.HANDLE_INTENT)
    	{
    		if (onPostResumeIntent != null)
    			handleIntent(onPostResumeIntent);
    	}

		onPostResume = OnPostResume.DO_NOTHING;
		onPostResumeIntent = null;
		
		// start the postqueue service
	    Intent msgIntent = new Intent(this, PostQueueService.class);
	    startService(msgIntent);
    }

    public void restartApp()
    {
        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage( getBaseContext().getPackageName() );
        finish();
        startActivity(i);
        return;
    }

    public void reloadPrefs()
    {
        if (_prefs != null) {
            _analytics = _prefs.getBoolean("analytics", true);
            _zoom = Float.parseFloat(_prefs.getString("fontZoom", "1.0"));
            _showPinnedInTL = _prefs.getBoolean("showPinnedInTL", true);
            _swappedSplit = _prefs.getBoolean("swappedSplit", false);
            _enableDonatorFeatures = true;
            if (_threadView != null) {
                if (_threadView._adapter != null) {
                    _threadView._adapter.loadPrefs();
                    _threadView._adapter.notifyDataSetChanged();
                }
            }
            if (_threadList != null) {
                if (_threadList._adapter != null) {
                    _threadList._adapter.updatePrefs();
                    _threadList._adapter.notifyDataSetChanged();
                }
            }
            evaluateDualPane(getResources().getConfiguration());
            _appMenu.updateMenuUi();
        }
    }
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	System.out.println("MAINACTIVITY: activity result recv R_OK=" +Activity.RESULT_OK + " reqCodes: PostThread OTV PR" + ThreadListFragment.POST_NEW_THREAD + " " + ThreadListFragment.OPEN_THREAD_VIEW + " " + ThreadViewFragment.POST_REPLY + " data: " + requestCode + " " + resultCode);
    	if (requestCode == ThreadListFragment.OPEN_PREFS)
    	{
    		
    		if (resultCode == PreferenceView.RESTART_APP)
    		{
    			restartApp();
		        return;
    		}
    		
    		if (resultCode == PreferenceView.OPEN_BROWSER_ZOOM_SETUP)
    		{
    			onPostResume = OnPostResume.OPEN_BROWSER_ZOOM_SETUP;
		        return;
    		}
    	}
    	if (requestCode == ThreadListFragment.POST_NEW_THREAD)
    	{
    		if (resultCode == Activity.RESULT_OK)
            {
                // read the resulting thread id from the post
                // int PQPID = data.getExtras().getInt("PQPID");
    			Toast.makeText(getApplicationContext(), "Your new thread will be opened after it has posted.", Toast.LENGTH_LONG).show();
                // openThreadViewPQPRoot(PQPID);
            }
    	}
    	if (requestCode == ThreadViewFragment.POST_REPLY)
    	{
            if (resultCode == Activity.RESULT_OK)
            {
                // read the resulting thread id from the post
            	// this is either the id of your new post or the id of the post your replied t
                
                // ThreadViewFragment TVf = (ThreadViewFragment)getSupportFragmentManager().findFragmentById(R.id.singleThread);
                if (_threadView != null)
                	_threadView.onActivityResult(requestCode, resultCode, data);
            }
        }

    }
    
    public void addToThreadIdBackStack (int threadId)
    {
    	_threadIdBackStack.add(threadId);
    }
    public void resetThreadIdBackStack ()
    {
    	_threadIdBackStack = new ArrayList<Integer>();
    }
	
    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch(keycode) {
            case KeyEvent.KEYCODE_MENU:
                toggleMenu();
                return true;
        }

        return super.onKeyDown(keycode, e);
    }
    
	// back button overriding
	@Override
	public void onBackPressed() {
		
		if (isMenuOpen())
		{
			closeMenu();
		}
		else if (mPopupBrowserOpen)
    	{
			if ((mPBfragment != null) && (mPBfragment.mWebview.canGoBack()))
				mPBfragment.mWebview.goBack();
			else
				closeBrowser();
    	}
		else if (_nextBackQuitsBecauseOpenedAppViaIntent && !getDualPane ())
		{
			_nextBackQuitsBecauseOpenedAppViaIntent = false;
			_threadList._nextBackQuitsBecauseOpenedAppViaIntent = false;
			super.onBackPressed();
		}
		else if (_threadIdBackStack.size() > 0)
		{
			this.openThreadViewAndSelectWithBackStack(_threadIdBackStack.get(_threadIdBackStack.size() -1));
			_threadIdBackStack.remove(_threadIdBackStack.size() -1);
		}
		else if (getThreadViewFullScreen())
		{
			// _threadView.toggleFullScreen();
		}
		else if (getSliderOpen() && !getDualPane())
		{
			_tviewFrame.closeLayer(true);
            annoyThreadViewClose();
		}
		else if (_sresFrame.isOpened())
		{
			_sresFrame.closeLayer(true);
		}
        else if ((_currentFragmentType == CONTENT_FRONTPAGE) && (_fpBrowser != null) && (isArticleOpen())) {
            closeArticleViewer();
        }
        else if ((_currentFragmentType == CONTENT_FRONTPAGE) && (_fpBrowser != null) && (_fpBrowser.mWebview.canGoBack()))
        {
             _fpBrowser.mWebview.goBack();
        }
		else if (_currentFragmentType != CONTENT_THREADLIST)
		{
			setContentTo(CONTENT_THREADLIST);
		}
		else
		{
			super.onBackPressed();
		}
	}
		
	// VOLUME KEY SCROLLING
	@Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
		/*
		ThreadViewFragment TVfragment = (ThreadViewFragment)getSupportFragmentManager().findFragmentById(R.id.singleThread);
        
    	FragmentPagerAdapter a = (FragmentPagerAdapter) mPager.getAdapter();
		ThreadListFragment TLfragment = (ThreadListFragment) a.instantiateItem(mPager, 1);
        */
        Boolean handleVolume = _prefs.getBoolean("useVolumeButtons", true);
        
        // do not do volume scroll with open web browser
        if (handleVolume && !mPopupBrowserOpen)
        {
            if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP)
            {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    if ((_threadView != null) && (getDualPane() || getSliderOpen())) _threadView.adjustSelected(-1);
                    else if (_sresFrame.isOpened()) { _searchResults.adjustSelected(-1); }
                    else if ((_threadList != null) && (_currentFragmentType == CONTENT_THREADLIST)) _threadList.adjustSelected(-1);
                    else if ((_messageList != null) && (_currentFragmentType == CONTENT_MESSAGES)) _messageList.adjustSelected(-1);
                }
                return true;
            }
            else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN)
            {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                {
                	if ((_threadView != null) && (getDualPane() || getSliderOpen())) _threadView.adjustSelected(1);
                	else if (_sresFrame.isOpened()) { _searchResults.adjustSelected(1); }
                    else if ((_threadList != null) && (_currentFragmentType == CONTENT_THREADLIST)) _threadList.adjustSelected(1);
                    else if ((_messageList != null) && (_currentFragmentType == CONTENT_MESSAGES)) _messageList.adjustSelected(1);
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
	
	/*
	 * CHECK FOR SHACKMESSAGES
	 */
	class CheckForSMTask extends AsyncTask<String, Void, Integer>
	{
	    Exception _exception;
	    String username;
	    String text;
	    int nlsid;
	    
        @Override
        protected Integer doInBackground(String... params)
        {
            try
            {
            	boolean verified = _prefs.getBoolean("usernameVerified", false);
                if (verified)
                {
	            	ArrayList<Message> msgs = ShackApi.getMessages(0, MainActivity.this);
	            	int unreadCount = 0;
	            	for (int i = 0; i < msgs.size(); i++)
	            	{
	            		if (msgs.get(i).getRead() == false)
	            		{
	            			if (unreadCount == 0)
	            			{
		            			username = msgs.get(i).getUserName();
		            			text = msgs.get(i).getRawContent();
		            			nlsid = msgs.get(i).getMessageId();
	            			}
	            			unreadCount++;
	            		}
	            	}
	            	return unreadCount;
            	}
                return 0;
            }
            catch (Exception e)
            {
                Log.e("shackbrowse", "Error getting sms", e);
                _exception = e;
                return 0;
            }
        }
        
        @Override
        protected void onPostExecute(Integer result)
        {
            if (_exception != null)
            {
            	System.out.println("SMget: err");
                ErrorDialog.display(MainActivity.this, "Error", "Error getting SMs:\n" + _exception.getMessage());
            }
            else if (result == null)
            {
            	System.out.println("SMget: err");
                ErrorDialog.display(MainActivity.this, "Error", "Unknown SM-related error.");
            }
            else
            {
            	int lastNotifiedId = Integer.parseInt(_prefs.getString("GCMShackMsgLastClickedId", "0"));
            	if (nlsid > lastNotifiedId)
            	{
            		text = PostFormatter.formatContent("", text, null, false, false).toString();
	            	if (result == 1)
	            		sendSMBroadcast(username, text, nlsid, false, 1);
	            	else if (result > 1)
	            		sendSMBroadcast("multiple", "", nlsid, true, result);
            	}
            }
        }
	}
	
	/*
	 * LIMES
	 */
	class LimeTask extends AsyncTask<String, Void, String[]>
	{
	    Exception _exception;
	    
        @Override
        protected String[] doInBackground(String... params)
        {
            try
            {
                if (_prefs.getBoolean("enableDonatorFeatures", false) && !_prefs.getString("userName", "").equals("")) {
                    ShackApi.putDonator(((_prefs.getString("limeUsers", "").toLowerCase().contains(_prefs.getString("userName", "").toLowerCase()) || _prefs.getString("quadLimeUsers", "").toLowerCase().contains(_prefs.getString("userName", "").toLowerCase()) || _prefs.getString("goldLimeUsers", "").toLowerCase().contains(_prefs.getString("userName", "").toLowerCase())) && !_prefs.getString("userName", "").equals("")), _prefs.getString("userName", ""));
                }
            	return ShackApi.getLimeList();
            }
            catch (Exception e)
            {
                Log.e("shackbrowse", "Error getting limes", e);
                _exception = e;
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(String[] result)
        {
            if (_exception != null)
            {
            	System.out.println("limeget: err");
                ErrorDialog.display(MainActivity.this, "Error", "Error getting limes:\n" + _exception.getMessage());
            }
            else if (result == null)
            {
            	System.out.println("limeget: err");
                ErrorDialog.display(MainActivity.this, "Error", "Unknown lime-related error.");
            }
            else
            {
            	SharedPreferences.Editor editor = _prefs.edit();
            	editor.putString("limeUsers", result[0]);
                editor.putString("goldLimeUsers", result[1]);
                editor.putString("quadLimeUsers", result[2]);
                editor.apply();
            }
        }
	}
	
	/*
	 * Version Check
	 */
	class VersionTask extends AsyncTask<String, Void, String>
	{
	    Exception _exception;
	    
        @Override
        protected String doInBackground(String... params)
        {
            try
            {
            	return ShackApi.getVersion();
            }
            catch (Exception e)
            {
            	_exception = e;
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(String result)
        {
            if ((_exception != null) || (result == null))
            {
                if (mActivityAvailable) {
                    MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(MainActivity.this);
                    builder.setTitle("Woggle Offline");
                    builder.setMessage("Woggle servers are down. ShackBrowse may not work properly. Try again later.");
                    builder.setCancelable(false);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    });
                    builder.setNegativeButton("Deal with brokenness", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
                    builder.create().show();
                }
            }
            else
            {
            	final String parts[] = result.split(Pattern.quote(" "));
            	if (parts.length > 0)
            	{
            		
            		if (parts[0].equals("d"))
            		{
                        if (mActivityAvailable) {
                            MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(MainActivity.this);
                            builder.setTitle("ShackBrowse Offline");
                            builder.setCancelable(false);
                            builder.setMessage("ShackBrowse is currently unavailable. Try again later.");
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    finish();
                                }
                            });
                            builder.create().show();
                        }
            		}
            		if ((parts[0].equals("f") || parts[0].equals("u")) && (parts.length > 1))
            		{
            			String thisversion = mVersion;

            			if (!parts[1].toLowerCase().contains(thisversion.toLowerCase()))
            			{
            				// opt out
            				if (_prefs.getString("ignoreNewVersion", "").equalsIgnoreCase(parts[1].toLowerCase()) && (parts[0].equals("u")))
            					return;
                            if (mActivityAvailable) {
                                MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(MainActivity.this);
                                builder.setTitle("ShackBrowse Version");
                                String versExp = "\nYour Version: " + thisversion + "\nNew: " + parts[1];
                                builder.setMessage(parts[0].equals("f") ? "ShackBrowse must update." + versExp : "A new version of ShackBrowse is available!" + versExp);
                                builder.setCancelable(false);
                                builder.setPositiveButton("Update Now", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        final String appPackageName = (parts.length > 2) ? parts[2] : getPackageName(); // getPackageName() from Context or Activity object
                                        try {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                                        } catch (android.content.ActivityNotFoundException anfe) {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
                                        }
                                        if (parts[0].equals("f"))
                                            finish();
                                    }
                                });
                                if (parts[0].equals("f")) {
                                    builder.setNegativeButton("Close App", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            finish();
                                        }
                                    });
                                } else {
                                    builder.setNegativeButton("Not Now", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {

                                        }
                                    });
                                    builder.setNeutralButton("Never", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            Editor edit = _prefs.edit();
                                            edit.putString("ignoreNewVersion", parts[1]);
                                            edit.apply();
                                        }
                                    });
                                }
                                builder.create().show();
                            }
            			}
            		}
            	}
            }
        }
	}
	
	/*
	 * Analytics
	 */
	
	public void track (String category, String action, String label)
	{
		_analytics  = _prefs.getBoolean("analytics", true);
		if (_analytics)
		{
			System.out.println("ANALYTICS: track " + category + action + label);
			// myTracker.sendEvent(category, action, label, null);
		}
	}
	public void trackScreen (String screen)
	{
		_analytics  = _prefs.getBoolean("analytics", true);
		if (_analytics)
		{
			System.out.println("ANALYTICS: screen " + screen);
			// myTracker.sendView(screen);
		}
	}
	
	/*
	 * INTENTS (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onNewIntent(android.content.Intent)
	 */
	
	@Override
	protected void onNewIntent(Intent intent) {
	    setIntent(intent);
	    onPostResumeIntent = intent;
	    onPostResume = OnPostResume.HANDLE_INTENT;
	}
	
	public static final int CANNOTHANDLEINTENT = 0;
	public static final int CANHANDLEINTENT = 1;
	public static final int CANHANDLEINTENTANDMUSTSETNBQBAOVI = 2;
	private int canHandleIntent(Intent intent) {
		// intent stuff
        String action = intent.getAction();
        String type = intent.getType();
        Uri uri = intent.getData();
        
        if (Intent.ACTION_SEND.equals(action) && type != null)
        {
            if ("text/plain".equals(type))
            {
            	String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null)
                	return CANHANDLEINTENT;
            }
            else if (type.startsWith("image/"))
            {
            	Uri imageUri = (Uri)intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null)
                	return CANHANDLEINTENT;
            }
        }
        else if (Intent.ACTION_VIEW.equals(action) && uri != null)
    	{
    		String id = uri.getQueryParameter("id");
    		if (id == null)
    			return CANNOTHANDLEINTENT;
    		else
    			return CANHANDLEINTENTANDMUSTSETNBQBAOVI;
    	}
        // external search intent      
        else if (Intent.ACTION_SEARCH.equals(action))
        {
        	return CANHANDLEINTENT;
        }
        
        // notifications
        Bundle extras = intent.getExtras();
        if (extras != null)
        {
        	//  REPLY NOTIFICATIONS
	        if ((extras.containsKey("notificationOpenRList")) || (extras.containsKey("notificationOpenId")))
	        {
				if (extras.containsKey("notificationOpenRList"))
		        	return CANHANDLEINTENT;
				else if (extras.containsKey("notificationOpenId"))
		        	return CANHANDLEINTENT;
	        }
	        // VANITY NOTIFICATIONS
	        else if ((extras.containsKey("notificationOpenVList")) || (extras.containsKey("notificationOpenVanityId")))
	        {
				// open search
				if (extras.containsKey("notificationOpenVList"))
		        	return CANHANDLEINTENT;
				// open post
				else if (extras.containsKey("notificationOpenVanityId"))
		        	return CANHANDLEINTENT;
	        }
	        // KEYWORD NOTIFICATIONS
	        else if ((extras.containsKey("notificationOpenKList")) || (extras.containsKey("notificationOpenKeywordId")))
	        {
				// open search
				if (extras.containsKey("notificationOpenKList"))
		        	return CANHANDLEINTENT;
				// open post
				else if (extras.containsKey("notificationOpenKeywordId"))
		        	return CANHANDLEINTENT;
	        }
	        // SHACKSM NOTIFICATIONS
	        else if (extras.containsKey("notificationOpenMessages"))
				return CANHANDLEINTENT;
        }
		return CANNOTHANDLEINTENT;
	}
	private boolean handleIntent(Intent intent) {
		if (intent != null)
		{
			if ((canHandleIntent(intent) != CANNOTHANDLEINTENT) && (mPopupBrowserOpen))
				closeBrowser();
			
			// intent stuff
	        String action = intent.getAction();
	        String type = intent.getType();
	        Uri uri = intent.getData();
	
	        if (Intent.ACTION_SEND.equals(action) && type != null)
	        {
	        	// sent either text intent or image which should be uploaded to chattypics
	            if ("text/plain".equals(type))
	            {
	            	String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
	                if (sharedText != null) {
	                    openComposer(ThreadListFragment.POST_NEW_THREAD, sharedText);
	                    return true;
	                }
	            }
	            else if (type.startsWith("image/"))
	            {
	            	Uri imageUri = (Uri)intent.getParcelableExtra(Intent.EXTRA_STREAM);
	                if (imageUri != null) {
	                    // Update UI to reflect image being shared
	                	openComposerAndUploadImage(ThreadListFragment.POST_NEW_THREAD, imageUri);
	                	return true;
	                }
	            }
	        }
	        else if (Intent.ACTION_VIEW.equals(action) && uri != null)
	    	{
	        	// shack chatty id URL intent sent
	    		String id = uri.getQueryParameter("id");
	    		if (id == null)
	    		{
	    			ErrorDialog.display(this, "Error", "Invalid URL Found");
	    			return false;
	    		}
	    		else
	    		{
	    			_nextBackQuitsBecauseOpenedAppViaIntent = true;
	    			_threadList._nextBackQuitsBecauseOpenedAppViaIntent = true;
	    			openThreadViewAndSelect(Integer.parseInt(id));
	    			_tviewFrame.setSlidingEnabled(false);
	    			mFrame.setVisibility(View.GONE);
	    			return true;
	    		}
	    	}
	        // external search intent      
	        else if (Intent.ACTION_SEARCH.equals(action))
	        {
		        String query = intent.getStringExtra(SearchManager.QUERY);
		        final Bundle args = new Bundle();
		        args.putString("terms", query);
	        	openSearch(args);
	        	return true;
	        }
	        
	        // notifications
	        Bundle extras = intent.getExtras();
	        if (extras != null)
	        {
	        	for (String key: extras.keySet())
	        	{
	        	  Log.d ("wogglesb", key + " is a key in the bundle");
	        	}
	        	//  REPLY NOTIFICATIONS
		        if ((extras.containsKey("notificationOpenRList")) || (extras.containsKey("notificationOpenId")))
		        {
		        	String noteNLSID = Integer.toString(extras.getInt("notificationNLSID"));
		        	
		        	Editor editor = _prefs.edit();
		        	editor.putInt("GCMNoteCountReply", 0);
		        	editor.apply();
					
					if (extras.containsKey("notificationOpenRList"))
					{
						// open notes
						runOnUiThread(new Runnable(){
		
							@Override
							public void run() {
								if (mPopupBrowserOpen)
					        	{
					        		closeBrowser();
					        	}
					        	if (_tviewFrame.isOpened() && !getDualPane())
					        		_tviewFrame.closeLayer(true);
					        	if (_sresFrame.isOpened())
					        		_sresFrame.closeLayer(true);
								setContentTo(CONTENT_NOTIFICATIONS);
							}});
						
						return true;
					}
					else if (extras.containsKey("notificationOpenId"))
					{
						// search seen
			        	if (_searchResults != null && _searchResults._seen != null && _searchResults._seen._seenTable != null)
			        	{
				        	_searchResults._seen._seenTable.put(getResources().getString(R.string.search_repliestome).hashCode(), Integer.parseInt(noteNLSID));
				        	_searchResults._seen.store();
			        	}
			        	System.out.println("OPENINGrepl " + noteNLSID);
			        	openThreadViewAndSelectWithBackStack(Integer.parseInt(noteNLSID));
			        	return true;
					}
		        }
		        // VANITY NOTIFICATIONS
		        else if ((extras.containsKey("notificationOpenVList")) || (extras.containsKey("notificationOpenVanityId")))
		        {
		        	String noteNLSID = Integer.toString(extras.getInt("notificationNLSID"));
		        			System.out.println("RESETTING VANIY COUNT");
		        	Editor editor = _prefs.edit();
		        	editor.putInt("GCMNoteCountVanity", 0);
		        	editor.apply();
					
					// open search
					if (extras.containsKey("notificationOpenVList"))
					{
						// open notes
						runOnUiThread(new Runnable(){
		
							@Override
							public void run() {
								if (mPopupBrowserOpen)
					        	{
					        		closeBrowser();
					        	}
					        	if (_tviewFrame.isOpened() && !getDualPane())
					        		_tviewFrame.closeLayer(true);
					        	if (_sresFrame.isOpened())
					        		_sresFrame.closeLayer(true);
								setContentTo(CONTENT_NOTIFICATIONS);
							}});
						
						return true;
					}
					// open post
					else if (extras.containsKey("notificationOpenVanityId"))
					{
						// search seen
			        	if (_searchResults != null && _searchResults._seen != null && _searchResults._seen._seenTable != null)
			        	{
				        	_searchResults._seen._seenTable.put(getResources().getString(R.string.search_vanity).hashCode(), Integer.parseInt(noteNLSID));
				        	_searchResults._seen.store();
			        	}
			        	System.out.println("OPENINGvan " + noteNLSID);
			        	openThreadViewAndSelectWithBackStack(Integer.parseInt(noteNLSID));
			        	return true;
					}
		        }
		     // KEYWORD NOTIFICATIONS
		        else if ((extras.containsKey("notificationOpenKList")) || (extras.containsKey("notificationOpenKeywordId")))
		        {
		        	String noteNLSID = Integer.toString(extras.getInt("notificationNLSID"));
		        	Editor editor = _prefs.edit();
		        	editor.putInt("GCMNoteCount" + extras.getString("notificationKeyword").hashCode(), 0);
		        	editor.apply();
					
					// open search
					if (extras.containsKey("notificationOpenKList"))
					{
						// open notes
						runOnUiThread(new Runnable(){
		
							@Override
							public void run() {
								if (mPopupBrowserOpen)
					        	{
					        		closeBrowser();
					        	}
					        	if (_tviewFrame.isOpened() && !getDualPane())
					        		_tviewFrame.closeLayer(true);
					        	if (_sresFrame.isOpened())
					        		_sresFrame.closeLayer(true);
								setContentTo(CONTENT_NOTIFICATIONS);
							}});
						System.out.println("OPENING list");
						return true;
					}
					// open post
					else if (extras.containsKey("notificationOpenKeywordId"))
					{
						// search seen
			        	if (_searchResults != null && _searchResults._seen != null && _searchResults._seen._seenTable != null)
			        	{
				        	_searchResults._seen._seenTable.put(getResources().getString(R.string.search_vanity).hashCode(), Integer.parseInt(noteNLSID));
				        	_searchResults._seen.store();
			        	}
			        	System.out.println("OPENINGkeyw " + noteNLSID);
			        	openThreadViewAndSelectWithBackStack(Integer.parseInt(noteNLSID));
			        	return true;
					}
					else { System.out.println("doing nothing"); }
		        }
		        // SHACKSM NOTIFICATIONS
		        else if (extras.containsKey("notificationOpenMessages"))
		        {
		        	String noteNLSID = Integer.toString(extras.getInt("notificationNLSID"));
		        	// update local last seen
		        	Editor editor = _prefs.edit();
					editor.putString("GCMShackMsgLastClickedId", noteNLSID);
					editor.apply();
					
					// open msgs
					runOnUiThread(new Runnable(){
	
						@Override
						public void run() {
							if (mPopupBrowserOpen)
				        	{
				        		closeBrowser();
				        	}
				        	if (_tviewFrame.isOpened() && !getDualPane())
				        		_tviewFrame.closeLayer(true);
				        	if (_sresFrame.isOpened())
				        		_sresFrame.closeLayer(true);
							setContentTo(CONTENT_MESSAGES);
						}});
					
					return true;
					
		        }
	        }
		}
		return false;
	}

	
	public boolean isMessagesShowing() {
		if (_currentFragmentType == CONTENT_MESSAGES)
			return true;
		return false;
	}

	public void showFilters() {
		// redirect command to threadlist fragment
		_threadList.showFilters();
	}
	public void showKeywords() {
		if (!_enableDonatorFeatures )
		{
            MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(this);
	        builder.setTitle("Donator Feature");
	        builder.setMessage("Enable this feature by clicking the menu, \"Unlock DLC\" and \"Access Donator Features\".");
	        builder.setPositiveButton("Go Now", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					Intent i = new Intent(MainActivity.this, DonateActivity.class);
	                startActivityForResult(i, ThreadListFragment.OPEN_PREFS);
	            }
	        });
	        builder.setNegativeButton("Cancel", null);
	        builder.create().show();
	        return;
		}
		
		_threadList.showFiltWordList();
	}
	
	// seen posts
	class Seen 
	{
		private static final int SEEN_HISTORY = 2000;
		private String SEEN_FILE = "seendb.cache";
		private Hashtable<Integer, Integer> _seenTable = null;
		Seen ()
		{
			_seenTable = load();
		}
		protected Hashtable<Integer, Integer> getTable()
		{
			return _seenTable;
		}
	    protected Hashtable<Integer, Integer> load()
	    {
	        Hashtable<Integer, Integer> counts = new Hashtable<Integer, Integer>();
	
	        if (getFileStreamPath(SEEN_FILE).exists())
	        {
	            // look at that, we got a file
	            try {
	                FileInputStream input = openFileInput(SEEN_FILE);
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
	    void store() throws IOException
	    {
	        List<Integer> postIds = Collections.list(_seenTable.keys());
	        Collections.sort(postIds);
	        
	        // trim to last 1000 posts
	        if (postIds.size() > SEEN_HISTORY)
	            postIds.subList(postIds.size() - SEEN_HISTORY, postIds.size() - 1);
	
	        FileOutputStream output = openFileOutput(SEEN_FILE, Activity.MODE_PRIVATE);
	        try
	        {
	            DataOutputStream out = new DataOutputStream(output);
	            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
	
	            for (Integer postId : postIds)
	            {
	            	writer.write(postId + "=" + _seenTable.get(postId));
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
	
	
	private void upgradeDonatorPreferences() {
		// check for old style preferences used in shackbrowse v2
		if (
				(_prefs.getBoolean("enablePushNotificationsPref", false)) ||
				(_prefs.getBoolean("displayLolButton", false)) ||
				(_prefs.getBoolean("displayNWSSearch", false)) ||
				(_prefs.getBoolean("enableSaveSearch", false)) ||
				(_prefs.getBoolean("enableKeywordFilter", false)) ||
				(_prefs.getString("limeUsers", "").contains(_prefs.getString("userName", "")) && !_prefs.getString("userName", "").equals(""))
			)
		{
			Editor edit = _prefs.edit();
			edit.putBoolean("enableDonatorFeatures", true);
			edit.apply();
		}
	}
	
	protected void onSaveInstanceState(Bundle save) {
		  super.onSaveInstanceState(save);
	}
	
	// what happens when new post button is clicked in threadlist fragment or from intent
	public void newPost()
	{
		boolean verified = _prefs.getBoolean("usernameVerified", false);
        if (!verified)
        {
        	LoginForm login = new LoginForm(this);
        	login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {
				@Override
				public void onSuccess() {
					newPost();
				}

				@Override
				public void onFailure() {
				}
			});
        	return;
        }
        openComposer(ThreadListFragment.POST_NEW_THREAD, null);
	}
	
	/*
	 * OFFLINE THREAD CLOUD STUFF
	 * 
	 */
    public void cloudChoose()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        boolean verified = prefs.getBoolean("usernameVerified", false);
        if (!verified)
        {
        	LoginForm login = new LoginForm(this);
        	login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {
				@Override
				public void onSuccess() {
					cloudChoose();
				}

				@Override
				public void onFailure() {
				}
			});
        	return;
        }
        
        boolean cloudEnabled = (this)._prefs.getBoolean("enableCloudSync", true);
    	if (!cloudEnabled)
    	{
            MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(this);
            builder.setTitle("Enable Cloud Sync");
            builder.setMessage("Cloud sync is disabled. Enable?");
            builder.setPositiveButton("Enable Cloud Sync", new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
			        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
	    	        	SharedPreferences.Editor editor = prefs.edit();
	                	editor.putBoolean("enableCloudSync", true);
	                	editor.commit();
				}});
            builder.setNegativeButton("Cancel", null);
            AlertDialog alert = builder.create();
            alert.setCanceledOnTouchOutside(true);
            alert.show();
    	}
    	else
    	{
            MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(this);
	        builder.setTitle("Choose Cloud Action");
	        final CharSequence[] items = { "Change Sync Interval (<1kb)","Perform Cloud to Local Copy","Perform Local to Cloud Copy","Merge Cloud and Local","Disable Cloud Sync"};
	        builder.setItems(items, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int item) {
	            	if (item == 0)
	            	{
	            		cloudIntervalChoose();
	            	}
	                if (item == 1)
	                {
	                	_threadList._offlineThread.setVerboseNext();
	                	_threadList._offlineThread.triggerCloudToLocal();
	                }
	                if (item == 2)
	                {
	                	_threadList._offlineThread.setVerboseNext();
	                	_threadList._offlineThread.triggerLocalToCloud();
	                }
	                if (item == 3)
	                {
	                	_threadList._offlineThread.setVerboseNext();
	                	_threadList._offlineThread.triggerCloudMerge();
	                }
	                if (item == 4)
	                {
	                	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
	    	        	SharedPreferences.Editor editor = prefs.edit();
	                	editor.putBoolean("enableCloudSync", false);
	                	editor.commit();
	                }
	                }});
	        AlertDialog alert = builder.create();
	        alert.setCanceledOnTouchOutside(true);
	        alert.show();
    	}
    }
	protected void cloudIntervalChoose() {
        MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(this);
        builder.setTitle("Choose Cloud Interval");
        final CharSequence[] items = { "30 seconds","1 minute","2 minutes","5 minutes (default)","10 minutes"};
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
            	
            	int len = 30;
            	if (item == 1)
            		len = 60;
                if (item == 2)
                	len = 120;
                if (item == 3)
                	len = 300;
                if (item == 4)
                	len = 600;
                
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
	        	SharedPreferences.Editor editor = prefs.edit();
            	editor.putInt("cloudInterval", len);
            	editor.commit();
                }});
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(true);
        alert.show();
		
	}

    public String getCloudUsername()
    {
        String userName = _prefs.getString("userName", "");
        if ((userName.length() == 0) || !_prefs.getBoolean("usernameVerified", false))
        {
            return null;
        }
        return userName.trim();
    }
	
	public void showOnlyProgressBarFromPTRLibrary(boolean showOnlyProgressBarWithoutHeader)
	{
		// only do this hack if the PTR library did not start the refresh
		if (getRefresher() != null && !getRefresher().isRefreshing())
    	{
            // prevent unnecessary changes
            if (showOnlyProgressBarWithoutHeader != mSOPBFPTRL) {
                mSOPBFPTRL = showOnlyProgressBarWithoutHeader;
                // hack it so only the progress bar shows up, but do it in the same way the library does so it will be set up for next PTR
                final SmoothProgressBar ptr_bar = ((SmoothProgressBar) getRefresher().getHeaderView().findViewById(R.id.ptr_progress));
                View ptr_head = getRefresher().getHeaderView().findViewById(R.id.ptr_content);
                ptr_head.setAlpha(showOnlyProgressBarWithoutHeader ? 0f : 1.0f);
                ptr_head.setTranslationY(showOnlyProgressBarWithoutHeader ? -ptr_head.getHeight() : ptr_head.getHeight());

                Drawable chkrunning = ptr_bar.getIndeterminateDrawable();
                boolean useDelayedKill = true;
                if (chkrunning == null || !(chkrunning instanceof SmoothProgressDrawable) || ((SmoothProgressDrawable) chkrunning).isRunning() == false)
                    useDelayedKill = false;

                if (showOnlyProgressBarWithoutHeader) {
                    ptr_bar.setVisibility(View.VISIBLE);
                    getRefresher().getHeaderView().setVisibility(showOnlyProgressBarWithoutHeader ? View.VISIBLE : View.GONE);
                    getRefresher().getHeaderView().setAlpha(showOnlyProgressBarWithoutHeader ? 1.0f : 0f);
                    ptr_bar.setIndeterminate(showOnlyProgressBarWithoutHeader);
                } else {
                    if (useDelayedKill) {
                        ptr_bar.setSmoothProgressDrawableCallbacks(new SmoothProgressDrawable.Callbacks() {
                            @Override
                            public void onStop() {
                                ptr_bar.setVisibility(View.GONE);
                                getRefresher().getHeaderView().setVisibility(View.GONE);
                                getRefresher().getHeaderView().setAlpha(0f);
                                ptr_bar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_bar_centered));
                                ptr_bar.setIndeterminate(false);
                            }

                            @Override
                            public void onStart() {

                            }
                        });
                        ptr_bar.progressiveStop();
                    }
                    else
                    {
                        ptr_bar.setVisibility(View.GONE);
                        getRefresher().getHeaderView().setVisibility(View.GONE);
                        getRefresher().getHeaderView().setAlpha(0f);
                        ptr_bar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_bar_centered));
                        ptr_bar.setIndeterminate(false);
                    }

                }
                System.out.println("PROGBAR: " + (showOnlyProgressBarWithoutHeader ? " START " : " STOP"));
            }
    	}
	}
	public void showOnlyProgressBarFromPTRLibraryDeterminate(boolean show, int prog)
	{
		// only do this hack if the PTR library did not start the refresh
		if (!getRefresher().isRefreshing())
    	{
    		// hack it so only the progress bar shows up, but do it in the same way the library does so it will be set up for next PTR
    		ProgressBar ptr_bar = ((ProgressBar)getRefresher().getHeaderView().findViewById(R.id.ptr_progress));
			View ptr_head = getRefresher().getHeaderView().findViewById(R.id.ptr_content);
			ptr_head.setAlpha(show ? 0f : 1.0f);
			ptr_head.setTranslationY(show ? - ptr_head.getHeight() : ptr_head.getHeight());
			ptr_bar.setVisibility(show ? View.VISIBLE : View.GONE);
			ptr_bar.setIndeterminate(false);
			ptr_bar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_bar_states));
			ptr_bar.setProgress(prog);
			getRefresher().getHeaderView().setVisibility(show ? View.VISIBLE : View.GONE);
			getRefresher().getHeaderView().setAlpha(show ? 1.0f : 0f);
    	}
	}
	
	private boolean restoreCollapsed() {
		FileOutputStream _output = null;
		try
        {
			_output = openFileOutput(ThreadListFragment.COLLAPSED_CACHE_FILENAME, Activity.MODE_PRIVATE);
            DataOutputStream out = new DataOutputStream(_output);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
            
            // clear file
            writer.write("");
            writer.flush();
            _output.close();
            
            _threadList.reloadCollapsed();
        }
        catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(this);
	        builder.setTitle("Error Clearing Collapsed");
	        builder.setMessage("Clear Failure");
	        builder.setNegativeButton("Ok", null);
	        builder.create().show();
		}
		finally
		{
			_threadList.refreshThreads();
		}
		return false;
	}

	public void openBrowser(String... hrefs) { StatsFragment.statInc(this, "PopUpBrowserOpened"); openBrowser(false, hrefs); }
	public void openBrowserZoomAdjust() { openBrowser(true, (String[])null); }
	private void openBrowser(boolean showZoomSetup, String... hrefs) { openBrowser(false, showZoomSetup, hrefs); }
	public void openBrowserPhotoView(String... hrefs) { openBrowser(true, false, hrefs); }
	private void openBrowser(boolean showPhotoView, boolean showZoomSetup, String... hrefs) {
		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		Bundle args = new Bundle();
		args.putStringArray("hrefs", hrefs);
		if (showZoomSetup)
	    	args.putBoolean("showZoomSetup", true);

		if (showPhotoView)
			args.putBoolean("showPhotoView", true);
		
		mPBfragment = (PopupBrowserFragment)Fragment.instantiate(getApplicationContext(), PopupBrowserFragment.class.getName(), args );
		ft.add(R.id.browser_frame, mPBfragment, "pbfrag");
		ft.attach(mPBfragment);
		ft.commit();
		
		new anim(mBrowserFrame).toVisible();
		
	    mPopupBrowserOpen  = true;
	    
	    setTitleContextually();
	}
	
	private void restartBrowserWithZoom() {
		
		if (!mBrowserIsClosing)
		{
            mPBfragment = (PopupBrowserFragment)getFragment("pbfrag");

			// stop youtube playing
			mPBfragment.mWebview.loadUrl("");
			
			// close current browser fragment
			FragmentManager fm = getFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			ft.remove(mPBfragment);
			ft.detach(mPBfragment);
			ft.commit();
			
			// create new one
			Bundle args = new Bundle();
			args.putBoolean("showZoomSetup", true);
			mPBfragment = (PopupBrowserFragment)Fragment.instantiate(getApplicationContext(), PopupBrowserFragment.class.getName(), args );
			ft = fm.beginTransaction();
			ft.add(R.id.browser_frame, mPBfragment, "pbfrag");
			ft.attach(mPBfragment);
			ft.commit();
		}		
	}

    public Fragment getFragment(String tag)
    {
        FragmentManager fm = getFragmentManager();
        return fm.findFragmentByTag(tag);
    }
	private void closeBrowser() {
		closeBrowser(false, null, false);
	}
	private void closeBrowser(boolean immediate, final mAnimEnd onEnd, final boolean quiet) {
		
		if (!mBrowserIsClosing)
		{
            mPBfragment = (PopupBrowserFragment)getFragment("pbfrag");

			// stop youtube playing
            if (mPBfragment != null) {
                mPBfragment.mWebview.loadUrl("");


                // set up end call
                mAnimEnd closeAction = new mAnimEnd() {
                    @Override
                    public void end() {
                        FragmentManager fm = getFragmentManager();
                        FragmentTransaction ft = fm.beginTransaction();
                        ft.remove(mPBfragment);
                        ft.detach(mPBfragment);
                        mBrowserIsClosing = false;
                        ft.commit();
                        mPopupBrowserOpen = false;
                        if (!quiet)
                            setTitleContextually();

                        if (onEnd != null)
                            onEnd.end();

                        // setting this field first forces a refresh of the progress bar
                        mSOPBFPTRL = true;
                        showOnlyProgressBarFromPTRLibrary(false);
                    }
                };

                mBrowserIsClosing = true;
                if (immediate)
                    closeAction.end();
                else
                    new anim(mBrowserFrame).toInvisible().setEndCall(closeAction);
            }
		}		
	}
	
	/*
	 * Animation manager. Used for animations throughout app
	 */
	public interface mAnimEnd
	{
		public void end();
	}
	public class anim
	{
		private mAnimEnd mCallBack = null;
		private View mView;
		anim (View view)
		{
			mView = view;
		}
		public anim setEndCall(mAnimEnd cb)
		{
			mCallBack = cb;
			return this;
		}
		public anim toVisible()
		{
			if (mView != null)
			{
				mView.setAlpha(0f);
				mView.setVisibility(View.VISIBLE);
				mView.animate().alpha(1f).setDuration(mShortAnimationDuration).setListener(new AnimatorListener(){
		
					@Override
					public void onAnimationCancel(Animator animation) {
						// TODO Auto-generated method stub
						
					}
		
					@Override
					public void onAnimationEnd(Animator animation) {
						mView.setAlpha(1f);
						mView.setVisibility(View.VISIBLE);
						if (mCallBack != null)
							mCallBack.end();
					}
		
					@Override
					public void onAnimationRepeat(Animator animation) {
						// TODO Auto-generated method stub
						
					}
		
					@Override
					public void onAnimationStart(Animator animation) {					
					}});
			}
			return this;
		}
		public anim toInvisible()
		{
			if (mView != null)
			{
				mView.setAlpha(1f);
				mView.setVisibility(View.VISIBLE);
				mView.animate().alpha(0f).setDuration(mShortAnimationDuration).setListener(new AnimatorListener(){
		
					@Override
					public void onAnimationCancel(Animator animation) {
						// TODO Auto-generated method stub
						
					}
		
					@Override
					public void onAnimationEnd(Animator animation) {
						mView.setAlpha(0f);
						
						if (mCallBack != null)
							mCallBack.end();
						
						mView.setVisibility(View.GONE);
					}
		
					@Override
					public void onAnimationRepeat(Animator animation) {
						// TODO Auto-generated method stub
						
					}
		
					@Override
					public void onAnimationStart(Animator animation) {
						
					}});
			}
			return this;
		}
	}
	
	/*
	 * This annoying dialog pops up if you havent setup autozoom
	 */
	public void annoyBrowserZoomDialog()
	{
		if ((!_prefs.contains("browserImageZoom5")) && (!_prefs.getBoolean("neverShowAutoZoomAnnoy", false)))
		{
            StatsFragment.statInc(this, "AnnoyedByStartupZoomDialog");
            MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(this);
		    builder.setTitle("Set up Autozoom");
		    LayoutInflater annoyInflater = LayoutInflater.from(this);
	        View annoyLayout = annoyInflater.inflate(R.layout.dialog_nevershowagain, null);
	        final CheckBox dontShowAgain = (CheckBox) annoyLayout.findViewById(R.id.skip);
	        ((TextView)annoyLayout.findViewById(R.id.annoy_text)).setText("You don't seem to have set up image auto-zoom for the popup browser yet. Do so now?");
		    builder.setView(annoyLayout)
		    // Set the action buttons
		    .setPositiveButton("Set It Up", new DialogInterface.OnClickListener() {
		    	@Override
		    	public void onClick(DialogInterface dialog, int id) {
		    		openBrowserZoomAdjust();
		    	}
		     })
		 	.setNegativeButton("Not Now", new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int id) {
			    	if (dontShowAgain.isChecked())
			    	{
				    	Editor edit = _prefs.edit();
				        edit.putBoolean("neverShowAutoZoomAnnoy", true);
				        edit.commit();
			    	}
			    }
		 	});
		
		    AlertDialog dialog = builder.create();//AlertDialog dialog; create like this outside onClick
		    dialog.show();
		}
	}
	
	/*
	 * This allows app to receive messages from postqueueservice that a post successfully was submitted
	 * 
	 */
	public class PQPServiceReceiver extends BroadcastReceiver{
		 
        @Override
        public void onReceive(Context context, Intent intent) {
        	Bundle ext = intent.getExtras();
        	if (ext.getBoolean("isPRL"))
        	{
        		Toast.makeText(context.getApplicationContext(), "Post PRL'd. Will retry. (" + ext.getInt("remaining") + " remaining in queue)", Toast.LENGTH_SHORT).show();
        	}
        	else if (ext.getBoolean("wasRootPost", false))
        	{
        		final int finalid = ext.getInt("finalId");
        		_tviewFrame.postDelayed(new Runnable(){

					@Override
					public void run() {
						openThreadViewAndFave(finalid);
					}}, 8000);
        	}
        	else
        	{
        		if ((ext.getInt("remaining") > 0) || (ext.getBoolean("isMessage")))
        			Toast.makeText(context.getApplicationContext(), (ext.getBoolean("isMessage") ? "Sent ShackMessage" : "Posted reply") + " successfully." + ((ext.getInt("remaining") > 0) ? "(" + ext.getInt("remaining") + " remaining in queue)" : ""), Toast.LENGTH_SHORT).show();
	        	System.out.println("POSTQU: MAINACTIVITY RECV SIGNAL");
	        	if (_threadView != null)
	        		_threadView.updatePQPostIdToFinal(Integer.parseInt(Long.toString(ext.getLong("PQPId"))),ext.getInt("finalId"));
        	}
        }
    }
	
	public class NetworkConnectivityReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			
		}
		
	}

	public void openPostQueueManager() {
        MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(this);
	    builder.setTitle("Post Queue System");
	    final PostQueueDB pdb = new PostQueueDB(this);
		pdb.open();
		List<PostQueueObj> plist = pdb.getAllPostsInQueue(true);
		pdb.close();
		
	    builder.setMessage("Currently " + plist.size() + " posts in queue.");
	    // Set the action buttons
	    if (plist.size() > 0)
	    {
		    builder.setPositiveButton("Delete Queued Posts", new DialogInterface.OnClickListener() {
		    	@Override
		    	public void onClick(DialogInterface dialog, int id) {
		    		pdb.open();
		    		pdb.deleteAll();
		    		pdb.close();
		    	}
		     });
	    }
	 	builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int id) {
		    }
	 	});
	
	    AlertDialog dialog = builder.create();//AlertDialog dialog; create like this outside onClick
	    dialog.show();
		
	}

    /*

    Front Page Fragment Article Viewer
     */

    public void openInArticleViewer(String href)
    {
        StatsFragment.statInc(this, "ArticleOpened");
        if (_currentFragmentType != CONTENT_FRONTPAGE) {
            mArticleViewerIsOpen = true;
            _fpBrowser.mSplashSuppress = true;
            setContentTo(CONTENT_FRONTPAGE);

            _articleViewer.setFirstOpen(href);
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .show(_articleViewer)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .hide(_fpBrowser)
                    .commit();
        }
        else if (_currentFragmentType == CONTENT_FRONTPAGE) {
            mArticleViewerIsOpen = true;
            _articleViewer.mSplashSuppress = true;
            if (_articleViewer.mWebview != null)
                _articleViewer.mWebview.loadData("", "text/html", null);

            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .show(_articleViewer)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .hide(_fpBrowser)
                    .commit();

            _articleViewer.open(href);
        }
        mTitle = "Article";
        setTitleContextually();
    }

    public void closeArticleViewer()
    {
        if (_currentFragmentType == CONTENT_FRONTPAGE) {
            // showOnlyProgressBarFromPTRLibrary(false);
            mArticleViewerIsOpen = false;
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .hide(_articleViewer)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .show(_fpBrowser)
                    .commit();

            mTitle = "Frontpage";
            setTitleContextually();
        }
    }
    public boolean isArticleOpen()
    {
        return mArticleViewerIsOpen && (_currentFragmentType == CONTENT_FRONTPAGE);
    }

    /*
     * Loading Splash Fragment
     *
     */
    public void showLoadingSplash()
    {
        System.out.println("SHOW:STATUSMSPLASHOPEN:" + mSplashOpen);
        if (mSplashOpen == false) {
            mSplashOpen = true;
            FragmentManager fM = getFragmentManager();
            FragmentTransaction fT = fM.beginTransaction();
                    fT.show(_loadingSplash);
                    fT.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);


            if (_currentFragmentType == CONTENT_FRONTPAGE) {
                        fT.hide(_articleViewer);
            }

            fT.hide(mCurrentFragment);
            fT.commit();

            _loadingSplash.randomizeTagline();
        }

    }

    public void hideLoadingSplash()
    {
        System.out.println("HIDE:STATUSMSPLASHOPEN:" + mSplashOpen);
        if (mSplashOpen == true) {
            mSplashOpen = false;
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .hide(_loadingSplash)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .commitAllowingStateLoss();

            if (isArticleOpen()) {
                fragmentManager.beginTransaction()
                        .hide(mCurrentFragment)
                        .show(_articleViewer)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commitAllowingStateLoss();
            } else if (_currentFragmentType == CONTENT_FRONTPAGE) {
                fragmentManager.beginTransaction()
                        .show(mCurrentFragment)
                        .hide(_articleViewer)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commitAllowingStateLoss();
            } else {
                fragmentManager.beginTransaction()
                        .show(mCurrentFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commitAllowingStateLoss();
            }
        }
    }

    public boolean isSplashOpen()
    {
        return mSplashOpen;
    }

    public void cleanUpViewer ()
    {
        if (isMenuOpen())
        {
            closeMenu();
        }
        else if (mPopupBrowserOpen)
        {
                closeBrowser();
        }
        else if (getSliderOpen() && !getDualPane())
        {
            _tviewFrame.closeLayer(true);
        }
        else if (_sresFrame.isOpened())
        {
            _sresFrame.closeLayer(true);
        }
        else if ((_currentFragmentType == CONTENT_FRONTPAGE) && (_fpBrowser != null) && (isArticleOpen())) {
            closeArticleViewer();
        }
    }

    public void annoyThreadViewClose()
    {
        if (!_prefs.getBoolean("seenTViewCloseAnnoyer", false)) {
            new MaterialDialog.Builder(this)
                    .title("Did you know?")
                    .content("You can close the reply list by swiping it to the right in addition to the back button and the arrow at the top.")
                    .positiveText("Confirm")
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            _prefs.edit().putBoolean("seenTViewCloseAnnoyer", true).apply();
                        }
                    })
                    .show();
        }
    }

}