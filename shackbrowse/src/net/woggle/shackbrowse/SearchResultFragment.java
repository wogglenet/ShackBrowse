package net.woggle.shackbrowse;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.app.ListFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

import net.woggle.CheckableLinearLayout;

import java.util.ArrayList;

public class SearchResultFragment extends ListFragment
{
    ArrayList<SearchResult> _results;
    SearchResultsAdapter _adapter;
    
    String _term ="";
    String _author ="";
    String _parentAuthor="";
    
    // list view saved state while rotating
    private Parcelable _listState = null;
    private int _listPosition = 0;
    private int _itemPosition = 0;
    private int _itemChecked = ListView.INVALID_POSITION;
    
    int _pageNumber = 0;

    private int _mode = SearchResult.TYPE_SHACKSEARCHRESULT;
	private String _tag = "lol";
	private int _days = 1;
	private boolean _viewAvailable;
	private boolean _dualPane;
	private String _tagger;
	private String _category;
	String _title;
	CacheSeenSearch _seen;
	private Bundle _lastArgs;
	private View _header;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        
        // this.getListView().setDivider(getActivity().getResources().getDrawable(R.drawable.divider));
       	this.getListView().setDividerHeight(0);
       	// getListView().setBackgroundColor(getActivity().getResources().getColor(R.color.app_bg_color));
        
       	// seen
     	_seen = new CacheSeenSearch(getActivity());
     	
     		
       	if (_adapter == null)
        {
        	System.out.println("adapter reset searchresults");
        	// first launch, try to set everything up
            if (getArguments() != null)
            {
    	        Bundle args = new Bundle();
    	    	args = getArguments();
    	    	if (args.containsKey("terms"))
    	    	{
    	    		_term = args.getString("terms");
    	    	}
    	    	if (args.containsKey("author"))
    	    	{
    	    		_author = args.getString("author");
    	    	}
    	    	if (args.containsKey("parentAuthor"))
    	    	{
    	    		_parentAuthor = args.getString("parentAuthor");
    	    	}
            }
            initResultView(getActivity());
           	
        }
        else    
        {
       		// user rotated the screen, try to go back to where they where
       		if (_listState != null){
       			getListView().onRestoreInstanceState(_listState);
       		}
       			
       		getListView().setSelectionFromTop(_listPosition,  _itemPosition);
        }
       	
       	
       	getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
       	
