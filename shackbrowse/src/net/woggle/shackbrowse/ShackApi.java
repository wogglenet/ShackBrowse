package net.woggle.shackbrowse;

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

import net.woggle.ApiUrl;

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
import android.widget.Toast;


public class ShackApi
{
	private static final int connectionTimeOutSec = 25;
	private static final int socketTimeoutSec = 35;
    static final String USER_AGENT = "shackbrowse/4.0.5";
    
    static final String IMAGE_LOGIN_URL = "http://chattypics.com/users.php?act=login_go";
    static final String IMAGE_UPLOAD_URL = "http://chattypics.com/upload.php";
    
    static final String LOGIN_URL = "https://www.shacknews.com/account/signin";
    static final String CHECKUSER_URL = "https://www.shacknews.com/account/username_exists";
    static final String MARKREAD_URL = "https://www.shacknews.com/messages/read";
    static final String MOD_URL = "http://www.shacknews.com/mod_chatty.x";
    static final String POST_URL = "https://www.shacknews.com/api/chat/create/17.json";
    static final String NEW_POST_URL = "http://www.shacknews.com/post_chatty.x";
    static final String MSG_POST_URL = "http://www.shacknews.com/messages/send";
    static final String LOL_URL = "http://www.lmnopc.com/greasemonkey/shacklol/report.php";
    static final String GET_LOL_URL = "http://lmnopc.com/greasemonkey/shacklol/api.php";
    
    static final String LOL_VERSION = "20090513";
    
    static final String PUSHSERV_URL = "http://shackbrowsepublic.appspot.com/";
    static final String FASTPUSHSERV_URL = "http://shackbrowse.appspot.com/";
    
    static final String NOTESERV_URL = "http://woggle.net/shackbrowsenotification/";
    
    static final String BASE_URL = "http://shackapi.hughes.cc/";
    static final String BASE_URL_ALT = "http://woggle.net/shackbrowseAPI/";
    static final String BASE_URL_ALT2 = "http://shackbrowseapi.appspot.com/";
    static final String WINCHATTYV2_API = "http://winchatty.com/v2/";
    static final String WOGGLEV2_API = "http://api.woggle.net/v2/";
    static final String CLOUDPIN_URL = "http://woggle.net/shackcloudpin/";
    static final String DONATOR_URL = "http://woggle.net/shackbrowsedonators/";
    static final String FAKE_STORY_ID = "17";
    static final String FAKE_NEWS_ID = "2";
    
    static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    
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
        System.out.println(content);
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
    	int type = tryParseInt(prefs.getString("apiUrl2", "5"));
    	if (type == 1)
    	{
    		return new ApiUrl(BASE_URL_ALT, false);
    	}
    	else if (type == 3)
    	{
    		return new ApiUrl(BASE_URL_ALT2, false);
    	}
    	else if (type == 4)
    	{
    		return new ApiUrl(WINCHATTYV2_API, true);
    	}
    	else if (type == 5)
    	{
    		return new ApiUrl(WOGGLEV2_API, true);
    	}
    	else if (type == 2)
    	{
    		return new ApiUrl(prefs.getString("apiCustom", BASE_URL), false);
    	}
		return new ApiUrl(BASE_URL_ALT2,false);
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
        File file = new File(imageLocation);
        String name = file.getName();
        name = "shackbrowseUpload.jpg";
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
    
    public static JSONObject postReply(Context context, int replyToThreadId, String content, boolean isNewsItem) throws Exception
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String userName = prefs.getString("userName", null).trim();
        String password = prefs.getString("password", null);
        
        List<NameValuePair> values = new ArrayList<NameValuePair>();
        values.add(new BasicNameValuePair("content_type_id", isNewsItem ? FAKE_NEWS_ID : FAKE_STORY_ID));
        values.add(new BasicNameValuePair("content_id", isNewsItem ? FAKE_NEWS_ID : FAKE_STORY_ID));
        values.add(new BasicNameValuePair("body", content));
        if (replyToThreadId > 0)
            values.add(new BasicNameValuePair("parent_id", Integer.toString(replyToThreadId)));
        
        System.out.println("SHACKAPI: SUBMITTING JSON FOR POST:" + content);
        
