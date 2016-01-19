package net.woggle.shackbrowse;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

public class LolObj {
	private int _lol = 0;
	private int _inf = 0;
	private int _unf = 0;
	private int _tag = 0;
	private int _wtf = 0;
	private int _ugh = 0;
	private SpannedString _tagSpan;
	private Context _context;
	private static int getIntFromSerialString(String string, int part)
	{
		String[] parts = string.split(":");
		return Integer.parseInt(parts[part]);
	}
	LolObj()
	{
		this(0,0,0,0,0,0);
	}
	LolObj(String serial)
	{
		this(getIntFromSerialString(serial,0),getIntFromSerialString(serial,1),getIntFromSerialString(serial,2),getIntFromSerialString(serial,3),getIntFromSerialString(serial,4),getIntFromSerialString(serial,5));
	}
	LolObj(int lol, int inf, int unf, int tag, int wtf, int ugh) {
		setLol(lol); setInf(inf); setUnf(unf); setTag(tag); setWtf(wtf); setUgh(ugh);
	}
	public String lolObjToString()
	{
		return getLol() + ":" + getInf() + ":" +getUnf() + ":" + getTag() + ":" + getWtf() + ":" + getUgh();
	}
	public int getLol() { return _lol; }
	public int getInf() { return _inf; }
	public int getUnf() { return _unf; }
	public int getTag() { return _tag; }
	public int getWtf() { return _wtf; }
	public int getUgh() { return _ugh; }
	public void setLol(int val) { _lol = val; }
	public void setInf(int val) { _inf = val; }
	public void setUnf(int val) { _unf = val; }
	public void setTag(int val) { _tag = val; }
	public void setWtf(int val) { _wtf = val; }
	public void setUgh(int val) { _ugh = val; }
    public void incLol() { _lol = _lol+1; }
	public void incInf() { _inf = _inf+1; }
	public void incUnf() { _unf = _unf+1; }
	public void incTag() { _tag = _tag+1; }
	public void incWtf() { _wtf = _wtf+1; }
	public void incUgh() { _ugh = _ugh+1; }
	public void decLol() { _lol = _lol-1; }
	public void decInf() { _inf = _inf-1; }
	public void decUnf() { _unf = _unf-1; }
	public void decTag() { _tag = _tag-1; }
	public void decWtf() { _wtf = _wtf-1; }
	public void decUgh() { _ugh = _ugh-1; }
	public void clear () { _lol = 0; _inf = 0; _unf = 0; _tag = 0; _wtf = 0; _ugh = 0; }
	public SpannedString getTagSpan()
	{
		return _tagSpan;
	}
	public void genTagSpan(Context context)
	{
		_tagSpan = makeTagSpan(context);
	}
	public SpannedString makeTagSpan(Context context) {
		SpannableString lol = new SpannableString(""), inf = new SpannableString(""), unf = new SpannableString(""), tag = new SpannableString(""), wtf = new SpannableString(""), ugh = new SpannableString("");
		
		if (getLol() > 0)
		{
			lol = new SpannableString(" lol "+Integer.toString(getLol())+" ");
			lol.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.shacktag_lol)), 0, lol.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		if (getInf() > 0)
		{
			inf = new SpannableString(" inf " +Integer.toString(getInf())+" ");
			inf.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.shacktag_inf)), 0, inf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		if (getUnf() > 0)
		{
			unf = new SpannableString(" unf "+Integer.toString(getUnf())+" ");
			unf.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.shacktag_unf)), 0, unf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		if (getTag() > 0)
		{
			tag = new SpannableString(" tag "+Integer.toString(getTag())+" ");
			tag.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.shacktag_tag)), 0, tag.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		if (getWtf() > 0)
		{
			wtf = new SpannableString(" wtf "+Integer.toString(getWtf())+" ");
			wtf.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.shacktag_wtf)), 0, wtf.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		if (getUgh() > 0)
		{
			ugh = new SpannableString(" ugh "+Integer.toString(getUgh())+" ");
			ugh.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.shacktag_ugh)), 0, ugh.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}


        SpannedString formatted = (SpannedString)TextUtils.concat(lol,inf,unf,tag,wtf,ugh);
        
        return formatted;
	}
}