       	//getListView().setBackgroundColor(getResources().getColor(R.color.app_bg_color));
        ListView lv = getListView();
        lv.setOnItemLongClickListener(new OnItemLongClickListener() {

            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
            	onListItemLongClick((ListView)arg0, arg1, pos, id);
                return true;
            }
        });
        lv.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				// Check if the last view is visible
				if (++firstVisibleItem + visibleItemCount > (int)(totalItemCount * .9)) {
					
					if ((!_adapter.isAsyncTaskLoading()) && (_adapter.wasLastCallSuccessful()))
					{
					    // if so, download more content
						System.out.println("THREADLISTFRAG: reached 3/4 down, loading more");
						_adapter.triggerLoadMore();
					}
				}
            }

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
				
			}
        });
        
        // pull to fresh integration
       	((MainActivity)getActivity()).getRefresher().addRefreshableView(getListView(), new PullToRefreshAttacher.OnRefreshListener(){

			@Override
			public void onRefreshStarted(View view) {
				retrySearch();
				
			}});
        /*
        ((View)getView()).findViewById(R.id.sres_close)
       	.setOnClickListener(new View.OnClickListener() {
    	    @Override
    	    public void onClick(View v) {
    	    	((MainActivity)getActivity())._sresFrame.closeLayer(true);
    	    }
    	});
        
        ((View)getView()).findViewById(R.id.tlist_preferences)
       	.setOnClickListener(new View.OnClickListener() {
    	    @Override
    	    public void onClick(View v) {
    	    	((MainActivity)getActivity()).toggleMenu();
    	    }
    	});
        */
        
    }

    protected void retrySearch() {
    	if (_mode > 0 && _lastArgs != null)
    	{
			if (_mode == SearchResult.TYPE_SHACKSEARCHRESULT)
				openSearch(_lastArgs, getActivity());
			if (_mode == SearchResult.TYPE_LOL)
				openSearchLOL(_lastArgs, getActivity());
			if (_mode == SearchResult.TYPE_DRAFTS)
				openSearchDrafts(getActivity());
    	}
	}
    
    protected void editSearch() {
    	if (_mode > 0 && _lastArgs != null)
    	{
			if ((_mode == SearchResult.TYPE_SHACKSEARCHRESULT) || (_mode == SearchResult.TYPE_LOL))
			{
				MainActivity mact = (MainActivity)getActivity();
				// change fragments and use arg bundle
				mact.setContentTo(MainActivity.CONTENT_SEARCHVIEW, _lastArgs);
				
			}
		}
	}

    
    public void onListItemLongClick(ListView l, View v, int position, long id)
    {
    	// hacky and awful
    	if (((MainActivity)getActivity() != null) && (((MainActivity)getActivity())._threadView != null))
    		((MainActivity)getActivity())._threadView.new GetTaggersTask().execute(_adapter.getItem(position).getPostId());
    }
    
	@Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
    	displayThread(_adapter.getItem(position));
    }
    public void showNoResults (boolean set)
    {
    	System.out.println("shownoresults " +set);
    	final boolean set2 = (set);
    	if (_viewAvailable)
    	{
    		if (getActivity () != null)
    		{
    			getActivity().runOnUiThread(new Runnable(){
        		@Override public void run()
        		{
        			if (_viewAvailable)
                	{
        				View lv = getListView();
        	        	if (lv != null)
        	        	{
    	((View)getListView().getParent()).findViewById(R.id.tlist_FSnoResults).setVisibility((set2) ? View.VISIBLE : View.GONE);
    	// getListView().setVisibility((set) ? View.VISIBLE : View.GONE);
        	        	}
                	}
        		}
    			});
    		}
    	}
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
    
    public void initResultView(Context context)
    {
       	_results = new ArrayList<SearchResult>();
        _adapter = new SearchResultsAdapter(context, _results);
        setListAdapter(_adapter);
        _adapter.setLastCallSuccessful(false);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {   
    	_viewAvailable = true;
    	
    	return inflater.inflate(R.layout.threadlist, null);
    }
    @Override
    public void onDestroyView()
    {
    	_viewAvailable = false;
    	super.onDestroyView();
    }
    
    public void openSearchLOL(Bundle args, Context context)
    {
    	_mode = SearchResult.TYPE_LOL;
    	_lastArgs = args;
    	if (args.containsKey("tag"))
    	{
    		_tag = args.getString("tag");
    	} else _tag = "lol";
    	if (args.containsKey("days"))
    	{
    		_days = args.getInt("days");
    	} else _days = 1;
    	if (args.containsKey("author"))
    	{
    		_author = args.getString("author");
    	} else _author = "";
    	if (args.containsKey("tagger"))
    	{
    		_tagger = args.getString("tagger");
    	} else _tagger = "";
    	
    	_pageNumber = 0;
    	
    	// this can happen with screwing around in viewpager or landscape mode
    	if (_adapter == null)
	    {
    		initResultView(context);
	    }
    	
    	showNoResults(false);
	    _adapter.clear();
	    _adapter.notifyDataSetInvalidated();
	    
	    if (args.containsKey("title"))
    	{
    		_title = args.getString("title");
    	} 
	    else 
    	{
	    	_title = "Search for" + (_tag.length() > 0 ? " tag: " + _tag : "") + (_days != 1 ? " days: " + _days : "") + (_author.length() > 0 ? " author: " + _author : "") + (_tagger.length() > 0 ? " tagger: " + _tagger : "");
	    }
    }
    
    public void openSearch(Bundle args, Context context)
    {
    	_mode = SearchResult.TYPE_SHACKSEARCHRESULT;
    	_lastArgs = args;
    	// first launch, try to set everything up
    	if (args.containsKey("terms"))
    	{
    		_term = args.getString("terms");
    	}
    	else _term = "";
    	if (args.containsKey("author"))
    	{
    		_author = args.getString("author");
    	}
    	else _author = "";
    	if (args.containsKey("category"))
    	{
    		_category = args.getString("category");
    	}
    	else _category = "";
    	if (args.containsKey("parentAuthor"))
    	{
    		_parentAuthor = args.getString("parentAuthor");
    	}
    	else _parentAuthor = "";
    	_pageNumber = 0;
    	
    	// this can happen with screwing around in viewpager or landscape mode
    	if (_adapter == null)
	    {
    		initResultView(context);
	    }
    	showNoResults(false);
    	_adapter.clear();
	    _adapter.notifyDataSetInvalidated();
	    
	    if (args.containsKey("title"))
    	{
    		_title = args.getString("title");
    	} 
	    else 
    	{
    		_title = "Search for" + (_term.length() > 0 ? " term: " + _term : "") + (_author.length() > 0 ? " author: " + _author : "") + (_parentAuthor.length() > 0 ? " parent author: " + _parentAuthor : "") + (_category.length() > 0 ? " category: " + _category : "");
    	}	    
    }
    
    public void openSearchDrafts(Context context)
    {
    	_mode = SearchResult.TYPE_DRAFTS;
    	
    	// this can happen with screwing around in viewpager or landscape mode
    	if (_adapter == null)
	    {
    		initResultView(context);
	    }
    	showNoResults(false);
    	_adapter.clear();
	    _adapter.notifyDataSetInvalidated();
	    
	    
    	_title = "Posts I Drafted Replies To";
    }
    
    private void displayThread(SearchResult result)
    {
    	((MainActivity)getActivity()).openThreadViewAndSelect(result.getPostId());
    }
    
    class SearchResultsAdapter extends LoadingAdapter<SearchResult>
    {
    	float _zoom = 1.0f;
    	
        public SearchResultsAdapter(Context context, ArrayList<SearchResult> items)
        {
            super(context, items);
            // prefs
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
            _zoom = Float.parseFloat(prefs.getString("fontZoom", "1.0"));
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {       
            if (convertView == null)
                convertView = _inflater.inflate(R.layout.search_result_row, null);
            
            return createView(position, convertView, parent);
        }
        
        @Override
        public void setCurrentlyLoading (boolean set)
        {
        	super.setCurrentlyLoading(set);
        	
        	final boolean set2 = set;
        	if (_viewAvailable)
        	{
        		if (getActivity () != null)
        		{
        			getActivity().runOnUiThread(new Runnable(){
            		@Override public void run()
            		{
            			if (_viewAvailable)
                    	{
            				View lv = getListView();
            	        	if (lv != null)
            	        	{
		            			if ((_adapter != null) && (_adapter.getCount() < 1))
		        				{

		            				((View)getListView().getParent()).findViewById(R.id.tlist_FSLoad).setVisibility((set2) ? View.VISIBLE : View.GONE);
		            				getListView().setVisibility((!set2) ? View.VISIBLE : View.GONE);
		        				}
		            			else
		            				((MainActivity) getActivity()).showOnlyProgressBarFromPTRLibrary(set2);
            	        	}
                    	}
            		}
        			});
        		}
        	}
        	
        }

        @Override
        protected View createView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder holder = (ViewHolder)convertView.getTag();
            
            if (holder == null)
            {
                holder = new ViewHolder();
                holder.container = (CheckableLinearLayout)convertView.findViewById(R.id.searchRowLayout);
                holder.userName = (TextView)convertView.findViewById(R.id.textUserName);
                holder.content = (TextView)convertView.findViewById(R.id.textContent);
                holder.posted = (TextView)convertView.findViewById(R.id.textPostedTime);
                holder.lolcount = (TextView)convertView.findViewById(R.id.sres_textPostLolCounts);
                                
                // support zoom
                holder.userName.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.userName.getTextSize() * _zoom);
                holder.content.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.content.getTextSize() * _zoom);
                holder.posted.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.posted.getTextSize() * _zoom);
                
                convertView.setTag(holder);
            }

            // get the thread to display and populate all the data into the layout
            SearchResult t = getItem(position);
            holder.userName.setText(t.getAuthor());
            holder.content.setText(PostFormatter.formatContent(t.getAuthor(), t.getContent(), null, false, true));
            
            if (t.getAuthor().equalsIgnoreCase("Shacknews"))
            	holder.content.setMaxLines(7);
            else
            	holder.content.setMaxLines(2);
            
            // markers for new posts
            if (t.getNew())
            	holder.container.setNew(true);
            else
            	holder.container.setNew(false);
            
            if ((_mode != SearchResult.TYPE_SHACKSEARCHRESULT) && (_mode != SearchResult.TYPE_DRAFTS))
            {
            	holder.lolcount.setVisibility(View.VISIBLE);
	            holder.lolcount.setText(Integer.toString(t.getExtra()));
	            if (t.getType() == SearchResult.TYPE_LOL)
	            	holder.lolcount.setBackgroundResource(R.color.shacktag_lol);
	            else if (t.getType() == SearchResult.TYPE_TAG)
	            	holder.lolcount.setBackgroundResource(R.color.shacktag_tag);
				else if (t.getType() == SearchResult.TYPE_INF)
	            	holder.lolcount.setBackgroundResource(R.color.shacktag_inf);
				else if (t.getType() == SearchResult.TYPE_UNF)
	            	holder.lolcount.setBackgroundResource(R.color.shacktag_unf);
				else if (t.getType() == SearchResult.TYPE_WTF)
	            	holder.lolcount.setBackgroundResource(R.color.shacktag_wtf);
				else if (t.getType() == SearchResult.TYPE_UGH)
	            	holder.lolcount.setBackgroundResource(R.color.shacktag_ugh);
				else
					holder.lolcount.setBackgroundResource(R.color.nonpreview_post_text_color);
            }
            else
            	holder.lolcount.setVisibility(View.GONE);
            
            if (!TimeDisplay.getYear(TimeDisplay.now()).equals(TimeDisplay.getYear(t.getPosted())))
        		holder.posted.setText(TimeDisplay.convTime(t.getPosted(), "MMM dd, yyyy h:mma zzz"));
        	else
        		holder.posted.setText(TimeDisplay.convertTimeLong(t.getPosted()));
                    
            return convertView;
        }
        
        @Override
        protected ArrayList<SearchResult> loadData()
        {
        	if (_mode == SearchResult.TYPE_LOL)
        	{
        		System.out.println("SEARCHING LOL: " +_tag +" days: "+_days);
        		ArrayList<SearchResult> results = new ArrayList<SearchResult>();
        		try {
        			results = ShackApi.searchLOL(_tag, _days, _author, _tagger, _pageNumber + 1);
        		}
        		catch (Exception e) {
        			// System.out.println("SRF: NO RESULTS EXCEPTION");
        			e.printStackTrace();
        			// showNoResults(true);
				}
        		if ((results.size() <= 0) && (_pageNumber == 0))
        		{
        			System.out.println("SRF: NO RESULT SIZE PAGE# 0");
	            	showNoResults(true);
        		}
        		
	            _seen.process(results, _pageNumber, _adapter, _title);
        		
        		_pageNumber++;		            
		        return results;
        	}
        	else if (_mode == SearchResult.TYPE_DRAFTS)
        	{
        		Drafts drafts = new Drafts(getActivity());
        		ArrayList<SearchResult> results = drafts.getDraftListAsSearchResults();
        		if (results.size() == 0)
        		{
        			showNoResults(true);
        		}
        		
        		// prevent loading extra times
        		_adapter.setLastCallSuccessful(false);
        		return results;
        	}
            else
        	{
	        	if ((_term.equals("")) && (_author.equals("")) && (_parentAuthor.equals("")))
	        	{
	        		ArrayList<SearchResult> blank = new ArrayList<SearchResult>();

        			System.out.println("SRF: NO RESULTS ALL TERMS ARE '' ");
	        		showNoResults(true);
	        		return blank;
	        	}
	        	else
	        	{
	        		// this seems to fix a bug where the viewpager forgets about the fragment state and the listview contents and then loads page 2 when you scroll onto it
	        		if (_adapter.getCount() < 2)
	        			_pageNumber = 0;
	        		ArrayList<SearchResult> results = new ArrayList<SearchResult>();
					try {
						results = ShackApi.search(_term, _author, _parentAuthor, _category, _pageNumber + 1, this.getContext());
					} catch (Exception e) {
	        			//System.out.println("SRF: NO RESULTS EXCEPTION");
	        			e.printStackTrace();
						//showNoResults(true);
					}
		            if ((results.size() <= 0) && (_pageNumber == 0))
		            {
	        			System.out.println("SRF: NO RESULTS PAGE#0 NO RES SIZE");
		            	showNoResults(true);
		            }
		            System.out.println("searching with API");
		            
		            _seen.process(results, _pageNumber, _adapter, _title);
		            
		            
		            _pageNumber++;
		            
		            return results;
	        	}
        	}
        }
        
        class ViewHolder
        {
			public TextView lolcount;
			TextView userName;
            TextView content;
            TextView posted;
            public CheckableLinearLayout container;
        }
        
        @SuppressLint("NewApi")
		@Override
        protected void afterDisplay()
        {
        	// pull to refresh integration
        	if (getActivity() != null)
        		((MainActivity)getActivity()).getRefresher().setRefreshComplete();
        }
        
    }
    
    public void adjustSelected(int movement)
    {
    	if (_viewAvailable)
    	{
    		// INTEGRATION PULL TO REFRESH +1
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