package net.woggle.shackbrowse;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

public class BackupAgent extends BackupAgentHelper 
{
	static final String MY_PREFS_BACKUP_KEY = "preferences";
	
	public void onCreate()
	{
		SharedPreferencesBackupHelper helper = new SharedPreferencesBackupHelper(this, getPreferencesName());
		addHelper(MY_PREFS_BACKUP_KEY, helper);
	}
	
	String getPreferencesName()
	{
	    return this.getPackageName() + "_preferences";
	}
	
}
