package net.woggle.shackbrowse.imgur;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends Activity {

	private WebView mWebView;

	private static final Pattern accessTokenPattern = Pattern.compile("access_token=([^&]*)");
	private static final Pattern usernamePattern = Pattern.compile("account_username=([^&]*)");
	private static final Pattern refreshTokenPattern = Pattern.compile("refresh_token=([^&]*)");
	private static final Pattern expiresInPattern = Pattern.compile("expires_in=(\\d+)");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		FrameLayout root = new FrameLayout(this);

		mWebView = new WebView(this);
		root.addView(mWebView);
		setContentView(root);

		mWebView.clearCache(true);
		mWebView.clearHistory();
		clearCookies(this);
		setupWebView();

		mWebView.loadUrl("https://api.imgur.com/oauth2/authorize?client_id=" + ImgurConstants.MY_IMGUR_CLIENT_ID + "&response_type=token");
	}

	private void setupWebView() {
		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				// intercept the tokens
				// http://example.com#access_token=ACCESS_TOKEN&token_type=Bearer&expires_in=3600
				boolean tokensURL = false;
				if (url.startsWith(ImgurConstants.MY_IMGUR_REDIRECT_URL)) {
					tokensURL = true;
					Matcher m;

					m = refreshTokenPattern.matcher(url);
					m.find();
					String refreshToken = m.group(1);

					m = accessTokenPattern.matcher(url);
					m.find();
					String accessToken = m.group(1);

					m = expiresInPattern.matcher(url);
					m.find();
					long expiresIn = Long.valueOf(m.group(1));

					m = usernamePattern.matcher(url);
					m.find();
					String username = m.group(1);

					ImgurAuthorization.getInstance().saveRefreshToken(refreshToken, accessToken, expiresIn, username);

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(LoginActivity.this, "Logged in as " + username, Toast.LENGTH_SHORT).show();
							finish();
						}
					});
				}
				return tokensURL;
			}
		});
	}

	@SuppressWarnings("deprecation")
	public static void clearCookies(Context context)
	{

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
			CookieManager.getInstance().removeAllCookies(null);
			CookieManager.getInstance().flush();
		} else
		{
			CookieSyncManager cookieSyncMngr=CookieSyncManager.createInstance(context);
			cookieSyncMngr.startSync();
			CookieManager cookieManager=CookieManager.getInstance();
			cookieManager.removeAllCookie();
			cookieManager.removeSessionCookie();
			cookieSyncMngr.stopSync();
			cookieSyncMngr.sync();
		}
	}
}
