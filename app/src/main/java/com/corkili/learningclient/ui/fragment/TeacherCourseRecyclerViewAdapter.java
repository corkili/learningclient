package com.corkili.learningclient.ui.fragment;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.corkili.learningclient.R;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;

import java.util.List;


public class TeacherCourseRecyclerViewAdapter extends RecyclerView.Adapter<TeacherCourseRecyclerViewAdapter.ViewHolder> {

    private final Context context;
    private final List<CourseInfo> courseInfos;

    public TeacherCourseRecyclerViewAdapter(Context context, List<CourseInfo> courseInfos) {
        this.context = context;
        this.courseInfos = courseInfos;
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
        holder.mIdView.setText(position);
        holder.mContentView.setText(courseInfos.get(position).getCourseName());

        holder.mView.setOnClickListener(v -> {
            // TODO 跳转至课程管理界面 Intent(context, Activity.class)
        });
    }

    @Override
    public int getItemCount() {
        return courseInfos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        public final TextView mContentView;
        public CourseInfo mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = view.findViewById(R.id.item_number);
            mContentView = view.findViewById(R.id.content);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
