package net.woggle;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * This is a really crappy workaround i found on stackexchange for a crash in android 6.0 when deselecting text while using the hack to make text selection work with linkmovementmethod
 * Created by brad on 12/11/2015.
 */
public class FixedTextView extends android.support.v7.widget.AppCompatTextView {

    public FixedTextView(final Context context) {
        super(context);
    }

    public FixedTextView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedTextView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // FIXME simple workaround to https://code.google.com/p/android/issues/detail?id=191430
        int startSelection = getSelectionStart();
        int endSelection = getSelectionEnd();
        if (startSelection != endSelection) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                final CharSequence text = getText();
                setText(null);
                setText(text);
            }
        }
        return super.dispatchTouchEvent(event);
    }

}
