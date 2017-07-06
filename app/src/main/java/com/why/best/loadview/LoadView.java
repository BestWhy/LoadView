package com.why.best.loadview;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by why on 2017/7/5.
 */

public class LoadView extends View {

    private int mTextSize;
    private int mStrokeColor;
    private int mTextColor;
    private String mText;
    private int mDefaultWidth;
    private int mDefaultTextSize;
    private int mDefaultRadiu;
    private int mRadiu;
    private int mTopBottomPadding;
    private int mLeftRightPadding;
    private int mBackgroundColor;
    private int mProgressColor;
    private int mProgressSecondColor;
    private float mProgressWidth;
    private Drawable mSuccessedDrawable;
    private Drawable mErrorDrawable;
    private Drawable mPauseDrawable;
    private TextPaint mTextPaint;
    private int mTextWidth;
    private int rectWidth;
    private Path mPath;
    private RectF leftRect;
    private RectF rightRect;
    private RectF contentRect;
    private Paint mPaint;
    private int left;
    private int top;
    private int rigth;
    private int bottom;
    private OnClickListener mListener;
    private ObjectAnimator mShringkAnimator;
    private RectF mProgressRect;
    private float circleSweep;
    private boolean progressReverse;
    private int mProgressStartAngel;
    private ObjectAnimator mLoadAnimator;


    enum State {
        INITIAL,    // 初始状态
        FOLDING,    // 正在伸缩
        LOADING,    // 正在加载
        ERROR,      // 加载失败
        SUCCESSED,  // 加载成功
        PAUSED      // 加载暂停
    }

    private State mCurrentState;

    private boolean isUnfold;

    LoadListenner mLoadListenner;

    public LoadListenner getLoadListenner() {
        return mLoadListenner;
    }

    public void setLoadListenner(LoadListenner loadListenner) {
        this.mLoadListenner = loadListenner;
    }

    public LoadView(Context context) {
        this(context, null);
    }

