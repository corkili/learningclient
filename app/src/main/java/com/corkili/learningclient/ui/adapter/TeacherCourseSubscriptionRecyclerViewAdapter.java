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
import com.corkili.learningclient.generate.protobuf.Info.CourseSubscriptionInfo;

import java.util.Date;
import java.util.List;

public class TeacherCourseSubscriptionRecyclerViewAdapter extends RecyclerView.Adapter<TeacherCourseSubscriptionRecyclerViewAdapter.ViewHolder> {

    private final Context context;
    private final List<CourseSubscriptionInfo> courseSubscriptionInfos;
    private final OnItemInteractionListener mListener;

    public TeacherCourseSubscriptionRecyclerViewAdapter(Context context,
                                                        List<CourseSubscriptionInfo> courseSubscriptionInfos,
                                                        OnItemInteractionListener mListener) {
        this.context = context;
        this.courseSubscriptionInfos = courseSubscriptionInfos;
        this.mListener = mListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_teacher_course_subscription_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.mItem = courseSubscriptionInfos.get(position);
        holder.indexView.setText(String.valueOf(position + 1));
        holder.usernameView.setText(IUtils.format("{}(点击联系)",
                holder.mItem.getSubscriberInfo().getUsername()));
        holder.subscribeTimeView.setText(IUtils.format("订阅时间：{}",
                IUtils.DATE_TIME_FORMATTER.format(new Date(holder.mItem.getCreateTime()))));

        holder.mView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(holder);
            }
        });

    }

    @Override
    public int getItemCount() {
        return courseSubscriptionInfos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView indexView;
        final TextView usernameView;
        final TextView subscribeTimeView;
        CourseSubscriptionInfo mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            indexView = view.findViewById(R.id.item_index);
            usernameView = view.findViewById(R.id.item_user_name);
            subscribeTimeView = view.findViewById(R.id.item_subscribe_time);
        }

        public CourseSubscriptionInfo getCourseSubscriptionInfo() {
            return mItem;
        }

    }

    public interface OnItemInteractionListener {

        void onItemClick(ViewHolder viewHolder);

    }
}
