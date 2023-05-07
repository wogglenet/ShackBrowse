package net.swigglesoft.shackbrowse;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.net.ssl.HttpsURLConnection;

import net.swigglesoft.ApiUrl;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseIntArray;

import com.google.android.gms.common.util.IOUtils;


public class ShackApi
{
	private static final int connectionTimeOutSec = 40;
	private static final int socketTimeoutSec = 35;
    static final String USER_AGENT = "shackbrowse/7.0";
    
    static final String IMAGE_LOGIN_URL = "http://chattypics.com/users.php?act=login_go";
    static final String IMAGE_UPLOAD_URL = "http://chattypics.com/upload.php";
    
    static final String LOGIN_URL = "https://www.shacknews.com/account/signin";
    static final String CHECKUSER_URL = "https://www.shacknews.com/account/username_exists";
    static final String MARKREAD_URL = "https://www.shacknews.com/messages/read";
    static final String MOD_URL = "https://www.shacknews.com/mod_chatty.x";
    static final String POST_URL = "https://www.shacknews.com/api/chat/create/17.json";
    static final String POST_URL_NEW = "https://www.shacknews.com/post_chatty.x";
    static final String MSG_POST_URL = "https://www.shacknews.com/messages/send";
    static final String LOL_URL = "http://www.lmnopc.com/greasemonkey/shacklol/report.php";
    static final String LOL_SN_URL = "https://www.shacknews.com/api2/api-index.php";

    static final String LOL_CACHE_FILE = "shacklol.cache";

    // {"status":"1","data":[{"tag_id":"1","tag":"lol","color":"#FF8800"},{"tag_id":"4","tag":"inf","color":"#0099CC"},{"tag_id":"3","tag":"unf","color":"#FF0000"},{"tag_id":"5","tag":"tag","color":"#77BB22"},{"tag_id":"2","tag":"wtf","color":"#C000C0"},{"tag_id":"6","tag":"wow","color":"#C4A3B3"},{"tag_id":"7","tag":"aww","color":"#13A4A7"}],"message":""}
    // the following array converts numbers to string text
    static final String[] SN_LOL_TAG_TYPE = { "zero", "lol" , "wtf", "unf", "inf", "tag", "wow", "aww" };

    static final String GET_LOL_URL = "http://lmnopc.com/greasemonkey/shacklol/api.php";
    
    static final String PUSHSERV_URL = "http://shackbrowsepublic.appspot.com/";
    static final String FASTPUSHSERV_URL = "http://shackbrowse.appspot.com/";
    
    static final String NOTESERV_URL = "https://shacknotify.bit-shift.com";
    static final String NOTESERV_URL_SSL = "https://woggle.net/shackbrowsenotification/";

    static final String BASE_URL = "http://shackapi.hughes.cc/";
    static final String BASE_URL_ALT = "http://woggle.net/shackbrowseAPI/";
    static final String BASE_URL_ALT_SSL = "https://woggle.net/shackbrowseAPI/";
    static final String WINCHATTYV2_API = "http://winchatty.com/v2/";
    static final String CLOUDPIN_URL = "http://woggle.net/shackcloudpin/";
    static final String DONATOR_URL = "http://woggle.net/shackbrowsedonators/";
    static final String FAKE_CORTEX_ID = "18";
    static final String FAKE_STORY_ID = "17";
    static final String FAKE_NEWS_ID = "2";
    
    static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
	static final String API_MESSAGES_URL = "https://www.shacknews.com/api/messages.json";

	public static Integer tryParseInt(String text) {
    	try {
    		return Integer.valueOf(text);
    	} catch (NumberFormatException e) {
    		return 0;
    	}
    }
    
    public static String modPost(String userName, String password, int rootPostId, int postId, String moderation) throws Exception
    {
    	BasicResponseHandler response_handler = new BasicResponseHandler();
    	DefaultHttpClient client = new DefaultHttpClient();
        final HttpParams httpParameters = client.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeOutSec * 1000);
        HttpConnectionParams.setSoTimeout        (httpParameters, socketTimeoutSec * 1000);
        HttpPost post = new HttpPost(LOGIN_URL);
        post.setHeader("User-Agent", USER_AGENT);
        post.setHeader("X-Requested-With", "XMLHttpRequest");
        
        List<NameValuePair> values = new ArrayList<NameValuePair>();
        values.add(new BasicNameValuePair("get_fields[]", "result"));
        values.add(new BasicNameValuePair("user-identifier", userName));
        values.add(new BasicNameValuePair("supplied-pass", password));
        values.add(new BasicNameValuePair("remember-login", "1"));
        
        UrlEncodedFormEntity e = new UrlEncodedFormEntity(values,"UTF-8");
        post.setEntity(e);
        
        String content = client.execute(post, response_handler);

        if (content.contains("{\"result\":{\"valid\":\"true\""))
        {
            int mod_type_id = getModTypeId(moderation);
            String mod = MOD_URL + "?root=" + rootPostId + "&post_id=" + postId + "&mod_type_id=" + mod_type_id;
            Log.d("shackbrowse", "Modding: " + mod);
            HttpGet get = new HttpGet(mod);
            get.setHeader("User-Agent", USER_AGENT);
            
            content = client.execute(get, response_handler);
            
            Log.d("shackbrowse", content);
            
            Pattern p = Pattern.compile("alert\\(\\s*\\\"(.+?)\\\"");
            Matcher match = p.matcher(content);
                            
            if (match.find())
                return match.group(1);
            
            return null;
        }
        
