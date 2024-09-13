package de.leckasemmel.sonde1.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

import de.leckasemmel.sonde1.R;

import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.round;
import static java.lang.Math.sin;

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;


public class SMeterView extends View {
    // Constants defining the S-meter geometry
    final float minAngle = -53.0f;      // Scale minimum angle
    final float maxAngle = 53.0f;       // Scale maximum angle
    final float scalePercent = 0.76f;   // Scale radius in percent of bounding rectangle height
    final float anchorPercent = 0.015f; // Distance of anchor from bottom in percent of bounding rectangle height
    final float pointerLength = 1.072f; // Length of pointer (multiple of scale radius)
    final float pointerStrokeWidth = 8f/400f;           // Stroke width of pointer (fraction of view height)
    final float angleS9 = 0;            // Angle of S9 position
    final float S9Dbm = -93;            // dBm at S9 position
    final float minDbm = -128;          // dBm at leftmost scale position
    final float maxDbm = -33;           // dBm at rightmost scale position
    final float tick100 = 1.09f;        // Tick marker end for full steps (in percent of scale radius)
    final float tick50 = 1.04f;         // Tick marker end for half steps (in percent of scale radius)
    final float markerTextPos = 1.15f;  // Marker text position (in percent of scale radius)
    final float markerTextSize = 0.057f;// Marker text size (in percent of bounding rectangle height)
    final float markerTextStrokeWidth = 1f/400f;        // Marker text stroke width (fraction of view height)
    final float keyTextSize = 0.08f;    // Key text size (in percent of bounding rectangle height)
    final float keyText1Angle = -58.0f; // Angle for key text 1
    final float keyText2Angle = 58.0f;  // Angle for key text 2
    final float scaleMajorStrokeWidth = 3f/400f;        // Stroke width for scale arc (fraction of view height)
    final float scaleMinorStrokeWidth = 2f/400f;        // Stroke width for minor scale ticks (fraction of view height)

    final float linear_xS9 = 0.5f;
    final float linear_xMinDbm = 0.014f;
    final float linear_scaleStrokeWidth = 0.1f;
    final float linear_textStrokeWidth = 0.02f;
    final float linear_textSize = 0.025f;                   // Fraction of view width
    final float linear_barWidth = 0.017361f;                // Fraction of view width
    final float linear_tickLength = 0.011111f;              // Fraction of view width

    private int mMode;
    private int mColorBackground;
    private int mColorScale;
    private int mColorNumbers;
    private int mColorNumbersAccent;
    private float dbm;
    private Bitmap mBitmap;
    private Paint mPaint;
    private Rect rBoundsI;
    private RectF rScaleF;
    private Rect textBounds;

    public SMeterView (Context context) {
        super(context);
        doWhatAConstructorMustDo();
    }

