package net.woggle.shackbrowse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.woggle.shackbrowse.legacy.LegacyFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import static net.woggle.shackbrowse.StatsFragment.statInc;

public class ComposePostView extends AppCompatActivity {

	protected static final int SELECT_IMAGE = 0;
	protected static final int TAKE_PICTURE = 1;
    protected static final int SELECT_IMAGE_KITKAT = 3;
    protected static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 50;
	
	static final long MAX_SIZE_LOGGED_IN = 6 * 1024 * 1024;
	static final long MAX_SIZE_NOT_LOGGED_IN = 3 * 1024 * 1024;
	
	final String[] _tagLabels = { "r{red}r", "g{green}g", "b{blue}b", "y{yellow}y", "e[olive]e", "l[lime]l", "n[orange]n", "p[multisync]p", "/[italics]/", "b[bold]b",  "q[quote]q",  "s[small]s",  "_[underline]_",   "-[strike]-",  "spoilo[er]o"};
    final String[] _tags = {      "r{...}r",  "g{...}g",   "b{...}b",  "y{...}y",  "e[...]e",  "l[...]l",  "n[...]n",	  "p[...]p",      "/[...]/",         "b[...]b",   "q[...]q",     "s[...]s",   "_[...]_",     "-[...]-",	   "o[...]o"};
	
	private boolean _isNewsItem = false;
    private int _replyToPostId = 0;
	private MaterialDialog _progressDialog;
	
	float _zoom = 1.0f;
	
	Uri _cameraImage;
	
	SharedPreferences _prefs;
	private int _orientLock = 0;
	private int _forcePostPreview = 1;
    private int _extendedEditor = 1;
	private boolean _messageMode = false;
	protected boolean _preventDraftSave = false;
	private String _parentAuthorForDraft = "";
	private String _parentPostForDraft = "";
	private Drafts _drafts;
	private long _parentDateForDraft;
	
	private boolean _showViewPost = false;
	private String mParentPostContent;
	private String mParentAuthor;
	private boolean _landscape = false;
    private int mThemeResId;
    private Long mLastResumeTime = 0L;
    private boolean mEditBarEnabled;
	private String mCurrentPhotoPath;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        // prefs
        _prefs = PreferenceManager.getDefaultSharedPreferences(this);

        mThemeResId = MainActivity.themeApplicator(this);
        
        _forcePostPreview  = Integer.parseInt(_prefs.getString("forcePostPreview", "1"));

        _extendedEditor  = Integer.parseInt(_prefs.getString("extendedEditor", "1"));

