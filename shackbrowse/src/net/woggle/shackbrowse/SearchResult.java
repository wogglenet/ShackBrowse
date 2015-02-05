package net.woggle.shackbrowse;

public class SearchResult
{
    private int _postId;
    private String _author;
    private String _content;
    private Long _postedTime;
    private int _type;
    private int _extra;
    private boolean _new = false;
    
    public static int TYPE_SHACKSEARCHRESULT = 1;
    public static int TYPE_LOL = 2;
    public static int TYPE_TAG = 3;
    public static int TYPE_INF = 4;
    public static int TYPE_UNF = 5;
    public static int TYPE_WTF = 6;
    public static int TYPE_UGH = 7;
    public static int TYPE_DRAFTS = 8;
    
    public SearchResult(int postId, String author, String content, Long posted, int type, int extra)
    {
        _postId = postId;
        _author = author;
        _content = content;
        _postedTime = posted;
        _type = type;
        _extra = extra;
    }
    
    public boolean getNew()
    {
    	return _new;
    }
    
    public void setNew(boolean set)
    {
    	_new = set;
    }
    
    public int getPostId()
    {
        return _postId;
    }
    
    public int getType()
    {
        return _type;
    }
    
    public int getExtra()
    {
        return _extra;
    }
    
    public String getAuthor()
    {
        return _author;
    }
    
    public String getContent()
    {
        return _content;
    }
    
    public Long getPosted()
    {
        return _postedTime;
    }

}
