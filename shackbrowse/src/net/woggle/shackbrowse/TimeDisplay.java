package net.woggle.shackbrowse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.util.Log;

public final class TimeDisplay {
	static String convTime(Long original, String format)
    {
        try
        {
            Date dt = new Date(original);
            SimpleDateFormat converter = new SimpleDateFormat(format);
            
            converter.setTimeZone(TimeZone.getDefault());
            return converter.format(dt);
        }
        catch (Exception ex)
        {
            Log.e("shackbrowse", "Error parsing date", ex);
        }
        
        return "Time Error";
    }
	static String convertTime(Long original)
	{
		return convTime(original, "E h:mma");
	}
	static String convertTimeLong(Long original)
    {
		return convTime(original, "MMM dd h:mma zzz");
    }
	public static Long now()
	{
		return System.currentTimeMillis();
	}
	static String getYear(Long original)
	{
		return convTime(original, "yyyy");
	}
	static double threadAge(Long original)
	{
		 try
	        {
			 	// returns in double representing hours since
			 	return ((double)(System.currentTimeMillis() - original) / 3600000d);
	        }
	        catch (Exception ex)
	        {
	            Log.e("shackbrowse", "Error parsing date", ex);
	        }
	        
	        return 0d;
	}
	static String postedLongToThreadAgeToString (Long original)
	{
		double threadAge = TimeDisplay.threadAge(original);
		// threadage is in hours
		return (((int)(threadAge) > 0) ? Integer.toString((int)(threadAge)) + "h " : "") + (int)(60 * (threadAge - (long)(threadAge))) + "m ago";
	}
	static String doubleThreadAgeToString (double threadAge)
	{
		// threadage is in hours
		return (((int)(threadAge) > 0) ? Integer.toString((int)(threadAge)) + "h " : "") + (int)(60 * (threadAge - (long)(threadAge))) + "m ago";
	}
}
