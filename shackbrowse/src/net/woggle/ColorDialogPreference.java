/**
 * Copyright CMW Mobile.com, 2011.
 */

package net.woggle;

import android.app.AlertDialog.Builder;

import android.content.Context;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;

import android.preference.DialogPreference;

import android.util.AttributeSet;

import android.view.MotionEvent;
import android.view.View;

/**
 * The ColorDialogPreference class is responsible for displaying a color
 * palet where the user can select a color from. 
 * @author Casper Wakkers
 */
public class ColorDialogPreference extends DialogPreference {
	private int color = 0;
	// Color change listener.
	private OnColorChangedListener dialogColorChangedListener = null;

	/**
	 * Interface describing a color change listener.
	 */
	public interface OnColorChangedListener {
		/**
		 * Method colorChanged is called when a new color is selected.
		 * @param color new color.
		 */
		void colorChanged(int color);
	}

	/**
	 * Inner class representing the color chooser.
	 */
	private class ColorPickerView extends View {
		private static final int CENTER_X = 100;
		private static final int CENTER_Y = 100;
		private static final int CENTER_RADIUS = 32;

		private Paint paint = null;
		private Paint centerPaint = null;
		private boolean trackingCenter = false;
		private final int[] colors = new int[] {
//			0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFFFFFFFF, 0xFF000000,
//			0xFF00FFFF, 0xFF00FF00,	0xFFFFFF00, 0xFFFF0000 };

			0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00,
			0xFFFFFF00, 0xFFFF0000 };

		/**
		 * @param context
		 * @param listener
		 * @param color
		 */
		ColorPickerView(Context context, OnColorChangedListener listener,
				int color) {
			super(context);

			paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setShader(new SweepGradient(0, 0, colors, null));
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(32);

			centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			centerPaint.setColor(color);
			centerPaint.setStrokeWidth(5);
		}
		/**
		 * {@inheritDoc}
		 */
		protected void onDraw(Canvas canvas) {
			int centerX = getRootView().getWidth()/2 -
				(int)(paint.getStrokeWidth()/2);
			float r = CENTER_X - paint.getStrokeWidth()*0.5f;

			canvas.translate(centerX, CENTER_Y);
			canvas.drawOval(new RectF(-r, -r, r, r), paint);
			canvas.drawCircle(0, 0, CENTER_RADIUS, centerPaint);

			if (trackingCenter) {
				int c = centerPaint.getColor();
				centerPaint.setStyle(Paint.Style.STROKE);
				centerPaint.setAlpha(0x80);

				canvas.drawCircle(0, 0, CENTER_RADIUS +
					centerPaint.getStrokeWidth(), centerPaint);

				centerPaint.setStyle(Paint.Style.FILL);
				centerPaint.setColor(c);
			}
		}
		/**
		 * {@inheritDoc}
		 */
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			int width = getRootView().getWidth();

			if (width == 0) {
				width = CENTER_X*2 + 50;
			}

			setMeasuredDimension(width, CENTER_Y*2);
		}
		/**
		 * @param s
		 * @param d
		 * @param p
		 * @return
		 */
		private int ave(int s, int d, float p) {
			return s + Math.round(p * (d - s));
		}
		/**
		 * @param colors
		 * @param unit
		 * @return
		 */
		private int interpColor(int colors[], float unit) {
			if (unit <= 0) {
				return colors[0];
			}

			if (unit >= 1) {
				return colors[colors.length - 1];
			}

			float p = unit * (colors.length - 1);
			int i = (int) p;
			p -= i;

			// now p is just the fractional part [0...1) and i is the index
			int c0 = colors[i];
			int c1 = colors[i + 1];
			int a = ave(Color.alpha(c0), Color.alpha(c1), p);
			int r = ave(Color.red(c0), Color.red(c1), p);
			int g = ave(Color.green(c0), Color.green(c1), p);
			int b = ave(Color.blue(c0), Color.blue(c1), p);

			return Color.argb(a, r, g, b);
		}
		/**
		 * {@inheritDoc}
		 */
		public boolean onTouchEvent(MotionEvent event) {
			float x = event.getX() - getRootView().getWidth()/2;
			float y = event.getY() - CENTER_Y;

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				float angle = (float) java.lang.Math.atan2(y, x);
				// need to turn angle [-PI ... PI] into unit [0....1]
				float unit = (float)(angle / (2 * Math.PI));

				if (unit < 0) {
					unit += 1;
				}

				centerPaint.setColor(interpColor(colors, unit));
				invalidate();
				break;
			case MotionEvent.ACTION_UP:
				ColorDialogPreference.this.color = centerPaint.getColor();
				break;
			}

			return true;
		}
	}

	/**
	 * ColorDialogPreference constructor.
	 * @param context of this class.
	 * @param attrs custom xml attributes.
	 */
	public ColorDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	/**
	 * {@inheritDoc}
	 */
	protected void onDialogClosed(boolean positiveResult) {
		// Persist the color after the ok button is clicked.
		if (positiveResult && color != 0) {
			persistInt(color);
		}

		super.onDialogClosed(positiveResult);
	}
	/**
	 * {@inheritDoc}
	 */
	protected void onPrepareDialogBuilder(Builder builder) {
		dialogColorChangedListener = new OnColorChangedListener() {
			/**
			 * {@inheritDoc}
			 */
			public void colorChanged(int c) {
				ColorDialogPreference.this.color = c;
			}
		};

		int color = getSharedPreferences().getInt("notificationColor", Color.GREEN);

		builder.setView(new ColorPickerView(getContext(),
			dialogColorChangedListener, color));

		super.onPrepareDialogBuilder(builder);
	}
}