package net.woggle.shackbrowse;

import android.content.Context;

public class NotificationObj
{
	private String _body;
	private String _type;
	private String _author;
	private String _keyword;
	private int _postid;
	private long _time;
    public long _uniqueid = -2;
    public NotificationObj(int postid, String type, String body, String author, long time, String keyword)
    {
        this(0, postid, type, body, author, time, keyword);
    }

	public NotificationObj(long uniqueid, int postid, String type, String body, String author, long time, String keyword)
	{
		_type = type; _body = body; _author = author; _postid = postid; _time = time; _keyword = keyword; _uniqueid = uniqueid;

	}
	public String getBody() { return _body; }
	public String getType() { return _type; }
	public String getAuthor() { return _author; }
	public String getKeyword() { return _keyword; }
	int getPostId() { return _postid; }
	long getTime() { return _time; }
    long getUniqueId() { return _uniqueid; }
	public void commit(Context con)
	{
        System.out.println("COMMITTING : " + _type + " " + _body);
		NotificationsDB ndb = new NotificationsDB(con);
		ndb.open();
		// ndb.deleteNote(this);
		ndb.createNote(this);
		ndb.close();
	}
}