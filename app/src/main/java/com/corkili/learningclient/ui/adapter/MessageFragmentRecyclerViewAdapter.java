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
import com.corkili.learningclient.generate.protobuf.Info.MessageInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class MessageFragmentRecyclerViewAdapter extends RecyclerView.Adapter<MessageFragmentRecyclerViewAdapter.ViewHolder> {

    private Context context;
    private final Map<Long, List<MessageInfo>> messageInfos;
    private final List<UserInfo> userInfos;
    private OnItemInteractionListener mListener;

    public MessageFragmentRecyclerViewAdapter(Context context, Map<Long, List<MessageInfo>> messageInfos,
                                              List<UserInfo> userInfos, OnItemInteractionListener mListener) {
        this.context = context;
        this.messageInfos = messageInfos;
        this.userInfos = userInfos;
        this.mListener = mListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_message_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.mItem = userInfos.get(position);
        if (!messageInfos.containsKey(holder.mItem.getUserId())) {
            messageInfos.put(holder.mItem.getUserId(), new ArrayList<>());
        }
        holder.messageInfoList = messageInfos.get(holder.mItem.getUserId());
        Collections.sort(holder.messageInfoList, (o1, o2) -> {
            if (o1.getCreateTime() == o2.getCreateTime()) {
                return 0;
            } else {
                return o1.getCreateTime() < o2.getCreateTime() ? -1 : 1;
            }
        });
        holder.usernameView.setText(holder.mItem.getUsername());
        holder.messageNumberView.setText(String.valueOf(holder.messageInfoList.size()));
        MessageInfo lastMessageInfo = holder.messageInfoList.get(holder.messageInfoList.size() - 1);
        if (lastMessageInfo.hasImage()) {
            holder.lastMessageView.setText("[图片]");
        } else {
            holder.lastMessageView.setText(lastMessageInfo.getText());
        }
        holder.lastMessageTimeView.setText(IUtils.format("创建时间：{}",
                IUtils.DATE_TIME_FORMATTER.format(new Date(lastMessageInfo.getCreateTime()))));

        holder.mView.setOnClickListener(v -> {
            if (this.mListener != null) {
                this.mListener.onItemClick(holder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userInfos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final View mView;
        private final TextView usernameView;
        private final TextView messageNumberView;
        private final TextView lastMessageView;
        private final TextView lastMessageTimeView;
        private UserInfo mItem;
        private List<MessageInfo> messageInfoList;

        ViewHolder(View view) {
            super(view);
            mView = view;
            usernameView = view.findViewById(R.id.item_username);
            messageNumberView = view.findViewById(R.id.item_message_number);
            lastMessageView = view.findViewById(R.id.item_last_message);
            lastMessageTimeView = view.findViewById(R.id.item_last_message_time);
        }

        public UserInfo getUserInfo() {
            return mItem;
        }

        public List<MessageInfo> getMessageInfoList() {
            return Collections.unmodifiableList(messageInfoList);
        }
    }

    public interface OnItemInteractionListener {

        void onItemClick(ViewHolder viewHolder);

    }
}
