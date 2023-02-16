package net.swigglesoft.shackbrowse.imgur;

import net.swigglesoft.shackbrowse.APIConstants;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImgurAuthURLHandling {

    public static boolean isImgurAuthUrl(String url) {
        return url != null && url.startsWith(APIConstants.MY_IMGUR_REDIRECT_URL);
    }

    public static String parseAuthUrl(String url) {
        Pattern accessTokenPattern = Pattern.compile("access_token=([^&]*)");
        Pattern refreshTokenPattern = Pattern.compile("refresh_token=([^&]*)");
        Pattern expiresInPattern = Pattern.compile("expires_in=(\\d+)");
        Pattern usernamePattern = Pattern.compile("account_username=([^&]*)");

        // intercept the tokens
        // shackbrowse://auth#access_token=ACCESS_TOKEN&token_type=Bearer&expires_in=3600
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
            return "Logged in as " + usernamef;
        }
        return "Login Failed";
    }

    public static String generateAuthUrl() {
        return "https://api.imgur.com/oauth2/authorize?client_id=" + APIConstants.MY_IMGUR_CLIENT_ID + "&response_type=token";
    }
}
