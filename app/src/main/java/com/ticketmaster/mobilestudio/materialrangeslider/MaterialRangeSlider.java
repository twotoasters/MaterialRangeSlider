package com.ticketmaster.mobilestudio.materialrangeslider;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import com.twotoasters.servos.util.DensityUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Slider following Material Design with two movable targets
 * that allow user to select a range of integers.
 */
public class MaterialRangeSlider extends View {

    public interface RangeSliderListener {
        void onMaxChanged(int newValue);
        void onMinChanged(int newValue);
    }

    //Padding that is always added to both sides of slider, in addition to layout_margin
    private static final int HORIZONTAL_PADDING = 80;
    private static final int DEFAULT_TOUCH_TARGET_SIZE = Math.round(DensityUtils.dpToPx(40));
    private static final int DEFAULT_UNPRESSED_RADIUS = 15;
    private static final int DEFAULT_PRESSED_RADIUS = 40;
    private static final int DEFAULT_INSIDE_RANGE_STROKE_WIDTH = 8;
    private static final int DEFAULT_OUTSIDE_RANGE_STROKE_WIDTH = 4;
    private static final int DEFAULT_MAX = 100;

    private float unpressedRadius;
    private float pressedRadius;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int lineStartX;
    private int lineEndX;
    private int lineLength;
    private float minTargetRadius = 0;
    private float maxTargetRadius = 0;
    private int minPosition = 0;
    private int maxPosition = 0;
    private int midY = 0;
    //List of event IDs touching targets
    private Set<Integer> isTouchingMinTarget = new HashSet<>();
    private Set<Integer> isTouchingMaxTarget = new HashSet<>();
    private int min = 0;
    private int max = DEFAULT_MAX;
    private int range;
    private float convertFactor;
    private RangeSliderListener rangesliderListener;
    private int targetColor;
    private int insideRangeColor;
    private int outsideRangeColor;
    private int colorControlNormal;
    private int colorControlHighlight;
    private float insideRangeLineStrokeWidth;
    private float outsideRangeLineStrokeWidth;
    private ObjectAnimator minAnimator;
    private ObjectAnimator maxAnimator;
    boolean lastTouchedMin;

    private Integer startingMin;
    private Integer startingMax;

    public MaterialRangeSlider(Context context) {
        super(context);
        init(null);
    }

    public MaterialRangeSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public MaterialRangeSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public void init(AttributeSet attrs) {
        getDefaultColors();
        getDefaultMeasurements();

        if (attrs != null) {
            //get attributes passed in XML
            TypedArray styledAttrs = getContext().obtainStyledAttributes(attrs,
                    R.styleable.MaterialRangeSlider, 0, 0);
            targetColor = styledAttrs.getColor(R.styleable.MaterialRangeSlider_insideRangeLineColor,
                    colorControlNormal);
            insideRangeColor = styledAttrs.getColor(R.styleable.MaterialRangeSlider_insideRangeLineColor,
                    colorControlNormal);
            outsideRangeColor = styledAttrs.getColor(R.styleable.MaterialRangeSlider_outsideRangeLineColor,
                    colorControlHighlight);
            min = styledAttrs.getInt(R.styleable.MaterialRangeSlider_min, min);
            max = styledAttrs.getInt(R.styleable.MaterialRangeSlider_max, max);

            unpressedRadius = styledAttrs.getDimension(R.styleable.MaterialRangeSlider_unpressedTargetRadius, DEFAULT_UNPRESSED_RADIUS);
            pressedRadius = styledAttrs.getDimension(R.styleable.MaterialRangeSlider_pressedTargetRadius, DEFAULT_PRESSED_RADIUS);
            insideRangeLineStrokeWidth = styledAttrs.getDimension(R.styleable.MaterialRangeSlider_insideRangeLineStrokeWidth, DEFAULT_INSIDE_RANGE_STROKE_WIDTH);
            outsideRangeLineStrokeWidth = styledAttrs.getDimension(R.styleable.MaterialRangeSlider_outsideRangeLineStrokeWidth, DEFAULT_OUTSIDE_RANGE_STROKE_WIDTH);

            styledAttrs.recycle();
        }

        minTargetRadius = unpressedRadius;
        maxTargetRadius = unpressedRadius;
        range = max - min;

        minAnimator = getMinTargetAnimator(true);
        maxAnimator = getMaxTargetAnimator(true);
    }

