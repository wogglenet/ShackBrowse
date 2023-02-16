package net.swigglesoft.shackbrowse.imgur;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import net.swigglesoft.shackbrowse.APIConstants;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import androidx.browser.customtabs.CustomTabsIntent;

public class LoginActivity extends Activity {
	private static final Pattern accessTokenPattern = Pattern.compile("access_token=([^&]*)");
	private static final Pattern refreshTokenPattern = Pattern.compile("refresh_token=([^&]*)");
	private static final Pattern expiresInPattern = Pattern.compile("expires_in=(\\d+)");
	private static final Pattern usernamePattern = Pattern.compile("account_username=([^&]*)");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
		customTabsIntent.launchUrl(this, Uri.parse("https://api.imgur.com/oauth2/authorize?client_id=" + APIConstants.MY_IMGUR_CLIENT_ID + "&response_type=token"));
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		parseAuthUrl(intent.getDataString());
	}

	private void parseAuthUrl(String url) {
		// intercept the tokens
		// shackbrowse://auth#access_token=ACCESS_TOKEN&token_type=Bearer&expires_in=3600
		if (!url.startsWith(APIConstants.MY_IMGUR_REDIRECT_URL)) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(LoginActivity.this, "Imgur Login failed, URL returned is " + url, Toast.LENGTH_SHORT).show();
					finish();
				}
			});
			return;
		}

		Matcher m; String refreshToken = null; String accessToken = null; long expiresIn = 0L; String username = null;
		m = refreshTokenPattern.matcher(url);
		if (m.find()) {
			refreshToken = m.group(1);
		}

		m = accessTokenPattern.matcher(url);
		if (m.find()) {
			accessToken = m.group(1);
		}

		m = expiresInPattern.matcher(url);
		if (m.find()) {
			expiresIn = Long.valueOf(m.group(1));
		}

		m = usernamePattern.matcher(url);
		if (m.find()) {
			username = m.group(1);
		}

		if (refreshToken != null) {
			ImgurAuthorization.getInstance().saveRefreshToken(refreshToken, accessToken, expiresIn, username);
			final String usernamef = username;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(LoginActivity.this, "Logged in as " + usernamef, Toast.LENGTH_SHORT).show();
					finish();
				}
			});
			return;
		}
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(LoginActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
				finish();
			}
		});
	}
}
