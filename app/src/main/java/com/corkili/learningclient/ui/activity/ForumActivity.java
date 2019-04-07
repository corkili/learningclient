package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.ForumTopicInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Response.ForumTopicCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.ForumTopicDeleteResponse;
import com.corkili.learningclient.generate.protobuf.Response.ForumTopicFindAllResponse;
import com.corkili.learningclient.generate.protobuf.Response.ForumTopicUpdateResponse;
import com.corkili.learningclient.service.ForumService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.ui.adapter.ForumRecyclerViewAdapter;
import com.corkili.learningclient.ui.adapter.ForumRecyclerViewAdapter.ViewHolder;
import com.corkili.learningclient.ui.other.MyRecyclerViewDivider;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog.CustomDialogBuilder;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.qmuiteam.qmui.widget.pullRefreshLayout.QMUIPullRefreshLayout;
import com.qmuiteam.qmui.widget.pullRefreshLayout.QMUIPullRefreshLayout.OnPullListener;

import java.util.ArrayList;
import java.util.List;

public class ForumActivity extends AppCompatActivity implements ForumRecyclerViewAdapter.OnItemInteractionListener {

    private QMUITopBarLayout topBar;
    private RecyclerView recyclerView;
    private QMUIPullRefreshLayout swipeRefreshLayout;
    private TextView tipView;

    private CourseInfo courseInfo;
    private UserInfo userInfo;

    private List<ForumTopicInfo> forumTopicInfos;

    private ForumRecyclerViewAdapter recyclerViewAdapter;

