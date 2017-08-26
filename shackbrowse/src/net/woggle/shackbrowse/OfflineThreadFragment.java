package net.woggle.shackbrowse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;



public class OfflineThreadFragment extends ThreadListFragment {
    OfflineThread _offlineThread;
    boolean _doNotGetReplies = false;
	protected ProgressDialog _progressDialog;
    
	@Override
	public void instantiateAdapter()
	{
		// no adapter? must be a new view
		_adapter = new OfflineThreadLoadingAdapter(getActivity(), new ArrayList<Thread>());
		setListAdapter(_adapter);
		_firstLoad = false;
       	_adapter.triggerLoadMore();
	}
	
	@Override
	public void initAutoLoader()
	{
		_offlineThread = ((MainActivity)getActivity()).mOffline;
	}
	
	void clearAllOfflineThreads()
	{
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setTitle("Clear Saved Threads");
        builder.setMessage("Are you sure you want to delete all threads?");
        builder.setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            	((MainActivity)getActivity()).mOffline.deleteAllThreadsTask();
            	updateThreadsWithoutUpdateReplies();
            }
        });
        builder.setNeutralButton("Only Expired", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            	((MainActivity)getActivity()).mOffline.deleteExpiredThreadsTask();
            	updateThreadsWithoutUpdateReplies();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
	}
	
	
	
    void refreshOfflineThreads()
    {
    	System.out.println("OFTVIEW: RELOAD ");
        getListView().clearChoices();
        if (_adapter != null)
        {
	        if (_adapter.updatePrefs())
	        {
	        	//getListView().invalidateViews();
	        	System.out.println("zoom or other pref changed, redraw listview");
	        	_adapter.notifyDataSetChanged();
	        }
	        
	        _adapter.triggerLoadMore();
        }
    }
    public void updateThreadsWithoutUpdateReplies()
    {
    	if (_adapter != null)
    	{
	    	_doNotGetReplies = true;
	    	_adapter.triggerLoadMore();
    	}
    }
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {       
    	View layout = super.onCreateView(inflater, container, savedInstanceState);
        return layout; 
    }
    
	
	private class OfflineThreadLoadingAdapter extends ThreadListFragment.ThreadLoadingAdapter
	{
	
		public OfflineThreadLoadingAdapter(Context context, ArrayList<Thread> items) {
			super(context, items);
			// TODO Auto-generated constructor stub
		}

		@Override
        public ArrayList<Thread> getThreadData() throws ClientProtocolException, IOException, JSONException
        {
			// seamless update
			setClearBeforeAddOnPostExecute(true);
			
        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(OfflineThreadFragment.this.getActivity());
            String userName = prefs.getString("userName", "");
        	ArrayList<Thread> new_threads = new ArrayList<Thread>();
			try {
				if (_doNotGetReplies)
				{
					new_threads = ShackApi.processThreads(_offlineThread.getThreadsAsJson(), true, getActivity());
					_doNotGetReplies = false;
				}
				else
				{
					new_threads = ShackApi.processThreadsAndUpdReplyCounts(_offlineThread.getThreadsAsJson(), getActivity());
					
		             Iterator<Thread> iter = new_threads.iterator();
		             while (iter.hasNext())
		             {
		             	Thread t = iter.next();
		                _offlineThread.updateRecordedReplyCount(t.getThreadId(), t.getReplyCount());
		             }	
		             _offlineThread.flushThreadsToDiskTask();
					
					// only a full refresh triggers shacklol
		        	 _shackloldata = ShackApi.getLols(getActivity());
		             if (_shackloldata.size() == 0)
		             {
		             	// no lol data
		             }
				}
	             
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	         _lastThreadGetTime = System.currentTimeMillis();
             return new_threads;
        }
	
	}
}
