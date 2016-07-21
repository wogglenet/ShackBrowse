package net.woggle.shackbrowse;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
* Created by brad on 7/20/2016.
*/

public class DatabaseHelperPQP extends SQLiteOpenHelper
{

	private static DatabaseHelperPQP instance;
	private int mOpenCounter = 0;
	private SQLiteDatabase mDatabase;

	public static synchronized DatabaseHelperPQP getHelper(Context context)
	{
		if (instance == null)
			instance = new DatabaseHelperPQP(context);

		return instance;
	}

	public synchronized SQLiteDatabase openDatabase() {
		mOpenCounter++;
		if(mOpenCounter == 1) {
			// Opening new database
			mDatabase = getWritableDatabase();
		}
		return mDatabase;
	}

	public synchronized void closeDatabase() {
		mOpenCounter--;
		if(mOpenCounter == 0) {
			// Closing database
			mDatabase.close();
		}
	}


	public static final String TABLE_POSTQUEUE = "postqueue";
	public static final String COLUMN_PID = "p_id";
	public static final String COLUMN_PTEXT = "p_text";
	public static final String COLUMN_PREPLYTO = "p_parentid";
	public static final String COLUMN_PFINALID = "p_finalid";
	public static final String COLUMN_PISMESSAGE = "p_ismessage";
	public static final String COLUMN_PISNEWS = "p_isnews";
	public static final String COLUMN_PSUBJECT = "p_subject";
	public static final String COLUMN_PRECIPIENT = "p_recipient";
	public static final String COLUMN_PFINALIZEDTIME = "p_finalizedtime";

	private static final String DATABASE_NAME = "shkbrsPQP.db";
	private static final int DATABASE_VERSION = 1;

	// Database creation sql statement

	private static final String DATABASE_PQP =  "create table "
			+ TABLE_POSTQUEUE + "(" + COLUMN_PID
			+ " integer primary key autoincrement, " + COLUMN_PTEXT
			+ " text not null, " + COLUMN_PREPLYTO + " integer, " + COLUMN_PFINALID + " integer, " + COLUMN_PISMESSAGE + " integer, " + COLUMN_PISNEWS + " integer, " + COLUMN_PSUBJECT + " text, " + COLUMN_PRECIPIENT + " text, " + COLUMN_PFINALIZEDTIME + " integer);";

	public DatabaseHelperPQP(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_PQP);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(DatabaseHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		if ((oldVersion == 1) && (newVersion == 2))
		{
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTQUEUE);
			onCreate(db);
		}
	}

}