    public SMeterView (Context context, AttributeSet attrs) {
        super(context, attrs);
        doWhatAConstructorMustDo();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SMeterView);
        mMode = a.getInt(R.styleable.SMeterView_mode, 0);
        mColorBackground = a.getInt(R.styleable.SMeterView_colorBackground, Color.WHITE);
        mColorScale = a.getInt(R.styleable.SMeterView_colorScale, Color.DKGRAY);
        mColorNumbers = a.getInt(R.styleable.SMeterView_colorNumbers, Color.BLACK);
        mColorNumbersAccent = a.getInt(R.styleable.SMeterView_colorNumbersAccent, Color.RED);
        a.recycle();
    }

    private void doWhatAConstructorMustDo () {
        mMode = 0;
        rBoundsI = new Rect();
        rScaleF = new RectF();
        textBounds = new Rect();
        mPaint = new Paint();
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        dbm = -999.0f;
        invalidate();
    }

    public void setStyle (int style) {
        if ((style >= 0) && (style <= 3)) {
            mMode = style;
            invalidate();
        }
    }

    @BindingAdapter("level")
    public static void setLevel (SMeterView view, Double d) {
        float f = -999.0f;
        if (d != null) {
            f = d.floatValue();
        }
        view.dbm = f;
        view.invalidate();
    }

    @BindingAdapter("smeterStyle")
    public static void setStyle (SMeterView view, Integer style) {
        int mode = 0;
        if (style != null) {
            mode = style;
        }
        view.mMode = mode;
        view.invalidate();
    }

    // Convert dBm to angle
    private float level2angle (float level) {
        float angle = minAngle;

        switch (mMode) {
            case 0:case 1:case 2:
                if (level > maxDbm) {
                    angle = maxAngle;
                } else if (level > S9Dbm) {
                    angle = angleS9 + (level - S9Dbm) * (maxAngle - angleS9) / (maxDbm - S9Dbm);
                } else if (level > minDbm) {
                    angle = minAngle + (level - minDbm) * (angleS9 - minAngle) / (S9Dbm - minDbm);
                }
                break;
        }
        return angle;
    }

    // (Re)create the background bitmap
    private void createBackground(int width, int height) {
        int offset;
        float startX, startY;
        float stopX, stopY;
        String s;
        int alpha = isEnabled() ? 255 : 100;

        if (mBitmap == null) {
            Bitmap.Config config = Bitmap.Config.ARGB_8888;
            mBitmap = Bitmap.createBitmap(width, height, config);
        }
        Canvas canvas = new Canvas(mBitmap);

        if (mMode == 3) {
            mPaint.setStrokeWidth(1f);
            mPaint.setColor(mColorBackground);
            mPaint.setAlpha(150);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            canvas.drawRect(rBoundsI, mPaint);
        }
        else {
            // Background color
            mPaint.setStrokeWidth(1f);
            mPaint.setColor(mColorBackground);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            // Set reference point to the anchor
            float anchorX = rBoundsI.centerX();
            float anchorY = rBoundsI.bottom - (float) rBoundsI.height() * anchorPercent;
            canvas.translate(anchorX, anchorY);

            // Rectangle as bounds for drawing the scale arc
            float scaleRadius = scalePercent * (float) rBoundsI.height();
            rScaleF.set(-scaleRadius, -scaleRadius, scaleRadius, scaleRadius);

            // Draw scale arc
            mPaint.setStrokeWidth(scaleMajorStrokeWidth * rBoundsI.height());
            mPaint.setColor(mColorScale);
            mPaint.setAlpha(alpha);
            mPaint.setStyle(Paint.Style.STROKE);
            canvas.drawArc(rScaleF, 270.0f + minAngle, maxAngle - minAngle, false, mPaint);
        }

        if (mMode == 0) {   // S-Meter
            // Marker for every full S step from S9 downwards
            offset = 0;
            while (S9Dbm + offset >= minDbm) {
                // Angle for this marker
                float markerAngle = minAngle + (S9Dbm + offset - minDbm) * (angleS9 - minAngle) / (S9Dbm - minDbm);
                startX = scalePercent * (float) rBoundsI.height() * (float) sin(markerAngle / 57.2958);
                startY = -scalePercent * (float) rBoundsI.height() * (float) cos(markerAngle / 57.2958);
                if ((offset % 6) == 0) {
                    stopX = tick100 * startX;
                    stopY = tick100 * startY;
                    mPaint.setStrokeWidth(scaleMajorStrokeWidth * rBoundsI.height());
                    mPaint.setColor(mColorScale);
                    canvas.drawLine(startX, startY, stopX, stopY, mPaint);

                    s = String.format(Locale.US, "%d", 9 + offset / 6);
                    mPaint.setColor(mColorNumbers);
                    mPaint.setStrokeWidth(markerTextStrokeWidth * rBoundsI.height());
                    mPaint.setTextSize(markerTextSize * rBoundsI.height());
                    mPaint.getTextBounds(s, 0, s.length(), textBounds);
                    mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                    stopX = markerTextPos * startX;
                    stopY = markerTextPos * startY;
                    canvas.drawText(s, stopX - textBounds.width() / 2.0f, stopY + textBounds.height() / 2.0f, mPaint);
                } else {
                    stopX = tick50 * startX;
                    stopY = tick50 * startY;
                    mPaint.setStrokeWidth(scaleMinorStrokeWidth * rBoundsI.height());
                    mPaint.setColor(mColorScale);
                    canvas.drawLine(startX, startY, stopX, stopY, mPaint);
                }

                offset -= 3;
            }

            // Marker for every 10 dB step from S9 upwards
            offset = 5;
            while (S9Dbm + offset <= maxDbm) {
                // Angle for this marker
                float markerAngle = angleS9 + (float) offset * (maxAngle - angleS9) / (maxDbm - S9Dbm);
                startX = scalePercent * (float) rBoundsI.height() * (float) sin(markerAngle / 57.2958);
                startY = -scalePercent * (float) rBoundsI.height() * (float) cos(markerAngle / 57.2958);
                if ((offset % 10) == 0) {
                    stopX = tick100 * startX;
                    stopY = tick100 * startY;
                    mPaint.setStrokeWidth(scaleMajorStrokeWidth * rBoundsI.height());
                    mPaint.setColor(mColorScale);
                    canvas.drawLine(startX, startY, stopX, stopY, mPaint);

                    s = String.format(Locale.US, "%+d", offset);
                    mPaint.setColor(mColorNumbersAccent);
                    mPaint.setStrokeWidth(markerTextStrokeWidth * rBoundsI.height());
                    mPaint.setTextSize(markerTextSize * rBoundsI.height());
                    mPaint.getTextBounds(s, 0, s.length(), textBounds);
                    mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                    stopX = markerTextPos * startX;
                    stopY = markerTextPos * startY;
                    canvas.drawText(s, stopX - textBounds.width() / 2.0f, stopY + textBounds.height() / 2.0f, mPaint);
                } else {
                    stopX = tick50 * startX;
                    stopY = tick50 * startY;
                    mPaint.setStrokeWidth(scaleMinorStrokeWidth * rBoundsI.height());
                    mPaint.setColor(mColorScale);
                    canvas.drawLine(startX, startY, stopX, stopY, mPaint);
                }

                offset += 5;
            }

            // Key symbols
            s = "S";
            mPaint.setColor(mColorNumbers);
            mPaint.setStrokeWidth(markerTextStrokeWidth * rBoundsI.height());
            mPaint.setTextSize(keyTextSize * rBoundsI.height());
            mPaint.getTextBounds(s, 0, s.length(), textBounds);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            startX = scalePercent * (float) rBoundsI.height() * (float) sin(keyText1Angle / 57.2958);
            startY = -scalePercent * (float) rBoundsI.height() * (float) cos(keyText1Angle / 57.2958);
            canvas.drawText(s, startX - textBounds.width() / 2.0f, startY + textBounds.height() / 2.0f, mPaint);

            s = "dB";
            mPaint.setColor(mColorNumbersAccent);
            mPaint.setStrokeWidth(markerTextStrokeWidth * rBoundsI.height());
            mPaint.setTextSize(keyTextSize * rBoundsI.height());
            mPaint.getTextBounds(s, 0, s.length(), textBounds);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            startX = scalePercent * (float) rBoundsI.height() * (float) sin(keyText2Angle / 57.2958);
            startY = -scalePercent * (float) rBoundsI.height() * (float) cos(keyText2Angle / 57.2958);
            canvas.drawText(s, startX - textBounds.width() / 2.0f, startY + textBounds.height() / 2.0f, mPaint);
        }

        if (mMode == 1) {   // dBµV
            // Marker for every 5 dB
            float minDbuv = minDbm + 106.9897f;
            float maxDbuv = maxDbm + 106.9897f;
            float lev1 = (float) (5.0 * ceil(minDbuv / 5.0));
            while (lev1 <= maxDbuv) {
                // Angle for this marker
                float markerAngle = level2angle(lev1 - 106.9897f);
                startX = scalePercent * (float) rBoundsI.height() * (float) sin(markerAngle / 57.2958);
                startY = -scalePercent * (float) rBoundsI.height() * (float) cos(markerAngle / 57.2958);
                if ((round(lev1) % 10) == 0) {
                    stopX = tick100 * startX;
                    stopY = tick100 * startY;
                    mPaint.setStrokeWidth(scaleMajorStrokeWidth * rBoundsI.height());
                    mPaint.setColor(mColorScale);
                    mPaint.setAlpha(alpha);
                    canvas.drawLine(startX, startY, stopX, stopY, mPaint);

                    s = String.format(Locale.US, "%+.0f", lev1);
                    if (lev1 >= S9Dbm + 106.9897f) {
                        mPaint.setColor(mColorNumbersAccent);
                    } else {
                        mPaint.setColor(mColorNumbers);
                    }
                    mPaint.setAlpha(alpha);
                    mPaint.setStrokeWidth(markerTextStrokeWidth * rBoundsI.height());
                    mPaint.setTextSize(markerTextSize * rBoundsI.height());
                    mPaint.getTextBounds(s, 0, s.length(), textBounds);
                    mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                    stopX = markerTextPos * startX;
                    stopY = markerTextPos * startY;
                    canvas.drawText(s, stopX - textBounds.width() / 2.0f, stopY + textBounds.height() / 2.0f, mPaint);
                } else {
                    stopX = tick50 * startX;
                    stopY = tick50 * startY;
                    mPaint.setStrokeWidth(scaleMinorStrokeWidth * rBoundsI.height());
                    mPaint.setColor(mColorScale);
                    mPaint.setAlpha(alpha);
                    canvas.drawLine(startX, startY, stopX, stopY, mPaint);
                }

                lev1 += 5.0;
            }

            // Key symbols
            s = "dBµV";
            mPaint.setColor(mColorNumbers);
            mPaint.setAlpha(alpha);
            mPaint.setStrokeWidth(markerTextStrokeWidth * rBoundsI.height());
            mPaint.setTextSize(keyTextSize * rBoundsI.height() * 2);
            mPaint.getTextBounds(s, 0, s.length(), textBounds);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            startX = 0;
            startY = -scalePercent * (float) rBoundsI.height() * (float) cos(keyText2Angle / 57.2958);
            canvas.drawText(s, startX - textBounds.width() / 2.0f, startY + textBounds.height() / 2.0f, mPaint);
        }

        if (mMode == 2) {   // dBm
            // Marker for every 5 dB
            float lev2 = (float) (5.0 * ceil(minDbm / 5.0));
            while (lev2 <= maxDbm) {
                // Angle for this marker
                float markerAngle = level2angle(lev2);
                startX = scalePercent * (float) rBoundsI.height() * (float) sin(markerAngle / 57.2958);
                startY = -scalePercent * (float) rBoundsI.height() * (float) cos(markerAngle / 57.2958);
                if ((round(lev2) % 10) == 0) {
                    stopX = tick100 * startX;
                    stopY = tick100 * startY;
                    mPaint.setStrokeWidth(scaleMajorStrokeWidth * rBoundsI.height());
                    mPaint.setColor(mColorScale);
                    mPaint.setAlpha(alpha);
                    canvas.drawLine(startX, startY, stopX, stopY, mPaint);

                    s = String.format(Locale.US, "%+.0f", lev2);
                    if (lev2 >= S9Dbm) {
                        mPaint.setColor(mColorNumbersAccent);
                    } else {
                        mPaint.setColor(mColorNumbers);
                    }
                    mPaint.setAlpha(alpha);
                    mPaint.setStrokeWidth(markerTextStrokeWidth * rBoundsI.height());
                    mPaint.setTextSize(markerTextSize * rBoundsI.height());
                    mPaint.getTextBounds(s, 0, s.length(), textBounds);
                    mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                    stopX = markerTextPos * startX;
                    stopY = markerTextPos * startY;
                    canvas.drawText(s, stopX - textBounds.width() / 2.0f, stopY + textBounds.height() / 2.0f, mPaint);
                } else {
                    stopX = tick50 * startX;
                    stopY = tick50 * startY;
                    mPaint.setStrokeWidth(scaleMinorStrokeWidth * rBoundsI.height());
                    mPaint.setColor(mColorScale);
                    mPaint.setAlpha(alpha);
                    canvas.drawLine(startX, startY, stopX, stopY, mPaint);
                }

                lev2 += 5.0;
            }

            // Key symbols
            s = "dBm";
            mPaint.setColor(mColorNumbers);
            mPaint.setAlpha(alpha);
            mPaint.setStrokeWidth(markerTextStrokeWidth * rBoundsI.height());
            mPaint.setTextSize(keyTextSize * rBoundsI.height() * 2);
            mPaint.getTextBounds(s, 0, s.length(), textBounds);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            startX = 0;
            startY = -scalePercent * (float) rBoundsI.height() * (float) cos(keyText2Angle / 57.2958);
            canvas.drawText(s, startX - textBounds.width() / 2.0f, startY + textBounds.height() / 2.0f, mPaint);
        }

        if (mMode == 3) {   // Linear gauge
            // Marker for every full S step from S9 downwards
            offset = 0;
            while (S9Dbm + offset >= minDbm) {
                // x position for this marker
                float markerX = linear_xMinDbm + (S9Dbm + offset - minDbm) * (linear_xS9 - linear_xMinDbm) / (S9Dbm - minDbm);
                markerX *= rBoundsI.width();
                startY = linear_barWidth * rBoundsI.width();
                stopY = (linear_barWidth + linear_tickLength) * rBoundsI.width();
                mPaint.setStrokeWidth(linear_scaleStrokeWidth * rBoundsI.height());
                mPaint.setColor(mColorScale);
                canvas.drawLine(markerX, startY, markerX, stopY, mPaint);

                s = String.format(Locale.US, "S%d", 9 + offset / 6);
                mPaint.setColor(mColorNumbers);
                mPaint.setStrokeWidth(linear_textStrokeWidth * rBoundsI.height());
                mPaint.setTextSize(linear_textSize * rBoundsI.width());
                mPaint.getTextBounds(s, 0, s.length(), textBounds);
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);

                float textX = markerX - 0.5f * textBounds.width();
                float textY = 0.6f * rBoundsI.height() + textBounds.height();
                canvas.drawText(s, textX, textY, mPaint);

                offset -= 6;
            }

            // Marker for every 10 dB step from S9 upwards
            offset = 10;
            while (S9Dbm + offset < maxDbm) {
                float markerX = linear_xS9 + offset * (1.0f - linear_xS9) / (maxDbm - S9Dbm);

                markerX *= rBoundsI.width();
                startY = linear_barWidth * rBoundsI.width();
                stopY = (linear_barWidth + linear_tickLength) * rBoundsI.width();
                mPaint.setStrokeWidth(linear_scaleStrokeWidth * rBoundsI.height());
                mPaint.setColor(mColorNumbersAccent);
                canvas.drawLine(markerX, startY, markerX, stopY, mPaint);

                s = String.format(Locale.US, "%+d", offset);
                mPaint.setColor(mColorNumbersAccent);
                mPaint.setStrokeWidth(linear_textStrokeWidth * rBoundsI.height());
                mPaint.setTextSize(linear_textSize * rBoundsI.width());
                mPaint.getTextBounds(s, 0, s.length(), textBounds);
                mPaint.setStyle(Paint.Style.FILL_AND_STROKE);

                float textX = markerX - 0.5f * textBounds.width();
                float textY = 0.6f * rBoundsI.height() + textBounds.height();
                canvas.drawText(s, textX, textY, mPaint);

                offset += 10;
            }
        }
    }

    @Override
    public void onDraw (@NonNull Canvas canvas) {
        super.onDraw(canvas);

        canvas.getClipBounds(rBoundsI);

        // Background
        createBackground(rBoundsI.width(), rBoundsI.height());
        canvas.drawBitmap(mBitmap, 0, 0, mPaint);

        if (mMode == 3) {
            float barLength = 0;
            if (dbm > maxDbm) {
                barLength = 1.0f;
            } else if (dbm > S9Dbm) {
                barLength = linear_xS9 + (dbm - S9Dbm) * (1.0f - linear_xS9) / (maxDbm - S9Dbm);
            } else if (dbm > minDbm) {
                barLength = (dbm - minDbm) / (S9Dbm - minDbm) * linear_xS9;
            }
            barLength *= rBoundsI.width();

            mPaint.setStrokeWidth(1);
            mPaint.setColor(mColorScale);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, barLength, linear_barWidth * rBoundsI.width(), mPaint);
        }
        else {
            // Set reference point to the anchor
            float anchorX = rBoundsI.centerX();
            float anchorY = rBoundsI.bottom - (float) rBoundsI.height() * anchorPercent;
            canvas.translate(anchorX, anchorY);

            if (isEnabled()) {
                float length;
                float stopX, stopY;

                // Find angle for pointer depending on current level
                float angle = level2angle(dbm);

                // Draw pointer
                length = pointerLength * scalePercent * (float) rBoundsI.height();
                stopX = length * (float) sin(angle / 57.2958);
                stopY = -length * (float) cos(angle / 57.2958);
                mPaint.setStrokeWidth(pointerStrokeWidth * rBoundsI.height());
                mPaint.setColor(mColorScale);
                canvas.drawLine(0, 0, stopX, stopY, mPaint);
            }
        }
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        // Find out what the system has planned
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        float width;
        float height;

        if (mMode == 3) {
            width = widthSize;
            height = switch (heightMode) {
                case MeasureSpec.EXACTLY -> heightSize;
                case MeasureSpec.UNSPECIFIED -> widthSize / 20.0f;
                case MeasureSpec.AT_MOST ->
                    widthSize / 20.0f > heightSize ? heightSize : widthSize / 20.0f;
                default -> throw new IllegalStateException("Unexpected heightMode: " + heightMode);
            };
        }
        else {
            // The optimum aspect ratio
            final float optimumAspectRatio = 720f / 400f;

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

            // Correct one of the parameters in case we didn't get the desired aspect ratio
            if (width / height < optimumAspectRatio) {
                // Leave the width as is, and adapt the height
                height = width / optimumAspectRatio;
            } else {
                // Leave the height as is, and adapt the width
                width = height * optimumAspectRatio;
            }
        }

        // Inform system about our decision
        setMeasuredDimension(round(width), round(height));

        // Prepare a background bitmap for the proposed dimensions
//        createBackground(round(width), round(height));
    }
}
