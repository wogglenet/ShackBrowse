package net.woggle.shackbrowse;

import android.text.Spanned;

public class Message {

    private int _messageId;
    private String _userName;
    private Long _postedTime;
    private String _content;
    private Spanned _preview;
	private String _subject;
	private boolean _read;
	private String _rawContent;

    
    public Message(int msgId, String userName, String subject, String content, String rawContent, Long postedTime, boolean read)
    {
        _messageId = msgId;
        _userName = userName;
        _content = content;
        _rawContent = rawContent;
        _subject = subject;
        _postedTime = postedTime;
        _read = read;
    }

    public int getMessageId()
    {
        return _messageId;
    }
    
    public boolean getRead()
    {
    	return _read;
    }
    public void setRead(boolean set)
    {
    	_read = set;
    }

    public String getUserName()
    {
        return _userName;
    }

    public Long getPosted()
    {
        return _postedTime;
    }

    public String getContent()
    {
        return _content;
    }
    
    public String getRawContent()
    {
        return _rawContent;
    }
    
    public String getSubject()
    {
        return _subject;
    }

    public Spanned getPreview(boolean showTags, boolean stripNewLines)
    {
        if (_preview == null)
            _preview = PostFormatter.formatContent(this, !stripNewLines, showTags);
        return _preview;
    }

}
