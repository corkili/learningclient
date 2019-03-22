package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.ProtoUtils;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.ExamInfo;
import com.corkili.learningclient.generate.protobuf.Info.ExamSimpleInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Response.ExamFindAllResponse;
import com.corkili.learningclient.generate.protobuf.Response.ExamGetResponse;
import com.corkili.learningclient.service.ExamService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.ui.adapter.ExamRecyclerViewAdapter;
import com.corkili.learningclient.ui.adapter.ExamRecyclerViewAdapter.ViewHolder;
import com.corkili.learningclient.ui.other.MyRecyclerViewDivider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExamActivity extends AppCompatActivity implements ExamRecyclerViewAdapter.OnItemInteractionListener {

    private static final int REQUEST_CODE_CREATE_EXAM = 0xF1;
    private static final int REQUEST_CODE_EDIT_EXAM = 0xF2;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton addExamFab;

    private CourseInfo courseInfo;
    private UserInfo userInfo;

    private List<ExamSimpleInfo> examSimpleInfos;
    private Map<Long, ExamInfo> examInfoCache;

    private ExamRecyclerViewAdapter recyclerViewAdapter;

    private boolean startEditActivity;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam);
        courseInfo = (CourseInfo) getIntent().getSerializableExtra(IntentParam.COURSE_INFO);
        userInfo = (UserInfo) getIntent().getSerializableExtra(IntentParam.USER_INFO);
        if (userInfo == null || courseInfo == null) {
            throw new RuntimeException("Intent param lost");
        }
        startEditActivity = false;
        recyclerView = findViewById(R.id.activity_exam_list);
        swipeRefreshLayout = findViewById(R.id.activity_exam_swipe_refresh_layout);
        addExamFab = findViewById(R.id.fab_add_exam);
        addExamFab.setOnClickListener(v -> enterAddExamActivity());
        examSimpleInfos = new ArrayList<>();
        examInfoCache = new ConcurrentHashMap<>();
        recyclerViewAdapter = new ExamRecyclerViewAdapter(this, examSimpleInfos, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addItemDecoration(new MyRecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL,
                1,ContextCompat.getColor(this,R.color.colorBlack)));
        swipeRefreshLayout.setOnRefreshListener(this::refreshExamSimpleInfos);
        refreshExamSimpleInfos();
    }

    private void refreshExamSimpleInfos() {
        ExamService.getInstance().findAllExam(handler, courseInfo.getCourseId());
    }

    private void enterAddExamActivity() {
        Intent intent = new Intent(ExamActivity.this, ExamEditActivity.class);
        intent.putExtra(IntentParam.IS_CREATE, true);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        startActivityForResult(intent, REQUEST_CODE_CREATE_EXAM);
    }

    private void enterEditExamActivity(ExamInfo examInfo) {
        Intent intent = new Intent(ExamActivity.this, ExamEditActivity.class);
        intent.putExtra(IntentParam.IS_CREATE, false);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        intent.putExtra(IntentParam.EXAM_INFO, examInfo);
        startActivityForResult(intent, REQUEST_CODE_EDIT_EXAM);
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ExamService.FIND_ALL_EXAM_MSG) {
                handleFindAllExamMsg(msg);
            } else if (msg.what == ExamService.GET_EXAM_MSG) {
                handleGetExamMsg(msg);
            }
        }
    };

    private void handleFindAllExamMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            examSimpleInfos.clear();
            examInfoCache.clear();
            examSimpleInfos.addAll(serviceResult.extra(ExamFindAllResponse.class).getExamSimpleInfoList());
            swipeRefreshLayout.setRefreshing(false);
            recyclerViewAdapter.notifyDataSetChanged();
        }
    }

    private void handleGetExamMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            ExamInfo examInfo = serviceResult.extra(ExamGetResponse.class).getExamInfo();
            examInfoCache.put(examInfo.getExamId(), examInfo);
            int needReplaceIndex = -1;
            for (int i = 0; i < examSimpleInfos.size(); i++) {
                if (examSimpleInfos.get(i).getExamId() == examInfo.getExamId()) {
                    needReplaceIndex = i;
                }
            }
            if (needReplaceIndex >= 0) {
                examSimpleInfos.set(needReplaceIndex, ProtoUtils.simplifyExamInfo(examInfo));
                recyclerViewAdapter.notifyItemChanged(needReplaceIndex);
            }
            if (startEditActivity) {
                startEditActivity = false;
                enterEditExamActivity(examInfo);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        ExamInfo examInfo = (ExamInfo) data.getSerializableExtra(IntentParam.EXAM_INFO);
        if (examInfo == null) {
            return;
        }
        if (requestCode == REQUEST_CODE_CREATE_EXAM) {
            examSimpleInfos.add(ProtoUtils.simplifyExamInfo(examInfo));
            examInfoCache.put(examInfo.getExamId(), examInfo);
            recyclerViewAdapter.notifyDataSetChanged();
        } else if (requestCode == REQUEST_CODE_EDIT_EXAM){
            boolean deleteExam = data.getBooleanExtra(IntentParam.DELETE_EXAM, false);
            int needModifyIndex = -1;
            for (int i = 0; i < examSimpleInfos.size(); i++) {
                if (examSimpleInfos.get(i).getExamId() == examInfo.getExamId()) {
                    needModifyIndex = i;
                    break;
                }
            }
            if (needModifyIndex >= 0) {
                if (deleteExam) {
                    examSimpleInfos.remove(needModifyIndex);
                    examInfoCache.remove(examInfo.getExamId());
                    recyclerViewAdapter.notifyDataSetChanged();
                } else {
                    examSimpleInfos.set(needModifyIndex, ProtoUtils.simplifyExamInfo(examInfo));
                    examInfoCache.put(examInfo.getExamId(), examInfo);
                    recyclerViewAdapter.notifyItemChanged(needModifyIndex);
                }
            }
        }
    }

    @Override
    public void onItemClick(ViewHolder viewHolder) {
        final ExamSimpleInfo examSimpleInfo = viewHolder.getExamSimpleInfo();
        ExamInfo examInfo = examInfoCache.get(examSimpleInfo.getExamId());
        if (examInfo != null) {
            enterEditExamActivity(examInfo);
        } else {
            startEditActivity = true;
            ExamService.getInstance().getExam(handler, examSimpleInfo.getExamId());
        }
    }

    @Override
    public void onSubmitViewClick(ViewHolder viewHolder) {
        final ExamSimpleInfo examSimpleInfo = viewHolder.getExamSimpleInfo();
        // TODO 跳转
    }
}
