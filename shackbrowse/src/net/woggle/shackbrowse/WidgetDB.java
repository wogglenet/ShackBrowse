package net.woggle.shackbrowse;

/**
 * Created by brad on 6/21/2016.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class WidgetDB {
	// Database fields
	private SQLiteDatabase database;
	private DatabaseHelper dbH;
	private String[] allColumns = { DatabaseHelper.COLUMN_WID, DatabaseHelper.COLUMN_WTID, DatabaseHelper.COLUMN_WPOSTER, DatabaseHelper.COLUMN_WTEXT, DatabaseHelper.COLUMN_WREPLYCOUNT, DatabaseHelper.COLUMN_WREPLIED, DatabaseHelper.COLUMN_WMODERATION, DatabaseHelper.COLUMN_WLOLOBJ, DatabaseHelper.COLUMN_WPOSTEDTIME, DatabaseHelper.COLUMN_WHOT };

	public WidgetDB(Context context) {
		dbH = DatabaseHelper.getHelper(context);
	}

	public void open() throws SQLException
	{
		database = dbH.openDatabase();
	}

	public void close() {
		dbH.closeDatabase();
	}

	public Thread getThread(int wId)
	{
		Cursor cursor = database.query(DatabaseHelper.TABLE_WIDGET, allColumns, DatabaseHelper.COLUMN_WID + " = " + wId, null, null, null, null);
		cursor.moveToFirst();
		Thread returned = null;
		if (!cursor.isAfterLast())
		{
			returned = cursorToThread(cursor);
		}
		cursor.close();
		return returned;
	}

	public ArrayList<Thread> getAllThreads(boolean byHot) {
		ArrayList<Thread> tl = new ArrayList<Thread>();

		Cursor cursor;
		cursor = database.query(DatabaseHelper.TABLE_WIDGET, allColumns, null, null, null, null, (byHot ? DatabaseHelper.COLUMN_WHOT : DatabaseHelper.COLUMN_WREPLYCOUNT) + " DESC");

		cursor.moveToFirst();
		while (!cursor.isAfterLast())
		{
			Thread t = cursorToThread(cursor);
			tl.add(t);
			cursor.moveToNext();
		}
		System.out.println("LOADED TL: " + tl.size());
		// make sure to close the cursor
		cursor.close();
		return tl;
	}

	private Thread cursorToThread(Cursor cursor) {
		Thread t = new Thread(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_WTID)),cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_WPOSTER)),cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_WTEXT)),cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_WPOSTEDTIME)), cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_WREPLYCOUNT)), cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_WMODERATION)), (cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_WREPLIED)) != 0), false);
		t.setLolObj(new LolObj(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_WLOLOBJ))));
		return t;
	}

	public Thread createWidgetDBFromThread(Thread t) {
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.COLUMN_WTID, t.getThreadId());
		values.put(DatabaseHelper.COLUMN_WPOSTER, t.getUserName());
		values.put(DatabaseHelper.COLUMN_WTEXT, t.getContent());
		values.put(DatabaseHelper.COLUMN_WPOSTEDTIME, t.getPosted());
		values.put(DatabaseHelper.COLUMN_WREPLYCOUNT, t.getReplyCount());
		values.put(DatabaseHelper.COLUMN_WMODERATION, t.getModeration());
		values.put(DatabaseHelper.COLUMN_WREPLIED, t.getReplied());
		values.put(DatabaseHelper.COLUMN_WLOLOBJ, t.getLolObj().lolObjToString());
		values.put(DatabaseHelper.COLUMN_WHOT, (long)((t.getReplyCount() / TimeDisplay.threadAgeInHours(t.getPosted())) * 100f));
		database.beginTransaction();
		Thread returned = null;
		try
		{
			long wId = database.insert(DatabaseHelper.TABLE_WIDGET, null, values);
			Cursor cursor = database.query(DatabaseHelper.TABLE_WIDGET, allColumns, DatabaseHelper.COLUMN_WID + " = " + wId, null, null, null, null);
			cursor.moveToFirst();
			returned = cursorToThread(cursor);
			cursor.close();
			database.setTransactionSuccessful();
		}
		catch (Exception e)
		{

		}
		finally {
			database.endTransaction();
		}

		return returned;
	}

	public void resetAI()
	{
		database.execSQL("UPDATE SQLITE_SEQUENCE SET seq = 0 WHERE name = " + DatabaseHelper.TABLE_WIDGET);
	}

	public void deleteAll() {
		database.delete(DatabaseHelper.TABLE_WIDGET, null, null);
	}

}