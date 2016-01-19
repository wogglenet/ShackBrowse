package net.woggle.shackbrowse;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.widget.Toast;

/**
 * Created by brad on 1/19/2016.
 */
// this receiver receives button callbacks from chrometabs
public class ChromeTabReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("GOT THING!");
        Bundle ext = intent.getExtras();
        if (ext.getBoolean("CopyUrl"))
        {
            copyText(ext.getString("Url"), context);
        }
        else if (ext.getBoolean("OpenInBrowser"))
        {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(ext.getString("Url")));
            context.startActivity(i);
        }
    }
    public void copyText(String text, Context context)
    {
        ClipboardManager clipboard = (ClipboardManager)context.getSystemService(Activity.CLIPBOARD_SERVICE);
        clipboard.setText(text);
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}