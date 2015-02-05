package net.woggle.shackbrowse.legacy;

import android.content.Context;

public class LegacyApi4 implements ILegacy
{
    public boolean hasCamera(Context context)
    {
        // uh, sure this has a camera!
        return true;
    }

    public int getRequiredImageRotation(String path)
    {
        // looks good to me!
        return 0;
    }
}
