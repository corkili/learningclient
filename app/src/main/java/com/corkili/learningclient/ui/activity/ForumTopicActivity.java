package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.ForumTopicInfo;
import com.corkili.learningclient.generate.protobuf.Info.TopicCommentInfo;
import com.corkili.learningclient.generate.protobuf.Info.TopicReplyInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.TopicCommentCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.TopicCommentDeleteResponse;
import com.corkili.learningclient.generate.protobuf.Response.TopicCommentFindAllResponse;
import com.corkili.learningclient.service.ForumService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.ui.activity.ForumTopicRecyclerViewAdapter.ViewHolder;
import com.corkili.learningclient.ui.other.MyRecyclerViewDivider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForumTopicActivity extends AppCompatActivity implements ForumTopicRecyclerViewAdapter.OnItemInteractionListener {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton addTopicFab;

    private CourseInfo courseInfo;
    private UserInfo userInfo;
    private ForumTopicInfo forumTopicInfo;

    private List<TopicCommentInfo> topicCommentInfos;
    private Map<Long, List<TopicReplyInfo>> topicReplyInfoMap;

    private ForumTopicRecyclerViewAdapter recyclerViewAdapter;

    @SuppressLint({"RestrictedApi", "UseSparseArrays"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum_topic);
        courseInfo = (CourseInfo) getIntent().getSerializableExtra(IntentParam.COURSE_INFO);
        userInfo = (UserInfo) getIntent().getSerializableExtra(IntentParam.USER_INFO);
        forumTopicInfo = (ForumTopicInfo) getIntent().getSerializableExtra(IntentParam.FORUM_TOPIC_INFO);
        if (userInfo == null || courseInfo == null || forumTopicInfo == null) {
            throw new RuntimeException("Intent param lost");
        }

        this.<TextView>findViewById(R.id.topic_title).setText(forumTopicInfo.getTitle());
        this.<TextView>findViewById(R.id.topic_user_name).setText(IUtils.format("{}{}",
                forumTopicInfo.getAuthorInfo().getUserType() == UserType.Teacher ? "老师" : "",
                forumTopicInfo.getAuthorInfo().getUsername()));
        this.<TextView>findViewById(R.id.topic_description).setText(forumTopicInfo.getDescription());
        this.<TextView>findViewById(R.id.topic_time).setText(IUtils.DATE_TIME_FORMATTER.format(forumTopicInfo.getUpdateTime()));

        addTopicFab = findViewById(R.id.fab_add_topic_comment);
        addTopicFab.setOnClickListener(v -> showAddTopicCommentDialog());

        topicCommentInfos = new ArrayList<>();
        topicReplyInfoMap = new HashMap<>();

        recyclerView = findViewById(R.id.activity_forum_topic_list);
        recyclerViewAdapter = new ForumTopicRecyclerViewAdapter(this, topicCommentInfos, this, userInfo);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addItemDecoration(new MyRecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL,
                1,ContextCompat.getColor(this,R.color.colorBlack)));

        swipeRefreshLayout = findViewById(R.id.activity_forum_topic_swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshTopicCommentInfos);
        refreshTopicCommentInfos();
    }

    private void refreshTopicCommentInfos() {
        ForumService.getInstance().findAllTopicComment(handler, forumTopicInfo.getForumTopicId());
    }

    private void showAddTopicCommentDialog() {
        AlertDialog.Builder addTopicDialog =
                new AlertDialog.Builder(ForumTopicActivity.this);
        final View dialogView = LayoutInflater.from(ForumTopicActivity.this)
                .inflate(R.layout.dialog_one_editor,null);
        dialogView.<TextView>findViewById(R.id.text_view_flag).setText("内容");
        addTopicDialog.setTitle("回复帖子");
        addTopicDialog.setView(dialogView);
        addTopicDialog.setPositiveButton("确定",
                (dialog, which) -> {
                    EditText contentEditView = dialogView.findViewById(R.id.text_edit_content);
                    ForumService.getInstance().createTopicComment(handler,
                            contentEditView.getText().toString().trim(),
                            forumTopicInfo.getForumTopicId());
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    dialog.cancel();
                    dialog.dismiss();
                });
        addTopicDialog.show();
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ForumService.FIND_ALL_TOPIC_COMMENT_MSG) {
                handleFindAllTopicCommentMsg(msg);
            } else if (msg.what == ForumService.CREATE_TOPIC_COMMENT_MSG) {
                handleCreateTopicCommentMsg(msg);
            } else if (msg.what == ForumService.DELETE_TOPIC_COMMENT_MSG) {
                handleDeleteTopicCommentMsg(msg);
            }
        }
    };

    private void handleFindAllTopicCommentMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            topicCommentInfos.clear();
            topicCommentInfos.addAll(serviceResult.extra(TopicCommentFindAllResponse.class).getTopicCommentInfoList());
            swipeRefreshLayout.setRefreshing(false);
            recyclerViewAdapter.notifyDataSetChanged();
        }
    }

    private void handleCreateTopicCommentMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            TopicCommentInfo topicCommentInfo = serviceResult.extra(TopicCommentCreateResponse.class).getTopicCommentInfo();
            if (topicCommentInfo != null) {
                topicCommentInfos.add(topicCommentInfo);
                recyclerViewAdapter.notifyDataSetChanged();
            }
        }
    }

    private void handleDeleteTopicCommentMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            long topicCommentId = serviceResult.extra(TopicCommentDeleteResponse.class).getTopicCommentId();
            int needDeleteIndex = -1;
            for (int i = 0; i < topicCommentInfos.size(); i++) {
                if (topicCommentInfos.get(i).getTopicCommentId() == topicCommentId) {
                    needDeleteIndex = i;
                    break;
                }
            }
            if (needDeleteIndex >= 0) {
                topicCommentInfos.remove(needDeleteIndex);
                recyclerViewAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public boolean onItemLongClick(ViewHolder viewHolder) {
        final TopicCommentInfo topicCommentInfo = viewHolder.getTopicCommentInfo();
        if (topicCommentInfo.getAuthorId() != userInfo.getUserId()) {
            return false;
        }
        AlertDialog.Builder confirmDeleteDialog =
                new AlertDialog.Builder(ForumTopicActivity.this);
        confirmDeleteDialog.setTitle("删除帖子评论");
        confirmDeleteDialog.setMessage("确定删除该帖子评论？");
        confirmDeleteDialog.setPositiveButton("确定", (ed, which) -> {
            ForumService.getInstance().deleteTopicComment(handler, topicCommentInfo.getTopicCommentId());
        });
        confirmDeleteDialog.setNegativeButton("取消", (ed, which) -> {
            ed.cancel();
            ed.dismiss();
        });
        confirmDeleteDialog.show();

        return true;
    }
}
