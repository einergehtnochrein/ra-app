package de.leckasemmel.sonde1.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.Locale;

import de.leckasemmel.sonde1.R;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.round;

import androidx.annotation.NonNull;


public class SpectrumView extends View {
    final double anchorPercent = 0.015; // Distance of anchor from bottom in percent of bounding rectangle height
    final double scaleMinorStrokeWidth = 2.0 / 400.0;        // Stroke width for minor scale ticks (fraction of view height)
    final double gridPhysical = 0.010;

    private int mColorBins;
    private int mColorGrid;
    private int mColorText;

    private Paint mPaint;
    private Rect rBoundsI;
    private Rect rBinI;
    private Rect textBounds;
    private DashPathEffect mDashPathEffect;

    private double mStartFrequency;
    private double mStopFrequency;
    private double mStartFrequencyView;
    private double mStopFrequencyView;
    private double mMinLevel;
    private double mMaxLevel;
    private int mNumBinsPhysical;
    private double[] mBinsPhysical;
    private double mLevelsFrequency;    // Start frequency of levels list in setLevels()
    private double mLevelsSpacing;      // Spacing in levels list in setLevels()

    private ScaleGestureDetector mScaleDetector;
    private double mScaleStartFrequency;
    private double mScaleStopFrequency;

    private double mLastTouchX;
    private int mActivePointerId;

    private RangeEventListener mRangeStartEventListener;
    private RangeEventListener mRangeEndEventListener;

    public interface RangeEventListener {
        void onRangeChange(double value);
    }

    public void setRangeStartEventListener(RangeEventListener listener) {
        mRangeStartEventListener = listener;
    }

    public void setRangeEndEventListener(RangeEventListener listener) {
        mRangeEndEventListener = listener;
    }

    public SpectrumView(Context context) {
        super(context);
        doWhatAConstructorMustDo(context);
    }

