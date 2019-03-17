package com.corkili.learningclient.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.generate.protobuf.Info.CourseCommentInfo;

import java.util.Date;
import java.util.List;

public class CourseCommentRecyclerViewAdapter extends RecyclerView.Adapter<CourseCommentRecyclerViewAdapter.ViewHolder> {

    private final Context context;
    private final List<CourseCommentInfo> courseCommentInfos;

    public CourseCommentRecyclerViewAdapter(Context context, List<CourseCommentInfo> courseCommentInfos) {
        this.context = context;
        this.courseCommentInfos = courseCommentInfos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_course_comment_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.mItem = courseCommentInfos.get(position);
        holder.usernameView.setText(holder.mItem.getCommentAuthorInfo().getUsername());
        holder.ratingView.setRating(holder.mItem.getCommentType().getNumber() + 1);
        holder.contentView.setText(holder.mItem.getContent());
        holder.commentTimeView.setText(IUtils.format("评论时间：{}",
                IUtils.DATE_TIME_FORMATTER.format(new Date(holder.mItem.getCreateTime()))));
    }

    @Override
    public int getItemCount() {
        return courseCommentInfos.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView usernameView;
        final RatingBar ratingView;
        final TextView contentView;
        final TextView commentTimeView;
        CourseCommentInfo mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            usernameView = view.findViewById(R.id.item_user_name);
            ratingView = view.findViewById(R.id.item_comment_type);
            contentView = view.findViewById(R.id.item_content);
            commentTimeView = view.findViewById(R.id.item_comment_time);
        }
    }
}

