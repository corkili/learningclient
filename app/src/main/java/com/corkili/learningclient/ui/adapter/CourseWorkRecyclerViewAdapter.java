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
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkSimpleInfo;

import java.util.Date;
import java.util.List;

public class CourseWorkRecyclerViewAdapter extends RecyclerView.Adapter<CourseWorkRecyclerViewAdapter.ViewHolder> {

    private final Context context;
    private final List<CourseWorkSimpleInfo> courseWorkSimpleInfos;
    private final OnItemInteractionListener mListener;

    public CourseWorkRecyclerViewAdapter(Context context, List<CourseWorkSimpleInfo> courseWorkSimpleInfos, OnItemInteractionListener mListener) {
        this.context = context;
        this.courseWorkSimpleInfos = courseWorkSimpleInfos;
        this.mListener = mListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_course_work_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.mItem = courseWorkSimpleInfos.get(position);
        holder.indexView.setText(String.valueOf(position + 1));
        holder.submitView.setText(holder.mItem.getOpen() ? IUtils.format("已开放提交(点击查看)") : "未开放提交");
        holder.courseWorkNameView.setText(holder.mItem.getCourseWorkName());
        holder.deadlineView.setText(holder.mItem.getHasDeadline() ? IUtils.format("截止日期：{}",
                IUtils.DATE_FORMATTER.format(new Date(holder.mItem.getDeadline()))) : "截止日期：无限期");

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
        return courseWorkSimpleInfos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final View mView;
        private final TextView indexView;
        private final TextView submitView;
        private final TextView courseWorkNameView;
        private final TextView deadlineView;
        private CourseWorkSimpleInfo mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            indexView = view.findViewById(R.id.item_index);
            submitView = view.findViewById(R.id.item_submit);
            courseWorkNameView = view.findViewById(R.id.item_course_work_name);
            deadlineView = view.findViewById(R.id.item_deadline);
        }

        public CourseWorkSimpleInfo getCourseWorkSimpleInfo() {
            return mItem;
        }
    }

    public interface OnItemInteractionListener {

        void onItemClick(ViewHolder viewHolder);

        void onSubmitViewClick(ViewHolder viewHolder);

    }
}

