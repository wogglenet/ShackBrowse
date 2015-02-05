package net.woggle.shackbrowse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public abstract class LoadingAdapter<T> extends ArrayAdapter<T>
{
    protected abstract View createView(int position, View convertView, ViewGroup parent);
    protected abstract ArrayList<T> loadData() throws Exception;
    protected void afterDisplay() { }
    
    private boolean _isLoading;
    private boolean _wasLastCallSuccessful;
    protected UUID _uniqueId;
    private boolean _verbose = true;
    
    LayoutInflater _inflater;
	private boolean _clearBeforeAddOnPostExecute = false;
	public ArrayList<T> _itemList;
	AsyncTask<UUID, Void, ArrayList<T>> mTask;
    
    public LoadingAdapter(Context context, List<T> objects)
    {
        super(context, 0, 0, objects);
        _inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        _uniqueId = UUID.randomUUID();
        _itemList = new ArrayList<T>();
        setCurrentlyLoading(false);
        setLastCallSuccessful(true);
        if (_verbose) System.out.println("LOADINGADAPTER: instantiated. uuid:" + _uniqueId);
    }
    
    @SuppressLint("NewApi")
	public void triggerLoadMore()
    {
    	if (_uniqueId == null)
    		if (_verbose) System.out.println("LOADINGADAPTER: null uuid. should never happen");
    	if ((mTask == null) || (mTask.getStatus() == AsyncTask.Status.FINISHED) || (mTask.isCancelled()))
    	{
    		if (_verbose) System.out.println("LOADINGADAPTER: triggered load. uuid:" + _uniqueId);
    		mTask = new LoadAndAppendTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, _uniqueId);
    	}
    }
    
    public void cancel()
    {
    	if (mTask != null)
    	{
    		mTask.cancel(true);
    		mTask = null;
    	}
    	setCurrentlyLoading(false);
    	setLastCallSuccessful(false);
    }
    
    public boolean wasLastCallSuccessful()
    {
    	return _wasLastCallSuccessful;
    }
    public void setLastCallSuccessful(boolean set)
    {
    	_wasLastCallSuccessful = set;
    }
    
    @Override
    public void clear()
    {
        super.clear();
        _itemList.clear();
        setLastCallSuccessful(true);
        if (_verbose && isAsyncTaskLoading())
        	System.out.println("LOADINGADAPTER: Canceling "+_uniqueId);
        if (isAsyncTaskLoading()) 
        	mTask.cancel(true);
        _uniqueId = UUID.randomUUID();
    }
    
    protected boolean isCurrentlyLoading()
    {
        return _isLoading;
    }
    protected boolean isAsyncTaskLoading()
    {
        return (mTask != null && mTask.getStatus() != AsyncTask.Status.FINISHED);
    }
    protected void setCurrentlyLoading(boolean set)
    {
    	if (_verbose) System.out.println("LOADINGADAPTER: set currently loading to " + set + " uuid:" + _uniqueId);
        _isLoading = set;
    }
    
    protected void setClearBeforeAddOnPostExecute(boolean set)
    {
    	_clearBeforeAddOnPostExecute  = set;
    	System.out.println("LOADINGADAPTER: set clearbeforeoPE to "+set);
    }

    class LoadAndAppendTask extends AsyncTask<UUID, Void, ArrayList<T>>
    {
        Exception _exception;
        UUID _loadingId;
        
        @Override
        protected ArrayList<T> doInBackground(UUID... arg0)
        {
        	if ((!isCurrentlyLoading()) || (!_uniqueId.equals(_loadingId)))
        	{
        		setCurrentlyLoading(true);
	            _loadingId = arg0[0];
	            
	            if (_verbose) System.out.println("LOADINGADAPTER: LOADING new task " + _loadingId);
	            try
	            {
	                return loadData();
	            }
	            catch (Exception e)
	            {
	                Log.e("shackbrowse", "Error loading data.", e);
	                _exception = e;
	            }
        	}
        	else
        		if (_verbose) System.out.println("LOADINGADAPTER: already loading."  + _uniqueId + _isLoading + isCurrentlyLoading());
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<T> result)
        {
        	if (_verbose) System.out.println("LOADINGADAPTER: onpostexecute "  + _uniqueId + _isLoading + isCurrentlyLoading());
            
        	// user did something to invalidate the previous request
            if (!_loadingId.equals(_uniqueId))
            {
                Log.i("shackbrowse", "Stale unique ID, discarding loaded results.");
                setCurrentlyLoading(false);
                return;
            }
            
        	setCurrentlyLoading(false);
            
            if (_exception != null)
            {
            	_wasLastCallSuccessful = false;
               ErrorDialog.display(getContext(), "Error", "Error loading data."); 
            }
            else if (result != null)
            {
            	if (_verbose) System.out.println("LOADINGADAPTER: FINISHED loading " + _loadingId + " results: " + result.size());
            	if (_clearBeforeAddOnPostExecute)
            	{
            		clear();
            		if (_verbose) System.out.println("LOADINGADAPTER: cleared before onpostexecute!");
            		setClearBeforeAddOnPostExecute(false);
            	}
            	if (result.size() > 0)
                {
                   	for (T item : result)
                   	{
                        add(item);
                        _itemList.add(item);
                   	}
                }
                else
                {
                	_wasLastCallSuccessful = false;
                }
            	
            	// dataset changed, either there are new items, or the count went down (no more "Loading")
                notifyDataSetChanged();
                
                // if there wasn't an error, run the after process
                afterDisplay();
            }
        }
    }
    
}
