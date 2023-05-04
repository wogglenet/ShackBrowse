package net.swigglesoft.shackbrowse;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.ListFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import net.swigglesoft.CheckableLinearLayout;

public class AppMenu extends ListFragment
{
    
    private boolean _viewAvailable;
	private MenuAdapter _adapter;
	private SharedPreferences _prefs;
	int mCheckedPosition = 3;
	private Object thisversion;

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);
		System.out.println("APPMENU ATTACHED");
	}
	
	@Override
    public void onCreate(Bundle savedInstanceBundle)
    {
    	System.out.println("APPMENU INIT");
    	super.onCreate(savedInstanceBundle);
    	this.setRetainInstance(true);
    }

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
	    System.out.println("APPMENU INIT VIEW");
    	_viewAvailable = true;
        return super.onCreateView(inflater, container, savedInstanceState);
    }
    
    @Override
    public void onDestroyView()
    {
    	_viewAvailable = false;
    	super.onDestroyView();
    }

	@Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
	    System.out.println("APPMENU INIT OAC");
        // set list view up
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        getListView().setDividerHeight(0);

        getListView().setBackgroundResource(MainActivity.getThemeResource(getActivity(), R.attr.menuBGdrawable));
        // getListView().setBackground(drawable);
        
        _prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if (_adapter == null)
        {
        	// instantiate adapter
        	_adapter = new MenuAdapter(getActivity(), new ArrayList<MenuItems>());

        	setListAdapter(_adapter);
        	
        	
        }
        
        
		try {
			thisversion = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			thisversion = "unknown";
		}
		
       	updateMenuUi();
       	
    }

    public void setSmallText(int menuID, String text)
    {
	    _adapter.getItem(_adapter.getPositionFromMID(menuID)).setSmallText(text);
    }
    
    public void updateMenuUi()
    {
    	if (_viewAvailable)
    	{
    		if (getListView() != null)
    		{
    			if (_adapter != null)
    	        {
    	        	_adapter.clear();
    	        	
    	        	// menu items
                    boolean verified = _prefs.getBoolean("usernameVerified", false);

                    _adapter.add(new MenuItems(0,"", 9999, 0));

                    if (!verified)
                        _adapter.add(new MenuItems(0, "Log In" , 2, R.drawable.ic_action_action_account_box));
                    else {
                        _adapter.add(new MenuItems(0, getUsername(), 2, R.drawable.ic_action_action_account_box, null, new View.OnClickListener(){
                            @Override
                            public void onClick(View v) {
                                ((MainActivity)getActivity()).cleanUpViewer();
                                ((MainActivity)getActivity()).setContentTo(MainActivity.CONTENT_STATS);

                            }
                        }, R.drawable.ic_action_action_assessment, ""));
                    }
    	        	_adapter.add(new MenuItems(1, "Navigation" , 0, 0));
                    _adapter.add(new MenuItems(0, "Frontpage" , 11, R.drawable.ic_action_action_home));
					_adapter.add(new MenuItems(0, "LOL Page" , 14, R.drawable.ic_action_lol));
    	        	_adapter.add(new MenuItems(0, "Latest Chatty" , 4, R.drawable.ic_action_communication_forum));
    	        	if (_prefs.getBoolean("noteEnabled", false))
    	        		_adapter.add(new MenuItems(0, "Notifications" , 9, R.drawable.ic_action_note_logo2018, null, new View.OnClickListener(){
                            @Override
                            public void onClick(View v) {
                                ((MainActivity)getActivity()).cleanUpViewer();
                                ((MainActivity)getActivity()).setContentTo(MainActivity.CONTENT_NOTEPREFS);
                            }
                        }, R.drawable.ic_action_action_settings, ""));
    	        	_adapter.add(new MenuItems(0, "Starred Posts" , 8, R.drawable.ic_action_toggle_star, null, null, 0, Integer.toString(((MainActivity)getActivity()).mOffline.getCount())));
    	        	_adapter.add(new MenuItems(0, "Shack Messages" , 5, R.drawable.ic_action_communication_email));
    	        	_adapter.add(new MenuItems(0, "Settings" , 7, R.drawable.ic_action_action_settings));
    	        	_adapter.add(new MenuItems(0, "Advanced Search" , 6, R.drawable.ic_action_action_search));
    	        	_adapter.add(new MenuItems(1, "Quick Search" , 0, 0));
    	        	
    	        	setupBasicSearchList(_adapter);
    	        	
    	        	_adapter.add(new MenuItems(1, "Application v" + thisversion , 0, 0));
					_adapter.add(new MenuItems(0, "Community Guidelines" , 1, R.drawable.ic_action_action_report_problem));
    	        	_adapter.add(new MenuItems(0, "Info" , 3, R.drawable.ic_action_action_info_outline));

    		        if (verified) {
    		        	_adapter.add(new MenuItems(0, "Queued Posts", 10, R.drawable.ic_action_image_add_to_photos));
    		        }
                    _adapter.add(new MenuItems(0, "Open Post by ID" , 13, R.drawable.ic_action_action_search));

                    informCheckedPosition();
    	        	getListView().setItemChecked(mCheckedPosition, true);
    	        }
    		}
    	}
    }

    public void informCheckedPosition()
    {
        if (((MainActivity)getActivity())._currentFragmentType == MainActivity.CONTENT_FRONTPAGE)
        {
            mCheckedPosition = _adapter.getPositionFromMID(11);
        }
		else if (((MainActivity)getActivity())._currentFragmentType == MainActivity.CONTENT_LOLPAGE)
		{
			mCheckedPosition = _adapter.getPositionFromMID(14);
		}
        else if (((MainActivity)getActivity())._currentFragmentType == MainActivity.CONTENT_THREADLIST)
        {
            mCheckedPosition = _adapter.getPositionFromMID(4);
        }
        else if (((MainActivity)getActivity())._currentFragmentType == MainActivity.CONTENT_NOTIFICATIONS)
        {
            mCheckedPosition = _adapter.getPositionFromMID(9);
        }
        else if (((MainActivity)getActivity())._currentFragmentType == MainActivity.CONTENT_FAVORITES)
        {
            mCheckedPosition = _adapter.getPositionFromMID(8);
        }
        else if (((MainActivity)getActivity())._currentFragmentType == MainActivity.CONTENT_MESSAGES)
        {
            mCheckedPosition = _adapter.getPositionFromMID(5);
        }
        else if (((MainActivity)getActivity())._currentFragmentType == MainActivity.CONTENT_PREFS)
        {
            mCheckedPosition = _adapter.getPositionFromMID(7);
        }
        else if (((MainActivity)getActivity())._currentFragmentType == MainActivity.CONTENT_SEARCHVIEW)
        {
            mCheckedPosition = _adapter.getPositionFromMID(6);
        }
        else if (((MainActivity)getActivity())._currentFragmentType == MainActivity.CONTENT_STATS)
        {
            mCheckedPosition = _adapter.getPositionFromMID(2);
        }
    }

    @Override
    public void onListItemClick(final ListView l, final View v, final int position, final long id)
    {
    	l.setItemChecked(position, false);
    	int mid = ((MenuItems)getListView().getItemAtPosition(position)).getId();
    	if (mid == 7)
    	{
            ((MainActivity)getActivity()).setContentTo(MainActivity.CONTENT_PREFS);
    	}
    	if (mid == 2)
    	{
    		LoginForm login = new LoginForm(getActivity(), true);
    		login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {
				@Override
				public void onSuccess() {
					AppMenu.this.updateMenuUi();
				}

				@Override
				public void onFailure() {
				}
			});
    	}
    	if (mid == 3)
    	{
    		Intent i = new Intent(getActivity(), DonateActivity.class);
            getActivity().startActivityForResult(i, ThreadListFragment.OPEN_PREFS);
    	}
    	if (mid == 1)
    	{
			((MainActivity)getActivity()).openBrowser(getResources().getString(R.string.shacknews_guidelines_url));
    	}
    	if (mid == 4)
    	{
    		((MainActivity)getActivity()).setContentTo(MainActivity.CONTENT_THREADLIST);
    	}
    	if (mid == 9)
    	{
    		((MainActivity)getActivity()).setContentTo(MainActivity.CONTENT_NOTIFICATIONS);
    	}
    	if (mid == 5)
    	{
			boolean verified = _prefs.getBoolean("usernameVerified", false);
	        if (!verified)
	        {
	        	LoginForm login = new LoginForm(getActivity());
	        	login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {
					@Override
					public void onSuccess() {
						AppMenu.this.onListItemClick(l,v,position,id);
					}

					@Override
					public void onFailure() {
					}
				});
	        	return;
	        }
    	    
    		((MainActivity)getActivity()).setContentTo(MainActivity.CONTENT_MESSAGES);
    	}
    	if (mid == 6)
    	{
    		((MainActivity)getActivity()).setContentTo(MainActivity.CONTENT_SEARCHVIEW);
    	}
    	if (mid == 8)
    	{
    		((MainActivity)getActivity()).setContentTo(MainActivity.CONTENT_FAVORITES);
    	}
    	if (mid == 999)
    	{
    		searchClick(position);
    	}
    	if (mid == 10)
    	{
    		((MainActivity)getActivity()).openPostQueueManager();
    	}
        if (mid == 11)
        {
            ((MainActivity)getActivity()).setContentTo(MainActivity.CONTENT_FRONTPAGE);
        }
		if (mid == 14)
		{
			((MainActivity)getActivity()).setContentTo(MainActivity.CONTENT_LOLPAGE);
		}
        if (mid == 12)
        {
            ((MainActivity)getActivity()).setContentTo(MainActivity.CONTENT_STATS);
        }
        if (mid == 13)
        {
            ((MainActivity)getActivity()).openThreadByIDDialog();
        }
        updateMenuUi();
        ((MainActivity)getActivity()).closeMenu();
    }
   
    class MenuAdapter extends ArrayAdapter<MenuItems>
    {
        float _zoom = 1.0f;
		

        public MenuAdapter(Context context, ArrayList<MenuItems> items)
        {
            super(context, R.layout.menu_row, items);
            loadPrefs();
        }
        
        void loadPrefs()
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AppMenu.this.getActivity());
            _zoom = Float.parseFloat(prefs.getString("fontZoom", "1.0"));
            
        }
        
        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position).getType();
        }

        public int getPositionFromMID(int MID)
        {
            for (int i = 0; i < _adapter.getCount(); i++)
            {
                if (_adapter.getItem(i).getId() == MID)
                    return i;
            }
            return 0;
        }
        

        @Override
		public View getView(int position, View convertView, ViewGroup parent)
        {
        	View vi = convertView;
            if (vi == null)
            {
            	if (getItemViewType(position) == 0)
            		vi = LayoutInflater.from(getActivity()).inflate(R.layout.menu_row, parent, false);
            	if (getItemViewType(position) == 1)
            		vi = LayoutInflater.from(getActivity()).inflate(R.layout.menu_header, parent, false);
            	if (getItemViewType(position) == 2)
            		vi = LayoutInflater.from(getActivity()).inflate(R.layout.menu_searchitem, parent, false);
            }

        	// get the thread to display and populate all the data into the layout
            MenuItems m = getItem(position);

            // regular menu item
            if (getItemViewType(position) == 0)
            {
	            ViewHolderMenuItem holder = (ViewHolderMenuItem)vi.getTag();
	            if (holder == null)
	            {
	                holder = new ViewHolderMenuItem();
	                holder.text = (TextView)vi.findViewById(R.id.menuItemText);
		            holder.smallText = (TextView)vi.findViewById(R.id.menuItemSmallText);
	                holder.icon = (ImageView)vi.findViewById(R.id.menuItemIcon);
                    holder.settings = (ImageView)vi.findViewById(R.id.menuItemSettings);
		            holder.menuItemCont = (CheckableLinearLayout)vi.findViewById(R.id.menuItemContainer);

		            holder.menuItemCont.setBackgroundResource(MainActivity.getThemeResource(getActivity(),R.attr.menuSelectorDrawable));

	                // zoom for preview.. needs to only be done ONCE, when holder is first created
	                holder.text.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.text.getTextSize() * _zoom);
		            holder.smallText.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.smallText.getTextSize() * _zoom);
                    holder.icon.setScaleX(_zoom);
                    holder.icon.setScaleY(_zoom);
                    holder.settings.setScaleX(_zoom);
                    holder.settings.setScaleY(_zoom);

	                vi.setTag(holder);
	            }

				// this makes no sense to me but it works
				holder.menuItemCont.setEnabled(true);
				holder.menuItemCont.setClickable(false);

				if (m.getText().equalsIgnoreCase(""))
				{
					// no click empty item
					holder.menuItemCont.setEnabled(false);
					holder.menuItemCont.setClickable(true);
				}

	            holder.text.setText(m.getText());

	            if (m.getImgRes() == 0)
	            	holder.icon.setVisibility(View.GONE);
	            else
	            	holder.icon.setVisibility(View.VISIBLE);

                if (m.getExtraImageDrawable() != 0) {
                    holder.settings.setOnClickListener(m.getExtraOnClickListener());
                    holder.settings.setVisibility(View.VISIBLE);
                    holder.settings.setImageResource(m.getExtraImageDrawable());
                }
                else
                {
                    holder.settings.setVisibility(View.GONE);
                }

	            if (m.getSmallText() != "") {
		            holder.smallText.setVisibility(View.VISIBLE);
		            holder.smallText.setText(m.getSmallText());
	            }
	            else
	            {
		            holder.smallText.setVisibility(View.GONE);
	            }
	            
	            holder.icon.setImageResource(m.getImgRes());
            }
            if (getItemViewType(position) == 1) // header
            {
	            ViewHolderMenuHeader holder = (ViewHolderMenuHeader)vi.getTag();
	            if (holder == null)
	            {
	                holder = new ViewHolderMenuHeader();
	                holder.text = (TextView)vi.findViewById(R.id.menuItemText);
	                holder.ptl = vi.findViewById(R.id.postTopLine);
	                holder.menuItemCont = (LinearLayout)vi.findViewById(R.id.menuItemContainer);
	                
	                // zoom for preview.. needs to only be done ONCE, when holder is first created
	                holder.text.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.text.getTextSize() * _zoom);
	                
	                holder.menuItemCont.setEnabled(false);
	                holder.menuItemCont.setClickable(false);
	                holder.menuItemCont.setFocusable(false);
	                holder.menuItemCont.setOnClickListener(null);

	                holder.ptl.setBackgroundResource(MainActivity.getThemeResource(getActivity(),R.attr.colorAccent));
	                
	                vi.setTag(holder);
	            }
	            
	            holder.text.setText(m.getText());
            }
            if (getItemViewType(position) == 2)
            {
	            ViewHolderMenuHeader holder = (ViewHolderMenuHeader)vi.getTag();
	            if (holder == null)
	            {
	                holder = new ViewHolderMenuHeader();
	                holder.text = (TextView)vi.findViewById(R.id.text);
		            holder.menuItemCont = (CheckableLinearLayout)vi.findViewById(R.id.searchItemContainer);

		            holder.menuItemCont.setBackgroundResource(MainActivity.getThemeResource(getActivity(),R.attr.menuSelectorDrawable));
	                
	                // zoom for preview.. needs to only be done ONCE, when holder is first created
	                holder.text.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.text.getTextSize() * _zoom);
	                
	                vi.setTag(holder);
	            }
	            
	            holder.text.setText(m.getText());
            }

    	
            return vi;
        }
        
        
        private class ViewHolderMenuItem
        {
            public ImageView icon;
			TextView text;
	        TextView smallText;
            public ImageView settings;
	        CheckableLinearLayout menuItemCont;
        }
        private class ViewHolderMenuHeader
        {
			TextView text;
			LinearLayout menuItemCont;
	        View ptl;
        }
        
        
        
    }
    
    public class MenuItems
    {
        private View.OnClickListener _settingsClick;
        private int _type;
    	// 0 = normal menu item, 1 = header, 2 = searchitem
    	private String _text;
    	private int _id;
    	private int _imgSrc;
		private PremadeSearch _premadesearch;
        private int mExtraImageDrawable;
	    private String mSmallText;

        MenuItems(PremadeSearch pms)
    	{
			this (2, pms.getName(), 999, 0, pms);
    	}
		MenuItems(int type, String text, int id, int img)
    	{
			this(type ,text,id,img,null);
    	}
		MenuItems(int type, String text, int id, int img, PremadeSearch pms)
    	{
            this(type , text, id ,img, pms, null, 0, "");
    	}
        MenuItems(int type, String text, int id, int img, PremadeSearch pms, View.OnClickListener settingsClick, int imageDrawable, String smallText)
        {
            _settingsClick = settingsClick;
            _text = text;
            _id = id;
            _imgSrc = img;
            _type = type;
            _premadesearch = pms;
            mExtraImageDrawable = imageDrawable;
	        mSmallText = smallText;
        }

		public String getText()
		{
			return _text;
		}
	    public String getSmallText()
	    {
		    return mSmallText;
	    }
		public int getId()
		{
			return _id;
		}
		public int getType()
		{
			return _type;
		}
		public int getImgRes()
		{
			return _imgSrc;
		}
		public PremadeSearch getPremadeSearch()
		{
			return _premadesearch;
		}

        public int getExtraImageDrawable() {
            return mExtraImageDrawable;
        }

        public View.OnClickListener getExtraOnClickListener() {
            return _settingsClick;
        }

        public void setSmallText(String smallText) { mSmallText = smallText; updateMenuUi(); }
    }
    
    /*
     * 
     * SEARCH STUFF
     * 
     */
    
    class PremadeSearch 
	{
		private String _name;
		private int _typeIsLol;
		private Bundle _args;
		private Boolean _requiresUsername;

		public PremadeSearch (String name, int typeIsLol, Bundle args, Boolean requiresUsername)
		{
			_name = name;
			_typeIsLol = typeIsLol;
			_args = args;
			_requiresUsername = requiresUsername;
		}
		public Bundle getArgs() { return _args; }
		public int getType() { return _typeIsLol; }
		public Boolean getRequiresUsername() { return _requiresUsername; }
		public String getName() { return _name; }
		@Override
		public String toString() {
			return getName();
		}
	}
	
	public void searchClick (final int pos)
	{
		MainActivity act = (MainActivity)getActivity();
		Bundle args = new Bundle();
		String username = "";
		PremadeSearch search = _adapter.getItem(pos).getPremadeSearch();

		args = search.getArgs();
		if (search.getRequiresUsername())
		{
			boolean verified = _prefs.getBoolean("usernameVerified", false);
	        if (!verified)
	        {
	        	LoginForm login = new LoginForm(getActivity());
	        	login.setOnVerifiedListener(new LoginForm.OnVerifiedListener() {
					@Override
					public void onSuccess() {
						searchClick(pos);
					}

					@Override
					public void onFailure() {
					}
				});
	        	return;
	        }
	        
	        args.putString(args.getString("userNameField"), getUsername());
		}
		
		
		if (args != null)
			args.putString("title", search.getName());
		
		if (search.getType() == 0)
			act.openSearch(args);
		if (search.getType() == 1)
			act.openSearchLOL(args);
		if (search.getType() == 3)
		{
			act.openSearchDrafts();
		}
				
	}
	public String getUsername()
    {
        String userName = _prefs.getString("userName", "");
		return userName.trim();
    }
	
	public void setupBasicSearchList(MenuAdapter _adapter) {
		System.out.println("Setting up search list");

		Bundle args = new Bundle();
		args.putString("userNameField", "parentAuthor");
		args.putString("parentAuthor", "");
		_adapter.add(new MenuItems(new PremadeSearch(getResources().getString(R.string.search_repliestome), 0, args, true)));

		args = new Bundle();
		args.putString("userNameField", "terms");
		args.putString("terms", "");
		_adapter.add(new MenuItems(new PremadeSearch(getResources().getString(R.string.search_vanity), 0, args, true)));
		
		args = new Bundle();
		args.putString("userNameField", "author");
		args.putString("author", "");
		_adapter.add(new MenuItems(new PremadeSearch("My Posts", 0, args, true)));

		if (false) {
			args = new Bundle();
			args.putString("userNameField", "author");
			args.putString("author", "");
			args.putString("tag", "lol");
			args.putInt("days", 365);
			_adapter.add(new MenuItems(new PremadeSearch("My Posts Which Were LOL'd", 1, args, true)));


			args = new Bundle();
			args.putString("tag", "lol");
			args.putInt("days", 1);
			_adapter.add(new MenuItems(new PremadeSearch("Top LOLs Today", 1, args, false)));


			args = new Bundle();
			args.putString("tag", "lol");
			args.putInt("days", 30);
			_adapter.add(new MenuItems(new PremadeSearch("Top LOLs This Month", 1, args, false)));

			args = new Bundle();
			args.putString("userNameField", "tagger");
			args.putString("tagger", "");
			args.putString("tag", "lol");
			args.putInt("days", 365);
			_adapter.add(new MenuItems(new PremadeSearch("Posts I LOL'd", 1, args, true)));

			args = new Bundle();
			args.putString("userNameField", "tagger");
			args.putString("tagger", "");
			args.putString("tag", "tag");
			args.putInt("days", 365);
			_adapter.add(new MenuItems(new PremadeSearch("Posts I TAG'd", 1, args, true)));
		}
		args = new Bundle();
		args.putString("author", "Shacknews");
		_adapter.add(new MenuItems(new PremadeSearch("News Posts", 0, args, false)));
		
		args = new Bundle();
		args.putString("terms", "*");
		args.putString("category", "informative");
		_adapter.add(new MenuItems(new PremadeSearch("Informative Posts", 0, args, false)));

		_adapter.add(new MenuItems(new PremadeSearch("Posts I Drafted Replies To", 3, null, false)));

        // saved searches
        if (_prefs.getString("savedSearchesJson", null) != null)
        {
	        try {
		    	final JSONArray savedSearches = new JSONArray(_prefs.getString("savedSearchesJson", ""));
		    	
		    	for (int i = 0; i < savedSearches.length(); i++)
		    	{
		    		_adapter.insert(new MenuItems(new PremadeSearch("Saved: " + savedSearches.getJSONObject(i).getString("name"), savedSearches.getJSONObject(i).getInt("typeIsLol"), SearchViewFragment.deserializeBundle(savedSearches.getJSONObject(i).getString("args")), false)), (_prefs.getBoolean("noteEnabled", false)) ? 11 : 10);
		    	}
	        }
	        catch (JSONException e)
	        {
	        }
        }
		_adapter.notifyDataSetChanged();
    }
}
