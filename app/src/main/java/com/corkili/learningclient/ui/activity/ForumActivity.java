package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
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
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.generate.protobuf.Info.ForumTopicInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Response.ForumTopicCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.ForumTopicDeleteResponse;
import com.corkili.learningclient.generate.protobuf.Response.ForumTopicFindAllResponse;
import com.corkili.learningclient.generate.protobuf.Response.ForumTopicUpdateResponse;
import com.corkili.learningclient.service.ForumService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.ui.activity.ForumRecyclerViewAdapter.ViewHolder;
import com.corkili.learningclient.ui.other.MyRecyclerViewDivider;

import java.util.ArrayList;
import java.util.List;

public class ForumActivity extends AppCompatActivity implements ForumRecyclerViewAdapter.OnItemInteractionListener {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton addTopicFab;

    private CourseInfo courseInfo;
    private UserInfo userInfo;

    private List<ForumTopicInfo> forumTopicInfos;

    private ForumRecyclerViewAdapter recyclerViewAdapter;

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
        recyclerView = findViewById(R.id.activity_forum_topic_list);
        swipeRefreshLayout = findViewById(R.id.activity_forum_topic_swipe_refresh_layout);
        addTopicFab = findViewById(R.id.fab_add_topic);
        addTopicFab.setOnClickListener(v -> showAddTopicDialog());
        forumTopicInfos = new ArrayList<>();
        recyclerViewAdapter = new ForumRecyclerViewAdapter(this, forumTopicInfos, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addItemDecoration(new MyRecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL,
                1,ContextCompat.getColor(this,R.color.colorBlack)));
        swipeRefreshLayout.setOnRefreshListener(this::refreshForumTopicInfos);
        refreshForumTopicInfos();
    }

    private void refreshForumTopicInfos() {
        ForumService.getInstance().findAllForumTopic(handler, courseInfo.getCourseId());
    }

    private void showAddTopicDialog() {
        AlertDialog.Builder addTopicDialog =
                new AlertDialog.Builder(ForumActivity.this);
        final View dialogView = LayoutInflater.from(ForumActivity.this)
                .inflate(R.layout.dialog_add_topic,null);
        addTopicDialog.setTitle("发表帖子");
        addTopicDialog.setView(dialogView);
        addTopicDialog.setPositiveButton("确定",
                (dialog, which) -> {
                    EditText titleEditView = dialogView.findViewById(R.id.text_edit_title);
                    EditText descriptionEditView = dialogView.findViewById(R.id.text_edit_description);
                    ForumService.getInstance().createForumTopic(handler,
                            titleEditView.getText().toString().trim(),
                            descriptionEditView.getText().toString().trim(), courseInfo.getCourseId());
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
            swipeRefreshLayout.setRefreshing(false);
            recyclerViewAdapter.notifyDataSetChanged();
        }
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
        final AlertDialog.Builder dialog =
                new AlertDialog.Builder(ForumActivity.this);
        dialog.setTitle("请选择操作");

        dialog.setPositiveButton("编辑", (d, w) -> {
            d.dismiss();
            AlertDialog.Builder editTopicDialog =
                    new AlertDialog.Builder(ForumActivity.this);
            final View dialogView = LayoutInflater.from(ForumActivity.this)
                    .inflate(R.layout.dialog_add_topic,null);
            dialogView.<EditText>findViewById(R.id.text_edit_title).setText(forumTopicInfo.getTitle());
            dialogView.findViewById(R.id.text_edit_title).setEnabled(false);
            dialogView.<EditText>findViewById(R.id.text_edit_description).setText(forumTopicInfo.getDescription());
            editTopicDialog.setTitle("修改帖子");
            editTopicDialog.setView(dialogView);
            editTopicDialog.setPositiveButton("确定", (ed, which) -> {
                EditText descriptionView = dialogView.findViewById(R.id.text_edit_description);
                String description = descriptionView.getText().toString().trim();
                if (!description.equals(forumTopicInfo.getDescription())) {
                    ForumService.getInstance().updateForumTopic(handler, forumTopicInfo.getForumTopicId(), description);
                }
            });
            editTopicDialog.setNegativeButton("取消", (ed, which) -> {
                ed.cancel();
                ed.dismiss();
            });
            editTopicDialog.show();
        });

        dialog.setNegativeButton("删除", (d, w) -> {
            d.dismiss();
            AlertDialog.Builder confirmDeleteDialog =
                    new AlertDialog.Builder(ForumActivity.this);
            confirmDeleteDialog.setTitle("删除帖子");
            confirmDeleteDialog.setMessage("确定删除该帖子？");
            confirmDeleteDialog.setPositiveButton("确定", (ed, which) -> {
                ForumService.getInstance().deleteForumTopic(handler, forumTopicInfo.getForumTopicId());
            });
            confirmDeleteDialog.setNegativeButton("取消", (ed, which) -> {
                ed.cancel();
                ed.dismiss();
            });
            confirmDeleteDialog.show();
        });

        dialog.show();

        return true;
    }
}
