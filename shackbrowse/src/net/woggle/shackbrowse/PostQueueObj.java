package net.woggle.shackbrowse;

import android.content.Context;

public class PostQueueObj
{
	private String _body;
	private String _subject;
	private String _recipient;
	private int _replyto;
	private int _ismessage;
	private long _postid;
	private int _finalid;
	private int _isnews;
	private long _finalizedtime;

	static public PostQueueObj FromDB(int postQueueObjId, Context ctx)
	{
		PostQueueDB pdb = new PostQueueDB(ctx);
		pdb.open();
		PostQueueObj returned = pdb.getPostQueueObj(postQueueObjId);
		pdb.close();
		return returned;
	}
	public PostQueueObj(int replyto, String body, boolean isNews)
	{
		this(0L, body, replyto, 0, 0, isNews ? 1 : 0, null, null, 0L);
	}
	public PostQueueObj(String subject, String recipient, String body)
	{
		this(0L, body, 0, 0, 1, 0, subject, recipient, 0L);
	}
	public PostQueueObj(long postid, String body, int replyto, int fid, int ismessage, int isnews, String subject, String recipient, long finaltime)
	{
		_finalizedtime = finaltime; _postid = postid; _body = body; _replyto = replyto; _finalid = fid; _ismessage = ismessage; _isnews = isnews; _subject = subject; _recipient = recipient;
	}
	public String getBody() { return _body; }
	public String getSubject() { return _subject; }
	public String getRecipient() { return _recipient; }
	public int getReplyToId() { return _replyto; }
	public long getPostQueueId() { return _postid; }
	boolean isMessage() { return _ismessage == 1 ? true : false; }
	boolean isNews() { return _isnews == 1 ? true : false; }
	public int getIsMessage() { return _ismessage; }
	public int getIsNews() { return _isnews; }
	public void setFinalId(int fid) { _finalid = fid; }
	int getFinalId() { return _finalid; }
	
	public void create(Context con)
	{
		PostQueueDB pdb = new PostQueueDB(con);
		pdb.open();
		PostQueueObj post = pdb.createPost(this);
		_postid = post.getPostQueueId();
		pdb.close();
	}
	public void commitDelete(Context con)
	{
		PostQueueDB pdb = new PostQueueDB(con);
		pdb.open();
		pdb.deletePost(this);
		pdb.close();
	}
	public void updateFinalId(Context ctx) {
		PostQueueDB pdb = new PostQueueDB(ctx);
		pdb.open();
		pdb.updateFinalId(this);
		pdb.close();
	}
	public long getFinalizedTime() {
		return _finalizedtime;
	}
	public void setFinalizedTime(long finaltime) {
		_finalizedtime = finaltime;
	}
}