package net.woggle;

import net.woggle.shackbrowse.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

public class CheckableLinearLayout extends LinearLayout implements Checkable
{
    private boolean _checked = false;
    
    private boolean _nws = false;
    private boolean _tangent = false;
    private boolean _political = false;
    private boolean _stupid = false;
    private boolean _inf = false;
    private boolean _new = false;
    private boolean _loading = false;
    
    private static final int[] STATE_NWS = {R.attr.state_nws};
    private static final int[] STATE_TANGENT = {R.attr.state_tangent};
    private static final int[] STATE_POLITICAL = {R.attr.state_political};
    private static final int[] STATE_STUPID = {R.attr.state_stupid};
    private static final int[] STATE_INF = {R.attr.state_inf};
    private static final int[] STATE_NEW = {R.attr.state_new};
    private static final int[] STATE_LOADING = {R.attr.state_loading};
    
    private static final int[] CHECKED_STATE_SET =
    {
        android.R.attr.state_checked
    };
    
    public CheckableLinearLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace)
    {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 8);
        if (_checked)
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        if (_political)
            mergeDrawableStates(drawableState, STATE_POLITICAL);
        if (_tangent)
            mergeDrawableStates(drawableState, STATE_TANGENT);
        if (_nws)
            mergeDrawableStates(drawableState, STATE_NWS);
        if (_stupid)
            mergeDrawableStates(drawableState, STATE_STUPID);
        if (_inf)
            mergeDrawableStates(drawableState, STATE_INF);
        if (_new)
            mergeDrawableStates(drawableState, STATE_NEW);
        if (_loading)
            mergeDrawableStates(drawableState, STATE_LOADING);
        return drawableState;
    }
    
    public void toggle()
    {
        setChecked(!_checked);
    }
    
    public boolean isChecked()
    {
        return _checked;
    }
    
    public void setPolitical (boolean set)
    {
    	if (_political != set) { _political = set; refreshDrawableState(); }
    }
    public void setTangent (boolean set)
    {
    	if (_tangent != set) { _tangent = set; refreshDrawableState(); }
    }
    public void setStupid (boolean set)
    {
    	if (_stupid != set) { _stupid = set; refreshDrawableState(); }
    }
    public void setNWS (boolean set)
    {
    	if (_nws != set) { _nws = set; refreshDrawableState(); }
    }
    public void setInf (boolean set)
    {
    	if (_inf != set) { _inf = set; refreshDrawableState(); }
    }
    public void setNew (boolean set)
    {
    	if (_new != set) { _new = set; refreshDrawableState(); }
    }
    public void setLoading (boolean set)
    {
    	if (_loading != set) { _loading = set; refreshDrawableState(); }
    }
    public void setChecked(boolean checked)
    {
        if (checked != _checked)
        {
            _checked = checked;
            refreshDrawableState();
        }
    }

	public void setModTagsFalse() {
		_inf = false;
		_nws = false;
		_stupid = false;
		_tangent = false;
		_political = false;
	}
}
