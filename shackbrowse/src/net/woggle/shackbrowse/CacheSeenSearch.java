package net.woggle.shackbrowse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import net.woggle.shackbrowse.SearchResultFragment.SearchResultsAdapter;

import android.app.Activity;

// LAST SEEN MARKERS
// seen posts
public class CacheSeenSearch 
{
	private static final int SEEN_HISTORY = 100;
	private String SEEN_FILE = "seensearchdb.cache";
	public Hashtable<Integer, Integer> _seenTable = null;
	private Activity _activity;
	private String _title;
	public CacheSeenSearch (Activity activity)
	{
		_activity = activity;
		_seenTable = load();
	}
	public ArrayList<SearchResult> process(ArrayList<SearchResult> results, int pageNumber, SearchResultsAdapter adapter, String searchTitle) {
		_title = searchTitle;
		
		// DO THINGS FOR SEEN POSTS
		if (!_seenTable.containsKey(getCurrentSearchHash()) && (results.size() > 0))
		{
			// this search has not yet been run, and there are results. this will only happen on page 0
			_seenTable.put(getCurrentSearchHash(), results.get(0).getPostId());
			store();
		}
		else if (results.size() > 0)
		{
			int lastSeen = _seenTable.get(getCurrentSearchHash());
			// search has been run, there are results
			if (pageNumber == 0)
			{
				// mark all as new until old found
				for (int i = 0; i < results.size(); i++)
	    		{
					if (results.get(i).getPostId() != lastSeen)
						results.get(i).setNew(true);
					else
						break;
	    		}
			}
			else
			{
				// next page, check if last item in adapter is new. if it is not marked as new, we already found the transition point
				if (adapter.getItem(adapter.getCount() -1) != null && adapter.getItem(adapter.getCount() -1).getNew())
				{
					// mark all as new until old found
	    			for (int i = 0; i < results.size(); i++)
	        		{
	    				if (results.get(i).getPostId() != lastSeen)
	    					results.get(i).setNew(true);
	    				else
	    					break;
	        		}
				}
			}
			
			if (pageNumber == 0)
			{
				// mark top as seen
				_seenTable.put(getCurrentSearchHash(), results.get(0).getPostId());
				store();
			}
		}
		return results;
		
	}
	public int getCurrentSearchHash()
	{
		return _title.hashCode();
	}
	public void setTitle(String set)
	{
		_title = set;
	}
	protected Hashtable<Integer, Integer> getTable()
	{
		return _seenTable;
	}
    protected Hashtable<Integer, Integer> load()
    {
        Hashtable<Integer, Integer> counts = new Hashtable<Integer, Integer>();

        if (_activity.getFileStreamPath(SEEN_FILE).exists())
        {
            // look at that, we got a file
            try {
                FileInputStream input = _activity.openFileInput(SEEN_FILE);
                try
                {
                    DataInputStream in = new DataInputStream(input);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line = reader.readLine();
                    while (line != null)
                    {
                        if (line.length() > 0)
                        {
                        	if (line.contains("="))
                        	{
	                            String[] parts = line.split("=");
	                            if (parts.length > 0)
	                            {
	                            	try
	                            	{
	                            		counts.put(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
	                            	}
	                            	catch (NumberFormatException e)
	                            	{ }
	                            }
                        	}
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

        return counts;
    }
    void store()
    {
        List<Integer> searchIds = Collections.list(_seenTable.keys());

        FileOutputStream output;
		try {
			output = _activity.openFileOutput(SEEN_FILE, Activity.MODE_PRIVATE);
		
	        try
	        {
	            DataOutputStream out = new DataOutputStream(output);
	            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
	
	            for (Integer searchId : searchIds)
	            {
	            	writer.write(searchId + "=" + _seenTable.get(searchId));
	                writer.newLine();
	            }
	            writer.flush();
	        } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        finally
	        {
	            try {
					output.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}