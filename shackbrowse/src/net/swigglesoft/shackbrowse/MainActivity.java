package net.swigglesoft.shackbrowse;

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

import net.swigglesoft.AutocompleteProvider;
import net.swigglesoft.shackbrowse.ChangeLog.onChangeLogCloseListener;
import net.swigglesoft.shackbrowse.NetworkNotificationServers.OnGCMInteractListener;

import net.swigglesoft.shackbrowse.imgur.ImgurAuthURLHandling;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import fr.castorflex.android.smoothprogressbar.SmoothProgressDrawable;

import android.text.ClipboardManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
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
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.google.android.material.appbar.AppBarLayout;
import com.pierfrancescosoffritti.androidyoutubeplayer.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.player.listeners.YouTubePlayerInitListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.player.YouTubePlayerView;
import com.pierfrancescosoffritti.androidyoutubeplayer.player.listeners.YouTubePlayerFullScreenListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.ui.PlayerUIController;
import com.twitter.sdk.android.core.Twitter;

import static net.swigglesoft.shackbrowse.StatsFragment.statInc;

public class MainActivity extends AppCompatActivity implements ColorChooserDialog.ColorCallback
{
	public static final boolean LOLENABLED = true;
	public static boolean termsAndConditionsChecked = false;

	static final String PQPSERVICESUCCESS = "net.swigglesoft.PQPServiceSuccess";
	static final String CLICKLINK = "net.swigglesoft.ClickLink";

	FrameLayout mFrame;
    OfflineThread mOffline;
    
    public SearchResultFragment _searchResults;
    public ThreadViewFragment _threadView;
    public FrontpageBrowserFragment _fpBrowser;
	public FrontpageBrowserFragment _lolBrowser;
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
	private boolean mPopupBrowserOpen = false;
	private FrameLayout mBrowserFrame;
	private PopupBrowserFragment mPBfragment;
	private int mShortAnimationDuration;
	private MenuItem mFinder;
	private boolean mBrowserIsClosing = false;
	private MenuItem mHighlighter;
	private int onPostResume = OnPostResume.DO_NOTHING;
	private PQPServiceReceiver mPQPServiceReceiver;
	private ClickLinkReceiver mClickLinkReceiver;
	private NetworkConnectivityReceiver mNetworkConnectivityReceiver;
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
	private String mBrowserPageTitle;
	private String mBrowserPageSubTitle;

	//FIREBASE 	private FirebaseAnalytics mFirebaseAnalytics;
	private YouTubePlayerView mYoutubeView;
	private boolean mYoutubeFullscreen = false;
	private Toolbar mToolbar;
	private boolean mEnableAutoHide = true;
	private boolean mStupidElectrolyOption = false;
	private boolean mStupidFastzoopOption = false;
	public SmoothProgressBar mProgressBar;
	private long mTimeStartedToShowSplash = 0L;
	private YouTubePlayer mYoutubePlayer;
//	private NetworkEchoChamberServer mEchoAccess;
	public JSONArray mBlockList;
	private JSONArray mAutoChamber;
	private MaterialDialog mProgressDialog;
	public boolean mStupidDonkeyAnonOption = false;

	private Bundle savedInstanceState;


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		_prefs = PreferenceManager.getDefaultSharedPreferences(this);
		oprf(false);

		// sets theme
		mThemeResId = MainActivity.themeApplicator(this);
		this.savedInstanceState = savedInstanceState;

		super.onCreate(savedInstanceState);

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

		NetworkNotificationServers.getRegToken();

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

		// load loading splash
		this.setContentView(R.layout.main_splitview);

		// set up toolbar
		mToolbar = (Toolbar) findViewById(R.id.app_toolbar);
		setSupportActionBar(mToolbar);
		mProgressBar = (SmoothProgressBar) findViewById(R.id.app_progress);
		mProgressBar.bringToFront();
		mProgressBar.setSmoothProgressDrawableCallbacks(new SmoothProgressDrawable.Callbacks() {
			@Override
			public void onStop() {
				mProgressBar.setVisibility(View.INVISIBLE);
			}

			@Override
			public void onStart() {
				mProgressBar.setVisibility(View.VISIBLE);
			}
		});

		evaluateAutoHide();

		initFragments(savedInstanceState);

		mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime) * 1;

		getWindow().setBackgroundDrawable(new ColorDrawable(getThemeColor(this, R.attr.colorAppBG)));

		// set up preferences
		reloadPrefs();

		// Setup notification channels. Including the system one which is needed for post queue
		// notifications
		PushNotificationSetup.SetupNotificationChannels(this);

		// notifications registrator, works mostly automatically
		OnGCMInteractListener GCMlistener = new OnGCMInteractListener(){
			@Override	public void networkResult(String res)
			{
				// this allows the check mark to be placed when push notifications are automatically
				// enabled if the setting has never been touched
				Editor edit = _prefs.edit();
				if (res.contains("ok"))
				{
					edit.putBoolean("noteEnabled", true);
					_appMenu.updateMenuUi();
					System.out.println("PUSHREG: registered");
				}
				edit.apply();
			}
		};
		_GCMAccess = new NetworkNotificationServers(this, GCMlistener);

		// this pref is OPT OUT
		if (_prefs.getBoolean("noteEnabled", false))
    	{
			_GCMAccess.registerDeviceOnStartup();
    	}
		if (_prefs.contains("echoChamberBlockList"))
		{
			try {
				mBlockList = new JSONArray(_prefs.getString("echoChamberBlockList", "[]"));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else {
			mBlockList = new JSONArray();
		}

		// SM autocheck
		long updateInterval = Long.parseLong(_prefs.getString("PeriodicNetworkServicePeriod", "10800")); // DEFAULT 3 HR 10800L,  5 minutes 50-100mb, 10 minutes 25-50mb, 30mins 10-20mb, 1 hr 5-10mb, 3 hr 1-3mb, 6hr .5-1.5mb, 12hr .25-1mb
		PeriodicNetworkService.scheduleJob(this, updateInterval); // scheduleJob also checks preferences

		// notification database pruning
		NotificationsDB ndb = new NotificationsDB(this);
		ndb.open();
		ndb.pruneNotes();
		ndb.close();

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
            	hideKeyboard();
                setTitleContextually();
            }
        };

        // popup frame
        mBrowserFrame = (FrameLayout)findViewById(R.id.browser_frame);
        
        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        
       getSupportActionBar().setDisplayHomeAsUpEnabled(true);
       getSupportActionBar().setHomeButtonEnabled(true);
        
        mFrame = (FrameLayout)findViewById(R.id.content_frame);

        // set up favorites class
        mOffline = new OfflineThread(this);

		// clean up webview
		WebView wbv = new WebView(this);
		wbv.clearFormData();
		wbv.clearCache(true);
        
        
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
					// annoyBrowserZoomDialog();
				}
			});
            StatsFragment.statInc(this, "AppUpgradedToNewVersion");
        }
        
        // sync stats
        StatsFragment sfrag = new StatsFragment();
        sfrag.blindStatSync(this);
        
        // clean up postqueue
	    Intent msgIntent = new Intent(this, PostQueueService.class);
	    msgIntent.putExtra("appinit", true);
		PostQueueService.enqueueWork(this, msgIntent);

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

				// send message to thread view to finish loading
				_threadView._adapter.setViewIsOpened(true);
			}
			
			@Override
			public void onOpen() {
			}
			
			@Override
			public void onClosed() {
				setTitleContextually(); _threadView._adapter.setViewIsOpened(false);
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

		Twitter.initialize(this);

        ShackMessageCheck SMC = new ShackMessageCheck(this);
        SMC.frugalSMCheck();

        // external intent handling
        Intent intent = getIntent();
        if ((intent == null) || (canHandleIntent(intent) == CANNOTHANDLEINTENT))
        {
        	if (!clIsShowing)
        	{
	        	// no external intent, do annoyance dialogs
	        	// annoyBrowserZoomDialog();
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

	private void initFragments(Bundle savedInstanceState)
	{
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

		if (fm.findFragmentByTag(Integer.toString(CONTENT_LOLPAGE)) != null)
		{
			_lolBrowser = (FrontpageBrowserFragment)fm.findFragmentByTag(Integer.toString(CONTENT_LOLPAGE));
		}
		else
		{
			_lolBrowser = new FrontpageBrowserFragment();
		}

		_messageList = new MessageFragment();

		if (fm.findFragmentById(R.id.menu_frame) != null)
			_appMenu = (AppMenu) fm.findFragmentById(R.id.menu_frame);
		else
		{
			_appMenu = new AppMenu();
			// menu setup
			ft = getFragmentManager().beginTransaction();
			ft.replace(R.id.menu_frame, _appMenu, "appmenu");
			ft.attach(_appMenu);
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
			ft.attach(_lolBrowser);
			ft.commit();

			ft = fm.beginTransaction();
			ft.attach(_messageList);
			ft.commit();

			ft = fm.beginTransaction();
			ft.attach(mPBfragment);
			ft.commit();
		}
		_loadingSplash = new LoadingSplashFragment();
	}

	public static int getThemeResource(Activity mainactivity, int Rid)
	{
		TypedArray a = mainactivity.getTheme().obtainStyledAttributes(((MainActivity)mainactivity).mThemeResId, new int[] {Rid});
		int attributeResourceId = a.getResourceId(0, 0);
		//Drawable drawable = getResources().getDrawable(attributeResourceId);
		a.recycle();
		return attributeResourceId;
	}
    public static int getThemeColor(Context mainactivity, int Rid)
    {
        TypedArray a = mainactivity.getTheme().obtainStyledAttributes(((MainActivity)mainactivity).mThemeResId, new int[] {Rid});
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }
    public static int getThemeId(Activity context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String appTheme =  prefs.getString("appTheme", "1");
        int themeId;

        if (appTheme.equals("2")) {
            themeId = R.style.AppThemePurple;
            // lightBarColor = R.color.briefcasepurple;
            // darkBarColor = R.color.selected_postbg;
        }
        else if (appTheme.equals("0")) {
            themeId = R.style.AppThemeGreen;
            // lightBarColor = R.color.SBdark;
            // darkBarColor = R.color.SBvdark;
        }
        else if (appTheme.equals("3")) {
            themeId = R.style.AppThemeWhite;
        }
        else {
            themeId = R.style.AppTheme;
        }
        return themeId;
    }
	public static int themeApplicator(Activity context) {
	    int themeId = getThemeId(context);
        context.setTheme(themeId);

        int lightBarColor;
        int darkBarColor;

		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = context.getTheme();
		theme.resolveAttribute(R.attr.statusBarColor, typedValue, true);
		lightBarColor = typedValue.data;
		theme.resolveAttribute(R.attr.navigationBarColor, typedValue, true);
		darkBarColor = typedValue.data;

        //We need to manually change statusbar color, otherwise, it remains green.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            context.getWindow().setNavigationBarColor(darkBarColor);
	        context.getWindow().setStatusBarColor(lightBarColor);
        }
        return themeId;
    }
    public static void setupSwipeRefreshColors(Context context, SwipeRefreshLayout layout)
	{
		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = context.getTheme();
		theme.resolveAttribute(R.attr.colorAccent, typedValue, true);
		@ColorInt int color = typedValue.data;
		layout.setColorSchemeColors(color);
		layout.setProgressBackgroundColorSchemeResource(R.color.swipeRefreshBackground);
	}
	public void evaluateAutoHide()
	{
		mEnableAutoHide = (_prefs.getBoolean("enableAutoHide", true) && !isYTOpen());
		System.out.println("TOOLBAR AUTOHIDE EVAL" + mEnableAutoHide);
		AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
		if (!mEnableAutoHide)
		{
			params.setScrollFlags(0);  // clear all scroll flags
		}
		else
		{
			params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP | AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);  //set all scroll flags
		}
		mToolbar.setLayoutParams(params);

		if (mYoutubeFullscreen)
		{
			AppBarLayout appBarLayout = (AppBarLayout)findViewById(R.id.appbarlayout);
			appBarLayout.setExpanded(false, false);
			mToolbar.setVisibility(View.GONE);
		}
		else
		{
			mToolbar.setVisibility(View.VISIBLE);
			AppBarLayout appBarLayout = (AppBarLayout)findViewById(R.id.appbarlayout);
			appBarLayout.setExpanded(true, false);

		}
		findViewById(R.id.app_toolbar).requestLayout();
	}

    protected void setTitleContextually() {
		mToolbar.setSubtitle(null);
		if (mDrawerLayout.isDrawerOpen(_menuFrame))
		{
        	mToolbar.setTitle(mDrawerTitle);
        	mDrawerToggle.setDrawerIndicatorEnabled(true);
		}
		else if (mPopupBrowserOpen)
		{
			boolean browserZoomMode = false; boolean browserPhotoMode = false;
	        if ((mPBfragment != null) && (mPBfragment.mState == mPBfragment.SHOW_ZOOM_CONTROLS))
				browserZoomMode = true;

			if ((mPBfragment != null) && (mPBfragment.mState == mPBfragment.SHOW_PHOTO_VIEW))
				browserPhotoMode = true;
	        
	        if (!browserZoomMode && !browserPhotoMode) {
				if (!TextUtils.isEmpty(mBrowserPageTitle) && !TextUtils.isEmpty(mBrowserPageSubTitle)) {
					mToolbar.setTitle(mBrowserPageTitle);
					mToolbar.setSubtitle(mBrowserPageSubTitle);
				}
				else if (!TextUtils.isEmpty(mBrowserPageTitle)) {
					mToolbar.setTitle(mBrowserPageTitle);
				}
				else {
					mToolbar.setTitle(getResources().getString(R.string.browser_title));
				}
			}
			else if (browserPhotoMode) {
				mToolbar.setTitle(getResources().getString(R.string.browser_photo_title));
			}
	        else {
				mToolbar.setTitle(getResources().getString(R.string.browserZoom_title));
			}
			mDrawerToggle.setDrawerIndicatorEnabled(false);
		}
		else if (_tviewFrame.isOpened() && (_currentFragmentType == CONTENT_MESSAGES) && !getDualPane())
		{
        	mToolbar.setTitle(getResources().getString(R.string.message_title));
        	mDrawerToggle.setDrawerIndicatorEnabled(false);
		}
        else if (_tviewFrame.isOpened() && (_currentFragmentType == CONTENT_FRONTPAGE || _currentFragmentType == CONTENT_NOTIFICATIONS || _currentFragmentType == CONTENT_THREADLIST || _currentFragmentType == CONTENT_FAVORITES) && !getDualPane())
        {
        	mToolbar.setTitle(getResources().getString(R.string.thread_title));
        	mDrawerToggle.setDrawerIndicatorEnabled(false);
        }
        else if (_sresFrame.isOpened())
        {
        	mToolbar.setTitle(_searchResults._title);
        	mDrawerToggle.setDrawerIndicatorEnabled(false);
        }
        else if (_currentFragmentType == CONTENT_MESSAGES)
		{
			if (getMessageType())
				mToolbar.setTitle("SMsg Inbox");
			else
				mToolbar.setTitle("SMsg Sent");

			mDrawerToggle.setDrawerIndicatorEnabled(true);
		}
        else
        {
        	mToolbar.setTitle(mTitle);
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
			case R.id.menu_fastZoop:
				_threadView.fastZoop();
				AppBarLayout appBarLayout = (AppBarLayout)findViewById(R.id.appbarlayout);
				appBarLayout.setExpanded(false, true);
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
		    case R.id.menu_browserZoomOk:
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
	        	final Context con = this; final NotificationFragment fnf = nf;
	        	new MaterialDialog.Builder(this).title("Delete Notifications?")
				        .positiveText("Clear All").onPositive(new MaterialDialog.SingleButtonCallback()
		        {
			        @Override
			        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which)
			        {
				        final NotificationsDB ndb = new NotificationsDB(con);
				        ndb.open();
				        ndb.deleteAll();
				        ndb.close();
				        fnf.refreshNotes();
			        }
		        }).negativeText("Cancel").show();
	        	break;
	        case R.id.menu_refreshNotes:
	        	nf.refreshNotes();
	        	break;
            case R.id.menu_fpbrowserCopyURL:
            	if (_currentFragmentType == CONTENT_FRONTPAGE) {
					if ((_fpBrowser != null) && (!isArticleOpen()))
						_fpBrowser.copyURL();
					if ((_articleViewer != null) && (isArticleOpen()))
						_articleViewer.copyURL();
				}
				if (_currentFragmentType == CONTENT_LOLPAGE) {
					if (_lolBrowser != null)
						_lolBrowser.copyURL();
				}
                break;
            case R.id.menu_fpbrowserShare:
				if (_currentFragmentType == CONTENT_FRONTPAGE) {
					if ((_fpBrowser != null) && (!isArticleOpen()))
						_fpBrowser.shareURL();
					if ((_articleViewer != null) && (isArticleOpen()))
						_articleViewer.shareURL();
				}
				if (_currentFragmentType == CONTENT_LOLPAGE) {
					if (_lolBrowser != null)
						_lolBrowser.shareURL();
				}
                break;
            case R.id.menu_fprefresh:
				if (_currentFragmentType == CONTENT_FRONTPAGE) {
					if ((_fpBrowser != null) && (!isArticleOpen()))
						_fpBrowser.refresh();
					if ((_articleViewer != null) && (isArticleOpen()))
						_articleViewer.refresh();
				}
				if (_currentFragmentType == CONTENT_LOLPAGE) {
					if (_lolBrowser != null)
						_lolBrowser.refresh();
				}
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
	                    if ((newText.length() == 0) && (_threadView._highlight.length() > 0))
	                    {
		                    new AutocompleteProvider(MainActivity.this, "Highlighter", 5).addItem(_threadView._highlight);
	                    }
                        _threadView._highlight = newText;
                        _threadView._adapter.notifyDataSetChanged();
                    }
                    return false;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {
                	if (query.length() > 0)
	                {
		                new AutocompleteProvider(MainActivity.this, "Highlighter", 5).addItem(query);
	                }

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
					if (_threadView._highlight.length() > 0)
					{
						new AutocompleteProvider(MainActivity.this, "Highlighter", 5).addItem(_threadView._highlight);
					}
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


		MenuItem menuRefreshItem = menu.findItem(R.id.menu_refreshThreads);
		MenuItem menuNewpostItem = menu.findItem(R.id.menu_newPost);
	    mFinder = menu.findItem(R.id.menu_findOnPage);
        MenuItemCompat.setOnActionExpandListener(mFinder, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem arg0) {
	            // save to list of suggestions
	            if ((_threadList != null) && (_threadList._adapter != null))
	            {
		            System.out.println("AUTOCOMP: SAVE OMIAC");
		            _threadList.saveFinderQueryToList();
	            }

	            if ((_threadList != null) && (_threadList._adapter != null)) {
		            _threadList._filtering = false;
		            _threadList._adapter.getFilter().filter("");
	            }
	            // unhide other items
	            menuRefreshItem.setVisible(true);
	            menuNewpostItem.setVisible(true);
	            supportInvalidateOptionsMenu();
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem arg0) {
	            if ((_threadList != null) && (_threadList._adapter != null))
	            {
		            _threadList._filtering = true;
	            }

	            // hide new post and refresh
	            menuRefreshItem.setVisible(false);
	            menuNewpostItem.setVisible(false);
	            return true;
            }
        });

	    final SearchView sview2 = (SearchView)mFinder.getActionView();
	    sview2.setOnQueryTextListener(new SearchView.OnQueryTextListener(){

			@Override
			public boolean onQueryTextChange(String newText) {
				System.out.println("AUTOCOMP: OQTC: " + newText);
				if ((_threadList != null) && (_threadList._adapter != null))
				{
					String query = (((ThreadListFragment.ThreadLoadingAdapter.ThreadFilter)_threadList._adapter.getFilter()).lastFilterString);

					if (newText.length() == 0)
					{
						System.out.println("AUTOCOMP: SAVE OQTC");
						_threadList.saveFinderQueryToList();
					}
					_threadList._adapter.getFilter().filter(newText);
				}
				return false;
			}

			@Override
			public boolean onQueryTextSubmit(String query) {
				System.out.println("AUTOCOMP: SAVE OQS");
				_threadList.saveFinderQueryToList();

				// used to hide the keyboard
				sview.setVisibility(View.INVISIBLE);
				sview.setVisibility(View.VISIBLE);
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

        boolean showFPBrowserItems = (((_currentFragmentType == CONTENT_FRONTPAGE) || (_currentFragmentType == CONTENT_LOLPAGE)) && (dualPane || !areSlidersOpen)) && (!mPopupBrowserOpen) && (!isMenuOpen) && (!isResultsOpen);
        
        boolean browserZoomMode = false;
        if ((mPBfragment != null) && (mPBfragment.mState == mPBfragment.SHOW_ZOOM_CONTROLS))
        	browserZoomMode = true;
        
        menu.findItem(R.id.menu_refreshFav).setVisible(showFavItems && (!isMenuOpen));
        menu.findItem(R.id.menu_discardFav).setVisible(showFavItems && (!isMenuOpen));
        
        menu.findItem(R.id.menu_refreshThreads).setVisible(showTListItems);
        menu.findItem(R.id.menu_findOnPage).setVisible(showTListItems);

        // hack to do autocomplete sview2
	    AutoCompleteTextView searchAutoCompleteTextView = (AutoCompleteTextView) menu.findItem(R.id.menu_findOnPage).getActionView().findViewById(androidx.appcompat.R.id.search_src_text);
	    searchAutoCompleteTextView.setAdapter(new AutocompleteProvider(MainActivity.this, "Finder", 5).getSuggestionAdapter());
	    searchAutoCompleteTextView.setThreshold(0);

	    searchAutoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener()
	    {
		    @Override
		    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l)
		    {
			    searchAutoCompleteTextView.setText(searchAutoCompleteTextView.getAdapter().getItem(position).toString());
		    }
	    });

	    // hack to do autocomplete sview1
	    AutoCompleteTextView searchAutoCompleteTextView2 = (AutoCompleteTextView) menu.findItem(R.id.menu_findInThread).getActionView().findViewById(androidx.appcompat.R.id.search_src_text);
	    searchAutoCompleteTextView2.setAdapter(new AutocompleteProvider(MainActivity.this,"Highlighter",5).getSuggestionAdapter());
	    searchAutoCompleteTextView2.setThreshold(0);
	    searchAutoCompleteTextView2.setOnItemClickListener(new AdapterView.OnItemClickListener()
	    {
		    @Override
		    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l)
		    {
			    searchAutoCompleteTextView2.setText(searchAutoCompleteTextView2.getAdapter().getItem(position).toString());
		    }
	    });


	    if ((!showTListItems) && (mFinder.isActionViewExpanded()))
        	mFinder.collapseActionView();
        menu.findItem(R.id.menu_keywordFilter).setVisible(showTListItems);
        menu.findItem(R.id.menu_modtagFilter).setVisible(showTListItems);
        menu.findItem(R.id.menu_newPost).setVisible(showTListItems);
        menu.findItem(R.id.menu_restoreCollapsed).setVisible(showTListItems);
        
        menu.findItem(R.id.menu_searchGo).setVisible(showSearchItems);
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

        // refresh replies
        menu.findItem(R.id.menu_refreshReplies).setVisible(showReplyViewItems && !showMessageItems);
        if (dualPane && !mStupidElectrolyOption)
        	menu.findItem(R.id.menu_refreshReplies).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        else
        	menu.findItem(R.id.menu_refreshReplies).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        if (dualPane && mStupidElectrolyOption)
		{
			menu.findItem(R.id.menu_refreshReplies).setIcon(R.drawable.exo_icon_repeat_all);
		}
        else
		{
			menu.findItem(R.id.menu_refreshReplies).setIcon(R.drawable.ic_action_navigation_refresh);
		}

        menu.findItem(R.id.menu_fastZoop).setVisible(showReplyViewItems);
        if (mStupidFastzoopOption)
			menu.findItem(R.id.menu_fastZoop).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		else
			menu.findItem(R.id.menu_fastZoop).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        
        // these two are so complicated they are managed in the actual fragment
        menu.findItem(R.id.menu_favThread).setVisible((_threadView._showFavSaved) && showReplyViewItems);
        menu.findItem(R.id.menu_unfavThread).setVisible((_threadView._showUnFavSaved) && showReplyViewItems);
        
        menu.findItem(R.id.menu_browserClose).setVisible(mPopupBrowserOpen && !browserZoomMode);
        menu.findItem(R.id.menu_browserOpenExt).setVisible(mPopupBrowserOpen && !browserZoomMode);
        menu.findItem(R.id.menu_browserSettings).setVisible(mPopupBrowserOpen && !browserZoomMode);
        menu.findItem(R.id.menu_browserShare).setVisible(mPopupBrowserOpen && !browserZoomMode);
        menu.findItem(R.id.menu_browserChangeZoom).setVisible(mPopupBrowserOpen && !browserZoomMode);
        menu.findItem(R.id.menu_browserCopyURL).setVisible(mPopupBrowserOpen && !browserZoomMode);
        menu.findItem(R.id.menu_browserZoomOk).setVisible(mPopupBrowserOpen && browserZoomMode);
        
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
	public static final int CONTENT_ECHOPREFS = 9;
	public static final int CONTENT_LOLPAGE = 10;
	
	void setContentTo(int type)
	{
		setContentTo(type, null);
	}
	void setContentTo(int type, Bundle bundle) {

		AppBarLayout appBarLayout = (AppBarLayout)findViewById(R.id.appbarlayout);
		appBarLayout.setExpanded(true, true);

		Fragment fragment = null;
		if (bundle == null)
			bundle = new Bundle();
		
		if (type == CONTENT_THREADLIST)
		{
			mTitle = "Latest Chatty";
			if (isBeta)
				mTitle = "Beta " + mVersion.replace("Beta","");
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
			mTitle = "Starred Posts";
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
		if (type == CONTENT_ECHOPREFS)
		{
			mTitle = "Block List Preferences";
			fragment = (PreferenceFragmentEchoChamber)Fragment.instantiate(getApplicationContext(), PreferenceFragmentEchoChamber.class.getName(), new Bundle());
		}
        if (type == CONTENT_FRONTPAGE)
        {
            mTitle = "Frontpage";
            fragment = _fpBrowser;
            _articleViewer = (FrontpageBrowserFragment) Fragment.instantiate(getApplicationContext(), FrontpageBrowserFragment.class.getName(), new Bundle());
        }
		if (type == CONTENT_LOLPAGE)
		{
			mTitle = "LOLpage";
			fragment = _lolBrowser;
		}

		// turn off any refresher bars so the new fragment can work
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

            _fpBrowser.setFirstOpen("https://www.shacknews.com/");
        }
		if (type == CONTENT_LOLPAGE)
		{
			_lolBrowser.setFirstOpen("https://www.shacknews.com/tags-home");
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
		if (_currentFragmentType == CONTENT_LOLPAGE)
		{
			// kill weird ad crap running in background
			_lolBrowser.mWebview.loadData("", "text/html", null);
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

		if (isYTOpen())
		{
			mYoutubePlayer.pause();
		}
		
		mOffline.endCloudUpdates();

		// unregister receiver for pqpservice
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mPQPServiceReceiver);

		// unregister receiver for clicklink
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mClickLinkReceiver);
		
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

		// Show Terms and Conditions
		if (!termsAndConditionsChecked) {
			termsAndConditionsChecked = true;
			SharedPreferences sharedPreferences = getSharedPreferences(TermsAndConditionsDialogFragment.SHACKBROWSE_TCS, Context.MODE_PRIVATE);
			if(sharedPreferences.getInt(TermsAndConditionsDialogFragment.TERMS_AND_CONDITIONS, 0) < TermsAndConditionsDialogFragment.CURRENT_TERMS_VERSION) {
				TermsAndConditionsDialogFragment termsAndConditions = new TermsAndConditionsDialogFragment(MainActivity.this);
				Dialog dialog = termsAndConditions.onCreateDialog(savedInstanceState);
				dialog.show();
				((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
			}
		}

        StatsFragment.statInc(this, "AppOpened");
        mLastResumeTime = TimeDisplay.now();

		mOffline.startCloudUpdates();

		// register to receive information from PQPService
		IntentFilter filter = new IntentFilter(PQPSERVICESUCCESS);
        mPQPServiceReceiver = new PQPServiceReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(
				mPQPServiceReceiver,
				filter);

		// register to receive information from CustomURLSpan
		IntentFilter filter2 = new IntentFilter(CLICKLINK);
		mClickLinkReceiver = new ClickLinkReceiver();
		LocalBroadcastManager.getInstance(this).registerReceiver(
				mClickLinkReceiver,
				filter2);
        
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

        setTitleContextually();
	}
	
	public void toggleMenu() {
		if (!mDrawerLayout.isDrawerOpen(_menuFrame))
			openMenu();
		else
			closeMenu();
	}

	public void updateMenuStarredPostsCount()
	{
		_appMenu.setSmallText(8,Integer.toString(mOffline.getCount()));
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
	final static String CONTENT_TYPE_ID = "ctid";

	public void openComposerForReply (int returnResultType, Post parentPost, int contentTypeId)
	{
		Intent i = new Intent(this, ComposePostView.class);
        i.putExtra(THREAD_ID, parentPost.getPostId());
        i.putExtra("parentAuthor", parentPost.getUserName());
        i.putExtra("parentContent", parentPost.getContent());
        i.putExtra("parentDate", parentPost.getPosted());
        i.putExtra(CONTENT_TYPE_ID, contentTypeId);
        startActivityForResult(i, returnResultType);
	}

	public void openComposerForMessageReply (int returnResultType, Post parentPost, String messageSubject, Boolean moderationReport)
	{
		Intent i = new Intent(this, ComposePostView.class);
		i.putExtra("mode", "message");
		i.putExtra("parentAuthor", parentPost.getUserName());
		i.putExtra("parentContent", parentPost.getCopyText());
		i.putExtra("messageSubject", messageSubject);
		i.putExtra("moderationReport", moderationReport);
		startActivityForResult(i, returnResultType);
	}
	public void openComposerForMessageReply (int returnResultType, Post parentPost, String messageSubject)
	{
		openComposerForMessageReply(returnResultType, parentPost, messageSubject, false);
	}

	public void openNewMessageForReportingPost (final String username, final String subject, final String content) {
		boolean verified = _prefs.getBoolean("usernameVerified", false);
		if (!verified) {
			ErrorDialog.display(this, "Login", "You must be logged in to report a user/post.");
			LoginForm login = new LoginForm(this);
			login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {
				@Override
				public void onSuccess() {
					openNewMessageForReportingPost(username, subject, content);
				}

				@Override
				public void onFailure() {
				}
			});
			return;
		}

		Post post = new Post(0, username, content, null, 0, "", false);
		openComposerForMessageReply(ThreadViewFragment.POST_MESSAGE, post, subject, true);
	}

	public void openNewMessagePromptForSubject (final String username, final String subject, final String content)
	{
    	boolean verified = _prefs.getBoolean("usernameVerified", false);
        if (!verified)
        {
        	LoginForm login = new LoginForm(this);
        	login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {
				@Override
				public void onSuccess() {
					openNewMessagePromptForSubject(username, subject, content);
				}

				@Override
				public void onFailure() {
				}
			});
        	return;
        }
		if (subject != null) {
			Post post = new Post(0, username, content, null, 0, "", false);
			openComposerForMessageReply(ThreadViewFragment.POST_MESSAGE, post, subject);
			return;
		}
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
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
	public void openThreadViewAndFave(int faveThreadId)	{

		// sometimes this is called while the app is actually closed, and this causes a crash
		if (!_threadView.isDetached())
			openThreadView(faveThreadId, null, 0, null, true, 0, null, false, false);
	}
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

        if ((!view.isPostIdInAdapter(threadId) || expired) || (view._messageId != messageId) && view.isAdded())
        {
        	view._rootPostId = threadId;
        	view._messageId = messageId;
        	view._selectPostIdAfterLoading = selectPostIdAfterLoading;
        	view._autoFaveOnLoad = autoFaveOnLoad;
        	view._messageSubject = messageSubject;
        	
        	if (view._adapter != null)
        	{
        		if (!_dualPane) { view._adapter.setHoldPostExecute(true); }
	        	view._adapter.clear();
	        	view._adapter.triggerLoadMore();
        	}
        	
        	if (post != null)
	        {

		        if (view._adapter != null)
		            view.loadPost(post);
		        else
			        view._loadPostAfterAdapterReady = post;
	        }

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
        
        _tviewFrame.postDelayed(new Runnable() {
            @Override
            public void run() {
                _tviewFrame.openLayer(true);
            }
        }, 100l);
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
			if (_threadList._adapter.getCount() > checkIndexFirst && _threadList._adapter.getItem(checkIndexFirst).getThreadId() == rootId)
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
	    	mOffline.updateSingleThreadToDisk(_rootPostId);
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
	    setTitleContextually();
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
			setOrientLock();
		}
    
        if (_splitView == 0 || _currentFragmentType == CONTENT_FRONTPAGE || _currentFragmentType == CONTENT_PREFS || _currentFragmentType == CONTENT_STATS || _currentFragmentType == CONTENT_NOTEPREFS || _currentFragmentType == CONTENT_ECHOPREFS)
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

	private void setOrientLock()
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

	public boolean getDualPane () { return _dualPane; }
	public boolean getSliderOpen () { return _tviewFrame.isOpened(); }

    private void slideContentFrameBasedOnTView(float x, boolean isTView) {
        FrameLayout contentframe = (FrameLayout)findViewById(R.id.content_frame);

        // doesnt slide in dual pane
        if ((!getDualPane() && contentframe.getVisibility() == View.VISIBLE) || (!isTView && contentframe.getVisibility() == View.VISIBLE))
        {
        	contentframe.setTranslationX((1f - x) * (-.2f * contentframe.getWidth()));
        }
        if (!getDualPane() && _sresFrame.isOpened() && isTView)
        {
            _sresFrame.setTranslationX((1f - x) * (-.2f * contentframe.getWidth()));
        }
    }
	
	public void setDualPane (boolean dualPane)
	{
		RelativeLayout ytholder = (RelativeLayout)findViewById(R.id.tlist_ytholder);
		SlideFrame slide = (SlideFrame)findViewById(R.id.singleThread);
		SlideFrame sres = (SlideFrame)findViewById(R.id.searchResults);
		FrameLayout contentframe = (FrameLayout)findViewById(R.id.content_frame);

		// YOUTUBE STUFF
		if ((isYTOpen()) && (dualPane))
		{
			((RelativeLayout.LayoutParams)slide.getLayoutParams()).addRule(RelativeLayout.ABOVE, 0);
			((RelativeLayout.LayoutParams)sres.getLayoutParams()).addRule(RelativeLayout.ABOVE, R.id.tlist_ytholder);
			((RelativeLayout.LayoutParams)contentframe.getLayoutParams()).addRule(RelativeLayout.ABOVE, R.id.tlist_ytholder);
		}
		else if ((isYTOpen()) && (!dualPane))
		{
			((RelativeLayout.LayoutParams)slide.getLayoutParams()).addRule(RelativeLayout.ABOVE, R.id.tlist_ytholder);
			((RelativeLayout.LayoutParams)sres.getLayoutParams()).addRule(RelativeLayout.ABOVE, R.id.tlist_ytholder);
			((RelativeLayout.LayoutParams)contentframe.getLayoutParams()).addRule(RelativeLayout.ABOVE, R.id.tlist_ytholder);
		}
		else
		{
			((RelativeLayout.LayoutParams)slide.getLayoutParams()).addRule(RelativeLayout.ABOVE, 0);
			((RelativeLayout.LayoutParams)sres.getLayoutParams()).addRule(RelativeLayout.ABOVE, 0);
			((RelativeLayout.LayoutParams)contentframe.getLayoutParams()).addRule(RelativeLayout.ABOVE, 0);
		}

		// END YOUTUBE STUFF


		if (!dualPane)
		{
			// CHANGE TO NON DUAL PANE MODE 
			
			// sresults slider
			((RelativeLayout.LayoutParams)sres.getLayoutParams()).width = getScreenWidth();
			((RelativeLayout.LayoutParams)sres.getLayoutParams()).addRule(RelativeLayout.RIGHT_OF, 0);
    		

			((RelativeLayout.LayoutParams)contentframe.getLayoutParams()).width = getScreenWidth();
			((RelativeLayout.LayoutParams)contentframe.getLayoutParams()).addRule(RelativeLayout.RIGHT_OF,0);
			contentframe.requestLayout();

			// ytholder
			((RelativeLayout.LayoutParams)ytholder.getLayoutParams()).width = getScreenWidth();
			((RelativeLayout.LayoutParams)ytholder.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    		
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

			// ytholder
			// ytholder
			if (mYoutubeFullscreen)
				((RelativeLayout.LayoutParams)ytholder.getLayoutParams()).width = (int)(getScreenWidth());
			else
				((RelativeLayout.LayoutParams)ytholder.getLayoutParams()).width = (int)(getScreenWidth() * (1f / 3f));

			
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

	    		//ytholder
			    ((RelativeLayout.LayoutParams)ytholder.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
    		}
    		else
    		{
	       		((RelativeLayout.LayoutParams)slide.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
	    		((RelativeLayout.LayoutParams)slide.getLayoutParams()).addRule(RelativeLayout.RIGHT_OF, R.id.content_frame);
	    		((RelativeLayout.LayoutParams)contentframe.getLayoutParams()).addRule(RelativeLayout.RIGHT_OF, 0);
	    		((RelativeLayout.LayoutParams)sres.getLayoutParams()).addRule(RelativeLayout.RIGHT_OF, 0);

			    //ytholder
			    ((RelativeLayout.LayoutParams)ytholder.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_LEFT);
    		}

    		slide.setSlidingEnabled(false);
    		slide.openLayer(false);
    		contentframe.setVisibility(View.VISIBLE);

    		
    		if (sres.isOpened())
    		{
    			sres.openLayer(false);
    			// correct for overdraw optimization where this is hidden when occluded
    			sres.setVisibility(View.VISIBLE);
    		}
		}

		ytholder.requestLayout();
		sres.requestLayout();
		slide.requestLayout();
		contentframe.requestLayout();
		((RelativeLayout)contentframe.getParent()).requestLayout();

        contentframe.setTranslationX(0f);
        sres.setTranslationX(0f);

		_dualPane = dualPane;
		_threadView.updateThreadViewUi();
		if (_threadView._adapter != null)
			_threadView._adapter.setViewIsOpened(dualPane);

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
        AlertDialog.Builder builder = new AlertDialog.Builder(_context);
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
	    PostQueueService.enqueueWork(this, msgIntent);
	    // startService(msgIntent);


	    // oprf
	    if (_prefs.getBoolean("oprf", false)) { System.out.println("oprf true"); finish(); }
    }

    public void restartApp()
    {
        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage( getBaseContext().getPackageName() );
        finish();
        startActivity(i);
        return;
    }

    public void oprf(boolean set)
    {
	    Editor edit = _prefs.edit();
	    edit.putBoolean("oprf",set);
	    edit.apply();
    }

    public void reloadPrefs()
    {
        if (_prefs != null) {
            _analytics = _prefs.getBoolean("analytics", true);
            _zoom = Float.parseFloat(_prefs.getString("fontZoom", "1.0"));
            _showPinnedInTL = _prefs.getBoolean("showPinnedInTL", true);
            _swappedSplit = _prefs.getBoolean("swappedSplit", false);

			mStupidElectrolyOption = _prefs.getBoolean("electrolyoption", false);
			mStupidFastzoopOption = _prefs.getBoolean("fastzoopoption", false);
			mStupidDonkeyAnonOption = _prefs.getBoolean("donkeyanonoption", false);

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

            evaluateAutoHide();
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
		super.onActivityResult(requestCode,resultCode, data);
    }
    
    public void addToThreadIdBackStack (int threadId)
    {
    	_threadIdBackStack.add(threadId);
    }
    public void resetThreadIdBackStack ()
    {
    	_threadIdBackStack = new ArrayList<Integer>();
    }

    
	// back button overriding
	@Override
	public void onBackPressed() {

    	if (isYTOpen() && mYoutubeFullscreen)
	    {
	    	mYoutubeView.exitFullScreen();
	    }
		else if (isMenuOpen())
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
		else if ((_currentFragmentType == CONTENT_LOLPAGE) && (_lolBrowser != null) && (_lolBrowser.mWebview.canGoBack()))
		{
			_lolBrowser.mWebview.goBack();
		}
		else if (_currentFragmentType != CONTENT_THREADLIST)
		{
			setContentTo(CONTENT_THREADLIST);
		}
		else
		{
			if (_prefs.getBoolean("backButtonGuard", false))
			{
				new MaterialDialog.Builder(this)
						.title("Quit")
						.content("Really Quit?")
						.positiveText("Yes")
						.onPositive(new MaterialDialog.SingleButtonCallback()
						{
							@Override
							public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which)
							{
								finish();
							}
						})
						.negativeText("No")
						.show();
			}
			else
			{
				super.onBackPressed();
			}
		}
	}


	// VOLUME KEY SCROLLING
	@Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {

		if (event.getKeyCode() == KeyEvent.KEYCODE_MENU)
		{
			if (event.getAction() == KeyEvent.ACTION_DOWN)
				toggleMenu();

			return true;
		}
		/*
		ThreadViewFragment TVfragment = (ThreadViewFragment)getSupportFragmentManager().findFragmentById(R.id.singleThread);
        
    	FragmentPagerAdapter a = (FragmentPagerAdapter) mPager.getAdapter();
		ThreadListFragment TLfragment = (ThreadListFragment) a.instantiateItem(mPager, 1);
        */
        Boolean handleVolume = _prefs.getBoolean("useVolumeButtons", false);

        // do not do volume scroll with open web browser
        if (handleVolume && !mPopupBrowserOpen && !isYTOpen())
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
	 * INTENTS (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onNewIntent(android.content.Intent)
	 */
	
	@Override
	protected void onNewIntent(Intent intent) {
	    setIntent(intent);
	    onPostResumeIntent = intent;
	    onPostResume = OnPostResume.HANDLE_INTENT;
	    super.onNewIntent(intent);
	}
	
	public static final int CANNOTHANDLEINTENT = 0;
	public static final int CANHANDLEINTENT = 1;
	public static final int CANHANDLEINTENTANDMUSTSETNBQBAOVI = 2;

	private int canHandleIntent(Intent intent) {
		// intent stuff
        String action = intent.getAction();
        String type = intent.getType();
        Uri uri = intent.getData();
		String dataString = intent.getDataString();

		if (ImgurAuthURLHandling.isImgurAuthUrl(dataString)) {
			String response = ImgurAuthURLHandling.parseAuthUrl(dataString);
			runOnUiThread(() -> Toast.makeText(getApplicationContext(), response, Toast.LENGTH_SHORT).show());
			mCurrentFragment.onResume();
			return CANHANDLEINTENT;
		}
        
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
        else if (intent.getCategories() != null && intent.getCategories().contains("android.intent.category.NOTIFICATION_PREFERENCES"))
        {
	        return CANHANDLEINTENT;
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
	        if ((extras.containsKey("notificationOpenGList")) || (extras.containsKey("notificationOpenId")))
	        {
				if (extras.containsKey("notificationOpenGList"))
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
		    // PQS NOTIFICATIONS
	        else if (extras.containsKey("notificationOpenPostQueue"))
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
			String dataString = intent.getDataString();

			if (ImgurAuthURLHandling.isImgurAuthUrl(dataString)) {
				return true;
			}
	
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
	        else if (intent.getCategories() != null && intent.getCategories().contains("android.intent.category.NOTIFICATION_PREFERENCES"))
	        {
		        if (_prefs.getBoolean("noteEnabled", false))
		        {
			        cleanUpViewer();
			        setContentTo(MainActivity.CONTENT_NOTEPREFS);
		        }
		        return true;
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


	        
	        // INTRA-APP COMMS
	        Bundle extras = intent.getExtras();
	        if (extras != null)
	        {
	        	for (String key: extras.keySet())
	        	{
	        	  Log.d ("wogglesb", key + " is a key in the bundle");
	        	}

	        	//  REPLY NOTIFICATIONS
		        if ((extras.containsKey("notificationOpenGList")) || (extras.containsKey("notificationOpenId")))
		        {
		        	String noteNLSID = Integer.toString(extras.getInt("notificationNLSID"));
		        	
		        	Editor editor = _prefs.edit();
		        	editor.putInt("GCMNoteCountGeneral", 0);
		        	editor.apply();
					
					if (extras.containsKey("notificationOpenGList"))
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
		        // PQS NOTIFICATIONS
		        else if (extras.containsKey("notificationOpenPostQueue"))
		        {
			        openPostQueueManager();
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
	
	protected void onSaveInstanceState(Bundle save) {
		save.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
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
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
	private void openBrowser(boolean showZoomSetup, String... hrefs) {
		AppBarLayout appBarLayout = (AppBarLayout)findViewById(R.id.appbarlayout);
		appBarLayout.setExpanded(true, true);

		FragmentManager fm = getFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		Bundle args = new Bundle();
		args.putStringArray("hrefs", hrefs);

		mPBfragment = (PopupBrowserFragment) Fragment.instantiate(getApplicationContext(), PopupBrowserFragment.class.getName(), args);
		if (showZoomSetup) {
			args.putBoolean("showZoomSetup", true);
			mPBfragment.showZoom = true;
		}

		ft.add(R.id.browser_frame, mPBfragment, "pbfrag");
		ft.attach(mPBfragment);
		ft.commitAllowingStateLoss();

		new anim(mBrowserFrame).toVisible();

		mPopupBrowserOpen = true;

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
			mPBfragment.showZoom = true;
			ft = fm.beginTransaction();
			ft.add(R.id.browser_frame, mPBfragment, "pbfrag");
			ft.attach(mPBfragment);
			ft.commit();

			setTitleContextually();
		}		
	}

    public Fragment getFragment(String tag)
    {
        FragmentManager fm = getFragmentManager();
        return fm.findFragmentByTag(tag);
    }
	public void setBrowserTitle(String title)
	{
		if (!title.contentEquals("about:blank")) {
			mBrowserPageTitle = title;
			setTitleContextually();
		}
	}
	public void setBrowserSubTitle(String title)
	{
		mBrowserPageSubTitle = title;
		setTitleContextually();
	}
	protected void closeBrowser() {
		closeBrowser(false, null, false);
	}
	private void closeBrowser(boolean immediate, final mAnimEnd onEnd, final boolean quiet) {
		
		if (!mBrowserIsClosing)
		{
            mPBfragment = (PopupBrowserFragment)getFragment("pbfrag");
			mBrowserPageTitle = "";
			mBrowserPageSubTitle = "";

			// stop youtube playing
            if (mPBfragment != null) {
                mPBfragment.mWebview.loadUrl("about:blank");

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
		private ViewPropertyAnimator mAnimator;

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
					public void onAnimationCancel(Animator animation) {}
					@Override
					public void onAnimationEnd(Animator animation) {
						mView.setAlpha(0f);
						
						if (mCallBack != null)
							mCallBack.end();
						
						mView.setVisibility(View.GONE);
					}
					@Override
					public void onAnimationRepeat(Animator animation) {}
					@Override
					public void onAnimationStart(Animator animation) {}});
			}
			return this;
		}
		public anim toolBarUp()
		{
			if (mView != null)
			{
				if (mView instanceof Toolbar)
				{
					mAnimator = mView.animate().translationY(mView.getY() - mView.getHeight()).setInterpolator(new AccelerateInterpolator(2)).setListener(new AnimatorListener()	{
						@Override
						public void onAnimationCancel(Animator animation) {}
						@Override
						public void onAnimationEnd(Animator animation) {
							if (mCallBack != null)
								mCallBack.end();
						}
						@Override
						public void onAnimationRepeat(Animator animation) {}
						@Override
						public void onAnimationStart(Animator animation) {}
					});
					mAnimator.start();
				}
			}
			return this;
		}
		public anim toolBarDown()
		{
			if (mView != null)
			{
				if (mView instanceof Toolbar)
				{
					mAnimator = mView.animate().translationY(mView.getY() + mView.getHeight()).setInterpolator(new DecelerateInterpolator(2)).setListener(new AnimatorListener()	{
						@Override
						public void onAnimationCancel(Animator animation) {}
						@Override
						public void onAnimationEnd(Animator animation) {
							if (mCallBack != null)
								mCallBack.end();
						}
						@Override
						public void onAnimationRepeat(Animator animation) {}
						@Override
						public void onAnimationStart(Animator animation) {}
					});
					mAnimator.start();
				}
			}
			return this;
		}
	}

	
	/*
	 * This annoying dialog pops up if you havent setup autozoom
	 */
	public void annoyBrowserZoomDialog()
	{
		if ((!_prefs.contains("browserImageZoom5")) && (!_prefs.getBoolean("neverShowAutoZoomAnnoy2", false)))
		{
            StatsFragment.statInc(this, "AnnoyedByStartupZoomDialog");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    builder.setTitle("Set up Autozoom");
		    LayoutInflater annoyInflater = LayoutInflater.from(this);
	        View annoyLayout = annoyInflater.inflate(R.layout.dialog_nevershowagain, null);
	        final CheckBox dontShowAgain = (CheckBox) annoyLayout.findViewById(R.id.skip);
	        ((TextView)annoyLayout.findViewById(R.id.annoy_text)).setText("You don't seem to have set up image auto-zoom for the popup browser yet. Do so now?");
		    builder.setView(annoyLayout)
		    // Set the action buttons
		    .setPositiveButton("Set Up", new DialogInterface.OnClickListener() {
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
				        edit.putBoolean("neverShowAutoZoomAnnoy2", true);
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


    // this is a workaround for a crash because i couldnt get an activity from a view 100% in customurlspan
	public class ClickLinkReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle ext = intent.getExtras();
			String href = ext.getString("URL");

			MainActivity mAct = MainActivity.this;

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
					/*
					if (mAct.getSliderOpen() && !mAct.getDualPane())
					{
						mAct._tviewFrame.closeLayer(true);
					} else if (mAct._sresFrame.isOpened())
					{
						mAct._sresFrame.closeLayer(true);
					}
					mAct.openInArticleViewer(href);
					*/
					mAct.openBrowser(href);
					return;
				} else
				{
					Uri uri = Uri.parse(href);
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
				Uri u = Uri.parse(href);
				if (u.getScheme() == null)
				{
					u = Uri.parse("http://" + href);
				}
				Intent i = new Intent(Intent.ACTION_VIEW, u);
				mAct.startActivity(i);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
                    .commitAllowingStateLoss();
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
                    .commitAllowingStateLoss();

            _articleViewer.open(href);
        }
        mTitle = "Article";
        setTitleContextually();
    }

    public void closeArticleViewer()
    {
        if (_currentFragmentType == CONTENT_FRONTPAGE) {
            // showOnlyProgressBarFromPTRLibrary(false);
	        _articleViewer.open("about:blank");
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
    * PROGRESS BAR
     */

    public void startProgressBar ()
	{
		mProgressBar.setIndeterminate(true);
		mProgressBar.progressiveStart();
		// mProgressBar.setVisibility(View.VISIBLE);
	}
	public void stopProgressBar ()
	{
		mProgressBar.setIndeterminate(true);
		mProgressBar.progressiveStop();
		// mProgressBar.setVisibility(View.INVISIBLE);
	}

    /*
     * Loading Splash Fragment
     *
     */
    public void showLoadingSplash()
    {
        System.out.println("SHOW:STATUSMSPLASHOPEN:" + mSplashOpen);

        if (mSplashOpen == false) {
        	mTimeStartedToShowSplash = System.currentTimeMillis();
            mSplashOpen = true;
            FragmentManager fM = getFragmentManager();
            FragmentTransaction fT = fM.beginTransaction();
                    fT.show(_loadingSplash);
                    fT.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);


            if (_currentFragmentType == CONTENT_FRONTPAGE) {
                        fT.hide(_articleViewer);
            }
			if (mCurrentFragment != null)
                fT.hide(mCurrentFragment);
            fT.commit();

            _loadingSplash.randomizeTagline();
            _loadingSplash.showEcho();
        }

    }

    public void hideLoadingSplash()
    {
    	System.out.println("HIDE:STATUSMSPLASHOPEN:" + mSplashOpen);
        if (mSplashOpen == true) {

			long difference = System.currentTimeMillis() - mTimeStartedToShowSplash;
			long postDelay = 0L;
			if (difference < 500L)
				postDelay = 500L;

			// delay solves bug that happens if splash screen is only up for a few millis where content never shows
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
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
			}, postDelay);



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
	public void copyText(String text)
	{
		ClipboardManager clipboard = (ClipboardManager)getSystemService(Activity.CLIPBOARD_SERVICE);
		clipboard.setText(text);
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

	// COLOR PREFERENCE CHOOSER

	@Override
	public void onColorSelection(ColorChooserDialog dialog, int color) {
		// TODO
		System.out.println("COLORCHOOSE DONE" + color);
		Editor edit = _prefs.edit();
		edit.putInt("notificationColor", color);
		edit.commit();
	}

	@Override
	public void onColorChooserDismissed(@NonNull ColorChooserDialog dialog)
	{

	}

	// YOUTUBE PLAYER
	public void openYoutube(String url)
	{
		System.out.println("OPENING YT" + url);
		if (isYTOpen())
		{
			mYoutubeView.release();
			RelativeLayout ytHolder = (RelativeLayout) findViewById(R.id.tlist_ytholder);
			ytHolder.removeAllViews();
		}

		mYoutubeView = new YouTubePlayerView(this);
		RelativeLayout ytHolder = (RelativeLayout) findViewById(R.id.tlist_ytholder);
		ytHolder.addView(mYoutubeView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		mYoutubeView.setVisibility(View.VISIBLE);
		PlayerUIController puic = mYoutubeView.getPlayerUIController();

		Drawable myIcon = getResources().getDrawable(R.drawable.ic_action_content_clear);
		ImageView close = new ImageView(this);
		close.setImageResource(R.drawable.ic_action_content_clear);
		close.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				closeYoutube();
			}
		});
		puic.addView(close);

		mYoutubeView.addFullScreenListener(new YouTubePlayerFullScreenListener(){
			@Override
			public void onYouTubePlayerEnterFullScreen()
			{
				mYoutubeFullscreen = true;
				// ActionBar actionBar = mToolbar;
				// actionBar.hide();
				View decorView = getWindow().getDecorView();
				// Hide the status bar.
				int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
				decorView.setSystemUiVisibility(uiOptions);

				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

				resizeOtherContentHeightsForYoutube();
				evaluateAutoHide();
			}

			@Override
			public void onYouTubePlayerExitFullScreen()
			{
				mYoutubeFullscreen = false;
				// ActionBar actionBar = mToolbar;
				// actionBar.show();
				View decorView = getWindow().getDecorView();
				// Show the status bar.
				int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
				decorView.setSystemUiVisibility(uiOptions);

				setOrientLock();

				resizeOtherContentHeightsForYoutube();
				evaluateAutoHide();
			}
		});

		final String youtubeId = PopupBrowserFragment.getYoutubeId(url);
		final int youtubeTime = PopupBrowserFragment.getYoutubeTime(url);
		mYoutubeView.initialize(new YouTubePlayerInitListener() {
			@Override
			public void onInitSuccess(final YouTubePlayer initializedYouTubePlayer) {
				initializedYouTubePlayer.addListener(new AbstractYouTubePlayerListener() {
					@Override
					public void onReady() {
						initializedYouTubePlayer.loadVideo(youtubeId, youtubeTime);
						mYoutubePlayer = initializedYouTubePlayer;
					}
				});
			}
		}, true);

		new getYTTitleTask().execute(youtubeId);

		resizeOtherContentHeightsForYoutube();
		evaluateAutoHide();
	}

	public void closeYoutube()
	{

		if (mYoutubeFullscreen)
		{
			mYoutubeFullscreen = false;
			// ActionBar actionBar = mToolbar.show();
			View decorView = getWindow().getDecorView();
			// Show the status bar.
			int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
			decorView.setSystemUiVisibility(uiOptions);

			setOrientLock();

		}

		mYoutubeView.release();
		mYoutubeView.setVisibility(View.GONE);
		RelativeLayout ytHolder = (RelativeLayout) findViewById(R.id.tlist_ytholder);
		ytHolder.removeAllViews();

		resizeOtherContentHeightsForYoutube();
		evaluateAutoHide();
	}
	public boolean isYTOpen() { return ((mYoutubeView != null && mYoutubeView.getVisibility() == View.VISIBLE) ? true : false); }

	private void resizeOtherContentHeightsForYoutube()
	{
		setDualPane(_dualPane);
	}

	/*
	 * YT TITLE
	 */
	class getYTTitleTask extends AsyncTask<String, Void, String>
	{
		@Override
		protected String doInBackground(String... params)
		{
			return ShackApi.getYTTitle(params[0]);
		}
		@Override
		protected void onPostExecute(String result)
		{
			if (isYTOpen())
				mYoutubeView.getPlayerUIController().setVideoTitle(result);
		}
	}
	/*
	Blocklist
	 */
	public boolean isOnBlocklist (String username)
	{
		if (mBlockList == null) return false;
		try {
			for (int i = 0; i < mBlockList.length(); i++) {
				if (mBlockList.getString(i).equalsIgnoreCase(username)) return true;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	public String getFancyBlockList (boolean autoChamberList) {
		JSONArray json;
		if (autoChamberList)
		{
			json = mAutoChamber;
		}
		else
		{
			json = mBlockList;
		}

		String list = "";
		if ((json != null) && (json.length() > 0)) {
			try {
				for (int i = 0; i < json.length(); i++) {
					list = list + (i > 0 ? ", " : "") + json.getString(i);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else { list = "(no names on list)"; }
		return list;
	}
	public void blockUser (String username)
	{
		boolean echoPalatize = _prefs.getBoolean("echoPalatize", false);
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle((echoPalatize ? "Palatize" : "Remove") + " ALL posts from " + username + "?");
		String action = (echoPalatize ? "PALATIZE" : "REMOVE");
		builder.setMessage("This will "+action+" all posts from this user in future threads. You will need to refresh the list to see this change. " + (echoPalatize ? "" : "This will ALSO REMOVE any subthreads from and replies to this user. ") +  "This can be changed in the Settings -> Block List. Continue?");
		builder.setCancelable(true);
		builder.setPositiveButton((echoPalatize ? "Palatize" : "Block") + " User", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				mBlockList.put(username);
				Editor ed = _prefs.edit();
				ed.putBoolean("echoEnabled", true);
				ed.putString("echoChamberBlockList", mBlockList.toString());
				ed.commit();
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
			}
		});
		builder.create().show();
	}
}
