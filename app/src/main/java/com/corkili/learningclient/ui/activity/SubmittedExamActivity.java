package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.ProtoUtils;
import com.corkili.learningclient.common.UIHelper;
import com.corkili.learningclient.generate.protobuf.Info.ExamInfo;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedExamInfo;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedExamSimpleInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedExamFindAllResponse;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.service.SubmittedExamService;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView.Section;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SubmittedExamActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CHECK_SUBMITTED_EXAM = 0xF1;

    private QMUITopBarLayout topBar;
    private QMUIGroupListView submitListView;
    private Section submitterSection;

    private ExamInfo examInfo;
    private UserInfo userInfo;

    private List<SubmittedExamSimpleInfo> submittedExamSimpleInfos;
    
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

        topBar = findViewById(R.id.topbar);

        topBar.setTitle("考试提交列表");

        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());

        topBar.addRightImageButton(R.drawable.ic_refresh_24dp, R.id.topbar_right_refresh)
                .setOnClickListener(v -> refreshSubmittedExamSimpleInfos());

        submitListView = findViewById(R.id.submit_list);
        
        submittedExamSimpleInfos = new ArrayList<>();

        QMUICommonListItemView examNameItemView = submitListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_coursework_24dp),
                "考试名称",
                examInfo.getExamName(),
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        String submitMsg;
        if (examInfo.getStartTime() <= System.currentTimeMillis()) {
            if (examInfo.getEndTime() <= System.currentTimeMillis()) {
                submitMsg = "已关闭提交";
            } else {
                submitMsg = "已开放提交";
            }
        } else {
            submitMsg = "未开放提交";
        }

        QMUICommonListItemView submitItemView = submitListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_state_24dp),
                "提交状态",
                submitMsg,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        QMUICommonListItemView startTimeItemView = submitListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_timer_24dp),
                "开始时间",
                IUtils.DATE_TIME_FORMATTER.format(new Date(examInfo.getStartTime())),
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        QMUICommonListItemView endTimeItemView = submitListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_timer_24dp),
                "结束时间",
                IUtils.DATE_TIME_FORMATTER.format(new Date(examInfo.getEndTime())),
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        int size = QMUIDisplayHelper.dp2px(this, 24);

        QMUIGroupListView.newSection(this)
                .setTitle("考试基本信息")
                .setLeftIconSize(size, ViewGroup.LayoutParams.WRAP_CONTENT)
                .addItemView(examNameItemView, null)
                .addItemView(submitItemView, null)
                .addItemView(startTimeItemView, null)
                .addItemView(endTimeItemView, null)
                .addTo(submitListView);

        refreshListView();
        
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

    private synchronized void refreshListView() {
        if (submitterSection != null) {
            submitterSection.removeFrom(submitListView);
        }
        int size = QMUIDisplayHelper.dp2px(this, 24);

        Section section = QMUIGroupListView.newSection(this)
                .setLeftIconSize(size, ViewGroup.LayoutParams.WRAP_CONTENT);
        for (SubmittedExamSimpleInfo submittedExamSimpleInfo : submittedExamSimpleInfos) {
            Drawable drawable;
            if (submittedExamSimpleInfo.getAlreadyCheckAllAnswer()) {
                drawable = ContextCompat.getDrawable(this, R.drawable.ic_finish_24dp);
            } else {
                drawable = ContextCompat.getDrawable(this, R.drawable.ic_notfinish_24dp);
            }

            QMUICommonListItemView itemView = submitListView.createItemView(
                    drawable,
                    submittedExamSimpleInfo.getSubmitterInfo().getUsername(),
                    IUtils.DATE_TIME_FORMATTER.format(new Date(submittedExamSimpleInfo.getUpdateTime())),
                    QMUICommonListItemView.HORIZONTAL,
                    QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);

            section.addItemView(itemView, v -> onItemClick(submittedExamSimpleInfo));
        }
        String title;
        if (submittedExamSimpleInfos.isEmpty()) {
            title = "没有提交的试卷";
        } else {
            title = IUtils.format("共有{}份已提交试卷", submittedExamSimpleInfos.size());
        }
        section.setTitle(title);
        this.submitterSection = section;
        submitterSection.addTo(submitListView);
    }

    private void onItemClick(SubmittedExamSimpleInfo submittedExamSimpleInfo) {
        if (submittedExamSimpleInfo != null) {
            enterExamDetailActivity(submittedExamSimpleInfo);
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
        if (serviceResult.isSuccess()) {
            submittedExamSimpleInfos.clear();
            for (SubmittedExamSimpleInfo submittedExamSimpleInfo : serviceResult.extra(SubmittedExamFindAllResponse.class).getSubmittedExamSimpleInfoList()) {
                if (submittedExamSimpleInfo.getFinished()) {
                    submittedExamSimpleInfos.add(submittedExamSimpleInfo);
                }
            }
            refreshListView();
        } else {
            UIHelper.toast(this, serviceResult, raw -> "加载已提交的考试失败");
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
                refreshListView();
            } else {
                submittedExamSimpleInfos.add(ProtoUtils.simplifySubmittedExamInfo(submittedExamInfo));
                refreshListView();
            }
        }
    }
    
}
