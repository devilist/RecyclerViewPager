package com.devilist.recyclerviewpager;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ProgressBar;
import android.widget.Scroller;
import android.widget.TextView;


/**
 * 带刷新头的recyclerviewpager
 * Created by zengpu on 16/10/29.
 */

public class RefreshRecyclerViewPager extends ViewGroup implements AbsListView.OnScrollListener {

    /**
     * 帮助View滚动的辅助类Scroller
     */
    protected Scroller mScroller;

    protected RecyclerView mRecyclerView;

    /**
     * 右滑刷新刷新时显示的headerView
     */
    protected View mRefreshHeaderView;

    /**
     * 左滑加载更多时显示的footerView
     */
    protected View mLoadMoreFooterView;

    /**
     * 本次触摸滑动x坐标上的偏移量
     */
    protected int mXOffset;

    /**
     * 手指滑动时的摩擦系数
     */
    private  float mTouchScrollFrictionFactor = 0.35f;

    /**
     * 触发刷新加载操作的最小距离
     */
    protected int mTouchSlop;

    /**
     * 最初的滚动位置.第一次布局时滚动header的宽度的距离
     */
    protected int mInitScrollX = 0;
    /**
     * 最后一次触摸事件的X轴坐标
     */
    protected int mLastX = 0;

    /**
     * 空闲状态
     */
    public static final int STATUS_IDLE = 0;

    /**
     * 右滑状态, 还没有到达可刷新的状态
     */
    public static final int STATUS_PULL_TO_REFRESH = 1;

    /**
     * 右滑状态，达到可刷新状态
     */
    public static final int STATUS_RELEASE_TO_REFRESH = 2;
    /**
     * 刷新中
     */
    public static final int STATUS_REFRESHING = 3;

    /**
     * 左滑状态, 还没有到达可加载更多的状态
     */
    public static final int STATUS_PULL_TO_LOAD = 4;

    /**
     * 左滑状态，达到可加载更多状态
     */
    public static final int STATUS_RELEASE_TO_LOAD = 5;

    /**
     * 加载更多中
     */
    public static final int STATUS_LOADING = 6;

    /**
     * 当前状态
     */
    protected int mCurrentStatus = STATUS_IDLE;

    /**
     * 是否滚到了最右侧。滚到最右侧后执行左滑加载更多
     */
    protected boolean isScrollToRight = false;

    /**
     * 是否滚到了最左侧。滚到最左侧后执行右滑刷新
     */
    protected boolean isScrollToLeft = false;

    /**
     * header 中的文本标签
     */
    protected TextView mHeaderTipsTextView;

    /**
     * header中的进度条
     */
    protected ProgressBar mHeaderProgressBar;

    /**
     * footer 中的文本标签
     */
    protected TextView mFooterTipsTextView;

    /**
     * footer 中的进度条
     */
    protected ProgressBar mFooterProgressBar;

    /**
     * 屏幕宽度
     */
    private int mScreenWidth;
    /**
     * Header宽度
     */
    private int mHeaderWidth;
    /**
     * Footer宽度
     */
    private int mFooterWidth;

    /**
     * 右滑刷新监听器
     */
    protected OnRefreshListener mOnRefreshListener;
    /**
     * 左滑加载更多监听器
     */
    protected OnLoadMoreListener mLoadMoreListener;

    protected Context context;

    /**
     * 是否需要右滑刷新功能
     */
    private boolean isCanRefresh = true;

    /**
     * 是否需要左滑加载更多功能
     */
    private boolean isCanLoadMore = true;

    /**
     * 右滑刷新是否失败，用于处理失败后header的隐藏问题
     */
    private boolean isRefreshFailure = false;

    /**
     * 左滑加载是否失败，用于处理失败后footer的隐藏问题
     */
    private boolean isLoadFailure = false;

    public RefreshRecyclerViewPager(Context context) {
        this(context, null);
    }