        return "Couldn't login";
    }

    public static ApiUrl getBaseUrl(Context context)
    {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	int type = tryParseInt(prefs.getString("apiUrl2", "4"));
    	if (type == 8)
    	{
    		return new ApiUrl(BASE_URL_ALT, false);
    	}
    	else if (type == 9)
    	{
            return new ApiUrl(BASE_URL, false);
    	}
        return new ApiUrl(WINCHATTYV2_API, true);
    }
    
    static int getModTypeId(String moderation) throws Exception
    {
        if (moderation.equalsIgnoreCase("interesting"))
            return 1;
        else if (moderation.equalsIgnoreCase("nws"))
            return 2;
        else if (moderation.equalsIgnoreCase("stupid"))
            return 3;
        else if (moderation.equalsIgnoreCase("tangent"))
            return 4;
        else if (moderation.equalsIgnoreCase("ontopic"))
            return 5;
        else if (moderation.equalsIgnoreCase("nuked"))
            return 8;
        else if (moderation.equalsIgnoreCase("political"))
            return 9;
        
        throw new Exception("Invalid mod type: " + moderation);
    }
    
    public static String loginToUploadImage(String userName, String password) throws Exception
    {
        BasicResponseHandler response_handler = new BasicResponseHandler();
        DefaultHttpClient client = new DefaultHttpClient();
        final HttpParams httpParameters = client.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeOutSec * 1000);
        HttpConnectionParams.setSoTimeout(httpParameters, socketTimeoutSec * 1000);
        HttpPost post = new HttpPost(IMAGE_LOGIN_URL);
        post.setHeader("User-Agent", USER_AGENT);
        
        List<NameValuePair> values = new ArrayList<NameValuePair>();
        values.add(new BasicNameValuePair("user_name", userName));
        values.add(new BasicNameValuePair("user_password", password));
        
        UrlEncodedFormEntity e = new UrlEncodedFormEntity(values,"UTF-8");
        post.setEntity(e);
        
        String content = client.execute(post, response_handler);
        if (content.contains("successfully been logged in"))
        {
            List<Cookie> cookies = client.getCookieStore().getCookies();
            return cookies.get(0).getName() + "=" + cookies.get(0).getValue();
        }
        
        return null;
    }


	public static String uploadImage(String imageLocation, String cookie) throws Exception
	{
		return uploadImage(imageLocation, cookie, "jpg");
	}
    public static String uploadImage(String imageLocation, String cookie, String extension) throws Exception
    {
        File file = new File(imageLocation);
        String name = file.getName();
        name = "shackbrowseUpload." + extension;
        FileEntity e = new FileEntity(file, "image");
        
        String BOUNDARY = "648f67b67d304b01f84ceb0e0c56c8b7";
        
        URL post_url = new URL(IMAGE_UPLOAD_URL);
        URLConnection con = post_url.openConnection();
        con.setRequestProperty("User-Agent", USER_AGENT);
        if (cookie != null)
            con.setRequestProperty("Cookie", cookie);
        con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
        
        // write our request
        con.setDoOutput(true);
        java.io.OutputStream os = con.getOutputStream();
        try
        {
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeBytes("--" + BOUNDARY + "\r\n");
            dos.writeBytes("Content-Disposition: form-data; name=\"userfile[]\";filename=\"" + name + "\"\r\n\r\n");

            e.writeTo(os);
            
            dos.writeBytes("\r\n--" + BOUNDARY + "--\r\n");
            os.flush();
        }
        finally
        {
            os.close();
        }
        
        // read the response
        java.io.InputStream input = con.getInputStream();
        try
        {
            return readStream(input);
        }
        finally
        {
            input.close();
        }
    }
	public static String uploadImageFromInputStream(InputStream inputfile, String cookie, String extension) throws Exception
	{
		String name = "shackbrowseUpload." + extension;

		String BOUNDARY = "648f67b67d304b01f84ceb0e0c56c8b7";

		URL post_url = new URL(IMAGE_UPLOAD_URL);
		URLConnection con = post_url.openConnection();
		con.setRequestProperty("User-Agent", USER_AGENT);
		if (cookie != null)
			con.setRequestProperty("Cookie", cookie);
		con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);

		// write our request
		con.setDoOutput(true);
		java.io.OutputStream os = con.getOutputStream();
		try
		{
			DataOutputStream dos = new DataOutputStream(os);
			dos.writeBytes("--" + BOUNDARY + "\r\n");
			dos.writeBytes("Content-Disposition: form-data; name=\"userfile[]\";filename=\"" + name + "\"\r\n\r\n");

			IOUtils.copyStream(inputfile,os);

			dos.writeBytes("\r\n--" + BOUNDARY + "--\r\n");
			os.flush();
		}
		finally
		{
			os.close();
		}

		// read the response
		java.io.InputStream input = con.getInputStream();
		try
		{
			return readStream(input);
		}
		finally
		{
			input.close();
		}
	}
    
    public static ArrayList<SearchResult> search(String term, String author, String parentAuthor, int pageNumber, Context context) throws Exception
    {
    	return search(term, author, parentAuthor, "", pageNumber, context);
    }
    public static ArrayList<SearchResult> search(String term, String author, String parentAuthor, String category, int pageNumber, Context context) throws Exception
    {
    	ArrayList<SearchResult> results = null;
    	if (getBaseUrl(context).isV2())
    	{
    		String url = getBaseUrl(context).getUrl() + "search?terms=" + URLEncoder.encode(term, "UTF8") + "&author=" + URLEncoder.encode(author, "UTF8") + "&category=" + URLEncoder.encode(category, "UTF8") + "&parentAuthor=" + URLEncoder.encode(parentAuthor, "UTF8") + "&offset=" + (pageNumber -1) * 35 + "&limit=35";
	        results = new ArrayList<SearchResult>();
	        JSONObject result = getJson(url);
	        JSONArray comments = result.getJSONArray("posts");
	        for (int i = 0; i < comments.length(); i++)
	        {
	        	JSONObject comment = comments.getJSONObject(i);
	        	int id = comment.getInt("id");
	        	String userName = comment.getString("author");
	            String body = comment.getString("body");
	            String posted = comment.getString("date");
	            
	            Long postedTime = convertTimeWinChatty(posted);
	            
	            results.add(new SearchResult(id, userName, body, postedTime, SearchResult.TYPE_SHACKSEARCHRESULT, 0));
	        }
    	}
    	else
    	{
	        String url = getBaseUrl(context).getUrl() + "search.php?terms=" + URLEncoder.encode(term, "UTF8") + "&author=" + URLEncoder.encode(author, "UTF8") + "&category=" + URLEncoder.encode(category, "UTF8") + "&parentAuthor=" + URLEncoder.encode(parentAuthor, "UTF8") + "&page=" + URLEncoder.encode(Integer.toString(pageNumber), "UTF-8");
	        results = new ArrayList<SearchResult>();
	        JSONObject result = getJson(url);
	        
	        JSONArray comments = result.getJSONArray("comments");
	        for (int i = 0; i < comments.length(); i++)
	        {
	            JSONObject comment = comments.getJSONObject(i);
	
	            int id = comment.getInt("id");
	            String userName = comment.getString("author");
	            String body = comment.getString("preview");
	            String posted = comment.getString("date");
	            
	            // convert time to local timezone
	            Long postedTime = convertTime(posted);
	            
	            results.add(new SearchResult(id, userName, body, postedTime, SearchResult.TYPE_SHACKSEARCHRESULT, 0));
	        }
    	}
        return results;
    }
    
    public static JSONObject postReply(Context context, int replyToThreadId, String content, String contentTypeID) throws Exception
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String userName = prefs.getString("userName", null).trim();
        String password = prefs.getString("password", null);
        
        List<NameValuePair> values = new ArrayList<NameValuePair>();
        values.add(new BasicNameValuePair("content_type_id", contentTypeID));
        values.add(new BasicNameValuePair("content_id", contentTypeID));
        values.add(new BasicNameValuePair("body", content));
        if (replyToThreadId > 0)
            values.add(new BasicNameValuePair("parent_id", Integer.toString(replyToThreadId)));
        
        System.out.println("SHACKAPI: SUBMITTING JSON FOR POST:" + content);
        JSONObject result = postJson(POST_URL, userName, password, values);
        
        if (!result.has("data"))
            throw new Exception("Missing response data.");
        
        JSONObject data = result.getJSONObject("data");
        return data;
    }
    
    private static JSONObject postJson(String url, String userName, String password, List<NameValuePair> values) throws Exception
    {
        UrlEncodedFormEntity e = new UrlEncodedFormEntity(values,"UTF-8");
        
        URL post_url = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) post_url.openConnection();
        con.setReadTimeout(40000);
        con.setRequestProperty("connection", "close");
        con.setConnectTimeout(20000);
        con.setChunkedStreamingMode(0);
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Authorization", "Basic " + android.util.Base64.encodeToString((userName + ":" + password).getBytes(), android.util.Base64.NO_WRAP));
        
        // write our request
        con.setDoOutput(true);
        java.io.OutputStream os = con.getOutputStream();
        try
        {
            e.writeTo(os);
            os.flush();
        }
        finally
        {
            os.close();
        }
        
        // read the response
        java.io.InputStream input = con.getInputStream();
        JSONObject result = null;
        try
        {
            String content = readStream(input);
            result = new JSONObject(content);
        }
        finally
        {
            input.close();
        }
        
		return result;
    }
	static String getShackMessageAPIText(String userName, String password) throws Exception
	{
		List<NameValuePair> values = new ArrayList<NameValuePair>(); values.add(new BasicNameValuePair("get", "yes"));
		UrlEncodedFormEntity e = new UrlEncodedFormEntity(values,"UTF-8");
		userName = userName.trim();

		URL post_url = new URL(API_MESSAGES_URL);
		HttpsURLConnection con = (HttpsURLConnection) post_url.openConnection();
		con.setReadTimeout(40000);
		con.setConnectTimeout(10000);
		con.setChunkedStreamingMode(0);
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Authorization", "Basic " + android.util.Base64.encodeToString((userName + ":" + password).getBytes(), android.util.Base64.NO_WRAP));

		// write our request
		con.setDoOutput(true);
		java.io.OutputStream os = con.getOutputStream();
		String result = "";
		try
		{
			e.writeTo(os);
			os.flush();
		}
		finally
		{
			os.close();
		}

		// read the response
        try {
            java.io.InputStream input = con.getInputStream();
            try {
                result = readStream(input);
            } finally {
                input.close();
            }
        }
        catch(Exception ex) {
            Log.w("getShackMessageAPIText", "An error happened during shackmessages retrieval: ", ex);
        }

		return result;
	}


    
    /*
     * THREAD RELATED
     */
    public static JSONObject getThreads(int pageNumber, String userName, Context context, boolean useWinChatty) throws ClientProtocolException, IOException, JSONException
    {
    	// v2
    	if (getBaseUrl(context).isV2())
    		return getJson(getBaseUrl(context).getUrl() + "getChattyRootPosts?limit=50&offset=" + (pageNumber -1) * 50 + "&username=" + URLEncoder.encode(userName, "UTF8"));
        // droid chatty api
    	return getJson(getBaseUrl(context).getUrl() + "page.php?page=" + Integer.toString(pageNumber) + "&user=" + URLEncoder.encode(userName, "UTF8"));
    }

	public static JSONObject getAllThreads(String userName, Context context) throws ClientProtocolException, IOException, JSONException
	{
		// requires v2
		if (getBaseUrl(context).isV2())
			return getJson(getBaseUrl(context).getUrl() + "getChattyRootPosts?limit=1000&offset=0&username=" + URLEncoder.encode(userName, "UTF8"));
		else
			return getJson(WINCHATTYV2_API + "getChattyRootPosts?limit=1000&offset=0&username=" + URLEncoder.encode(userName, "UTF8"));
	}
    
    public static JSONObject getThreads(int pageNumber, String userName, Context context) throws ClientProtocolException, IOException, JSONException
    {
        return getThreads(pageNumber, userName, context, false);
    }
    
    public static ArrayList<Thread> processThreads(JSONObject json, boolean filterNone, Context activity) throws ClientProtocolException, IOException, JSONException
    {
		return processThreads(json, filterNone, new ArrayList<Integer>(), activity);
    }
    public static ArrayList<Thread> processThreads(JSONObject json, boolean filterNone, ArrayList<Integer> collapsed, Context activity) throws ClientProtocolException, IOException, JSONException
    {
        ArrayList<Thread> threads = new ArrayList<Thread>();
        
        HashSet<String> visible_categories = new HashSet<String>();
        
        if (activity != null)
        {
    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
	        if (prefs.getBoolean("showInformative", true))
                visible_categories.add("informative");
            if (prefs.getBoolean("showTangent", true))
                visible_categories.add("offtopic");
            if (prefs.getBoolean("showStupid", true))
                visible_categories.add("stupid");
            if (prefs.getBoolean("showPolitical", false))
                visible_categories.add("political");
            if (prefs.getBoolean("showOntopic", true))
                visible_categories.add("ontopic");
            if (prefs.getBoolean("showNWS", false))
                visible_categories.add("nws");
            if (prefs.getBoolean("showCortex", true))
                visible_categories.add("cortex");
        }
        else
        {
            visible_categories.add("informative");
            visible_categories.add("offtopic");
            visible_categories.add("stupid");
            visible_categories.add("political");
            visible_categories.add("ontopic");
            visible_categories.add("cortex");
        }

        // winchatty uses "rootPosts" instead of "comments"
        boolean is_winchatty = json.has("rootPosts");

        // go through each of the comments and pull out the data that is used
        JSONArray comments = json.getJSONArray(is_winchatty ? "rootPosts" : "comments");
        for (int i = 0; i < comments.length(); i++)
        {
            JSONObject comment = comments.getJSONObject(i);

            String category = comment.getString("category");
            

            // winchatty v2 renames "offtopic" to "tangent" for some reason
            if (category.equalsIgnoreCase("tangent"))
            	category = "offtopic";
            
            int id = comment.getInt("id");
            
             if  ((filterNone || visible_categories.contains(category)) && (!collapsed.contains(id)))
            {
	            //    final boolean[] checkedItems = { _showTangent,_showInformative,_showNWS,_showStupid,_showPolitical};
	            String userName = comment.getString("author");
	            String body = comment.getString("body");
	            String date = comment.getString("date");
	    		// the -1 is because we do not count root posts and the api server does
	            int replyCount = comment.getInt(is_winchatty ? "postCount" : "reply_count");
	            
	            // this doesnt come from the actual api, but offlinethread supports this item
	            int replyCountPrevious = 0;
	            
	            boolean pinned = false;
                if (comment.has("reply_count_prev")) {
                	replyCountPrevious = comment.getInt("reply_count_prev");
                    pinned = comment.getBoolean("pinned");
                }
	            
                boolean replied = comment.getBoolean(is_winchatty ? "isParticipant" : "replied");
	
	            // convert time to local timezone
                Long dateTime = is_winchatty ? convertTimeWinChatty(date) : convertTime(date);
	            
	            Thread thread = new Thread(id, userName, body, dateTime, replyCount, category, replied, pinned);
	            thread.setReplyCountPrevious(replyCountPrevious);
	            
	            threads.add(thread);
            }
        }

        return threads;
    }
    
    /*
     * 
     * POST RELATED
     */

    public static JSONObject getRootPost(int threadId, Context context) throws ClientProtocolException, IOException, JSONException
    {
        if (threadId == 0)
        {
            // nothing to do
            return new JSONObject();
        }
        // v2
        if (getBaseUrl(context).isV2())
        {
        	try
	        {
	            //JSONObject json = getJson(BASE_URL + "thread/" + threadId + ".json");
	            return getJson(getBaseUrl(context).getUrl() + "getPost?id=" + threadId);
	        }
	        catch (Exception ex)
	        {
	            Log.e("shackbrowse", "Network Error getting post data", ex);
	        }
        }
        // droid chatty api
        else
        {
	        try
	        {
	            //JSONObject json = getJson(BASE_URL + "thread/" + threadId + ".json");
	            return getJson(getBaseUrl(context).getUrl() + "rootpost.php?id=" + threadId);
	        }
	        catch (Exception ex)
	        {
	            Log.e("shackbrowse", "Network Error getting post data", ex);
	        }
        }
        return new JSONObject();
    }
    
    // the second one is used in benchmarking and allows specification of the baseUrl manually
    public static JSONObject getPosts(int threadId, Context context) throws ClientProtocolException, IOException, JSONException
    {
    	return getPosts(threadId, context, getBaseUrl(context));
    }
    public static JSONObject getPosts(int threadId, Context context, ApiUrl choice) throws ClientProtocolException, IOException, JSONException
    {
    	if (threadId == 0)
        {
            // nothing to do
            return new JSONObject();
        }
    	
    	// v2
    	if (choice.isV2())
    		return getJson(choice.getUrl() + "getThread?id=" + threadId);
    	
    	// droid chatty api
    	return getJson(choice.getUrl() + "thread.php?id=" + threadId);
    }
    
    public static ArrayList<Post> processPosts(JSONObject json, int threadId, int _maxBullets, Context context) throws ClientProtocolException, IOException, JSONException
    {
    	ArrayList<Post> posts = new ArrayList<Post>();
        try
        {
            
            Hashtable<Integer, Post> post_map = new Hashtable<Integer, Post>();
            
            boolean is_winchatty = json.has("threads");
            boolean is_v2rootpost = json.has("posts");
            
            // go through each of the comments and pull out the data that is used
            JSONArray comments;
            int rootPostId;
            if (is_winchatty) {
            	if (!json.has("threads"))
            	{
            		System.out.println("LOADERR: " + json.toString());
            		NotificationObj errnote = new NotificationObj((int) Math.round((Math.random() * 100000d)), "reply", "errSAPI", "LOADERR: " + json.toString(), TimeDisplay.now(), null);
            		errnote.commit(context);
            	}
                JSONObject thread = json.getJSONArray("threads").getJSONObject(0);
                rootPostId = thread.getInt("threadId");
                comments = thread.getJSONArray("posts");
            }
            else if (is_v2rootpost) {
                JSONObject thread = json.getJSONArray("posts").getJSONObject(0);
                rootPostId = thread.getInt("threadId");
                comments = json.getJSONArray("posts");
            }
            else
            {
                comments = json.getJSONArray("replies");
                rootPostId = comments.getJSONObject(0).getInt("id");
            }

            // check for seen
            // last check this thread at epoch 0 secs
            int epochSecs = 0;
            boolean _showNewBranches = true;
            
            if (context != null)
            {
            	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                _showNewBranches = prefs.getBoolean("showNewBranches", true);
                
            	if ((((MainActivity)context)._seen.getTable().containsKey(rootPostId)) && (_showNewBranches))
            	{
            		// last checked it at *seendb* time
            		epochSecs = ((MainActivity)context)._seen.getTable().get(rootPostId);
            	}
            	else
            		epochSecs = (int) (TimeDisplay.now() / 1000);
            }
            
            // winchatty stuff
            HashMap<Integer, Integer> parents = is_winchatty ? new HashMap<Integer, Integer>() : null;
            
            for (int i = 0; i < comments.length(); i++)
            {
            	try
                {
            		JSONObject comment = comments.getJSONObject(i);

                    int postId = comment.getInt("id");
                    String userName = comment.getString("author");
                    String body = comment.getString("body");
                    String dateString = comment.getString("date");
                    String category = comment.getString("category");

                    // winchatty api doesn't provide depth, it provides parent/child relationships
                    // depth will be set after we have all the posts
                    int depth = (is_winchatty || is_v2rootpost) ? 0 : comment.getInt("depth");

                    // convert time to milliseconds since unix epoch
                    Long dateTime = (is_winchatty || is_v2rootpost) ? convertTimeWinChatty(dateString) : convertTime(dateString);

                    boolean seen = true;
                    // check if post is newer than last seen time
                    if ((int)(dateTime / 1000) > epochSecs)
                        seen = false;

                    Post post = new Post(postId, userName, body, dateTime, depth, category, false, seen, false);

                    posts.add(post);
                    post_map.put(postId, post);
                    
                    // keep track of parent posts for winchatty
                    if (is_winchatty)
                        parents.put(postId, comment.getInt("parentId"));
                }
                catch (Exception ex)
                {
                	ex.printStackTrace();
                    Log.e("shackbrowse", "Error parsing post: " + i, ex);
                }
            }
            if (is_winchatty)
            {
                // put the posts in the correct order, and set their depth level
                ArrayList<Post> ordered_posts = new ArrayList<Post>(posts.size());
                orderPosts(posts, ordered_posts, parents, 0, 0);
                posts = ordered_posts;
            }
            
            // mark as seen
            if ((context != null) && (_showNewBranches))
            {
	            ((MainActivity)context)._seen.getTable().put(rootPostId, (int)(TimeDisplay.now() / 1000));
	            ((MainActivity)context)._seen.store();
            }
           
            Vector<Integer> ids = new Vector<Integer>(post_map.keySet());
            Collections.sort(ids, Collections.reverseOrder());
            
            // set the order on the newest 10 posts, so they can be highlighted
            for (int i = 0; i < Math.min(posts.size(), 10); i++)
            {
                Post post = post_map.get(ids.get(i));
                post.setOrder(i);
            }
            
        }
        catch (Exception ex)
        {
        	ex.printStackTrace();
            Log.e("shackbrowse", "Error getting posts.", ex);
        }

        return posts;
    }
    
    public static int tagPost(int postId, String tag, String userName, boolean untag) throws Exception
    {
        // damn thomw to hell for abandoning his api
    	BasicResponseHandler response_handler = new BasicResponseHandler();
        DefaultHttpClient client = new DefaultHttpClient();
        final HttpParams httpParameters = client.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeOutSec * 1000);
        HttpConnectionParams.setSoTimeout(httpParameters, socketTimeoutSec * 1000);
        HttpPost post = new HttpPost(LOL_SN_URL);
        post.setHeader("User-Agent", USER_AGENT);
        
        List<NameValuePair> values = new ArrayList<NameValuePair>();
        values.add(new BasicNameValuePair("action2", "ext_create_tag_via_api"));
        values.add(new BasicNameValuePair("user", userName));
        values.add(new BasicNameValuePair("id", Integer.toString(postId)));
        values.add(new BasicNameValuePair("tag", tag));
        values.add(new BasicNameValuePair("untag", Integer.toString(untag ? 1 : 0)));
        values.add(new BasicNameValuePair("secret", APIConstants.SHACKNEWS_LOL_API_KEY));
        
        UrlEncodedFormEntity e = new UrlEncodedFormEntity(values,"UTF-8");
        post.setEntity(e);
        
        String content = client.execute(post, response_handler);
        System.out.println("TAGGED POST" + postId + " RESPONSE: " + content);
    	
        // see if it did work
        if (!content.contains("status\":\"1\"")) {
            throw new Exception(content);
        }
        else
        {
            return 1;
        }
    }
    
    private static void orderPosts(ArrayList<Post> posts, ArrayList<Post> ordered_posts, HashMap<Integer,Integer> parents, int current_parent, int level) {
        // There has got to be a better way to do this
        int parent_index = -1;
        ArrayList<Post> children = new ArrayList<Post>();

        // find the current parent
        for (int i = 0; i < ordered_posts.size(); i++)
        {
            Post item = ordered_posts.get(i);

            if (item.getPostId() == current_parent)
                parent_index = i;
        }

        // find all the children for the parent
        for (int i = 0; i < posts.size(); i++)
        {
            Post item = posts.get(i);
            if (parents.get(item.getPostId()) == current_parent)
            {
                item.setLevel(level);
                children.add(item);
            }
        }

        // sort children by post id
        Collections.sort(children);

        // add children to our list
        for (int i = 1; i <= children.size(); i++)
            ordered_posts.add(parent_index + i, children.get(i - 1));

        // add the children of our children
        for (Post p : children)
            orderPosts(posts, ordered_posts, parents, p.getPostId(), level + 1);

    }

    /*
     * JSON STUFF
     */
    private static String get(String location) throws ClientProtocolException, IOException 
    {
    	return get (location, false);
    }
    private static String get(String location, boolean laxTimeout) throws ClientProtocolException, IOException 
    {
        Log.d("shackbrowse", "Requested: " + location);
        
        URL url = new URL(location);
        
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        

        if  (laxTimeout)
    	{
    		connection.setConnectTimeout(15000);
        	connection.setReadTimeout(40000);
    	}
    	else
        {
        	connection.setConnectTimeout(socketTimeoutSec * 1000);
        	connection.setReadTimeout(connectionTimeOutSec * 1000);
    	}
        try
        {
            connection.setRequestProperty("User-Agent", USER_AGENT);
            InputStream in = new BufferedInputStream(connection.getInputStream());
            return readStream(in);
        }
    	catch (java.net.SocketTimeoutException e) {
    		System.out.println("conn timeout");
    	   return "";
    	}
        finally
        {
            connection.disconnect();
        }
    }

    private static boolean post(String location, List<NameValuePair> values) throws ClientProtocolException, IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        final HttpParams httpParameters = client.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeOutSec * 1000);
        HttpConnectionParams.setSoTimeout        (httpParameters, socketTimeoutSec * 1000);
        HttpPost post = new HttpPost(location);
        post.setHeader("User-Agent", USER_AGENT);
        Log.d("shackbrowse", "Posting to: " + location + ", values " + values.toString());

        UrlEncodedFormEntity e = new UrlEncodedFormEntity(values,"UTF-8");
        post.setEntity(e);

        HttpResponse response = client.execute(post);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= 200 && statusCode <= 299)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private static String getSSL(String location) throws ClientProtocolException, IOException
    {
        return getSSL (location, false);
    }
    private static String getSSL(String location, boolean laxTimeout) throws ClientProtocolException, IOException
    {
        Log.d("shackbrowse", "Requested: " + location);

        URL url = new URL(location);

        HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
        if (laxTimeout)
        {
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
        }
        else
        {
            connection.setConnectTimeout(socketTimeoutSec * 1000);
            connection.setReadTimeout(connectionTimeOutSec * 1000);
        }
        try
        {
            connection.setRequestProperty("User-Agent", USER_AGENT);
            InputStream in = new BufferedInputStream(connection.getInputStream());
            return readStream(in);
        }
        catch (java.net.SocketTimeoutException e) {
            System.out.println("conn timeout");
            return "";
        }
        finally
        {
            connection.disconnect();
        }
    }


    private static JSONObject getJson(String url) throws ClientProtocolException, IOException, JSONException
    {
        String content = get(url);
        return new JSONObject(content);
    }

    private static String readStream(java.io.InputStream stream) throws IOException
    {
        StringBuilder output = new StringBuilder();
        InputStreamReader input = new InputStreamReader(stream);

        char[] buffer = new char[DEFAULT_BUFFER_SIZE];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.append(buffer, 0, n);
        }
        input.close();
        return output.toString();
    }
    
    /*
     * Time stuff
     */
    static final SimpleDateFormat _winChattyDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    static final SimpleDateFormat _thomWDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss Z", Locale.US);
    static final SimpleDateFormat _shackDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mmaa zzz", Locale.US);
    static final SimpleDateFormat _shackDateFormatMsg = new SimpleDateFormat("MMM dd, yyyy, hh:mm aa zzz", Locale.US);
    static final DateFormat _displayDateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    
    static Long convertTime(String original)
    {
        try
        {
            Date dt = _shackDateFormat.parse(original.replace("PDT", "-0700"));
            return dt.getTime();
        }
        catch (Exception ex)
        {
            Log.e("shackbrowse", "Error parsing date", ex);
        }
        
        return 0L;
    }
    static Long convertTimeWinChatty(String original)
    {
        _winChattyDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date dt = _winChattyDateFormat.parse(original);
            return dt.getTime();
        }
        catch (Exception ex)
        {
            Log.e("shackbrowse", "Error parsing date", ex);
        }

        return 0L;
    }
    static Long convertTimeThom(String original)
    {
        try
        {
            Date dt = _thomWDateFormat.parse(original);
            return dt.getTime();
        }
        catch (Exception ex)
        {
            Log.e("shackbrowse", "Error parsing date", ex);
        }
        
        return 0L;
    }
    static Long convertTimeMsg(String original)
    {
        try
        {
            Date dt = _shackDateFormatMsg.parse(original + " UTC");
            // fix weird shacknews time pproblem with shackmessages
            dt.setTime(dt.getTime() + (1000 * 60 * 60 * 14));
            return dt.getTime();
        }
        catch (Exception ex)
        {
            Log.e("shackbrowse", "Error parsing date", ex);
        }
        
        return 0L;
    }

    /*
     * SHACK LOL stuff
     */
    public static HashMap<String, HashMap<String, LolObj>> getLols (Context activity) throws ClientProtocolException, IOException, JSONException
    {
    	boolean _getLols = (true && MainActivity.LOLENABLED);
    	if (activity != null)
        {
    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            _getLols = (prefs.getBoolean("getLols", true) && MainActivity.LOLENABLED);
        }
        else return new HashMap<String, HashMap<String,LolObj>>();
        
        if (!_getLols)
        {
        	return new HashMap<String, HashMap<String,LolObj>>();
        }
        
        JSONObject json = new JSONObject();
        boolean mustRefresh = true;
        
        if ((activity != null) && (activity.getFileStreamPath(LOL_CACHE_FILE).exists()))
        {
            // look at that, we got a file
            try {
                FileInputStream input = activity.openFileInput(LOL_CACHE_FILE);
                try
                {
                    DataInputStream in = new DataInputStream(input);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line = reader.readLine();
                    boolean firstLine = true;
                    
                    while (line != null)
                    {
                        if (line.length() > 0)
                        {
                        	if (firstLine)
                        	{
                        		Long oldTime = Long.parseLong(line);
                        		Long newTime = System.currentTimeMillis();
                        		// read date in milliseconds
                        		
                        		// four minute wait to refresh
                        		if ((newTime - (60000 * 4)) < oldTime)
                        		{
                        			mustRefresh = false;
                        		}
                        	}
                        	else
                        	{
                        		// grab cache data
                        		json = new JSONObject(line);
                        	}
                        	firstLine = false;
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
        
        JSONObject newJson = new JSONObject();
        
        if (mustRefresh)
        {
        	System.out.println("loading new shacklol data");
        	boolean lolTimedOut = false;
        	// refresh
        	try {
        		newJson = getJson(LOL_SN_URL + "?action2=ext_get_counts");
        	}
        	catch (Exception e)
        	{
        		System.out.println("connection timeout for LOLDATA");
        		lolTimedOut = true;
        		final Context bcont = activity.getApplicationContext();
		        /*
        		activity.runOnUiThread(new Runnable(){
             		@Override public void run()
             		{
             			
             			Toast.makeText(bcont, "ShackLOL server timed out\nConsider disabling ShackLOL API in Preferences", Toast.LENGTH_SHORT).show();
             		}
             	});*/
        		
        		// cache it
    	        FileOutputStream output = activity.openFileOutput(LOL_CACHE_FILE, Activity.MODE_PRIVATE);
    	        try
    	        {
    	            DataOutputStream out = new DataOutputStream(output);
    	            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
    	            
    	            // write timestamp, should cause us to wait another 4 minutes
    	            writer.write(Long.toString(System.currentTimeMillis()));
    	            writer.newLine();
    	            // cache json
    	            writer.write(json.toString());
    	            writer.flush();
    	        }
    	        finally
    	        {
    	            output.close();
    	        }

    	        System.out.println("SHACKLOLDATA2: " + json.toString());
        		return processLols(json, activity);
        	}
        	
        	// cache it
	        FileOutputStream output = activity.openFileOutput(LOL_CACHE_FILE, Activity.MODE_PRIVATE);
	        try
	        {
	            DataOutputStream out = new DataOutputStream(output);
	            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
	            
	            // write timestamp
	            writer.write(Long.toString(System.currentTimeMillis()));
	            writer.newLine();
	            // cache json
	            writer.write(newJson.toString());
	            writer.flush();
	        }
	        finally
	        {
	            output.close();
	        }
        }
        else
        {
        	System.out.println("loading cached shacklol data");
        	if (json.length() > 0) newJson = json;
        }

        System.out.println("SHACKLOLDATA: " + newJson.toString());
        return processLols(newJson, activity);
    }
    
    private static HashMap<String, HashMap<String, LolObj>> processLols(JSONObject json, Context context) throws ClientProtocolException, IOException, JSONException
    {
    	HashMap<String, HashMap<String, LolObj>> map = new HashMap<String, HashMap<String,LolObj>>();
        JSONArray threads = json.names();
        
        
        int lolCount = 0;
        int tagCount = 0;
        int infCount = 0;
        int wtfCount = 0;
        int unfCount = 0;
        int wowCount = 0;
        int awwCount = 0;
        LolObj lolobj = new LolObj();
        if (threads != null)
        {
	        for (int i = 0; i <  threads.length(); i++)
	        {
	        	JSONObject thisThread = json.getJSONObject(threads.getString(i));
	        	JSONArray posts = thisThread.names();
	        	
	        	
	        	
	        	map.put(threads.getString(i), new HashMap<String, LolObj>());
	        	
	        	lolCount = 0;
	        	tagCount = 0;
	        	infCount = 0;
	        	wtfCount = 0;
	        	unfCount = 0;
	        	wowCount = 0;
	        	awwCount = 0;
	        	
	        	for (int j = 0; j <  posts.length(); j++)
	            {
	        		JSONObject thisPost = thisThread.getJSONObject(posts.getString(j));
	        		lolobj = new LolObj();
	        		if (thisPost.has("lol")) { lolobj.setLol(tryParseInt(thisPost.getString("lol"))); lolCount = lolCount + tryParseInt(thisPost.getString("lol")); }
	        		if (thisPost.has("inf")) { lolobj.setInf(tryParseInt(thisPost.getString("inf"))); infCount = infCount + tryParseInt(thisPost.getString("inf")); }
	        		if (thisPost.has("unf")) { lolobj.setUnf(tryParseInt(thisPost.getString("unf"))); unfCount = unfCount + tryParseInt(thisPost.getString("unf")); }
	        		if (thisPost.has("tag")) { lolobj.setTag(tryParseInt(thisPost.getString("tag"))); tagCount = tagCount + tryParseInt(thisPost.getString("tag")); }
	        		if (thisPost.has("wtf")) { lolobj.setWtf(tryParseInt(thisPost.getString("wtf"))); wtfCount = wtfCount + tryParseInt(thisPost.getString("wtf")); }
                    if (thisPost.has("wow")) { lolobj.setWow(tryParseInt(thisPost.getString("wow"))); wowCount = wowCount + tryParseInt(thisPost.getString("wow")); }
                    if (thisPost.has("aww")) { lolobj.setAww(tryParseInt(thisPost.getString("aww"))); awwCount = awwCount + tryParseInt(thisPost.getString("aww")); }
	        		lolobj.genTagSpan(context);
	        		map.get(threads.getString(i)).put(posts.getString(j), lolobj);
	            }
	        	
	        	lolobj = new LolObj();
	        	lolobj.setLol(lolCount);
	        	lolobj.setTag(tagCount);
	        	lolobj.setInf(infCount);
	        	lolobj.setWtf(wtfCount);
	        	lolobj.setUnf(unfCount);
	        	lolobj.setWow(wowCount);
                lolobj.setAww(awwCount);
	        	lolobj.genTagSpan(context);
	        	map.get(threads.getString(i)).put("totalLols", lolobj);
	        	
	        }
	        return map;
        }
        else
        	return new HashMap<String, HashMap<String,LolObj>>();
        
    }
    
    // saved threads
    
    public static SparseIntArray getReplyCounts(ArrayList<Integer> threadIDs, Context context) throws Exception
    {
	    // getReplyCounts dies if the list is too long, have to break into multiple parts

	    if (threadIDs.size() == 0)
		    return new SparseIntArray(threadIDs.size());

		int partitionSize = 40;
		SparseIntArray reply_counts = new SparseIntArray(threadIDs.size());

		for (int i = 0; i < threadIDs.size(); i += partitionSize)
		{
			ArrayList<Integer> sublist = new ArrayList<Integer>();
			sublist.addAll(threadIDs.subList(i, Math.min(i + partitionSize, threadIDs.size())));
			SparseIntArray reply_counts_part = ShackApi.getReplyCountsPart(sublist, context);
			for (int j = 0; j < reply_counts_part.size(); j++)
			{
				reply_counts.put(reply_counts_part.keyAt(j), reply_counts_part.valueAt(j));
			}
		}

        return reply_counts;
    }
	public static SparseIntArray getReplyCountsPart(ArrayList<Integer> threadIDs, Context context) throws Exception
	{
		SparseIntArray results = new SparseIntArray(threadIDs.size());

		if (threadIDs.size() == 0)
			return results;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		// v2
		if (getBaseUrl(context).isV2())
		{
			String commaSepList = "";
			for (Integer threadID : threadIDs)
			{
				if (commaSepList.equals(""))
					commaSepList = Integer.toString(threadID);
				else
					commaSepList = commaSepList + "," + Integer.toString(threadID);
			}
			String url = getBaseUrl(context).getUrl() + "getThreadPostCount?id=" + commaSepList;
			JSONArray response = getJson(url).getJSONArray("threads");
			for (int i = 0; i < response.length(); i++)
			{
				results.put(((JSONObject)response.get(i)).getInt("threadId"), ((JSONObject)response.get(i)).getInt("postCount"));
			}
		}
		// droid chatty api
		else
		{
			for (Integer threadID : threadIDs)
			{
				String url = getBaseUrl(context).getUrl() + "replies.php?id=" + Integer.toString(threadID);
				String replyCount = get(url);
				// the -1 is because we do not count root posts and the api server does
				results.put(threadID, (tryParseInt(replyCount)));
			}
		}
		return results;
	}
    
    public static ArrayList<Thread> processThreadsAndUpdReplyCounts (JSONObject json, Context activity) throws Exception
    {
        System.out.println("OFFLINETHREAD:  process1");
    	ArrayList<Thread> new_threads = processThreads(json, true, activity);
        System.out.println("OFFLINETHREAD:  process2");
    	ArrayList<Thread> upd_threads = new ArrayList<Thread>();
    	
    	ArrayList<Integer> new_thread_ids = new ArrayList<Integer>();
    	for (Thread thisT : new_threads)
    	{
    		new_thread_ids.add(thisT.getThreadId());
    	}
    	SparseIntArray new_counts = getReplyCounts(new_thread_ids, activity);
    	
    	for (Thread thisT : new_threads)
    	{    		
    		// set current to info from web
			thisT.setReplyCount(new_counts.get(thisT.getThreadId()));
			
    		upd_threads.add(thisT);
    	}
        
		return upd_threads;
    	
    }
    // shackLOL get taggers
    public static ArrayList<String> getLOLTaggers(int postId, String type) throws JSONException, ClientProtocolException, IOException, Exception
    {

        // damn thomw to hell for abandoning his api
        BasicResponseHandler response_handler = new BasicResponseHandler();
        DefaultHttpClient client = new DefaultHttpClient();
        final HttpParams httpParameters = client.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeOutSec * 1000);
        HttpConnectionParams.setSoTimeout(httpParameters, socketTimeoutSec * 1000);
        HttpPost post = new HttpPost(LOL_SN_URL);
        post.setHeader("User-Agent", USER_AGENT);

        List<NameValuePair> values = new ArrayList<NameValuePair>();
        values.add(new BasicNameValuePair("action2", "ext_get_all_raters"));
        values.add(new BasicNameValuePair("ids[]", Integer.toString(postId)));
        values.add(new BasicNameValuePair("tag", type));

        UrlEncodedFormEntity e = new UrlEncodedFormEntity(values,"UTF-8");
        post.setEntity(e);

        String content = client.execute(post, response_handler);
        System.out.println("GET TAGGERS " + postId + " RESPONSE: " + content);

        // see if it did work
        if (!content.contains("status\":\"1\"")) {
            throw new Exception(content);
        }
        else {
            ArrayList<String> results = new ArrayList<String>();
            if (!content.contains("data\":null")) {
                JSONArray result = new JSONObject(content).getJSONArray("data").getJSONObject(0).getJSONArray("usernames");
                for (int i = 0; i < result.length(); i++) {
                    results.add(result.getString(i));
                }
            }
            return results;
        }
    }
    // SHACKLOL SEARCH
    public static ArrayList<SearchResult> searchLOL(String tag, int days, String author, String tagger, int pageNumber) throws Exception
    {
    	
    	Date date = new Date();
    	Calendar cal = Calendar.getInstance();
    	cal.setTime(date);
    	cal.add(Calendar.DAY_OF_YEAR, - days);
    	date = cal.getTime();
    	
    	if (tag.equalsIgnoreCase("all"))
    		tag = "";
    	else tag = "&tag=" + URLEncoder.encode(tag, "UTF8");
    	
    	if (author.length() > 0)
    		author = "&author=" + URLEncoder.encode(author, "UTF8");
    	
    	if (tagger.length() > 0)
    		tagger = "&tagger=" + URLEncoder.encode(tagger, "UTF8");
    	
    	String order= "";
		if (tagger.length() > 0 || author.length() > 0)
    		order = "&order=date_desc";
    		
    	SimpleDateFormat sdf = new SimpleDateFormat("M/d/yy hh:mm Z");
    	String queryDate = sdf.format(date);

        String url = GET_LOL_URL + "?format=json" + tag + author + tagger + order + "&page=" + pageNumber + "&limit=50&since=" + URLEncoder.encode(queryDate, "UTF8");
        System.out.println(url);
        ArrayList<SearchResult> results = new ArrayList<SearchResult>();
        JSONArray result = new JSONArray(get(url));
        
        for (int i = 0; i < result.length(); i++)
        {
            JSONObject comment = result.getJSONObject(i);

            int id = comment.getInt("id");
            int tag_count = comment.getInt("tag_count");
            String userName = comment.getString("author");
            String body = comment.getString("body");
            String posted = comment.getString("date");
            
            int type = SearchResult.TYPE_SHACKSEARCHRESULT;
            if (comment.getString("tag").equalsIgnoreCase("lol"))
            	type = SearchResult.TYPE_LOL;
            if (comment.getString("tag").equalsIgnoreCase("tag"))
            	type = SearchResult.TYPE_TAG;
            if (comment.getString("tag").equalsIgnoreCase("inf"))
            	type = SearchResult.TYPE_INF;
            if (comment.getString("tag").equalsIgnoreCase("unf"))
            	type = SearchResult.TYPE_UNF;
            if (comment.getString("tag").equalsIgnoreCase("wtf"))
            	type = SearchResult.TYPE_WTF;
            if (comment.getString("tag").equalsIgnoreCase("wow"))
            	type = SearchResult.TYPE_WOW;
            if (comment.getString("tag").equalsIgnoreCase("aww"))
                type = SearchResult.TYPE_AWW;
            	
            
            // convert time to local timezone
            Long postedTime = convertTimeThom(posted);
            
            results.add(new SearchResult(id, userName, body, postedTime, type, tag_count));
        }
        
        return results;
    }
    
    /* MESSAGES
     * 
     * all messages stuff
     * 
     * 
    */
	public static Boolean usernameExists(String username, Context context) throws Exception {
	    	BasicResponseHandler response_handler = new BasicResponseHandler();
	        DefaultHttpClient client = new DefaultHttpClient();
	        final HttpParams httpParameters = client.getParams();
	        HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeOutSec * 1000);
	        HttpConnectionParams.setSoTimeout        (httpParameters, socketTimeoutSec * 1000);
	        HttpPost post = new HttpPost(CHECKUSER_URL);
	        post.setHeader("User-Agent", USER_AGENT);
	        post.setHeader("X-Requested-With", "XMLHttpRequest");
	        
	        List<NameValuePair> values = new ArrayList<NameValuePair>();
	        values.add(new BasicNameValuePair("get_fields[]", "result"));
	        values.add(new BasicNameValuePair("username", username));
	        
	        UrlEncodedFormEntity e = new UrlEncodedFormEntity(values,"UTF-8");
	        post.setEntity(e);
	        
	        String content = client.execute(post, response_handler);
	        if (content.contains("\"result\":\"true\""))
	        {
	            System.out.println("USER EXISTS: " + username);
	            return true;
	        }
	        else {
                System.out.println("USER !NOT! EXISTS: " + username);
	            return false;
            }
	}

    public static List<Cookie> getLoginCookie(Context context) throws Exception {

	    // expect this should only be used with verified credentials
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String userName = prefs.getString("userName", null);
        String password = prefs.getString("password", null);

        BasicResponseHandler response_handler = new BasicResponseHandler();
        DefaultHttpClient client = new DefaultHttpClient();
        final HttpParams httpParameters = client.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeOutSec * 1000);
        HttpConnectionParams.setSoTimeout        (httpParameters, socketTimeoutSec * 1000);
        HttpPost post = new HttpPost(LOGIN_URL);
        post.setHeader("User-Agent", USER_AGENT);
        post.setHeader("X-Requested-With", "XMLHttpRequest");

        List<NameValuePair> values = new ArrayList<NameValuePair>();
        values.add(new BasicNameValuePair("get_fields[]", "result"));
        values.add(new BasicNameValuePair("user-identifier", userName));
        values.add(new BasicNameValuePair("supplied-pass", password));
        values.add(new BasicNameValuePair("remember-login", "1"));

        UrlEncodedFormEntity e = new UrlEncodedFormEntity(values,"UTF-8");
        post.setEntity(e);

        String content = client.execute(post, response_handler);

        if (content.contains("{\"result\":{\"valid\":\"true\""))
        {

            return client.getCookieStore().getCookies();
        }
        else
        {
            return null;
        }
    }

    public static boolean markRead(String messageId, Context context) throws Exception {
    	
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String userName = prefs.getString("userName", null);
        String password = prefs.getString("password", null);
    	
    	BasicResponseHandler response_handler = new BasicResponseHandler();
    	DefaultHttpClient client = new DefaultHttpClient();
        final HttpParams httpParameters = client.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeOutSec * 1000);
        HttpConnectionParams.setSoTimeout        (httpParameters, socketTimeoutSec * 1000);
        HttpPost post = new HttpPost(LOGIN_URL);
        post.setHeader("User-Agent", USER_AGENT);
        post.setHeader("X-Requested-With", "XMLHttpRequest");
        
        List<NameValuePair> values = new ArrayList<NameValuePair>();
        values.add(new BasicNameValuePair("get_fields[]", "result"));
        values.add(new BasicNameValuePair("user-identifier", userName));
        values.add(new BasicNameValuePair("supplied-pass", password));
        values.add(new BasicNameValuePair("remember-login", "1"));
        
        UrlEncodedFormEntity e = new UrlEncodedFormEntity(values,"UTF-8");
        post.setEntity(e);
        
        String content = client.execute(post, response_handler);
        
        if (content.contains("{\"result\":{\"valid\":\"true\""))
        {
            
        	post = new HttpPost(MARKREAD_URL);
            post.setHeader("User-Agent", USER_AGENT);
            
            values = new ArrayList<NameValuePair>();
            values.add(new BasicNameValuePair("mid", messageId));
            
            e = new UrlEncodedFormEntity(values, "UTF-8");
            post.setEntity(e);
            	
            content = client.execute(post, response_handler);
            
            return true;
        }
        else
        {
        	return false;
        }        
    }	
	public static ArrayList<Message> getMessages(int pageNumber, Context context) throws Exception { return getMessages(pageNumber,context,true); }
    public static ArrayList<Message> getMessages(int pageNumber, Context context, Boolean inbox) throws Exception {
    	
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String userName = prefs.getString("userName", null);
        String password = prefs.getString("password", null);
    	
    	BasicResponseHandler response_handler = new BasicResponseHandler();
        DefaultHttpClient client = new DefaultHttpClient();
        final HttpParams httpParameters = client.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeOutSec * 1000);
        HttpConnectionParams.setSoTimeout        (httpParameters, socketTimeoutSec * 1000);
        HttpPost post = new HttpPost(LOGIN_URL);
        post.setHeader("User-Agent", USER_AGENT);
        post.setHeader("X-Requested-With", "XMLHttpRequest");

        List<NameValuePair> values = new ArrayList<NameValuePair>();
        values.add(new BasicNameValuePair("get_fields[]", "result"));
        values.add(new BasicNameValuePair("user-identifier", userName));
        values.add(new BasicNameValuePair("supplied-pass", password));
        values.add(new BasicNameValuePair("remember-login", "1"));
        
        UrlEncodedFormEntity e = new UrlEncodedFormEntity(values,"UTF-8");
        post.setEntity(e);
        
        String content = client.execute(post, response_handler);
        if (content.contains("{\"result\":{\"valid\":\"true\""))
        {
            String getType = "inbox";
        	if (!inbox)
        		getType = "sent";

        	HttpGet get = new HttpGet("https://www.shacknews.com/messages/"+getType+"?page=" + pageNumber);
            get.setHeader("User-Agent", USER_AGENT);
            
            content = client.execute(get, response_handler);
            
            ArrayList<Message> msg_items = new ArrayList<Message>();
            // process
            String[] msgs = content.split("<li class=\"message");
            for (int i = 1; i < msgs.length; i++)
            {
            	boolean read = false;
            	if (msgs[i].substring(0,10).contains("read"))
            	{
            		read = true;
            	}
            	String startStr = "<a class=\"username\" href=\"#\">";
            	int start = msgs[i].indexOf(startStr);
            	int end = msgs[i].indexOf("</a>",start + startStr.length());
            	String user = msgs[i].substring(start + startStr.length(), end);
            	
            	startStr = "<input type=\"checkbox\" class=\"mid\" name=\"messages[]\" value=\"";
            	start = msgs[i].indexOf(startStr);
            	end = msgs[i].indexOf("\">",start + startStr.length());
            	String id = msgs[i].substring(start + startStr.length(), end);
            	int iid = tryParseInt(id);
            	
            	startStr = "<div class=\"subject-column toggle-message\"><a href=\"#\">";
            	start = msgs[i].indexOf(startStr);
            	end = msgs[i].indexOf("</a>",start + startStr.length());
            	String subject = msgs[i].substring(start + startStr.length(), end);

            	startStr = "<div class=\"date-column toggle-message\"><a href=\"#\">";
            	start = msgs[i].indexOf(startStr);
            	end = msgs[i].indexOf("</a>",start + startStr.length());
            	String date = msgs[i].substring(start + startStr.length(), end);

            	startStr = "<div class=\"message-body\">";
            	start = msgs[i].indexOf(startStr);
            	end = msgs[i].indexOf("</div>",start + startStr.length());
            	String mContent = urlify("<span class=\"jt_sample\"><b>Subject</b>: " + subject + "<br/>" + "<b>Date</b>: " + date + "</span><br/><br/>" + msgs[i].substring(start + startStr.length(), end));


            	// convert time to local timezone
                Long postedTime = convertTimeMsg(date);

	            if (!inbox)
	                msg_items.add(new Message(iid, "To: " + user, subject, mContent, msgs[i].substring(start + startStr.length(), end), postedTime, read));
	            else
            	    msg_items.add(new Message(iid, user, subject, mContent, msgs[i].substring(start + startStr.length(), end), postedTime, read));
            	
            }
            return msg_items;
        }
        else
        {
        	ArrayList<Message> msg_items = new ArrayList<Message>();
        	msg_items.add(new Message(1, "Login Failure", "You must set your login correctly to check messages", "Either you have not yet set your login credentials up, or you have set them incorrectly. Login with the current settings failed.", "ERROR", 0l, false));
        	return msg_items;
        }        
    }
    
    public static boolean postMessage(Context context, String subject, String recipient, String msg) throws Exception {
    	
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String userName = prefs.getString("userName", null);
        String password = prefs.getString("password", null);
    	
    	BasicResponseHandler response_handler = new BasicResponseHandler();
    	DefaultHttpClient client = new DefaultHttpClient();
        final HttpParams httpParameters = client.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeOutSec * 1000);
        HttpConnectionParams.setSoTimeout        (httpParameters, socketTimeoutSec * 1000);
        HttpPost post = new HttpPost(LOGIN_URL);
        post.setHeader("User-Agent", USER_AGENT);
        post.setHeader("X-Requested-With", "XMLHttpRequest");
        
        List<NameValuePair> values = new ArrayList<NameValuePair>();
        values.add(new BasicNameValuePair("get_fields[]", "result"));
        values.add(new BasicNameValuePair("user-identifier", userName));
        values.add(new BasicNameValuePair("supplied-pass", password));
        values.add(new BasicNameValuePair("remember-login", "1"));
        
        UrlEncodedFormEntity e = new UrlEncodedFormEntity(values,"UTF-8");
        post.setEntity(e);
        
        String content = client.execute(post, response_handler);
        
        if (content.contains("{\"result\":{\"valid\":\"true\""))
        {
            
        	post = new HttpPost(MSG_POST_URL);
            post.setHeader("User-Agent", USER_AGENT);
            
            JSONObject resjson = new JSONObject(content);
            String uid = resjson.getJSONObject("result").getString("uid");
            
            values = new ArrayList<NameValuePair>();
            values.add(new BasicNameValuePair("message", msg));
            values.add(new BasicNameValuePair("to", recipient));
            values.add(new BasicNameValuePair("subject", subject));
            values.add(new BasicNameValuePair("uid", uid));
            
            e = new UrlEncodedFormEntity(values,"UTF-8");
            post.setEntity(e);

            try {
                client.execute(post, response_handler);
            } catch (Exception ex) {
                // This Apache client considers 302 an error in whatever default config it is.
                // Why does this only happen in release builds? Something to look into eventually.
                // 302 is normal though from the Shack message sending call from trying this in
                // Chrome.
                if (!ex.getMessage().equalsIgnoreCase("found")) {
                    System.out.println("EXCEPTION DURING postMessage, MESSAGE: " + ex.getMessage());
                    return false;
                }
            }
            System.out.println("RESPONSE TO POST: " + msg + uid + recipient + subject);
            return true;
        }
        else
        {
        	return false;
        }        
    }	
    
    public static String urlify(String mytext) throws PatternSyntaxException {
        try {
            Matcher matcher = android.util.Patterns.WEB_URL.matcher(mytext);
            if (matcher.find()) {
                return matcher.replaceAll("<a href=\"$0\">$0</a>");
            } else {
                return mytext;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return mytext;
        }
    }
    
    // PUSH NOTIFICATIONS
    public static String regPush(String username, String regId) throws ClientProtocolException, UnsupportedEncodingException, IOException
    {
    	return get(PUSHSERV_URL + "register?&userName=" +  URLEncoder.encode(username, "UTF8") + "&regId=" +  URLEncoder.encode(regId, "UTF8"));
    }
    
    public static String unregPush(String regId) throws ClientProtocolException, UnsupportedEncodingException, IOException
    {
    	return get(PUSHSERV_URL + "unregister?regId=" +  URLEncoder.encode(regId, "UTF8"));
    }
    
    public static String updPush(String type, String regId, String lastSeenId) throws ClientProtocolException, UnsupportedEncodingException, IOException
    {
    	return get(PUSHSERV_URL + "update?type=" + type + "&regId=" +  URLEncoder.encode(regId, "UTF8") + "&lastSeen=" +  URLEncoder.encode(lastSeenId, "UTF8"));
    }
    
    public static String regFastPush(String username, String regId) throws ClientProtocolException, UnsupportedEncodingException, IOException
    {
    	return get(FASTPUSHSERV_URL + "register?&userName=" +  URLEncoder.encode(username, "UTF8") + "&regId=" +  URLEncoder.encode(regId, "UTF8"));
    }
    
    public static String unregFastPush(String regId) throws ClientProtocolException, UnsupportedEncodingException, IOException
    {
    	return get(FASTPUSHSERV_URL + "unregister?regId=" +  URLEncoder.encode(regId, "UTF8"));
    }
    
    public static String updFastPush(String type, String regId, String lastSeenId) throws ClientProtocolException, UnsupportedEncodingException, IOException
    {
    	return get(FASTPUSHSERV_URL + "update?type=" + type + "&regId=" +  URLEncoder.encode(regId, "UTF8") + "&lastSeen=" +  URLEncoder.encode(lastSeenId, "UTF8"));
    }
    
    // cloud sync
    public static JSONObject getCloudPinned(String username) throws ClientProtocolException, IOException, JSONException
    {
    	if (username != null && !username.equals(""))
    		return getJson(CLOUDPIN_URL + URLEncoder.encode(username, "UTF8") + "/settings");
		return null;
    }
    
    public static String putCloudPinned(JSONObject json, String username) throws ClientProtocolException, IOException, JSONException
    {
        // Commented out 2023-02-14 as woggle.net is gone, bring back later?
//        if (username != null && !username.equals(""))
//    	{
//	    	//instantiates httpclient to make request
//    		DefaultHttpClient client = new DefaultHttpClient();
//	        final HttpParams httpParameters = client.getParams();
//	        HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeOutSec * 1000);
//	        HttpConnectionParams.setSoTimeout        (httpParameters, socketTimeoutSec * 1000);
//
//	        //url with the post data
//	        HttpPost httpost = new HttpPost(CLOUDPIN_URL + URLEncoder.encode(username, "UTF8") + "/settings");
//
//	        //passes the results to a string builder/entity
//	        StringEntity se = new StringEntity(json.toString());
//
//	        //sets the post request as the resulting string
//	        httpost.setEntity(se);
//
//	        //sets a request header so the page receving the request
//	        //will know what to do with it
//	        httpost.setHeader("Accept", "application/json");
//
//	        httpost.setHeader("Content-type", "application/json");
//
//	        //Handles what is returned from the page
//	        BasicResponseHandler responseHandler = new BasicResponseHandler();
//	        return client.execute(httpost, responseHandler);
//    	}
		return null;
    }
    
    public static boolean testLogin(Context context, String username, String password) throws Exception {    	
    	BasicResponseHandler response_handler = new BasicResponseHandler();
    	DefaultHttpClient client = new DefaultHttpClient();
        final HttpParams httpParameters = client.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeOutSec * 1000);
        HttpConnectionParams.setSoTimeout        (httpParameters, socketTimeoutSec * 1000);
        HttpPost post = new HttpPost(LOGIN_URL);
        post.setHeader("User-Agent", USER_AGENT);
        post.setHeader("X-Requested-With", "XMLHttpRequest");
        
        List<NameValuePair> values = new ArrayList<NameValuePair>();
        values.add(new BasicNameValuePair("get_fields[]", "result"));
        values.add(new BasicNameValuePair("user-identifier", username));
        values.add(new BasicNameValuePair("supplied-pass", password));
        values.add(new BasicNameValuePair("remember-login", "1"));
        
        UrlEncodedFormEntity e = new UrlEncodedFormEntity(values,"UTF-8");
        post.setEntity(e);
        
        String content = client.execute(post, response_handler);
        
        if (content.contains("{\"result\":{\"valid\":\"true\""))
        {
            return true;
        }
        else
        {
        	return false;
        }        
    }

	public static boolean noteAddUser(String userName, JSONArray keywords, boolean vanityEnabled) throws ClientProtocolException, UnsupportedEncodingException, IOException, JSONException {
        List<NameValuePair> values = new ArrayList<>();
        values.add(new BasicNameValuePair("UserName", userName));
        values.add(new BasicNameValuePair("NotifyOnUserName", vanityEnabled ? "1" : "0"));
        for(int i = 0; i < keywords.length(); i++) {
            values.add(new BasicNameValuePair("NotificationKeywords[" + i + "]", keywords.getString(i)));
        }
        return post(NOTESERV_URL + "/v2/user", values);
	}
	public static boolean noteReg(String userName, String deviceid) throws ClientProtocolException, UnsupportedEncodingException, IOException {
        List<NameValuePair> values = new ArrayList<>();
        values.add(new BasicNameValuePair("UserName", userName));
        values.add(new BasicNameValuePair("DeviceId", "fcm://" + deviceid));
        values.add(new BasicNameValuePair("ChannelUri", "fcm://" + deviceid));
        return post(NOTESERV_URL + "/register", values);
	}
	public static boolean noteUnreg(String userName, String deviceid) throws ClientProtocolException, UnsupportedEncodingException, IOException {
        List<NameValuePair> values = new ArrayList<>();
        values.add(new BasicNameValuePair("DeviceId", "fcm://" + deviceid));
        return post(NOTESERV_URL + "/deregister", values);
	}

	/*
	Blocklist
	 */
    public static String blocklistAdd (String userName, String keyword) throws ClientProtocolException, UnsupportedEncodingException, IOException {
        return getSSL(NOTESERV_URL_SSL + "blocklist.php?apikey=" + APIConstants.BLOCKLIST_API_KEY + "&type=blocklist&action=add&user=" + URLEncoder.encode(userName, "UTF8") + "&item=" + URLEncoder.encode(keyword, "UTF8"));
    }
    public static String blocklistRemove (String userName, String keyword) throws ClientProtocolException, UnsupportedEncodingException, IOException {
        return getSSL(NOTESERV_URL_SSL + "blocklist.php?apikey=" + APIConstants.BLOCKLIST_API_KEY + "&type=blocklist&action=remove&user=" + URLEncoder.encode(userName, "UTF8") + "&item=" + URLEncoder.encode(keyword, "UTF8"));
    }
    public static String blocklistCheck (String userName) throws ClientProtocolException, UnsupportedEncodingException, IOException {
        return getSSL(NOTESERV_URL_SSL + "blocklist.php?apikey=" + APIConstants.BLOCKLIST_API_KEY + "&type=blocklist&action=get&user=" + URLEncoder.encode(userName, "UTF8"));
    }

    /*
        DONATOR LIST
     */
    public static JSONArray getDonators() throws ClientProtocolException, IOException, JSONException
    {
        return new JSONArray(get(DONATOR_URL));
    }
    public static String[] getLimeList() throws ClientProtocolException, IOException, JSONException
    {
        JSONArray list = getDonators();
        String limes = new String();
        String quadLimes = new String();
        String goldLimes = new String();
        for (int i = 0; i < list.length(); i++)
        {
            if (list.getJSONObject(i).getString("lime").equals("1") && list.getJSONObject(i).getString("type").equals("0")) {
                limes = limes + ":" + list.getJSONObject(i).getString("user").toLowerCase() + ";\n";
            }
            if (list.getJSONObject(i).getString("lime").equals("1") && list.getJSONObject(i).getString("type").equals("1")) {
                goldLimes = goldLimes + ":" + list.getJSONObject(i).getString("user").toLowerCase() + ";\n";
            }
            if (list.getJSONObject(i).getString("lime").equals("1") && list.getJSONObject(i).getString("type").equals("2")) {
                quadLimes = quadLimes + ":" + list.getJSONObject(i).getString("user").toLowerCase() + ";\n";
            }
        }
        return new String[]{limes, goldLimes, quadLimes};
    }
    public static boolean getLimeStatus(String username) throws ClientProtocolException, IOException, JSONException
    {
        if (username != null)
        {
            String lime = get(DONATOR_URL + URLEncoder.encode(username, "UTF8"));
            if (lime.equalsIgnoreCase("1"))
            {
                return true;
            }
        }
        return false;
    }

    public static String putDonator(boolean lime, String username) throws ClientProtocolException, IOException, JSONException
    {
        if (username != null)
        {
            //instantiates httpclient to make request
            DefaultHttpClient client = new DefaultHttpClient();
            final HttpParams httpParameters = client.getParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeOutSec * 1000);
            HttpConnectionParams.setSoTimeout        (httpParameters, socketTimeoutSec * 1000);

            //url with the post data
            HttpPost httpost = new HttpPost(DONATOR_URL + URLEncoder.encode(username, "UTF8"));

            //passes the results to a string builder/entity
            StringEntity se = new StringEntity(lime ? "true" : "false");

            //sets the post request as the resulting string
            httpost.setEntity(se);

            //sets a request header so the page receving the request
            //will know what to do with it
            httpost.setHeader("Accept", "application/json");

            httpost.setHeader("Content-type", "application/json");

            //Handles what is returned from the page
            BasicResponseHandler responseHandler = new BasicResponseHandler();
            return client.execute(httpost, responseHandler);
        }
        return null;
    }

	public static String getYTTitleAPI(String ytid)
	{
		String title = "";
		JSONObject response = null;
		try {
			response = getJson("https://www.googleapis.com/youtube/v3/videos?part=snippet&id=" + URLEncoder.encode(ytid, "UTF8") + "&key=AIzaSyBKsZFus5jO-MefknNd58QYa3sAcQhUo2Q");
			JSONArray items = response.getJSONArray("items");


			for(int i=0;i<items.length();i++){
				System.out.println("Item "+i+" : ");
				System.out.println("Title : "+items.getJSONObject(i).getJSONObject("snippet").get("title"));

				System.out.println("Description : "+items.getJSONObject(i).getJSONObject("snippet").get("description"));
				System.out.println("URL : https://www.youtube.com/watch?v="+items.getJSONObject(i).getJSONObject("id").getString("videoId"));

			}

			title = items.getJSONObject(0).getJSONObject("snippet").getString("title");
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return title;
	}
	public static String getYTTitle(String ytid)
	{
		String title = "";
		JSONObject response = null;
		try {
			response = getJson("https://noembed.com/embed?url=https://www.youtube.com/watch?v=" + URLEncoder.encode(ytid, "UTF8"));
			title = response.getString("title");
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return title;
	}
}
