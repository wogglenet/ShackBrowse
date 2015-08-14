package net.woggle.shackbrowse;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.Spannable;

public class Thread implements Parcelable {

	// these must be initialized or the parcelable gets borked
    private int _threadId = 0;
    private String _userName = "unset";
    private Long _postedTime = 0L;
    private String _content = "unset";
    private int _replyCount = 0;
    private int _replyCountPrevious = 0;
    private String _moderation = "ontopic";
    private boolean _replied = false;
    private JSONObject _json;

    private Spannable _preview = null;
	private boolean _pinned = false;
	private String _filterable;
	private LolObj _lolObj;

    public Thread(int threadId, String userName, String content, Long postedTime, int replyCount, String moderation, boolean replied, boolean pinned)
    {
        _threadId = threadId;
        _userName = userName;
        _content = content;
        _postedTime = postedTime;
        _replyCount = replyCount;
        _moderation = moderation;
        _replied = replied;
        _pinned = pinned;
        _filterable = _userName.toLowerCase() + " " + PostFormatter.formatContent(this, false, false).toString().toLowerCase();

        JSONObject jconst = new JSONObject();
        try {
	        jconst.put("id", Integer.toString(threadId));
	        jconst.put("body", content);
	        jconst.put("author", userName);
	        jconst.put("category", moderation);
	        // serverside replycount is off by one
	        jconst.put("reply_count", replyCount);
	        jconst.put("replied", replied);
			jconst.put("date", TimeDisplay.convTime(postedTime, "MMM dd, yyyy h:mma zzz"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        _json = jconst;
    }
    
    public void setLolObj(LolObj lol)
    {
    	_lolObj = lol;
    }
    
    public LolObj getLolObj()
    {
    	return _lolObj;
    }

    public int getThreadId()
    {
        return _threadId;
    }

    public String getUserName()
    {
        return _userName;
    }
    public JSONObject getJson()
    {
        return _json;
    }

    public Long getPosted()
    {
        return _postedTime;
    }

    public String getContent()
    {
        return _content;
    }
    
    public void setContent(String set)
    {
    	_content = set;
    }

    public int getReplyCount()
    {
        return _replyCount;
    }

    public int getReplyCountPrevious()
    {
        return _replyCountPrevious;
    }

    public void setReplyCount(int replyCount)
    {
        _replyCount = replyCount;
    }
    
    public void setReplyCountPrevious(int replyCountPrevious)
    {
        _replyCountPrevious = replyCountPrevious;
    }
    public String getModeration()
    {
        return _moderation;
    }
    
    public boolean getReplied()
    {
        return _replied;
    }
    public void setReplied(boolean set)
    {
        _replied = set;
    }
    public Spannable getPreview(boolean showTags, boolean stripNewLines)
    {
        if (_preview == null)
            _preview = PostFormatter.formatContent(this, !stripNewLines, showTags);
        return _preview;
    }
    public void nullifyPreview()
    {
    	_preview = null;
    }
    public String getFilterable ()
    {
        return _filterable;
    }

	public boolean getPinned() {
		
		return _pinned;
	}

	
	// parcelable
	
	 // Your existing code

    public Thread(Parcel in) {
        super(); 
        readFromParcel(in);
    }

    public static final Parcelable.Creator<Thread> CREATOR = new Parcelable.Creator<Thread>() {
        public Thread createFromParcel(Parcel in) {
            return new Thread(in);
        }

        public Thread[] newArray(int size) {

            return new Thread[size];
        }

    };

    public void readFromParcel(Parcel in) {
    	_threadId = in.readInt();
    	_postedTime = in.readLong();
    	_replyCount = in.readInt();
    	_replyCountPrevious = in.readInt();
    	_replied = (in.readInt() == 1) ? true : false;
    	_pinned = (in.readInt() == 1) ? true : false;
    	_content = in.readString();
    	_userName = in.readString();
    	_moderation = in.readString();
    	String lolString = in.readString();
    	if (lolString.equalsIgnoreCase("x"))
    		_lolObj = null;
    	else
    	{
    		_lolObj = new LolObj(lolString);
    	}
    	
    	JSONObject jconst = new JSONObject();
        try {
	        jconst.put("id", Integer.toString(_threadId));
	        jconst.put("body", _content);
	        jconst.put("author", _userName);
	        jconst.put("category", _moderation);
	        // serverside replycount is off by one
	        jconst.put("reply_count", _replyCount);
	        jconst.put("replied", _replied);
			jconst.put("date", TimeDisplay.convTime(_postedTime, "MMM dd, yyyy h:mma zzz"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        _json = jconst;
        
        _filterable = _userName.toLowerCase() + " " + PostFormatter.formatContent(this, false, false).toString().toLowerCase();
    }
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
    	dest.writeInt(_threadId);
    	dest.writeLong(_postedTime);
    	dest.writeInt(_replyCount);
    	dest.writeInt(_replyCountPrevious);
    	dest.writeInt(bToInt(_replied));
    	dest.writeInt(bToInt(_pinned));
    	dest.writeString(_content);
    	dest.writeString(_userName);
    	dest.writeString(_moderation);
    	dest.writeString(_lolObj != null ? _lolObj.lolObjToString() : "x");
   }

    public int bToInt (boolean val)
    {
    	if (val)
    		return 1;
    	else
    		return 0;
    }
}