        // grab the post being replied to, if this is a reply
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(MainActivity.THREAD_ID))
        {
            _replyToPostId = getIntent().getExtras().getInt(MainActivity.THREAD_ID);

            if (extras.containsKey(MainActivity.IS_NEWS_ITEM))
                _isNewsItem = extras.getBoolean(MainActivity.IS_NEWS_ITEM);
        }
        
        // drafts
        _drafts = new Drafts(this);
        
        // orientation
        doOrientation();
        
        if (_orientLock == 2 || _orientLock == 4 || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
        	setContentView(R.layout.edit_post_lollipop);
            _landscape  = true;
        }
        else
        {
        	setContentView(R.layout.edit_post_lollipop);
            _landscape  = false;
        }

        decideEditBar();

        // home button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // zoom handling
        _zoom = Float.parseFloat(_prefs.getString("fontZoom", "1.0"));
        EditText editBox = (EditText)findViewById(R.id.textContent);
        editBox.setTextSize(TypedValue.COMPLEX_UNIT_PX, editBox.getTextSize() * _zoom);
        
        editBox.setCustomSelectionActionModeCallback(new StyleCallback());
        

        
        // check for message reply
        if (extras != null && extras.containsKey("mode"))
        {
        	// this actually serves no purpose, but is useful to keep state in mind
            if (extras.getString("mode").equals("message"))
            {
            	_messageMode  = true;
            }
        }
        
        // setup buttons
        setupButtonBindings(extras);
        
        // handle text sharing intent
        if (extras != null && extras.containsKey("preText"))
        {
        	appendText(extras.getString("preText"));
        }
        // handle image share intent
        if (extras != null && extras.containsKey("preImage"))
        {
	        Uri selectedImage = (Uri)extras.getParcelable("preImage");
	        String realPath = getPath(this, selectedImage);
	        uploadImage(realPath);
        }
        
        if (savedInstanceState == null)
        {
        	boolean verified = _prefs.getBoolean("usernameVerified", false);
            if (!verified)
            {
            	LoginForm login = new LoginForm(ComposePostView.this);
            	login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {
    				@Override
    				public void onFailure() {
    					finish();
    				}

					@Override
					public void onSuccess() {
						// TODO Auto-generated method stub
						
					}
    			});
            }
        }
        else
        {
            // we kinda need this
            if (savedInstanceState.containsKey("cameraImageLocation"))
                _cameraImage = Uri.parse(savedInstanceState.getString("cameraImageLocation"));
        }
        
	}


    // returns true if edit bar enabled
    private boolean decideEditBar() {
        if ((_landscape && (_extendedEditor == 1)) || (_extendedEditor == 0))
            setEditBarEnabled(false);
        else
            setEditBarEnabled(true);
        return !((_landscape && (_extendedEditor == 1)) || (_extendedEditor == 0));
    }

    private void setEditBarEnabled(boolean b) {
        findViewById(R.id.editBarContainer).setVisibility(b ? View.VISIBLE : View.GONE);

        if (b) {
            mEditBarEnabled = true;
            invalidateOptionsMenu();
        }
        else
        {
            mEditBarEnabled = false;
            invalidateOptionsMenu();
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, SELECT_IMAGE);
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }
    // HOME BUTTON
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
                Intent upIntent = new Intent(this, MainActivity.class);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is not part of the application's task, so create a new task
                    // with a synthesized back stack.
                    TaskStackBuilder.from(this)
                            .addNextIntent(upIntent)
                            .startActivities();
                    saveOrExit();
                } else {
                    // This activity is part of the application's task, so simply
                    // navigate up to the hierarchical parent activity.
                    saveOrExit();
                }
                return true;
        }
        switch(item.getItemId())
        {
        	case R.id.menu_compose_markup:
                openMarkupSelector(false);
                break;
	        case R.id.menu_compose_macro:
		        openMarkupSelector(false,true);
		        break;
        	case R.id.menu_compose_post:
                postClick();
                break;
        	case R.id.menu_compose_picture:
                openPictureSelector();
        		break;
        	case R.id.menu_compose_camera:
        		openCameraSelector();
        		break;
        	case R.id.menu_compose_showParent:
        		showParentPost();
        		break;
        	case R.id.menu_compose_preview:
        		showPreview();
        		break;
            case R.id.menu_compose_showextended_on:
                _prefs.edit().putString("extendedEditor", "2").commit();
                _extendedEditor = 2;
                decideEditBar();
                break;
            case R.id.menu_compose_showextended_onlyportrait:
                _prefs.edit().putString("extendedEditor", "1").commit();
                _extendedEditor = 1;
                decideEditBar();
                break;
            case R.id.menu_compose_showextended_off:
                _prefs.edit().putString("extendedEditor", "0").commit();
                _extendedEditor = 0;
                decideEditBar();
                break;
            case R.id.menu_compose_forcepreview_on:
                _prefs.edit().putString("forcePostPreview", "2").commit();
                _forcePostPreview = 2;
                break;
            case R.id.menu_compose_forcepreview_onlyroot:
                _prefs.edit().putString("forcePostPreview", "1").commit();
                _forcePostPreview = 1;
                break;
            case R.id.menu_compose_forcepreview_off:
                _prefs.edit().putString("forcePostPreview", "0").commit();
                _forcePostPreview = 0;
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openPictureSelector()
    {
        if (Build.VERSION.SDK_INT < 19)
        {
            Intent intent = new Intent();
            intent.setType("image/jpeg");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Image"), SELECT_IMAGE);
        }
        else
        {
            if (Build.VERSION.SDK_INT >= 23)
            {
                // Here, thisActivity is the current activity
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                {
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE))
                    {

                        // Show an expanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.

                    }
                    else
                    {

                        // No explanation needed, we can request the permission.

                        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                        // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                }
                else
                {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
            }
            else
            {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_IMAGE);
            }
        }
    }

    public void setupButtonBindings(Bundle extras)
	{
		if (extras == null)
			extras = new Bundle();
        
        if ((_replyToPostId == 0) && (!_messageMode))
        {
            setTitle("New Post");
        }
        
        if (extras.containsKey("messageSubject"))
        {
        	_messageSubject = extras.getString("messageSubject");
        }
        if (extras.containsKey("parentAuthor"))
        {
        	final String author = extras.getString("parentAuthor");
        	_parentAuthorForDraft = author;
        	final String postContent = extras.getString("parentContent").replace("jt_spoiler", "jt_prevspoiler");
        	_parentPostForDraft = postContent;
        	_parentDateForDraft = extras.getLong("parentDate");
        	setTitle("Reply to " + author);
        	
        	if (_messageMode)
        	{
        		_messageRecipient = extras.getString("parentAuthor");
        		if (postContent.length() > 5)
        		{
	        		EditText edit = (EditText)findViewById(R.id.textContent);
	        		edit.setText("\r\n\r\nPrevious message from " + author + ": \r\n" + postContent);
        		}
        		setTitle("Msg to " + author);
        	}
        	
        	mParentAuthor = author;
        	mParentPostContent = postContent;
        	_showViewPost  = true;
        }

        findViewById(R.id.composeButtonPost).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
	            // Pivots indicate where the animation begins from
	            float pivotX = view.getPivotX() + view.getTranslationX();
	            float pivotY = view.getPivotY() + view.getTranslationY();
	            final View v2 = view;

	            // Animate FAB shrinking
	            ScaleAnimation anim = new ScaleAnimation(1, 0, 1, 0, pivotX, pivotY);
	            anim.setDuration(150);
	            anim.setInterpolator(new DecelerateInterpolator());
	            anim.setAnimationListener(new Animation.AnimationListener()
	            {
		            @Override
		            public void onAnimationStart(Animation animation)
		            {

		            }

		            @Override
		            public void onAnimationEnd(Animation animation)
		            {
						v2.setVisibility(View.INVISIBLE);
		            }

		            @Override
		            public void onAnimationRepeat(Animation animation)
		            {

		            }
	            });

	            view.startAnimation(anim);

                postClick();
	            // Only use scale animation if FAB is visible
            }
        });
        findViewById(R.id.composeButtonCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCameraSelector();
            }
        });
        findViewById(R.id.composeButtonPicture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openPictureSelector();
            }
        });
        findViewById(R.id.composeButtonMarkup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openMarkupSelector(false);
            }
        });
		findViewById(R.id.composeButtonMacros).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				openMarkupSelector(false, true);
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.compose, menu);
	    return super.onCreateOptionsMenu(menu);
	}
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        prepMenuItems(menu, !mEditBarEnabled);


		return true;
    }

    private void prepMenuItems(Menu menu, boolean showPostAsAction)
    {
        _forcePostPreview  = Integer.parseInt(_prefs.getString("forcePostPreview", "1"));
        _extendedEditor  = Integer.parseInt(_prefs.getString("extendedEditor", "1"));

        if (menu != null && menu.findItem(R.id.menu_compose_forcepreview_off) != null)
        {
            menu.findItem(R.id.menu_compose_forcepreview_off).setChecked(false);
            menu.findItem(R.id.menu_compose_forcepreview_on).setChecked(false);
            menu.findItem(R.id.menu_compose_forcepreview_onlyroot).setChecked(false);
            if (_forcePostPreview == 0)
                menu.findItem(R.id.menu_compose_forcepreview_off).setChecked(true);
            if (_forcePostPreview == 1)
                menu.findItem(R.id.menu_compose_forcepreview_onlyroot).setChecked(true);
            if (_forcePostPreview == 2)
                menu.findItem(R.id.menu_compose_forcepreview_on).setChecked(true);
        }

        if (menu != null && menu.findItem(R.id.menu_compose_showextended_on) != null)
        {
            menu.findItem(R.id.menu_compose_showextended_on).setChecked(false);
            menu.findItem(R.id.menu_compose_showextended_onlyportrait).setChecked(false);
            menu.findItem(R.id.menu_compose_showextended_off).setChecked(false);
            if (_extendedEditor == 0)
                menu.findItem(R.id.menu_compose_showextended_off).setChecked(true);
            if (_extendedEditor == 1)
                menu.findItem(R.id.menu_compose_showextended_onlyportrait).setChecked(true);
            if (_extendedEditor == 2)
                menu.findItem(R.id.menu_compose_showextended_on).setChecked(true);
        }

        if (menu != null && menu.findItem(R.id.menu_compose_showParent) != null)
        {
            if (_replyToPostId == 0)
            {
                menu.findItem(R.id.menu_compose_showParent).setVisible(false);
            }
            if (!LegacyFactory.getLegacy().hasCamera(this))
                menu.findItem(R.id.menu_compose_camera).setVisible(false);
        }

        if (menu != null && menu.findItem(R.id.menu_compose_post) != null)
        {
            /*
            menu.findItem(R.id.menu_compose_post).setShowAsAction(showPostAsAction ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
            menu.findItem(R.id.menu_compose_camera).setShowAsAction(showPostAsAction ? MenuItem.SHOW_AS_ACTION_IF_ROOM : MenuItem.SHOW_AS_ACTION_NEVER);
            menu.findItem(R.id.menu_compose_picture).setShowAsAction(showPostAsAction ? MenuItem.SHOW_AS_ACTION_IF_ROOM : MenuItem.SHOW_AS_ACTION_NEVER);
            menu.findItem(R.id.menu_compose_markup).setShowAsAction(showPostAsAction ? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
            */
            menu.findItem(R.id.menu_compose_post).setVisible(showPostAsAction);
            menu.findItem(R.id.menu_compose_camera).setVisible(showPostAsAction);
            menu.findItem(R.id.menu_compose_picture).setVisible(showPostAsAction);
            menu.findItem(R.id.menu_compose_markup).setVisible(showPostAsAction);
	        menu.findItem(R.id.menu_compose_macro).setVisible(showPostAsAction);
        }
    }

    
    public void showParentPost()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(ComposePostView.this);
        builder.setTitle("Post by " + mParentAuthor);
        ScrollView scrolly = new ScrollView(ComposePostView.this);
        TextView content = new TextView(ComposePostView.this);
        content.setPadding(4, 4, 4, 4);
        content.setTextSize(TypedValue.COMPLEX_UNIT_SP, _zoom * getResources().getDimension(R.dimen.viewPostTextSize));
        scrolly.addView(content);
        content.setText(PostFormatter.formatContent(mParentAuthor, mParentPostContent, scrolly, true, true));
        if (Build.VERSION.SDK_INT >= 11)
        {
        	content.setTextIsSelectable(true);
        }
        builder.setView(scrolly);
        builder.setNegativeButton("Close", new DialogInterface.OnClickListener(){
		@Override
		public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
			
		} });
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(true);
        alert.show();
    }
    
    public void showPreview()
    {
        statInc(this, "CheckedPostPreview");
    	EditText edit = (EditText)findViewById(R.id.textContent);
		String postContent = edit.getText().toString();
		postContent = getPreviewFromHTML(postContent);


        AlertDialog.Builder builder = new AlertDialog.Builder(ComposePostView.this);
        builder.setTitle("Post by " + _prefs.getString("userName", "user"));
        ScrollView scrolly = new ScrollView(ComposePostView.this);
        TextView content = new TextView(ComposePostView.this);
        content.setPadding(10, 5, 10, 5);
        content.setText(PostFormatter.formatContent(_prefs.getString("userName", "user"), postContent, scrolly, true, true));
        content.setTextSize(TypedValue.COMPLEX_UNIT_SP, _zoom * getResources().getDimension(R.dimen.viewPostTextSize));
        scrolly.addView(content);
        builder.setView(scrolly);
        builder.setNegativeButton("Close", new DialogInterface.OnClickListener(){
		@Override
		public void onClick(DialogInterface dialog, int which) {
			// TODO Auto-generated method stub
			findViewById(R.id.composeButtonPost).setVisibility(View.VISIBLE);
			
		} });
	    builder.setOnCancelListener(new DialogInterface.OnCancelListener()
	    {
		    @Override
		    public void onCancel(DialogInterface dialogInterface)
		    {
			    findViewById(R.id.composeButtonPost).setVisibility(View.VISIBLE);
		    }
	    });
        // handle force preview preference
        if ((((_replyToPostId == 0) && (_forcePostPreview >= 1)) || (_forcePostPreview == 2)) && (!_messageMode))
        {
        	builder.setPositiveButton("Post", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					postTask();
				} });
        }
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(true);
        alert.show();
    }
    
    public void doOrientation () 
    {
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
    }
	
    // handle exit button
	public void saveOrExit()
	{
		EditText edit = (EditText)findViewById(R.id.textContent);
		if (edit.length() > 1)
		{
            AlertDialog.Builder builder = new AlertDialog.Builder(ComposePostView.this);
	        builder.setTitle("Exit Composer");
	        String whatDo = (_replyToPostId == 0) ? "create a new topic post" : "reply to this post";
	        if (!_messageMode)
	        	builder.setMessage("You have typed a post. The next time you " + whatDo + ", your drafted post can be restored.");
	        else
	        	builder.setMessage("You have typed a post. A draft will not be saved.");
	        
	        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
			} });

            final Context ctx = this;
	        
	        // message mode supports no drafts
	        if (!_messageMode)
	        {
		        builder.setNeutralButton("Save Draft", new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
                        statInc(ctx, "DraftsSaved");
						// save
						_preventDraftSave = false;
						finish();
				} });
		        builder.setPositiveButton("Discard", new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// delete any drafts
                        statInc(ctx, "BackedOutOfPost");
                        if (_replyToPostId == 0)
                            statInc(ctx, "BackedOutOfRootPost");

			        	_drafts.deleteDraftById(_replyToPostId);
			        	_preventDraftSave = true;
						finish();
				} });
	        }
	        else
	        {
	        	builder.setPositiveButton("Exit", new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
                        statInc(ctx, "BackedOutOfMessageReply");
						_preventDraftSave = true;
						finish();
				} });
	        }
	        AlertDialog alert = builder.create();
	        alert.setCanceledOnTouchOutside(true);
	        alert.show();
		}
		else { _preventDraftSave = true; finish(); }
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	saveOrExit();
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
	    super.onSaveInstanceState(outState);
	    
	    if (_cameraImage != null)
    	    outState.putString("cameraImageLocation", _cameraImage.toString());
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		if (!_preventDraftSave)
		{
			EditText edit = (EditText)findViewById(R.id.textContent);
            if (!_messageMode)
			    _drafts.saveThisDraft(_replyToPostId, edit.getText().toString().replaceAll("\n", "<br/>"), _parentAuthorForDraft, _parentPostForDraft, _parentDateForDraft);
		}
        statInc(this, "TimeInComposer", TimeDisplay.secondsSince(mLastResumeTime));
	}

    @Override
    public void onResume()
    {
        super.onResume();
        mLastResumeTime = TimeDisplay.now();

        _forcePostPreview = Integer.parseInt(_prefs.getString("forcePostPreview", "1"));
    }
	
	@Override
	public void onConfigurationChanged (Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		
		EditText editBox = (EditText)findViewById(R.id.textContent);
		String postText = editBox.getText().toString();
		
		doOrientation();
		if (((_orientLock == 0) && (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)) || (_orientLock == 1) || (_orientLock == 3))
		{
			//setContentView(R.layout.edit_post_lollipop);
			_landscape  = false;
		}
		else
		{
			//setContentView(R.layout.edit_post_lollipop);
			_landscape = true;
		}
        //setContentView(R.layout.edit_post_lollipop);
        if (decideEditBar())
        {

        }



		setupButtonBindings(null);
		
		editBox = (EditText)findViewById(R.id.textContent);
		editBox.setText(postText);
        editBox.setTextSize(TypedValue.COMPLEX_UNIT_PX, editBox.getTextSize() * _zoom);
        editBox.setCustomSelectionActionModeCallback(new StyleCallback());
		editBox.requestFocus();
	}



    @Override
    public void onStart()
    {
		super.onStart();
		EditText edit = (EditText)findViewById(R.id.textContent);

		_drafts.loadDraftsFromDisk();
		if ((_drafts._drafts != null) && (!_messageMode))
		{
			String oldText = _drafts._drafts.get(_replyToPostId);
			if (oldText != null)
			{
				String[] draftbits = oldText.split(Pattern.quote(_drafts.SECOND_TOKEN));
				oldText = draftbits[0];
			
				// restore draft
				edit.setText(oldText.replaceAll("<br/>", "\n"));
				edit.setSelection(edit.getText().length());
			}
		}
		
	}
	
	void appendText(String text) { appendText(text, true); }
	void appendText(String text, boolean newline)
	{
	    EditText edit = (EditText)findViewById(R.id.textContent);
	    if (newline)
	    {
		    // if there is text in there, put the image on a new line
		    if (edit.length() > 0 && !edit.getText().toString().endsWith(("\n")))
		        text = "\n" + text;
		    
		    edit.append(text + "\n");
	    }
	    else edit.append(" " + text + " ");
	    
	    System.out.println("composeview: append : " + text + " result: " + edit.getText().toString());
	}
	
	protected AlertDialog _shackTagDialog;

	protected void openMarkupSelector(boolean andReselect)
	{
		openMarkupSelector(andReselect, false);
	}

	static final String MARKUPTITLE = "Select Shack Tag";
	static final String MACROTITLE = "Select Macro";
	boolean mIsMacroItem = false;
	protected void openMarkupSelector(boolean andReselect, boolean macrosInstead) {
		mIsMacroItem = macrosInstead;

		AlertDialog.Builder builder = new AlertDialog.Builder(ComposePostView.this);
		builder.setTitle(MARKUPTITLE);
		if (macrosInstead)
			builder.setTitle(MACROTITLE);

		GridView grid = new GridView(ComposePostView.this);
		if (macrosInstead) {
			grid.setNumColumns(2);
		}
		else
			grid.setNumColumns(3);
		grid.setHorizontalSpacing(2);
		grid.setVerticalSpacing(2);
		//grid.setNumColumns(GridView.AUTO_FIT);
		grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		grid.setGravity(Gravity.CENTER);

		grid.setOnItemClickListener(onShackTagSelected);


		ArrayList<Spanned> itemList = new ArrayList<Spanned>();
		if (!macrosInstead)
		{
			for (int i = 0; i < _tagLabels.length; i++)
			{
				itemList.add(PostFormatter.formatContent("bradsh", getPreviewFromHTML(_tagLabels[i]), null, true, true));
			}
		}
		if (macrosInstead)
		{
			// AMERICA item
			itemList.add(PostFormatter.formatContent("bradsh", getPreviewFromHTML("Macro: r{A}rMb{E}br{R}rIb{C}br{A}r"), null, true, true));
			// RAINBOW
			itemList.add(PostFormatter.formatContent("bradsh", getPreviewFromHTML("Macro: r{R}rg{A}gb{I}by{N}yl[B]ln[O]np[W]p"), null, true, true));
			// ALLCAPS
			itemList.add(PostFormatter.formatContent("bradsh", getPreviewFromHTML("Macro: ALLCAPS"), null, true, true));
			// RAINBOW
			itemList.add(PostFormatter.formatContent("bradsh", getPreviewFromHTML("Macro: r{C}rg{H}gr{R}rg{I}gr{S}rg{T}gr{M}rg{A}gr{S}r"), null, true, true));
		}

		ArrayAdapter<Spanned> adapter = new ArrayAdapter<Spanned>(ComposePostView.this,android.R.layout.simple_list_item_1, itemList);
		grid.setAdapter(adapter);
		builder.setView(grid);

		builder.setNegativeButton("Close", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub

			} });
		_shackTagDialog = builder.create();
		_shackTagDialog.setCanceledOnTouchOutside(true);
		_shackTagDialog.show();

		if (andReselect)
		{
			_shackTagDialog.setOnDismissListener(new OnDismissListener(){

				@Override
				public void onDismiss(DialogInterface dialog) {
					EditText et = (EditText)findViewById(R.id.textContent);
					et.post(new Runnable(){

						@Override
						public void run() {
							EditText et = (EditText)findViewById(R.id.textContent);
							// completely retarded workaround to bring the contextual action mode actionbar back
							et.setHapticFeedbackEnabled(false);
							et.performLongClick();
							et.setHapticFeedbackEnabled(true);
						}});
				}});
		}
	}

	OnItemClickListener onShackTagSelected =	new OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3) {
            applyMarkup(which);
		}		
	};

    private void applyMarkup(int which) {

        statInc(ComposePostView.this, "AppliedShackTag");

        if (_shackTagDialog != null)
            _shackTagDialog.cancel();

        final String[] tags = _tags;
        EditText edit = (EditText)findViewById(R.id.textContent);

        // check if text is selected. if so, apply tags. if no text selected, ask for text
        int start = edit.getSelectionStart();
        int end = edit.getSelectionEnd();
        String seltext = edit.getText().toString().substring(start, end);
        if (seltext.length() > 0)
        {
	        String textToInsert = "";
	        if (mIsMacroItem)
	        {
		        switch (which)
		        {
			        case 0:
				        // AMERICA
				        for (int i = 0; i < seltext.length(); i++)
				        {
					        String curTag = ""; String curOpenBracket = "{"; String curCloseBracket = "}";
					        if ((i % 3) == 0) { curTag = "r"; curOpenBracket = "{"; curCloseBracket = "}"; }
					        if ((i % 3) == 1) { curTag = ""; curOpenBracket = ""; curCloseBracket = ""; }
					        if ((i % 3) == 2) { curTag = "b"; curOpenBracket = "{"; curCloseBracket = "}"; }
					        textToInsert = textToInsert + curTag + curOpenBracket + seltext.charAt(i) + curCloseBracket + curTag;
				        }
				        break;
			        case 1:
				        // RAINBOW
				        for (int i = 0; i < seltext.length(); i++)
				        {
					        String curTag = ""; String curOpenBracket = "{"; String curCloseBracket = "}";
					        if ((i % 7) == 0) { curTag = "r"; curOpenBracket = "{"; curCloseBracket = "}"; }
					        if ((i % 7) == 1) { curTag = "g"; curOpenBracket = "{"; curCloseBracket = "}"; }
					        if ((i % 7) == 2) { curTag = "b"; curOpenBracket = "{"; curCloseBracket = "}"; }
					        if ((i % 7) == 3) { curTag = "y"; curOpenBracket = "{"; curCloseBracket = "}"; }
					        if ((i % 7) == 4) { curTag = "l"; curOpenBracket = "["; curCloseBracket = "]"; }
					        if ((i % 7) == 5) { curTag = "n"; curOpenBracket = "["; curCloseBracket = "]"; }
					        if ((i % 7) == 6) { curTag = "p"; curOpenBracket = "["; curCloseBracket = "]"; }
					        textToInsert = textToInsert + curTag + curOpenBracket + seltext.charAt(i) + curCloseBracket + curTag;
				        }
				        break;
			        case 2:
				        // ALLCAPS
				        textToInsert = seltext.toUpperCase();
				        break;
			        case 3:
				        // CHRISTMAS
				        for (int i = 0; i < seltext.length(); i++)
				        {
					        String curTag = ""; String curOpenBracket = "{"; String curCloseBracket = "}";
					        if ((i % 2) == 0) { curTag = "r"; curOpenBracket = "{"; curCloseBracket = "}"; }
					        if ((i % 2) == 1) { curTag = "g"; curOpenBracket = "{"; curCloseBracket = "}"; }
					        textToInsert = textToInsert + curTag + curOpenBracket + seltext.charAt(i) + curCloseBracket + curTag;
				        }
				        break;
		        }
	        }
	        else
	        {
		        textToInsert = tags[which].substring(0, 2) + seltext + tags[which].substring(5);
	        }
	        edit.getText().replace(Math.min(start, end), Math.max(start, end), textToInsert, 0, textToInsert.length());
	        edit.setSelection(Math.min(start, end), Math.min(start, end) + textToInsert.length());
        }
        else
        {
            if (!_prefs.getBoolean("hasSeenMarkupTip", false))
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(ComposePostView.this);
                builder.setTitle("Markup Button Tip");
                builder.setMessage("You can long-press on a word to select the word in Android. Once a word is selected, you can hit the markup button and that markup will be applied. You can do this multiple times to easily add multiple markups to a text selection.");
                builder.setNegativeButton("Never show this again", null);
                builder.show();

                Editor editor = _prefs.edit();
                editor.putBoolean("hasSeenMarkupTip", true);
                editor.apply();
            }
	        if (mIsMacroItem)
	        {
		        AlertDialog.Builder builder = new AlertDialog.Builder(ComposePostView.this);
		        builder.setTitle("Cannot Apply");
		        builder.setMessage("You must select text before applying text macros.");
		        builder.setNegativeButton("OK", null);
		        builder.show();
	        }
	        else
	        {
		        String textToInsert = tags[which].substring(0, 2) + tags[which].substring(5);
		        edit.getText().replace(Math.min(start, end), Math.max(start, end), textToInsert, 0, textToInsert.length());
		        edit.setSelection(Math.min(start, end) + 2);
	        }
        }
    }

    public void postClick()
	{
		if ((((_replyToPostId == 0) && (_forcePostPreview >= 1)) || (_forcePostPreview == 2)) && (!_messageMode))
        {
			showPreview();
        }
		else
			postTask();
	}

    public String pad(String value, int length, String with) {
        StringBuilder result = new StringBuilder(length);
        result.append(value);

        while (result.length() < length) {
            result.append(with);
        }
        return result.toString();
    }

	public void postTask()
	{
		// grab the content to post
	    EditText et = (EditText)findViewById(R.id.textContent);
        String content = et.getText().toString();

	    if (content.length() < 6)
	    {

            content = pad(content, 6, " ");
            statInc(this, "LessThanSixChars");

	    }

        // post in the background
        new PostTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, content);
	}
	
	public void openCameraSelector()
	{

		// store our image in a temp spot
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state))
	    {
	        // application directory, per Android Data Storage guidelines
	        File app_dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
	        
	        // make sure the directory exists
	        if (!app_dir.exists())
	        {
		        if (!app_dir.mkdirs())
		        {
    		        ErrorDialog.display(ComposePostView.this, "Error", "Could not create application directory.");
    		        return;
		        }
	        }
	        
	        // our temp file for taking a picture, delete if we already have one
		    File image = null;
		    try
		    {
			    image = File.createTempFile(
				    "shackbrowseUpload",  /* prefix */
				    ".jpg",         /* suffix */
				   app_dir     /* directory */
		            );
		    } catch (IOException e)
		    {
			    e.printStackTrace();
		    }

		    if (image.exists())
		        image.delete();
		    
		    // _cameraImage = Uri.fromFile(file);
		    mCurrentPhotoPath = image.getAbsolutePath();

		    _cameraImage = FileProvider.getUriForFile(ComposePostView.this,
				    BuildConfig.APPLICATION_ID + ".provider",
				    image);
		    
		    // start the camera
		    Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		    i.putExtra(MediaStore.EXTRA_OUTPUT, _cameraImage);
		    startActivityForResult(i, TAKE_PICTURE);
	    }
	    else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
	    {
	        ErrorDialog.display(ComposePostView.this, "Error", "External storage is mounted as read only.");
	    }
	    else
	    {
	        ErrorDialog.display(ComposePostView.this, "Error", "External storage is in an unknown state.");
	    }
	}
	
	public String _messageSubject = null;
	public String _messageRecipient = null;
	
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ThreadListFragment.OPEN_PREFS)
        {
        	doOrientation();
        	invalidateOptionsMenu();
        }
        
        if (requestCode == SELECT_IMAGE || requestCode == SELECT_IMAGE_KITKAT)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                if (null == data) return;
                Uri originalUri = null;

                if (requestCode == SELECT_IMAGE) {
                    originalUri = data.getData();
                }
                else if (requestCode == SELECT_IMAGE_KITKAT) {
                    originalUri = data.getData();

                    final int takeFlags = data.getFlags()
                            & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    // Check for the freshest data.
                    //noinspection ResourceType
                    getContentResolver().takePersistableUriPermission(originalUri, takeFlags);
                }



                String realPath = getPath(this, originalUri);
                uploadImage(realPath);
            } 
        }
        else if (requestCode == TAKE_PICTURE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                // picture was taken, and resides at the location we specified
                uploadImage(mCurrentPhotoPath);
            }
        }
	}



	
	void uploadImage(String imageLocation)
	{
	    _progressDialog = MaterialProgressDialog.show(ComposePostView.this, "Upload", "Uploading image to chattypics");
	    new UploadAndInsertTask().execute(imageLocation);
        statInc(this, "ImagesToChattyPics");
	}
	
	void postSuccessful(PostReference pr)
	{
	    Intent reply = new Intent();

		reply.putExtra("PQPId", pr.mPQPId);
	    reply.putExtra("parentPostId", pr.mParentPostId);
	    
	    setResult(RESULT_OK, reply);
	    
	    // start the postqueue service
	    Intent msgIntent = new Intent(this, PostQueueService.class);
	    startService(msgIntent);
	    
	    // lets get the hell out of here!
	    _preventDraftSave = true;
	    finish();
	}
	
	class PostReference
	{
		int mParentPostId;
		long mPQPId;

		PostReference(int parentpostid, long postqueuepostid)
		{
			mParentPostId = parentpostid; mPQPId = postqueuepostid;
		}
	}
	
	class PostTask extends AsyncTask<String, Void, PostReference>
	{
	    Exception _exception;
	    
        @Override
        protected PostReference doInBackground(String... params)
        {
            try
            {
                String content = params[0];
                
                if (!_messageMode)
                {
                	/*
                	runOnUiThread(new Runnable(){
                		@Override public void run()
                		{
                			_progressDialog = ProgressDialog.show(ComposePostView.this, "Posting", "Contacting server...", true, false);
                		}
        			});
        			*/
                	PostQueueObj pqo = new PostQueueObj(_replyToPostId, content, _isNewsItem);
                	pqo.create(ComposePostView.this);
	                // JSONObject data = ShackApi.postReply(ComposePostView.this, _replyToPostId, content, _isNewsItem);
	                // int reply_id = data.getInt("post_insert_id");
	                // return Integer.valueOf(reply_id);
                	System.out.println("POSTCOMPOSER: PR = " + pqo.getPostQueueId());
                	return new PostReference(_replyToPostId, pqo.getPostQueueId());
                }
                else
                {
                	/*
                	runOnUiThread(new Runnable(){
                		@Override public void run()
                		{
                			_progressDialog = ProgressDialog.show(ComposePostView.this, "Sending Message", "Contacting server...", true, false);
                		}
        			});
        			*/
                	// ShackApi.postMessage(ComposePostView.this, _messageSubject, _messageRecipient, content);
                	PostQueueObj pqo = new PostQueueObj(_messageSubject, _messageRecipient, content);
                	pqo.create(ComposePostView.this);
                	return new PostReference(0, pqo.getPostQueueId());
                }
            }
            catch (Exception e)
            {
                Log.e("shackbrowse", "Error posting reply", e);
                _exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(PostReference res)
        {
        	// delete any drafts
        	if (!_messageMode)
        		_drafts.deleteDraftById(_replyToPostId);
        	
        	/*
            _progressDialog.dismiss();
            */
            
            if (_exception != null)
            {
            	System.out.println(_exception.getMessage());
            	if (_exception.getMessage().contains("create/17.json"))
            	{
                    AlertDialog.Builder builder = new AlertDialog.Builder(ComposePostView.this);
        	        builder.setTitle("Error");
        	        builder.setMessage("Could not access Shacknews posting API.\nPossibly bad login credentials?\nOpen settings?");
        	        builder.setPositiveButton("Open login settings", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent i = new Intent(ComposePostView.this, PreferenceView.class);
                            i.putExtra("pscreenkey", "logins");
                            startActivity(i);
                        }
                    });
        	        builder.setNegativeButton("OK", null);
        	        builder.create().show();
            	}
            	else
            		ErrorDialog.display(ComposePostView.this, "Error", "Error posting:\n" + _exception.getMessage());
            }
            else
                postSuccessful(res);
        }
        
	}
	
	class UploadAndInsertTask extends AsyncTask<String, Void, String>
	{
	    Exception _exception;
	    
        @Override
        protected String doInBackground(String... params)
        {
            try
            {
                String imageLocation = params[0];
                
                // resize the image for faster uploading
                String smallImageLocation = resizeImage(imageLocation);
                
                String userName = _prefs.getString("chattyPicsUserName", null);
                String password = _prefs.getString("chattyPicsPassword", null);
                
                // attempt to log in so the image will appear in the user's gallery
                String login_cookie = null;
                if (userName != null && password != null)
                    login_cookie = ShackApi.loginToUploadImage(userName, password);
                
                // actually upload the thing
                String content = ShackApi.uploadImage(smallImageLocation, login_cookie);
                
                // if the image was resized, delete the small one
                if (!imageLocation.equals(smallImageLocation))
                {
                    try
                    {
                        File file = new File(smallImageLocation);
                        file.delete();
                    }
                    catch (Exception ex)
                    {
                        Log.e("shackbrowse", "Error deleting resized image", ex);
                    }
                }
                
                Pattern p = Pattern.compile("http\\:\\/\\/chattypics\\.com\\/viewer\\.php\\?file=(.*?)\"");
                Matcher match = p.matcher(content);
                                
                if (match.find())
                    return "http://chattypics.com/files/" + match.group(1);
                
                return null;
            }
            catch (Exception e)
            {
                Log.e("shackbrowse", "Error posting reply", e);
                _exception = e;
                return null;
            }
        }
        
        String resizeImage(String path) throws Exception
        {
            final int MAXIMUM_SIZE = 1600;
            
            // get the original image
            // Bitmap original = BitmapFactory.decodeFile(path);

            Bitmap original=null;
            File f= new File(path);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try {
                original = BitmapFactory.decodeStream(new FileInputStream(f), null, options);
            } catch (FileNotFoundException e) {
                System.out.println("404 ERROR");
                e.printStackTrace();
            }

            float scale = Math.min((float)MAXIMUM_SIZE / original.getWidth(), (float)MAXIMUM_SIZE / original.getHeight());
            
            // work around for older devices that don't support EXIF
            int rotation = LegacyFactory.getLegacy().getRequiredImageRotation(path);
            
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            matrix.postRotate(rotation);
            
            Bitmap resized = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
            
            // generate the new image
            File file = new File(path);
            File newFile = getFileStreamPath(file.getName());
            
            // save the image
            FileOutputStream f2 = new FileOutputStream(newFile);
            try
            {
                resized.compress(CompressFormat.JPEG, 97, f2);
            }
            finally
            {
                f2.close();
            }
            
            return newFile.getAbsolutePath();
        }

        @Override
        protected void onPostExecute(String result)
        {
        	try {
        		_progressDialog.dismiss();
        	}
        	catch (Exception e)
        	{
        		
        	}
            
            if (_exception != null)
            {
            	System.out.println("camupload: err");
                ErrorDialog.display(ComposePostView.this, "Error", "Error posting:\n" + _exception.getMessage());
            }
            else if (result == null)
            {
            	System.out.println("camupload: err");
                ErrorDialog.display(ComposePostView.this, "Error", "Couldn't find image URL after uploading.");
            }
            else
            {
            	final String result1 = result;
            	runOnUiThread(new Runnable(){
            		@Override public void run()
            		{
            			appendText(result1);
            		}
            	});
            }
        }
	}
	
	// DRAFTS
	
	/*
	{
        "r{...}r",
        "/[...]/",
        "g{...}g",
        "b[...]b",
        "b{...}b",
        "q[...]q",
        "y{...}y",
        "s[...]s",
        "e[...]e",
        "_[...]_",
        "l[...]l",
        "-[...]-",
        "n[...]n",
        "o[...]o",
        "p[...]p"};
*/
	// PREVIEW PARSER
	static public String getPreviewFromHTML(String markup)
	{
		
        markup = markup
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("(?i)(\\A|\\s)((http|https|ftp|mailto):\\S+)(\\s|\\z)","$1<a href=\"$2\">$2</a>$4")
		.replaceAll("\\}r|\\}g|\\}b|\\]q|\\}y|\\]s|\\]e|\\]l|\\]-|\\]n|\\]p|\\]o", "</span>")
		.replaceAll("r\\{", "<span class=\"jt_red\">")
		.replaceAll("g\\{", "<span class=\"jt_green\">")
		.replaceAll("b\\{", "<span class=\"jt_blue\">")
		.replaceAll("q\\[", "<span class=\"jt_quote\">")
		.replaceAll("y\\{", "<span class=\"jt_yellow\">")
		.replaceAll("s\\[", "<span class=\"jt_sample\">")
		.replaceAll("e\\[", "<span class=\"jt_olive\">")
		.replaceAll("l\\[", "<span class=\"jt_lime\">")
		.replaceAll("n\\[", "<span class=\"jt_orange\">")
		.replaceAll("-\\[", "<span class=\"jt_strike\">")
		.replaceAll("p\\[", "<span class=\"jt_pink\">")
		.replaceAll("o\\[", "<span class=\"jt_prevspoiler\" onclick=\"return doSpoiler(event)\">")
		.replaceAll("b\\[|\\*\\[", "<b>")
		.replaceAll("\\]b|\\]\\*", "</b>")
		.replaceAll("/\\[", "<i>")
		.replaceAll("\\]/", "</i>")
		.replaceAll("_\\[", "<u>")
		.replaceAll("\\]_", "</u>")
		.replaceAll("\n", "<br />");
		System.out.println("markup: " + markup);
		return markup;
	}
	
	class StyleCallback implements ActionMode.Callback {

	    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
	        MenuInflater inflater = mode.getMenuInflater();
	        inflater.inflate(R.menu.compose_style, menu);
	        
	        return true;
	    }

	    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
	    	if (_landscape)
	    	{
	    		menu.findItem(R.id.menu_compMarkup).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			    menu.findItem(R.id.menu_compMacro).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
	    		if (menu.findItem(android.R.id.selectAll) != null)
	    			menu.findItem(android.R.id.selectAll).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_WITH_TEXT);


	    	}
	    	else
	    	{
	    		menu.findItem(R.id.menu_compMarkup).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			    menu.findItem(R.id.menu_compMacro).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	    		if (menu.findItem(android.R.id.selectAll) != null)
	    			menu.findItem(android.R.id.selectAll).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	    	}
	        
	    	return false;
	    }

	    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
	        CharacterStyle cs;
	        EditText bodyView = (EditText)findViewById(R.id.textContent);
	        int start = bodyView.getSelectionStart();
	        int end = bodyView.getSelectionEnd();
	        SpannableStringBuilder ssb = new SpannableStringBuilder(bodyView.getText());

	        switch(item.getItemId()) {

		        case R.id.menu_compMarkup:
		            openMarkupSelector(true);
		            return true;
		        case R.id.menu_compMacro:
			        openMarkupSelector(true, true);
			        return true;
	        }
	        return false;
	    }

	    public void onDestroyActionMode(ActionMode mode) {
	    }
	}

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}