    public LoadView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mDefaultRadiu = 40;

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.LoadView);
        mDefaultTextSize = 24;
        mTextSize = ta.getDimensionPixelSize(R.styleable.LoadView_android_textSize, mDefaultTextSize);
        mStrokeColor = ta.getColor(R.styleable.LoadView_stroke_color, Color.RED);
        mTextColor = ta.getColor(R.styleable.LoadView_content_color, Color.WHITE);
        mText = ta.getString(R.styleable.LoadView_android_text);
        mRadiu = ta.getDimensionPixelOffset(R.styleable.LoadView_radiu, mDefaultRadiu);
        mTopBottomPadding = ta.getDimensionPixelOffset(R.styleable.LoadView_contentPaddingTB, 20);
        mLeftRightPadding = ta.getDimensionPixelOffset(R.styleable.LoadView_contentPaddingLR, 20);
        mBackgroundColor = ta.getColor(R.styleable.LoadView_backColor, Color.WHITE);
        mProgressColor = ta.getColor(R.styleable.LoadView_progressColor, Color.WHITE);
        mProgressSecondColor = ta.getColor(R.styleable.LoadView_progressSecondColor, Color.WHITE);
        mProgressWidth = ta.getDimensionPixelOffset(R.styleable.LoadView_progressedWidth, 2);

        mSuccessedDrawable = ta.getDrawable(R.styleable.LoadView_loadSuccessDrawable);
        mErrorDrawable = ta.getDrawable(R.styleable.LoadView_loadErrorDrawable);
        mPauseDrawable = ta.getDrawable(R.styleable.LoadView_loadPauseDrawable);

        ta.recycle();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(mStrokeColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mProgressWidth);

        mDefaultWidth = 200;

        mTextPaint = new TextPaint();
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(mTextSize);


        rectWidth = mDefaultWidth - mDefaultRadiu * 2;

        leftRect = new RectF();
        rightRect = new RectF();
        contentRect = new RectF();
        isUnfold = true;

        mListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentState == State.FOLDING) {
                    return;
                }
                if (mCurrentState == State.INITIAL) {
                    if (isUnfold) {
                        shringk();
                    }
                } else if (mCurrentState == State.LOADING) {
                    mCurrentState = State.PAUSED;
                    cancelAnimation();
                    invaidateSelft();
                } else if (mCurrentState == State.PAUSED) {
                    if (mLoadListenner != null) {
                        mLoadListenner.needLoading();
                        load();
                    }

                } else if (mCurrentState == State.SUCCESSED) {
                    if (mLoadListenner != null) {
                        mLoadListenner.onClick(true);
                    }

                } else if (mCurrentState == State.ERROR) {
                    if (mLoadListenner != null) {
                        mLoadListenner.onClick(false);
                    }

                }
            }
        };

        setOnClickListener(mListener);

        mCurrentState = State.INITIAL;

        if (mSuccessedDrawable == null) {
            mSuccessedDrawable = context.getResources().getDrawable(R.drawable.yes);
        }
        if (mErrorDrawable == null) {
            mErrorDrawable = context.getResources().getDrawable(R.drawable.no);
        }
        if (mPauseDrawable == null) {
            mPauseDrawable = context.getResources().getDrawable(R.drawable.pause);
        }

        mProgressSecondColor = Color.parseColor("#c3c3c3");
        mProgressColor = Color.WHITE;


    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //获取宽度和模式
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);

        //获取高度和模式
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        //保存最终尺寸
        int resultW = widthSize;
        int resultH = heightSize;

        //确定中间矩形尺寸
        int contentW = 0;
        int contentH = 0;

        if (widthMode == MeasureSpec.AT_MOST) {
            mTextWidth = (int) mTextPaint.measureText(mText);
            contentW += mTextWidth + mLeftRightPadding * 2 + mRadiu * 2;

            resultW = contentW < widthSize ? contentW : widthSize;
        }

        if (heightMode == MeasureSpec.AT_MOST) {
            contentH += mTopBottomPadding * 2 + mTextSize;
            resultH = contentH < heightSize ? contentH : heightSize;
        }

        resultW = resultW < 2 * mRadiu ? 2 * mRadiu : resultW;
        resultH = resultH < 2 * mRadiu ? 2 * mRadiu : resultH;

        //修正圆的半径
        mRadiu = resultH / 2;
        rectWidth = resultW - 2 * mRadiu;
        setMeasuredDimension(resultW, resultH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        drawPath(canvas, cx, cy);

        //绘制中间文字
        int descent = (int) mTextPaint.getFontMetrics().descent;
        int ascent = (int) mTextPaint.getFontMetrics().ascent;
        int delta = Math.abs(ascent) - descent;

        int circleR = mRadiu / 2;

        if (mCurrentState == State.INITIAL) {
            canvas.drawText(mText, cx, cy + delta / 2, mTextPaint);

        } else if (mCurrentState == State.LOADING) {

            if (mProgressRect == null) {
                mProgressRect = new RectF();
            }
            mProgressRect.set(cx - circleR, cy - circleR, cx + circleR, cy + circleR);

            mPaint.setColor(mProgressSecondColor);
            //先绘制背景圆
            canvas.drawCircle(cx, cy, circleR, mPaint);
            mPaint.setColor(mProgressColor);

            if (circleSweep != 360) {
                mProgressStartAngel = progressReverse ? 270 : (int) (270 + circleSweep);
                canvas.drawArc(
                        mProgressRect,
                        mProgressStartAngel,
                        progressReverse ? circleSweep : (int) (360 - circleSweep),
                        false, mPaint
                );
            }
            mPaint.setColor(mBackgroundColor);
        } else if (mCurrentState == State.PAUSED) {
            mPauseDrawable.setBounds(cx - circleR, cy - circleR, cx + circleR, cy + circleR);
            mPauseDrawable.draw(canvas);
        } else if (mCurrentState == State.SUCCESSED) {
            mSuccessedDrawable.setBounds(cx - circleR, cy - circleR, cx + circleR, cy + circleR);
            mSuccessedDrawable.draw(canvas);
        } else if (mCurrentState == State.ERROR) {
            mErrorDrawable.setBounds(cx - circleR, cy - circleR, cx + circleR, cy + circleR);
            mErrorDrawable.draw(canvas);
        }

    }

    //画外轮廓
    private void drawPath(Canvas canvas, int cx, int cy) {
        if (mPath == null) {
            mPath = new Path();
        }

        mPath.reset();

        left = cx - rectWidth / 2 - mRadiu;
        top = 0;
        rigth = cx + rectWidth / 2 + mRadiu;
        bottom = getHeight();

        leftRect.set(left, top, left + mRadiu * 2, bottom);
        rightRect.set(rigth - mRadiu * 2, top, rigth, bottom);
        contentRect.set(cx - rectWidth / 2, top, cx + rectWidth / 2, bottom);

        //path 起始位置
        mPath.moveTo(cx - rectWidth / 2, bottom);
        //左半边圆
        mPath.arcTo(leftRect, 90.0f, 180f);

        //链接右半边
        mPath.lineTo(cx + rectWidth / 2, top);

        //右半边圆
        mPath.arcTo(rightRect, 270.0f, 180f);

        mPath.close();

        // 以填充的方向将图形填充为指定的背景色
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mBackgroundColor);
        canvas.drawPath(mPath, mPaint);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mStrokeColor);

    }

    private void invaidateSelft() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invalidate();
        } else {
            postInvalidate();
        }
    }

    private void setRectWidth(int width) {
        this.rectWidth = width;
        invaidateSelft();
    }

    public void shringk() {
        if (mShringkAnimator == null) {
            mShringkAnimator = ObjectAnimator.ofInt(this, "rectWidth", rectWidth, 0);
        }
        mShringkAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isUnfold = false;
                load();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mShringkAnimator.setDuration(500);
        mShringkAnimator.start();
        mCurrentState = State.FOLDING;
    }


    private void setCircleSweep(float sweep) {
        this.circleSweep = sweep;
        invaidateSelft();
    }

    public void load() {
        if (mLoadAnimator == null) {
            mLoadAnimator = ObjectAnimator.ofFloat(this, "circleSweep", 0, 360);
        }
        mLoadAnimator.setDuration(1000);
        mLoadAnimator.setRepeatMode(ValueAnimator.RESTART);
        mLoadAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mLoadAnimator.removeAllListeners();
        mLoadAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                progressReverse = !progressReverse;
            }
        });
        mLoadAnimator.start();
        mCurrentState = State.LOADING;
    }


    private void cancelAnimation() {
        if (mShringkAnimator != null && mShringkAnimator.isRunning()) {
            mShringkAnimator.removeAllListeners();
            mShringkAnimator.cancel();
            mShringkAnimator = null;
        }
        if (mLoadAnimator != null && mLoadAnimator.isRunning()) {
            mLoadAnimator.removeAllListeners();
            mLoadAnimator.cancel();
            mLoadAnimator = null;
        }
    }


    public interface LoadListenner {
        void onClick(boolean isSuccessed);

        void needLoading();
    }


    public void loadSuccessed() {
        mCurrentState = State.SUCCESSED;
        cancelAnimation();
        invaidateSelft();
    }

    public void loadFailed() {
        mCurrentState = State.ERROR;
        cancelAnimation();
        invaidateSelft();
    }

    public void reset() {
        mCurrentState = State.INITIAL;
        rectWidth = getWidth() - mRadiu * 2;
        isUnfold = true;
        cancelAnimation();
        invaidateSelft();
    }

}
