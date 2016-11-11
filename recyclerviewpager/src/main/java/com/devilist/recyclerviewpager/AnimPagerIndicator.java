package com.devilist.recyclerviewpager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by zengpu on 2016/11/4.
 */
public class AnimPagerIndicator extends LinearLayout {

    private Context context;

    private int mScreenWidth = 0;
    /**
     * 屏幕可见item个数，奇数
     */
    private int mVisibleCount = 7;
    /**
     * 每个item宽度
     */
    private int mItemWidth = mScreenWidth / mVisibleCount;

    /**
     * item Y向偏移
     */
    private int mItemOffsetY = 0;

    /**
     * 手指触摸过程中动画持续时间
     */
    private long mTouchAnimDuration = 100;

    /**
     * 手指抬起后Y向动画持续时间
     */
    private long mSelectAnimDuration = 100;

    /**
     * 存储每个itemView
     */
    private List<View> mItemViewList = new ArrayList<>();

    /**
     * 存储所有动画,集中管理
     */
    private List<AnimatorSet> mAnimatorSetList = new ArrayList<>();

    /**
     * 记录每一个item的实时偏移量
     */
    private Map<Integer, Float> mItemOffsetList = new HashMap<>();

    /**
     * 上一次滚动第一个可见的item位置
     */
    private int mLastFirstVisablePosition = 0;

    /**
     * 上一次滚动手指滑动的目标位置
     */
    private int mLastTargetPosition = 1;

    /**
     * 是否是indicator的touch事件触发的联动
     */
    private boolean isTouchEventMode = false;

    /**
     * 上一次touch事件结束时间
     */
    private long mLastActionEventTime = 0;

    private boolean isTouchEnable = true;

    private RecyclerView viewPager;

    public AnimPagerIndicator(Context context) {
        this(context, null);
    }

