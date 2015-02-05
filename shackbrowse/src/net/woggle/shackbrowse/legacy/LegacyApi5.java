package net.woggle.shackbrowse.legacy;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.ExifInterface;
import android.util.Log;

public class LegacyApi5 implements ILegacy
{

    @Override
    public boolean hasCamera(Context context)
    {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    @Override
    public int getRequiredImageRotation(String path)
    {
        try
        {
            ExifInterface exif = new ExifInterface(path);
            int orientation = Integer.parseInt(exif.getAttribute(ExifInterface.TAG_ORIENTATION));
            
            // only handle common cases
            switch (orientation)
            {
                case 3:
                    return 180;
                case 6:
                    return 90;
                case 8:
                    return 270;
            }
        }
        catch (Exception ex)
        {
            Log.e("shackbrowse", "Couldn't determine image orientation", ex);
        }
        
        return 0;
    }

}
