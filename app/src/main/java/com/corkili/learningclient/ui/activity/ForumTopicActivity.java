package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.View;
import android.widget.TextView;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.UIHelper;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.ForumTopicInfo;
import com.corkili.learningclient.generate.protobuf.Info.TopicCommentInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.TopicCommentCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.TopicCommentDeleteResponse;
import com.corkili.learningclient.generate.protobuf.Response.TopicCommentFindAllResponse;
import com.corkili.learningclient.service.ForumService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.ui.adapter.ForumTopicRecyclerViewAdapter;
import com.corkili.learningclient.ui.adapter.ForumTopicRecyclerViewAdapter.ViewHolder;
import com.corkili.learningclient.ui.other.MyRecyclerViewDivider;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;

import java.util.ArrayList;
import java.util.List;

public class ForumTopicActivity extends AppCompatActivity implements ForumTopicRecyclerViewAdapter.OnItemInteractionListener {

    private QMUITopBarLayout topBar;
    private RecyclerView recyclerView;
    private FloatingActionButton addTopicFab;
    private TextView tipView;

    private CourseInfo courseInfo;
    private UserInfo userInfo;
    private ForumTopicInfo forumTopicInfo;

    private List<TopicCommentInfo> topicCommentInfos;

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

        topBar = findViewById(R.id.topbar);

        topBar.setTitle("帖子详情");
        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());
        topBar.addRightImageButton(R.drawable.ic_refresh_24dp, R.id.topbar_right_refresh)
                .setOnClickListener(v -> refreshTopicCommentInfos());

        this.<TextView>findViewById(R.id.topic_title).setText(forumTopicInfo.getTitle());
        String username;
        if (forumTopicInfo.getAuthorInfo().getUserId() == userInfo.getUserId()) {
            username = IUtils.format("@[我]{}", forumTopicInfo.getAuthorInfo().getUsername());
        } else {
            username = IUtils.format("@{}{}",
                    forumTopicInfo.getAuthorInfo().getUserType() == UserType.Teacher ? "[老师]" : "",
                    forumTopicInfo.getAuthorInfo().getUsername());
        }
        this.<TextView>findViewById(R.id.topic_user_name).setText(username);
        this.<TextView>findViewById(R.id.topic_description).setText(forumTopicInfo.getDescription());
        this.<TextView>findViewById(R.id.topic_time).setText(IUtils.DATE_TIME_FORMATTER.format(forumTopicInfo.getUpdateTime()));

        addTopicFab = findViewById(R.id.fab_add_topic_comment);
        addTopicFab.setOnClickListener(v -> showAddTopicCommentDialog());

        tipView = findViewById(R.id.tip);

        topicCommentInfos = new ArrayList<>();

        recyclerView = findViewById(R.id.activity_forum_topic_list);
        recyclerViewAdapter = new ForumTopicRecyclerViewAdapter(this, topicCommentInfos, this, userInfo);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addItemDecoration(new MyRecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL,
                1,ContextCompat.getColor(this,R.color.colorBlack)));

        refreshTopicCommentInfos();
    }

    private void updateTipView() {
        if (topicCommentInfos.isEmpty()) {
            tipView.setVisibility(View.VISIBLE);
        } else {
            tipView.setVisibility(View.GONE);
        }
    }

    private void refreshTopicCommentInfos() {
        ForumService.getInstance().findAllTopicComment(handler, forumTopicInfo.getForumTopicId());
    }

    private void showAddTopicCommentDialog() {
        final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(this);
        builder.setTitle("回复帖子")
                .setPlaceholder("输入内容")
                .setInputType(InputType.TYPE_CLASS_TEXT)
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("确定", (dialog, index) -> {
                    CharSequence content = builder.getEditText().getText();
                    if (content != null) {
                        ForumService.getInstance().createTopicComment(handler, content.toString().trim(),
                                forumTopicInfo.getForumTopicId());
                    }
                    dialog.dismiss();
                })
                .show();
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
        if (serviceResult.isSuccess()) {
            topicCommentInfos.clear();
            topicCommentInfos.addAll(serviceResult.extra(TopicCommentFindAllResponse.class).getTopicCommentInfoList());
            recyclerViewAdapter.notifyDataSetChanged();
        } else {
            UIHelper.toast(this, serviceResult, raw -> "加载帖子评论失败");
        }
        updateTipView();
    }

    private void handleCreateTopicCommentMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        UIHelper.toast(this, serviceResult, raw -> serviceResult.isSuccess() ? "发表帖子评论成功" : "发表帖子评论失败");
        if (serviceResult.isSuccess()) {
            TopicCommentInfo topicCommentInfo = serviceResult.extra(TopicCommentCreateResponse.class).getTopicCommentInfo();
            if (topicCommentInfo != null) {
                topicCommentInfos.add(topicCommentInfo);
                recyclerViewAdapter.notifyDataSetChanged();
            }
        }
        updateTipView();
    }

    private void handleDeleteTopicCommentMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        UIHelper.toast(this, serviceResult, raw -> serviceResult.isSuccess() ? "删除帖子评论成功" : "删除帖子评论失败");
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
        updateTipView();
    }

    @Override
    public boolean onItemLongClick(ViewHolder viewHolder) {
        final TopicCommentInfo topicCommentInfo = viewHolder.getTopicCommentInfo();
        if (topicCommentInfo.getAuthorId() != userInfo.getUserId()) {
            return false;
        }

        new QMUIDialog.MessageDialogBuilder(this)
                .setTitle("删除帖子评论")
                .setMessage("确定删除该帖子评论？")
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction(0, "删除", QMUIDialogAction.ACTION_PROP_NEGATIVE, (dialog, index) -> {
                    ForumService.getInstance().deleteTopicComment(handler, topicCommentInfo.getTopicCommentId());
                    dialog.dismiss();
                })
                .show();

        return true;
    }
}
