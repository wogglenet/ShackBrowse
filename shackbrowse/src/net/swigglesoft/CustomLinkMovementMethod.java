package net.swigglesoft;

import net.swigglesoft.shackbrowse.SpoilerSpan;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TextView;

public class CustomLinkMovementMethod extends LinkMovementMethod {
	
	private Long lastClickTime = 0l;
	private int lastRawX = 0;
	private int lastRawY = 0;
	
	private ClickableSpan thisLink;
	private TextView lastWidget;

	
	@Override
    public boolean canSelectArbitrarily () {
        return true;
    }

    @Override
    public void initialize(TextView widget, Spannable text) {
            Selection.setSelection(text, 0);
    }

    @Override
    public void onTakeFocus(TextView view, Spannable text, int dir) {
       if ((dir & (View.FOCUS_FORWARD | View.FOCUS_DOWN)) != 0) {
           if (view.getLayout() == null) {
               // This shouldn't be null, but do something sensible if it is.
                Selection.setSelection(text, text.length());
           }
       } else {
            Selection.setSelection(text, text.length());
       }
    }
	
	@Override
    public boolean onTouchEvent(TextView widget, Spannable buffer,
                                MotionEvent event) {
        int action = event.getAction();
        
        if (action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            lastRawX = (int) event.getRawX();
            lastRawY = (int) event.getRawY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);
            SpoilerSpan[] check = buffer.getSpans(off, off, SpoilerSpan.class);
            
            if ((link.length != 0) || (check.length != 0)) {
                if (action == MotionEvent.ACTION_UP) {
                	if (System.currentTimeMillis() - lastClickTime < ViewConfiguration.getLongPressTimeout())
                	{
                		// check for spoilers
                		boolean allspoilersspoiled = true;
                		for (SpoilerSpan spoiler : check)
                			if (!spoiler.getSpoiled())
                				allspoilersspoiled = false;
                			
                		if ((check == null || allspoilersspoiled) && link.length > 0)
                			link[0].onClick(widget);
                		else if (check.length > 0)
                		{
                			
                			for (SpoilerSpan spoiler : check)
                				spoiler.onClick(widget);
                		}
                	}
                } else if (action == MotionEvent.ACTION_DOWN) {
                    // Selection.setSelection(buffer, buffer.getSpanStart(link[0]), buffer.getSpanEnd(link[0]));
                    lastClickTime = System.currentTimeMillis();

                }
                return true;
            }
        }

        return super.onTouchEvent(widget, buffer, event);
    }
	
	
    public static MovementMethod getInstance() {
        if (sInstance == null)
            sInstance = new CustomLinkMovementMethod();

        return sInstance;
    }

    private static CustomLinkMovementMethod sInstance;
}