package net.woggle.shackbrowse.legacy;

import android.content.Context;

public interface ILegacy
{
    boolean hasCamera(Context context);
    int getRequiredImageRotation(String path);
}
