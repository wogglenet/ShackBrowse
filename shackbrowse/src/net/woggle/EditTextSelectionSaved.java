package net.woggle;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

public class EditTextSelectionSaved extends EditText {

	
	public EditTextSelectionSaved(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onWindowFocusChanged (boolean hasWindowFocus) {
	    boolean hadSelection = this.hasSelection();
	    int start=0, end=0;
	    if(hadSelection) {
	        start = getSelectionStart();
	        end = getSelectionEnd();
	    }
	    super.onWindowFocusChanged(hasWindowFocus);
	    if(hadSelection) {
	        setSelection(start, end);
	    }
	} 
}
