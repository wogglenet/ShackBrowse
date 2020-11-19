package net.woggle.shackbrowse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.woggle.EditTextSelectionSavedAllowImage;
import net.woggle.shackbrowse.imgur.ImgurAuthorization;
import net.woggle.shackbrowse.imgur.ImgurTools;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;

import android.text.InputType;
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
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.StackingBehavior;
import com.gc.materialdesign.views.ButtonFloatSmall;
import com.vdurmont.emoji.EmojiParser;

import org.json.JSONObject;

import static net.woggle.shackbrowse.StatsFragment.statInc;
import static net.woggle.shackbrowse.imgur.ImgurTools.uploadImageToImgur;

public class ComposePostView extends AppCompatActivity {

	protected static final int SELECT_IMAGE = 0;
	protected static final int TAKE_PICTURE = 1;
    protected static final int SELECT_IMAGE_KITKAT = 3;
    protected static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 50;
	protected static final int PERMISSIONS_MULTIPLE_REQUEST = 41;
	
	static final long MAX_SIZE_LOGGED_IN = 6 * 1024 * 1024;
	static final long MAX_SIZE_NOT_LOGGED_IN = 3 * 1024 * 1024;
	
	final String[] _tagLabels = { "r{red}r", "g{green}g", "b{blue}b", "y{yellow}y", "e[olive]e", "l[lime]l", "n[orange]n", "p[multisync]p", "/[italics]/", "b[bold]b",  "q[quote]q",  "s[small]s",  "_[underline]_",   "-[strike]-",  "spoilo[er]o"};
    final String[] _tags = {      "r{...}r",  "g{...}g",   "b{...}b",  "y{...}y",  "e[...]e",  "l[...]l",  "n[...]n",	  "p[...]p",      "/[...]/",         "b[...]b",   "q[...]q",     "s[...]s",   "_[...]_",     "-[...]-",	   "o[...]o"};
	final String mEmoji = "\u2600\u2601\u2602\u2603\u2604\u2605\u2606\u2607\u2608\u2609\u260e\u2614\u2615\u2616\u2617\u2618\u261c\u261d\u261e\u261f\u2620\u2621\u2622\u2623\u2639\u263a\u263b\u2665\u2666\u2698\u26a0\u26a1\u26bd\u26be\u26c7\u26c8\u26c4\u26c5\u26cf\u26d4\u26e4\u26e5\u26e6\u26e7\u26f0\u26f1\u26f2\u26f3\u26f4\u26f5\u26f7\u26f8\u26f9\u26fa\u2701\u2702\u2703\u2704\u2705\u2706\u2707\u2708\u2709\u270a\u270b\u270c\u270d\u270e\u270f\u2710\u2711\u2712\u2713\u2714\u2715\u2716\u2717\u2718\u2728\u2744\u274c\u274e\u2764\u2b50\u231a\u231b\u23f0\u23f1\u23f3";

	private int _contentTypeId = 0;
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
	private Toolbar mToolbar;
	private boolean mAnonMode = false;

	private DraftTemplates mTpl;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        // prefs
        _prefs = PreferenceManager.getDefaultSharedPreferences(this);

        mThemeResId = MainActivity.themeApplicator(this);
        
        _forcePostPreview  = Integer.parseInt(_prefs.getString("forcePostPreview", "1"));

        _extendedEditor  = Integer.parseInt(_prefs.getString("extendedEditor", "1"));

