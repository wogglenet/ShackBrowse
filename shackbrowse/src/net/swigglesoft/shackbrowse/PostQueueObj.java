package net.swigglesoft.shackbrowse;

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
	private int _contenttypeid;
	private long _finalizedtime;

	static public PostQueueObj FromDB(int postQueueObjId, Context ctx)
	{
		PostQueueDB pdb = new PostQueueDB(ctx);
		pdb.open();
		PostQueueObj returned = pdb.getPostQueueObj(postQueueObjId);
		pdb.close();
		return returned;
	}
	public PostQueueObj(int replyto, String body, int contentTypeId)
	{
		this(0L, body, replyto, 0, 0, contentTypeId, null, null, 0L);
	}
	public PostQueueObj(String subject, String recipient, String body)
	{
		this(0L, body, 0, 0, 1, 0, subject, recipient, 0L);
	}
	public PostQueueObj(long postid, String body, int replyto, int fid, int ismessage, int contenttypeid, String subject, String recipient, long finaltime)
	{
		_finalizedtime = finaltime; _postid = postid; _body = body; _replyto = replyto; _finalid = fid; _ismessage = ismessage; _contenttypeid = contenttypeid; _subject = subject; _recipient = recipient;
	}
	public String getBody() { return _body; }
	public String getSubject() { return _subject; }
	public String getRecipient() { return _recipient; }
	public int getReplyToId() { return _replyto; }
	public long getPostQueueId() { return _postid; }
	boolean isMessage() { return _ismessage == 1 ? true : false; }
	String getContentTypeId() { return Integer.toString(_contenttypeid); }
	public int getIsMessage() { return _ismessage; }
	public int getIntContentTypeId() { return _contenttypeid; }
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