package net.woggle.shackbrowse;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by brad on 7/1/2016.
 */
public class OfflineDB
{
	// Database fields
	private SQLiteDatabase database;
	private DatabaseHelper dbH;
	private String[] allColumns = { DatabaseHelper.COLUMN_SID, DatabaseHelper.COLUMN_STID, DatabaseHelper.COLUMN_SJSON, DatabaseHelper.COLUMN_SPOSTEDTIME };

	public OfflineDB(Context context) {
		dbH = new DatabaseHelper(context);
	}

	public void open() throws SQLException
	{
		database = dbH.getWritableDatabase();
	}

	public void close() {
		dbH.close();
	}

	public SavedThreadObj getSavedThread(int stId)
	{
		Cursor cursor = database.query(DatabaseHelper.TABLE_STARRED, allColumns, DatabaseHelper.COLUMN_STID + " = " + stId, null, null, null, null);
		cursor.moveToFirst();
		SavedThreadObj returned = null;
		if (!cursor.isAfterLast())
		{
			returned = cursorToSavedThread(cursor);
		}
		cursor.close();
		return returned;
	}

	public Hashtable<Integer, SavedThreadObj> getAllSavedThreads() {

		Hashtable<Integer, SavedThreadObj> st = new Hashtable<Integer, SavedThreadObj>();

		Cursor cursor;
		cursor = database.query(DatabaseHelper.TABLE_STARRED, allColumns, null, null, null, null, null);

		cursor.moveToFirst();
		while (!cursor.isAfterLast())
		{
			SavedThreadObj t = cursorToSavedThread(cursor);
			st.put(t._rootId,t);
			cursor.moveToNext();
		}
		System.out.println("LOADED SavedTheadObjs: " + st.size());
		// make sure to close the cursor
		cursor.close();
		return st;
	}

	private SavedThreadObj cursorToSavedThread(Cursor cursor) {
		SavedThreadObj t = new SavedThreadObj(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_STID)),cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_SPOSTEDTIME)),cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_SJSON)));
		return t;
	}

	public SavedThreadObj createSavedThreadDBFromSavedThreadObj(SavedThreadObj t) {
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.COLUMN_STID, t._rootId);
		values.put(DatabaseHelper.COLUMN_SJSON, t._threadJson.toString());
		values.put(DatabaseHelper.COLUMN_SPOSTEDTIME, t._postTime);
		database.beginTransaction();
		SavedThreadObj returned = null;
		try
		{
			long sId = database.insert(DatabaseHelper.TABLE_STARRED, null, values);
			Cursor cursor = database.query(DatabaseHelper.TABLE_STARRED, allColumns, DatabaseHelper.COLUMN_SID + " = " + sId, null, null, null, null);
			cursor.moveToFirst();
			returned = cursorToSavedThread(cursor);
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

	public void deleteAll() {
		database.delete(DatabaseHelper.TABLE_STARRED, null, null);
	}
}
