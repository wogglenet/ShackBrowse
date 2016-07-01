package net.woggle.shackbrowse;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

  public static final String TABLE_NOTES = "notes";
  public static final String COLUMN_NUNIQUE = "n_id";
  public static final String COLUMN_NTYPE = "n_type";
  public static final String COLUMN_NPOSTID = "n_postid";
  public static final String COLUMN_NBODY = "n_body";
  public static final String COLUMN_NAUTHOR = "n_author";
  public static final String COLUMN_NTIME = "n_time";
  public static final String COLUMN_NKW = "n_keyword";

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

  public static final String TABLE_WIDGET = "widget";
  public static final String COLUMN_WID = "w_id";
  public static final String COLUMN_WTID = "w_tid";
  public static final String COLUMN_WTEXT = "w_text";
  public static final String COLUMN_WREPLIED = "w_replied";
  public static final String COLUMN_WPOSTER = "w_poster";
  public static final String COLUMN_WREPLYCOUNT = "w_replycount";
  public static final String COLUMN_WPOSTEDTIME = "w_posted";
  public static final String COLUMN_WMODERATION = "w_moderation";
  public static final String COLUMN_WLOLOBJ = "w_lolobj";
  public static final String COLUMN_WHOT = "w_hot";

  public static final String TABLE_STARRED = "starred";
  public static final String COLUMN_SID= "s_id";
  public static final String COLUMN_STID = "s_root";
  public static final String COLUMN_SJSON = "s_json";
  public static final String COLUMN_SPOSTEDTIME = "s_posted";

  private static final String DATABASE_NAME = "shkbrs3.db";
  private static final int DATABASE_VERSION = 11;

  // Database creation sql statement
  private static final String DATABASE_CREATE = "create table "
      + TABLE_NOTES + "("
          + COLUMN_NUNIQUE + " integer primary key,"
          + COLUMN_NPOSTID + " integer,"
          + COLUMN_NTYPE + " text not null,"
          + COLUMN_NBODY + " text not null,"
          + COLUMN_NAUTHOR + " text not null, "
          + COLUMN_NTIME + " integer, "
          + COLUMN_NKW + " text not null);";
  private static final String DATABASE_CREATE2 =  "create table "
      + TABLE_POSTQUEUE + "(" + COLUMN_PID
      + " integer primary key autoincrement, " + COLUMN_PTEXT
      + " text not null, " + COLUMN_PREPLYTO + " integer, " + COLUMN_PFINALID + " integer, " + COLUMN_PISMESSAGE + " integer, " + COLUMN_PISNEWS + " integer, " + COLUMN_PSUBJECT + " text, " + COLUMN_PRECIPIENT + " text, " + COLUMN_PFINALIZEDTIME + " integer);";

  private static final String DATABASE_CREATE3 =  "create table "
          + TABLE_WIDGET + "(" + COLUMN_WID
          + " integer primary key autoincrement, " + COLUMN_WTID + " integer, " + COLUMN_WTEXT
          + " text not null, " + COLUMN_WREPLYCOUNT + " integer, " + COLUMN_WLOLOBJ + " text, " + COLUMN_WREPLIED + " integer, " + COLUMN_WPOSTEDTIME + " integer, " + COLUMN_WPOSTER + " text, " + COLUMN_WMODERATION + " text, " + COLUMN_WHOT + " integer);";

  private static final String DATABASE_MAKEINDEX_FOR_DB3 = "CREATE INDEX widget_replycount_idx ON " + TABLE_WIDGET + " (" + COLUMN_WREPLYCOUNT + ");";
  private static final String DATABASE_MAKEINDEX2_FOR_DB3 = "CREATE INDEX widget_hot_idx ON " + TABLE_WIDGET + " (" + COLUMN_WHOT + ");";

  private static final String DATABASE_CREATE4 =  "create table "
          + TABLE_STARRED + "(" + COLUMN_SID
          + " integer primary key autoincrement, " + COLUMN_STID + " integer, " + COLUMN_SJSON
          + " text not null, " + COLUMN_SPOSTEDTIME + " integer);";

  private static final String DATABASE_MAKEINDEX_FOR_DB4 = "CREATE INDEX starred_threadid_idx ON " + TABLE_STARRED + " (" + COLUMN_STID + ");";

  public DatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase database) {
    database.execSQL(DATABASE_CREATE);
    database.execSQL(DATABASE_CREATE2);
    database.execSQL(DATABASE_CREATE3);
    database.execSQL(DATABASE_CREATE4);
    database.execSQL(DATABASE_MAKEINDEX_FOR_DB3);
    database.execSQL(DATABASE_MAKEINDEX2_FOR_DB3);
    database.execSQL(DATABASE_MAKEINDEX_FOR_DB4);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.w(DatabaseHelper.class.getName(),
        "Upgrading database from version " + oldVersion + " to "
            + newVersion + ", which will destroy all old data");
    if ((oldVersion == 10) && (newVersion == 11))
    {
      db.execSQL(DATABASE_CREATE4);
      db.execSQL(DATABASE_MAKEINDEX_FOR_DB4);
    }
    else if ((oldVersion == 9) && (newVersion == 11))
    {
      db.execSQL(DATABASE_CREATE3);
      db.execSQL(DATABASE_MAKEINDEX_FOR_DB3);
      db.execSQL(DATABASE_MAKEINDEX2_FOR_DB3);
      db.execSQL(DATABASE_CREATE4);
      db.execSQL(DATABASE_MAKEINDEX_FOR_DB4);
    }
    else
    {
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTQUEUE);
	    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
	    onCreate(db);
    }
  }

} 