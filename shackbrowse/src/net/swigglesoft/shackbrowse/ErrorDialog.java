package net.swigglesoft.shackbrowse;

import android.content.Context;
import android.widget.Toast;

public class ErrorDialog implements Runnable
{
    private Context _context;
    private String _title;
    private String _text;
    
    public ErrorDialog(Context context, String title, String text)
    {
        _context = context;
        _title = title;
        _text = text;
    }

    @Override
    public void run()
    {
        display(_context, _title, _text);
    }
    public static void display(Context context, String title, String text)
    {
    	System.out.println("ERROR DIALOG: " + title + " [] " + text);
    	Toast.makeText(context.getApplicationContext(), title + ": " + text, Toast.LENGTH_SHORT).show();
    }
}
