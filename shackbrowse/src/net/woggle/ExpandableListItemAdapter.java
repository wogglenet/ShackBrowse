package net.woggle;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.nhaarman.listviewanimations.ArrayAdapter;
import com.nhaarman.listviewanimations.ListViewSetter;
import com.nhaarman.listviewanimations.itemmanipulation.ExpandCollapseListener;
import com.nhaarman.listviewanimations.util.AdapterViewUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.woggle.shackbrowse.Post;
import net.woggle.shackbrowse.R;

/**
 * An {@link ArrayAdapter} which allows items to be expanded using an animation.
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class ExpandableListItemAdapter<T> extends ArrayAdapter<T> implements ListViewSetter {

    private static final int DEFAULTTITLEPARENTRESID = 10000;
    private static final int DEFAULTCONTENTPARENTRESID = 10001;

    private final Context mContext;
    public float mAnimSpeed = 1.0f;
    private long mDuration = 1500L;
    private AnimationResIds mResIds = new AnimationResIds();
    private int mActionViewResId;
    private final List<Long> mExpandedIds;

    private int mLimit;

    private AbsListView mAbsListView;

    private ExpandCollapseListener mExpandCollapseListener;
	private int mOriginalUsernameHeight = 0;
    private float mZoom = 1.0f;
    private ValueAnimator mLastExpansionAnimation;
    private ValueAnimator mLastCollapseAnimation;
    private boolean mEnablePostCollapseOnClick = false;

    /**
     * Creates a new ExpandableListItemAdapter with an empty list.
     */
    
    public int getExpandedPosition()
    {
    	if ((mLimit >= 2) && (mExpandedIds.size() > 1))
    		return findPositionForId(mExpandedIds.get(1));
    	else return -1;
    }

    /**
     * Creates a new {@link ExpandableListItemAdapter} with the specified list,
     * or an empty list if items == null.
     */
    public ExpandableListItemAdapter(final Context context, final List<T> items) {
        super(items);
        mContext = context;
        setupPref();

        mExpandedIds = new ArrayList<Long>();
    }

    public void setupPref() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mDuration = (long) (mContext.getResources().getInteger(android.R.integer.config_shortAnimTime) * Float.parseFloat(prefs.getString("replyAnimationSpeed", "1.3")));
        mAnimSpeed = Float.parseFloat(prefs.getString("replyAnimationSpeed", "1.3"));
        mZoom = Float.parseFloat(prefs.getString("fontZoom", "1.0"));
        mEnablePostCollapseOnClick = prefs.getBoolean("allowPostCollapseOnClick", false);
    }

    /**
     * Creates a new ExpandableListItemAdapter with an empty list. Uses given
     * layout resource for the view; titleParentResId and contentParentResId
     * should be identifiers for ViewGroups within that layout.
     */
    public ExpandableListItemAdapter(final Context context, final AnimationResIds resids) {
        this(context, resids, null);
    }

    /**
     * Creates a new ExpandableListItemAdapter with the specified list, or an
     * empty list if items == null. Uses given layout resource for the view;
     * titleParentResId and contentParentResId should be identifiers for
     * ViewGroups within that layout.
     */
    public ExpandableListItemAdapter(final Context context, AnimationResIds resids, final List<T> items) {
        super(items);
        mContext = context;
        mResIds = resids;

        mExpandedIds = new ArrayList<Long>();
    }

    @Override
    public void setAbsListView(final AbsListView listView) {
        mAbsListView = listView;
    }

    /**
     * Set the resource id of the child {@link View} contained in the View
     * returned by {@link #getTitleView(int, View, ViewGroup)} that will be the
     * actuator of the expand / collapse animations.<br>
     * If there is no View in the title View with given resId, a
     * {@link NullPointerException} is thrown.</p> Default behavior: the whole
     * title View acts as the actuator.
     *
     * @param resId the resource id.
     */
    public void setActionViewResId(final int resId) {
        mActionViewResId = resId;
    }

    /**
     * Set the maximum number of items allowed to be expanded. When the
     * (limit+1)th item is expanded, the first expanded item will collapse.
     *
     * @param limit the maximum number of items allowed to be expanded. Use <= 0
     *              for no limit.
     */
    public void setLimit(final int limit) {
        mLimit = limit;
        mExpandedIds.clear();
        notifyDataSetChanged();
    }

    /**
     * Set the {@link com.nhaarman.listviewanimations.itemmanipulation.ExpandCollapseListener} that should be notified of expand / collapse events.
     */
    public void setExpandCollapseListener(final ExpandCollapseListener expandCollapseListener) {
        mExpandCollapseListener = expandCollapseListener;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        ViewGroup view = (ViewGroup) convertView;
        ViewHolder viewHolder;

        if (view == null) {
            view = createView(parent);

            viewHolder = new ViewHolder();
            viewHolder.titleParent = (ViewGroup) view.findViewById(mResIds.titleResId);
            viewHolder.contentParent = (ViewGroup) view.findViewById(mResIds.contentResId);

            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        View titleView = getTitleView(position, viewHolder.titleView, viewHolder.titleParent);
        if (titleView != viewHolder.titleView) {
            viewHolder.titleParent.removeAllViews();
            viewHolder.titleParent.addView(titleView);

            if (mActionViewResId == 0) {
                view.setOnClickListener(new TitleViewOnClickListener(viewHolder.contentParent));
            } else {
                view.findViewById(mActionViewResId).setOnClickListener(new TitleViewOnClickListener(viewHolder.contentParent));
            }
        }
        viewHolder.titleView = titleView;


        View contentView = getContentView(position, viewHolder.contentView, viewHolder.contentParent);
        if (contentView != viewHolder.contentView) {
            viewHolder.contentParent.removeAllViews();
            viewHolder.contentParent.addView(contentView);
        }
        viewHolder.contentView = contentView;

        // optimization
        if (mExpandedIds.contains(getItemId(position)))
        { loadExpandedViewDataIntoView(position, viewHolder.contentView); }

        viewHolder.contentParent.setVisibility(mExpandedIds.contains(getItemId(position)) ? View.VISIBLE : View.GONE);
        viewHolder.contentParent.setTag(getItemId(position));

        ViewGroup.LayoutParams layoutParams = viewHolder.contentParent.getLayoutParams();
        layoutParams.height = LayoutParams.WRAP_CONTENT;
        viewHolder.contentParent.setLayoutParams(layoutParams);

        // System.out.println("GETVIEW: " + position);
        return view;
    }

    abstract public void loadExpandedViewDataIntoView(int position, View convertView);

    private ViewGroup createView(final ViewGroup parent) {
        ViewGroup view;

        if (mResIds.layoutResId == 0) {
            view = new RootView(mContext);
        } else {
            view = (ViewGroup) LayoutInflater.from(mContext).inflate(mResIds.layoutResId, parent, false);
        }

        return view;
    }

    /**
     * Get a View that displays the <b>title of the data</b> at the specified
     * position in the data set. You can either create a View manually or
     * inflate it from an XML layout file. When the View is inflated, the parent
     * View (GridView, ListView...) will apply default layout parameters unless
     * you use
     * {@link android.view.LayoutInflater#inflate(int, android.view.ViewGroup, boolean)}
     * to specify a root view and to prevent attachment to the root.
     *
     * @param position    The position of the item within the adapter's data set of the
     *                    item whose view we want.
     * @param convertView The old view to reuse, if possible. Note: You should check
     *                    that this view is non-null and of an appropriate type before
     *                    using. If it is not possible to convert this view to display
     *                    the correct data, this method can create a new view.
     * @param parent      The parent that this view will eventually be attached to
     * @return A View corresponding to the title of the data at the specified
     * position.
     */
    public abstract View getTitleView(int position, View convertView, ViewGroup parent);

    /**
     * Get a View that displays the <b>content of the data</b> at the specified
     * position in the data set. You can either create a View manually or
     * inflate it from an XML layout file. When the View is inflated, the parent
     * View (GridView, ListView...) will apply default layout parameters unless
     * you use
     * {@link android.view.LayoutInflater#inflate(int, android.view.ViewGroup, boolean)}
     * to specify a root view and to prevent attachment to the root.
     *
     * @param position    The position of the item within the adapter's data set of the
     *                    item whose view we want.
     * @param convertView The old view to reuse, if possible. Note: You should check
     *                    that this view is non-null and of an appropriate type before
     *                    using. If it is not possible to convert this view to display
     *                    the correct data, this method can create a new view.
     * @param parent      The parent that this view will eventually be attached to
     * @return A View corresponding to the content of the data at the specified
     * position.
     */
    public abstract View getContentView(int position, View convertView, ViewGroup parent);

    /**
     * Indicates if the item at the specified position is expanded.
     *
     * @param position Index of the view whose state we want.
     * @return true if the view is expanded, false otherwise.
     */
    public boolean isExpanded(final int position) {
        long itemId = getItemId(position);
        return mExpandedIds.contains(itemId);
    }

    /**
     * Return the title view at the specified position.
     *
     * @param position Index of the view we want.
     * @return the view if it exist, null otherwise.
     */
    public View getTitleView(final int position) {
        View titleView = null;

        View parentView = findViewForPosition(position);
        if (parentView != null) {
            Object tag = parentView.getTag();
            if (tag instanceof ViewHolder) {
                titleView = ((ViewHolder) tag).titleView;
            }
        }
        return titleView;
    }
    protected View getTitleParent(final int position) {
        View titleParent = null;

        View parentView = findViewForPosition(position);
        if (parentView != null) {
            Object tag = parentView.getTag();
            if (tag instanceof ViewHolder) {
                titleParent = ((ViewHolder) tag).titleParent;
            }
        }

        return titleParent;
    }

    /**
     * Return the content view at the specified position.
     *
     * @param position Index of the view we want.
     * @return the view if it exist, null otherwise.
     */
    public View getContentView(final int position) {
        View contentView = null;

        View parentView = findViewForPosition(position);
        if (parentView != null) {
            Object tag = parentView.getTag();
            if (tag instanceof ViewHolder) {
                contentView = ((ViewHolder) tag).contentView;
            }
        }

        return contentView;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

        Set<Long> removedIds = new HashSet<Long>(mExpandedIds);

        for (int i = 0; i < getCount(); ++i) {
            long id = getItemId(i);
            removedIds.remove(id);
        }

        mExpandedIds.removeAll(removedIds);
        
        // System.out.println("NDSC: mEX1:" + ((mExpandedIds.size() > 0) ? findPositionForId(mExpandedIds.get(0)) : " ") + " 2:" + ((mExpandedIds.size() > 1) ? findPositionForId(mExpandedIds.get(1)) : " "));
    }

    /**
     * Return the content parent at the specified position.
     *
     * @param position Index of the view we want.
     * @return the view if it exist, null otherwise.
     */
    private View getContentParent(final int position) {
        View contentParent = null;

        View parentView = findViewForPosition(position);
        if (parentView != null) {
            Object tag = parentView.getTag();
            if (tag instanceof ViewHolder) {
                contentParent = ((ViewHolder) tag).contentParent;
            }
        }

        return contentParent;
    }

    /**
     * Expand the view at given position. Will do nothing if the view is already expanded.
     *
     * @param position the position to expand.
     */
    public void expand(final int position) {
        long itemId = getItemId(position);
        if (mExpandedIds.contains(itemId)) {
            return;
        }

        toggle(position);
    }
    
    public void expandWithoutAnimation(final int position) {
    	boolean shouldCollapseOther = mLimit > 0 && mExpandedIds.size() >= mLimit;
        if (shouldCollapseOther) {
        	Long firstId = mExpandedIds.get(0);
        	
        	// hack to allow first post to remain open
        	if ((mLimit >= 2) && (mExpandedIds.size() > 1))
        		firstId = mExpandedIds.get(1);
	        
        	mExpandedIds.remove(firstId);

            //System.out.println("COLLAPSED ID" + findPositionForId(firstId));
        }
        
        long itemId = getItemId(position);
        if (mExpandedIds.contains(itemId)) {
            return;
        }
        mExpandedIds.add(itemId);
        //System.out.println("EXPANDED ID" + position);
        
        
        notifyDataSetChanged();
    }

    /**
     * Collapse the view at given position. Will do nothing if the view is already collapsed.
     *
     * @param position the position to collapse.
     */
    public void collapse(final int position) {
        long itemId = getItemId(position);
        if (!mExpandedIds.contains(itemId)) {
            return;
        }

        toggle(position);
    }

    private View findViewForPosition(final int position) {
        View result = null;
        for (int i = 0; i < mAbsListView.getChildCount() && result == null; i++) {
            View childView = mAbsListView.getChildAt(i);
            if (AdapterViewUtil.getPositionForView(mAbsListView, childView) == position) {
                result = childView;
            }
        }
        return result;
    }

    public int findPositionForId(final long id) {
        for (int i = 0; i < getCount(); i++) {
            if (getItemId(i) == id) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Toggle the {@link View} at given position, ignores header or footer Views.
     *
     * @param position the position of the view to toggle.
     */
    public void toggle(final int position) {
        long itemId = getItemId(position);
        boolean isExpanded = mExpandedIds.contains(itemId);

        View contentParent = getContentParent(position);
        if (contentParent != null) {
            toggle(contentParent);
        }

        if (contentParent == null && isExpanded) {
            mExpandedIds.remove(itemId);
        } else if (contentParent == null && !isExpanded) {
            mExpandedIds.add(itemId);
            boolean isVisible = false;
            boolean shouldCollapseOther = !isVisible && mLimit > 0 && mExpandedIds.size() >= mLimit;
            if (shouldCollapseOther) {
            	Long firstId = mExpandedIds.get(0);
            	
            	// hack to allow first post to remain open
            	if ((mLimit >= 2) && (mExpandedIds.size() > 1))
            		firstId = mExpandedIds.get(1);

                int firstPosition = findPositionForId(firstId);

                // stop the current expansion animation if running
    	        if ((mLastExpansionAnimation != null) && (mLastExpansionAnimation.isRunning()))
                    mLastExpansionAnimation.cancel();

                View firstEV = getContentParent(firstPosition);
                if (firstEV != null) {
                    mLastCollapseAnimation = ExpandCollapseHelper.animateCollapsing(firstEV, mContext.getResources().getColor(R.color.selected_highlight_postbg), mContext.getResources().getColor(R.color.ics_background_start), mDuration);
                }

                View firstTV = getTitleParent(firstPosition);
                if (firstTV != null) {
                    ExpandCollapseHelper.animateSwapTitle(firstTV, mResIds, true, (Post) getItem(firstPosition), mOriginalUsernameHeight, mDuration, mZoom);
                }
                mExpandedIds.remove(firstId);

                if (mExpandCollapseListener != null) {
                    mExpandCollapseListener.onItemCollapsed(firstPosition);
                }
            	
            }
        }
    }

    public void toggle(final View contentParent) {
        boolean isVisible = contentParent.getVisibility() == View.VISIBLE;
        boolean shouldCollapseOther = !isVisible && mLimit > 0 && mExpandedIds.size() >= mLimit;
        if (shouldCollapseOther) {

        	Long firstId = mExpandedIds.get(0);
        	
        	// hack to allow first post to remain open
        	if ((mLimit >= 2) && (mExpandedIds.size() > 1))
        		firstId = mExpandedIds.get(1);

            int firstPosition = findPositionForId(firstId);

            // stop the current expansion animation if running
            if ((mLastExpansionAnimation != null) && (mLastExpansionAnimation.isRunning()))
                mLastExpansionAnimation.cancel();
	

            View firstEV = getContentParent(firstPosition);
            if (firstEV != null) {
                mLastCollapseAnimation = ExpandCollapseHelper.animateCollapsing(firstEV, mContext.getResources().getColor(R.color.selected_highlight_postbg), mContext.getResources().getColor(R.color.ics_background_start), mDuration);
            }

            View firstTV = getTitleParent(firstPosition);
            if (firstTV != null) {
                ExpandCollapseHelper.animateSwapTitle(firstTV, mResIds, true, (Post) getItem(firstPosition), mOriginalUsernameHeight, mDuration, mZoom);
            }
            mExpandedIds.remove(firstId);

            if (mExpandCollapseListener != null) {
                mExpandCollapseListener.onItemCollapsed(firstPosition);
            }
        	
        }

        Long id = (Long) contentParent.getTag();
        int position = findPositionForId(id);
        if (isVisible) {
            if (mEnablePostCollapseOnClick) {
                mLastCollapseAnimation = ExpandCollapseHelper.animateCollapsing(contentParent, mContext.getResources().getColor(R.color.selected_highlight_postbg), mContext.getResources().getColor(R.color.ics_background_start), mDuration);
                View firstTV = getTitleView(position);
                if (firstTV != null) {
                    ExpandCollapseHelper.animateSwapTitle(firstTV, mResIds, true, (Post) getItem(position), mOriginalUsernameHeight, mDuration, mZoom);
                }
                mExpandedIds.remove(id);

                if (mExpandCollapseListener != null) {
                    mExpandCollapseListener.onItemCollapsed(position);
                }
            }
        } else {
            loadExpandedViewDataIntoView(position, getContentView(position));
            mLastExpansionAnimation = ExpandCollapseHelper.animateExpanding(contentParent, mAbsListView, mContext.getResources().getColor(R.color.selected_highlight_postbg), mContext.getResources().getColor(R.color.ics_background_start), mDuration);
            View firstTV = getTitleView(position);
            if (firstTV != null) {
                ExpandCollapseHelper.animateSwapTitle(firstTV, mResIds, false, (Post) getItem(position), mOriginalUsernameHeight, mDuration, mZoom);
            }
            mExpandedIds.add(id);

            if (mExpandCollapseListener != null) {
                mExpandCollapseListener.onItemExpanded(position);
            }
        }
    }

    private class TitleViewOnClickListener implements View.OnClickListener {

        private final View mContentParent;

        private TitleViewOnClickListener(final View contentParent) {
            mContentParent = contentParent;
        }

        @Override
        public void onClick(final View view) {
            if (mTitleViewOnClickListener == null)
            {
                toggle(mContentParent);
            }
            else
                mTitleViewOnClickListener.OnClick(mContentParent);
        }
    }
    titleViewOnClick mTitleViewOnClickListener = null;
    public void setTitleViewOnClickListener (titleViewOnClick listener)
    {
        mTitleViewOnClickListener = listener;
    }
    public interface titleViewOnClick
    {
        public void OnClick(View contentParent);
    }

    private static class RootView extends LinearLayout {

        private ViewGroup mTitleViewGroup;
        private ViewGroup mContentViewGroup;

        public RootView(final Context context) {
            super(context);
            init();
        }

        private void init() {
            setOrientation(VERTICAL);

            mTitleViewGroup = new FrameLayout(getContext());
            mTitleViewGroup.setId(DEFAULTTITLEPARENTRESID);
            addView(mTitleViewGroup);

            mContentViewGroup = new FrameLayout(getContext());
            mContentViewGroup.setId(DEFAULTCONTENTPARENTRESID);
            addView(mContentViewGroup);
        }
    }

    private static class ViewHolder {
        ViewGroup titleParent;
        ViewGroup contentParent;
        View titleView;
        View contentView;
        Animation currentAnim;
    }
    
    public static class AnimationResIds
    {
    	int contentResId;
    	int titleResId;
    	int textPreviewResId;
    	int userNameResId;
    	int postedTimeResId;
    	int layoutResId;
    	int rowType;
    	int previewLolTag;
    	AnimationResIds()
    	{}
    	public AnimationResIds(int content, int title, int layout, int textpreview, int username, int postedtime, int rowtype, int prevloltag)
    	{
    		contentResId = content;
    		titleResId =  title;
    		textPreviewResId = textpreview;
    		userNameResId = username;
    		postedTimeResId = postedtime;
    		layoutResId = layout;
    		rowType = rowtype;
    		previewLolTag = prevloltag;
    	}
    }
    public void setOriginalUsernameHeight(int set)
    {
    	mOriginalUsernameHeight =  set;
    }

    private static class ExpandCollapseHelper {
    	
    	public static void animateSwapTitle(final View view, final AnimationResIds resIds, boolean collapse, Post p, int oUNH, long duration, float zoom)
    	{

    		final TextView tomove = (TextView) view.findViewById(resIds.userNameResId);
    		final View loltag = view.findViewById(resIds.previewLolTag);
    		final View time = view.findViewById(resIds.postedTimeResId);
    		final View tohide = view.findViewById(resIds.textPreviewResId);
    		final CheckableTableLayout bg = (CheckableTableLayout) view.findViewById(resIds.rowType);

            final TableRow tr = (TableRow) bg.getChildAt(0);

            tr.setLayoutTransition(new LayoutTransition());
            tr.getLayoutTransition().setDuration(duration);

            tr.getLayoutTransition().setStartDelay(LayoutTransition.DISAPPEARING, 0L);
            tr.getLayoutTransition().setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0L);
            tr.getLayoutTransition().setStartDelay(LayoutTransition.APPEARING, 0L);
            tr.getLayoutTransition().setStartDelay(LayoutTransition.CHANGE_APPEARING, 0L);

           /* tr.getLayoutTransition().addTransitionListener(new LayoutTransition.TransitionListener() {
                @Override
                public void startTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i) {

                }

                @Override
                public void endTransition(LayoutTransition layoutTransition, ViewGroup viewGroup, View view, int i) {
                    tr.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            tr.setLayoutTransition(null);
                        }
                    }, 100L);
                }
            });*/

    		if (collapse)
    		{
                /*
    			ObjectAnimator animalpha3 = ObjectAnimator.ofFloat(tohide, "alpha", 1f, 0f);
    			animalpha3.setInterpolator(new DecelerateInterpolator());
    			animalpha3.addListener(new Animator.AnimatorListener(){
					@Override
					public void onAnimationCancel(Animator arg0) {}
					@Override
					public void onAnimationEnd(Animator arg0) {
						tohide.setAlpha(1f);
					}
					@Override
					public void onAnimationRepeat(Animator arg0) {}
					@Override
					public void onAnimationStart(Animator arg0) {
						tohide.setAlpha(0f);
			            
					}});
    			animalpha3.start();
*/

                // text and username moving handled by layouttransition
                if (p.getLolObj() != null)
                {
                    loltag.setVisibility(View.VISIBLE);
                }
                else { loltag.setVisibility(View.GONE);
                }
                time.setVisibility(View.GONE);
                tomove.setTextSize(TypedValue.COMPLEX_UNIT_PX, view.getContext().getResources().getDimension(R.dimen.previewUserNameSize) * zoom);
                /*
                AnimatorSet set = new AnimatorSet();
                ObjectAnimator sizeAnimX = ObjectAnimator.ofFloat(tomove, "scaleX", view.getContext().getResources().getDimension(R.dimen.previewUserNameSize) / view.getContext().getResources().getDimension(R.dimen.previewUserNameSizeBig));
                ObjectAnimator sizeAnimY = ObjectAnimator.ofFloat(tomove, "scaleY", view.getContext().getResources().getDimension(R.dimen.previewUserNameSize) / view.getContext().getResources().getDimension(R.dimen.previewUserNameSizeBig));
                set.playTogether(sizeAnimX,sizeAnimY);
                set.setDuration(duration);
                set.start(); */
                tohide.setVisibility(View.VISIBLE);
    			bg.setChecked(false);
    		}
    		else
            {
                loltag.setVisibility(View.GONE);
                time.setVisibility(View.VISIBLE);
                tohide.setVisibility(View.GONE);
                tomove.setTextSize(TypedValue.COMPLEX_UNIT_PX, view.getContext().getResources().getDimension(R.dimen.previewUserNameSizeBig) * zoom);
                bg.setChecked(true);

/*
    			ObjectAnimator animalpha3 = ObjectAnimator.ofFloat(time, "alpha", 0f, 1f);
    			animalpha3.setInterpolator(new DecelerateInterpolator());
    			animalpha3.addListener(new Animator.AnimatorListener(){
					@Override
					public void onAnimationCancel(Animator arg0) {}
					@Override
					public void onAnimationEnd(Animator arg0) {
						time.setAlpha(1f);
					}
					@Override
					public void onAnimationRepeat(Animator arg0) {}
					@Override
					public void onAnimationStart(Animator arg0) {
						time.setAlpha(0f);
					}});
    			animalpha3.start();
    			*/
    		}
    	}

        public static ValueAnimator animateCollapsing(final View view, int to, int from, long duration) {
        	final View secondRow = ((RelativeLayout)((LinearLayout)(((FrameLayout)view).getChildAt(0))).getChildAt(0));
            
            int origHeight = view.getHeight();

            /*ObjectAnimator colorFade = ObjectAnimator.ofObject(secondRow, "backgroundColor", new ArgbEvaluator(), to, from);
            colorFade.setDuration(duration);
            colorFade.start();
            */
            secondRow.setBackgroundColor(from);
            ValueAnimator animator = createHeightAnimator(view, origHeight, 0, duration);
            animator.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationEnd(final Animator animator) {
                    view.setVisibility(View.GONE);
                    super.onAnimationEnd(animator);
                }
                @Override
                public void onAnimationCancel(Animator animation) {
                    view.setVisibility(View.VISIBLE);
                    super.onAnimationCancel(animation);
                }
            });
            animator.start();
            return animator;
        }

        public static ValueAnimator animateExpanding(final View view, final AbsListView listView, int to, int from, final long duration) {
            view.setVisibility(View.VISIBLE);

            final View secondRow = ((RelativeLayout)((LinearLayout)(((FrameLayout)view).getChildAt(0))).getChildAt(0));
            secondRow.setBackgroundColor(to);
            /*
            ObjectAnimator colorFade = ObjectAnimator.ofObject(secondRow, "backgroundColor", new ArgbEvaluator(), from, to);
            colorFade.setDuration(duration);
            colorFade.start();
            */
            View parent = (View) view.getParent();
            final int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getMeasuredWidth() - parent.getPaddingLeft() - parent.getPaddingRight(), View.MeasureSpec.AT_MOST);
            final int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            view.measure(widthSpec, heightSpec);

            ValueAnimator animator = createHeightAnimator(view, 0, view.getMeasuredHeight(), duration);
            animator.addUpdateListener(
                    new ValueAnimator.AnimatorUpdateListener() {
                        final int listViewHeight = listView.getHeight();
                        final int listViewTop = listView.getTop();
                        final int listViewTopPadding = listView.getPaddingTop();
                        final int listViewBottomPadding = listView.getPaddingBottom();
                        final View v = findDirectChild(view, listView);
                        final int origTop = v != null ? v.getTop() : 0;
                        boolean hitTop = false;
                        boolean hitBottom = false;
                        @Override
                        public void onAnimationUpdate(final ValueAnimator animation) {
                            if (v != null) {
                                final int bottom = v.getBottom();
                                final int top = v.getTop();
                                if (top < 0) {
                                    listView.smoothScrollBy(top - (listViewTop + listViewTopPadding), 0);
                                    hitTop = true;
                                } else if ((bottom > listViewHeight) && (!hitTop)) {
                                    if (top > 0) {
                                        listView.smoothScrollBy(Math.min(bottom - listViewHeight + listViewBottomPadding, top), 0);
                                        hitBottom = true;
                                    }
                                } else if (origTop != v.getTop() && !hitTop && !hitBottom && duration < 50) {
                                    listView.smoothScrollBy(v.getTop() - origTop, 0);
                                }
                            }
                        }
                    }
            );

            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {

                    listView.post(new Runnable() {
                        @Override
                        public void run() {
                            listView.invalidateViews();
                        }
                    });

                    super.onAnimationEnd(animation);
                }
            });
            animator.start();
            return animator;
        }

        private static View findDirectChild(final View view, final AbsListView listView) {
            View result = view;
            View parent = (View) result.getParent();
            while (parent != listView) {
                result = parent;
                if (result.getParent() instanceof  View) {
                    parent = (View) result.getParent();
                }
                else {
                    break;
                }
            }
            return result;
        }

        public static ValueAnimator createHeightAnimator(final View view, final int start, final int end, long duration) {
            ValueAnimator animator = ValueAnimator.ofInt(start, end);
            animator.setDuration(duration);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                @Override
                public void onAnimationUpdate(final ValueAnimator valueAnimator) {
                    int value = (Integer) valueAnimator.getAnimatedValue();
                    ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                    layoutParams.height = value;
                    view.setLayoutParams(layoutParams);
                }
            });

            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    int value = end;
                    ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                    layoutParams.height = value;
                    view.setLayoutParams(layoutParams);
                    super.onAnimationEnd(animation);
                }
                @Override
                public void onAnimationCancel(Animator animation) {
                    int value = start;
                    ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                    layoutParams.height = value;
                    view.setLayoutParams(layoutParams);
                    super.onAnimationCancel(animation);
                }
            });

            return animator;
        }
    }

	public static AnimationResIds createResIds(
			int tviewThreadrowExpandedContainer,
			int tviewThreadrowPreviewContainer, int threadRowContainer,
			int textpreview, int textpreviewusername, int textpostedtime, int rowType, int previewloltag) {
		// TODO Auto-generated method stub
		return new AnimationResIds(tviewThreadrowExpandedContainer, tviewThreadrowPreviewContainer,threadRowContainer, textpreview, textpreviewusername, textpostedtime, rowType, previewloltag);
	}
}
