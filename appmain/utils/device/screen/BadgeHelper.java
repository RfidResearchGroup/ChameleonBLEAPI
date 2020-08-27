package com.proxgrind.chameleon.utils.device.screen;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.IntDef;
import androidx.transition.Slide;

import com.google.android.material.tabs.TabLayout;
import com.proxgrind.chameleon.utils.system.LogUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.regex.Pattern;

/**
 * 作者:东芝(2018/8/23).
 * 支持 重叠目标模式 和 放目标右上角但不重叠的模式 两种模式 通过setOverlap 设置,  默认为false=不重叠
 */

public class BadgeHelper extends View {
    private static final String TAG = "BadgeHelper";
    private float density;
    private Paint mTextPaint;
    private Paint mBackgroundPaint;
    private String text = "0";
    private int number;

    @Type
    private int type = Type.TYPE_POINT;
    private boolean isOverlap;
    private final RectF rect = new RectF();
    private int badgeColor = 0xFFD3321B; //默认的小红点颜色
    private int textColor = 0xFFFFFFff;
    private float textSize;
    private int w;
    private int h;
    private boolean isSetup;
    private boolean mIgnoreTargetPadding;
    private boolean isCenterVertical;
    private int leftMargin;
    private int topMargin;
    private int rightMargin;
    private int bottomMargin;
    private int gravity = -99999;

    @IntDef({Type.TYPE_POINT, Type.TYPE_TEXT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
        int TYPE_POINT = 0;
        int TYPE_TEXT = 1;
    }

    public BadgeHelper(Context context) {
        super(context);
    }

    private void init(@Type int type, boolean isOverlap) {
        this.type = type;
        this.isOverlap = isOverlap;
        density = getResources().getDisplayMetrics().density;
        switch (type) {
            case Type.TYPE_POINT:
                mBackgroundPaint = new Paint();
                mBackgroundPaint.setStyle(Paint.Style.FILL);
                mBackgroundPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
                mBackgroundPaint.setColor(badgeColor);
                //计算小红点无文本情况下的小红点大小,  按屏幕像素计算, 如果你有你自己认为更好的算法, 改这里即可
                w = h = Math.round(density * 7f);
                break;
            case Type.TYPE_TEXT:
                mBackgroundPaint = new Paint();
                mBackgroundPaint.setStyle(Paint.Style.FILL);
                mBackgroundPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
                mBackgroundPaint.setColor(badgeColor);
                mTextPaint = new Paint();
                mTextPaint.setStyle(Paint.Style.FILL);
                mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
                mTextPaint.setColor(textColor);//文本颜色
                if (textSize == 0) {
                    mTextPaint.setTextSize(density * 10);//文本大小按屏幕像素 计算, 没写死是为了适配各种屏幕,  但如果你有你认为更合理的计算方式 你可以改这里
                } else {
                    mTextPaint.setTextSize(textSize);//使用自定义大小
                }
                //计算小红点有文本情况下的小红点大小,  按文本宽高计算, 如果你有你自己认为更好的算法, 改这里即可
                float textWidth = getTextWidth(text, mTextPaint);
                float textHeight = getTextHeight(text, mTextPaint);
                w = Math.round(textWidth * 1.4f);//让背景比文本大一点
                h = Math.round(textHeight * 1.2f);
                break;
        }
    }

    /**
     * 设置Margin 可用于做偏移
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     * @return
     */
    public BadgeHelper setBadgeMargins(int left, int top, int right, int bottom) {
        leftMargin = left;
        topMargin = top;
        rightMargin = right;
        bottomMargin = bottom;
        return this;
    }

    /**
     * 设置Gravity居中
     *
     * @return
     */
    public BadgeHelper setBadgeCenterVertical() {
        isCenterVertical = true;
        return this;
    }

    public BadgeHelper setBadgeGravity(int gravity) {
        this.gravity = gravity;
        return this;
    }

    /**
     * 设置小红点类型
     *
     * @param type
     * @return
     */
    public BadgeHelper setBadgeType(@Type int type) {
        this.type = type;
        return this;
    }

    /**
     * 设置小红点大小, 默认自动适配
     *
     * @param textSize
     * @return
     */
    public BadgeHelper setBadgeTextSize(int textSize) {
        Context c = getContext();
        Resources r;
        if (c == null) {
            r = Resources.getSystem();
        } else {
            r = c.getResources();
        }
        this.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize, r.getDisplayMetrics());
        return this;
    }