    public SpectrumView(Context context, AttributeSet attrs) {
        super(context, attrs);
        doWhatAConstructorMustDo(context);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SpectrumView);
        mColorBins = a.getInt(R.styleable.SpectrumView_colorBins, Color.WHITE);
        mColorGrid = a.getInt(R.styleable.SpectrumView_colorGrid, Color.DKGRAY);
        mColorText = a.getInt(R.styleable.SpectrumView_colorText, Color.BLACK);
        a.recycle();
    }

    private void doWhatAConstructorMustDo(Context context) {
        rBoundsI = new Rect();
        rBinI = new Rect();
        textBounds = new Rect();
        mPaint = new Paint();
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mDashPathEffect = new DashPathEffect(new float[]{4, 4}, 0);

        mStartFrequency = 395.0f;
        mStopFrequency = 410.0f;
        mStartFrequencyView = 400.0f;
        mStopFrequencyView = 406.0f;
        mMinLevel = -130.0f;
        mMaxLevel = -80.0f;

        mNumBinsPhysical = (int) round((mStopFrequency - mStartFrequency) / gridPhysical) + 1;
        mBinsPhysical = new double[mNumBinsPhysical];
        for (int i = 0; i < mNumBinsPhysical; i++) {
            mBinsPhysical[i] = Float.NaN;
        }

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mScaleDetector.setQuickScaleEnabled(true);
    }

    // Determine bin# from frequency
    int _getBin(double frequency) {

        int index = (int) round((frequency - mStartFrequency) / gridPhysical);
        if (index < 0) {
            index = 0;
        }
        if (index > mNumBinsPhysical - 1) {
            index = mNumBinsPhysical - 1;
        }

        return index;
    }

    // Determine frequency from bin#
    double _getFrequencyFromBin(int bin) {
        return mStartFrequency + bin * gridPhysical;
    }

    public void setLevelsFrequency(double frequency) {
        mLevelsFrequency = frequency;
    }

    public void setLevelsSpacing(double spacing) {
        mLevelsSpacing = spacing;
    }

    public void setLevels(Double[] levels) {
        for (int i = 0; i < levels.length; i++) {
            //TODO: Assume 'step' is identical to 'gridPhysical'...
            int index = _getBin(mLevelsFrequency + i * mLevelsSpacing);
            mBinsPhysical[index] = levels[i];
        }
        invalidate();
    }

    public double getRangeStart() {
        return mStartFrequencyView;
    }

    public void setRangeStart(double d) {
        mStartFrequencyView = d;
    }

    public double getRangeEnd() {
        return mStopFrequencyView;
    }

    public void setRangeEnd(double d) {
        mStopFrequencyView = d;
    }

    // (Re)create the background bitmap
    private void createBackground(int width, int height) {
        Bitmap.Config config = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);
        Canvas canvas = new Canvas(bitmap);

        // Set reference point to the anchor
        float anchorX = rBoundsI.centerX();
        float anchorY = rBoundsI.bottom - rBoundsI.height() * (float) anchorPercent;
        canvas.translate(anchorX, anchorY);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        canvas.getClipBounds(rBoundsI);

        mPaint.setStrokeWidth((float) scaleMinorStrokeWidth * rBoundsI.height());
        mPaint.setAlpha(255);

        final double span = mStopFrequencyView - mStartFrequencyView;

        int i;
        double y_step = rBoundsI.height() / (mMaxLevel - mMinLevel);

        // Draw grid
        mPaint.setStyle(Paint.Style.FILL);
        int spSize = 12;
        float scaledSizeInPixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, spSize, getResources().getDisplayMetrics());
        mPaint.setTextSize(scaledSizeInPixels);
        mPaint.setTextAlign(Paint.Align.CENTER);
        for (double f = mStartFrequencyView; f <= mStopFrequencyView; f += gridPhysical) {
            double scale = rBoundsI.width() / span;
            double left = (f - mStartFrequencyView - 0) * scale;
            double right = (f - mStartFrequencyView + 0) * scale;

            rBinI.left = (int) ceil(rBoundsI.left + left);
            rBinI.right = (int) floor(rBoundsI.left + right);
            rBinI.bottom = rBoundsI.bottom;
            rBinI.top = rBoundsI.top;

            String formatLabel = "%.1f";
            boolean showLabel = false;
            boolean showGrid = false;
            mPaint.setPathEffect(mDashPathEffect);

            if (round(100 * f) % 100 == 0) {      /* Full MHz */
                mPaint.setPathEffect(null);
                if (span > 2.0) {
                    formatLabel = "%.0f";
                }
                showLabel = true;
                showGrid = true;
            } else if (round(100 * f) % 20 == 0) {  /* 200 kHz */
                if (span <= 15.0) {            /* Show up to 15 MHz */
                    showGrid = true;
                    if (span < 2.0) {
                        showLabel = true;
                    }
                }
            } else if (round(100 * f) % 10 == 0) {  /* 100 kHz */
                if (span <= 3.0) {             /* Show when window <= 3 MHz */
                    showGrid = true;
                    if (span <= 1.0) {
                        showLabel = true;
                    }
                }
            } else if (round(100 * f) % 5 == 0) {   /* 50 kHz */
                if (span <= 1.2) {
                    showGrid = true;
                }
            }

            if (showGrid) {
                mPaint.setColor(mColorGrid);
                canvas.drawRect(rBinI, mPaint);
            }

            if (showLabel) {
                mPaint.setColor(mColorText);
                String s = String.format(Locale.US, formatLabel, f);
                mPaint.getTextBounds(s, 0, s.length(), textBounds);
                canvas.drawText(s, rBinI.left, rBoundsI.top - 2 * textBounds.exactCenterY(), mPaint);
            }
        }

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAlpha(200);
        mPaint.setColor(mColorBins);
        for (i = 0; i < mNumBinsPhysical; i++) {
            double f = _getFrequencyFromBin(i);
            double scale = rBoundsI.width() / span;
            double level = mBinsPhysical[i];

            if (Double.isNaN(level)) {
                level = mMinLevel;
            }

            double left = (f - mStartFrequencyView - gridPhysical / 2.0f) * scale;
            double right = (f - mStartFrequencyView + gridPhysical / 2.0f) * scale;

            rBinI.left = (int) floor(rBoundsI.left + left);
            rBinI.right = (int) ceil(rBoundsI.left + right);
            if (rBinI.right - rBinI.left < 2) {
                if ((i > 0) && (i < mNumBinsPhysical - 1)) {
                    if ((level >= mBinsPhysical[i - 1]) && (level >= mBinsPhysical[i + 1])) {
                        rBinI.left -= 2;
                    }
                }
            }
            rBinI.bottom = rBoundsI.bottom;
            rBinI.top = rBoundsI.bottom - (int) round((level - mMinLevel) * y_step);

            canvas.drawRect(rBinI, mPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // The optimum aspect ratio
        final float optimumAspectRatio = 720f / 190f;

        // Find out what the system has planned
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        float width;
        float height;

        // Measure Width
        if ((widthMode == MeasureSpec.EXACTLY) || (widthMode == MeasureSpec.AT_MOST)) {
            // Use what is available
            width = widthSize;
        } else {
            // No constraints
            width = 720f;
        }

        // Measure Height
        if ((heightMode == MeasureSpec.EXACTLY) || (heightMode == MeasureSpec.AT_MOST)) {
            // Use what is available
            height = heightSize;
        } else {
            // No constraints
            height = 400f;
        }

        // Correct one of the parameters iin case we didn't get the desired aspect ratio
        if (width / height < optimumAspectRatio) {
            // Leave the width as is, and adapt the height
            height = width / optimumAspectRatio;
        } else {
            // Leave the height as is, and adapt the width
            width = height * optimumAspectRatio;
        }

        // Inform system about our decision
        setMeasuredDimension(Math.round(width), Math.round(height));

        // Prepare a background bitmap for the proposed dimensions
        createBackground(Math.round(width), Math.round(height));

        //TODO Not the best place to do this...
        for (int i = 0; i < mNumBinsPhysical; i++) {
            mBinsPhysical[i] = Float.NaN;
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);

        performClick();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                final int pointerIndex = event.getActionMasked();
                mLastTouchX = event.getX(pointerIndex);
                mActivePointerId = event.getPointerId(0);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = event.findPointerIndex(mActivePointerId);
                if (pointerIndex >= 0) {
                    final double x = event.getX(pointerIndex);
                    final double span = mStopFrequencyView - mStartFrequencyView;
                    final int windowWidth = getWidth();
                    double deltaF = (x - mLastTouchX) / (float) windowWidth * span;
                    mLastTouchX = x;
                    double f1 = mStartFrequencyView - deltaF;
                    double f2 = mStopFrequencyView - deltaF;
                    if (f1 < mStartFrequency) {
                        f1 = mStartFrequency;
                        f2 = f1 + span;
                    }
                    if (f2 > mStopFrequency) {
                        f2 = mStopFrequency;
                        f1 = f2 - span;
                    }
                    mStartFrequencyView = f1;
                    mStopFrequencyView = f2;

                    if (mRangeStartEventListener != null) {
                        mRangeStartEventListener.onRangeChange(mStartFrequencyView);
                    }
                    if (mRangeEndEventListener != null) {
                        mRangeEndEventListener.onRangeChange(mStopFrequencyView);
                    }

                    invalidate();
                }
                break;
            }
        }

        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
            Log.d("spectrum", "onScaleBegin()");

            mScaleStartFrequency = mStartFrequencyView;
            mScaleStopFrequency = mStopFrequencyView;

            return super.onScaleBegin(detector);
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            double scaleFactor = detector.getScaleFactor();
            double focusX = detector.getFocusX();
            double currentSpan = mScaleStopFrequency - mScaleStartFrequency;
            double newCenter = mScaleStartFrequency + currentSpan * (focusX / (float) getWidth());
            double newSpan = currentSpan / scaleFactor;

            if (newSpan < 0.2f) {
                newSpan = 0.2f;
            }

            mStartFrequencyView = newCenter - newSpan / 2.0f;
            mStopFrequencyView = newCenter + newSpan / 2.0f;

            if (mStartFrequencyView < mStartFrequency) {
                mStartFrequencyView = mStartFrequency;
            }
            if (mStopFrequencyView > mStopFrequency) {
                mStopFrequencyView = mStopFrequency;
            }

            Log.d("spectrum", String.format(Locale.US, "scale=%f", scaleFactor));


            return false;
        }

        @Override
        public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
            Log.d("spectrum", "onScaleEnd()");

            if (mRangeStartEventListener != null) {
                mRangeStartEventListener.onRangeChange(mStartFrequencyView);
            }
            if (mRangeEndEventListener != null) {
                mRangeEndEventListener.onRangeChange(mStopFrequencyView);
            }

            super.onScaleEnd(detector);
        }
    }
}
