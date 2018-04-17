package net.woggle.shackbrowse;

import java.io.File;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.woggle.shackbrowse.legacy.LegacyFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.ClipboardManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import static net.woggle.shackbrowse.StatsFragment.statInc;

public class PicUploader extends AppCompatActivity {
	private static final int MY_PERMISSIONS_REQUEST_READ_EXT = 20;
	private MaterialDialog _progressDialog;
	
	
	SharedPreferences _prefs;
	private Uri mImageUri;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        // prefs
        _prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // grab the post being replied to, if this is a reply
        statInc(this, "UsedStandaloneChattyPicsUploader");
        setContentView(R.layout.picupload);
        String action = getIntent().getAction();
        String type = getIntent().getType();
        
        if (Intent.ACTION_SEND.equals(action) && type != null)
        {
        	// sent either text intent or image which should be uploaded to chattypics
         
	        if (type.startsWith("image/"))
	        {

		        mImageUri = (Uri)getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
		        
		        
		        if (ContextCompat.checkSelfPermission(this,
				        Manifest.permission.READ_EXTERNAL_STORAGE)
				        != PackageManager.PERMISSION_GRANTED) {


				        // No explanation needed, we can request the permission.

				        ActivityCompat.requestPermissions(this,
						        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
						        MY_PERMISSIONS_REQUEST_READ_EXT);

				        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
				        // app-defined int constant. The callback method gets the
				        // result of the request.

		        }
		        else
		        {
			        uploadURI();
		        }

	        }
        }
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_READ_EXT: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {

					// permission was granted, yay! Do the
					uploadURI();

				} else {

					// permission denied, boo! Disable the
					// functionality that depends on this permission.
				}
				return;
			}

			// other 'case' lines to check for other
			// permissions this app might request
		}
	}
	
	public void uploadURI () {
		System.out.println("uploadin");
		

		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 4;

		final String realPath = getRealPathFromURI(mImageUri);

		Bitmap bm = BitmapFactory.decodeFile(realPath,options);
		((ImageView)findViewById(R.id.picUploadBG)).setImageBitmap(bm);

		// ((ImageView)findViewById(R.id.picUploadBG)).setImageURI(imageUri);

		new MaterialDialog.Builder(this)
				.title("Really upload?")
				.content("This will upload the selected image to the internet for public consumption. Continue?")
				.positiveText("Upload It")
				.onPositive(new MaterialDialog.SingleButtonCallback()
				{
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which)
					{
						uploadImage(realPath);
					}
				}).negativeText("Do NOT Upload")
				.onNegative(new MaterialDialog.SingleButtonCallback()
				{
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which)
					{
						finish();
					}
				}).show();
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
	    _progressDialog = new MaterialDialog.Builder(this)
				.title("Upload")
				.content("Uploading image to chattypics")
				.progress(true, 0)
				.show();
	    new UploadAndInsertTask().execute(imageLocation);
        statInc(this, "ImagesToChattyPics");
	}
	
	
	class UploadAndInsertTask extends AsyncTask<String, String, String>
	{
	    Exception _exception;
	    
	    @Override 
	    protected void onProgressUpdate(String...params)
	    {
		    if ((_progressDialog != null) && (_progressDialog.isShowing()))
		    {
		    	try
			    {
				    _progressDialog.setContent(params[0]);
			    }
			    catch (Exception e)
			    {
				    System.out.println("FAILED" + params[0] + e.getMessage());
			    }
		    }
	    	System.out.println("X" + params[0]);
	    }
	    
        @Override
        protected String doInBackground(String... params)
        {
            try
            {
                String imageLocation = params[0];
                
                publishProgress("Resizing Image... ");
                // resize the image for faster uploading
                String smallImageLocation = resizeImage(imageLocation);
                
                String userName = _prefs.getString("chattyPicsUserName", null);
                String password = _prefs.getString("chattyPicsPassword", null);
                
                publishProgress("Logging in to ChattyPics... ");
                // attempt to log in so the image will appear in the user's gallery
                String login_cookie = null;
                if (userName != null && password != null)
                    login_cookie = ShackApi.loginToUploadImage(userName, password);
                
                // actually upload the thing
                publishProgress("Uploading Image... ");
                String content = ShackApi.uploadImage(smallImageLocation, login_cookie);
                System.out.println(content);
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
                Log.e("shackbrowse", "Error uploading image", e);
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
                ErrorDialog.display(PicUploader.this, "Error", "Error uploading:\n" + _exception.getMessage());
            }
            else if (result == null)
            {
            	System.out.println("camupload: err");
                ErrorDialog.display(PicUploader.this, "Error", "Couldn't find image URL after uploading.");
            }
            else
            {
            	final String result1 = result;
            	runOnUiThread(new Runnable(){
            		@Override public void run()
            		{
                        AlertDialog.Builder builder = new AlertDialog.Builder(PicUploader.this);
            	        builder.setTitle("Image Uploaded");
            	        builder.setMessage("Do what with the image URL?");
            	        builder.setCancelable(true);
            	        builder.setPositiveButton("New Root Shack Post", new DialogInterface.OnClickListener() {
            				public void onClick(DialogInterface dialog, int id) {
            					Intent sendIntent = new Intent();
            				    sendIntent.setAction(Intent.ACTION_SEND);
            				    sendIntent.putExtra(Intent.EXTRA_TEXT, result1);
            				    sendIntent.setType("text/plain");
            				    sendIntent.setClass(getApplication(), MainActivity.class);
            				    startActivity(sendIntent);
            				    finish();
            	            }
            	        });
            	        builder.setNeutralButton("Append to Clipboard", new DialogInterface.OnClickListener() {
            				public void onClick(DialogInterface dialog, int id) {

                    			ClipboardManager clipboard = (ClipboardManager)getSystemService(Activity.CLIPBOARD_SERVICE);
                    		
                    	    	clipboard.setText(clipboard.getText() + "\n" + result1);
                    	    	Toast.makeText(PicUploader.this, clipboard.getText(), Toast.LENGTH_LONG).show();
                    	    	finish();
            	            }
            	        });
            	        builder.setNegativeButton("Replace Clipboard", new DialogInterface.OnClickListener() {
            				public void onClick(DialogInterface dialog, int id) {

                    			ClipboardManager clipboard = (ClipboardManager)getSystemService(Activity.CLIPBOARD_SERVICE);
                    	    	clipboard.setText(result1);
                    	    	Toast.makeText(PicUploader.this, result1, Toast.LENGTH_LONG).show();
                    	    	finish();
            	            }
            	        });
            	        builder.create().show();
            		}
            	});

            }
        }
	}
}