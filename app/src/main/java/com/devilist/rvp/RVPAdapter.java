package com.devilist.rvp;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zengpu on 2016/10/28.
 */

public class RVPAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<AppInfo> appInfolist = new ArrayList<>();
    private OnItemClickListener mOnItemClickListener;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private CardView rootCv;
        private ImageView appIconIv;
        private TextView appNameTv;
        private TextView appVCodeTv;
        private TextView appVNameTv;
        private TextView appPnameTv;
        private OnItemClickListener mListener;

        public ViewHolder(View itemView, OnItemClickListener listener) {
            super(itemView);
            mListener = listener;

            rootCv = (CardView) itemView.findViewById(R.id.cv_view);
            appIconIv = (ImageView) itemView.findViewById(R.id.iv_app_icon);
            appNameTv = (TextView) itemView.findViewById(R.id.tv_app_name);
            appVNameTv = (TextView) itemView.findViewById(R.id.tv_app_version_name);
            appPnameTv = (TextView) itemView.findViewById(R.id.tv_app_package_name);

            rootCv.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mListener.onItemClick(v, getLayoutPosition());
        }
    }

    public RVPAdapter(Context context, List<AppInfo> appInfolist) {
        this.context = context;
        this.appInfolist = appInfolist;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.rvp_activity_item, parent, false);
        return new ViewHolder(v, mOnItemClickListener);
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        AppInfo appInfo = appInfolist.get(position);
        ViewHolder mViewHolder = (ViewHolder) holder;
        mViewHolder.appNameTv.setText(appInfo.getAppName());
        mViewHolder.appIconIv.setImageDrawable(appInfo.getAppIcon());
        mViewHolder.appVNameTv.setText("v" + appInfo.getVersionName());
        mViewHolder.appPnameTv.setText(appInfo.getPackageName());
    }

    @Override
    public int getItemCount() {
        return appInfolist.size();
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }
}