    private boolean shouldFinishRefresh;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum);
        courseInfo = (CourseInfo) getIntent().getSerializableExtra(IntentParam.COURSE_INFO);
        userInfo = (UserInfo) getIntent().getSerializableExtra(IntentParam.USER_INFO);
        if (userInfo == null || courseInfo == null) {
            throw new RuntimeException("Intent param lost");
        }

        topBar = findViewById(R.id.topbar);

        topBar.setTitle("课程论坛");
        topBar.addLeftBackImageButton().setOnClickListener(v -> ForumActivity.this.finish());
        topBar.addRightImageButton(R.drawable.ic_edit_24dp, R.id.topbar_right_edit).setOnClickListener(v -> showAddTopicDialog());

        tipView = findViewById(R.id.tip);
        recyclerView = findViewById(R.id.activity_forum_topic_list);
        swipeRefreshLayout = findViewById(R.id.activity_forum_topic_swipe_refresh_layout);
        forumTopicInfos = new ArrayList<>();
        recyclerViewAdapter = new ForumRecyclerViewAdapter(this, forumTopicInfos, userInfo, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addItemDecoration(new MyRecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL,
                1,ContextCompat.getColor(this,R.color.colorBlack)));
        swipeRefreshLayout.setOnPullListener(new OnPullListener() {
            @Override
            public void onMoveTarget(int offset) {

            }

            @Override
            public void onMoveRefreshView(int offset) {

            }

            @Override
            public void onRefresh() {
                shouldFinishRefresh = true;
                refreshForumTopicInfos();
            }
        });
        refreshForumTopicInfos();
    }

    private void updateTipView() {
        if (forumTopicInfos.isEmpty()) {
            tipView.setVisibility(View.VISIBLE);
        } else {
            tipView.setVisibility(View.GONE);
        }
    }

    private void refreshForumTopicInfos() {
        ForumService.getInstance().findAllForumTopic(handler, courseInfo.getCourseId());
    }

    private void showAddTopicDialog() {
        QMUIDialog.CustomDialogBuilder builder = new CustomDialogBuilder(this);
        builder.setTitle("发表帖子");
        builder.setLayout(R.layout.dialog_add_topic);
        builder.addAction("取消", (dialog, index) -> dialog.dismiss());
        builder.addAction("确定", ((dialog, index) -> {
            dialog.dismiss();
            EditText titleEditView = dialog.findViewById(R.id.text_edit_title);
            EditText descriptionEditView = dialog.findViewById(R.id.text_edit_description);
            ForumService.getInstance().createForumTopic(handler,
                    titleEditView.getText().toString().trim(),
                    descriptionEditView.getText().toString().trim(), courseInfo.getCourseId());
        }));
        builder.show();
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ForumService.FIND_ALL_FORUM_TOPIC_MSG) {
                handleFindAllForumTopicMsg(msg);
            } else if (msg.what == ForumService.CREATE_FORUM_TOPIC_MSG) {
                handleCreateForumTopicMsg(msg);
            } else if (msg.what == ForumService.UPDATE_FORUM_TOPIC_MSG) {
                handleUpdateForumTopicMsg(msg);
            } else if (msg.what == ForumService.DELETE_FORUM_TOPIC_MSG) {
                handleDeleteForumTopicMsg(msg);
            }
        }
    };

    private void handleFindAllForumTopicMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            forumTopicInfos.clear();
            forumTopicInfos.addAll(serviceResult.extra(ForumTopicFindAllResponse.class).getForumTopicInfoList());
            recyclerViewAdapter.notifyDataSetChanged();
        }
        if (shouldFinishRefresh) {
            shouldFinishRefresh = false;
            swipeRefreshLayout.finishRefresh();
        }
        updateTipView();
    }

    private void handleCreateForumTopicMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            ForumTopicInfo forumTopicInfo = serviceResult.extra(ForumTopicCreateResponse.class).getForumTopicInfo();
            if (forumTopicInfo != null) {
                forumTopicInfos.add(forumTopicInfo);
                recyclerViewAdapter.notifyDataSetChanged();
            }
        }
        updateTipView();
    }

    private void handleUpdateForumTopicMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            ForumTopicInfo forumTopicInfo = serviceResult.extra(ForumTopicUpdateResponse.class).getForumTopicInfo();
            int needReplaceIndex = -1;
            for (int i = 0; i < forumTopicInfos.size(); i++) {
                if (forumTopicInfos.get(i).getForumTopicId() == forumTopicInfo.getForumTopicId()) {
                    needReplaceIndex = i;
                    break;
                }
            }
            if (needReplaceIndex >= 0) {
                forumTopicInfos.set(needReplaceIndex, forumTopicInfo);
                recyclerViewAdapter.notifyDataSetChanged();
            }
        }
        updateTipView();
    }

    private void handleDeleteForumTopicMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            long forumTopicId = serviceResult.extra(ForumTopicDeleteResponse.class).getForumTopicId();
            int needDeleteIndex = -1;
            for (int i = 0; i < forumTopicInfos.size(); i++) {
                if (forumTopicInfos.get(i).getForumTopicId() == forumTopicId) {
                    needDeleteIndex = i;
                    break;
                }
            }
            if (needDeleteIndex >= 0) {
                forumTopicInfos.remove(needDeleteIndex);
                recyclerViewAdapter.notifyDataSetChanged();
            }
        }
        updateTipView();
    }

    @Override
    public void onItemClick(ViewHolder viewHolder) {
        final ForumTopicInfo forumTopicInfo = viewHolder.getForumTopicInfo();
        Intent intent = new Intent(ForumActivity.this, ForumTopicActivity.class);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        intent.putExtra(IntentParam.USER_INFO, userInfo);
        intent.putExtra(IntentParam.FORUM_TOPIC_INFO, forumTopicInfo);
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(ViewHolder viewHolder) {
        final ForumTopicInfo forumTopicInfo = viewHolder.getForumTopicInfo();
        if (forumTopicInfo.getAuthorId() != userInfo.getUserId()) {
            return false;
        }

        new QMUIBottomSheet.BottomListSheetBuilder(this)
                .addItem("修改描述")
                .addItem("删除帖子")
                .setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                    dialog.dismiss();
                    if (position == 0) {
                        final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(this);
                        builder.setTitle("修改帖子描述")
                                .setPlaceholder("输入帖子描述")
                                .setDefaultText(forumTopicInfo.getDescription())
                                .setInputType(InputType.TYPE_CLASS_TEXT)
                                .addAction("取消", (d, index) -> d.dismiss())
                                .addAction("确定", (d, index) -> {
                                    d.dismiss();
                                    CharSequence description = builder.getEditText().getText();
                                    if (description != null && !description.toString().equals(forumTopicInfo.getDescription())) {
                                        ForumService.getInstance().updateForumTopic(handler, forumTopicInfo.getForumTopicId(), description.toString());
                                    }
                                })
                                .show();
                    } else if (position == 1) {
                        new QMUIDialog.MessageDialogBuilder(this)
                                .setTitle("删除帖子")
                                .setMessage(IUtils.format("确定删除帖子-[{}]？", forumTopicInfo.getTitle()))
                                .addAction("取消", (d, index) -> d.dismiss())
                                .addAction(0, "删除", QMUIDialogAction.ACTION_PROP_NEGATIVE, (d, index) -> {
                                    d.dismiss();
                                    ForumService.getInstance().deleteForumTopic(handler, forumTopicInfo.getForumTopicId());
                                })
                                .show();
                    }
                })
                .build()
                .show();

        return true;
    }
}
