package com.corkili.learningclient.ui.fragment;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.ui.fragment.TeacherCourseFragment.OnRecycleItemClickListener;

import java.util.Date;
import java.util.List;


public class TeacherCourseRecyclerViewAdapter extends RecyclerView.Adapter<TeacherCourseRecyclerViewAdapter.ViewHolder> {

    private Context context;
    private final List<CourseInfo> courseInfos;
    private OnRecycleItemClickListener onRecycleItemClickListener;

    public TeacherCourseRecyclerViewAdapter(Context context, List<CourseInfo> courseInfos,
                                            OnRecycleItemClickListener onRecycleItemClickListener) {
        this.context = context;
        this.courseInfos = courseInfos;
        this.onRecycleItemClickListener = onRecycleItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_teacher_course_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.mItem = courseInfos.get(position);
        holder.indexView.setText(String.valueOf(position + 1));
        if (holder.mItem.getOpen()) {
            holder.openView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_yes_bk_green));
        } else {
            holder.openView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_no_bk_red));
        }
        holder.courseNameView.setText(holder.mItem.getCourseName());
        holder.descriptionView.setText(holder.mItem.getDescription());
        holder.createTimeView.setText(IUtils.format("创建时间：{}",
                IUtils.DATE_TIME_FORMATTER.format(new Date(holder.mItem.getCreateTime()))));
        holder.updateTimeView.setText(IUtils.format("更新时间：{}",
                IUtils.DATE_TIME_FORMATTER.format(new Date(holder.mItem.getUpdateTime()))));
        holder.mView.setOnClickListener(v -> {
            if (this.onRecycleItemClickListener != null) {
                this.onRecycleItemClickListener.onRecycleItemClick(holder.mItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return courseInfos.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView indexView;
        final ImageView openView;
        final TextView courseNameView;
        final TextView descriptionView;
        final TextView createTimeView;
        final TextView updateTimeView;
        CourseInfo mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            indexView = view.findViewById(R.id.item_index);
            openView = view.findViewById(R.id.item_open);
            courseNameView = view.findViewById(R.id.item_course_name);
            descriptionView = view.findViewById(R.id.item_description);
            createTimeView = view.findViewById(R.id.item_create_time);
            updateTimeView = view.findViewById(R.id.item_update_time);
        }
    }
}
