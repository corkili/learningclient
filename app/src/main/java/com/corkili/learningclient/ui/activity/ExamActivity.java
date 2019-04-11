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
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.ExamInfo;
import com.corkili.learningclient.generate.protobuf.Info.ExamSimpleInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.ExamFindAllResponse;
import com.corkili.learningclient.generate.protobuf.Response.ExamGetResponse;
import com.corkili.learningclient.service.ExamService;
import com.corkili.learningclient.service.ServiceResult;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet.BottomListSheetBuilder;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView.Section;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExamActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CREATE_EXAM = 0xF1;
    private static final int REQUEST_CODE_EDIT_EXAM = 0xF2;

    private QMUITopBarLayout topBar;
    private QMUIGroupListView examListView;

    private CourseInfo courseInfo;
    private UserInfo userInfo;

    private List<ExamSimpleInfo> examSimpleInfos;
    private Map<Long, ExamInfo> examInfoCache;

    private boolean startEditActivity;
    private boolean startDetailActivity;
    private boolean startSubmitActivity;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam);
        courseInfo = (CourseInfo) getIntent().getSerializableExtra(IntentParam.COURSE_INFO);
        userInfo = (UserInfo) getIntent().getSerializableExtra(IntentParam.USER_INFO);
        if (userInfo == null || courseInfo == null) {
            throw new RuntimeException("Intent param expected");
        }

        topBar = findViewById(R.id.topbar);
        topBar.setTitle(userInfo.getUserType() == UserType.Teacher ? "考试管理" : "我的考试");
        topBar.addLeftBackImageButton().setOnClickListener(v -> ExamActivity.this.finish());
        topBar.addRightImageButton(R.drawable.ic_refresh_24dp, R.id.topbar_right_refresh)
                .setOnClickListener(v -> refreshExamSimpleInfos());
        if (userInfo.getUserType() == UserType.Teacher) {
            topBar.addRightImageButton(R.drawable.ic_add_24dp, R.id.topbar_right_add)
                    .setOnClickListener(v -> enterAddExamActivity());
        }
        
        startEditActivity = false;
        startDetailActivity = false;
        startSubmitActivity = false;

        examListView = findViewById(R.id.exam_list);
        examSimpleInfos = new ArrayList<>();
        examInfoCache = new ConcurrentHashMap<>();
        
        refreshListView();        
        
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
        if (userInfo.getUserType() == UserType.Teacher) {
            Intent intent = new Intent(ExamActivity.this, ExamEditActivity.class);
            intent.putExtra(IntentParam.IS_CREATE, false);
            intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
            intent.putExtra(IntentParam.EXAM_INFO, examInfo);
            startActivityForResult(intent, REQUEST_CODE_EDIT_EXAM);
        }
    }

    private void enterExamDetailActivity(ExamInfo examInfo) {
        if (userInfo.getUserType() == UserType.Student) {
            Intent intent = new Intent(ExamActivity.this, ExamDetailActivity.class);
            intent.putExtra(IntentParam.USER_INFO, userInfo);
            intent.putExtra(IntentParam.EXAM_INFO, examInfo);
            startActivity(intent);
        }
    }

    private void enterSubmittedExamActivity(ExamInfo examInfo) {
        if (userInfo.getUserType() == UserType.Teacher) {
            Intent intent = new Intent(ExamActivity.this, SubmittedExamActivity.class);
            intent.putExtra(IntentParam.USER_INFO, userInfo);
            intent.putExtra(IntentParam.EXAM_INFO, examInfo);
            startActivity(intent);
        }
    }
    
    private void refreshListView() {
        examListView.removeAllViews();
        int size = QMUIDisplayHelper.dp2px(this, 24);
        Section section = QMUIGroupListView.newSection(this)
                .setLeftIconSize(size, ViewGroup.LayoutParams.WRAP_CONTENT);
        for (ExamSimpleInfo examSimpleInfo : examSimpleInfos) {
            Drawable drawable;
            if (examSimpleInfo.getStartTime() <= System.currentTimeMillis()) {
                if (examSimpleInfo.getEndTime() <= System.currentTimeMillis()) {
                    drawable = ContextCompat.getDrawable(this, R.drawable.ic_submit_red_24dp);
                } else {
                    drawable = ContextCompat.getDrawable(this, R.drawable.ic_submit_green_24dp);
                }
            } else {
                drawable = ContextCompat.getDrawable(this, R.drawable.ic_submit_black_24dp);
            }

            QMUICommonListItemView itemView = examListView.createItemView(
                    drawable,
                    examSimpleInfo.getExamName(),
                    IUtils.format("开始：{}\n结束：{}",
                            IUtils.DATE_TIME_FORMATTER.format(new Date(examSimpleInfo.getStartTime())), 
                            IUtils.DATE_TIME_FORMATTER.format(new Date(examSimpleInfo.getEndTime()))),
                    QMUICommonListItemView.HORIZONTAL,
                    QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);

            section.addItemView(itemView, v -> onItemClick(examSimpleInfo));
        }
        String title;
        if (examSimpleInfos.isEmpty()) {
            title = "当前没有考试";
        } else {
            title = IUtils.format("共有{}个考试", examSimpleInfos.size());
        }
        section.setTitle(title);
        section.addTo(examListView);
    }

    private void onItemClick(ExamSimpleInfo examSimpleInfo) {
        ExamInfo examInfo = examInfoCache.get(examSimpleInfo.getExamId());
        if (userInfo.getUserType() == UserType.Teacher) {
            BottomListSheetBuilder builder = new QMUIBottomSheet.BottomListSheetBuilder(this)
                    .addItem("编辑考试");
            if (examSimpleInfo.getStartTime() <= System.currentTimeMillis()) {
                builder.addItem("查看已提交考试");
            }
            builder.setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                dialog.dismiss();
                if (position == 0) {
                    if (examInfo != null) {
                        enterEditExamActivity(examInfo);
                    } else {
                        startEditActivity = true;
                        ExamService.getInstance().getExam(handler, examSimpleInfo.getExamId());
                    }
                } else if (position == 1) {
                    if (examInfo != null) {
                        enterSubmittedExamActivity(examInfo);
                    } else {
                        startSubmitActivity = true;
                        ExamService.getInstance().getExam(handler, examSimpleInfo.getExamId());
                    }
                }
            });
            builder.build().show();
        } else {
            if (examSimpleInfo.getStartTime() <= System.currentTimeMillis()) {
                if (examInfo != null) {
                    enterExamDetailActivity(examInfo);
                } else {
                    startDetailActivity = true;
                    ExamService.getInstance().getExam(handler, examSimpleInfo.getExamId());
                }
            } else {
                UIHelper.toast(this, "考试尚未开始");
            }
        }
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
        if (serviceResult.isSuccess()) {
            examSimpleInfos.clear();
            examInfoCache.clear();
            examSimpleInfos.addAll(serviceResult.extra(ExamFindAllResponse.class).getExamSimpleInfoList());
            refreshListView();
        } else {
            UIHelper.toast(this, serviceResult, raw -> "加载考试信息失败");
        }
    }

    private void handleGetExamMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
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
                refreshListView();
            }
            if (startEditActivity) {
                startEditActivity = false;
                enterEditExamActivity(examInfo);
            }
            if (startDetailActivity) {
                startDetailActivity = false;
                enterExamDetailActivity(examInfo);
            }
            if (startSubmitActivity) {
                startSubmitActivity = false;
                enterSubmittedExamActivity(examInfo);
            }
        } else {
            UIHelper.toast(this, serviceResult, raw -> "获取考试信息失败");
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
        ExamInfo examInfo = (ExamInfo) data.getSerializableExtra(IntentParam.EXAM_INFO);
        if (examInfo == null) {
            return;
        }
        if (requestCode == REQUEST_CODE_CREATE_EXAM) {
            examSimpleInfos.add(ProtoUtils.simplifyExamInfo(examInfo));
            examInfoCache.put(examInfo.getExamId(), examInfo);
            refreshListView();
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
                    refreshListView();
                } else {
                    examSimpleInfos.set(needModifyIndex, ProtoUtils.simplifyExamInfo(examInfo));
                    examInfoCache.put(examInfo.getExamId(), examInfo);
                    refreshListView();
                }
            }
        }
    }

}