    public AnimPagerIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnimPagerIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
        mVisibleCount = 7;
        mItemWidth = mScreenWidth / mVisibleCount;
        setOrientation(LinearLayout.HORIZONTAL);
    }

    /**
     * 初始化数据
     *
     * @param indicatorIconList
     */
    public void setData(List<Drawable> indicatorIconList) {
        if (this.getChildCount() != 0) {
            this.removeAllViews();
            mItemViewList.clear();
            mItemOffsetList.clear();
        }

        for (int i = 0; i < indicatorIconList.size(); i++) {
            LinearLayout itemView = createIndicatorItem(indicatorIconList.get(i));
            this.addView(itemView);
            mItemViewList.add(itemView.getChildAt(1));
            mItemOffsetList.put(i, 0f);
        }
        MarginLayoutParams params = (MarginLayoutParams) getLayoutParams();
        params.width = LayoutParams.MATCH_PARENT;
        params.height = LayoutParams.WRAP_CONTENT;
        params.bottomMargin = -mItemOffsetY;

        this.invalidate();

        Animator animator = tranYAnimation(0, -mItemOffsetY, mSelectAnimDuration);
        animator.start();
    }

    /**
     * 添加数据
     *
     * @param indicatorIconList
     */
    public void addData(List<Drawable> indicatorIconList) {

        int ori_size = mItemOffsetList.size();

        for (int i = 0; i < indicatorIconList.size(); i++) {
            LinearLayout itemView = createIndicatorItem(indicatorIconList.get(i));
            this.addView(itemView);
            mItemViewList.add(itemView.getChildAt(1));
            mItemOffsetList.put(ori_size + i, 0f);
        }

        int scrollCount = Math.min(mVisibleCount / 2, indicatorIconList.size());
        // 往左滚动
        scrollBy(scrollCount * mItemWidth, 0);
        this.invalidate();
        // 滚动完成后，相关参数复位
        isTouchEventMode = false;
        mLastFirstVisablePosition = findFirstVisibleItemPosition();
    }

    /**
     * 创建indicator item
     *
     * @param iconDrawable
     * @return
     */
    private LinearLayout createIndicatorItem(Drawable iconDrawable) {
        // 根布局
        LinearLayout ll_item = new LinearLayout(context);
        ll_item.setOrientation(LinearLayout.VERTICAL);
        // 子布局，Y偏移量
        LinearLayout ll_sub_offset = new LinearLayout(context);
        ll_sub_offset.setOrientation(LinearLayout.VERTICAL);
        // 子布局，icon
        LinearLayout ll_sub_icon = new LinearLayout(context);
        ll_sub_icon.setOrientation(LinearLayout.VERTICAL);
        ll_sub_icon.setGravity(Gravity.CENTER_HORIZONTAL);
        ll_sub_icon.setBackground(context.getResources().getDrawable(R.drawable.shape_rvp_indicator_bg));

        ImageView iv_icon = new ImageView(context);

        ll_item.addView(ll_sub_offset);
        ll_item.addView(ll_sub_icon);
        ll_sub_icon.addView(iv_icon);

        int rootPadding = 6;
        int iconPadding = 8;
        mItemOffsetY = mItemWidth - rootPadding * 2 - iconPadding;
        int mItemHeight = mItemWidth * 11 / 10 + mItemOffsetY;

        // 根布局
        LayoutParams rootLayoutParams = new LayoutParams(mItemWidth, mItemHeight);
        ll_item.setLayoutParams(rootLayoutParams);
        ll_item.setPadding(rootPadding, 0, rootPadding, 0);

        // 偏移量布局
        LayoutParams offsetLayoutParams = new LayoutParams(mItemWidth - rootPadding * 2, mItemOffsetY);
        ll_sub_offset.setLayoutParams(offsetLayoutParams);

        // icon布局
        LayoutParams iconLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        ll_sub_icon.setLayoutParams(iconLayoutParams);
        ll_sub_icon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);

        // icon
        ViewGroup.LayoutParams iconLayoutparams = iv_icon.getLayoutParams();
        int iconWidth = mItemWidth - rootPadding * 2 - iconPadding * 2;
        iconLayoutparams.width = iconWidth;
        iconLayoutparams.height = iconWidth;
        iv_icon.setLayoutParams(iconLayoutparams);
        iv_icon.setImageDrawable(iconDrawable);

        return ll_item;
    }

    /**
     * 添加viewpager
     *
     * @param viewPager
     */
    public void setRecyclerViewPager(final RecyclerView viewPager) {
        this.viewPager = viewPager;
        if (null == viewPager) {
            // // TODO: 异常处理
        }

        this.viewPager.setOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) viewPager.getLayoutManager();

                    //获取viewPager当前第一个可见的item位置,用以下公式反推出indicator第一个可见的item位置
                    // selectionPosition = firstVisablePosition + targetPosition - 1;

                    int mFirstVisablePosition = layoutManager.findFirstVisibleItemPosition() + 1 - mLastTargetPosition;

                    Log.e("AnimPagerIndicator2", "viewpager : mFirstVisablePosition " + layoutManager.findFirstVisibleItemPosition());
                    Log.e("AnimPagerIndicator2", "new mFirstVisablePosition " + mFirstVisablePosition);
                    Log.e("AnimPagerIndicator2", "isTouchEventMode " + isTouchEventMode);

                    if (!isTouchEventMode) {
                        // 如果不是indicator的touch事件触发的联动,需要对mLastTargetPosition修正
                        // 往右滑动，position减小;往左滑动，position增加
                        mLastTargetPosition += mFirstVisablePosition - mLastFirstVisablePosition;
                        // 边界控制
                        if (mLastTargetPosition < 1)
                            mLastTargetPosition = 1;
                        if (mLastTargetPosition > mVisibleCount)
                            mLastTargetPosition = mVisibleCount;
                    }
                    Log.e("AnimPagerIndicator2", "************ viewPager Scroll finish ************");
                    doSelectAnimation(mLastTargetPosition, mLastFirstVisablePosition);
                }
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        isTouchEventMode = true;
        float x_offset = e.getX();
        int targetPosition = computeTargetPositionFromOffsetX(x_offset);
        long action_time = 0;
        boolean isDoAnimation = false;

        if (!isTouchEnable)
            return false;

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.e("AnimPagerIndicator2", " ACTION_DOWN   " + System.currentTimeMillis());
                // 屏蔽双击事件
                long second_action_event_time = System.currentTimeMillis();
                if (mLastActionEventTime > 0) {
                    if (second_action_event_time - mLastActionEventTime < 500) {
                        mLastActionEventTime = second_action_event_time;
                        return false;
                    } else {
                        doTouchAnimation(targetPosition);
                    }
                } else {
                    doTouchAnimation(targetPosition);
                }

                break;
            case MotionEvent.ACTION_MOVE:
                action_time = e.getEventTime() - e.getDownTime();
                Log.e("AnimPagerIndicator2", "time_move : " + action_time);
                if (action_time < mTouchAnimDuration)
                    isDoAnimation = false;
                else
                    isDoAnimation = action_time % (2 * mTouchAnimDuration) <= 20;
                Log.e("AnimPagerIndicator2", "ACTION_MOVE " + "isDoAnimation : " + isDoAnimation);

                if (isDoAnimation && isAllAnimatorFinish()) {
                    doTouchAnimation(targetPosition);
                }
                break;
            case MotionEvent.ACTION_UP:
                mLastActionEventTime = System.currentTimeMillis();
                mLastFirstVisablePosition = findFirstVisibleItemPosition();
                mLastTargetPosition = targetPosition;
                Log.e("AnimPagerIndicator2", "ACTION_UP  " + "time_up is : " + action_time);
                Log.e("AnimPagerIndicator2", "mLastFirstVisablePosition : " + mLastFirstVisablePosition);
                Log.e("AnimPagerIndicator2", "mLastTargetPosition : " + mLastTargetPosition);
                scrollPagerToPosition(targetPosition);
                break;
        }

        return true;
    }

    /**
     * 通过手指在屏幕的位置寻找目标item
     *
     * @param x_offset 手指相对布局左边缘的偏移量
     * @return 目标位置；范围在 1 ~maxVisableCount 之间
     */
    private int computeTargetPositionFromOffsetX(float x_offset) {
        int targetPosition = 1;
        targetPosition = (int) x_offset / mItemWidth + 1;
        if (targetPosition > mVisibleCount)
            targetPosition = mVisibleCount;
        return targetPosition;
    }

    /**
     * 获得第一个可见的item位置,通过布局的偏移量与子布局宽度的比值确定
     *
     * @return
     */
    private int findFirstVisibleItemPosition() {
        return Math.abs(getScrollX() / mItemWidth);
    }

    /**
     * 触发ACTION_DOWN 和ACTION_MOVE事件时,可见item的属性动画动画
     *
     * @param targetPosition 手指在屏幕上移动时的目标位置
     */
    private void doTouchAnimation(final int targetPosition) {
        int firstVisiblePosition = findFirstVisibleItemPosition();
        AnimatorSet animatorSet = touchAnimation(targetPosition, firstVisiblePosition);
        if (null != animatorSet)
            mAnimatorSetList.add(animatorSet);
    }

    /**
     * 触发ACTION_UP事件时,可见item的属性动画动画
     *
     * @param targetPosition       手指离开屏幕时的目标位置
     * @param firstVisiblePosition 第一个可见的item的位置
     */
    private void doSelectAnimation(final int targetPosition, final int firstVisiblePosition) {
        // 为防止TouchAnimation还没完成就开始SelectAnimation导致item动画混乱，做延迟处理，
        // 延迟时间为TouchAnimation的持续时间
        this.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e("AnimPagerIndicator2", "************ touch animation finish ************");
                AnimatorSet selectAnimatorSet = selectAnimation(targetPosition, firstVisiblePosition);
                if (null != selectAnimatorSet)
                    mAnimatorSetList.add(selectAnimatorSet);
                doScrollXAnimation(targetPosition, firstVisiblePosition);
            }
        }, mTouchAnimDuration);

    }

    /**
     * X方向的滚动，SelectAnimation完成后执行
     *
     * @param targetPosition       手指离开屏幕时的目标位置
     * @param firstVisiblePosition 第一个可见的item的位置
     */
    private void doScrollXAnimation(final int targetPosition, final int firstVisiblePosition) {
        final int itemCount = this.getChildCount();
        final int midPosition = mVisibleCount / 2 + 1; // 中间位置
        int offsetX = 0; // 需要滚动的偏移量
        int scrollCount = 0; // 需要滚动的item个数

        int lastVisablePosition = firstVisiblePosition + mVisibleCount - 1;

        if (itemCount <= targetPosition)
            return;
        if (targetPosition < midPosition && firstVisiblePosition != 0) {
            // 往右滚动
            scrollCount = Math.min(midPosition - targetPosition, firstVisiblePosition);
            offsetX = -scrollCount * mItemWidth;
        }
        if (targetPosition > midPosition && lastVisablePosition != itemCount - 1) {
            // 往左滚动
            scrollCount = Math.min(targetPosition - midPosition, itemCount - lastVisablePosition - 1);
            offsetX = scrollCount * mItemWidth;
        }

        if (offsetX != 0) {
            final int finalOffsetX = offsetX;
            this.postDelayed(new Runnable() {
                @Override
                public void run() {
                    removeInvalidAnim();
                    scrollBy(finalOffsetX, 0);
                    invalidate();

                    // 滚动完成后，相关参数复位
                    isTouchEventMode = false;
                    mLastFirstVisablePosition = findFirstVisibleItemPosition();
                    mLastTargetPosition = midPosition;

                    Log.e("AnimPagerIndicator2", "reset mLastTargetPosition " + mLastTargetPosition);
                    Log.e("AnimPagerIndicator2", "reset mLastFirstVisablePosition " + mLastFirstVisablePosition);
                    Log.e("AnimPagerIndicator2", "************ select animation finish ************");
                }
            }, mSelectAnimDuration + 100);
        } else {
            removeInvalidAnim();
            isTouchEventMode = false;
            mLastFirstVisablePosition = findFirstVisibleItemPosition();
            mLastTargetPosition = midPosition;
            Log.e("AnimPagerIndicator2", "************ select animation finish ************");
        }
    }

    /**
     * viewpager的滚动
     *
     * @param targetPosition 手指滑动的目标位置，不是pager将要滚动的位置
     */
    private void scrollPagerToPosition(int targetPosition) {
        if (null != viewPager) {
            // 计算滚动的位置
            int firstVisablePosition = findFirstVisibleItemPosition();
            int selectionPosition = firstVisablePosition + targetPosition - 1;
            Log.e("AnimPagerIndicator2", "************ viewPager Scroll start ************");
            viewPager.smoothScrollToPosition(selectionPosition);
        } else {
            // todo 抛出异常处理
            doSelectAnimation(mLastTargetPosition, mLastFirstVisablePosition);
        }
    }


    public void setTouchEnable(boolean isTouchEnable) {
        this.isTouchEnable = isTouchEnable;
    }



    /**
     * 所有动画是否播放完毕
     *
     * @return
     */
    private boolean isAllAnimatorFinish() {
        removeInvalidAnim();
        return mAnimatorSetList.size() == 0;
    }

    /**
     * 清除播放完的动画
     */
    private void removeInvalidAnim() {

        if (mAnimatorSetList.size() == 0)
            return;

        List<AnimatorSet> temporaryList = new ArrayList<>();

        for (int i = 0; i < mAnimatorSetList.size(); i++) {
            if (mAnimatorSetList.get(i).isRunning()) {
                temporaryList.add(mAnimatorSetList.get(i));
            }
        }
        mAnimatorSetList.clear();
        mAnimatorSetList.addAll(temporaryList);
    }

    /**
     * 手指触摸时item的动画
     *
     * @param targetPosition        手指触摸的目标位置 [1,maxVisableCount]
     * @param firstVisiablePosition 第一个可见的item位置
     */
    public AnimatorSet touchAnimation(int targetPosition, final int firstVisiablePosition) {
        if (mItemViewList.size() == 0)
            return null;

        float offsetY = 0;
        float maxOffset = -(float) mItemOffsetY;

        AnimatorSet animatorSet = new AnimatorSet();
        List<Animator> animatorList = new ArrayList<>();

        Log.e("AnimPagerIndicator2", "************ touch animation start ************");
        Log.e("AnimPagerIndicator2", "targetPosition is:  " + targetPosition
                + " firstVisiablePosition is:  " + firstVisiablePosition);

        for (int i = 1; i <= mVisibleCount; i++) {
            //边界控制
            if (firstVisiablePosition + i - 1 > mItemViewList.size() - 1)
                break;

            Log.e("AnimPagerIndicator2", "currentPosition is:  " + (firstVisiablePosition + i - 1)
                    + " offsetY " + i + " is: " + mItemOffsetList.get(firstVisiablePosition + i - 1));

            if (i <= targetPosition) {
                offsetY = (i + mVisibleCount - targetPosition) * maxOffset / mVisibleCount;
            } else {
                offsetY = (-i + mVisibleCount + targetPosition) * maxOffset / mVisibleCount;
            }
            if (mItemOffsetList.get(firstVisiablePosition + i - 1) != offsetY) {
                Animator animator = tranYAnimation(firstVisiablePosition + i - 1, offsetY, mTouchAnimDuration);
                animatorList.add(animator);
            }
        }
        if (animatorList.size() != 0) {
            animatorSet.playTogether(animatorList);
            animatorSet.setDuration(mTouchAnimDuration);
            animatorSet.start();
        }
        return animatorSet;
    }

    /**
     * 手指抬起后item的动画
     *
     * @param targetPosition        手指触摸的目标位置 [1,maxVisableCount]
     * @param firstVisiablePosition 第一个可见的item位置
     */
    public AnimatorSet selectAnimation(int targetPosition, final int firstVisiablePosition) {
        if (mItemViewList.size() == 0)
            return null;

        AnimatorSet animatorSet = new AnimatorSet();
        List<Animator> animatorList = new ArrayList<>();
        Log.e("AnimPagerIndicator2", "************ select animation start ************");
        Log.e("AnimPagerIndicator2", "targetPosition is:  " + targetPosition
                + " firstVisiablePosition is:  " + firstVisiablePosition);
        for (int i = 1; i <= mVisibleCount; i++) {

            if (firstVisiablePosition + i - 1 > mItemViewList.size() - 1)
                break;

            Log.e("AnimPagerIndicator2", "currentPosition is:  " + (firstVisiablePosition + i - 1)
                    + " offsetY " + i + " is: " + mItemOffsetList.get(firstVisiablePosition + i - 1));

            if (i == targetPosition && mItemOffsetList.get(firstVisiablePosition + i - 1) > -mItemOffsetY) {
                Animator animator = tranYAnimation(firstVisiablePosition + i - 1, -mItemOffsetY, mSelectAnimDuration);
                animatorList.add(animator);

            } else if (i != targetPosition && mItemOffsetList.get(firstVisiablePosition + i - 1) < 0) {

                Animator animator = tranYAnimation(firstVisiablePosition + i - 1, 0, mSelectAnimDuration);
                animatorList.add(animator);
            }
        }
        if (animatorList.size() != 0) {
            animatorSet.playTogether(animatorList);
            animatorSet.setDuration(mSelectAnimDuration);
            animatorSet.start();
        }
        return animatorSet;
    }

    /**
     * Y向偏移属性动画
     *
     * @param position item位置
     * @param end      结束位置
     */
    private Animator tranYAnimation(final int position, final float end, long duration) {

        // 找到目标item
        final View view = mItemViewList.get(position);
        // 该item上一次动画结束后的偏移量
        float start = mItemOffsetList.get(position);
        // 本次动画过程中的实时偏移量
        final float[] current = {start};

        final ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationY", start, end);
        animator.setDuration(duration);

        animator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                // 动画啊结束，记录本次的偏移量
                mItemOffsetList.put(position, current[0]);
                Log.e("AnimPagerIndicator2", "finish offsetY " + position + " is: " + (int) current[0]
                        + "  end is : " + (int) end);
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // 实时更新的偏移量
                current[0] = (float) animation.getAnimatedValue("translationY");
            }
        });

        return animator;
    }
}
