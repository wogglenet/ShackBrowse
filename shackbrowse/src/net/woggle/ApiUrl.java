package net.woggle;

public class ApiUrl {
	String mUrl;
	boolean mIsV2;
	public ApiUrl(String url, boolean isV2)
	{
		mUrl = url; mIsV2 = isV2;
	}
	public String getUrl () { return mUrl; }
	public boolean isV2() { return  mIsV2; }
}
