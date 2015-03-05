package net.woggle.shackbrowse;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.woggle.shackbrowse.legacy.LegacyFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
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
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBarActivity;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialogCompat;

public class ComposePostView extends ActionBarActivity {

	protected static final int SELECT_IMAGE = 0;
	protected static final int TAKE_PICTURE = 1;
	
	static final long MAX_SIZE_LOGGED_IN = 6 * 1024 * 1024;
	static final long MAX_SIZE_NOT_LOGGED_IN = 3 * 1024 * 1024;
	
	final String[] _tagLabels = { "r{red}r", "/[italics]/", "g{green}g", "b[bold]b", "b{blue}b", "q[quote]q", "y{yellow}y", "s[small]s", "e[olive]e", "_[underline]_",  "l[lime]l", "-[strike]-", "n[orange]n", "spoilo[er]o", "p[multisync]p"};	        
    final String[] _tags = {      "r{...}r",       "/[...]/",         "g{...}g",          "b[...]b",      "b{...}b",      "q[...]q",          "y{...}y",          "s[...]s",          "e[...]e",          "_[...]_",    	        "l[...]l",            "-[...]-",	        "n[...]n",	        "o[...]o",	        "p[...]p"};
	
	private boolean _isNewsItem = false;
    private int _replyToPostId = 0;
	private MaterialDialog _progressDialog;
	
	float _zoom = 1.0f;
	
	Uri _cameraImageLocation;
	
	SharedPreferences _prefs;
	private int _orientLock = 0;
	private int _forcePostPreview = 1;
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

    @Override
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        // prefs
        _prefs = PreferenceManager.getDefaultSharedPreferences(this);

        mThemeResId = MainActivity.themeApplicator(this);
        
        _forcePostPreview  = Integer.parseInt(_prefs.getString("forcePostPreview", "1"));
        
        // drafts
        _drafts = new Drafts(this);
        
        // orientation
        doOrientation();
        
        if (_orientLock == 2 || _orientLock == 4 || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
        	setContentView(R.layout.edit_post);
        }
        else
        {
        	setContentView(R.layout.edit_post);
        }
        
        // home button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // zoom handling
        _zoom = Float.parseFloat(_prefs.getString("fontZoom", "1.0"));
        EditText editBox = (EditText)findViewById(R.id.textContent);
        editBox.setTextSize(TypedValue.COMPLEX_UNIT_PX, editBox.getTextSize() * _zoom);
        
        editBox.setCustomSelectionActionModeCallback(new StyleCallback());
        
