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
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkInfo;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedCourseWorkInfo;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedCourseWorkSimpleInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedCourseWorkFindAllResponse;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.service.SubmittedCourseWorkService;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView.Section;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SubmittedCourseWorkActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CHECK_SUBMITTED_COURSE_WORK = 0xF1;

    private QMUITopBarLayout topBar;
    private QMUIGroupListView submitListView;
    private Section submitterSection;

    private CourseWorkInfo courseWorkInfo;
    private UserInfo userInfo;

    private List<SubmittedCourseWorkSimpleInfo> submittedCourseWorkSimpleInfos;

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

        topBar = findViewById(R.id.topbar);

        topBar.setTitle("作业提交列表");

        topBar.addLeftBackImageButton().setOnClickListener(v -> finish());

        topBar.addRightImageButton(R.drawable.ic_refresh_24dp, R.id.topbar_right_refresh)
                .setOnClickListener(v -> refreshSubmittedCourseWorkSimpleInfos());

        submitListView = findViewById(R.id.submit_list);

        submittedCourseWorkSimpleInfos = new ArrayList<>();

        QMUICommonListItemView courseWorkNameItemView = submitListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_coursework_24dp),
                "作业名称",
                courseWorkInfo.getCourseWorkName(),
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        String submitMsg;
        if (courseWorkInfo.getOpen()) {
            if (courseWorkInfo.getHasDeadline() && courseWorkInfo.getDeadline() <= System.currentTimeMillis()) {
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

        QMUICommonListItemView deadlineItemView = submitListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_timer_24dp),
                "截止日期",
                courseWorkInfo.getHasDeadline()
                        ? IUtils.DATE_FORMATTER.format(new Date(courseWorkInfo.getDeadline()))
                        : "无限期",
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        int size = QMUIDisplayHelper.dp2px(this, 24);

        QMUIGroupListView.newSection(this)
                .setTitle("作业基本信息")
                .setLeftIconSize(size, ViewGroup.LayoutParams.WRAP_CONTENT)
                .addItemView(courseWorkNameItemView, null)
                .addItemView(submitItemView, null)
                .addItemView(deadlineItemView, null)
                .addTo(submitListView);

        refreshListView();

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

    private synchronized void refreshListView() {
        if (submitterSection != null) {
            submitterSection.removeFrom(submitListView);
        }
        int size = QMUIDisplayHelper.dp2px(this, 24);

        Section section = QMUIGroupListView.newSection(this)
                .setLeftIconSize(size, ViewGroup.LayoutParams.WRAP_CONTENT);
        for (SubmittedCourseWorkSimpleInfo submittedCourseWorkSimpleInfo : submittedCourseWorkSimpleInfos) {
            Drawable drawable;
            if (submittedCourseWorkSimpleInfo.getAlreadyCheckAllAnswer()) {
                drawable = ContextCompat.getDrawable(this, R.drawable.ic_finish_24dp);
            } else {
                drawable = ContextCompat.getDrawable(this, R.drawable.ic_notfinish_24dp);
            }

            QMUICommonListItemView itemView = submitListView.createItemView(
                    drawable,
                    submittedCourseWorkSimpleInfo.getSubmitterInfo().getUsername(),
                    IUtils.DATE_TIME_FORMATTER.format(new Date(submittedCourseWorkSimpleInfo.getUpdateTime())),
                    QMUICommonListItemView.HORIZONTAL,
                    QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);

            section.addItemView(itemView, v -> onItemClick(submittedCourseWorkSimpleInfo));
        }
        String title;
        if (submittedCourseWorkSimpleInfos.isEmpty()) {
            title = "没有提交的作业";
        } else {
            title = IUtils.format("共有{}份已提交作业", submittedCourseWorkSimpleInfos.size());
        }
        section.setTitle(title);
        this.submitterSection = section;
        submitterSection.addTo(submitListView);
    }

    private void onItemClick(SubmittedCourseWorkSimpleInfo submittedCourseWorkSimpleInfo) {
        if (submittedCourseWorkSimpleInfo != null) {
            enterCourseWorkDetailActivity(submittedCourseWorkSimpleInfo);
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
        if (serviceResult.isSuccess()) {
            submittedCourseWorkSimpleInfos.clear();
            for (SubmittedCourseWorkSimpleInfo submittedCourseWorkSimpleInfo : serviceResult.extra(SubmittedCourseWorkFindAllResponse.class).getSubmittedCourseWorkSimpleInfoList()) {
                if (submittedCourseWorkSimpleInfo.getFinished()) {
                    submittedCourseWorkSimpleInfos.add(submittedCourseWorkSimpleInfo);
                }
            }
            refreshListView();
        } else {
            UIHelper.toast(this, serviceResult, raw -> "加载已提交的作业失败");
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
            } else {
                submittedCourseWorkSimpleInfos.add(ProtoUtils.simplifySubmittedCourseWorkInfo(submittedCourseWorkInfo));
            }
            refreshListView();
        }
    }

}
