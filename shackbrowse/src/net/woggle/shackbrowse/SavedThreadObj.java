package net.woggle.shackbrowse;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by brad on 7/1/2016.
 */
public class SavedThreadObj
{
	public int _rootId;
	public long _postTime;
	public JSONObject _threadJson;
	public SavedThreadObj (int rootId,long postTime,JSONObject postsJson)
	{
		_rootId = rootId; _postTime = postTime; _threadJson = postsJson;
	}
	public SavedThreadObj (int rootId,long postTime,String postsJson)
	{
		_rootId = rootId; _postTime = postTime;
		try
		{
			_threadJson = new JSONObject(postsJson);
		} catch (JSONException e)
		{
			e.printStackTrace();
			System.out.println("SAVEDTHREADOBJ: CANNOT MAKE JSONOBJECT");
		}
	}
}
