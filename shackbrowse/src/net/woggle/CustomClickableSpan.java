package net.woggle;

import android.text.style.ClickableSpan;
import android.view.View;
import android.view.View.OnLongClickListener;

public abstract class CustomClickableSpan extends ClickableSpan implements OnLongClickListener {
	abstract public boolean onLongClick(View view);
}