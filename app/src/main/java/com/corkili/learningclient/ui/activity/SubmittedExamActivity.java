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
import com.corkili.learningclient.generate.protobuf.Info.ExamInfo;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedExamInfo;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedExamSimpleInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedExamFindAllResponse;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.service.SubmittedExamService;
import com.corkili.learningclient.ui.adapter.SubmittedRecyclerViewAdapter;
import com.corkili.learningclient.ui.adapter.SubmittedRecyclerViewAdapter.ViewHolder;
import com.corkili.learningclient.ui.other.MyRecyclerViewDivider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SubmittedExamActivity extends AppCompatActivity implements SubmittedRecyclerViewAdapter.OnItemInteractionListener {

    private static final int REQUEST_CODE_CHECK_SUBMITTED_EXAM = 0xF1;

    private View examInformationView;
    private TextView indexView;
    private TextView submitView;
    private TextView examNameView;
    private TextView startTimeView;
    private TextView endTimeView;
    
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ExamInfo examInfo;
    private UserInfo userInfo;

    private List<SubmittedExamSimpleInfo> submittedExamSimpleInfos;

    private SubmittedRecyclerViewAdapter recyclerViewAdapter;
    
    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submitted_exam);
        examInfo = (ExamInfo) getIntent().getSerializableExtra(IntentParam.EXAM_INFO);
        userInfo = (UserInfo) getIntent().getSerializableExtra(IntentParam.USER_INFO);
        if (userInfo == null || examInfo == null) {
            throw new RuntimeException("Intent param lost");
        }

        examInformationView = findViewById(R.id.exam_information);
        indexView = examInformationView.findViewById(R.id.item_index);
        submitView = examInformationView.findViewById(R.id.item_submit);
        examNameView = examInformationView.findViewById(R.id.item_exam_name);
        startTimeView = examInformationView.findViewById(R.id.item_start_time);
        endTimeView = examInformationView.findViewById(R.id.item_end_time);

        indexView.setVisibility(View.GONE);
        if (examInfo.getStartTime() <= System.currentTimeMillis()) {
            if (examInfo.getEndTime() <= System.currentTimeMillis()) {
                submitView.setText("已关闭提交");
            } else {
                submitView.setText("已开放提交");
            }
        }
        examNameView.setSingleLine(false);
        examNameView.setText(examInfo.getExamName());
        startTimeView.setText(IUtils.format("开始时间：{}", IUtils.DATE_TIME_FORMATTER
                .format(new Date(examInfo.getStartTime()))));
        endTimeView.setText(IUtils.format("结束时间：{}", IUtils.DATE_TIME_FORMATTER
                .format(new Date(examInfo.getEndTime()))));
        
        recyclerView = findViewById(R.id.activity_submit_list);
        swipeRefreshLayout = findViewById(R.id.activity_submitted_exam_swipe_refresh_layout);
        
        submittedExamSimpleInfos = new ArrayList<>();
        recyclerViewAdapter = new SubmittedRecyclerViewAdapter(submittedExamSimpleInfos, this, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addItemDecoration(new MyRecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL,
                1,ContextCompat.getColor(this,R.color.colorBlack)));
        swipeRefreshLayout.setOnRefreshListener(this::refreshSubmittedExamSimpleInfos);
        refreshSubmittedExamSimpleInfos();
    }

    private void refreshSubmittedExamSimpleInfos() {
        SubmittedExamService.getInstance().findAllSubmittedExam(handler, examInfo.getExamId());
    }

    private void enterExamDetailActivity(SubmittedExamSimpleInfo submittedExamSimpleInfo) {
        if (userInfo.getUserType() == UserType.Teacher) {
            Intent intent = new Intent(SubmittedExamActivity.this, ExamDetailActivity.class);
            intent.putExtra(IntentParam.USER_INFO, userInfo);
            intent.putExtra(IntentParam.EXAM_INFO, examInfo);
            intent.putExtra(IntentParam.SUBMITTED_EXAM_ID, submittedExamSimpleInfo.getSubmittedExamId());
            startActivityForResult(intent, REQUEST_CODE_CHECK_SUBMITTED_EXAM);
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SubmittedExamService.FIND_ALL_SUBMITTED_EXAM_MSG) {
                handleFindAllSubmittedExamMsg(msg);
            }
        }
    };

    private void handleFindAllSubmittedExamMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            submittedExamSimpleInfos.clear();
            for (SubmittedExamSimpleInfo submittedExamSimpleInfo : serviceResult.extra(SubmittedExamFindAllResponse.class).getSubmittedExamSimpleInfoList()) {
                if (submittedExamSimpleInfo.getFinished()) {
                    submittedExamSimpleInfos.add(submittedExamSimpleInfo);
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
        SubmittedExamInfo submittedExamInfo = (SubmittedExamInfo) data.getSerializableExtra(IntentParam.SUBMITTED_EXAM_INFO);
        if (submittedExamInfo == null) {
            return;
        }
        if (requestCode == REQUEST_CODE_CHECK_SUBMITTED_EXAM) {
            int needReplaceIndex = -1;
            for (int i = 0; i < submittedExamSimpleInfos.size(); i++) {
                SubmittedExamSimpleInfo submittedExamSimpleInfo = submittedExamSimpleInfos.get(i);
                if (submittedExamSimpleInfo.getSubmittedExamId() == submittedExamInfo.getSubmittedExamId()) {
                    needReplaceIndex = i;
                    break;
                }
            }
            if (needReplaceIndex >= 0) {
                submittedExamSimpleInfos.set(needReplaceIndex, ProtoUtils.simplifySubmittedExamInfo(submittedExamInfo));
                recyclerViewAdapter.notifyItemChanged(needReplaceIndex);
            } else {
                submittedExamSimpleInfos.add(ProtoUtils.simplifySubmittedExamInfo(submittedExamInfo));
                recyclerViewAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onItemClick(ViewHolder viewHolder) {
        final SubmittedExamSimpleInfo submittedExamSimpleInfo = viewHolder.getSubmittedExamSimpleInfo();
        if (submittedExamSimpleInfo != null) {
            enterExamDetailActivity(submittedExamSimpleInfo);
        }
    }
    
}