       /* DefaultHttpClient httpclient = new DefaultHttpClient();
        String encoding = android.util.Base64.encodeToString((userName + ":" + password).getBytes(), android.util.Base64.NO_WRAP);
        HttpPost httppost = new HttpPost(POST_URL);
        httppost.setHeader("Authorization", "Basic " + encoding);

        System.out.println("executing request " + httppost.getRequestLine());
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();
        JSONObject result = new JSONObject(EntityUtils.toString(entity));
        entity.consumeContent();*/
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
        con.setReadTimeout(30000);
        con.setRequestProperty("connection", "close");
        con.setConnectTimeout(10000);
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
        }
        else
        {
            visible_categories.add("informative");
            visible_categories.add("offtopic");
            visible_categories.add("stupid");
            visible_categories.add("political");
            visible_categories.add("ontopic");
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
            
            /*
             * This function has been moved to threadviewfragment adapter
             * 
             * 
            // create depthstrings
            for (int i = 0; i < posts.size(); i++)
            {
            	int j = i -1;
            	while ((j > 0) && (posts.get(j).getLevel() >= posts.get(i).getLevel()))
            	{
            		StringBuilder jDString = new StringBuilder(posts.get(j).getDepthString());
            		
            		if ((jDString.charAt(posts.get(i).getLevel()-1) == "L".charAt(0)) && (posts.get(i).getLevel() == posts.get(j).getLevel()))
            		{
            			jDString.setCharAt(posts.get(i).getLevel()-1, "T".charAt(0));
            		}
            		if ((jDString.charAt(posts.get(i).getLevel()-1) == "[".charAt(0)) && (posts.get(i).getLevel() == posts.get(j).getLevel()))
            		{
            			jDString.setCharAt(posts.get(i).getLevel()-1, "+".charAt(0));
            		}
            		if (jDString.charAt(posts.get(i).getLevel()-1) == "0".charAt(0))
            		{
            			// ! denotes blue line, | denotes gray
            			if (posts.get(i).getSeen())
            				jDString.setCharAt(posts.get(i).getLevel()-1, "|".charAt(0));
            			else
            				jDString.setCharAt(posts.get(i).getLevel()-1, "!".charAt(0));
            		}
            		
            		posts.get(j).setDepthString(jDString.toString());
            		j--;
            	}
            }
            
            // collapser for deep threads
            for (int i = 0; i < posts.size(); i++)
            {
	            StringBuilder depthStr = new StringBuilder(posts.get(i).getDepthString());
	        	
	        	// collapser for deep threads
	        	if (depthStr.length() >= _maxBullets)
	        	{
		        	int j = 0;
		        	String depthStr2 = depthStr.toString();
		        	while (depthStr2.length() > _maxBullets)
		        	{
		        		depthStr2 = depthStr2.substring(5);
		        		j++;
		        	}
		        	if (j > 0)
		        	{
			        	String repeated = new String(new char[j]).replace("\0", "C");
			        	String repeated2 = new String(new char[(depthStr2.length() - 1)]).replace("\0", "0");
			        	depthStr = new StringBuilder(repeated + repeated2 + "L");
		        	}
		        	// end collapser
	        	}

        		posts.get(i).setDepthString(depthStr.toString());
            }
            */
            
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
    
    public static void tagPost(int postId, String tag, String userName) throws Exception
    {
    	BasicResponseHandler response_handler = new BasicResponseHandler();
        DefaultHttpClient client = new DefaultHttpClient();
        final HttpParams httpParameters = client.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeOutSec * 1000);
        HttpConnectionParams.setSoTimeout(httpParameters, socketTimeoutSec * 1000);
        HttpPost post = new HttpPost(LOL_URL);
        post.setHeader("User-Agent", USER_AGENT);
        
        List<NameValuePair> values = new ArrayList<NameValuePair>();
        values.add(new BasicNameValuePair("who", userName));
        values.add(new BasicNameValuePair("what", Integer.toString(postId)));
        values.add(new BasicNameValuePair("tag", tag));
        values.add(new BasicNameValuePair("version", URLEncoder.encode(LOL_VERSION, "UTF-8")));
        
        UrlEncodedFormEntity e = new UrlEncodedFormEntity(values,"UTF-8");
        post.setEntity(e);
        
        String content = client.execute(post, response_handler);
        System.out.println("TAGGED POST" + postId + " RESPONSE: " + content);
    	/* THOMW UPDATES LOLAPI
        // construct the lol stuff
        String url = LOL_URL + "?who=" + URLEncoder.encode(userName, "UTF8") + "&what=" + postId + "&tag=" + URLEncoder.encode(tag, "UTF8") + "&version=" + URLEncoder.encode(LOL_VERSION, "UTF-8");
        
        // make things work
        String content = get(url);
        
        Log.d("shackbrowse", "LOL response: " + content);
        */
    	
        // see if it did work
        if (!content.contains("ok"))
            throw new Exception(content);
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
    
    public static HashMap<String, HashMap<String, LolObj>> getLols (Activity activity) throws ClientProtocolException, IOException, JSONException
    {
    	boolean _getLols = true;
    	if (activity != null)
        {
    		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
            _getLols = prefs.getBoolean("getLols", true);
        }
        else return new HashMap<String, HashMap<String,LolObj>>();
        
        if (!_getLols)
        {
        	return new HashMap<String, HashMap<String,LolObj>>();
        }
        
        JSONObject json = new JSONObject();
        boolean mustRefresh = true;
        
        if ((activity != null) && (activity.getFileStreamPath("shacklol.cache").exists()))
        {
            // look at that, we got a file
            try {
                FileInputStream input = activity.openFileInput("shacklol.cache");
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
                        		
                        		// six minute wait to refresh
                        		if ((newTime - (60000 * 6)) < oldTime)
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
        		newJson = getJson(GET_LOL_URL + "?special=getcounts");
        	}
        	catch (JSONException e)
        	{
        		System.out.println("connection timeout for LOLDATA");
        		lolTimedOut = true;
        		final Context bcont = activity.getBaseContext();
        		activity.runOnUiThread(new Runnable(){
             		@Override public void run()
             		{
             			
             			Toast.makeText(bcont, "ShackLOL server timed out\nConsider disabling ShackLOL API in Preferences", Toast.LENGTH_SHORT).show();
             		}
             	});
        		
        		// cache it
    	        FileOutputStream output = activity.openFileOutput("shacklol.cache", Activity.MODE_PRIVATE);
    	        try
    	        {
    	            DataOutputStream out = new DataOutputStream(output);
    	            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
    	            
    	            // write timestamp, should cause us to wait another 5 minutes
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
    	        
        		return processLols(json, activity);
        	}
        	
        	// cache it
	        FileOutputStream output = activity.openFileOutput("shacklol.cache", Activity.MODE_PRIVATE);
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
        int ughCount = 0;
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
	        	ughCount = 0;
	        	
	        	for (int j = 0; j <  posts.length(); j++)
	            {
	        		JSONObject thisPost = thisThread.getJSONObject(posts.getString(j));
	        		lolobj = new LolObj();
	        		if (thisPost.has("lol")) { lolobj.setLol(tryParseInt(thisPost.getString("lol"))); lolCount = lolCount + tryParseInt(thisPost.getString("lol")); }
	        		if (thisPost.has("inf")) { lolobj.setInf(tryParseInt(thisPost.getString("inf"))); infCount = infCount + tryParseInt(thisPost.getString("inf")); }
	        		if (thisPost.has("unf")) { lolobj.setUnf(tryParseInt(thisPost.getString("unf"))); unfCount = unfCount + tryParseInt(thisPost.getString("unf")); }
	        		if (thisPost.has("tag")) { lolobj.setTag(tryParseInt(thisPost.getString("tag"))); tagCount = tagCount + tryParseInt(thisPost.getString("tag")); }
	        		if (thisPost.has("wtf")) { lolobj.setWtf(tryParseInt(thisPost.getString("wtf"))); wtfCount = wtfCount + tryParseInt(thisPost.getString("wtf")); }
	        		if (thisPost.has("ugh")) { lolobj.setUgh(tryParseInt(thisPost.getString("ugh"))); ughCount = ughCount + tryParseInt(thisPost.getString("ugh")); }
	        		lolobj.genTagSpan(context);
	        		map.get(threads.getString(i)).put(posts.getString(j), lolobj);
	            }
	        	
	        	lolobj = new LolObj();
	        	lolobj.setLol(lolCount);
	        	lolobj.setTag(tagCount);
	        	lolobj.setInf(infCount);
	        	lolobj.setWtf(wtfCount);
	        	lolobj.setUnf(unfCount);
	        	lolobj.setUgh(ughCount);
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
    	ArrayList<Thread> new_threads = processThreads(json, true, activity);
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
    public static ArrayList<String> getLOLTaggers(int postId, String type) throws JSONException, ClientProtocolException, IOException
    {
    	String url = GET_LOL_URL + "?special=get_taggers&thread_id=" + postId + "&tag=" + type;
    	ArrayList<String> results = new ArrayList<String>();
    	
    	JSONArray result = new JSONObject(get(url,true)).getJSONArray(type);
        for (int i = 0; i < result.length(); i++)
        {
            results.add(result.getString(i));
            
        }
        
        return results;
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
            if (comment.getString("tag").equalsIgnoreCase("ugh"))
            	type = SearchResult.TYPE_UGH;
            	
            
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
	            return true;
	        }
	        else return false;
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

        	HttpGet get = new HttpGet("http://shacknews.com/messages/"+getType+"?page=" + pageNumber);
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
            	
            content = client.execute(post, response_handler);
            System.out.println("RESPOSE TO POST: " + msg + uid + recipient + subject);
            
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
    
    // LIMES
    /*
    public static String getLimes() throws ClientProtocolException, IOException
    {
    	return get(BASE_URL_ALT + "limes.txt");
    }
    
    public static String addLime(String username, String data, String sign) throws ClientProtocolException, UnsupportedEncodingException, IOException
    {
    	return get(BASE_URL_ALT + "limes2.php?mode=add&user=" +  URLEncoder.encode(username, "UTF8") + "&data=" +  URLEncoder.encode(data, "UTF8") + "&sign=" +  URLEncoder.encode(sign, "UTF8"));
    }
    
    public static String removeLime(String data, String sign) throws ClientProtocolException, UnsupportedEncodingException, IOException
    {
    	return get(BASE_URL_ALT + "limes2.php?mode=remove&data=" +  URLEncoder.encode(data, "UTF8") + "&sign=" +  URLEncoder.encode(sign, "UTF8"));
    }
    */
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
        if (username != null && !username.equals(""))
    	{
	    	//instantiates httpclient to make request
    		DefaultHttpClient client = new DefaultHttpClient();
	        final HttpParams httpParameters = client.getParams();
	        HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeOutSec * 1000);
	        HttpConnectionParams.setSoTimeout        (httpParameters, socketTimeoutSec * 1000);
	
	        //url with the post data
	        HttpPost httpost = new HttpPost(CLOUDPIN_URL + URLEncoder.encode(username, "UTF8") + "/settings");
	
	        //passes the results to a string builder/entity
	        StringEntity se = new StringEntity(json.toString());
	
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

    // version
    public static String getVersion() throws ClientProtocolException, IOException
    {
    	return get(BASE_URL_ALT + "versions.txt");
    }

	public static String noteAddUser(String userName, String getreplies, String getvanity) throws ClientProtocolException, UnsupportedEncodingException, IOException {
		return get(NOTESERV_URL + "change.php?type=user&action=add&user=" + URLEncoder.encode(userName, "UTF8") + "&getreplies=" + getreplies + "&getvanity=" + getvanity );
	}
	public static String noteReg(String userName, String deviceid) throws ClientProtocolException, UnsupportedEncodingException, IOException {
		return get(NOTESERV_URL + "change.php?type=device&action=add&user=" + URLEncoder.encode(userName, "UTF8") + "&deviceid=" + URLEncoder.encode(deviceid, "UTF8"));
	}
	public static String noteUnreg(String userName, String deviceid) throws ClientProtocolException, UnsupportedEncodingException, IOException {
		return get(NOTESERV_URL + "change.php?type=device&action=remove&user=" + URLEncoder.encode(userName, "UTF8") + "&deviceid=" + URLEncoder.encode(deviceid, "UTF8"));
	}
	public static JSONObject noteGetUser(String userName) throws ClientProtocolException, UnsupportedEncodingException, IOException, JSONException {
		return getJson(NOTESERV_URL + "getuser.php?user=" + URLEncoder.encode(userName, "UTF8"));
	}
	public static String noteAddKeyword(String userName, String keyword) throws ClientProtocolException, UnsupportedEncodingException, IOException {
		return get(NOTESERV_URL + "change.php?type=keyword&action=add&user=" + URLEncoder.encode(userName, "UTF8") + "&keyword=" + URLEncoder.encode(keyword, "UTF8"));
	}
	public static String noteRemoveKeyword(String userName, String keyword) throws ClientProtocolException, UnsupportedEncodingException, IOException {
		return get(NOTESERV_URL + "change.php?type=keyword&action=remove&user=" + URLEncoder.encode(userName, "UTF8") + "&keyword=" + URLEncoder.encode(keyword, "UTF8"));
	}
	public static JSONObject noteGetKeywords(String userName) throws ClientProtocolException, UnsupportedEncodingException, IOException, JSONException {
		return getJson(NOTESERV_URL + "getkeywords.php?user=" + URLEncoder.encode(userName, "UTF8"));
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
}
