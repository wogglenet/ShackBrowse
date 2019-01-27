package net.woggle;

import android.content.Context;
import android.os.Bundle;
import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;
import androidx.core.os.BuildCompat;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public class EditTextSelectionSavedAllowImage extends androidx.appcompat.widget.AppCompatEditText {

	private String[] imgTypeString;
	private KeyBoardInputCallbackListener keyBoardInputCallbackListener;

	public EditTextSelectionSavedAllowImage(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public EditTextSelectionSavedAllowImage(Context context) {
		super(context);
		initView();
	}

	private void initView() {
		imgTypeString = new String[]{"image/png",
				"image/gif",
				"image/jpeg",
				"image/webp"};
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		final InputConnection ic = super.onCreateInputConnection(outAttrs);
		EditorInfoCompat.setContentMimeTypes(outAttrs,
				imgTypeString);
		return InputConnectionCompat.createWrapper(ic, outAttrs, callback);
	}


	final InputConnectionCompat.OnCommitContentListener callback =
			new InputConnectionCompat.OnCommitContentListener() {
				@Override
				public boolean onCommitContent(InputContentInfoCompat inputContentInfo,
				                               int flags, Bundle opts) {

					// read and display inputContentInfo asynchronously
					if (BuildCompat.isAtLeastNMR1() && (flags &
							InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0) {
						try {
							inputContentInfo.requestPermission();
						} catch (Exception e) {
							return false; // return false if failed
						}
					}
					boolean supported = false;
					for (final String mimeType : imgTypeString) {
						if (inputContentInfo.getDescription().hasMimeType(mimeType)) {
							supported = true;
							break;
						}
					}
					if (!supported) {
						return false;
					}

					if (keyBoardInputCallbackListener != null) {
						keyBoardInputCallbackListener.onCommitContent(inputContentInfo, flags, opts);
					}
					return true;  // return true if succeeded
				}
			};

	public interface KeyBoardInputCallbackListener {
		void onCommitContent(InputContentInfoCompat inputContentInfo,
		                     int flags, Bundle opts);
	}

	public void setKeyBoardInputCallbackListener(KeyBoardInputCallbackListener keyBoardInputCallbackListener) {
		this.keyBoardInputCallbackListener = keyBoardInputCallbackListener;
	}

	public String[] getImgTypeString() {
		return imgTypeString;
	}

	public void setImgTypeString(String[] imgTypeString) {
		this.imgTypeString = imgTypeString;
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
