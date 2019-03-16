package com.corkili.learningclient.ui.activity;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.generate.protobuf.Info.ForumTopicInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;

import java.util.Date;
import java.util.List;

public class ForumRecyclerViewAdapter extends RecyclerView.Adapter<ForumRecyclerViewAdapter.ViewHolder> {

    private final Context context;
    private final List<ForumTopicInfo> forumTopicInfos;
    private final OnItemInteractionListener mListener;

    public ForumRecyclerViewAdapter(Context context, List<ForumTopicInfo> forumTopicInfos, OnItemInteractionListener mListener) {
        this.context = context;
        this.forumTopicInfos = forumTopicInfos;
        this.mListener = mListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_forum_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.mItem = forumTopicInfos.get(position);
        holder.titleView.setText(holder.mItem.getTitle());
        if (holder.mItem.getAuthorInfo().getUserType() == UserType.Teacher) {
            holder.usernameView.setText(IUtils.format("[老师]{}", holder.mItem.getAuthorInfo().getUsername()));
        } else {
            holder.usernameView.setText(holder.mItem.getAuthorInfo().getUsername());
        }
        holder.descriptionView.setText(holder.mItem.getDescription());
        holder.timeView.setText(IUtils.format("{}",
                IUtils.DATE_TIME_FORMATTER.format(new Date(holder.mItem.getUpdateTime()))));

        holder.mView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(holder);
            }
        });

        holder.mView.setOnLongClickListener(v -> {
            if (mListener != null) {
                return mListener.onItemLongClick(holder);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return forumTopicInfos.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final View mView;
        private final TextView titleView;
        private final TextView usernameView;
        private final TextView descriptionView;
        private final TextView timeView;
        private ForumTopicInfo mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            titleView = view.findViewById(R.id.item_title);
            usernameView = view.findViewById(R.id.item_user_name);
            descriptionView = view.findViewById(R.id.item_description);
            timeView = view.findViewById(R.id.item_time);
        }

        public ForumTopicInfo getForumTopicInfo() {
            return mItem;
        }
    }

    public interface OnItemInteractionListener {

        void onItemClick(ViewHolder viewHolder);

        boolean onItemLongClick(ViewHolder viewHolder);

    }
}