        // grab the post being replied to, if this is a reply
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(MainActivity.THREAD_ID))
        {
            _replyToPostId = getIntent().getExtras().getInt(MainActivity.THREAD_ID);
            
            if (extras.containsKey(MainActivity.IS_NEWS_ITEM))
            	_isNewsItem = extras.getBoolean(MainActivity.IS_NEWS_ITEM);
        }
        
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
	        String realPath = getRealPathFromURI(selectedImage);
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
                _cameraImageLocation = Uri.parse(savedInstanceState.getString("cameraImageLocation"));
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
        	case R.id.menu_compose_settings:
                showSettings();
                break;
        	case R.id.menu_compose_markup:
                openMarkupSelector(false);
                break;
        	case R.id.menu_compose_post:
                postClick();
                break;
        	case R.id.menu_compose_picture:
        		startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI), SELECT_IMAGE);
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
            
        }
        return super.onOptionsItemSelected(item);
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
        _forcePostPreview  = Integer.parseInt(_prefs.getString("forcePostPreview", "1"));
        
        if (_replyToPostId == 0)
        {
        	menu.findItem(R.id.menu_compose_showParent).setVisible(false);
        }
        if (!LegacyFactory.getLegacy().hasCamera(this))
        	menu.findItem(R.id.menu_compose_camera).setVisible(false);
		return true;
    }
    private void showSettings()
    {
        Intent i = new Intent(this, PreferenceView.class);
        i.putExtra("pscreenkey", "postcomposer");
        startActivityForResult(i, ThreadListFragment.OPEN_PREFS);
    }
    
    public void showParentPost()
    {
        MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(ComposePostView.this);
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
    	EditText edit = (EditText)findViewById(R.id.textContent);
		String postContent = edit.getText().toString();
		postContent = getPreviewFromHTML(postContent);


        MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(ComposePostView.this);
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
			
		} });
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
            MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(ComposePostView.this);
	        builder.setTitle((!_messageMode) ? "Delete or Save Draft" : "Stay or Exit");
	        String whatDo = (_replyToPostId == 0) ? "create a new topic post" : "reply to this post";
	        if (!_messageMode)
	        	builder.setMessage("You have typed a post. If you save a draft, the next time you " + whatDo + ", your entered text will automatically be loaded. Otherwise, you may choose to delete this post entirely, or stay in the composer.");
	        else
	        	builder.setMessage("You have typed a post. You may choose to delete this post entirely and exit, or stay in the composer.");
	        
	        builder.setNegativeButton("Stay", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
			} });
	        
	        // message mode supports no drafts
	        if (!_messageMode)
	        {
		        builder.setNeutralButton("Save and Exit", new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// save
						_preventDraftSave = false;
						finish();
				} });
		        builder.setPositiveButton("Delete and Exit", new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// delete any drafts
						
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
	    
	    if (_cameraImageLocation != null)
    	    outState.putString("cameraImageLocation", _cameraImageLocation.toString());
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
			setContentView(R.layout.edit_post);
			//getSupportActionBar().show();
			_landscape  = false;
		}
		else
		{
			setContentView(R.layout.edit_post);
			_landscape = true;
			//getSupportActionBar().hide();
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
	

	OnItemClickListener onShackTagSelected =	new OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int which, long arg3) {
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
				String textToInsert = tags[which].substring(0, 2) + seltext + tags[which].substring(5);
		        edit.getText().replace(Math.min(start, end), Math.max(start, end), textToInsert, 0, textToInsert.length());
		        edit.setSelection(Math.min(start, end), Math.min(start, end) + textToInsert.length());
			}
			else
			{
				if (!_prefs.getBoolean("hasSeenMarkupTip", false))
				{
                    MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(ComposePostView.this);
					builder.setTitle("Markup Button Tip");
					builder.setMessage("You can long-press on a word to select the word in Android. Once a word is selected, you can hit the markup button and that markup will be applied. You can do this multiple times to easily add multiple markups to a text selection.");
					builder.setNegativeButton("Never show this again", null);
					builder.show();
					
					Editor editor = _prefs.edit();
					editor.putBoolean("hasSeenMarkupTip", true);
					editor.apply();
				}
				String textToInsert = tags[which].substring(0, 2) + tags[which].substring(5);
		        edit.getText().replace(Math.min(start, end), Math.max(start, end), textToInsert, 0, textToInsert.length());
		        edit.setSelection(Math.min(start, end) +2);
				/*
				AlertDialog.Builder builder = new AlertDialog.Builder(ComposePostView.this);
				builder.setTitle("Insert text into " + tags[which]);

				// make final
				final int tagtype = which;
				// Set up the input
				final EditText input = new EditText(ComposePostView.this);
				// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
				input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
		        
				input.setText(edit.getText().toString().substring(start, end));
				builder.setView(input);

				// Set up the buttons
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { 
				    @Override
				    public void onClick(DialogInterface dialog, int which2) {
				    	
				        String textToInsert = tags[tagtype].substring(0, 2) + input.getText().toString() + tags[tagtype].substring(5);
				        EditText edit = (EditText)findViewById(R.id.textContent);
				        int start = edit.getSelectionStart();
				        int end = edit.getSelectionEnd();
				        edit.getText().replace(Math.min(start, end), Math.max(start, end), textToInsert, 0, textToInsert.length());
				    }
				});
				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				    @Override
				    public void onClick(DialogInterface dialog, int which) {
				        dialog.cancel();
				    }
				});

				builder.show();
				*/
			}
			
			
		}		
	};
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
            /* Old Behavior, make the user fix it


	    	// posts must be 6 chars long
	    	AlertDialog.Builder builder = new AlertDialog.Builder(ComposePostView.this);
	        builder.setTitle("Post too short");
	        builder.setMessage("Shacknews will not allow posts below 6 characters long.");
	        builder.setNegativeButton("OK", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
			} });
	        AlertDialog alert = builder.create();
	        alert.setCanceledOnTouchOutside(true);
	        alert.show();
            */
	    }

        // post in the background
        new PostTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, content);
	}
	
	protected void openMarkupSelector(boolean andReselect) {
        MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(ComposePostView.this);
        builder.setTitle("Select Shack Tag");
        
        GridView grid = new GridView(ComposePostView.this);
        grid.setNumColumns(3);
        grid.setHorizontalSpacing(2);
        grid.setVerticalSpacing(2);
        grid.setGravity(Gravity.CENTER);
        grid.setOnItemClickListener(onShackTagSelected);
        
        ArrayList<Spanned> itemList = new ArrayList<Spanned>();
        for (int i = 0; i < _tagLabels.length; i++)
        {
        	itemList.add(PostFormatter.formatContent("bradsh", getPreviewFromHTML(_tagLabels[i]), null, true, true));
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

	public void openCameraSelector()
	{
		// store our image in a temp spot
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state))
	    {
	        // application directory, per Android Data Storage guidelines
	        final String APP_DIRECTORY = "/Android/data/net.woggle.shackbrowse/files/";
	        File app_dir = new File(Environment.getExternalStorageDirectory(), APP_DIRECTORY);
	        
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
		    File file = new File(app_dir, "shackbrowseUpload.jpg");
		    if (file.exists())
		        file.delete();
		    
		    _cameraImageLocation = Uri.fromFile(file);
		    
		    // start the camera
		    Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		    i.putExtra(MediaStore.EXTRA_OUTPUT, _cameraImageLocation);
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
        
        if (requestCode == SELECT_IMAGE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                Uri selectedImage = data.getData();
                String realPath = getRealPathFromURI(selectedImage);
                uploadImage(realPath);
            } 
        }
        else if (requestCode == TAKE_PICTURE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                // picture was taken, and resides at the location we specified
                uploadImage(_cameraImageLocation.getPath());
            }
        }
	}
	
	// convert the image URI to the direct file system path of the image file
    public String getRealPathFromURI(Uri contentUri) {

        String [] proj= { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        if (cursor != null)
        {
	        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	        cursor.moveToFirst();
	
	        return cursor.getString(column_index);
        }
        else
        	return contentUri.getPath();
}
	
	void uploadImage(String imageLocation)
	{
	    _progressDialog = MaterialProgressDialog.show(ComposePostView.this, "Upload", "Uploading image to chattypics");
	    new UploadAndInsertTask().execute(imageLocation);
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
                    MaterialDialogCompat.Builder builder = new MaterialDialogCompat.Builder(ComposePostView.this);
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
            Bitmap original = BitmapFactory.decodeFile(path);
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
            FileOutputStream f = new FileOutputStream(newFile);
            try
            {
                resized.compress(CompressFormat.JPEG, 97, f);
            }
            finally
            {
                f.close();
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
	    		if (menu.findItem(android.R.id.selectAll) != null)
	    			menu.findItem(android.R.id.selectAll).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM|MenuItem.SHOW_AS_ACTION_WITH_TEXT);
	    	}
	    	else
	    	{
	    		menu.findItem(R.id.menu_compMarkup).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
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
	        }
	        return false;
	    }

	    public void onDestroyActionMode(ActionMode mode) {
	    }
	}
}