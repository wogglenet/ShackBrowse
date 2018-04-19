package net.woggle.shackbrowse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import net.woggle.CheckableLinearLayout;
import net.woggle.SwipeDismissListViewTouchListener;
import net.woggle.SwipeDismissListViewTouchListener.DismissCallbacks;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.app.ListFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

/* TO DO
 * widget for tagged threads               
*        
 */
public class NotificationFragment extends ListFragment
{
    NoteLoadingAdapter _adapter;

    // list view saved state while rotating
    private Parcelable _listState = null;
    private int _listPosition = 0;
    private int _itemPosition = 0;
    private int _itemChecked = ListView.INVALID_POSITION;
    
    private boolean _viewAvailable = false;
    private boolean _silentLoad = false;

	protected ProgressDialog _progressDialog;

	private SharedPreferences _prefs;

	public int _swipecollapse = 2;

	private SwipeDismissListViewTouchListener _touchListener;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	setRetainInstance(true);

        _prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }
    
    public View getParentView() { return getView(); }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {        
        _viewAvailable = true;
        return inflater.inflate(R.layout.message_list, null);
    }

    
    @Override
    public void onDestroyView()
    {
    	_viewAvailable = false;
    	super.onDestroyView();
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	refreshNotes();
    }
    
    public void instantiateAdapter()
    {
    	// no adapter? must be a new view
   		_adapter = new NoteLoadingAdapter(getActivity(), new ArrayList<NotificationObj>());
   		setListAdapter(_adapter);
    }
    
    public boolean getDualPane()
    {
    	View singleThread = getActivity().findViewById(R.id.singleThread);
       	return singleThread != null && singleThread.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
       
       	getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
       	// style things on the listview
       	// this.getListView().setDivider(getActivity().getResources().getDrawable(R.drawable.divider));
       	this.getListView().setDividerHeight(0);
       	// getListView().setBackgroundColor(getActivity().getResources().getColor(R.color.app_bg_color));
       	_swipecollapse = Integer.parseInt(_prefs.getString("swipeCollapse", "2"));
       	if (_adapter == null)
       	{
       		instantiateAdapter();
       	}
       	else
       	{
       		// user rotated the screen, try to go back to where they where
       		if (_listState != null)
       			getListView().onRestoreInstanceState(_listState);
       		getListView().setSelectionFromTop(_listPosition,  _itemPosition);
       		
       		if (_itemChecked != ListView.INVALID_POSITION)
       			getListView().setItemChecked(_itemChecked, true);
       	}
       	
        // pull to fresh integration
       	((MainActivity)getActivity()).getRefresher().addRefreshableView(getListView(), new PullToRefreshAttacher.OnRefreshListener(){

			@Override
			public void onRefreshStarted(View view) {
				refreshNotes();
				
			}});
       	// this will also fix the ontouchlistener which was setup by the PTR
       	initAutoLoader();
       	
       	// load
       	refreshNotes();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
    	super.onSaveInstanceState(outState);

    	// we should put this info into the outState, but the compatibility framework
    	// seems to swallow it somewhere
    	if (this.isVisible())
    	{
	    	ListView listView = getListView();
	    	_listState = listView.onSaveInstanceState();
	    	_listPosition = listView.getFirstVisiblePosition();
	    	View itemView = listView.getChildAt(0);
	    	_itemPosition = itemView == null ? 0 : itemView.getTop();
	    	_itemChecked = listView.getCheckedItemPosition();
    	}
    }
    
    public void initAutoLoader ()
    {
    	
    	_touchListener =
                new SwipeDismissListViewTouchListener(
                        getListView(),
                        new DismissCallbacks() {
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    deleteNotificationAtPosition(position);
                                }
                                _adapter.notifyDataSetChanged();
                            }

							@Override
							public boolean canDismiss(int position) {
								return true;
							}
                        });
        getListView().setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				((MainActivity)getActivity()).getRefresher().onTouch(v, event);
				if (_swipecollapse > 0)
					_touchListener.onTouch(v, event);
				return false;
			}
		});
        
        // swipe directional pref
        if (_swipecollapse == 1)
        {
        	_touchListener.setAllowRightSwipe(false);
        }
        else
        {
        	_touchListener.setAllowRightSwipe(true);
        }
        
    }
        
    protected void deleteNotificationAtPosition(int position) {
    	if (_adapter != null)
    	{
			NotificationObj n = _adapter.getItem(position);
			NotificationsDB ndb = new NotificationsDB(getActivity());
			ndb.open();
			ndb.deleteNote(n);
			ndb.close();
			_adapter.remove(n);
    	}
	}

	@Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
    	NotificationObj n = (NotificationObj) getListView().getItemAtPosition(position);
        ((MainActivity)getActivity()).openThreadViewAndSelect(n.getPostId());
    }
    
    void refreshNotes()
    {
    	_silentLoad = false;
        getListView().clearChoices();
        _adapter.clear();
        _adapter.triggerLoadMore();
        
        if (_adapter.updatePrefs())
        {
        	//getListView().invalidateViews();
        	System.out.println("zoom or other pref changed, redraw listview");
        	getListView().invalidate();
        	_adapter.notifyDataSetChanged();
        	
        }
    }
    
    protected class NoteLoadingAdapter extends LoadingAdapter<NotificationObj>
    {
    	protected HashSet<Integer> _noteIds = new HashSet<Integer>();
        protected int _pageNumber = 0;
        private Boolean _showShackTags;
        private Boolean _stripNewLines;
        private int _previewLines;
        
        float _zoom = 1.0f;
        boolean _showOntopic = true;
		private boolean _showHoursSince = true;
        
        
        public NoteLoadingAdapter(Context context, ArrayList<NotificationObj> items)
        {
            super(context, items);
            setShowTags();
            
            // prefs
            _zoom = Float.parseFloat(_prefs.getString("fontZoom", "1.0"));
            _showOntopic = _prefs.getBoolean("showOntopic", true);
            _showHoursSince = _prefs.getBoolean("showHoursSince", true);
            _swipecollapse = Integer.parseInt(_prefs.getString("swipeCollapse", "2"));
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {       
            if (convertView == null)
                convertView = _inflater.inflate(R.layout.notification_row, null);
            
            return createView(position, convertView, parent);
        }
        
        @Override
        public void clear()
        {
            _pageNumber = 0;
            _noteIds.clear();
            setShowTags();
            super.clear();
            
            System.out.println("NOTE REFRESH " + _adapter.wasLastCallSuccessful() + _adapter.isCurrentlyLoading());
        }
        
        @Override
        public void setCurrentlyLoading (final boolean set)
        {
        	super.setCurrentlyLoading(set);
        	if (getActivity() != null)
        	{
	        	if (set == true)
	        	{
	        		getActivity().runOnUiThread(new Runnable(){
	            		@Override public void run()
	            		{
	            			if (_viewAvailable)
	            			{
	            				if (!_silentLoad)
	            				{
		            				((View)getParentView()).findViewById(R.id.mlist_FSLoad).setVisibility(View.VISIBLE);
		            				getListView().setVisibility(View.GONE);
	            				}
	            				((MainActivity)getActivity()).showOnlyProgressBarFromPTRLibrary(set);
		        			}
	            		}
	            	});
	        	}
	        	else
	        	{
	        		getActivity().runOnUiThread(new Runnable(){
	            		@Override public void run()
	            		{
	            			if (_viewAvailable)
	            			{
	            				((View)getParentView()).findViewById(R.id.mlist_FSLoad).setVisibility(View.GONE);
	            				getListView().setVisibility(View.VISIBLE);
	            				
	            				((MainActivity)getActivity()).showOnlyProgressBarFromPTRLibrary(set);
	            			}
	            			
	            		}
	            	});
	        	}
        	}
        }
        
        public boolean updatePrefs()
        {
        	// prefs
        	boolean changed = false;
        	
            if (_zoom != Float.parseFloat(_prefs.getString("fontZoom", "1.0")))
            {
            	_zoom = Float.parseFloat(_prefs.getString("fontZoom", "1.0"));
            	changed = true;
            }
           
            if (_showOntopic != _prefs.getBoolean("showOntopic", true))
            {
            	_showOntopic = _prefs.getBoolean("showOntopic", true);
            	changed = true;
            }
            if (_showHoursSince != _prefs.getBoolean("showHoursSince", true))
            {
	            _showHoursSince = _prefs.getBoolean("showHoursSince", true);
	        	changed = true;
            }
            if (_swipecollapse != Integer.parseInt(_prefs.getString("swipeCollapse", "2")))
            {
            	_swipecollapse = Integer.parseInt(_prefs.getString("swipeCollapse", "2"));
            	initAutoLoader();
            	changed = true;
            }
            
            return changed;
        }
        void setShowTags()
        {
            Activity activity = NotificationFragment.this.getActivity();
            if (activity != null)
            {
                _showShackTags = _prefs.getBoolean("showShackTagsInThreadList", true);
                _stripNewLines = _prefs.getBoolean("previewStripNewLines", false);
                _previewLines = Integer.parseInt(_prefs.getString("previewLineCount", "7"));
            }
        }
        
        @Override
        protected View createView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder holder = (ViewHolder)convertView.getTag();
            if (holder == null)
            {
                holder = new ViewHolder();
                holder.container = (CheckableLinearLayout)convertView.findViewById(R.id.notificationContainer);
                holder.userName = (TextView)convertView.findViewById(R.id.textUserName);
                holder.content = (TextView)convertView.findViewById(R.id.textContent);
                holder.posted = (TextView)convertView.findViewById(R.id.textPostedTime);
                holder.defaultTimeColor = holder.posted.getTextColors().getDefaultColor();
                
                // zoom
                holder.content.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.content.getTextSize() * _zoom);
                holder.userName.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.userName.getTextSize() * _zoom);
                holder.posted.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.posted.getTextSize() * _zoom);
               
                convertView.setTag(holder);
            }
            
            // get the thread to display and populate all the data into the layout
            NotificationObj n = getItem(position);
            
            // make sure item exists
            if (n == null)
            {
            	holder.container.setVisibility(View.GONE);
            	return convertView;
            }
            else
            	holder.container.setVisibility(View.VISIBLE);
            
            
       
            String friendly = null;
            if (n.getType().equals("reply"))
            	friendly = " replied to your post";
            else if (n.getType().equals("keyword"))
            	friendly = " mentioned " + n.getKeyword();
            else if (n.getType().equals("vanity"))
            	friendly = " mentioned your name";
            holder.userName.setText(n.getAuthor() + friendly);
            holder.content.setMaxLines(_previewLines);
            holder.content.setLinkTextColor(getResources().getColor(R.color.linkColor));
            // holder.content.setText(m.getPreview(_showShackTags, _stripNewLines));
            holder.content.setText(PostFormatter.formatContent(n.getAuthor(), n.getBody(), null, false, true));

            /*
            final double threadAgeInHours = TimeDisplay.threadAgeInHours(n.getTime());
            // threadage > 8760 == one year. optimization to prevent getyear from being run on every thread
        	if (threadAgeInHours > 8760f && !TimeDisplay.getYear(TimeDisplay.now()).equals(TimeDisplay.getYear(n.getTime())))
        		holder.posted.setText(TimeDisplay.convTime(n.getTime(), "MMM dd, yyyy h:mma zzz"));
        	else
        	{
	            if ((!_showHoursSince) || (threadAgeInHours > 24f))
	            {
	            	if (threadAgeInHours > 96f)
	            		holder.posted.setText(TimeDisplay.convertTimeLong(n.getTime()));
	            	else
	            		holder.posted.setText(TimeDisplay.convertTime(n.getTime()));
	            }
	            else
	            	holder.posted.setText(TimeDisplay.doubleThreadAgeToString(threadAgeInHours));
        	}*/
            holder.posted.setText(TimeDisplay.getNiceTimeSince(n.getTime(), _showHoursSince));
          
            // special highlight for employee and mod names
            if (User.isEmployee(n.getAuthor()))
    		{
    			holder.userName.setTextColor(getResources().getColor(R.color.emplUserName));
    		}
    		else if (User.isModerator(n.getAuthor()))
    		{
    			holder.userName.setTextColor(getResources().getColor(R.color.modUserName));
    		}
    		else
    		{
    			holder.userName.setTextColor(getResources().getColor(R.color.userName));
    		}
            
            //if (m.getRead())
            	holder.container.setNew(false);
           // else
            //	holder.container.setNew(true);
            
            return convertView;
        }
        
        boolean _wasLastThreadGetSuccessful = false;
        public ArrayList<NotificationObj> getNoteData() throws ClientProtocolException, IOException, JSONException
        {
            ArrayList<NotificationObj> new_notes = new ArrayList<NotificationObj>();
			try {
				NotificationsDB ndb = new NotificationsDB(getActivity());
				ndb.open();
				new_notes = new ArrayList<NotificationObj>(ndb.getAllNotes());
				ndb.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

            _adapter.setLastCallSuccessful(true);
             
             // reset all counts
             Map<String, ?> allPrefs = _prefs.getAll();
             Editor edit = _prefs.edit();
             for (Map.Entry<String, ?> entry: allPrefs.entrySet()) {
                 if (entry.getKey().contains("GCMNoteCount")) {
                	 edit.putInt(entry.getKey(), 0);
                 }
             }
             edit.commit();
             
             
             return new_notes;
        }
        
        @Override
        protected ArrayList<NotificationObj> loadData() throws Exception
        {
            // grab threads from the api
            return getNoteData();
        }
        @Override
        protected void afterDisplay()
        {
        	if (!_silentLoad)
        		_silentLoad = true;
        	
        	// pull to refresh integration
        	if (getActivity() != null)
        		((MainActivity)getActivity()).getRefresher().setRefreshComplete();
        	
        }
        private class ViewHolder
        {
			public CheckableLinearLayout container;
            TextView userName;
            TextView content;
            TextView posted;
            int defaultTimeColor;
        }
    }

    public void adjustSelected(int movement)
    {
    	if (_viewAvailable)
    	{
	        int index = getListView().getCheckedItemPosition() + movement;
	        if (index >= 0 && index < getListView().getCount())
	        {
	        	getListView().setItemChecked(index, true);
	            ensureVisible(index, 0);
	        }
    	}
    }
    
    void ensureVisible(int position, int minPos)
    {
    	ListView view = getListView();

        
        if (position < minPos || position >= view.getCount())
            return;
        
        int first = view.getFirstVisiblePosition();
        int last = view.getLastVisiblePosition();
        int destination = 0;
        
        if (position < first)
            destination = position;
        else if (position >= last)
            destination = (position - (last - first));
        
        if ((position < first) || (position >= last))
        {
        	view.setSelection(destination);
        }
        
        view.smoothScrollToPosition(position);
    }
}