		_contentTypeId = Integer.parseInt(ShackApi.FAKE_STORY_ID);
        // grab the post being replied to, if this is a reply
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(MainActivity.THREAD_ID))
        {
            _replyToPostId = getIntent().getExtras().getInt(MainActivity.THREAD_ID);

            _contentTypeId = getIntent().getExtras().getInt(MainActivity.CONTENT_TYPE_ID);
        }
        
        // drafts
        _drafts = new Drafts(this);

        mTpl = new DraftTemplates(this);
        
        // orientation
        doOrientation();
        
        if (_orientLock == 2 || _orientLock == 4 || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
        	setContentView(R.layout.edit_post);
            _landscape  = true;
        }
        else
        {
        	setContentView(R.layout.edit_post);
            _landscape  = false;
        }

		mToolbar = (Toolbar) findViewById(R.id.edit_app_toolbar);
		setSupportActionBar(mToolbar);

        decideEditBar();

        // home button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // zoom handling
        _zoom = Float.parseFloat(_prefs.getString("fontZoom", "1.0"));
		EditTextSelectionSavedAllowImage editBox = (EditTextSelectionSavedAllowImage)findViewById(R.id.textContent);
        editBox.setTextSize(TypedValue.COMPLEX_UNIT_PX, editBox.getTextSize() * _zoom);

		if (_zoom >= 0.9)
		{
			applyButtonZoom(findViewById(R.id.composeButtonPost));
			applyButtonZoom(((ButtonFloatSmall)findViewById(R.id.composeButtonPost)).getIcon());
			applyButtonZoom(findViewById(R.id.composeButtonCamera));
			applyButtonZoom(findViewById(R.id.composeButtonMacros));
			applyButtonZoom(findViewById(R.id.composeButtonMarkup));
			applyButtonZoom(findViewById(R.id.composeButtonPicture));
			applyButtonZoom(findViewById(R.id.composeButtonSelectAll));
		}
        
        editBox.setCustomSelectionActionModeCallback(new StyleCallback());

		editBox.setKeyBoardInputCallbackListener(new EditTextSelectionSavedAllowImage.KeyBoardInputCallbackListener() {
			@Override
			public void onCommitContent(InputContentInfoCompat inputContentInfo, int flags, Bundle opts) {
				try
				{

					uploadImage(inputContentInfo.getContentUri());
				}
				catch (Exception e)
				{

				}
			//you will get your gif/png/jpg here in opts bundle
		}
	});
        

        
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
        	System.out.println("PRETEXT: " + extras.getString("preText"));
        	EditTextSelectionSavedAllowImage ed = findViewById(R.id.textContent);
        	ed.post(new Runnable()
	        {
		        @Override
		        public void run()
		        {
			        ed.append(extras.getString("preText"));
		        }
	        });

        }
        // handle image share intent
        if (extras != null && extras.containsKey("preImage"))
        {
	        Uri selectedImage = (Uri)extras.getParcelable("preImage");
	        uploadImage(selectedImage);
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

	public void applyButtonZoom (View imageButton)
	{
		// ImageButton button = (ImageButton) imageButton;
		ViewGroup.LayoutParams buttonLayout = imageButton.getLayoutParams();
		buttonLayout.height = (int) Math.floor(buttonLayout.height * _zoom);
		buttonLayout.width = (int) Math.floor(buttonLayout.width * _zoom);
		imageButton.setLayoutParams(buttonLayout);
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
			case R.id.menu_compose_template:
				showTemplates();
				break;
	        case R.id.menu_compose_emoji:
	        	showEmoji();
				break;
	        case R.id.menu_compose_selectall:
		        ((EditTextSelectionSavedAllowImage)findViewById(R.id.textContent)).selectAll();
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

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					sendPictureSelectIntent();
				} else {
					// permission denied, boo! Disable the
					// functionality that depends on this permission.
				}
				return;
			}
			case PERMISSIONS_MULTIPLE_REQUEST: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					sendCameraIntent();
				} else {
					// permission denied, boo! Disable the
					// functionality that depends on this permission.
				}
				return;
			}
		}
	}
	private void sendPictureSelectIntent()
	{
		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		photoPickerIntent.setType("image/*");
		startActivityForResult(photoPickerIntent, SELECT_IMAGE);
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
					ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
					/*
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE))
                    {
						ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
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
                    */
                }
                else
                {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
            }
            else
            {
                sendPictureSelectIntent();
            }
        }
    }

	public void openCameraSelector()
	{
		if (Build.VERSION.SDK_INT >= 23)
		{
			if ((ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) || (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED))
			{
				ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_MULTIPLE_REQUEST);
			}
			else
				sendCameraIntent();
		}
		else
		{
			sendCameraIntent();
		}
	}
	private void sendCameraIntent()
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

			_cameraImage = FileProvider.getUriForFile(ComposePostView.this, BuildConfig.APPLICATION_ID + ".provider", image);

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


	public void setupButtonBindings(Bundle extras)
	{
		mAnonMode = _prefs.getBoolean("donkeyanonoption", false);
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
        	setTitle("Reply to " + (mAnonMode ? "shacker" : author));
        	
        	if (_messageMode)
        	{
        		_messageRecipient = extras.getString("parentAuthor");
        		if (postContent.length() > 5)
        		{
	        		EditText edit = (EditText)findViewById(R.id.textContent);
	        		edit.setText("\r\n\r\nPrevious message from " + author + ": \r\n" + postContent);
	        		edit.setSelection(0,0);
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
		findViewById(R.id.composeButtonSelectAll).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				((EditTextSelectionSavedAllowImage)findViewById(R.id.textContent)).selectAll();
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
	        PackageManager pm = ComposePostView.this.getPackageManager();

	        if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
		        menu.findItem(R.id.menu_compose_camera).setVisible(false);
	        }

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
	        menu.findItem(R.id.menu_compose_selectall).setVisible(showPostAsAction);
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
    
    @SuppressLint("SourceLockedOrientationActivity")
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

        new ImgurTools.RefreshAccessTokenTask().execute();

		EditText editBox = (EditText)findViewById(R.id.textContent);
		editBox.requestFocus();

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
			//setContentView(R.layout.edit_post);
			_landscape  = false;
		}
		else
		{
			//setContentView(R.layout.edit_post);
			_landscape = true;
		}
        //setContentView(R.layout.edit_post);
        if (decideEditBar())
        {

        }



		setupButtonBindings(null);
		
		editBox = (EditText)findViewById(R.id.textContent);
		editBox.setText(postText);
       //  editBox.setTextSize(TypedValue.COMPLEX_UNIT_PX, editBox.getTextSize() * _zoom);
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

		mTpl.loadTplsFromDisk();

	    if (_messageMode)
		    edit.post(new Runnable() {
			    @Override
			    public void run() {
				    edit.setSelection(0);
			    }
		    });
	}
	
	void appendText(String text) { appendText(text, true); }
	void appendText(String text, boolean newline)
	{
	    EditText edit = (EditText)findViewById(R.id.textContent);
	    if (newline)
	    {
		    // if there is text in there, put the image on a new line
		    if (edit.length() > 0 && !edit.getText().toString().endsWith(("\n")))
		        text = "\n" + " " + text + " ";
		    
		    edit.append(text + "\n");
	    }
	    else edit.append(" " + text + " ");
	    
	    System.out.println("composeview: append : " + text + " result: " + edit.getText().toString());
	}

	protected void showEmoji() {

		AlertDialog.Builder builder = new AlertDialog.Builder(ComposePostView.this);
		builder.setTitle("Terrible \"Emoji\"");

		GridView grid = new GridView(ComposePostView.this);
		grid.setNumColumns(3);
		grid.setHorizontalSpacing(2);
		grid.setVerticalSpacing(2);
		//grid.setNumColumns(GridView.AUTO_FIT);
		grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
		grid.setGravity(Gravity.CENTER);

		grid.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
			{
				EditText edit = (EditText)findViewById(R.id.textContent);
				edit.append(Character.toString(mEmoji.charAt(i)));
			}
		});


		ArrayList<String> itemList = new ArrayList<String>();
		for (int i = 0; i < mEmoji.length(); i++)
		{
			itemList.add(Character.toString(mEmoji.charAt(i)));
		}

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(ComposePostView.this,android.R.layout.simple_list_item_1, itemList);
		grid.setAdapter(adapter);
		builder.setView(grid);

		builder.setNegativeButton("Close", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub

			} });
		AlertDialog built = builder.create();
		built.setCanceledOnTouchOutside(true);
		built.show();
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
				itemList.add(PostFormatter.formatContent("", getPreviewFromHTML(_tagLabels[i]), null, true, true));
			}
		}
		if (macrosInstead)
		{
			// AMERICA item
			itemList.add(PostFormatter.formatContent("", getPreviewFromHTML("Macro: r{A}rMb{E}br{R}rIb{C}br{A}r"), null, true, true));
			// random
			itemList.add(PostFormatter.formatContent("", getPreviewFromHTML("Macro: r{R}rs[A]sb{N}b-[D]-l[O]l/[M]/p[!]p"), null, true, true));
			// ALLCAPS
			itemList.add(PostFormatter.formatContent("", getPreviewFromHTML("Macro: ALLCAPS"), null, true, true));
			// Christmas
			itemList.add(PostFormatter.formatContent("", getPreviewFromHTML("Macro: r{C}rg{H}gr{R}rg{I}gr{S}rg{T}gr{M}rg{A}gr{S}r"), null, true, true));
			// N U K E ' D
			itemList.add(PostFormatter.formatContent("", getPreviewFromHTML("Macro: *[r{*" + "&nbsp;" + "N" + "&nbsp;" + "U" + "&nbsp;" + "K" + "&nbsp;" + "E" + "&nbsp;" + "'" + "&nbsp;" + "D*}r]*"), null, true, true));
			// RAINBOW
			itemList.add(PostFormatter.formatContent("", getPreviewFromHTML("Macro: r{R}rn[A]ny{I}yl[N]lg{B}gb{O}bp[W]p"), null, true, true));
			// ZA???L?????G???O??????!???
			itemList.add(PostFormatter.formatContent("", getPreviewFromHTML("Macro: " + zalgo_text("Zalgo / Cthulhu")), null, true, true));
			// random caps
			itemList.add(PostFormatter.formatContent("", getPreviewFromHTML("Macro: rANdOm cApITalS"), null, true, true));

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

		// this is apparently no longer needed in new versions of android
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
		{
			if (andReselect)
			{
				_shackTagDialog.setOnDismissListener(new OnDismissListener()
				{

					@Override
					public void onDismiss(DialogInterface dialog)
					{
						EditText et = (EditText) findViewById(R.id.textContent);
						et.post(new Runnable()
						{

							@Override
							public void run()
							{
								System.out.println("POSTCOMPOSER: DO WORKAROUND");
								EditText et = (EditText) findViewById(R.id.textContent);
								// completely retarded workaround to bring the contextual action mode actionbar back
								et.setHapticFeedbackEnabled(false);
								et.performLongClick();
								et.setHapticFeedbackEnabled(true);
							}
						});
					}
				});
			}
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
        String str = edit.getText().toString().substring(start, end);
        if (str.length() > 0)
        {
	        String textToInsert = "";
	        if (mIsMacroItem)
	        {
		        switch (which)
		        {
			        case 0:
				        // AMERICA
				        for (int i = 0; i < str.length(); )
				        {
					        String curTag = ""; String curOpenBracket = "{"; String curCloseBracket = "}";
					        if ((i % 3) == 0) { curTag = "r"; curOpenBracket = "{"; curCloseBracket = "}"; }
					        if ((i % 3) == 1) { curTag = ""; curOpenBracket = ""; curCloseBracket = ""; }
					        if ((i % 3) == 2) { curTag = "b"; curOpenBracket = "{"; curCloseBracket = "}"; }
					        textToInsert = textToInsert + curTag + curOpenBracket + new StringBuilder().appendCodePoint(str.codePointAt(i)).toString() + curCloseBracket + curTag;

							i += Character.charCount(str.codePointAt(i));
				        }
				        break;
			        case 1:
				        // RANDOM
						for (int i = 0; i < str.length(); )
				        {
					        String curTag = ""; String curOpenBracket = "{"; String curCloseBracket = "}";

					        Random r = new Random(); int j = r.nextInt(13);

					        if (j == 0) { curTag = "r"; curOpenBracket = "{"; curCloseBracket = "}"; }
					        if (j == 1) { curTag = "g"; curOpenBracket = "{"; curCloseBracket = "}"; }
					        if (j == 2) { curTag = "b"; curOpenBracket = "{"; curCloseBracket = "}"; }
					        if (j == 3) { curTag = "y"; curOpenBracket = "{"; curCloseBracket = "}"; }
					        if (j == 4) { curTag = "l"; curOpenBracket = "["; curCloseBracket = "]"; }
					        if (j == 5) { curTag = "n"; curOpenBracket = "["; curCloseBracket = "]"; }
					        if (j == 6) { curTag = "p"; curOpenBracket = "["; curCloseBracket = "]"; }
					        if (j == 7) { curTag = "/"; curOpenBracket = "["; curCloseBracket = "]"; }
					        if (j == 8) { curTag = "b"; curOpenBracket = "["; curCloseBracket = "]"; }
					        if (j == 9) { curTag = "q"; curOpenBracket = "["; curCloseBracket = "]"; }
					        if (j == 10) { curTag = "s"; curOpenBracket = "["; curCloseBracket = "]"; }
					        if (j == 11) { curTag = "_"; curOpenBracket = "["; curCloseBracket = "]"; }
					        if (j == 12) { curTag = "-"; curOpenBracket = "["; curCloseBracket = "]"; }

					        textToInsert = textToInsert + curTag + curOpenBracket + new StringBuilder().appendCodePoint(str.codePointAt(i)).toString() + curCloseBracket + curTag;

							i += Character.charCount(str.codePointAt(i));
				        }
				        break;
			        case 2:
				        // ALLCAPS
				        textToInsert = str.toUpperCase();
				        break;
			        case 3:
				        // CHRISTMAS
						for (int i = 0; i < str.length(); )
				        {
					        String curTag = ""; String curOpenBracket = "{"; String curCloseBracket = "}";
					        if ((i % 2) == 0) { curTag = "r"; curOpenBracket = "{"; curCloseBracket = "}"; }
					        if ((i % 2) == 1) { curTag = "g"; curOpenBracket = "{"; curCloseBracket = "}"; }
					        textToInsert = textToInsert + curTag + curOpenBracket + new StringBuilder().appendCodePoint(str.codePointAt(i)).toString() + curCloseBracket + curTag;

							i += Character.charCount(str.codePointAt(i));
				        }
				        break;
			        case 4:
				        // NUKE'D
				        String textToStart = str.replaceAll("\\s+",""); // remove whitespace
				        textToStart = textToStart.toUpperCase(); // uppercase

				        for (int i = 0; i < textToStart.length(); i++)
				        {
					        textToInsert = textToInsert + textToStart.charAt(i);
					        // space it out
					        if (i < (textToStart.length() - 1))
					        {
						        textToInsert = textToInsert + " ";
					        }
				        }

				        textToInsert = "*[r{* " + textToInsert + " ' D *}r]*";

				        break;
			        case 5:
				        // RAINBOW
				        int j = 0;
						for (int i = 0; i < str.length(); )
				        {
					        if (Character.toString(str.charAt(i)).equals(" "))
					        {
						        textToInsert = textToInsert + " ";
						        i++;
						        continue;
					        }
					        else
					        {
						        String curTag = ""; String curOpenBracket = "{"; String curCloseBracket = "}";
						        if ((j % 7) == 0) { curTag = "r"; curOpenBracket = "{"; curCloseBracket = "}"; }
						        if ((j % 7) == 1) { curTag = "n"; curOpenBracket = "["; curCloseBracket = "]"; }
						        if ((j % 7) == 2) { curTag = "y"; curOpenBracket = "{"; curCloseBracket = "}"; }
						        if ((j % 7) == 3) { curTag = "l"; curOpenBracket = "["; curCloseBracket = "]"; }
						        if ((j % 7) == 4) { curTag = "g"; curOpenBracket = "{"; curCloseBracket = "}"; }
						        if ((j % 7) == 5) { curTag = "b"; curOpenBracket = "{"; curCloseBracket = "}"; }
						        if ((j % 7) == 6) { curTag = "p"; curOpenBracket = "["; curCloseBracket = "]"; }

						        textToInsert = textToInsert + curTag + curOpenBracket + new StringBuilder().appendCodePoint(str.codePointAt(i)).toString() + curCloseBracket + curTag;

						        j++;
					        }
							i += Character.charCount(str.codePointAt(i));
				        }
				        break;
			        case 6:
				        //  ZA???L?????G???O??????!???
				        textToInsert = zalgo_text(str);
				        break;
					case 7:
						// RaNdOm CaPS
						for (int i = 0; i < str.length(); )
						{


							Random r = new Random(); int k = r.nextInt(2); String chr = new StringBuilder().appendCodePoint(str.codePointAt(i)).toString();

							if (k == 0) { chr = chr.toLowerCase(); }
							if (k == 1) { chr = chr.toUpperCase(); }

							textToInsert = textToInsert + chr;

							i += Character.charCount(str.codePointAt(i));
						}
						break;
		        }
	        }
	        else
	        {
		        textToInsert = tags[which].substring(0, 2) + str + tags[which].substring(5);
	        }
	        edit.getText().replace(Math.min(start, end), Math.max(start, end), textToInsert, 0, textToInsert.length());
	        System.out.println("EDIT: SETSELECTION" + start + " " + end + " " + (Math.min(start, end) + textToInsert.length()));
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

        String content = EmojiParser.parseToHtmlDecimal(et.getText().toString());

	    if (content.length() < 6)
	    {

            content = pad(content, 6, " ");
            statInc(this, "LessThanSixChars");

	    }

		if ((content.length() > 3000) && (_replyToPostId == 0) && (!_messageMode))
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(ComposePostView.this);
			builder.setTitle("Giant Post");
			builder.setMessage("This post is way too big for a root post. You must trim it down.");
			builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					findViewById(R.id.composeButtonPost).setVisibility(View.VISIBLE);
				}
			});
			builder.create().show();
		}
	    else if ((content.length() > 4900) && (_replyToPostId != 0) && (!_messageMode))
		{
			final String fincontent = content;
			AlertDialog.Builder builder = new AlertDialog.Builder(ComposePostView.this);
			builder.setTitle("Giant Post");
			builder.setMessage("This post is way too big. It will be posted in multiple parts. Continue?");
			builder.setPositiveButton("Post in parts", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					String[] parts = splitInParts(fincontent, 4900);
					for (int i = 0; i < parts.length; i++)
					{
						new PostTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "Part " + (i + 1) + "\r\n\r\n" + parts[i]);
					}
				}
			});
			builder.setNegativeButton("Let me edit", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					findViewById(R.id.composeButtonPost).setVisibility(View.VISIBLE);
				}
			});
			builder.create().show();

		}
		else {
			// post in the background
			new PostTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, content);
		}
	}


	public String[] splitInParts(String s, int partLength)
	{
		int len = s.length();

		// Number of parts
		int nparts = (len + partLength - 1) / partLength;
		String parts[] = new String[nparts];

		// Break into parts
		int offset= 0;
		int i = 0;
		while (i < nparts)
		{
			parts[i] = s.substring(offset, Math.min(offset + partLength, len));
			offset += partLength;
			i++;
		}

		return parts;
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

                uploadImage(originalUri);
            } 
        }
        else if (requestCode == TAKE_PICTURE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                // picture was taken, and resides at the location we specified
	            File file = new File(mCurrentPhotoPath);
                uploadImage(Uri.fromFile(file));
            }
        }
	}

	void uploadImage(Uri imageUri)
	{
		// create dialog with thumbnail
		LinearLayout parent = new LinearLayout(this);
		parent.setPadding(2, 2, 2, 2);
		parent.setOrientation(LinearLayout.VERTICAL);
		TextView text = new TextView(this);
		text.setText("This will upload the selected image to the internet for public consumption. Continue?");
		text.setTextColor(Color.WHITE);
		text.setPadding(3, 3, 3, 3);
		ImageView image = new ImageView(this);
		image.setAdjustViewBounds(true);
		image.setScaleType(ImageView.ScaleType.FIT_CENTER);
		if (imageUri != null) { image.setImageURI(imageUri); System.out.println("UPLOADIMAGEuri: " + imageUri.toString()); }
		parent.addView(image,  new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
		parent.addView(text);
		new MaterialDialog.Builder(this)
				.title("Really upload?")
				.customView(parent, true)
				.btnStackedGravity(GravityEnum.END)
				.stackingBehavior(StackingBehavior.ALWAYS)
				.positiveText("Upload to ChattyPics")
				.onPositive(new MaterialDialog.SingleButtonCallback()
				{
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which)
					{
						_progressDialog = MaterialProgressDialog.show(ComposePostView.this, "Upload", "Uploading image to chattypics");
						if (imageUri != null)
						{
							new UploadUriAndInsertTask().execute(imageUri.toString(),"chattypics");
							statInc(ComposePostView.this, "ImagesToChattyPics");
						}
						else
						{
							_progressDialog.dismiss();
						}
					}
				})
				.negativeText("Upload to Imgur (" + (ImgurAuthorization.getInstance().isLoggedIn() ? "as " + ImgurAuthorization.getInstance().getUsername() + ")" : "anonymously)"))
				.onNegative(new MaterialDialog.SingleButtonCallback()
				{
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which)
					{
						_progressDialog = MaterialProgressDialog.show(ComposePostView.this, "Upload", "Uploading image to imgur");
						if (imageUri != null)
						{
							new UploadUriAndInsertTask().execute(imageUri.toString(),"imgur");
							statInc(ComposePostView.this, "ImagesToImgur");
						}
						else
						{
							_progressDialog.dismiss();
						}
					}
				})
				.neutralText("Do NOT Upload").show();
	}
	
	void postSuccessful(PostReference pr)
	{
	    Intent reply = new Intent();

		reply.putExtra("PQPId", pr.mPQPId);
	    reply.putExtra("parentPostId", pr.mParentPostId);
	    
	    setResult(RESULT_OK, reply);
	    
	    // start the postqueue service
		PostQueueService.enqueueWork(this, new Intent(this, PostQueueService.class));
	    // startService(msgIntent);
	    
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

                	int contentTypeId = _contentTypeId;
                	PostQueueObj pqo = new PostQueueObj(_replyToPostId, content, contentTypeId);
                	pqo.create(ComposePostView.this);
	                // JSONObject data = ShackApi.postReply(ComposePostView.this, _replyToPostId, content, _isNewsItem);
	                // int reply_id = data.getInt("post_insert_id");
	                // return Integer.valueOf(reply_id);
                	System.out.println("POSTCOMPOSER: PR = " + pqo.getPostQueueId());
                	return new PostReference(_replyToPostId, pqo.getPostQueueId());
                }
                else
                {

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

	public static String getMimeTypeOfUri(Context context, Uri uri) {
		BitmapFactory.Options opt = new BitmapFactory.Options();
		/* The doc says that if inJustDecodeBounds set to true, the decoder
		 * will return null (no bitmap), but the out... fields will still be
		 * set, allowing the caller to query the bitmap without having to
		 * allocate the memory for its pixels. */
		opt.inJustDecodeBounds = true;

		InputStream istream = null;
		try
		{
			istream = context.getContentResolver().openInputStream(uri);

			BitmapFactory.decodeStream(istream, null, opt);
			istream.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		MimeTypeMap mime = MimeTypeMap.getSingleton();

		return mime.getExtensionFromMimeType(opt.outMimeType);
	}

	class UploadUriAndInsertTask extends AsyncTask<String, Void, String>
	{
		Exception _exception;

		@Override
		protected String doInBackground(String... params)
		{
			try
			{
				Uri uri = Uri.parse(params[0]);
				String ext = getMimeTypeOfUri(ComposePostView.this, uri);
				InputStream inputstream = getContentResolver().openInputStream(uri);

				if ((ext == null) || (ext == "jpeg"))
				{
					ext = "jpg";
				}

				// resize jpg
				if (ext == "jpg")
				{
					System.out.println("UPLOADuri: RESIZE");
					Bitmap img = handleSamplingAndRotationBitmap(uri);
					final BitmapFactory.Options options = new BitmapFactory.Options();
					options.inPreferredConfig = Bitmap.Config.ARGB_8888;
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					img.compress(CompressFormat.JPEG, 100, stream);
					inputstream = new ByteArrayInputStream(stream.toByteArray());
				}

				boolean chattyPics = false;
				if (params[1].equalsIgnoreCase("chattypics"))
				{
					chattyPics = true;
				}
				if (chattyPics)
				{
					String userName = _prefs.getString("chattyPicsUserName", null);
					String password = _prefs.getString("chattyPicsPassword", null);

					// attempt to log in so the image will appear in the user's gallery
					String login_cookie = null;
					if (userName != null && password != null)
						login_cookie = ShackApi.loginToUploadImage(userName, password);

					// actually upload the thing
					String content = ShackApi.uploadImageFromInputStream(inputstream, login_cookie, ext);

					Pattern p = Pattern.compile("http\\:\\/\\/chattypics\\.com\\/viewer\\.php\\?file=(.*?)\"");
					Matcher match = p.matcher(content);

					if (match.find())
						return "http://chattypics.com/files/" + match.group(1);
				}
				else
				{
					String userName = _prefs.getString("imgurUserName", null);
					String password = _prefs.getString("imgurPassword", null);

					JSONObject response = uploadImageToImgur(inputstream);
					if (response != null)
					{
						String link = "";
						if (response.getJSONObject("data").has("gifv"))
						{
							link = response.getJSONObject("data").getString("gifv");
						}
						else if (response.getJSONObject("data").has("link"))
						{
							link = response.getJSONObject("data").getString("link");
						}
						return link;
					}
				}

				return null;
			}
			catch (Exception e)
			{
				Log.e("shackbrowse", "Error posting image", e);
				_exception = e;
				return null;
			}
		}

		public Bitmap handleSamplingAndRotationBitmap(Uri selectedImage) throws IOException {
			int MAX_HEIGHT = 1600;
			int MAX_WIDTH = 1600;

			// First decode with inJustDecodeBounds=true to check dimensions
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			InputStream imageStream = ComposePostView.this.getContentResolver().openInputStream(selectedImage);
			BitmapFactory.decodeStream(imageStream, null, options);
			imageStream.close();

			// Calculate inSampleSize
			options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);

			// Decode bitmap with inSampleSize set
			options.inJustDecodeBounds = false;
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			imageStream = ComposePostView.this.getContentResolver().openInputStream(selectedImage);
			Bitmap img = BitmapFactory.decodeStream(imageStream, null, options);

			img = rotateImageIfRequired(ComposePostView.this, img, selectedImage);
			return img;
		}
		private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
			// Raw height and width of image
			final int height = options.outHeight;
			final int width = options.outWidth;
			int inSampleSize = 1;

			if (height > reqHeight || width > reqWidth) {

				// Calculate ratios of height and width to requested height and width
				final int heightRatio = Math.round((float) height / (float) reqHeight);
				final int widthRatio = Math.round((float) width / (float) reqWidth);

				// Choose the smallest ratio as inSampleSize value, this will guarantee a final image
				// with both dimensions larger than or equal to the requested height and width.
				inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

				// This offers some additional logic in case the image has a strange
				// aspect ratio. For example, a panorama may have a much larger
				// width than height. In these cases the total pixels might still
				// end up being too large to fit comfortably in memory, so we should
				// be more aggressive with sample down the image (=larger inSampleSize).

				final float totalPixels = width * height;

				// Anything more than 2x the requested pixels we'll sample down further
				final float totalReqPixelsCap = reqWidth * reqHeight * 2;

				while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
					inSampleSize++;
				}
			}
			return inSampleSize;
		}
		private Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) throws IOException {

			InputStream input = context.getContentResolver().openInputStream(selectedImage);
			ExifInterface ei;
			if (Build.VERSION.SDK_INT > 23)
				ei = new ExifInterface(input);
			else
				ei = new ExifInterface(selectedImage.getPath());

			int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

			switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_90:
					return rotateImage(img, 90);
				case ExifInterface.ORIENTATION_ROTATE_180:
					return rotateImage(img, 180);
				case ExifInterface.ORIENTATION_ROTATE_270:
					return rotateImage(img, 270);
				default:
					return img;
			}
		}
		private Bitmap rotateImage(Bitmap img, int degree) {
			Matrix matrix = new Matrix();
			matrix.postRotate(degree);
			Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
			img.recycle();
			return rotatedImg;
		}

		@Override
		protected void onPostExecute(String result)
		{
			try {
				_progressDialog.dismiss();
			}
			catch (Exception e)
			{}

			if (_exception != null)
			{
				System.out.println("imgupload: err");
				ErrorDialog.display(ComposePostView.this, "Error", "Error posting:\n" + _exception.getMessage());
			}
			else if (result == null)
			{
				System.out.println("imgupload: err");
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


	// TEMPLATES
	/*
	 * KEYWORDS
	 */
	public void addTemplate()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Add Template");
		// Set up the input
		final LinearLayout lay = new LinearLayout(this);
		lay.setOrientation(LinearLayout.VERTICAL);
		final TextView tv = new TextView(this);
		tv.setText("Your post composer text will be saved as a template. Enter the name for this template.");
		final EditText input = new EditText(this);
		// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
		lay.addView(tv);
		lay.addView(input);
		builder.setView(lay);
		input.requestFocus();
		builder.setPositiveButton("Save Template", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				EditText edit = (EditText)findViewById(R.id.textContent);

				mTpl.saveThisTpl(input.getText().toString(), edit.getText().toString().replaceAll("\n", "<br/>"));

				showTemplates();
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showTemplates();
			}
		});
		AlertDialog alert = builder.create();
		alert.setCanceledOnTouchOutside(false);
		alert.show();
	}
	public void actionTemplate(final String keyword)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Take Action on Template");

		builder.setMessage("Load or delete `" + keyword + "`?");

		builder.setPositiveButton("Load", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				AlertDialog.Builder builder = new AlertDialog.Builder(ComposePostView.this);
				builder.setTitle("Really Load?"); builder.setMessage("Loading `" + keyword + "` will replace your current post text.");
				builder.setPositiveButton("Load", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						EditText edit = (EditText)findViewById(R.id.textContent);
						edit.setText(mTpl.mTpls.get(keyword).replaceAll("<br/>", "\n"));
						edit.setSelection(edit.getText().length());
					}
				});
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						showTemplates();
					}
				});
				AlertDialog alert = builder.create(); alert.setCanceledOnTouchOutside(false); alert.show();
			}
		});
		builder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				AlertDialog.Builder builder = new AlertDialog.Builder(ComposePostView.this);
				builder.setTitle("Really Delete?"); builder.setMessage("Deleting `" + keyword + "` will make you sad, possibly.");
				builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mTpl.deleteTplById(keyword);
						showTemplates();
					}
				});
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						showTemplates();
					}
				});
				AlertDialog alert = builder.create(); alert.setCanceledOnTouchOutside(false); alert.show();
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showTemplates();
			}
		});
		AlertDialog alert = builder.create();
		alert.setCanceledOnTouchOutside(false);
		alert.show();
	}
	public void showTemplates()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Stored Post Templates");

		final CharSequence[] items = mTpl.mTpls.keySet().toArray(new CharSequence[mTpl.mTpls.keySet().size()]);
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				actionTemplate(items[item].toString());
			}});
		builder.setNegativeButton("Close", null);
		builder.setPositiveButton("Save Current as Template", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				addTemplate();
			}
		});
		AlertDialog alert = builder.create();
		alert.setCanceledOnTouchOutside(false);
		alert.show();
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
        .replaceAll("(?i)(https?:\\/\\/(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|https?:\\/\\/(?:www\\.|(?!www))[a-zA-Z0-9]\\.[^\\s]{2,}|www\\.[a-zA-Z0-9]\\.[^\\s]{2,})","<a href=\"$1\">$1</a>")
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


    // ***********************
	// **** ZALGO  CRAP *****
	// *******************

	// data set of leet unicode chars
	//---------------------------------------------------

	//those go UP
	final char[] zalgo_up = {
			'\u030d', /*     ?     */        '\u030e', /*     ?     */        '\u0304', /*     ?     */        '\u0305', /*     ?     */
			'\u033f', /*     ?     */        '\u0311', /*     ?     */        '\u0306', /*     ?     */        '\u0310', /*     ?     */
			'\u0352', /*     ?     */        '\u0357', /*     ?     */        '\u0351', /*     ?     */        '\u0307', /*     ?     */
			'\u0308', /*     ?     */        '\u030a', /*     ?     */        '\u0342', /*     ?     */        '\u0343', /*     ?     */
			'\u0344', /*     ?     */        '\u034a', /*     ?     */        '\u034b', /*     ?     */        '\u034c', /*     ?     */
			'\u0303', /*     ?     */        '\u0302', /*     ?     */        '\u030c', /*     ?     */        '\u0350', /*     ?     */
			'\u0300', /*     ?     */        '\u0301', /*     ?     */        '\u030b', /*     ?     */        '\u030f', /*     ?     */
			'\u0312', /*     ?     */        '\u0313', /*     ?     */        '\u0314', /*     ?     */        '\u033d', /*     ?     */
			'\u0309', /*     ?     */        '\u0363', /*     ?     */        '\u0364', /*     ?     */        '\u0365', /*     ?     */
			'\u0366', /*     ?     */        '\u0367', /*     ?     */        '\u0368', /*     ?     */        '\u0369', /*     ?     */
			'\u036a', /*     ?     */        '\u036b', /*     ?     */        '\u036c', /*     ?     */        '\u036d', /*     ?     */
			'\u036e', /*     ?     */        '\u036f', /*     ?     */        '\u033e', /*     ?     */        '\u035b', /*     ?     */
			'\u0346', /*     ?     */        '\u031a' /*     ?     */
	};

	//those go DOWN
	final char[] zalgo_down = {
			'\u0316', /*     ?     */        '\u0317', /*     ?     */        '\u0318', /*     ?     */        '\u0319', /*     ?     */
			'\u031c', /*     ?     */        '\u031d', /*     ?     */        '\u031e', /*     ?     */        '\u031f', /*     ?     */
			'\u0320', /*     ?     */        '\u0324', /*     ?     */        '\u0325', /*     ?     */        '\u0326', /*     ?     */
			'\u0329', /*     ?     */        '\u032a', /*     ?     */        '\u032b', /*     ?     */        '\u032c', /*     ?     */
			'\u032d', /*     ?     */        '\u032e', /*     ?     */        '\u032f', /*     ?     */        '\u0330', /*     ?     */
			'\u0331', /*     ?     */        '\u0332', /*     ?     */        '\u0333', /*     ?     */        '\u0339', /*     ?     */
			'\u033a', /*     ?     */        '\u033b', /*     ?     */        '\u033c', /*     ?     */        '\u0345', /*     ?     */
			'\u0347', /*     ?     */        '\u0348', /*     ?     */        '\u0349', /*     ?     */        '\u034d', /*     ?     */
			'\u034e', /*     ?     */        '\u0353', /*     ?     */        '\u0354', /*     ?     */        '\u0355', /*     ?     */
			'\u0356', /*     ?     */        '\u0359', /*     ?     */        '\u035a', /*     ?     */        '\u0323' /*     ?     */
	};

	//those always stay in the middle
	final char[] zalgo_mid = {
			'\u0315', /*     ?     */        '\u031b', /*     ?     */        '\u0340', /*     ?     */        '\u0341', /*     ?     */
			'\u0358', /*     ?     */        '\u0321', /*     ?     */        '\u0322', /*     ?     */        '\u0327', /*     ?     */
			'\u0328', /*     ?     */        '\u0334', /*     ?     */        '\u0335', /*     ?     */        '\u0336', /*     ?     */
			'\u034f', /*     ?     */        '\u035c', /*     ?     */        '\u035d', /*     ?     */        '\u035e', /*     ?     */
			'\u035f', /*     ?     */        '\u0360', /*     ?     */        '\u0362', /*     ?     */        '\u0338', /*     ?     */
			'\u0337', /*     ?     */        '\u0361', /*     ?     */        '\u0489' /*     ?_     */
	};

	// rand funcs
	//---------------------------------------------------

	//gets an int between 0 and max
	public int rand(int max)
	{
		return (int) Math.floor(Math.random() * max);
	}

	//gets a random char from a zalgo char table
	public char rand_zalgo(char[] array)
	{
		int ind = (int) Math.floor(Math.random() * array.length);
		return array[ind];
	}

	// utils funcs
	//---------------------------------------------------


	//lookup char to know if its a zalgo char or not
	public boolean is_zalgo_char(char c)
	{
		int i;
		for(i=0; i<zalgo_up.length; i++)
			if(c == zalgo_up[i])
				return true;
		for(i=0; i<zalgo_down.length; i++)
			if(c == zalgo_down[i])
				return true;
		for(i=0; i<zalgo_mid.length; i++)
			if(c == zalgo_mid[i])
				return true;
		return false;
	}

	// main shit
	//---------------------------------------------------
	public String zalgo_text(String txt)
	{
		String newtxt = "";

		for(int i=0; i<txt.length(); i++)
		{
			if(is_zalgo_char(txt.charAt(i)))
				continue;

			int num_up;
			int num_mid;
			int num_down;

			//add the normal character
			newtxt += txt.charAt(i);

			//options
			if(true) // mini
			{
				num_up = rand(8);
				num_mid = rand(2);
				num_down = rand(8);
			}
			else if(false) // norm
			{
				num_up = rand(16) / 2 + 1;
				num_mid = rand(6) / 2;
				num_down = rand(16) / 2 + 1;
			}
			else //maxi
			{
				num_up = rand(64) / 4 + 3;
				num_mid = rand(16) / 4 + 1;
				num_down = rand(64) / 4 + 3;
			}
			/*
			if(document.getElementById('zalgo_opt_up').checked)
				for(var j=0; j<num_up; j++)
					newtxt += rand_zalgo(zalgo_up);
			*/

				for(int j=0; j<num_mid; j++)
					newtxt += Character.toString(rand_zalgo(zalgo_mid));

				for(int j=0; j<num_down; j++)
					newtxt += Character.toString(rand_zalgo(zalgo_down));
		}

		//result is in nextxt, display that

		return newtxt;

		//done
	}


}