    /**
     * 设置小红点文字颜色, 默认白
     *
     * @param textColor
     * @return
     */
    public BadgeHelper setBadgeTextColor(int textColor) {
        this.textColor = textColor;
        return this;
    }

    /**
     * 设置重叠模式, 默认是false(不重叠)
     *
     * @param isOverlap 是否把小红点重叠到目标View之上
     * @return
     */
    public BadgeHelper setBadgeOverlap(boolean isOverlap) {
        return setBadgeOverlap(isOverlap, false);
    }

    /**
     * 设置重叠模式, 默认是false(不重叠)
     *
     * @param isOverlap             是否把小红点重叠到目标View之上
     * @param isIgnoreTargetPadding 是否忽略目标View的padding
     * @return
     */
    public BadgeHelper setBadgeOverlap(boolean isOverlap, boolean isIgnoreTargetPadding) {
        this.isOverlap = isOverlap;
        this.mIgnoreTargetPadding = isIgnoreTargetPadding;
        if (!isOverlap && isIgnoreTargetPadding) {
            Log.w(TAG, "警告:只有重叠模式isOverlap=true 设置mIgnoreTargetPadding才有意义");
        }
        return this;
    }

    /**
     * 设置小红点颜色
     *
     * @param mBadgeColor
     * @return
     */
    public BadgeHelper setBadgeColor(int mBadgeColor) {
        this.badgeColor = mBadgeColor;
        return this;
    }

    /**
     * 设置小红点大小
     *
     * @param w
     * @param h
     * @return
     */
    public BadgeHelper setBadgeSize(int w, int h) {
        this.w = w;
        this.h = h;
        return this;
    }

    /**
     * 是否显示
     *
     * @param enable
     */
    public void setBadgeEnable(boolean enable) {
        setVisibility(enable ? VISIBLE : INVISIBLE);
    }

    /**
     * 设置小红点的文字
     *
     * @param number
     */
    public void setBadgeNumber(int number) {
        this.number = number;
        this.text = String.valueOf(number);
        if (isSetup) {
            if (number == 0) {
                setVisibility(INVISIBLE);
            } else {
                setVisibility(VISIBLE);
            }
            invalidate();
        }
    }

    /**
     * 设置小红点的文字
     *
     * @param text
     */
    public void setBadgeText(CharSequence text) {
        if (isSetup) {
            if (text == null) {
                setVisibility(INVISIBLE);
            } else {
                this.text = text.toString();
                init(Type.TYPE_TEXT, isOverlap);
                setVisibility(VISIBLE);
            }
            invalidate();
        }
    }