    /**
     * Get default colors from theme.  Compatible with 5.0+ themes and AppCompat themes.
     * Will attempt to get 5.0 colors, if not avail fallback to AppCompat, and if not avail use
     * black and gray.
     * These will be used if colors are not set in xml.
     */
    private void getDefaultColors() {
        TypedValue typedValue = new TypedValue();

        TypedArray materialStyledAttrs = getContext().obtainStyledAttributes(typedValue.data, new int[]{
                android.R.attr.colorControlNormal,
                android.R.attr.colorControlHighlight
        });

        TypedArray appcompatMaterialStyledAttrs = getContext().obtainStyledAttributes(typedValue.data, new int[]{
                android.support.v7.appcompat.R.attr.colorControlNormal,
                android.support.v7.appcompat.R.attr.colorControlHighlight
        });
        colorControlNormal = materialStyledAttrs.getColor(0, appcompatMaterialStyledAttrs.getColor(0, android.R.color.holo_blue_dark));
        colorControlHighlight = materialStyledAttrs.getColor(1, appcompatMaterialStyledAttrs.getColor(1, android.R.color.black));

        targetColor = colorControlNormal;
        insideRangeColor = colorControlHighlight;

        materialStyledAttrs.recycle();
        appcompatMaterialStyledAttrs.recycle();
    }

    /**
     * Get default measurements to use for radius and stroke width.
     * These are used if measurements are not set in xml.
     */
    private void getDefaultMeasurements() {
        pressedRadius = Math.round(DensityUtils.dpToPx(DEFAULT_PRESSED_RADIUS));
        unpressedRadius = Math.round(DensityUtils.dpToPx(DEFAULT_UNPRESSED_RADIUS));
        insideRangeLineStrokeWidth = Math.round(DensityUtils.dpToPx(DEFAULT_INSIDE_RANGE_STROKE_WIDTH));
        outsideRangeLineStrokeWidth = Math.round(DensityUtils.dpToPx(DEFAULT_OUTSIDE_RANGE_STROKE_WIDTH));
    }

