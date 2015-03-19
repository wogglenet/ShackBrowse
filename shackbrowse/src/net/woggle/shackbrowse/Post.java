package net.woggle.shackbrowse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import android.graphics.Bitmap;
import android.text.Spannable;

public class Post implements Comparable<Post> {

    private int _postId;
    private String _userName;
    private String _content;
    private Long _postedTime;
    private int _level;
    private String _moderation;
    private int _order = Integer.MAX_VALUE;
    private boolean _expanded;
    private HashMap<Integer, Boolean> _spoiled;
    private String _depthString;
    private String _depthStringF;

    private Spannable _preview;
	private ArrayList<String> imageUrls = new ArrayList<String>();
	private Spannable _formattedContent;
	private Bitmap _treebmp;
	private LolObj _lolobj = null;
	
	private int _userType = 0;
	private boolean _isNWS = false;
	private boolean _isINF = false;
	private boolean _seen = true;
	private boolean _isPolitical = false;
	private boolean _isPQP = false;
    private boolean _isWorking = false;

    /**
     * gets the depth string representing the tree lines for this post, before the collapse icon is added
     * @return String
     */
    public String getDepthString()
    {
    	return _depthString;
    }
    public void setDepthString(String depth)
    {
    	_depthString = depth;
    }
    /**
     * gets the depth string representing the tree lines for this post, after the collapse icon is added
     * @return String
     */
    public String getDepthStringFormatted()
    {
        return _depthStringF;
    }
    public void setDepthStringFormatted(String depth)
    {
        _depthStringF = depth;
    }
    /*
    public Bitmap getTreeBMP()
    {
		return _treebmp;
    }
    public void setTreeBMP(Bitmap tree)
    {
		_treebmp = tree;
    }
    public void recycleTreeBMP()
    {
    	if (_treebmp != null)
    	{
    		_treebmp.recycle();
    	}
		_treebmp = null;
    }*/
    public boolean getSpoiled(int index)
    {
    	return _spoiled.get(index);
    }
    public HashMap<Integer, Boolean> getSpoiledHash()
    {
    	return _spoiled;
    }
    
    public void setSpoiled(int index, boolean spoiled)
    {
	   _spoiled.put(index, spoiled);
    }
    public int getNumSpoilers()
    {
    	return _spoiled.size();
    }
    
    public static Post fromThread(Thread thread)
    {
        return new Post(thread.getThreadId(), thread.getUserName(), thread.getContent(), thread.getPosted(), 0, thread.getModeration(), true);
    }
    
    public static Post fromMessage(Message thread)
    {
        return new Post(thread.getMessageId(), thread.getUserName(), thread.getContent(), thread.getPosted(), 0, "ontopic", true);
    }
    
    public static Post fromSearchResult(SearchResult result)
    {
        return new Post(result.getPostId(), result.getAuthor(), result.getContent(), result.getPosted(), 0, "", true);
    }

    public Post(int postId, String userName, String content, Long postedTime, int level, String moderation, boolean expanded)
    {
    	this(postId, userName, content, postedTime, level, moderation, expanded, true, false);
    }
    public Post(int postId, String userName, String content, Long postedTime, int level, String moderation, boolean expanded, boolean seen, boolean isPQP)
    {
        _postId = postId;
        _userName = userName;
        _content = content;
        _postedTime = postedTime;
        _level = level;
        _moderation = moderation;
        _expanded = expanded;
        _seen = seen;
        // post queue post
        _isPQP  = isPQP;
        
        setLevel(level);
        
        _spoiled = new HashMap<Integer, Boolean>();
        Spannable prePreview = PostFormatter.formatContent(this, false);
        // this limits the amount of work the ui thread has to do when displaying single line previews
        _preview = (Spannable)prePreview.subSequence(0, Math.min(prePreview.length(), 100));
        _formattedContent = PostFormatter.formatContent(this, true);
        // imageUrls = new ArrayList<String>();
        setUserType();
        preSetModeration();
    }
    
    public void setLevel(int level) {
    	_level = level;
    	if (level > 1)
        {
	        char[] zeroes1 = new char[(level -1)];
	        Arrays.fill(zeroes1, '0');
	        if (_seen)
	        	_depthString = new String(zeroes1) + "L";
	        else
	        	_depthString = new String(zeroes1) + "[";
        }
        else if (level == 1)
        {
        	if (_seen)
        		_depthString = "L";
        	else
        		_depthString = "[";
        }
        else _depthString = "";

        setDepthString(_depthString);
        setDepthStringFormatted(_depthString);
    }
    
    public boolean getSeen()
    {
    	return _seen;
    }
    public boolean isPQP()
    {
    	return _isPQP;
    }
    public boolean isWorking()
    {
        return _isWorking;
    }
    public void setSeen(boolean set)
    {
    	_seen  = set;
    }

    public int getPostId()
    {
        return _postId;
    }

    public String getUserName()
    {
        return _userName;
    }

    public String getContent()
    {
        return _content;
    }
    
    public Spannable getFormattedContent()
    {
        return _formattedContent;
    }

    public Long getPosted()
    {
        return _postedTime;
    }

    public int getLevel()
    {
        return _level;
    }
    
    public String getModeration()
    {
        return _moderation;
    }
    // next 3 are purely optimization
    public void preSetModeration()
    {
    	if (getModeration().equalsIgnoreCase("informative"))
    		_isINF =  true;
    	if (getModeration().equalsIgnoreCase("nws"))
    		_isNWS =  true;
    	if (getModeration().equalsIgnoreCase("political"))
    		_isPolitical =  true;
    }
    public boolean isNWS()
    {
    	return _isNWS;
    }
    public boolean isINF()
    {
    	return _isINF;
    }
    public boolean isPolitical()
    {
    	return _isPolitical ;
    }
    public void setOrder(int value)
    {
        _order = value;
    }
    
    public int getOrder()
    {
        return _order;
    }

    public Spannable getPreview()
    {
        return _preview;
    }
    
    public boolean getExpanded()
    {
    	return _expanded;
    }
    public void setExpanded(boolean setTo)
    {
    	_expanded = setTo;
    }
    public String getCopyText()
    {
        return PostFormatter.formatContent(this, true).toString();
    }
    
    public LolObj getLolObj()
    {
    	return _lolobj;
    }
    public void setLolObj(LolObj obj)
    {
    	_lolobj = obj;
    }
    // this stuff is a mild optimization. setusertype runs at load so we dont have to do this check on scrolling
    public void setUserType()
    {
    	if (User.isEmployee(getUserName()))
    		_userType = 2;
    	if (User.isModerator(getUserName()))
        	_userType = 1;
    }
    public boolean isFromModerator()
    {
    	if (_userType == 1)
    		return true;
    	return false;
    }
    public boolean isFromEmployee()
    {
    	if (_userType == 2)
    		return true;
    	return false;
    }
    
    @Override
    public int compareTo(Post post) {
        return _postId > post.getPostId() ? 1 : _postId < post.getPostId() ? -1 : 0;
    }
	public void setIsPQP(boolean isPQP) {
		_isPQP = isPQP;
	}
    public void setIsWorking(boolean isWorking) {
        _isWorking = isWorking;
    }
	public void setPostId(int postId) {
		_postId = postId;
	}
	public void setPosted(long posted)
	{
		_postedTime = posted;
	}
}