    public RefreshRecyclerViewPager(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshRecyclerViewPager(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // 初始化Scroller对象
        mScroller = new Scroller(context);
        // 获取屏幕高度
        mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
        // header 的宽度为屏幕宽度的 1/3
        mHeaderWidth = mScreenWidth / 3;
        mFooterWidth = mScreenWidth / 3;
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        // 初始化整个布局
//        config = initHeaderAndFooter();
        initLayout(context);

    }

    /**
     * 初始化整个布局
     *
     * @param context
     */
    private final void initLayout(Context context) {
        /* 往布局里添加 headerView,mHeaderView = getChildAt(0)*/
        mRefreshHeaderView = LayoutInflater.from(context).inflate(R.layout.rvp_pull_to_refresh_header, this, false);
        mRefreshHeaderView.setLayoutParams(new LayoutParams(mHeaderWidth, LayoutParams.MATCH_PARENT));
        mRefreshHeaderView.setPadding(mHeaderWidth / 2, 0, 0, 0);
        addView(mRefreshHeaderView);

        // HEADER VIEWS
        mHeaderTipsTextView = (TextView) mRefreshHeaderView.findViewById(R.id.pull_to_refresh_text);
        mHeaderProgressBar = (ProgressBar) mRefreshHeaderView.findViewById(R.id.pull_to_refresh_progress);

        /* 初始化footerView，添加到布局里,mFooterView = getChildAt(1); */
        mLoadMoreFooterView = LayoutInflater.from(context).inflate(R.layout.rvp_pull_to_load_footer, this, false);
        mLoadMoreFooterView.setLayoutParams(new LayoutParams(mFooterWidth, LayoutParams.MATCH_PARENT));
        mLoadMoreFooterView.setPadding(0, 0, mFooterWidth / 2, 0);
        addView(mLoadMoreFooterView);

        mFooterProgressBar = (ProgressBar) mLoadMoreFooterView.findViewById(R.id.pull_to_loading_progress);
        mFooterTipsTextView = (TextView) mLoadMoreFooterView.findViewById(R.id.pull_to_loading_text);

    }


    /**
     * 丈量视图的宽、高。
     * 高度为用户设置的高度，
     * 宽度则为header, contentView，footer这三个子控件的高度和。
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int height = MeasureSpec.getSize(heightMeasureSpec);
        int childCount = getChildCount();
        int finalWidth = 0;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            // measure
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            // 该view所需要的总宽度
            finalWidth += child.getMeasuredWidth();
        }
        setMeasuredDimension(finalWidth, height);
    }

    /**
     * 布局函数，将header, contentView,两个view从左到右布局。
     * 布局完成后通过Scroller滚动到header的右侧，
     * 即滚动距离为header的宽度 +本视图的paddingLeft，从而达到隐藏header的效果.
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        /* headview = getChildAt(0);
           footview = getChildAt(1);
           mRecyclerView = getChildAt(2);
           布局的时候要把 mRecyclerView 放中间; */
        int top = getPaddingTop();
        int left = getPaddingLeft();
        View child0 = getChildAt(0);
        View child1 = getChildAt(1);
        View child2 = getChildAt(2);
        child0.layout(left, top, left + child0.getMeasuredWidth(), child0.getMeasuredHeight() + top);
        left += child0.getMeasuredWidth();
        child2.layout(left, top, left + child2.getMeasuredWidth(), child2.getMeasuredHeight() + top);
        left += child2.getMeasuredWidth();
        child1.layout(left, top, left + child1.getMeasuredWidth(), child1.getMeasuredHeight() + top);
        // 为mRecyclerView添加滚动监听
        mRecyclerView = (RecyclerView) child2;
        setRecyclerViewScrollListener();
        // 计算初始化滑动的x轴距离
        mInitScrollX = mRefreshHeaderView.getMeasuredWidth() + getPaddingLeft();
        // 滑动到headerView宽度的位置, 从而达到隐藏headerView的效果
        Log.d("RefreshRVP", "mInitScrollX is :" + mInitScrollX);
        // 要移动view到坐标点（100，100），那么偏移量就是(0，0)-(100，100）=（-100 ，-100）,
        // 就要执行view.scrollTo(-100,-100),达到这个效果。
        scrollTo(mInitScrollX, 0);

        // 显示或隐藏footer
        if (isRecyclerViewCompletelyShow())
            mLoadMoreFooterView.setVisibility(GONE);
        else
            mLoadMoreFooterView.setVisibility(VISIBLE);
    }


