package net.woggle.shackbrowse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.text.ClipboardManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.StackingBehavior;

import net.woggle.shackbrowse.imgur.ImgurAuthorization;

import org.json.JSONObject;

import static net.woggle.shackbrowse.StatsFragment.statInc;
import static net.woggle.shackbrowse.imgur.ImgurTools.uploadImageToImgur;

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
				        android.Manifest.permission.READ_EXTERNAL_STORAGE)
				        != PackageManager.PERMISSION_GRANTED) {


				        // No explanation needed, we can request the permission.

				        ActivityCompat.requestPermissions(this,
						        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
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



	void uploadURI()
	{
		Uri imageUri = mImageUri;
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
						_progressDialog = MaterialProgressDialog.show(PicUploader.this, "Upload", "Uploading image to chattypics");
						if (imageUri != null)
						{
							new UploadUriAndInsertTask().execute(imageUri.toString(),"chattypics");
							statInc(PicUploader.this, "ImagesToChattyPics");
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
						_progressDialog = MaterialProgressDialog.show(PicUploader.this, "Upload", "Uploading image to imgur");
						if (imageUri != null)
						{
							new UploadUriAndInsertTask().execute(imageUri.toString(),"imgur");
							statInc(PicUploader.this, "ImagesToImgur");
						}
						else
						{
							_progressDialog.dismiss();
						}
					}
				})
				.neutralText("Do NOT Upload").show();
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
				String ext = getMimeTypeOfUri(PicUploader.this, uri);
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
			InputStream imageStream = PicUploader.this.getContentResolver().openInputStream(selectedImage);
			BitmapFactory.decodeStream(imageStream, null, options);
			imageStream.close();

			// Calculate inSampleSize
			options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);

			// Decode bitmap with inSampleSize set
			options.inJustDecodeBounds = false;
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			imageStream = PicUploader.this.getContentResolver().openInputStream(selectedImage);
			Bitmap img = BitmapFactory.decodeStream(imageStream, null, options);

			img = rotateImageIfRequired(PicUploader.this, img, selectedImage);
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
				ErrorDialog.display(PicUploader.this, "Error", "Error posting:\n" + _exception.getMessage());
			}
			else if (result == null)
			{
				System.out.println("imgupload: err");
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
						builder.setPositiveButton("New Root Shack Post", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								Intent sendIntent = new Intent();
								sendIntent.setAction(Intent.ACTION_SEND);
								sendIntent.putExtra(Intent.EXTRA_TEXT, result1);
								sendIntent.setType("text/plain");
								sendIntent.setClass(getApplication(), MainActivity.class);
								startActivity(sendIntent);
								finish();
							}
						});
						builder.setNeutralButton("Append to Clipboard", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{

								ClipboardManager clipboard = (ClipboardManager) getSystemService(Activity.CLIPBOARD_SERVICE);

								clipboard.setText(clipboard.getText() + "\n" + result1);
								Toast.makeText(PicUploader.this, clipboard.getText(), Toast.LENGTH_LONG).show();
								finish();
							}
						});
						builder.setNegativeButton("Replace Clipboard", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{

								ClipboardManager clipboard = (ClipboardManager) getSystemService(Activity.CLIPBOARD_SERVICE);
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

	void uploadURI_old()
	{
		Uri imageUri = mImageUri;
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
				.positiveText("Upload It")
				.onPositive(new MaterialDialog.SingleButtonCallback()
				{
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which)
					{
						_progressDialog = MaterialProgressDialog.show(PicUploader.this, "Upload", "Uploading image to chattypics");
						if (imageUri != null)
						{
							new UploadUriAndInsertTask_Old().execute(imageUri);
							statInc(PicUploader.this, "ImagesToChattyPics");
						}
						else
						{
							_progressDialog.dismiss();
						}
					}
				}).negativeText("Do NOT Upload").show();
	}

	class UploadUriAndInsertTask_Old extends AsyncTask<Uri, Void, String>
	{
		Exception _exception;

		@Override
		protected String doInBackground(Uri... params)
		{
			try
			{
				Uri uri = params[0];
				String ext = getMimeTypeOfUri(PicUploader.this, uri);
				InputStream inputstream = getContentResolver().openInputStream(uri);

				if (ext == null)
				{
					ext = "jpg";
				}

				// resize jpeg
				if ((ext == "jpg") || (ext == "jpeg"))
				{
					System.out.println("UPLOADuri: RESIZE");
					Bitmap img = handleSamplingAndRotationBitmap(uri);
					final BitmapFactory.Options options = new BitmapFactory.Options();
					options.inPreferredConfig = Bitmap.Config.ARGB_8888;
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					img.compress(CompressFormat.JPEG, 100, stream);
					inputstream = new ByteArrayInputStream(stream.toByteArray());
				}

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

				return null;
			}
			catch (Exception e)
			{
				Log.e("shackbrowse", "Error posting image", e);
				_exception = e;
				return null;
			}
		}

		public Bitmap handleSamplingAndRotationBitmap(Uri selectedImage) throws IOException
		{
			int MAX_HEIGHT = 1600;
			int MAX_WIDTH = 1600;

			// First decode with inJustDecodeBounds=true to check dimensions
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			InputStream imageStream = PicUploader.this.getContentResolver().openInputStream(selectedImage);
			BitmapFactory.decodeStream(imageStream, null, options);
			imageStream.close();

			// Calculate inSampleSize
			options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);

			// Decode bitmap with inSampleSize set
			options.inJustDecodeBounds = false;
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			imageStream = PicUploader.this.getContentResolver().openInputStream(selectedImage);
			Bitmap img = BitmapFactory.decodeStream(imageStream, null, options);

			img = rotateImageIfRequired(PicUploader.this, img, selectedImage);
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
				ErrorDialog.display(PicUploader.this, "Error", "Error posting:\n" + _exception.getMessage());
			}
			else if (result == null)
			{
				System.out.println("imgupload: err");
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
						builder.setPositiveButton("New Root Shack Post", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								Intent sendIntent = new Intent();
								sendIntent.setAction(Intent.ACTION_SEND);
								sendIntent.putExtra(Intent.EXTRA_TEXT, result1);
								sendIntent.setType("text/plain");
								sendIntent.setClass(getApplication(), MainActivity.class);
								startActivity(sendIntent);
								finish();
							}
						});
						builder.setNeutralButton("Append to Clipboard", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{

								ClipboardManager clipboard = (ClipboardManager) getSystemService(Activity.CLIPBOARD_SERVICE);

								clipboard.setText(clipboard.getText() + "\n" + result1);
								Toast.makeText(PicUploader.this, clipboard.getText(), Toast.LENGTH_LONG).show();
								finish();
							}
						});
						builder.setNegativeButton("Replace Clipboard", new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{

								ClipboardManager clipboard = (ClipboardManager) getSystemService(Activity.CLIPBOARD_SERVICE);
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