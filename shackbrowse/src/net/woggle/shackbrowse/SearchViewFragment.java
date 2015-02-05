package net.woggle.shackbrowse;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class SearchViewFragment extends Fragment
{
    private OnEditorActionListener IMESearchActionListener;
	private Spinner tagspinner;
	private Spinner dayspinner;
	private EditText termEditor;
	private EditText authorEditor;
	private EditText parentEditor;
	private EditText authorEditorLol;
	private EditText taggerEditorLol;
	private SharedPreferences _prefs;
	private boolean _enableSaveSearch;
	private float _zoom = 1.0f;
	private TableLayout sPCont;
	private TableLayout sTCont;

	@Override
	public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        /*
         * NEW SEARCH MODE
         */
        // prefs
        _prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        _zoom  = Float.parseFloat(_prefs.getString("fontZoom", "1.0"));
        // "enableDonatorFeatures"
        _enableSaveSearch = true;
        
        setupBasicSearchList();
        
        
        /*
         * OLD SEARCH MODE
         */
        // search mode spinner
         final Spinner spinner = (Spinner)this.getActivity().findViewById(R.id.searchModeSelector);
		 ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getActivity(), R.array.search_choices, android.R.layout.simple_spinner_item);
		 adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		 spinner.setAdapter(adapter);
		 sPCont = (TableLayout) getActivity().findViewById(R.id.searchPostContainer);
		 sTCont = (TableLayout) getActivity().findViewById(R.id.searchTagContainer);
		 OnItemSelectedListener spinlisten = new OnItemSelectedListener(){
			 	public void onItemSelected(AdapterView<?> parent, View view, 
			            int pos, long id) {
					sPCont.setVisibility((pos == 0) ? View.VISIBLE : View.GONE);
					sTCont.setVisibility((pos == 1) ? View.VISIBLE : View.GONE);
			    }
			    public void onNothingSelected(AdapterView<?> parent) { }
		 };
		 spinner.setOnItemSelectedListener(spinlisten);
		 
		 // mode toggle button
		 ImageButton searchtogButton = (ImageButton)this.getActivity().findViewById(R.id.search_button_togglemode);
         searchtogButton.setOnClickListener(new OnClickListener()
         {
             @Override
             public void onClick(View v)
             {
            	 int i = spinner.getSelectedItemPosition();
            	 if (i == 1) i = 0;
            	 else i = 1;
                 spinner.setSelection(i,true);
             }
         });
		 
		 // search shacktag spinner
         tagspinner = (Spinner)this.getActivity().findViewById(R.id.searchTagSelector);
		 ArrayAdapter<CharSequence> tagadapter = ArrayAdapter.createFromResource(this.getActivity(), R.array.search_tag_choices, android.R.layout.simple_spinner_item);
		 tagadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		 tagspinner.setAdapter(tagadapter);
		 
	     // search date spinner
         dayspinner = (Spinner)this.getActivity().findViewById(R.id.searchDaySelector);
		 ArrayAdapter<CharSequence> dayadapter = ArrayAdapter.createFromResource(this.getActivity(), R.array.search_day_choices, android.R.layout.simple_spinner_item);
		 dayadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		 dayspinner.setAdapter(dayadapter);	        
	        
         ((Button)this.getActivity().findViewById(R.id.search_button_vanity))
         .setOnClickListener(new OnClickListener()
         {
             @Override
             public void onClick(View v)
             { vanitySearch(v); }
         });
         
         ((Button)this.getActivity().findViewById(R.id.search_button_ownposts))
         .setOnClickListener(new OnClickListener()
         {
             @Override
             public void onClick(View v)
             { ownSearch(v); }
         });
         
         ((Button)this.getActivity().findViewById(R.id.search_button_Lolownposts))
         .setOnClickListener(new OnClickListener()
         {
             @Override
             public void onClick(View v)
             { ownLolSearch(v); }
         });
         
         ((Button)this.getActivity().findViewById(R.id.search_button_Lolowntags))
         .setOnClickListener(new OnClickListener()
         {
             @Override
             public void onClick(View v)
             { ownLolSearchTagger(v); }
         });
         
         ((Button)this.getActivity().findViewById(R.id.search_button_ownparent))
         .setOnClickListener(new OnClickListener()
         {
             @Override
             public void onClick(View v)
             { parentSearch(v); }
         });
         
         
         // individual clear buttons
         ((ImageButton)this.getActivity().findViewById(R.id.search_clearTerm)).setOnClickListener(new OnClickListener()
         {
             @Override
             public void onClick(View v)
             { ((EditText)getActivity().findViewById(R.id.searchTerm)).setText(""); }
         });
         ((ImageButton)this.getActivity().findViewById(R.id.search_clearAuthor)).setOnClickListener(new OnClickListener()
         {
             @Override
             public void onClick(View v)
             { ((EditText)getActivity().findViewById(R.id.searchAuthor)).setText(""); }
         });
         ((ImageButton)this.getActivity().findViewById(R.id.search_clearParentAuthor)).setOnClickListener(new OnClickListener()
         {
             @Override
             public void onClick(View v)
             { ((EditText)getActivity().findViewById(R.id.searchParentAuthor)).setText(""); }
         });
         ((ImageButton)this.getActivity().findViewById(R.id.search_clearLolAuthor)).setOnClickListener(new OnClickListener()
         {
             @Override
             public void onClick(View v)
             { ((EditText)getActivity().findViewById(R.id.searchLolAuthor)).setText(""); }
         });
         ((ImageButton)this.getActivity().findViewById(R.id.search_clearLolTagger)).setOnClickListener(new OnClickListener()
         {
             @Override
             public void onClick(View v)
             { ((EditText)getActivity().findViewById(R.id.searchLolTagger)).setText(""); }
         });
         
         // support zoom
         ((EditText)this.getActivity().findViewById(R.id.searchTerm)).setTextSize(TypedValue.COMPLEX_UNIT_PX, ((EditText)this.getActivity().findViewById(R.id.searchTerm)).getTextSize() * _zoom);
         ((EditText)this.getActivity().findViewById(R.id.searchAuthor)).setTextSize(TypedValue.COMPLEX_UNIT_PX, ((EditText)this.getActivity().findViewById(R.id.searchAuthor)).getTextSize() * _zoom);
         ((EditText)this.getActivity().findViewById(R.id.searchParentAuthor)).setTextSize(TypedValue.COMPLEX_UNIT_PX, ((EditText)this.getActivity().findViewById(R.id.searchParentAuthor)).getTextSize() * _zoom);
         
         // support hiding and showing individual clear buttons on edittexts
         ((EditText)this.getActivity().findViewById(R.id.searchTerm)).addTextChangedListener(new TextWatcher(){
             public void afterTextChanged(Editable s) {}
             public void beforeTextChanged(CharSequence s, int start, int count, int after){}
             public void onTextChanged(CharSequence s, int start, int before, int count){ View button = getActivity().findViewById(R.id.search_clearTerm); if (count > 0) button.setVisibility(View.VISIBLE); else button.setVisibility(View.GONE); }
         });
         ((EditText)this.getActivity().findViewById(R.id.searchAuthor)).addTextChangedListener(new TextWatcher(){
             public void afterTextChanged(Editable s) {}
             public void beforeTextChanged(CharSequence s, int start, int count, int after){}
             public void onTextChanged(CharSequence s, int start, int before, int count){ View button = getActivity().findViewById(R.id.search_clearAuthor); if (count > 0) button.setVisibility(View.VISIBLE); else button.setVisibility(View.GONE); }
         }); 
         ((EditText)this.getActivity().findViewById(R.id.searchParentAuthor)).addTextChangedListener(new TextWatcher(){
             public void afterTextChanged(Editable s) {}
             public void beforeTextChanged(CharSequence s, int start, int count, int after){}
             public void onTextChanged(CharSequence s, int start, int before, int count){ View button = getActivity().findViewById(R.id.search_clearParentAuthor); if (count > 0) button.setVisibility(View.VISIBLE); else button.setVisibility(View.GONE); }
         }); 
         ((EditText)this.getActivity().findViewById(R.id.searchLolAuthor)).addTextChangedListener(new TextWatcher(){
             public void afterTextChanged(Editable s) {}
             public void beforeTextChanged(CharSequence s, int start, int count, int after){}
             public void onTextChanged(CharSequence s, int start, int before, int count){ View button = getActivity().findViewById(R.id.search_clearLolAuthor); if (count > 0) button.setVisibility(View.VISIBLE); else button.setVisibility(View.GONE); }
         }); 
         ((EditText)this.getActivity().findViewById(R.id.searchLolTagger)).addTextChangedListener(new TextWatcher(){
             public void afterTextChanged(Editable s) {}
             public void beforeTextChanged(CharSequence s, int start, int count, int after){}
             public void onTextChanged(CharSequence s, int start, int before, int count){ View button = getActivity().findViewById(R.id.search_clearLolTagger); if (count > 0) button.setVisibility(View.VISIBLE); else button.setVisibility(View.GONE); }
         }); 
         
         IMESearchActionListener = new TextView.OnEditorActionListener() {
             @Override
             public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                 if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                	 int i = spinner.getSelectedItemPosition();
                	 if (i == 0) doSearch();
                	 else doSearchLol();
                     return true;
                 }
                 return false;
             }
         };
         termEditor.setOnEditorActionListener(IMESearchActionListener);
         authorEditor.setOnEditorActionListener(IMESearchActionListener);
         taggerEditorLol.setOnEditorActionListener(IMESearchActionListener);
         parentEditor.setOnEditorActionListener(IMESearchActionListener);
         authorEditorLol.setOnEditorActionListener(IMESearchActionListener);
         
         if (getArguments() != null)
        	 loadSearchArgs(getArguments());
    }
    
    /*
     * SEARCH SAVING
     */
    protected void saveSearch() {
    	final TableLayout sPCont = (TableLayout) getActivity().findViewById(R.id.searchPostContainer);
		
		Bundle saveArgs = new Bundle();
		if (sPCont.getVisibility() == View.VISIBLE)
			saveArgs = getSearchArgs();
	
		else
			saveArgs = getSearchLolArgs();
		
		if (saveArgs.size() > 0)
		{
	    	AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
	    	alert.setTitle("New Saved Search");
	    	alert.setMessage("Name:");
	    	final EditText input = new EditText(getActivity());
	    	alert.setView(input);
	    	alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
	    	public void onClick(DialogInterface dialog, int whichButton) {
				Editable value = input.getText();
				saveSearchProcess(value.toString());
				}
	    	});
	    	alert.setNegativeButton("Cancel", null);
	    	alert.show();
		}
		else
			ErrorDialog.display(getActivity(), "Could not save", "Enter values to save first");
    }
    
    public void saveSearchProcess(String name)
    {
		final TableLayout sPCont = (TableLayout) getActivity().findViewById(R.id.searchPostContainer);
		final TableLayout sTCont = (TableLayout) getActivity().findViewById(R.id.searchTagContainer);
		
		Bundle saveArgs = new Bundle();
		int typeIsLol;
		if (sPCont.getVisibility() == View.VISIBLE)
		{
			System.out.println("GOT ARGS");
			saveArgs = getSearchArgs();
			typeIsLol = 0;
		}
		else
		{
			System.out.println("GOT LOL ARGS");
			saveArgs = getSearchLolArgs();
			typeIsLol = 1;
		}
		
		JSONObject thisSearch = new JSONObject();
		try {
			thisSearch.put("name", name);
			thisSearch.put("typeIsLol", typeIsLol);
			thisSearch.put("args", serializeBundle(saveArgs));
			JSONArray savedSearches;
			if (_prefs.getString("savedSearchesJson", null) != null)
				savedSearches = new JSONArray(_prefs.getString("savedSearchesJson", ""));
			else
				savedSearches = new JSONArray();
			
			savedSearches.put(thisSearch);
			SharedPreferences.Editor editor = _prefs.edit();
	    	editor.putString("savedSearchesJson", savedSearches.toString());
	    	editor.commit();
	    	
	    	// refresh the list
	    	setupBasicSearchList();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ErrorDialog.display(getActivity(), "Error", "Could not save search");
		}
	}
    
    private void setupBasicSearchList() {
    	((MainActivity)getActivity()).updateAppMenu();
		
	}

	protected void deleteSearches()
    {
    	try {
	    	final JSONArray savedSearches = new JSONArray(_prefs.getString("savedSearchesJson", ""));
	    	if (savedSearches.length() == 0)
	    	{
	    		ErrorDialog.display(getActivity(), "No Saved Searches to Delete", "Create a saved search before trying to delete");
	    		return;
	    	}
	    	final ArrayList<CharSequence> savedNames = new ArrayList<CharSequence>();
	    	for (int i = 0; i < savedSearches.length(); i++)
	    	{
	    		savedNames.add(savedSearches.getJSONObject(i).getString("name"));
	    	}
	    	// hint is so toarray() works right, otherwise gives object[]
	    	CharSequence[] savedItemsHint = new CharSequence[savedNames.size()];
	    	final CharSequence[] savedItems = (CharSequence[])savedNames.toArray(savedItemsHint);
	    	final boolean[] checkedItems = new boolean[savedNames.size()];
	        
	    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        builder.setTitle("Choose which to delete");
	        builder.setMultiChoiceItems(savedItems, checkedItems, new OnMultiChoiceClickListener() {
	        	@Override
	        	public void onClick(DialogInterface dialog, int which, boolean isChecked) {
	        		
	        	}});
	        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){ @Override public void onClick(DialogInterface dialog, int which) {}});
	        builder.setPositiveButton("Delete Selected", new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					try {
						Editor edit = _prefs.edit();
						
						ListView lv = ((AlertDialog)dialog).getListView();
						SparseBooleanArray checkedItems = lv.getCheckedItemPositions();
						ArrayList<String> deleteTheseNames = new ArrayList<String>();
						
						System.out.println("size: " + lv.getCheckedItemPositions().size() + " info " + lv.getAdapter().getCount());
						for (int i = 0; i < checkedItems.size(); i++)
						{
							if (checkedItems.valueAt(i))
							{
								deleteTheseNames.add((String)lv.getItemAtPosition(checkedItems.keyAt(i)));
								System.out.println("TRYING TO DELTE NAME: " + (String)lv.getItemAtPosition(checkedItems.keyAt(i)));
							}
						}
						
						// copy to new JSONArray only if name does not exist in delnames array
						JSONArray newSearches = new JSONArray();
						for (int i = 0; i < savedSearches.length(); i++)
				    	{
							if (deleteTheseNames.size() == 0)
							{
								newSearches.put(savedSearches.getJSONObject(i));
							}
							else if (deleteTheseNames.contains(savedSearches.getJSONObject(i).getString("name")))
							{
								
							}
							else
							{
								newSearches.put(savedSearches.getJSONObject(i));
							}
				    	}
						edit.putString("savedSearchesJson", newSearches.toString());
						edit.commit();
						
						// refresh the list
						setupBasicSearchList();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						ErrorDialog.display(getActivity(), "Error", "Could not delete search");
					}
				}});
	        AlertDialog alert = builder.create();
	        alert.setCanceledOnTouchOutside(true);
	        alert.show();
    	} catch (JSONException e) {
			// TODO Auto-generated catch block
    		e.printStackTrace();
			ErrorDialog.display(getActivity(), "No Saved Searches to Delete", "Could not open delete search dialog");
		}
    }

	

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View layout = inflater.inflate(R.layout.search, null);
        termEditor = (EditText)layout.findViewById(R.id.searchTerm);
        authorEditor = (EditText)layout.findViewById(R.id.searchAuthor);
        parentEditor = (EditText)layout.findViewById(R.id.searchParentAuthor);
        authorEditorLol = (EditText)layout.findViewById(R.id.searchLolAuthor);
        taggerEditorLol = (EditText)layout.findViewById(R.id.searchLolTagger);
        return layout;
    }
	
	public void searchGo()
	{
		if (sPCont.getVisibility() == View.VISIBLE)
   		 	doSearch();
	   	else if (sTCont.getVisibility() == View.VISIBLE)
	   		doSearchLol();
	}
	
	public void loadSearchArgs(Bundle args)
	{
		if (args.containsKey("tag"))
		{
			// type is lolsearch
			if (args.containsKey("author"))
				authorEditorLol.setText(args.getString("author"));
			
			if (args.containsKey("tagger"))
				taggerEditorLol.setText(args.getString("tagger"));
			
	        for (int i = 0; i < tagspinner.getCount(); i++)
	        {
	        	if (((CharSequence)tagspinner.getItemAtPosition(i)).toString().equalsIgnoreCase(args.getString("tag")))
	        		tagspinner.setSelection(i);
	        }
	        for (int i = 0; i < dayspinner.getCount(); i++)
	        {
	        	if (((CharSequence)dayspinner.getItemAtPosition(i)).toString().equalsIgnoreCase(Integer.toString(args.getInt("days"))))
	        		dayspinner.setSelection(i);
	        }
	        
	        // show tag search mode
	        final Spinner spinner = (Spinner)this.getActivity().findViewById(R.id.searchModeSelector);
	        spinner.setSelection(1);
		}
		else
		{
			if (args.containsKey("terms"))
				termEditor.setText(args.getString("terms"));
			if (args.containsKey("author"))
				authorEditor.setText(args.getString("author"));
			if (args.containsKey("parentAuthor"))
				parentEditor.setText(args.getString("parentAuthor"));
		}
	}
	
	public Bundle getSearchArgs()
    {
		String term = termEditor.getText().toString();
        String author = authorEditor.getText().toString();
        String parentAuthor = parentEditor.getText().toString();
        
        Bundle args = new Bundle();
        if (term.length() > 0)
        	args.putString("terms", term);
        if (author.length() > 0)
        	args.putString("author", author);
        if (parentAuthor.length() > 0)
        	args.putString("parentAuthor", parentAuthor);
        return args;
    }
    
    public void doSearch()
    {
        ((MainActivity)SearchViewFragment.this.getActivity()).openSearch(getSearchArgs());
        System.out.println("searchresult page request sent");
    }
    
    public Bundle getSearchLolArgs()
    {
    	String tag = tagspinner.getSelectedItem().toString();
        int days = Integer.parseInt(dayspinner.getSelectedItem().toString());
        String author = authorEditorLol.getText().toString();
        String tagger = taggerEditorLol.getText().toString();
        
        Bundle args = new Bundle();
        args.putString("tag", tag);
        args.putString("author", author);
        args.putString("tagger", tagger);
        args.putInt("days", days);
        return args;
    }
    public void doSearchLol ()
    {
        ((MainActivity)SearchViewFragment.this.getActivity()).openSearchLOL(getSearchLolArgs());
        System.out.println("searchresult page request sent");
    }
    
    public String getUsername()
    {
        String userName = _prefs.getString("userName", "");
		return userName.trim();
    }
    
    public void vanitySearch(View view)
    {
        termEditor.setText(getUsername());
    }
    
    public void clrSearch(View view)
    {
        termEditor.setText("");
        authorEditor.setText("");
        parentEditor.setText("");
    }
    
    public void clrSearchLOL(View view)
    {
        authorEditorLol.setText("");
    }
    public void clrSearchLOLtagger(View view)
    {
        taggerEditorLol.setText("");
    }
    
    public void ownSearch(View view)
    {
        authorEditor.setText(getUsername());
    }
    
    public void ownLolSearch(View view)
    {
        authorEditorLol.setText(getUsername());
    }
    public void ownLolSearchTagger(View view)
    {
        taggerEditorLol.setText(getUsername());
    }
    
    public void parentSearch(View view)
    {
        parentEditor.setText(getUsername());
    }

    // serialize stuff
    private String serializeBundle(final Bundle bundle) {
    	String base64 = null;
    	final Parcel parcel = Parcel.obtain();
    	try {
	    	parcel.writeBundle(bundle);
	    	final ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    	final GZIPOutputStream zos = new GZIPOutputStream(new BufferedOutputStream(bos));
	    	zos.write(parcel.marshall());
	    	zos.close();
	    	base64 = android.util.Base64.encodeToString(bos.toByteArray(), 0);
    	} catch(IOException e) {
    		e.printStackTrace();
    		base64 = null;
    	} finally {
    		parcel.recycle();
    	}
    	return base64;
    }
    	 
    static Bundle deserializeBundle(final String base64) {
    	Bundle bundle = null;
    	final Parcel parcel = Parcel.obtain();
    	try {
    		final ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
    		final byte[] buffer = new byte[1024];
    		final GZIPInputStream zis = new GZIPInputStream(new ByteArrayInputStream(android.util.Base64.decode(base64, 0)));
    		int len = 0;
    		while ((len = zis.read(buffer)) != -1) {
    			byteBuffer.write(buffer, 0, len);
    		}
    		zis.close();
    		parcel.unmarshall(byteBuffer.toByteArray(), 0, byteBuffer.size());
    		parcel.setDataPosition(0);
    		bundle = parcel.readBundle();
    	} catch (IOException e) {
    		e.printStackTrace();
    		bundle = null;
    	} finally {
    		parcel.recycle();
    	}
    	 
    	return bundle;
    }
}
