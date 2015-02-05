package net.woggle.shackbrowse.legacy;

import android.os.Build;

public class LegacyFactory
{
    
    public static ILegacy getLegacy()
    {
        // this is a workaround for older versions of the API that are missing functionality
       int api = Build.VERSION.SDK_INT; 
       
       // use old junk for API 4
       if (api == 4)
           return new LegacyApi4();
       
       return new LegacyApi5();
    }

}
