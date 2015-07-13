package net.woggle.shackbrowse;

import android.graphics.Color;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import static net.woggle.shackbrowse.StatsFragment.statInc;

public class SpoilerSpan extends ClickableSpan
{
    static final int SPOILER_COLOR = Color.parseColor("#505050");
    static final int PREVSPOILER_COLOR = Color.parseColor("#404040");
    
    View _view;
    boolean _spoiled = false;
    int idInPost = 0;

	private boolean _preview;
    
    public SpoilerSpan(View view, int numberInPost, boolean preview)
    {
        _view = view;
        idInPost = numberInPost;
        _preview = preview;
    }
    
    public boolean getSpoiled ()
    {
    	return _spoiled;
    }
    
    @Override
    public void onClick(View widget)
    {
        // only spoil if the view was given
            statInc(widget.getContext(), "SpoilersClicked");
        
            _spoiled = true;
            // Toast.makeText(widget.getContext(), "spoiled:"+idInPost, 0).show();
            
            // so hacky i have nightmares at night
            
            ListView lv = (ListView)(widget.getParent().getParent().getParent().getParent().getParent().getParent());
            Post post = (Post)lv.getAdapter().getItem(lv.getPositionForView(widget));
            post.setSpoiled(idInPost, true);
            ((ThreadViewFragment.PostLoadingAdapter)((ListAdapter)lv.getAdapter())).notifyDataSetChanged();
    }

    @Override
    public void updateDrawState(TextPaint ds)
    {
        // if it hasn't been spoiled yet, hide the text
        if ((!_spoiled) && (!_preview))
        {
            ds.bgColor = SPOILER_COLOR;
            ds.setColor(SPOILER_COLOR);
        }
        else if (_preview)
        {
        	ds.bgColor = SPOILER_COLOR;
        }
    }
}
