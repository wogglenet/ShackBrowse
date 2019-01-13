package net.woggle.shackbrowse.imgur;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class ImgurTools
{
	private static final String TAG = ImgurTools.class.getSimpleName();
	private static final String UPLOAD_URL = "https://api.imgur.com/3/image";

	public static class RefreshAccessTokenTask extends AsyncTask<Void, Void, String> {

		@Override
		protected String doInBackground(Void... params) {
			String accessToken = ImgurAuthorization.getInstance().requestNewAccessToken();
			if (!TextUtils.isEmpty(accessToken)) {
				Log.i(TAG, "Got new access token");
			}
			else {
				Log.i(TAG, "Could not get new access token");
			}
			return accessToken;
		}
	}
	private static int copy(InputStream input, OutputStream output) throws IOException
	{
		byte[] buffer = new byte[8192];
		int count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}
	protected static JSONObject onInput(InputStream in) throws Exception {
		StringBuilder sb = new StringBuilder();
		Scanner scanner = new Scanner(in);
		while (scanner.hasNext()) {
			sb.append(scanner.next());
		}

		JSONObject root = new JSONObject(sb.toString());
		String id = root.getJSONObject("data").getString("id");
		String deletehash = root.getJSONObject("data").getString("deletehash");

		Log.e(TAG, sb.toString());
		Log.i(TAG, "new imgur url: http://imgur.com/" + id + " (delete hash: " + deletehash + ")");
		return root;
	}
	public static JSONObject uploadImageToImgur (InputStream imageIn)
	{
		HttpURLConnection conn = null;
		InputStream responseIn = null;

		try {
			conn = (HttpURLConnection) new URL(UPLOAD_URL).openConnection();
			conn.setDoOutput(true);

			ImgurAuthorization.getInstance().addToHttpURLConnection(conn);

			OutputStream out = conn.getOutputStream();
			copy(imageIn, out);
			out.flush();
			out.close();

			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				responseIn = conn.getInputStream();
				return onInput(responseIn);
			}
			else {
				Log.i(TAG, "responseCode=" + conn.getResponseCode());
				responseIn = conn.getErrorStream();
				StringBuilder sb = new StringBuilder();
				Scanner scanner = new Scanner(responseIn);
				while (scanner.hasNext()) {
					sb.append(scanner.next());
				}
				Log.i(TAG, "error response: " + sb.toString());
				return null;
			}
		} catch (Exception ex) {
			Log.e(TAG, "Error during POST", ex);
			return null;
		} finally {
			try {
				responseIn.close();
			} catch (Exception ignore) {}
			try {
				conn.disconnect();
			} catch (Exception ignore) {}
			try {
				imageIn.close();
			} catch (Exception ignore) {}
		}
	}
}
