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
import com.corkili.learningclient.generate.protobuf.Info.TopicReplyInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;

import java.util.Date;
import java.util.List;

public class ForumTopicReplyRecyclerViewAdapter extends RecyclerView.Adapter<ForumTopicReplyRecyclerViewAdapter.ViewHolder> {

    private final Context context;
    private final List<TopicReplyInfo> topicReplyInfos;
    private final OnItemInteractionListener mListener;

    public ForumTopicReplyRecyclerViewAdapter(Context context, List<TopicReplyInfo> topicReplyInfos, OnItemInteractionListener mListener) {
        this.context = context;
        this.topicReplyInfos = topicReplyInfos;
        this.mListener = mListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.comment_reply_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.mItem = topicReplyInfos.get(position);
        holder.indexView.setText(IUtils.format("{}层", position + 1));
        if (holder.mItem.getAuthorInfo().getUserType() == UserType.Teacher) {
            holder.usernameView.setText(IUtils.format("[老师]{}", holder.mItem.getAuthorInfo().getUsername()));
        } else {
            holder.usernameView.setText(holder.mItem.getAuthorInfo().getUsername());
        }
        holder.contentView.setText(holder.mItem.getContent());
        holder.timeView.setText(IUtils.format("{}",
                IUtils.DATE_TIME_FORMATTER.format(new Date(holder.mItem.getUpdateTime()))));

        holder.mView.setOnLongClickListener(v -> {
            if (mListener != null) {
                return mListener.onItemLongClick(holder);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return topicReplyInfos.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final View mView;
        private final TextView indexView;
        private final TextView usernameView;
        private final TextView contentView;
        private final TextView timeView;
        private TopicReplyInfo mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            indexView = view.findViewById(R.id.reply_index);
            usernameView = view.findViewById(R.id.reply_user_name);
            contentView = view.findViewById(R.id.reply_content);
            timeView = view.findViewById(R.id.reply_time);
        }

        public TopicReplyInfo getTopicReplyInfo() {
            return mItem;
        }
    }

    public interface OnItemInteractionListener {

        boolean onItemLongClick(ViewHolder viewHolder);

    }
}