    /**
     * 为RecyclerView添加滚动监听
     */
    protected void setRecyclerViewScrollListener() {
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

    }


    /**
     * 是否已经到了最左侧
     * 如果到达最左侧，用户继续滑动则拦截事件;
     *
     * @return
     */
    protected boolean isLeft() {
        LinearLayoutManager lm = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        return lm.findFirstCompletelyVisibleItemPosition() == 0
                && getScrollX() <= mRefreshHeaderView.getMeasuredWidth();
    }

    /**
     * 是否已经到了最右侧
     * 如果到达最左侧，用户继续滑动则拦截事件;
     *
     * @return
     */
    protected boolean isRight() {
        LinearLayoutManager lm = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        return mRecyclerView != null && mRecyclerView.getAdapter() != null
                && lm.findLastCompletelyVisibleItemPosition() ==
                mRecyclerView.getAdapter().getItemCount() - 1;
    }

    /**
     * mRecyclerView 是否充满整个屏幕
     * 如果没有充满整个屏幕，禁用上拉加载更多
     *
     * @return
     */
    protected boolean isRecyclerViewCompletelyShow() {
        LinearLayoutManager lm = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        return mRecyclerView != null && mRecyclerView.getAdapter() != null
                && lm.findFirstCompletelyVisibleItemPosition() == 0
                && lm.findLastCompletelyVisibleItemPosition() == mRecyclerView.getAdapter().getItemCount() - 1;
    }

