package net.woggle;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.ArrayAdapter;

import org.json.JSONArray;

import java.util.ArrayList;

public class AutocompleteProvider
{
	private final Context mContext;
	private ArrayList<String> mListdata;
	private SharedPreferences mPrefs;
	private String mId;
	private int mLimit;
	private JSONArray jArray;
	private static final int MIN_CHAR_LENGTH = 3;

	public AutocompleteProvider (Context context, String id, int limit)
	{
		mContext = context;
		mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mId = id;
		mLimit = limit;

		try
		{
			if (mPrefs.getString(mId + "SuggestionList", "").contentEquals(""))
			{
				jArray = new JSONArray();
			} else
			{
				jArray = new JSONArray(mPrefs.getString(mId + "SuggestionList", ""));
			}

			// make arraylist
			mListdata = new ArrayList<String>();
			if (jArray != null)
			{
				for (int i = 0; i < jArray.length(); i++)
				{
					mListdata.add(jArray.getString(i));
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
	public ArrayList<String> getArrayList() { return mListdata; }
	public JSONArray getJSONArray() { return jArray; }
	public void addItem (String item)
	{
		if (item.length() >= MIN_CHAR_LENGTH)
		{
			try
			{

				if (jArray.toString().toLowerCase().contains("\"" + item + "\""))
				{
					mListdata.remove(item);
				}

				mListdata.add(0, item);

				if (mListdata.size() > mLimit)
					mListdata.remove(mLimit);

				// arraylist to json
				jArray = new JSONArray(mListdata);
				SharedPreferences.Editor edit = mPrefs.edit();
				edit.putString(mId + "SuggestionList", jArray.toString());
				edit.commit();

			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	public ArrayAdapter<String> getSuggestionAdapter()
	{
		return new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, mListdata);
	}
}
