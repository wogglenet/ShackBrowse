package net.woggle.shackbrowse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.SparseIntArray;
import android.widget.Toast;

public class OfflineThread
{
	Activity _activity;
	private Hashtable<Integer, SavedThreadObj> _threads = new Hashtable<Integer,SavedThreadObj>();
	
	private String FIRST_TOKEN = "@#$%@";
	private String SECOND_TOKEN = "<#$%>";
	private String THREADCACHE_NAME = "savedthreads2.cache";
	
	private Handler cloudUpdateHandler;
	
	private boolean _verbose;
	private String _verboseMsg;
	
	public OfflineThread (Activity activity)
	{
		_activity = activity;
		loadThreadsFromDiskTask();
		
		// start the periodic updater
		endCloudUpdates();
		cloudUpdateHandler = new Handler();
	}
	public int getCount()
	{
		return _threads.size();
	}
	public boolean containsThreadId(int key)
	{			
		return _threads.containsKey(key);
	}
	
	public void updateRecordedReplyCount(int threadId, int count)
	{
		SavedThreadObj value = _threads.get(threadId);
		if (value != null)
		{
			try {
				// +1 because server is off by one kill me now
				value._threadJson.put("reply_count", count);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			_threads.put(threadId, value);
		}
    }
	
	public void updateRecordedReplyCountPrev(int threadId, int count)
	{
		SavedThreadObj value = _threads.get(threadId);
		if (value != null)
		{
			try {
				// +1 because server is off by one kill menow
				value._threadJson.put("reply_count_prev", count);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			_threads.put(threadId, value);
		}
    }
	
	public class SavedThreadObj
	{
		public int _rootId;
		public long _postTime;
		public JSONObject _threadJson;
		public int previousReplyCount;
		public SavedThreadObj (int rootId,long postTime,JSONObject postsJson)
		{
			_rootId = rootId; _postTime = postTime; _threadJson = postsJson;
		}
	}
	
	public boolean saveThread(int rootId, long postedTime, JSONObject json)
	{
		if (!_threads.containsKey(rootId))
		{
			System.out.println("OFFLINETHREAD: could not find this thread in cache, saving");
			SavedThreadObj thisThread = new SavedThreadObj(rootId,postedTime,json);
			SaveThreadTask(thisThread);
			
			return true;
		}
		else
		{
			System.out.println("OFFLINETHREAD: thread already exists. not saving duped thread in cache.");
			return false;
		}
	}
	
	public boolean toggleThread(int rootId, long postedTime, JSONObject json)
	{
		if (!_threads.containsKey(rootId))
		{
			try {
				json.put("reply_count_prev", json.getInt("reply_count"));
				System.out.println("OFFLINETHREAD: could not find this thread in cache, saving. RCP: " + json.getInt("reply_count_prev"));
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			SavedThreadObj thisThread = new SavedThreadObj(rootId,postedTime,json);
			SaveThreadTask(thisThread);

			return true;
		}
		else
		{
			System.out.println("OFFLINETHREAD: thread already exists. removing.");
			_threads.remove(rootId);
			flushThreadsToDiskTask();
			triggerLocalToCloud();
			return false;
		}
	}
	
 	public Hashtable<Integer,SavedThreadObj> loadThreadsFromDiskTask()
	{
 		_threads = new Hashtable<Integer,SavedThreadObj>();
 		System.out.println("OFFLINETHREAD: reloading from disk");
 		int i = 0;
 		
		 if ((_activity != null) && (_activity.getFileStreamPath(THREADCACHE_NAME).exists()))
	     {
	         // look at that, we got a file
	         try {
	             FileInputStream input = _activity.openFileInput(THREADCACHE_NAME);
	             try
	             {
	                 DataInputStream in = new DataInputStream(input);
	                 BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	                 String line = reader.readLine();
	                 
	                 while (line != null)
	                 {
	                     if (line.length() > 0)
	                     {
	                    	 if ((line.indexOf(FIRST_TOKEN) > -1) && (line.indexOf(SECOND_TOKEN) > -1))
	                    	 {
	                    		 String rootId = line.substring(0, line.indexOf(FIRST_TOKEN));
	                    		String postedTime = line.substring(line.indexOf(FIRST_TOKEN) + FIRST_TOKEN.length(), line.indexOf(SECOND_TOKEN));
	                    		String json = line.substring(line.indexOf(SECOND_TOKEN) + SECOND_TOKEN.length());
	                    		
	                    		i++;
	                    		_threads.put(Integer.valueOf(rootId), new SavedThreadObj(Integer.valueOf(rootId),Long.valueOf(postedTime), new JSONObject(json)));
	                    		
	                    	 }
	                     }
	                     	
	                     line = reader.readLine();
	                     
	                 }
	             } catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	             finally
	             {
	                 input.close();
	             }
	         }
	         catch (IOException e) { e.printStackTrace(); }
	     }
		 System.out.println("OFFLINETHREAD: loaded " + i + " threads from disk");
		 
		 return _threads;
	}
    
 	public boolean SaveThreadTask (SavedThreadObj thread)
	{
 		boolean result = true;

		// TODO Auto-generated method stub
		// cache it
        try
        {
        	System.out.println("OFFLINETHREAD: opening for save");
        	FileOutputStream _output = _activity.openFileOutput(THREADCACHE_NAME, Activity.MODE_APPEND);
	        try
	        {
	            DataOutputStream out = new DataOutputStream(_output);
	            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
	            
	            // write line
	            writer.append(Integer.toString(thread._rootId) + FIRST_TOKEN + Long.toString(thread._postTime) + SECOND_TOKEN + thread._threadJson.toString());
	            writer.newLine();
	            writer.flush();
	            
	            System.out.println("OFFLINETHREAD: appended 1 thread to disk JSON: " + thread._threadJson.toString());
	        }
	        catch (IOException e)
	        {
	        	 System.out.println("OFFLINETHREAD: exception1 " + e.toString());
	        	 e.printStackTrace();
	        	result = false;
	        }
	        _output.close();
		}
	    catch (Exception e)
	    {
	    	System.out.println("OFFLINETHREAD: exception2 " + e.getMessage());
	    	e.printStackTrace();
	    	result = false;
	    }
    
        if (result)
        {
        	_threads.put(thread._rootId, thread);
    		
        }
        triggerLocalToCloud();
		return result;
	}
 	
 	public boolean flushThreadsToDiskTask()
	{
 		boolean result = true;

		// TODO Auto-generated method stub
		// cache it
        try
        {
        	FileOutputStream _output = _activity.openFileOutput(THREADCACHE_NAME, Activity.MODE_PRIVATE);
	        try
	        {
	            DataOutputStream out = new DataOutputStream(_output);
	            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
	            
	            // clear file
	            writer.write("");
	             int i = 0;
	             
	            Enumeration keys = _threads.keys();
	            while( keys.hasMoreElements() ) {
					Object key = keys.nextElement();
					SavedThreadObj value = _threads.get(key);
	            
					i++;
		            // write line
		            writer.write(Integer.toString(value._rootId) + FIRST_TOKEN + Long.toString(value._postTime) + SECOND_TOKEN + value._threadJson.toString());
		            writer.newLine();
	            }
	            writer.flush();
	            
	            System.out.println("OFFLINETHREAD: flushed " + i + " threads to disk");
	        }
	        catch (Exception e)
	        {
	        	result = false;
	        }
	        _output.close();
		}
	    catch (Exception e)
	    {
	    	result = false;
	    }
		return result;
	}
 	
	public void deleteAllThreadsTask()
 	{
 		System.out.println("OFFLINETHREAD: deleting cache");
 		_threads.clear();
		flushThreadsToDiskTask();
		triggerLocalToCloud();
		/*
    	try {
    		
			FileOutputStream _output = _activity.openFileOutput(THREADCACHE_NAME, Activity.MODE_PRIVATE);
			DataOutputStream out = new DataOutputStream(_output);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
            writer.write("");
            writer.flush();
            _output.close();
           
    		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
 	}
 	public void deleteExpiredThreadsTask()
 	{
 		System.out.println("OFFLINETHREAD: deleting expired");
 		
 		 Enumeration keys = _threads.keys();
         while( keys.hasMoreElements() ) {
				Object key = keys.nextElement();
				SavedThreadObj value = _threads.get(key);
				if (TimeDisplay.threadAgeInHours(value._postTime) > 18d)
				{
					_threads.remove(key);
				}
         }
         flushThreadsToDiskTask();
         triggerLocalToCloud();
 	}
 	
 	public JSONObject getThreadsAsJson()
 	{
 		return getThreadsAsJson(false, false);
 	}
	public JSONObject getThreadsAsJson(boolean asc, boolean onlyLessThan18Hours)
 	{
 		JSONObject listJson = new JSONObject(); 
 		JSONArray array = new JSONArray();
 		 
 		List<Integer> postIds = Collections.list(_threads.keys());
        Collections.sort(postIds);
        
        if (asc)
 			Collections.reverse(postIds);

        int i = postIds.size() -1;
    	while ( i >= 0) {
			SavedThreadObj value = _threads.get(postIds.get(i));
		    if ((TimeDisplay.threadAgeInHours(value._postTime) > 18d) && (onlyLessThan18Hours))
		    {
			    i--;
			    continue;
		    }
			// indicate that these are pinned
			try {
				value._threadJson.put("pinned", true);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			array.put(value._threadJson);
			i--;
    	}
       
        try {
			listJson.accumulate("comments", array);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        System.out.println("OFFLINETHREAD: faked json data for " + postIds.size() + " threads");
        return listJson;
 	}
 	
 	/*
 	 * Cloud sync
 	 */
 	
 	public void triggerCloudToLocal() {
 		System.out.println("OFFLINETHREAD: triggered cloudtolocal");
 		if (getCloudUsername() !=  null)
 			new CloudToLocal().execute();
	}
 	public void triggerLocalToCloud() {
 		if (getCloudUsername() !=  null)
 			new LocalToCloud().execute();
	}
 	public void triggerCloudMerge() {
 		if (getCloudUsername() !=  null)
 			new CloudMerge().execute();
	}
 	class CloudToLocal extends AsyncTask <String, Void, JSONArray>
 	{
		@Override
		protected JSONArray doInBackground(String... params) {
			JSONObject cloudJson = new JSONObject();
			
			JSONArray watched = new JSONArray();
			try {
				cloudJson = ShackApi.getCloudPinned(getCloudUsername());
			}
			catch (Exception e)
			{

			}
			try {
				watched = cloudJson.getJSONArray("watched");
				
				System.out.println("OFFLINETHREAD: CLOUDTOLOCAL: watched#: " + watched.length());
				
				Hashtable<Integer, SavedThreadObj> _newthreads = new Hashtable<Integer,SavedThreadObj>();
				
				// convert to arraylist
				List<Integer> watchedList = new ArrayList<Integer>();
				for (int i=0; i<watched.length(); i++) {
				    try {
						watchedList.add(Integer.parseInt(watched.getString(i)));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				// all existing threads must be represented in cloud. if not, delete
				Enumeration keys = _threads.keys();
	            while( keys.hasMoreElements() ) {
					Object key = keys.nextElement();
					SavedThreadObj value = _threads.get(key);
					
					if (watchedList.contains(value._rootId))
					{
						// add this thread, it exists locally and on server
						_newthreads.put(value._rootId, value);
					}
	            }
	            
	            // if any ids in the watchlist are not in newthreads, add them
	            for (int key: watchedList)
				{
					if (!_newthreads.containsKey(key))
					{
						Thread fakeThread = new Thread(key, "Cloud Data", "This thread was saved to the cloud from another device and will load upon next refresh.", 10L, 1, "ontopic", false, true);
						fakeThread.setReplyCountPrevious(1);
						SavedThreadObj value = new SavedThreadObj(key, 0L, fakeThread.getJson());
						// add nonexistent
						_newthreads.put(key, value);
						
					}
				}
	            
	            // fix any broken "Cloud data will load shortly threads", including the ones we just made
	            keys = _newthreads.keys();
	            ArrayList<Integer> getDataList = new ArrayList<Integer>();
	            while( keys.hasMoreElements() ) {
					Integer key = (Integer)keys.nextElement();
					if (_newthreads.get(key)._threadJson.getString("author").equals("Cloud Data"))
						getDataList.add(key);
	            }
	            if (getDataList.size() > 0)
					new UpdateCloudThread().execute(getDataList);
	            
	            // update field with new thread list
	            _threads = _newthreads;
	            // and save
	            flushThreadsToDiskTask();
	            if (_verbose) _verboseMsg = "Sync Success" + _threads.size() + " items";
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				verboseError();
				// if this happens, data on server may be corrupt. delete it.

			}
			
			_activity.runOnUiThread(new Runnable(){
        		@Override public void run()
        		{
        			((MainActivity)_activity).mOfflineThreadsNotifyAdapter();
        		}
			});
			
			
			return watched;
		}
		@Override
		protected void onPostExecute(JSONArray result)
		{
			if (_verbose) Toast.makeText(_activity, _verboseMsg, Toast.LENGTH_SHORT).show();
			_verbose = false;
		}
 	}
 	
 	class LocalToCloud extends AsyncTask <String, Void, JSONArray>
 	{
		@Override
		protected JSONArray doInBackground(String... params) {
			// all existing threads will be represented in cloud
			JSONArray watched = new JSONArray();
			
			Enumeration keys = _threads.keys();
            while( keys.hasMoreElements() ) {
				Object key = keys.nextElement();
				watched.put(key);
            }

			JSONObject cloudJson = new JSONObject();
			try {
				cloudJson = ShackApi.getCloudPinned(getCloudUsername());
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				verboseError();
			}
			try {
				cloudJson.put("watched",watched);
				String result = ShackApi.putCloudPinned(cloudJson, getCloudUsername());
				System.out.println("OFFLINETHREAD: LOCALTOCLOUD: watched#: " + watched.length());
				if ((_verbose) && (result != null)) _verboseMsg = "Sync Success" + watched.length() + " items";
				
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				verboseError();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				verboseError();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				verboseError();
			}
			
			
			return watched;
		}
		@Override
		protected void onPostExecute(JSONArray result)
		{
			if (_verbose) Toast.makeText(_activity, _verboseMsg, Toast.LENGTH_SHORT).show();
			_verbose = false;
		}
 	}
 	class CloudMerge extends AsyncTask <String, Void, JSONArray>
 	{
		@Override
		protected JSONArray doInBackground(String... params) {
			JSONObject cloudJson;
			
			JSONArray watched = new JSONArray();
			try {
				cloudJson = ShackApi.getCloudPinned(getCloudUsername());
				watched = cloudJson.getJSONArray("watched");
				
				// move jsonarray to arraylist
				List<Integer> watchedList = new ArrayList<Integer>();
				for (int i=0; i<watched.length(); i++) {
				    try {
						watchedList.add(Integer.parseInt(watched.getString(i)));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
	            
	            // if any ids in the watchlist are not in threads, add them
	            for (int key: watchedList)
				{
	            	ArrayList<Integer> getDataList = new ArrayList<Integer>();
					if (!_threads.containsKey(key))
					{
						Thread fakeThread = new Thread(key, "Cloud Data", "This thread was saved to the cloud from another device and will load shortly.", 0L, 1, "ontopic", false, true);
						SavedThreadObj value = new SavedThreadObj(key, 0L, fakeThread.getJson());
						// add nonexistent
						_threads.put(key, value);
						getDataList.add(key);
					}
					if (getDataList.size() > 0)
						new UpdateCloudThread().execute(getDataList);
				}
	            
	            // clean watched jsonarray
	            watched = new JSONArray();
	            
	            // rebuild watched jsonarray from thread keys
	            Enumeration keys = _threads.keys();
	            while( keys.hasMoreElements() ) {
					Object key = keys.nextElement();
					watched.put(key);
	            }
				
				cloudJson.put("watched",watched);
				String result = ShackApi.putCloudPinned(cloudJson, getCloudUsername());
				if ((_verbose) && (result != null)) _verboseMsg = "Sync Success" + watched.length() + " items";
					
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				verboseError();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				verboseError();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				verboseError();
			}
			
			return watched;
		}
		@Override
		protected void onPostExecute(JSONArray result)
		{
			if (_verbose) Toast.makeText(_activity, _verboseMsg, Toast.LENGTH_SHORT).show();
			_verbose = false;
		}
 	}
 	class UpdateCloudThread extends AsyncTask <ArrayList<Integer>, Void, Void>
 	{
		@Override
		protected Void doInBackground(ArrayList<Integer>... params) {
			System.out.println("OFFLINETHREAD: starting load of cloud content for posts #: " + params[0].size());
			
			try {
				SparseIntArray reply_counts = ShackApi.getReplyCounts(params[0], _activity);
			
			
				// first loop updates content
				for (int key: params[0])
				{
					System.out.println("OFFLINETHREAD: downloading cloud content for post " + key);
					ArrayList<Post> posts = new ArrayList<Post>();
					Post root = new Post(0, "Cloud Error", "Error retrieving data for empty thread", 0L, 1, "ontopic", false);
					try {
						posts = ShackApi.processPosts(ShackApi.getRootPost(key, _activity), key, 50, null);
					
						if (posts != null && posts.size() > 0)
						{
							root = posts.get(0);
							
							// sanity check
							if (_threads.containsKey(root.getPostId()))
							{
								SavedThreadObj value = _threads.get(root.getPostId());
								Thread fakeThread = new Thread(root.getPostId(), root.getUserName(), root.getContent(), root.getPosted(), reply_counts.get(root.getPostId()), root.getModeration(), value._threadJson.getBoolean("replied"), true);
								value = new SavedThreadObj(root.getPostId(), root.getPosted(), fakeThread.getJson());
								_threads.put(root.getPostId(), value);
								
								_activity.runOnUiThread(new Runnable(){
					        		@Override public void run()
					        		{
					        			((MainActivity)_activity).mOfflineThreadsNotifyAdapter();
					        		}
								});
							}
						}
					} catch (ClientProtocolException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			flushThreadsToDiskTask();
			((MainActivity)_activity).mRefreshOfflineThreads();
			return null;
		}
 	
 	}

    public String getCloudUsername()
    {
        return ((MainActivity)_activity).getCloudUsername();
    }
 	
 	Runnable cloudUpdater = new Runnable()
 	{
 	     @Override 
 	     public void run() {
 	    	
 	    	 boolean cloudEnabled = (((MainActivity)_activity)._prefs.getBoolean("usernameVerified", false) && ((MainActivity)_activity)._prefs.getBoolean("enableCloudSync", true));
 	    	 if (cloudEnabled)
 	    	 {
	 	    	 triggerCloudToLocal(); //this function can change value of m_interval.
	 	    	 Integer cloudInterval = ((MainActivity)_activity)._prefs.getInt("cloudInterval", 300);
	 	         cloudUpdateHandler.postDelayed(cloudUpdater, (1000 * cloudInterval));
	 	         System.out.println("OFFLINETHREAD: auto update cloud timer");
 	    	 }
 	     }
 	};
	
 	public void startCloudUpdates()
 	{
 		cloudUpdater.run();
 		System.out.println("OFFLINETHREAD: auto update cloud timer started");
 	}
 	public void endCloudUpdates()
 	{
 		if (cloudUpdateHandler != null)
 			cloudUpdateHandler.removeCallbacks(cloudUpdater);
 	}
	public void setVerboseNext() {
		_verbose = true;
	}
	public void verboseError() {
		_verboseMsg = "Sync Error";
	}
}