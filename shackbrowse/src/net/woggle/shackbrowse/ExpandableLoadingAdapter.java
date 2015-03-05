package net.woggle.shackbrowse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.woggle.ExpandableListItemAdapter;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;

public abstract class ExpandableLoadingAdapter<T> extends ExpandableListItemAdapter<T>
{
    protected abstract ArrayList<T> loadData() throws Exception;
    protected void afterDisplay() { }
	public void afterLoad() { }
    
    private boolean _isLoading;
    private boolean _wasLastCallSuccessful;
    protected UUID _uniqueId;
    private boolean _verbose = true;
    
    LayoutInflater _inflater;
	private boolean _clearBeforeAddOnPostExecute = false;
	AsyncTask<UUID, Void, ArrayList<T>> mTask;
	private Context _context;
    
    public ExpandableLoadingAdapter(Context context, List<T> objects)
    {
    	super(context, ExpandableListItemAdapter.createResIds(R.id.tview_threadrow_expanded_container, R.id.tview_threadrow_preview_container, R.layout.thread_row_container, R.id.textPreview, R.id.textPreviewUserName, R.id.textPostedTime, R.id.previewView, R.id.textPostLolCounts), objects);
        _uniqueId = UUID.randomUUID();
        _context = context;
        setCurrentlyLoading(false);
        setLastCallSuccessful(true);
        if (_verbose) System.out.println("LOADINGADAPTER: instantiated. uuid:" + _uniqueId);
    }
    
    public Context getContext()
    {
    	return _context;
    }

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
    /*
    public int getCount()
    {
    	return _itemList.size();
    }
    */
    public boolean add(T item)
    {
    	super.add(item);
		return true;
    }
    
    public void insert(T item, int pos)
    {
    	super.add(pos, item);
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
    
    public void clear()
    {
        super.clear();
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

    protected ArrayList<T> getAll()
    {
        return (ArrayList<T>) mItems;
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
                    beforeClear();
                    mItems.clear();
                    mItems.addAll(result);
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

    protected abstract void beforeClear();


}
