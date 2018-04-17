package net.woggle.shackbrowse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import net.woggle.shackbrowse.notifier.NotifierReceiver;
import net.woggle.shackbrowse.notifier.ShackMessageNotifierReceiver;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * Created by brad on 6/22/2016.
 */
public class ShackMessageCheck
{
	private String _userName;
	private String _password;
	private boolean _verified;
	private SharedPreferences _prefs;
	private Context _context;

	public ShackMessageCheck(Context context)
	{
		_context = context;
		_prefs = PreferenceManager.getDefaultSharedPreferences(_context);
		_userName = _prefs.getString("userName", "");
		_password = _prefs.getString("password", "");
		_verified = _prefs.getBoolean("usernameVerified", false);
		System.out.println("SMCHK: init check");
	}

	public void frugalSMCheck()
	{
		System.out.println("SMCHK: trigger frugal");
		new CheckForSMTaskFrugal().execute();
	}
	public void startFullSMCheck()
	{
		System.out.println("SMCHK: trigger full");
		new CheckForSMTask().execute();
	}

	/*
	 * FRUGAL CHECK FOR SHACKMESSAGES
	 */
	class CheckForSMTaskFrugal extends AsyncTask<String, Void, Integer>
	{
		Exception _exception;
		String username;
		String text;
		int nlsid;


		@Override
		protected Integer doInBackground(String... params)
		{
			try
			{
				boolean verified = _prefs.getBoolean("usernameVerified", false);
				if (verified)
				{
					String userName = _prefs.getString("userName", null).trim();
					String password = _prefs.getString("password", null);
					String msgs = ShackApi.getShackMessageAPIText(userName, password);
					if (msgs.contentEquals(""))
						return 0;

					String md5cur = md5(msgs);
					String previous = _prefs.getString("PreviousMsgAPIReturnTextHash", null);
					if (previous == null)
					{
						SharedPreferences.Editor ed = _prefs.edit();
						ed.putString("PreviousMsgAPIReturnTextHash", md5cur);
						ed.commit();
						System.out.println("SMCHK: no previous found");
						return 1; // 1 == do full check. we cant confirm
					}
					else
					{
						if (!md5cur.contentEquals(previous))
						{
							// update previous
							SharedPreferences.Editor ed = _prefs.edit();
							ed.putString("PreviousMsgAPIReturnTextHash", md5cur);
							ed.commit();

							System.out.println("SMCHK: change found, full check");
							return 1;
						}
						else
							return 0; // no change from previous. no full check
					}
				}
				return 0;
			}
			catch (Exception e)
			{
				Log.e("shackbrowse", "Error getting sms", e);
				_exception = e;
				return 0;
			}
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			if (_exception != null)
			{
				System.out.println("SMCHK: err" + _exception.getMessage());
				ErrorDialog.display(_context, "Error", "Error getting SMs:\n" + _exception.getMessage());
			}
			else if (result == null)
			{
				System.out.println("SMCHK: errUnknown SM-related error");
				ErrorDialog.display(_context, "Error", "Unknown SM-related error.");
			}
			else
			{
				System.out.println("SMCHK: result: " + result);
				if (result == 1)
					startFullSMCheck();
			}
		}
	}

	/*
	 * CHECK FOR SHACKMESSAGES
	 */
	class CheckForSMTask extends AsyncTask<String, Void, Integer>
	{
		Exception _exception;
		String username;
		String text;
		int nlsid;


		@Override
		protected Integer doInBackground(String... params)
		{
			System.out.println("SMCHK: full check starting");
			try
			{
				boolean verified = _prefs.getBoolean("usernameVerified", false);
				if (verified)
				{
					ArrayList<Message> msgs = ShackApi.getMessages(0, _context);
					int unreadCount = 0;
					for (int i = 0; i < msgs.size(); i++)
					{
						if (msgs.get(i).getRead() == false)
						{
							if (unreadCount == 0)
							{
								username = msgs.get(i).getUserName();
								text = msgs.get(i).getRawContent();
								nlsid = msgs.get(i).getMessageId();
							}
							unreadCount++;
						}
					}
					return unreadCount;
				}
				return 0;
			}
			catch (Exception e)
			{
				Log.e("shackbrowse", "Error getting sms", e);
				_exception = e;
				return 0;
			}
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			if (_exception != null)
			{
				System.out.println("SMCHK: err");
				ErrorDialog.display(_context, "Error", "Error getting SMs:\n" + _exception.getMessage());
			}
			else if (result == null)
			{
				System.out.println("SMCHK: err");
				ErrorDialog.display(_context, "Error", "Unknown SM-related error.");
			}
			else
			{
				int lastNotifiedId = Integer.parseInt(_prefs.getString("GCMShackMsgLastClickedId", "0"));
				if (nlsid > lastNotifiedId)
				{
					text = PostFormatter.formatContent("", text, null, false, false).toString();
					if (result == 1)
						sendSMBroadcast(username, text, nlsid, false, 1);
					else if (result > 1)
						sendSMBroadcast("multiple", "", nlsid, true, result);
				}
			}
		}
	}


	// used for sending fake notifications to the notifierreceiver. check for sms locally, then send notification!
	private void sendSMBroadcast(String username, String text, int nlsid, boolean multiple, int howMany) {
		ShackMessageNotifierReceiver receiver = new ShackMessageNotifierReceiver();
		_context.registerReceiver(receiver, new IntentFilter( "net.woggle.fakenotification" ) );

		Intent broadcast = new Intent();
		broadcast.putExtra("type", "shackmsg");
		broadcast.putExtra("username", (multiple) ? "multiple" : username);
		broadcast.putExtra("text", (multiple) ? Integer.toString(howMany) : text);
		broadcast.putExtra("nlsid", Integer.toString(nlsid));
		broadcast.setAction("net.woggle.fakenotification");
		_context.sendBroadcast(broadcast);

		// unregisterReceiver(receiver);
	}

	public static String md5(String s)
	{
		MessageDigest digest;
		try
		{
			digest = MessageDigest.getInstance("MD5");
			digest.update(s.getBytes(Charset.forName("US-ASCII")),0,s.length());
			byte[] magnitude = digest.digest();
			BigInteger bi = new BigInteger(1, magnitude);
			String hash = String.format("%0" + (magnitude.length << 1) + "x", bi);
			return hash;
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		return "";
	}
}
