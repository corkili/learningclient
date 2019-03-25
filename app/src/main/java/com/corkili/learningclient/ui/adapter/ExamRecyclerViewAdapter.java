package com.corkili.learningclient.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.generate.protobuf.Info.ExamSimpleInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;

import java.util.Date;
import java.util.List;

public class ExamRecyclerViewAdapter extends RecyclerView.Adapter<ExamRecyclerViewAdapter.ViewHolder> {

    private final Context context;
    private final List<ExamSimpleInfo> examSimpleInfos;
    private final OnItemInteractionListener mListener;
    private final UserInfo userInfo;

    public ExamRecyclerViewAdapter(Context context, List<ExamSimpleInfo> examSimpleInfos,
                                   OnItemInteractionListener mListener, UserInfo userInfo) {
        this.context = context;
        this.examSimpleInfos = examSimpleInfos;
        this.mListener = mListener;
        this.userInfo = userInfo;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_exam_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.mItem = examSimpleInfos.get(position);
        holder.indexView.setText(String.valueOf(position + 1));
        if (userInfo.getUserType() == UserType.Teacher) {
            if (holder.mItem.getStartTime() <= System.currentTimeMillis()) {
                if (holder.mItem.getEndTime() <= System.currentTimeMillis()) {
                    holder.submitView.setText("已关闭提交(点击查看)");
                } else {
                    holder.submitView.setText("已开放提交(点击查看)");
                }
            } else {
                holder.submitView.setText("未开放提交");
            }
        } else {
            if (holder.mItem.getStartTime() <= System.currentTimeMillis()) {
                if (holder.mItem.getEndTime() <= System.currentTimeMillis()) {
                    holder.submitView.setText("已关闭提交");
                } else {
                    holder.submitView.setText("已开放提交");
                }
            } else {
                holder.mView.setVisibility(View.GONE);
            }
        }
        holder.examNameView.setText(holder.mItem.getExamName());
        holder.startTimeView.setText(IUtils.format("开始时间：{}", IUtils.DATE_TIME_FORMATTER
                .format(new Date(holder.mItem.getStartTime()))));
        holder.endTimeView.setText(IUtils.format("结束时间：{}", IUtils.DATE_TIME_FORMATTER
                .format(new Date(holder.mItem.getEndTime()))));

        holder.mView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(holder);
            }
        });

        holder.submitView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onSubmitViewClick(holder);
            }
        });

    }

    @Override
    public int getItemCount() {
        return examSimpleInfos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final View mView;
        private final TextView indexView;
        private final TextView submitView;
        private final TextView examNameView;
        private final TextView startTimeView;
        private final TextView endTimeView;
        private ExamSimpleInfo mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            indexView = view.findViewById(R.id.item_index);
            submitView = view.findViewById(R.id.item_submit);
            examNameView = view.findViewById(R.id.item_exam_name);
            startTimeView = view.findViewById(R.id.item_start_time);
            endTimeView = view.findViewById(R.id.item_end_time);
        }

        public ExamSimpleInfo getExamSimpleInfo() {
            return mItem;
        }
    }

    public interface OnItemInteractionListener {

        void onItemClick(ViewHolder viewHolder);

        void onSubmitViewClick(ViewHolder viewHolder);

    }
}

