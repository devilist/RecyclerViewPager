package com.devilist.rvp;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.devilist.recyclerviewpager.AnimPagerIndicator;
import com.devilist.recyclerviewpager.RecyclerViewPager;
import com.devilist.recyclerviewpager.RefreshRecyclerViewPager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        RecyclerViewPager.OnPageSelectListener,
        RefreshRecyclerViewPager.OnRefreshListener,
        RefreshRecyclerViewPager.OnLoadMoreListener {

    private RefreshRecyclerViewPager refreshRecyclerViewPager;
    private RecyclerViewPager recyclerViewPager;
    private RVPAdapter adapter;
    private List<AppInfo> appInfolist = new ArrayList<>();

    private AnimPagerIndicator animPagerIndicator;
    private List<Drawable> indicatorIconlist = new ArrayList<>();

    private List<String> bgColorList = new ArrayList<>();

    private RelativeLayout rootRl;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initData();
        initView();
    }

    private void initData() {

        List<PackageInfo> packages = getPackageManager().getInstalledPackages(0);

        for (int i = 0; i < packages.size(); i++) {

            PackageInfo packageInfo = packages.get(i);
            String appName = packageInfo.applicationInfo.loadLabel(getPackageManager()).toString();
            Drawable appIcon = packageInfo.applicationInfo.loadIcon(getPackageManager());
            int versionCode = packageInfo.versionCode;
            String versionName = packageInfo.versionName;
            String packageName = packageInfo.packageName;

            AppInfo appInfo = new AppInfo();
            appInfo.setAppName(appName);
            appInfo.setAppIcon(appIcon);
            appInfo.setVersionCode(versionCode);
            appInfo.setVersionName(versionName);
            appInfo.setPackageName(packageName);

            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                appInfolist.add(appInfo);
                indicatorIconlist.add(appIcon);
            }
        }

        bgColorList.add("#448aff");
        bgColorList.add("#00bcd4");
        bgColorList.add("#009688");
        bgColorList.add("#4caf50");
        bgColorList.add("#8bc34a");
        bgColorList.add("#cddc39");
        bgColorList.add("#ffeb3b");
        bgColorList.add("#ff9800");
        bgColorList.add("#ff5722");
        bgColorList.add("#9e9e9e");

    }

    private void initView() {

        rootRl = (RelativeLayout) findViewById(R.id.rl_root);

        refreshRecyclerViewPager = (RefreshRecyclerViewPager) findViewById(R.id.rrvp_pager);

        recyclerViewPager = (RecyclerViewPager) findViewById(R.id.rvp_list);
        adapter = new RVPAdapter(this, appInfolist);
        recyclerViewPager.setAdapter(adapter);
        adapter.setOnItemClickListener(new RVPAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Toast.makeText(MainActivity.this, appInfolist.get(position).getAppName(), Toast.LENGTH_SHORT).show();
            }
        });

        recyclerViewPager.setOnPageSelectListener(this);

        refreshRecyclerViewPager.setOnRefreshListener(this);
        refreshRecyclerViewPager.setOnLoadMoreListener(this);

        animPagerIndicator = (AnimPagerIndicator) findViewById(R.id.view_indictor);
        animPagerIndicator.setData(indicatorIconlist);
        animPagerIndicator.setRecyclerViewPager(recyclerViewPager);

    }

    int textFlag = 0;

    @Override
    public void onRefresh() {

        animPagerIndicator.setTouchEnable(false);

        refreshRecyclerViewPager.postDelayed(new Runnable() {

            @Override
            public void run() {

                animPagerIndicator.setTouchEnable(true);

                // 加载失败
                if (textFlag == 0) {
                    refreshRecyclerViewPager.refreshAndLoadFailure();
                    textFlag = 1;
                    return;
                }
                // 加载成功
                if (textFlag == 1) {
                    // 更新数据
                    adapter = new RVPAdapter(MainActivity.this, appInfolist);
                    recyclerViewPager.setAdapter(adapter);
                    refreshRecyclerViewPager.refreshComplete();
                    animPagerIndicator.setData(indicatorIconlist);

                    textFlag = 2;
                    return;
                }
                // 没有更多数据
                if (textFlag == 2) {
                    refreshRecyclerViewPager.refreshAndLoadNoMore();
                    textFlag = 0;
                    return;
                }
            }
        }, 1500);

    }

    @Override
    public void onLoadMore() {

        animPagerIndicator.setTouchEnable(false);

        refreshRecyclerViewPager.postDelayed(new Runnable() {

            @Override
            public void run() {
                animPagerIndicator.setTouchEnable(true);
                if (textFlag == 0) {
                    refreshRecyclerViewPager.refreshAndLoadFailure();
                    textFlag = 1;
                    return;
                }
                if (textFlag == 1) {
                    // 更新数据
                    appInfolist.add(appInfolist.get(0));
                    appInfolist.add(appInfolist.get(1));
                    appInfolist.add(appInfolist.get(2));
                    appInfolist.add(appInfolist.get(3));
                    appInfolist.add(appInfolist.get(4));
                    adapter.notifyDataSetChanged();
                    // 更新完后调用该方法结束刷新
                    refreshRecyclerViewPager.loadMoreCompelte();
                    List<Drawable> list = new ArrayList<Drawable>();
                    list.add(indicatorIconlist.get(0));
                    list.add(indicatorIconlist.get(1));
                    list.add(indicatorIconlist.get(2));
                    list.add(indicatorIconlist.get(3));
                    list.add(indicatorIconlist.get(4));
                    animPagerIndicator.addData(list);
                    textFlag = 2;
                    return;
                }
                if (textFlag == 2) {
                    refreshRecyclerViewPager.refreshAndLoadNoMore();
                    textFlag = 0;
                    return;
                }
            }
        }, 1500);

    }


    @Override
    public void onPageScrolled(int position, float positionOffset) {

    }

    @Override
    public void onPageSelected(int position) {
        Log.e("MainActivity", "position : " + position);
        rootRl.setBackgroundColor(Color.parseColor(bgColorList.get(position % 10)));

    }

    @Override
    public void onPageScrollStateChanged(int state) {

        if (state == RecyclerViewPager.SCROLL_STATE_IDLE)
            animPagerIndicator.setTouchEnable(true);
        else
            animPagerIndicator.setTouchEnable(false);

    }
}

