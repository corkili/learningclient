package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.ProtoUtils;
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkInfo;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedCourseWorkInfo;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedCourseWorkSimpleInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedCourseWorkFindAllResponse;
import com.corkili.learningclient.service.SubmittedCourseWorkService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.ui.adapter.SubmittedRecyclerViewAdapter;
import com.corkili.learningclient.ui.adapter.SubmittedRecyclerViewAdapter.ViewHolder;
import com.corkili.learningclient.ui.other.MyRecyclerViewDivider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SubmittedCourseWorkActivity extends AppCompatActivity implements SubmittedRecyclerViewAdapter.OnItemInteractionListener {

    private static final int REQUEST_CODE_CHECK_SUBMITTED_COURSE_WORK = 0xF1;

    private View courseWorkInformationView;
    private TextView indexView;
    private TextView submitView;
    private TextView courseWorkNameView;
    private TextView deadlineView;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private CourseWorkInfo courseWorkInfo;
    private UserInfo userInfo;

    private List<SubmittedCourseWorkSimpleInfo> submittedCourseWorkSimpleInfos;

    private SubmittedRecyclerViewAdapter recyclerViewAdapter;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submitted_course_work);
        courseWorkInfo = (CourseWorkInfo) getIntent().getSerializableExtra(IntentParam.COURSE_WORK_INFO);
        userInfo = (UserInfo) getIntent().getSerializableExtra(IntentParam.USER_INFO);
        if (userInfo == null || courseWorkInfo == null) {
            throw new RuntimeException("Intent param lost");
        }

        courseWorkInformationView = findViewById(R.id.course_work_information);
        indexView = courseWorkInformationView.findViewById(R.id.item_index);
        submitView = courseWorkInformationView.findViewById(R.id.item_submit);
        courseWorkNameView = courseWorkInformationView.findViewById(R.id.item_course_work_name);
        deadlineView = courseWorkInformationView.findViewById(R.id.item_deadline);

        indexView.setVisibility(View.GONE);
        if (courseWorkInfo.getOpen()) {
            if (courseWorkInfo.getHasDeadline() && courseWorkInfo.getDeadline() <= System.currentTimeMillis()) {
                submitView.setText("已关闭提交");
            } else {
                submitView.setText("已开放提交");
            }
        }
        courseWorkNameView.setSingleLine(false);
        courseWorkNameView.setText(courseWorkInfo.getCourseWorkName());
        deadlineView.setText(courseWorkInfo.getHasDeadline() ? IUtils.format("截止日期：{}",
                IUtils.DATE_FORMATTER.format(new Date(courseWorkInfo.getDeadline()))) : "截止日期：无限期");

        recyclerView = findViewById(R.id.activity_submit_list);
        swipeRefreshLayout = findViewById(R.id.activity_submitted_course_work_swipe_refresh_layout);

        submittedCourseWorkSimpleInfos = new ArrayList<>();
        recyclerViewAdapter = new SubmittedRecyclerViewAdapter(this, submittedCourseWorkSimpleInfos, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addItemDecoration(new MyRecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL,
                1,ContextCompat.getColor(this,R.color.colorBlack)));
        swipeRefreshLayout.setOnRefreshListener(this::refreshSubmittedCourseWorkSimpleInfos);
        refreshSubmittedCourseWorkSimpleInfos();
    }

    private void refreshSubmittedCourseWorkSimpleInfos() {
        SubmittedCourseWorkService.getInstance().findAllSubmittedCourseWork(handler, courseWorkInfo.getCourseWorkId());
    }

    private void enterCourseWorkDetailActivity(SubmittedCourseWorkSimpleInfo submittedCourseWorkSimpleInfo) {
        if (userInfo.getUserType() == UserType.Teacher) {
            Intent intent = new Intent(SubmittedCourseWorkActivity.this, CourseWorkDetailActivity.class);
            intent.putExtra(IntentParam.USER_INFO, userInfo);
            intent.putExtra(IntentParam.COURSE_WORK_INFO, courseWorkInfo);
            intent.putExtra(IntentParam.SUBMITTED_COURSE_WORK_ID, submittedCourseWorkSimpleInfo.getSubmittedCourseWorkId());
            startActivityForResult(intent, REQUEST_CODE_CHECK_SUBMITTED_COURSE_WORK);
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SubmittedCourseWorkService.FIND_ALL_SUBMITTED_COURSE_WORK_MSG) {
                handleFindAllSubmittedCourseWorkMsg(msg);
            }
        }
    };

    private void handleFindAllSubmittedCourseWorkMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            submittedCourseWorkSimpleInfos.clear();
            for (SubmittedCourseWorkSimpleInfo submittedCourseWorkSimpleInfo : serviceResult.extra(SubmittedCourseWorkFindAllResponse.class).getSubmittedCourseWorkSimpleInfoList()) {
                if (submittedCourseWorkSimpleInfo.getFinished()) {
                    submittedCourseWorkSimpleInfos.add(submittedCourseWorkSimpleInfo);
                }
            }
            swipeRefreshLayout.setRefreshing(false);
            recyclerViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        if (data == null) {
            return;
        }
        SubmittedCourseWorkInfo submittedCourseWorkInfo = (SubmittedCourseWorkInfo) data.getSerializableExtra(IntentParam.SUBMITTED_COURSE_WORK_INFO);
        if (submittedCourseWorkInfo == null) {
            return;
        }
        if (requestCode == REQUEST_CODE_CHECK_SUBMITTED_COURSE_WORK) {
            int needReplaceIndex = -1;
            for (int i = 0; i < submittedCourseWorkSimpleInfos.size(); i++) {
                SubmittedCourseWorkSimpleInfo submittedCourseWorkSimpleInfo = submittedCourseWorkSimpleInfos.get(i);
                if (submittedCourseWorkSimpleInfo.getSubmittedCourseWorkId() == submittedCourseWorkInfo.getSubmittedCourseWorkId()) {
                    needReplaceIndex = i;
                    break;
                }
            }
            if (needReplaceIndex >= 0) {
                submittedCourseWorkSimpleInfos.set(needReplaceIndex, ProtoUtils.simplifySubmittedCourseWorkInfo(submittedCourseWorkInfo));
                recyclerViewAdapter.notifyItemChanged(needReplaceIndex);
            } else {
                submittedCourseWorkSimpleInfos.add(ProtoUtils.simplifySubmittedCourseWorkInfo(submittedCourseWorkInfo));
                recyclerViewAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onItemClick(ViewHolder viewHolder) {
        final SubmittedCourseWorkSimpleInfo submittedCourseWorkSimpleInfo = viewHolder.getSubmittedCourseWorkSimpleInfo();
        if (submittedCourseWorkSimpleInfo != null) {
            enterCourseWorkDetailActivity(submittedCourseWorkSimpleInfo);
        }
    }

}
