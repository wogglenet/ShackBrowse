package net.woggle.shackbrowse;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class PostQueueDB {
	// Database fields
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	private String[] allColumns = { DatabaseHelper.COLUMN_PID, DatabaseHelper.COLUMN_PREPLYTO, DatabaseHelper.COLUMN_PTEXT, DatabaseHelper.COLUMN_PFINALID, DatabaseHelper.COLUMN_PISMESSAGE, DatabaseHelper.COLUMN_PISNEWS, DatabaseHelper.COLUMN_PSUBJECT, DatabaseHelper.COLUMN_PRECIPIENT, DatabaseHelper.COLUMN_PFINALIZEDTIME };
	
	public PostQueueDB(Context context) {
		dbHelper = DatabaseHelper.getHelper(context);
	}
	
	public void open() throws SQLException {
		database = dbHelper.openDatabase();
	}
	
	public void close() {
		dbHelper.closeDatabase();
	}
	
	public PostQueueObj getPostQueueObj(int pqoId)
	{
		Cursor cursor = database.query(DatabaseHelper.TABLE_POSTQUEUE, allColumns, DatabaseHelper.COLUMN_PID + " = " + pqoId, null, null, null, null);
		cursor.moveToFirst();
		PostQueueObj returned = null;
		if (!cursor.isAfterLast())
		{
			returned = cursorToPost(cursor);
		}
		cursor.close();
		return returned;
	}
	
	public PostQueueObj createPost(PostQueueObj p) {
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.COLUMN_PTEXT, p.getBody());
		values.put(DatabaseHelper.COLUMN_PREPLYTO, p.getReplyToId());
		values.put(DatabaseHelper.COLUMN_PFINALID, p.getFinalId());
		values.put(DatabaseHelper.COLUMN_PISMESSAGE, p.getIsMessage());
		values.put(DatabaseHelper.COLUMN_PISNEWS, p.getIsNews());
		values.put(DatabaseHelper.COLUMN_PSUBJECT, p.getSubject());
		values.put(DatabaseHelper.COLUMN_PRECIPIENT, p.getRecipient());
		database.beginTransaction();
		PostQueueObj returned = null;
        try
        {
        	long postId = database.insert(DatabaseHelper.TABLE_POSTQUEUE, null, values);
    		Cursor cursor = database.query(DatabaseHelper.TABLE_POSTQUEUE, allColumns, DatabaseHelper.COLUMN_PID + " = " + postId, null, null, null, null);
    		cursor.moveToFirst();
    		returned = cursorToPost(cursor);
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
	
	public void updateFinalId(PostQueueObj p)
	{
		ContentValues newValues = new ContentValues();
		newValues.put(DatabaseHelper.COLUMN_PFINALID, p.getFinalId());
		newValues.put(DatabaseHelper.COLUMN_PFINALIZEDTIME, p.getFinalizedTime());
		database.update(DatabaseHelper.TABLE_POSTQUEUE, newValues, DatabaseHelper.COLUMN_PID + "=" + p.getPostQueueId(), null);
	}

	public void deletePost(PostQueueObj p) {
		long id = p.getPostQueueId();
		System.out.println("PostQueue deleted with id: " + id);
		database.beginTransaction();
		try
        {
			database.delete(DatabaseHelper.TABLE_POSTQUEUE, DatabaseHelper.COLUMN_PID + " = " + id, null);
	        
			database.setTransactionSuccessful();
        }
		catch (Exception e)
        {
        	
        }
        finally {
            database.endTransaction();
        }
		
	}
	
	public List<PostQueueObj> getAllPostsInQueue(boolean onlyUnsubmittedPosts) {
		List<PostQueueObj> ps = new ArrayList<PostQueueObj>();
		
		Cursor cursor;
		if (onlyUnsubmittedPosts)
			cursor = database.query(DatabaseHelper.TABLE_POSTQUEUE, allColumns, DatabaseHelper.COLUMN_PFINALID + " = 0", null, null, null, DatabaseHelper.COLUMN_PID + " ASC");
		else
			cursor = database.query(DatabaseHelper.TABLE_POSTQUEUE, allColumns, null, null, null, null, DatabaseHelper.COLUMN_PID + " ASC");
		
		cursor.moveToFirst();
		while (!cursor.isAfterLast())
		{
			PostQueueObj p = cursorToPost(cursor);
			ps.add(p);
			cursor.moveToNext();
		}
		System.out.println("LOADED PQS: " + ps.size());
		// make sure to close the cursor
		cursor.close();
		return ps;
	}
	private PostQueueObj cursorToPost(Cursor cursor) {
		PostQueueObj p = new PostQueueObj(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_PID)),cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PTEXT)),cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_PREPLYTO)),cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_PFINALID)), cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_PISMESSAGE)), cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_PISNEWS)), cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PSUBJECT)), cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PRECIPIENT)), cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_PFINALIZEDTIME)));
		return p;
	}
	
	public void resetAI()
	{
		database.execSQL("UPDATE SQLITE_SEQUENCE SET seq = 0 WHERE name = " + DatabaseHelper.TABLE_POSTQUEUE);
	}

	public void deleteAll() {
		database.delete(DatabaseHelper.TABLE_POSTQUEUE, null, null);
	}

	public void cleanUpFinalizedPosts() {
		// removes day old final posts from postqueue
		database.delete(DatabaseHelper.TABLE_POSTQUEUE, DatabaseHelper.COLUMN_PFINALID + " != 0 AND " + DatabaseHelper.COLUMN_PFINALIZEDTIME + " < " + (TimeDisplay.now() - (1000L * 60L * 60L * 24L)), null);
	}
}