    public void bindToTargetView(TabLayout target, int tabIndex) {
        TabLayout.Tab tab = target.getTabAt(tabIndex);
        View targetView = null;
        View tabView = null;
        try {
            Field viewField = TabLayout.Tab.class.getDeclaredField("mView");
            viewField.setAccessible(true);
            targetView = tabView = (View) viewField.get(tab);

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (tabView != null) {
                Field mTextViewField = tabView.getClass().getDeclaredField("mTextView");//"mIconView"
                mTextViewField.setAccessible(true);
                targetView = (View) mTextViewField.get(tabView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (targetView != null) {
            bindToTargetView(targetView);
        }
    }

    /**
     * 绑定小红点到目标View的右上角
     *
     * @param target
     */
    public void bindToTargetView(View target) {
        init(type, isOverlap);
        if (getParent() != null) {
            ((ViewGroup) getParent()).removeView(this);
        }
        if (target == null) {
            return;
        }
        if (target.getParent() instanceof ViewGroup) {
            // 先移除目标view！
            ViewGroup parent = (ViewGroup) target.getParent();
            int groupIndex = parent.indexOfChild(target);
            parent.removeView(target);
            // 根据条件放置到一个新的容器当中!
            if (isOverlap) {//[小红点与目标View重叠]模式
                FrameLayout badgeContainer = new FrameLayout(getContext());
                ViewGroup.LayoutParams targetLayoutParams = target.getLayoutParams();
                badgeContainer.setLayoutParams(targetLayoutParams);
                target.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                parent.addView(badgeContainer, groupIndex, targetLayoutParams);
                badgeContainer.addView(target);
                badgeContainer.addView(this);
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();
                if (gravity != -99999) {
                    layoutParams.gravity = gravity;
                } else {
                    if (isCenterVertical) {
                        layoutParams.gravity = Gravity.CENTER_VERTICAL;
                    } else {
                        layoutParams.gravity = Gravity.END | Gravity.TOP;
                    }
                }
                if (mIgnoreTargetPadding) {
                    layoutParams.rightMargin = target.getPaddingRight() - w;
                    layoutParams.topMargin = target.getPaddingTop() - h / 2;
                }
                setLayoutParams(layoutParams);
            } else { //[小红点放右侧]模式
                LinearLayout badgeContainer = new LinearLayout(getContext());
                badgeContainer.setOrientation(LinearLayout.HORIZONTAL);
                ViewGroup.LayoutParams targetLayoutParams = target.getLayoutParams();
                badgeContainer.setLayoutParams(targetLayoutParams);
                target.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                parent.addView(badgeContainer, groupIndex, targetLayoutParams);
                badgeContainer.addView(target);
                badgeContainer.addView(this);
                if (gravity != -99999) {
                    badgeContainer.setGravity(gravity);
                } else {
                    if (isCenterVertical) {
                        badgeContainer.setGravity(Gravity.CENTER_VERTICAL);
                    }
                }
            }
            boolean hasSetMargin = leftMargin > 0 || topMargin > 0 || rightMargin > 0 || bottomMargin > 0;
            if (hasSetMargin && getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) getLayoutParams();
                p.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);
                setLayoutParams(p);
            }
            isSetup = true;
        } else if (target.getParent() == null) {
            throw new IllegalStateException("目标View不能没有父布局!");
        }
        if (number == 0) {
            setVisibility(INVISIBLE);
        } else {
            setVisibility(VISIBLE);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (w > 0 && h > 0) {
            setMeasuredDimension(w, h);
        } else {
            throw new IllegalStateException("如果你自定义了小红点的宽高,就不能设置其宽高小于0 ,否则请不要设置!");
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 计算文本的大小
        float textWidth = getTextWidth(text, mTextPaint);
        float textHeight = getTextHeight(text, mTextPaint);
        //这里不用解释了 很简单 就是画一个圆形和文字
        rect.left = 0;
        rect.top = 0;
        // 设置宽高!
        rect.right = getWidth();
        rect.bottom = getHeight();
        // 先绘制圆圈!!
        canvas.drawRoundRect(rect, getWidth() / 2, getWidth() / 2, mBackgroundPaint);
        if (type == Type.TYPE_TEXT) {
            float x = getWidth() / 2 - textWidth / 2;
            float y = getHeight() / 2 + textHeight / 2;
            // 根据给出的坐标值绘制图片!
            canvas.drawText(text, x, y, mTextPaint);
        }
        super.onDraw(canvas);
    }

    public boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

    private float getTextWidth(String text, Paint p) {
        return p.measureText(text, 0, text.length());
    }

    private float getTextHeight(String text, Paint p) {
        Rect rect = new Rect();
        p.getTextBounds(text, 0, text.length(), rect);
        return rect.height();
    }
}
