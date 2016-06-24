package net.woggle.shackbrowse;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class NotificationsDB {
	// Database fields
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	private String[] allColumns = { DatabaseHelper.COLUMN_NUNIQUE, DatabaseHelper.COLUMN_NPOSTID, DatabaseHelper.COLUMN_NTYPE, DatabaseHelper.COLUMN_NBODY, DatabaseHelper.COLUMN_NAUTHOR, DatabaseHelper.COLUMN_NTIME, DatabaseHelper.COLUMN_NKW };
	
	public NotificationsDB(Context context) {
		dbHelper = new DatabaseHelper(context);
	}
	
	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}
	
	public void close() {
		dbHelper.close();
	}
	
	public void createNote(NotificationObj note) {
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.COLUMN_NTYPE, note.getType());
		values.put(DatabaseHelper.COLUMN_NBODY, note.getBody());
		values.put(DatabaseHelper.COLUMN_NAUTHOR, note.getAuthor());
		values.put(DatabaseHelper.COLUMN_NPOSTID, note.getPostId());
		values.put(DatabaseHelper.COLUMN_NTIME, note.getTime());
		values.put(DatabaseHelper.COLUMN_NKW, note.getKeyword());

        try
        {
            long returnid = database.insertOrThrow(DatabaseHelper.TABLE_NOTES, null, values);
            note._uniqueid = returnid;
        }
        catch(SQLException e)
        {
            // Sep 12, 2013 6:50:17 AM
            System.out.println("SQLException"+String.valueOf(e.getMessage()));
            e.printStackTrace();
        }

    		/*
    		Cursor cursor = database.query(DatabaseHelper.TABLE_NOTES, allColumns, DatabaseHelper.COLUMN_NPOSTID + " = " + note.getPostId(), null, null, null, null);

    		cursor.moveToFirst();
    		returned = cursorToNote(cursor);
    		*/
        // System.out.println("TIMES " + returnid + " " + note.getTime());
        /*
		database.beginTransaction();
		NotificationObj returned = null;
        try
        {

    		// cursor.close();
            database.setTransactionSuccessful();
        }
        catch (Exception e)
        {
        	
        }
        finally {
            database.endTransaction();
        }
        */
	}

	public void deleteNote(NotificationObj note) {
		long id = note.getUniqueId();
		System.out.println("Comment deleted with id: " + id);
		database.delete(DatabaseHelper.TABLE_NOTES, DatabaseHelper.COLUMN_NUNIQUE
		+ " = " + id, null);
	}

	public List<NotificationObj> getAllNotes() {
		List<NotificationObj> notes = new ArrayList<NotificationObj>();
	
		Cursor cursor = database.query(DatabaseHelper.TABLE_NOTES, allColumns, null, null, null, null, DatabaseHelper.COLUMN_NTIME + " DESC");
	
		cursor.moveToFirst();
		while (!cursor.isAfterLast())
		{
			NotificationObj note = cursorToNote(cursor);
			notes.add(note);
			cursor.moveToNext();
		}
		System.out.println("LOADED NOTES: " + notes.size());
		// make sure to close the cursor
		cursor.close();
		return notes;
	}
	public List<NotificationObj> getNew(String type, String keyword, int limit) {
		List<NotificationObj> notes = new ArrayList<NotificationObj>();
		
		
		Cursor cursor;
		if (keyword != null)
			cursor = database.query(DatabaseHelper.TABLE_NOTES, allColumns, DatabaseHelper.COLUMN_NTYPE+"=? AND " + DatabaseHelper.COLUMN_NKW + "=?", new String[]{type, keyword}, null, null, DatabaseHelper.COLUMN_NTIME + " DESC", Integer.toString(limit));
		else
			cursor = database.query(DatabaseHelper.TABLE_NOTES, allColumns, DatabaseHelper.COLUMN_NTYPE+"=?", new String[]{type}, null, null, DatabaseHelper.COLUMN_NTIME + " DESC", Integer.toString(limit));

		cursor.moveToFirst();
		while (!cursor.isAfterLast())
		{
			NotificationObj note = cursorToNote(cursor);
			notes.add(note);
			cursor.moveToNext();
		}
		System.out.println("LOADED NOTES: " + notes.size());
		// make sure to close the cursor
		cursor.close();
		return notes;
	}
	private NotificationObj cursorToNote(Cursor cursor) {
		NotificationObj note = new NotificationObj(cursor.getInt(0),cursor.getInt(1),cursor.getString(2),cursor.getString(3),cursor.getString(4),cursor.getLong(5), cursor.getString(6));
		return note;
	}

	public void deleteAll() {
		database.delete(DatabaseHelper.TABLE_NOTES, null, null);
	}

	public void pruneNotes() // keep only most recent 200
	{
		database.execSQL("DELETE FROM "+DatabaseHelper.TABLE_NOTES+" WHERE ROWID IN (SELECT ROWID FROM "+DatabaseHelper.TABLE_NOTES+" ORDER BY ROWID DESC LIMIT -1 OFFSET 300);");
	}
}