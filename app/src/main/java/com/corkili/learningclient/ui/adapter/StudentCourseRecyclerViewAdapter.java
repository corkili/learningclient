package com.corkili.learningclient.ui.adapter;

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

import java.util.List;


public class StudentCourseRecyclerViewAdapter extends RecyclerView.Adapter<StudentCourseRecyclerViewAdapter.ViewHolder> {

    private Context context;
    private final List<CourseInfo> courseInfos;
    private OnItemInteractionListener mListener;

    public StudentCourseRecyclerViewAdapter(Context context, List<CourseInfo> courseInfos,
                                            OnItemInteractionListener mListener) {
        this.context = context;
        this.courseInfos = courseInfos;
        this.mListener = mListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_student_course_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.mItem = courseInfos.get(position);
        holder.indexView.setText(String.valueOf(position + 1));
        holder.courseNameView.setText(holder.mItem.getCourseName());
        if (holder.mItem.getOpen()) {
            holder.openView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_yes_bk_green));
        } else {
            holder.openView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_no_bk_red));
        }
        holder.tagsView.setText(IUtils.list2String(holder.mItem.getTagList(), ";"));
        holder.descriptionView.setText(holder.mItem.getDescription());
        holder.teacherView.setText(IUtils.format("[老师]{}", holder.mItem.getTeacherInfo().getUsername()));
        holder.mView.setOnClickListener(v -> {
            if (this.mListener != null) {
                this.mListener.onItemClick(holder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return courseInfos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final View mView;
        private final TextView indexView;
        private final TextView courseNameView;
        private final ImageView openView;
        private final TextView tagsView;
        private final TextView descriptionView;
        private final TextView teacherView;
        private CourseInfo mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            indexView = view.findViewById(R.id.item_index);
            courseNameView = view.findViewById(R.id.item_course_name);
            openView = view.findViewById(R.id.item_open);
            tagsView = view.findViewById(R.id.item_tags);
            descriptionView = view.findViewById(R.id.item_description);
            teacherView = view.findViewById(R.id.item_teacher);
        }

        public CourseInfo getCourseInfo() {
            return mItem;
        }
    }

    public interface OnItemInteractionListener {

        void onItemClick(ViewHolder viewHolder);

    }
}