    /**
     * 与Scroller合作,实现平滑滚动。在该方法中调用Scroller的computeScrollOffset来判断滚动是否结束。
     * 如果没有结束，那么滚动到相应的位置，并且调用postInvalidate方法重绘界面，
     * 从而再次进入到这个computeScroll流程，直到滚动结束。
     * view重绘时会调用此方法
     */

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }


    /**
     * 在适当的时候拦截触摸事件，两种情况：
     * <p/>
     * 1.当mContentView滑动到最左侧，并且是右滑时拦截触摸事件，
     * 2.当mContentView滑动到最右侧，并且是左滑时拦截触摸事件，
     * 其它情况不拦截，交给其childview 来处理。
     *
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        final int action = MotionEventCompat.getActionMasked(ev);
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastX = (int) ev.getRawX();
                if (mCurrentStatus == STATUS_REFRESHING | mCurrentStatus == STATUS_LOADING)
                    return true;
                break;
            case MotionEvent.ACTION_MOVE:
                mXOffset = (int) ev.getRawX() - mLastX;
                Log.d("RefreshRVP", "mXOffset is: " + mXOffset);
                Log.d("RefreshRVP", "isRight() is: " + isRight());

                // 处理加载失败时，header和footer的隐藏问题
                if (isRefreshFailure && mXOffset < -2) {
                    mScroller.startScroll(getScrollX(), getScrollY(), mInitScrollX - getScrollX(), 0);
                    invalidate();
                    isRefreshFailure = false;
                }

                if (isLoadFailure && mXOffset >= 2) {
                    mScroller.startScroll(getScrollX(), getScrollY(), mInitScrollX - getScrollX(), 0);
                    invalidate();
                    isLoadFailure = false;
                }

                // 如果拉到了最左侧, 并且是右滑,则拦截触摸事件,从而转到onTouchEvent来处理右滑刷新事件
                if (isLeft() && mXOffset > 0) {
                    isScrollToLeft = true;
                    isScrollToRight = false;
                    // 如果RecyclerView没有完全占满屏幕，隐藏footer
                    if (isRecyclerViewCompletelyShow()) {
                        mLoadMoreFooterView.setVisibility(GONE);
                    } else {
                        mLoadMoreFooterView.setVisibility(VISIBLE);
                    }
                    return true;
                }
                // 如果拉到了最右侧, 并且是左滑,则拦截触摸事件,从而转到onTouchEvent来处理左滑加载更多事件
                if (isRight() && mXOffset < 0) {
                    isScrollToLeft = false;
                    Log.d("RefreshRVP", "isRecyclerViewCompletelyShow() is: " + isRecyclerViewCompletelyShow());
                    // 如果RecyclerView没有完全占满屏幕，隐藏footer，并禁用上拉加载功能
                    if (isRecyclerViewCompletelyShow()) {
                        mLoadMoreFooterView.setVisibility(GONE);
                        isScrollToRight = false;
                    } else {
                        mLoadMoreFooterView.setVisibility(VISIBLE);
                        isScrollToRight = true;
                    }

                    // 是否需要左滑加载功能
                    if (isCanLoadMore) {
                        // 如果RecyclerView没有完全占满屏幕，隐藏footer，并禁用上拉加载功能
                        if (isRecyclerViewCompletelyShow()) {
                            mLoadMoreFooterView.setVisibility(GONE);
                            isScrollToRight = false;
                        } else {
                            mLoadMoreFooterView.setVisibility(VISIBLE);
                            isScrollToRight = true;
                        }
                    } else {
                        mLoadMoreFooterView.setVisibility(GONE);
                        isScrollToRight = false;
                    }

                    return true;
                }
                break;
        }
        return false;
    }

    /**
     * 在这里处理触摸事件以达到右滑刷新或者左滑自动加载的问题
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:

                int currentX = (int) event.getRawX();
                mXOffset = currentX - mLastX;

                //当处于刷新状态时，不能继续下右滑或左滑
                if (mCurrentStatus == STATUS_REFRESHING && mXOffset >= 0)
                    break;
                if (mCurrentStatus == STATUS_LOADING && mXOffset <= 0)
                    break;

                changeScrollX((int) (mXOffset * mTouchScrollFrictionFactor));

                mLastX = currentX;
                break;
            case MotionEvent.ACTION_UP:

                Log.d("RefreshRVP", "mCurrentStatus is: " + mCurrentStatus);
                Log.d("RefreshRVP", "isScrollToRight is: " + isScrollToRight);

                if (isScrollToLeft && mCurrentStatus != STATUS_LOADING && mCurrentStatus != STATUS_REFRESHING)
                    doRefresh();
                if (isScrollToRight && mCurrentStatus != STATUS_REFRESHING && mCurrentStatus != STATUS_LOADING)
                    doLoadMore();

                break;
            default:
                break;
        }
        return false;
    }

    /**
     * 根据右滑刷新或左滑加载的距离改变header或footer状态
     *
     * @param distance
     * @return
     */
    protected void changeScrollX(int distance) {
        mHeaderTipsTextView.setVisibility(INVISIBLE);
        mFooterTipsTextView.setVisibility(INVISIBLE);
        // 最大值为 scrollX(header 隐藏), 最小值为0 ( header 完全显示).
        //curX是当前X的偏移量，在右滑过程中curX从最大值mInitScrollX逐渐变为0.

        // 下拉刷新过程
        if (isScrollToLeft && mCurrentStatus != STATUS_LOADING) {
            int curX = getScrollX();
            Log.d("RefreshRVP", "右滑刷新 curX is: " + curX);
            Log.d("RefreshRVP", "右滑刷新 distance is: " + distance);
            // 右滑过程边界处理
            if (distance > 0 && curX - distance > getPaddingLeft()) {
                scrollBy(-distance, 0);
            } else if (distance < 0 && curX - distance <= mInitScrollX) {
                // 左滑过程边界处理
                scrollBy(-distance, 0);
            }
            curX = getScrollX();
            int slop = mInitScrollX / 2;
            // curX是当前X的偏移量，在右滑过程中curX从最大值mInitScrollX逐渐变为0.
            if (curX > 0 && curX < slop) {
                mCurrentStatus = STATUS_RELEASE_TO_REFRESH;
            } else if (curX > 0 && curX > slop) {
                mCurrentStatus = STATUS_PULL_TO_REFRESH;
            }
        }

        // 左滑加载过程
        if (isScrollToRight && mCurrentStatus != STATUS_REFRESHING) {
            int curX = getScrollX() - mHeaderWidth;
//            LogUtil.d("RefreshRVP", "左滑加载 curX is: " + curX);
//            LogUtil.d("RefreshRVP", "左滑加载 distance is: " + distance);
            // 右滑过程边界处理
            if (distance > 0 && curX - distance > 0) {
                scrollBy(-distance, 0);
            } else if (distance < 0 && curX - distance <= mFooterWidth) {
                // 左滑过程边界处理
                scrollBy(-distance, 0);
            }
            curX = getScrollX() - mHeaderWidth;
            int slop = mInitScrollX / 2;

            if (curX > 0 && curX < slop) {
                mCurrentStatus = STATUS_PULL_TO_LOAD;
            } else if (curX > 0 && curX > slop) {
                mCurrentStatus = STATUS_RELEASE_TO_LOAD;
            }
        }
    }

    /**
     * 执行右滑刷新
     */
    protected void doRefresh() {
        changeHeaderViewStaus();
        // 执行刷新操作
        if (mCurrentStatus == STATUS_REFRESHING && mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }

    /**
     * 执行左滑(自动)加载更多的操作
     */
    protected void doLoadMore() {
        changeFooterViewStatus();
        if (mCurrentStatus == STATUS_LOADING && mLoadMoreListener != null) {
            mLoadMoreListener.onLoadMore();
        }
    }

    /**
     * 右滑刷新
     * 手指抬起时,根据用户右滑的宽度来判断是否是有效的右滑刷新操作。如果右滑的距离超过headerview宽度的
     * 1/2那么则认为是有效的右滑刷新操作，否则恢复原来的视图状态.
     */
    private void changeHeaderViewStaus() {
        int curScrollX = getScrollX();
        // 超过1/2则认为是有效的下拉刷新, 否则还原
        if (curScrollX <= mInitScrollX / 2) {
            mScroller.startScroll(curScrollX, getScrollY(), mRefreshHeaderView.getPaddingLeft() - curScrollX, 0);
            mCurrentStatus = STATUS_REFRESHING;
            mHeaderProgressBar.setVisibility(View.VISIBLE);
        } else {
            mScroller.startScroll(curScrollX, getScrollY(), mInitScrollX - curScrollX, 0);
            mCurrentStatus = STATUS_IDLE;
        }
        invalidate();
    }

    /**
     * 左滑加载
     * 手指抬起时,根据用户左滑的宽度来判断是否是有效的左滑刷新操作。如果左滑的距离超过footerview宽度的
     * 1/2那么则认为是有效的左滑加载操作，否则恢复原来的视图状态.
     */
    private void changeFooterViewStatus() {
        int curScrollX = getScrollX();
        // 超过1/2则认为是有效的下拉刷新, 否则还原
        if (curScrollX >= mHeaderWidth + mFooterWidth / 2) {
            mScroller.startScroll(curScrollX, getScrollY(),
                    mHeaderWidth + mLoadMoreFooterView.getPaddingRight() - curScrollX, 0);
            mCurrentStatus = STATUS_LOADING;
            mFooterProgressBar.setVisibility(View.VISIBLE);
        } else {
            mScroller.startScroll(curScrollX, getScrollY(), mHeaderWidth - curScrollX, 0);
            mCurrentStatus = STATUS_IDLE;
            mFooterProgressBar.setVisibility(View.GONE);
        }
        invalidate();
    }

    /**
     * 刷新结束，恢复状态
     */
    public void refreshComplete() {
        mScroller.startScroll(getScrollX(), getScrollY(), mInitScrollX - getScrollX(), 0);
        mCurrentStatus = STATUS_IDLE;
        invalidate();
        isRefreshFailure = false;
        mHeaderProgressBar.setVisibility(View.GONE);
        mHeaderTipsTextView.setVisibility(INVISIBLE);
        mRefreshHeaderView.setOnClickListener(null);
    }

    /**
     * 加载结束，恢复状态
     */
    public void loadMoreCompelte() {
        mScroller.startScroll(getScrollX(), getScrollY(), mInitScrollX - getScrollX(), 0);
        mCurrentStatus = STATUS_IDLE;
        invalidate();
        isLoadFailure = false;
        mFooterProgressBar.setVisibility(View.GONE);
        mFooterTipsTextView.setVisibility(INVISIBLE);
        mLoadMoreFooterView.setOnClickListener(null);
    }

    /**
     * 右滑刷新或左滑加载没有更多数据
     */
    public void refreshAndLoadNoMore() {

        if (mCurrentStatus == STATUS_REFRESHING) {
            mHeaderProgressBar.setVisibility(INVISIBLE);
            mHeaderTipsTextView.setVisibility(VISIBLE);
            mHeaderTipsTextView.setBackgroundResource(R.drawable.no_more);
            isRefreshFailure = false;
            mRefreshHeaderView.setOnClickListener(null);

        } else if (mCurrentStatus == STATUS_LOADING) {
            mFooterProgressBar.setVisibility(View.INVISIBLE);
            mFooterTipsTextView.setVisibility(VISIBLE);
            mFooterTipsTextView.setBackgroundResource(R.drawable.no_more);
            isLoadFailure = false;
            mLoadMoreFooterView.setOnClickListener(null);
        }

        postDelayed(new Runnable() {
            @Override
            public void run() {
                // 隐藏header
                mScroller.startScroll(getScrollX(), getScrollY(), mInitScrollX - getScrollX(), 0);
                invalidate();
                mCurrentStatus = STATUS_IDLE;
            }
        }, 1000);

    }

    /**
     * 下拉刷新上拉加载失败
     */
    public void refreshAndLoadFailure() {

        if (mCurrentStatus == STATUS_REFRESHING) {

            mHeaderTipsTextView.setVisibility(VISIBLE);
            mHeaderTipsTextView.setBackgroundResource(R.drawable.load_failure);
            mHeaderProgressBar.setVisibility(INVISIBLE);
            isRefreshFailure = true;

        } else if (mCurrentStatus == STATUS_LOADING) {

            mFooterTipsTextView.setVisibility(VISIBLE);
            mFooterTipsTextView.setBackgroundResource(R.drawable.load_failure);
            mFooterProgressBar.setVisibility(View.INVISIBLE);
            isLoadFailure = true;
        }

        mCurrentStatus = STATUS_IDLE;

        mRefreshHeaderView.setOnClickListener(loadFailureLisenter);
        mLoadMoreFooterView.setOnClickListener(loadFailureLisenter);
    }

    /**
     * 加载失败时点击重新加载
     */
    private OnClickListener loadFailureLisenter = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int i = v.getId();
            if (i == R.id.rl_header) {
                refreshing();

            } else if (i == R.id.rl_footer) {
                loading();

            }
        }
    };

    /**
     * 手动设置刷新
     */
    public void refreshing() {

        scrollTo(mInitScrollX / 2, 0);
        mCurrentStatus = STATUS_REFRESHING;
        mHeaderProgressBar.setVisibility(View.VISIBLE);
        mHeaderTipsTextView.setVisibility(INVISIBLE);

        doRefresh();
    }

    /**
     * 手动上拉加载
     */
    private void loading() {

        scrollTo(mInitScrollX + mFooterWidth / 2, 0);
        mCurrentStatus = STATUS_LOADING;
        mFooterProgressBar.setVisibility(View.VISIBLE);
        mFooterTipsTextView.setVisibility(INVISIBLE);

        doLoadMore();
    }

    /**
     * 当前是否处于加载状态
     *
     * @return
     */
    public boolean isLoading() {
        return mCurrentStatus == STATUS_LOADING;
    }

    /**
     * 当前是否处于刷新状态
     *
     * @return
     */
    public boolean isRefreshing() {
        return mCurrentStatus == STATUS_REFRESHING;
    }


    /**
     * 设置是否需要下拉加载功能
     *
     * @param canLoad
     */
    public void setCanLoad(boolean canLoad) {
        this.isCanLoadMore = canLoad;
    }

    public void setCanRefresh(boolean canRefresh) {
        this.isCanRefresh = canRefresh;
    }


    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }


    /**
     * 设置右滑刷新监听器
     *
     * @param listener
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mOnRefreshListener = listener;
    }

    /**
     * 设置左滑加载更多的监听器
     *
     * @param listener
     */
    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        mLoadMoreListener = listener;
    }

    public interface OnRefreshListener {
        void onRefresh();
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

}