    private ObjectAnimator getMinTargetAnimator(boolean touching) {
        final ObjectAnimator anim = ObjectAnimator.ofFloat(this,
                "minTargetRadius",
                minTargetRadius,
                touching ? pressedRadius : unpressedRadius);
        anim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                invalidate();
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                anim.removeAllListeners();
                super.onAnimationEnd(animation);
            }
        });
        anim.setInterpolator(new AccelerateInterpolator());
        return anim;
    }

    private ObjectAnimator getMaxTargetAnimator(boolean touching) {
        final ObjectAnimator anim = ObjectAnimator.ofFloat(this,
                "maxTargetRadius",
                maxTargetRadius,
                touching ? pressedRadius : unpressedRadius);
        anim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                invalidate();
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                anim.removeAllListeners();
            }
        });
        anim.setInterpolator(new AccelerateInterpolator());
        return anim;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int desiredWidth = widthSize;

        int desiredHeight = 96;

        int width = desiredWidth;
        int height = desiredHeight;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(desiredWidth, widthSize);
        } else {
            width = desiredWidth;
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = desiredHeight;
        }

        lineLength = (width - HORIZONTAL_PADDING * 2);
        midY = height / 2;
        lineStartX = HORIZONTAL_PADDING;
        lineEndX = lineLength + HORIZONTAL_PADDING;

        calculateConvertFactor();

        setSelectedMin(startingMin != null ? startingMin : min);
        setSelectedMax(startingMax != null ? startingMax : max);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawEntireRangeLine(canvas);
        drawSelectedRangeLine(canvas);
        drawSelectedTargets(canvas);
    }

    private void drawEntireRangeLine(Canvas canvas) {
        paint.setColor(outsideRangeColor);
        paint.setStrokeWidth(outsideRangeLineStrokeWidth);
        canvas.drawLine(lineStartX, midY, lineEndX, midY, paint);
    }

    private void drawSelectedRangeLine(Canvas canvas) {
        paint.setStrokeWidth(insideRangeLineStrokeWidth);
        paint.setColor(insideRangeColor);
        canvas.drawLine(minPosition, midY, maxPosition, midY, paint);
    }

    private void drawSelectedTargets(Canvas canvas) {
        paint.setColor(targetColor);
        canvas.drawCircle(minPosition, midY, minTargetRadius, paint);
        canvas.drawCircle(maxPosition, midY, maxTargetRadius, paint);
    }

    //user has touched outside the target, lets jump to that position
    private void jumpToPosition(int index, MotionEvent event) {
        if (event.getX(index) > maxPosition && event.getX(index) <= lineEndX) {
            maxPosition = (int) event.getX(index);
            invalidate();
            callMaxChangedCallbacks();
        } else if (event.getX(index) < minPosition && event.getX(index) >= lineStartX) {
            minPosition = (int) event.getX(index);
            invalidate();
            callMinChangedCallbacks();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled())
            return false;

        final int actionIndex = event.getActionIndex();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (lastTouchedMin) {
                    if (!checkTouchingMinTarget(actionIndex, event)
                            && !checkTouchingMaxTarget(actionIndex, event)) {
                        jumpToPosition(actionIndex, event);
                    }
                } else if (!checkTouchingMaxTarget(actionIndex, event)
                        && !checkTouchingMinTarget(actionIndex, event)) {
                    jumpToPosition(actionIndex, event);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                isTouchingMinTarget.remove(event.getPointerId(actionIndex));
                isTouchingMaxTarget.remove(event.getPointerId(actionIndex));
                if (isTouchingMinTarget.isEmpty()) {
                    minAnimator.cancel();
                    minAnimator = getMinTargetAnimator(false);
                    minAnimator.start();
                }
                if (isTouchingMaxTarget.isEmpty()) {
                    maxAnimator.cancel();
                    maxAnimator = getMaxTargetAnimator(false);
                    maxAnimator.start();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    if (isTouchingMinTarget.contains(event.getPointerId(i))) {
                        int touchX = (int) event.getX(i);
                        touchX = clamp(touchX, lineStartX, lineEndX);
                        if (touchX >= maxPosition) {
                            maxPosition = touchX;
                            callMaxChangedCallbacks();
                        }
                        minPosition = touchX;
                        callMinChangedCallbacks();
                    }
                    if (isTouchingMaxTarget.contains(event.getPointerId(i))) {
                        int touchX = (int) event.getX(i);
                        touchX = clamp(touchX, lineStartX, lineEndX);
                        if (touchX <= minPosition) {
                            minPosition = touchX;
                            callMinChangedCallbacks();
                        }
                        maxPosition = touchX;
                        callMaxChangedCallbacks();
                    }
                }
                invalidate();
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    if (lastTouchedMin) {
                        if (!checkTouchingMinTarget(i, event)
                                && !checkTouchingMaxTarget(i, event)) {
                            jumpToPosition(i, event);
                        }
                    } else if (!checkTouchingMaxTarget(i, event)
                            && !checkTouchingMinTarget(i, event)) {
                        jumpToPosition(i, event);
                    }
                }

                break;

            case MotionEvent.ACTION_CANCEL:
                isTouchingMinTarget.clear();
                isTouchingMaxTarget.clear();
                break;

            default:
                break;
        }

        return true;
    }

    /**
     * Checks if given index is touching the min target.  If touching start animation.
     */
    private boolean checkTouchingMinTarget(int index, MotionEvent event) {
        if (isTouchingMinTarget(index, event)) {
            lastTouchedMin = true;
            isTouchingMinTarget.add(event.getPointerId(index));
            if (!minAnimator.isRunning()) {
                minAnimator = getMinTargetAnimator(true);
                minAnimator.start();
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if given index is touching the max target.  If touching starts animation.
     */
    private boolean checkTouchingMaxTarget(int index, MotionEvent event) {
        if (isTouchingMaxTarget(index, event)) {
            lastTouchedMin = false;
            isTouchingMaxTarget.add(event.getPointerId(index));
            if (!maxAnimator.isRunning()) {
                maxAnimator = getMaxTargetAnimator(true);
                maxAnimator.start();
            }
            return true;
        }
        return false;
    }

    private void callMinChangedCallbacks() {
        if (rangesliderListener != null) {
            rangesliderListener.onMinChanged(getSelectedMin());
        }
    }

    private void callMaxChangedCallbacks() {
        if (rangesliderListener != null) {
            rangesliderListener.onMaxChanged(getSelectedMax());
        }
    }

    private boolean isTouchingMinTarget(int pointerIndex, MotionEvent event) {
        return event.getX(pointerIndex) > minPosition - DEFAULT_TOUCH_TARGET_SIZE
                && event.getX(pointerIndex) < minPosition + DEFAULT_TOUCH_TARGET_SIZE
                && event.getY(pointerIndex) > midY - DEFAULT_TOUCH_TARGET_SIZE
                && event.getY(pointerIndex) < midY + DEFAULT_TOUCH_TARGET_SIZE;
    }

    private boolean isTouchingMaxTarget(int pointerIndex, MotionEvent event) {
        return event.getX(pointerIndex) > maxPosition - DEFAULT_TOUCH_TARGET_SIZE
                && event.getX(pointerIndex) < maxPosition + DEFAULT_TOUCH_TARGET_SIZE
                && event.getY(pointerIndex) > midY - DEFAULT_TOUCH_TARGET_SIZE
                && event.getY(pointerIndex) < midY + DEFAULT_TOUCH_TARGET_SIZE;
    }

    private void calculateConvertFactor() {
        convertFactor = ((float) range) / lineLength;
    }

    public int getSelectedMin() {
        return Math.round((minPosition - lineStartX) * convertFactor + min);
    }

    public int getSelectedMax() {
        return Math.round((maxPosition - lineStartX) * convertFactor + min);
    }

    public void setStartingMinMax(int startingMin, int startingMax) {
        this.startingMin = startingMin;
        this.startingMax = startingMax;
    }

    private void setSelectedMin(int selectedMin) {
        minPosition = Math.round(((selectedMin - min) / convertFactor) + lineStartX);
        callMinChangedCallbacks();
    }

    private void setSelectedMax(int selectedMax) {
        maxPosition = Math.round(((selectedMax - min) / convertFactor) + lineStartX);
        callMaxChangedCallbacks();
    }

    public void setRangeSliderListener(RangeSliderListener listener) {
        rangesliderListener = listener;
    }

    public RangeSliderListener getRangeSliderListener() {
        return rangesliderListener;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
        range = max - min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
        range = max - min;
    }

    /**
     * Resets selected values to MIN and MAX.
     */
    public void reset() {
        minPosition = lineStartX;
        maxPosition = lineEndX;
        if (rangesliderListener != null) {
            rangesliderListener.onMinChanged(getSelectedMin());
            rangesliderListener.onMaxChanged(getSelectedMax());
        }
        invalidate();
    }

    public void setMinTargetRadius(float minTargetRadius) {
        this.minTargetRadius = minTargetRadius;
    }

    public void setMaxTargetRadius(float maxTargetRadius) {
        this.maxTargetRadius = maxTargetRadius;
    }

    /**
     * Keeps Number value inside min/max bounds by returning min or max if outside of
     * bounds.  Otherwise will return the value without altering.
     */
    private <T extends Number> T clamp(@NonNull T value, @NonNull T min, @NonNull T max) {
        if (value.doubleValue() > max.doubleValue()) {
                return max;
            } else if (value.doubleValue() < min.doubleValue()) {
                return min;
            }
            return value;
    }
}
