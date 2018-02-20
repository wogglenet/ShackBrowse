package net.woggle.shackbrowse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

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


	static double threadAgeInHours(Long original)
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
		double threadAge = TimeDisplay.threadAgeInHours(original);
		// threadage is in hours
		return (((int)(threadAge) > 0) ? Integer.toString((int)(threadAge)) + "h " : "") + (int)(60 * (threadAge - (long)(threadAge))) + "m ago";
	}
	static String doubleThreadAgeToString (double threadAge)
	{
		// threadage is in hours
		return (((int)(threadAge) > 0) ? Integer.toString((int)(threadAge)) + "h " : "") + (int)(60 * (threadAge - (long)(threadAge))) + "m ago";
	}

    public static int secondsSince(long posted)
    {
        int seconds = (int)((TimeDisplay.now() - posted) / 1000f);
        return seconds;
    }
    public static String secondsToNiceTime (int seconds)
    {
        int day = (int) TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) -
                TimeUnit.DAYS.toHours(day);
        long minute = TimeUnit.SECONDS.toMinutes(seconds) -
                TimeUnit.DAYS.toMinutes(day) -
                TimeUnit.HOURS.toMinutes(hours);
        long second = TimeUnit.SECONDS.toSeconds(seconds) -
                TimeUnit.DAYS.toSeconds(day) -
                TimeUnit.HOURS.toSeconds(hours) -
                TimeUnit.MINUTES.toSeconds(minute);
        /*
        int day = (int) TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) - (day *24);
        long minute = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds)* 60);
        long second = TimeUnit.SECONDS.toSeconds(seconds) - (TimeUnit.SECONDS.toMinutes(seconds) * 60); */
        return ((day > 0) ? day + "d" : "") + ((hours > 0L) ? " " + hours + "h" : "") + ((minute > 0L) ? " " + minute + "m" : "") + ((second > 0L) ? " " + second + "s" : "");
    }
    public static String getNiceTimeSince(Long posted, boolean showHoursSince) {
        String ret = "";
        final double threadAge = TimeDisplay.threadAgeInHours(posted);
        // set posted time
        if (threadAge <= 24f && showHoursSince)
        {
            // this is actually the same as the final else below, but this is the most common result
            ret = TimeDisplay.doubleThreadAgeToString(threadAge);
        }
        else {
            // check if this post is so old its not even the same year
            // threadage > 4380 == one half year. optimization to prevent getyear from being run on every thread
            if (threadAge > 4380f && !TimeDisplay.getYear(TimeDisplay.now()).equals(TimeDisplay.getYear(posted)))
            {
                // older than one year
                ret = TimeDisplay.convTime(posted, "MMM dd, yyyy h:mma zzz");
            }
            else
            {
                if ((!showHoursSince) || (threadAge > 24f)) {
                    if (TimeDisplay.threadAgeInHours(posted) > 96f) {
                        // default readout for !showsince or > 96h, has month
                        ret = TimeDisplay.convertTimeLong(posted);
                    }
                    else {
                        // has only day of week
                        ret = TimeDisplay.convertTime(posted);
                    }
                } else {
                    // standard less than 24h with showtimesince... this will actually always be caught by the first if as an optimization
                    ret = TimeDisplay.doubleThreadAgeToString(threadAge);
                }
            }
        }
        return ret;
    }

}
