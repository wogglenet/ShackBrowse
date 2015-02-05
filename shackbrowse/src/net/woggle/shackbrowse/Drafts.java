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
import java.util.regex.Pattern;

import android.app.Activity;

public class Drafts {
	protected String DRAFTSFILE_NAME = "saveddraftsV3.cache";
	protected String FIRST_TOKEN = "<($#)*@)!@%$$%#!;[]_-;'d{{~`>";
	protected String SECOND_TOKEN = "<(!*@($*)@@!&(fdsa+_#$:><!%$`>";
	int MAX_DRAFTS_TO_SAVE = 75;
	private Activity _activity;
	
	Hashtable<Integer,String> _drafts = null;
	
	Drafts (Activity activity) {
		_activity = activity;
		loadDraftsFromDisk();
	}
	public void deleteDraftById(int id)
	{
		if (_drafts != null)
		{
			_drafts.remove(id);
			saveDraftsToDisk();
		}
	}
	public void saveThisDraft (Integer replyToPostId, String draftText, String parentAuthor, String parentPost, long posted)
	{		
		if (_drafts == null)
		{
			_drafts = new Hashtable<Integer,String>();
		}
		
		// trimming
		if (_drafts.size() > MAX_DRAFTS_TO_SAVE)
		{
			// trim down
			List<Integer> postIds = Collections.list(_drafts.keys());
	        Collections.sort(postIds);
	        
	        List<Integer> postIdsToRemove = postIds.subList(0, postIds.size() - (1 + MAX_DRAFTS_TO_SAVE));
	        for (int postId : postIdsToRemove)
	        {
	        	_drafts.remove(postId);
	        }
		}
		String truncatedParentPost = parentPost;
		//if (truncatedParentPost.length() > 120)
		//	truncatedParentPost = truncatedParentPost.substring(0, 120);
		
		_drafts.put(replyToPostId, draftText + SECOND_TOKEN + parentAuthor + SECOND_TOKEN + Long.toString(posted) + SECOND_TOKEN + truncatedParentPost);
		saveDraftsToDisk();
	}
	
	public void loadDraftsFromDisk()
	{
		_drafts = new Hashtable<Integer,String>();
		 if ((_activity != null) && (_activity.getFileStreamPath(DRAFTSFILE_NAME).exists()))
	     {
	         // look at that, we got a file
	         try {
	             FileInputStream input = _activity.openFileInput(DRAFTSFILE_NAME);
	             try
	             {
	                 DataInputStream in = new DataInputStream(input);
	                 BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	                 String line = reader.readLine();
	                 int i = 0;
	                 while (line != null)
	                 {
	                     if (line.length() > 0)
	                     {
	                    	 if ((line.indexOf(FIRST_TOKEN) > -1) )
	                    	 {
	                    		String[] draftbits = line.split(Pattern.quote(FIRST_TOKEN));
	                    		String parentPostId = draftbits[0];
	                    		String draft = "";
	                    		if (draftbits.length > 1)
	                    			draft = draftbits[1];
	                    		i++;
	                    		_drafts.put(Integer.valueOf(parentPostId), draft);
	                    	 }
	                     }
	                     line = reader.readLine();
	                 }
	             } catch (NumberFormatException e) {
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
	}
	public boolean saveDraftsToDisk()
	{
 		boolean result = true;
        try
        {
        	FileOutputStream _output = _activity.openFileOutput(DRAFTSFILE_NAME, Activity.MODE_PRIVATE);
	        try
	        {
	            DataOutputStream out = new DataOutputStream(_output);
	            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
	            // clear file
	            writer.write("");
	            int i = 0;
	             
	            Enumeration keys = _drafts.keys();
	            while(keys.hasMoreElements()) {
					int key = (Integer)keys.nextElement();
					String value = _drafts.get(key);
					i++;
		            // write line
		            writer.write(Integer.toString(key) + FIRST_TOKEN + value);
		            writer.newLine();
	            }
	            writer.flush();
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
	public ArrayList<SearchResult> getDraftListAsSearchResults()
	{
		ArrayList<SearchResult> results = new ArrayList<SearchResult>();
		
		Enumeration<Integer> keys = _drafts.keys();
		while (keys.hasMoreElements())
		{
			int key = keys.nextElement();
			String value = _drafts.get(key);
			String[] draftbits = value.split(Pattern.quote(SECOND_TOKEN));
			
			if (draftbits.length > 3)
				results.add(new SearchResult(key, draftbits[1], draftbits[3], Long.parseLong(draftbits[2]), SearchResult.TYPE_DRAFTS, 0));
		}
		return results;
	}
}
