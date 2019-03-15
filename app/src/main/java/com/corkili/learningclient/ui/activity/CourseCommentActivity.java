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
import android.widget.RatingBar;
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.generate.protobuf.Info.CourseCommentInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseCommentType;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.CourseCommentCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.CourseCommentFindAllResponse;
import com.corkili.learningclient.service.CourseCommentService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.ui.other.MyRecyclerViewDivider;

import java.util.ArrayList;
import java.util.List;

public class CourseCommentActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton addCommentFab;

    private CourseInfo courseInfo;

    private List<CourseCommentInfo> courseCommentInfos;

    private CourseCommentRecyclerViewAdapter recyclerViewAdapter;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_comment);
        UserType userType = (UserType) getIntent().getSerializableExtra(IntentParam.USER_TYPE);
        courseInfo = (CourseInfo) getIntent().getSerializableExtra(IntentParam.COURSE_INFO);
        if (userType == null || courseInfo == null) {
            throw new RuntimeException("Intent param lost");
        }
        recyclerView = findViewById(R.id.activity_course_comment_list);
        swipeRefreshLayout = findViewById(R.id.activity_course_comment_swipe_refresh_layout);
        addCommentFab = findViewById(R.id.fab_add_course_comment);
        if (userType != UserType.Student || !getIntent().getBooleanExtra(IntentParam.ALREADY_SUBSCRIBE, false)) {
            addCommentFab.setVisibility(View.GONE); // RestrictedApi
            addCommentFab.setSystemUiVisibility(View.GONE);
            addCommentFab.setEnabled(false);
        } else {
            addCommentFab.setOnClickListener(v -> showAddCommentDialog());
        }
        courseCommentInfos = new ArrayList<>();
        recyclerViewAdapter = new CourseCommentRecyclerViewAdapter(this, courseCommentInfos);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addItemDecoration(new MyRecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL,
                1,ContextCompat.getColor(this,R.color.colorBlack)));
        swipeRefreshLayout.setOnRefreshListener(this::refreshCourseCommentInfos);
        refreshCourseCommentInfos();
    }

    private void refreshCourseCommentInfos() {
        CourseCommentService.getInstance().findAllCourseComment(handler, courseInfo.getCourseId());
    }

    private void showAddCommentDialog() {
        AlertDialog.Builder addCommentDialog =
                new AlertDialog.Builder(CourseCommentActivity.this);
        final View dialogView = LayoutInflater.from(CourseCommentActivity.this)
                .inflate(R.layout.dialog_add_course_comment,null);
        addCommentDialog.setTitle("增加评论");
        addCommentDialog.setView(dialogView);
        addCommentDialog.setPositiveButton("确定",
                (dialog, which) -> {
                    // 获取EditView中的输入内容
                    RatingBar ratingBar = dialogView.findViewById(R.id.rating_bar_comment);
                    EditText commentEditView = dialogView.findViewById(R.id.text_edit_comment);
                    CourseCommentType commentType = CourseCommentType.forNumber((int) ratingBar.getRating() - 1);
                    CourseCommentService.getInstance().createCourseComment(handler,
                            commentType, commentEditView.getText().toString().trim(), courseInfo.getCourseId());
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    dialog.cancel();
                    dialog.dismiss();
                });
        addCommentDialog.show();
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CourseCommentService.FIND_ALL_COURSE_COMMENT_MSG) {
                handleFindAllCourseCommentMsg(msg);
            } else if (msg.what == CourseCommentService.CREATE_COURSE_COMMENT_MSG) {
                handleCreateCourseCommentMsg(msg);
            }
        }
    };

    private void handleFindAllCourseCommentMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            courseCommentInfos.clear();
            courseCommentInfos.addAll(serviceResult.extra(CourseCommentFindAllResponse.class).getCourseCommentInfoList());
            swipeRefreshLayout.setRefreshing(false);
            recyclerViewAdapter.notifyDataSetChanged();
        }
    }

    private void handleCreateCourseCommentMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            CourseCommentInfo courseCommentInfo = serviceResult.extra(CourseCommentCreateResponse.class).getCourseCommentInfo();
            if (courseCommentInfo != null) {
                courseCommentInfos.add(courseCommentInfo);
                recyclerViewAdapter.notifyDataSetChanged();
            }
        }
    }

}
