package net.woggle.shackbrowse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import net.woggle.CheckableLinearLayout;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.app.ListFragment;
import android.text.Editable;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

import com.afollestad.materialdialogs.MaterialDialog;

/* TO DO
 * widget for tagged threads               
*        
 */
public class MessageFragment extends ListFragment
{
    MessageLoadingAdapter _adapter;

    // list view saved state while rotating
    private Parcelable _listState = null;
    private int _listPosition = 0;
    private int _itemPosition = 0;
    private int _itemChecked = ListView.INVALID_POSITION;
    
    private boolean _viewAvailable = false;
    private boolean _silentLoad = false;

	private boolean _getInbox = true;

	protected MaterialDialog _progressDialog;

	private SharedPreferences _prefs;

	private View _header;
    
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
    
    public void instantiateAdapter()
    {
    	// no adapter? must be a new view
   		_adapter = new MessageLoadingAdapter(getActivity(), new ArrayList<Message>());
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
				refreshMessages();
				
			}});
       	
       	initAutoLoader();
    }

	public void initAutoLoader ()
    {        
    	// set listview so it loads more when you hit 3/4 the way down
        getListView().setOnScrollListener(new OnScrollListener() {

			@Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				// Check if the last view is visible
				if (((MainActivity)getActivity()).isMessagesShowing())
				{
					if (++firstVisibleItem + visibleItemCount > (int)(totalItemCount * .9)) {
						if (
								(!_adapter.isAsyncTaskLoading()) 
								&&
								(_adapter.wasLastCallSuccessful())
							)
						{
						    // if so, download more content
							if (_adapter.getCount() > 0) 
								_silentLoad = true;
							else 
								_silentLoad = false;
							_adapter.triggerLoadMore();
						}
					}
				}
            }

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
				
			}
        });
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
        
    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        openMessageView(position);
    }
    
    void refreshMessages()
    {
    	_silentLoad = false;
        getListView().clearChoices();
        _adapter.clear();
        
        if (_adapter.updatePrefs())
        {
        	//getListView().invalidateViews();
        	System.out.println("zoom or other pref changed, redraw listview");
        	getListView().invalidate();
        	_adapter.notifyDataSetChanged();
        	
        }
    }
    
    public void promptRecipient()
    {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    	boolean verified = prefs.getBoolean("usernameVerified", false);
        if (!verified)
        {
        	LoginForm login = new LoginForm(getActivity());
        	login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {
				@Override
				public void onSuccess() {
					promptRecipient();
				}

				@Override
				public void onFailure() {
				}
			});
        	return;
        }
	    AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle("New Shackmessage");
        LinearLayout layout = new LinearLayout(getActivity());
        layout.setPadding(5,5,5,5);
        TextView tv = new TextView(getActivity());
        tv.setText("To:");
        int padding_in_dp = 5;  // 6 dps
        final float scale = getResources().getDisplayMetrics().density;
        int padding_in_px = (int) (padding_in_dp * scale + 0.5f);
        tv.setPadding(padding_in_px, padding_in_px, padding_in_px, padding_in_px);
        tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        layout.addView(tv);
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        layout.addView(input);
        alert.setView(layout);
        alert.setPositiveButton("Next", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Editable value = input.getText();
                _progressDialog = MaterialProgressDialog.show(getActivity(), "Please wait", "Checking that username exists...");
                new CheckUsernameTask().execute(value.toString());
            }});
        alert.setNegativeButton("Cancel", null);
        alert.show().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        input.requestFocus();
    }
    
    
    class CheckUsernameTask extends AsyncTask<String, Void, String>
    {
        Exception _exception;
        
        @Override
        protected String doInBackground(String... params)
        {
            String userName = params[0];
            try {
            	if (ShackApi.usernameExists(userName,getActivity()))
            	{
            		return userName;
            	}
            	
            }
            catch (Exception ex) { Log.e("shackbrowse", "Error checking username exists", ex); _exception = ex; }
            return null;
        }
        @Override
        protected void onPostExecute(String result)
        {
            _progressDialog.dismiss();
            if (result != null)
            	((MainActivity)getActivity()).openNewMessagePromptForSubject(result);
            else
            {
                MaterialDialog.Builder alert = new MaterialDialog.Builder(getActivity());
                alert.content("Could not find a user by that name")
                .title("Not Found")
                .positiveText("Sorry")
                        .show();
            }
            if (_exception != null)
               ErrorDialog.display(getActivity(), "Error", "Error checking username exists:\n" + _exception.getMessage()); 
        }
    }
    
    class MarkReadTask extends AsyncTask<String, Void, Boolean>
    {
        Exception _exception;
        
        @Override
        protected Boolean doInBackground(String... params)
        {
            String mid = params[0];
            try {
            	return  ShackApi.markRead(mid,getActivity());
            	
            	
            }
            catch (Exception ex) { Log.e("shackbrowse", "Error checking username exists", ex); _exception = ex; }
            return false;
        }
        @Override
        protected void onPostExecute(Boolean result)
        {
            
            if (!result)
            	ErrorDialog.display(getActivity(), "Error", "Could not mark as read"); 
            if (_exception != null)
               ErrorDialog.display(getActivity(), "Error", "Error checking username exists:\n" + _exception.getMessage()); 
        }
    }
    
    public static final int POST_MESSAGE = 879;
    public static final int OPEN_THREAD_VIEW = 2090;
    
    private void showSettings()
    {
    	((MainActivity)getActivity()).toggleMenu();
    }
    
    void openMessageView(int index)
    {
        Message msg = _adapter.getItem(index);
        if (!msg.getRead())
        {
	        msg.setRead(true);
	        new MarkReadTask().execute(Integer.toString(msg.getMessageId()));
        }
        
        // probably clicked the "Loading..." or something
        if (msg == null)
            return;
        
        _itemChecked = index;
        _adapter.notifyDataSetChanged();
        getListView().setItemChecked(index, true);
        
        
        
        
        ((MainActivity)getActivity()).openMessageView(msg.getMessageId(), msg);
    }
    
    protected class MessageLoadingAdapter extends LoadingAdapter<Message>
    {
    	protected HashSet<Integer> _messageIds = new HashSet<Integer>();
        protected int _pageNumber = 0;
        private Boolean _showShackTags;
        private Boolean _stripNewLines;
        private int _previewLines;
        
        float _zoom = 1.0f;
        boolean _showOntopic = true;
		private boolean _showHoursSince = true;
        
        
        public MessageLoadingAdapter(Context context, ArrayList<Message> items)
        {
            super(context, items);
            setShowTags();
            
            // prefs
            _zoom = Float.parseFloat(_prefs.getString("fontZoom", "1.0"));
            _showOntopic = _prefs.getBoolean("showOntopic", true);
            _showHoursSince = _prefs.getBoolean("showHoursSince", true);
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {       
            if (convertView == null)
                convertView = _inflater.inflate(R.layout.message_row, null);
            
            return createView(position, convertView, parent);
        }
        
        @Override
        public void clear()
        {
            _pageNumber = 0;
            _messageIds.clear();
            setShowTags();
            super.clear();
            
            System.out.println("MSG REFRESH " + _adapter.wasLastCallSuccessful() + _adapter.isCurrentlyLoading());
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
            
            return changed;
        }
        void setShowTags()
        {
            Activity activity = MessageFragment.this.getActivity();
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
                holder.container = (CheckableLinearLayout)convertView.findViewById(R.id.messageContainer);
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
            Message m = getItem(position);
            
            // make sure item exists
            if (m == null)
            {
            	holder.container.setVisibility(View.GONE);
            	return convertView;
            }
            else
            	holder.container.setVisibility(View.VISIBLE);
            
            
       
            
            holder.userName.setText(m.getUserName());
            holder.content.setMaxLines(_previewLines);
            holder.content.setLinkTextColor(getResources().getColor(R.color.linkColor));
            // holder.content.setText(m.getPreview(_showShackTags, _stripNewLines));
            holder.content.setText(m.getSubject());
            
            final double threadAge = TimeDisplay.threadAgeInHours(m.getPosted());
            // threadage > 8760 == one year. optimization to prevent getyear from being run on every thread
        	if (threadAge > 8760f && !TimeDisplay.getYear(TimeDisplay.now()).equals(TimeDisplay.getYear(m.getPosted())))
        		holder.posted.setText(TimeDisplay.convTime(m.getPosted(), "MMM dd, yyyy h:mma zzz"));
        	else
        	{
	            if ((!_showHoursSince) || (threadAge > 24f))
	            {
	            	if (threadAge > 96f)
	            		holder.posted.setText(TimeDisplay.convertTimeLong(m.getPosted()));
	            	else
	            		holder.posted.setText(TimeDisplay.convertTime(m.getPosted()));
	            }
	            else
	            	holder.posted.setText(TimeDisplay.doubleThreadAgeToString(threadAge));
        	}
          
            // special highlight for employee and mod names
            if (User.isEmployee(m.getUserName()))
    		{
    			holder.userName.setTextColor(getResources().getColor(R.color.emplUserName));
    		}
    		else if (User.isModerator(m.getUserName()))
    		{
    			holder.userName.setTextColor(getResources().getColor(R.color.modUserName));
    		}
    		else
    		{
    			holder.userName.setTextColor(getResources().getColor(R.color.userName));
    		}
            
            if (m.getRead())
            	holder.container.setNew(false);
            else
            	holder.container.setNew(true);
            
            return convertView;
        }
        
        boolean _wasLastThreadGetSuccessful = false;
        public ArrayList<Message> getMessageData() throws ClientProtocolException, IOException, JSONException
        {
            System.out.println("MSG start");
            ArrayList<Message> new_messages = new ArrayList<Message>();
			try {
				new_messages = ShackApi.getMessages(_pageNumber + 1, this.getContext(), ((MainActivity)getActivity()).getMessageType());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
                System.out.println("MSG Error getting msg!");
			}
            
             _pageNumber++;

            System.out.println("MSG removing already displayed");
             // remove threads already displayed
             if (new_messages != null && new_messages.size() > 0)
             {
	             Iterator<Message> iter = new_messages.iterator();
	             while (iter.hasNext())
	             	if (!_messageIds.add(iter.next().getMessageId()))
	             		iter.remove();
             }
             else
             {
                 System.out.println("MSG STLCS false");
            	 _adapter.setLastCallSuccessful(false);
             }
            System.out.println("MSG RETURN!");
             return new_messages;
        }
        
        @Override
        protected ArrayList<Message> loadData() throws Exception
        {
            System.out.println("MSG loaddata");
            // grab threads from the api
            return getMessageData();
        }
        @Override
        protected void afterDisplay()
        {
            System.out.println("MSG afterdisplay");
        	if (!_silentLoad) {
                System.out.println("MSG silentload");
                _silentLoad = true;
            }
        	
        	// pull to refresh integration
            System.out.println("MSG refreshcomplete");
        	if (getActivity() != null){
                System.out.println("MSG refresh setting now");
                ((MainActivity)getActivity()).getRefresher().setRefreshComplete();
            }

        	
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

