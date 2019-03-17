package com.corkili.learningclient.ui.adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.generate.protobuf.Info.TopicCommentInfo;
import com.corkili.learningclient.generate.protobuf.Info.TopicReplyInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.TopicReplyCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.TopicReplyDeleteResponse;
import com.corkili.learningclient.generate.protobuf.Response.TopicReplyFindAllResponse;
import com.corkili.learningclient.service.ForumService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.ui.other.MyRecyclerViewDivider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ForumTopicRecyclerViewAdapter extends RecyclerView.Adapter<ForumTopicRecyclerViewAdapter.ViewHolder> {

    private final Context context;
    private final List<TopicCommentInfo> topicCommentInfos;
    private final OnItemInteractionListener mListener;
    private final UserInfo userInfo;
    private int opened;

    public ForumTopicRecyclerViewAdapter(Context context, List<TopicCommentInfo> topicCommentInfos,
                                         OnItemInteractionListener mListener, UserInfo userInfo) {
        this.context = context;
        this.topicCommentInfos = topicCommentInfos;
        this.mListener = mListener;
        this.userInfo = userInfo;
        this.opened = -1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_forum_topic_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.mItem = topicCommentInfos.get(position);
        holder.indexView.setText(IUtils.format("{}楼", position + 1));
        if (holder.mItem.getAuthorInfo().getUserType() == UserType.Teacher) {
            holder.usernameView.setText(IUtils.format("[老师]{}", holder.mItem.getAuthorInfo().getUsername()));
        } else {
            holder.usernameView.setText(holder.mItem.getAuthorInfo().getUsername());
        }
        holder.contentView.setText(holder.mItem.getContent());
        holder.timeView.setText(IUtils.format("{}",
                IUtils.DATE_TIME_FORMATTER.format(new Date(holder.mItem.getCreateTime()))));

        holder.mView.setOnLongClickListener(v -> {
            if (mListener != null) {
                return mListener.onItemLongClick(holder);
            }
            return false;
        });

        holder.refreshTopicReply();

        if (position == opened) {
            holder.replyLayout.setVisibility(View.VISIBLE);
        } else {
            holder.replyLayout.setVisibility(View.GONE);
        }

    }

    @Override
    public int getItemCount() {
        return topicCommentInfos.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements ForumTopicReplyRecyclerViewAdapter.OnItemInteractionListener {
        private final View mView;
        private final TextView indexView;
        private final TextView usernameView;
        private final TextView contentView;
        private final TextView timeView;
        private final TextView replyView;
        private final ConstraintLayout replyLayout;
        private final RecyclerView recyclerView;
        private final EditText replyEditView;
        private final Button addReplyButton;

        private final List<TopicReplyInfo> topicReplyInfos;
        private final ForumTopicReplyRecyclerViewAdapter recyclerViewAdapter;

        private TopicCommentInfo mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            indexView = view.findViewById(R.id.comment_index);
            usernameView = view.findViewById(R.id.comment_user_name);
            contentView = view.findViewById(R.id.comment_content);
            timeView = view.findViewById(R.id.comment_time);
            replyView = view.findViewById(R.id.comment_reply);
            replyLayout = view.findViewById(R.id.comment_reply_layout);
            recyclerView = view.findViewById(R.id.comment_reply_list);
            replyEditView = view.findViewById(R.id.comment_reply_editor);
            addReplyButton = view.findViewById(R.id.add_comment_reply);
            topicReplyInfos = new ArrayList<>();
            recyclerViewAdapter = new ForumTopicReplyRecyclerViewAdapter(context, this.topicReplyInfos, this);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
            linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.setAdapter(recyclerViewAdapter);
            recyclerView.addItemDecoration(new MyRecyclerViewDivider(context, LinearLayoutManager.HORIZONTAL,
                    1,ContextCompat.getColor(context,R.color.colorBlack)));

            replyView.setOnClickListener(v -> {
                if (opened == getAdapterPosition()) {
                    opened = -1;
                    notifyItemChanged(getAdapterPosition());
                } else {
                    int oldOpened = opened;
                    opened = getAdapterPosition();
                    notifyItemChanged(oldOpened);
                    notifyItemChanged(opened);
                }
            });

            addReplyButton.setOnClickListener(v -> {
                ForumService.getInstance().createTopicReply(handler, replyEditView.getText().toString().trim(), mItem.getTopicCommentId());
            });
            
        }

        public TopicCommentInfo getTopicCommentInfo() {
            return mItem;
        }

        private void refreshTopicReply() {
            ForumService.getInstance().findAllTopicReply(handler, mItem.getTopicCommentId());
        }

        private void onReplyInfoSetChange() {
            replyView.setText(IUtils.format("回复({})", topicReplyInfos.size()));
            recyclerViewAdapter.notifyDataSetChanged();
        }

        @Override
        public boolean onItemLongClick(ForumTopicReplyRecyclerViewAdapter.ViewHolder viewHolder) {
            final TopicReplyInfo topicReplyInfo = viewHolder.getTopicReplyInfo();
            if (topicReplyInfo.getAuthorId() != userInfo.getUserId()) {
                return false;
            }
            AlertDialog.Builder confirmDeleteDialog =
                    new AlertDialog.Builder(context);
            confirmDeleteDialog.setTitle("删除回复");
            confirmDeleteDialog.setMessage("确定删除该回复？");
            confirmDeleteDialog.setPositiveButton("确定", (ed, which) -> {
                ForumService.getInstance().deleteTopicReply(handler, topicReplyInfo.getTopicReplyId());
            });
            confirmDeleteDialog.setNegativeButton("取消", (ed, which) -> {
                ed.cancel();
                ed.dismiss();
            });
            confirmDeleteDialog.show();

            return true;
        }

        @SuppressLint("HandlerLeak")
        private Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == ForumService.FIND_ALL_TOPIC_REPLY_MSG) {
                    handleFindAllTopicReplyMsg(msg);
                } else if (msg.what == ForumService.CREATE_TOPIC_REPLY_MSG) {
                    handleCreateTopicReplyMsg(msg);
                } else if (msg.what == ForumService.DELETE_TOPIC_REPLY_MSG) {
                    handleDeleteTopicReplyMsg(msg);
                }
            }
        };

        private void handleFindAllTopicReplyMsg(Message msg) {
            ServiceResult serviceResult = (ServiceResult) msg.obj;
            Toast.makeText(context, serviceResult.msg(), Toast.LENGTH_SHORT).show();
            if (serviceResult.isSuccess()) {
                topicReplyInfos.clear();
                topicReplyInfos.addAll(serviceResult.extra(TopicReplyFindAllResponse.class).getTopicReplyInfoList());
                onReplyInfoSetChange();
            }
        }

        private void handleCreateTopicReplyMsg(Message msg) {
            ServiceResult serviceResult = (ServiceResult) msg.obj;
            Toast.makeText(context, serviceResult.msg(), Toast.LENGTH_SHORT).show();
            if (serviceResult.isSuccess()) {
                replyEditView.setText("");
                TopicReplyInfo topicReplyInfo = serviceResult.extra(TopicReplyCreateResponse.class).getTopicReplyInfo();
                if (topicReplyInfo != null) {
                    topicReplyInfos.add(topicReplyInfo);
                    onReplyInfoSetChange();
                }
            }
        }

        private void handleDeleteTopicReplyMsg(Message msg) {
            ServiceResult serviceResult = (ServiceResult) msg.obj;
            Toast.makeText(context, serviceResult.msg(), Toast.LENGTH_SHORT).show();
            if (serviceResult.isSuccess()) {
                long topicReplyId = serviceResult.extra(TopicReplyDeleteResponse.class).getTopicReplyId();
                int needDeleteIndex = -1;
                for (int i = 0; i < topicReplyInfos.size(); i++) {
                    if (topicReplyInfos.get(i).getTopicReplyId() == topicReplyId) {
                        needDeleteIndex = i;
                        break;
                    }
                }
                if (needDeleteIndex >= 0) {
                    topicReplyInfos.remove(needDeleteIndex);
                    onReplyInfoSetChange();
                }
            }
        }
    }

    public interface OnItemInteractionListener {

        boolean onItemLongClick(ViewHolder viewHolder);

    }
}

