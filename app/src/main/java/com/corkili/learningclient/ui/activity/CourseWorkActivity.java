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
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.ProtoUtils;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkSimpleInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.CourseWorkFindAllResponse;
import com.corkili.learningclient.generate.protobuf.Response.CourseWorkGetResponse;
import com.corkili.learningclient.service.CourseWorkService;
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

public class CourseWorkActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CREATE_COURSE_WORK = 0xF1;
    private static final int REQUEST_CODE_EDIT_COURSE_WORK = 0xF2;

    private QMUITopBarLayout topBar;
    private QMUIGroupListView courseWorkListView;

    private CourseInfo courseInfo;
    private UserInfo userInfo;

    private List<CourseWorkSimpleInfo> courseWorkSimpleInfos;
    private Map<Long, CourseWorkInfo> courseWorkInfoCache;

    private boolean startEditActivity;
    private boolean startDetailActivity;
    private boolean startSubmitActivity;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_work);
        courseInfo = (CourseInfo) getIntent().getSerializableExtra(IntentParam.COURSE_INFO);
        userInfo = (UserInfo) getIntent().getSerializableExtra(IntentParam.USER_INFO);
        if (userInfo == null || courseInfo == null) {
            throw new RuntimeException("Intent param lost");
        }

        topBar = findViewById(R.id.topbar);
        topBar.setTitle(userInfo.getUserType() == UserType.Teacher ? "作业管理" : "我的作业");
        topBar.addLeftBackImageButton().setOnClickListener(v -> CourseWorkActivity.this.finish());
        topBar.addRightImageButton(R.drawable.ic_refresh_24dp, R.id.topbar_right_refresh)
                .setOnClickListener(v -> refreshCourseWorkSimpleInfos());
        if (userInfo.getUserType() == UserType.Teacher) {
            topBar.addRightImageButton(R.drawable.ic_add_24dp, R.id.topbar_right_add)
                    .setOnClickListener(v -> enterAddCourseWorkActivity());
        }

        startEditActivity = false;
        startDetailActivity = false;
        startSubmitActivity = false;
        
        courseWorkListView = findViewById(R.id.course_work_list);
        courseWorkSimpleInfos = new ArrayList<>();
        courseWorkInfoCache = new ConcurrentHashMap<>();
        refreshCourseWorkSimpleInfos();
    }

    private void refreshCourseWorkSimpleInfos() {
        CourseWorkService.getInstance().findAllCourseWork(handler, courseInfo.getCourseId());
    }

    private void enterAddCourseWorkActivity() {
        Intent intent = new Intent(CourseWorkActivity.this, CourseWorkEditActivity.class);
        intent.putExtra(IntentParam.IS_CREATE, true);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        startActivityForResult(intent, REQUEST_CODE_CREATE_COURSE_WORK);
    }

    private void enterEditCourseWorkActivity(CourseWorkInfo courseWorkInfo) {
        if (userInfo.getUserType() == UserType.Teacher) {
            Intent intent = new Intent(CourseWorkActivity.this, CourseWorkEditActivity.class);
            intent.putExtra(IntentParam.IS_CREATE, false);
            intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
            intent.putExtra(IntentParam.COURSE_WORK_INFO, courseWorkInfo);
            startActivityForResult(intent, REQUEST_CODE_EDIT_COURSE_WORK);
        }
    }

    private void enterCourseWorkDetailActivity(CourseWorkInfo courseWorkInfo) {
        if (userInfo.getUserType() == UserType.Student) {
            Intent intent = new Intent(CourseWorkActivity.this, CourseWorkDetailActivity.class);
            intent.putExtra(IntentParam.USER_INFO, userInfo);
            intent.putExtra(IntentParam.COURSE_WORK_INFO, courseWorkInfo);
            startActivity(intent);
        }
    }

    private void enterSubmittedCourseWorkActivity(CourseWorkInfo courseWorkInfo) {
        if (userInfo.getUserType() == UserType.Teacher) {
            Intent intent = new Intent(CourseWorkActivity.this, SubmittedCourseWorkActivity.class);
            intent.putExtra(IntentParam.USER_INFO, userInfo);
            intent.putExtra(IntentParam.COURSE_WORK_INFO, courseWorkInfo);
            startActivity(intent);
        }
    }
    
    private void refreshListView() {
        courseWorkListView.removeAllViews();
        int size = QMUIDisplayHelper.dp2px(this, 24);
        Section section = QMUIGroupListView.newSection(this)
                .setLeftIconSize(size, ViewGroup.LayoutParams.WRAP_CONTENT);
        for (CourseWorkSimpleInfo courseWorkSimpleInfo : courseWorkSimpleInfos) {
            Drawable drawable;
            if (userInfo.getUserType() == UserType.Teacher) {
                if (courseWorkSimpleInfo.getOpen()) {
                    if (courseWorkSimpleInfo.getHasDeadline() && courseWorkSimpleInfo.getDeadline() <= System.currentTimeMillis()) {
                        drawable = ContextCompat.getDrawable(this, R.drawable.ic_submit_red_24dp);
                    } else {
                        drawable = ContextCompat.getDrawable(this, R.drawable.ic_submit_green_24dp);
                    }
                } else {
                    drawable = ContextCompat.getDrawable(this, R.drawable.ic_submit_black_24dp);
                }
            } else {
                if (courseWorkSimpleInfo.getOpen()) {
                    if (courseWorkSimpleInfo.getHasDeadline() && courseWorkSimpleInfo.getDeadline() <= System.currentTimeMillis()) {
                        drawable = ContextCompat.getDrawable(this, R.drawable.ic_submit_red_24dp);
                    } else {
                        drawable = ContextCompat.getDrawable(this, R.drawable.ic_submit_green_24dp);
                    }
                } else {
                    drawable = ContextCompat.getDrawable(this, R.drawable.ic_submit_black_24dp);
                }
            }
            
            QMUICommonListItemView itemView = courseWorkListView.createItemView(
                    drawable,
                    courseWorkSimpleInfo.getCourseWorkName(),
                    courseWorkSimpleInfo.getHasDeadline()
                            ? IUtils.DATE_FORMATTER.format(new Date(courseWorkSimpleInfo.getDeadline()))
                            : "无限期",
                    QMUICommonListItemView.HORIZONTAL,
                    QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);

            section.addItemView(itemView, v -> onItemClick(courseWorkSimpleInfo));
        }
        String title;
        if (courseWorkSimpleInfos.isEmpty()) {
            title = "当前没有作业";
        } else {
            title = IUtils.format("共有{}个作业", courseWorkSimpleInfos.size());
        }
        section.setTitle(title);
        section.addTo(courseWorkListView);
    }

    private void onItemClick(CourseWorkSimpleInfo courseWorkSimpleInfo) {
        CourseWorkInfo courseWorkInfo = courseWorkInfoCache.get(courseWorkSimpleInfo.getCourseWorkId());
        if (userInfo.getUserType() == UserType.Teacher) {
            BottomListSheetBuilder builder = new QMUIBottomSheet.BottomListSheetBuilder(this)
                    .addItem("编辑作业");
            if (courseWorkSimpleInfo.getOpen()) {
                builder.addItem("查看已提交作业");
            }
            builder.setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                dialog.dismiss();
                if (position == 0) {
                    if (courseWorkInfo != null) {
                        enterEditCourseWorkActivity(courseWorkInfo);
                    } else {
                        startEditActivity = true;
                        CourseWorkService.getInstance().getCourseWork(handler, courseWorkSimpleInfo.getCourseWorkId());
                    }
                } else if (position == 1) {
                    if (courseWorkInfo != null) {
                        enterSubmittedCourseWorkActivity(courseWorkInfo);
                    } else {
                        startSubmitActivity = true;
                        CourseWorkService.getInstance().getCourseWork(handler, courseWorkSimpleInfo.getCourseWorkId());
                    }
                }
            });
            builder.build().show();
        } else {
            if (courseWorkInfo != null) {
                enterCourseWorkDetailActivity(courseWorkInfo);
            } else {
                startDetailActivity = true;
                CourseWorkService.getInstance().getCourseWork(handler, courseWorkSimpleInfo.getCourseWorkId());
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CourseWorkService.FIND_ALL_COURSE_WORK_MSG) {
                handleFindAllCourseWorkMsg(msg);
            } else if (msg.what == CourseWorkService.GET_COURSE_WORK_MSG) {
                handleGetCourseWorkMsg(msg);
            }
        }
    };

    private void handleFindAllCourseWorkMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            courseWorkSimpleInfos.clear();
            courseWorkInfoCache.clear();
            if (userInfo.getUserType() == UserType.Teacher) {
                courseWorkSimpleInfos.addAll(serviceResult.extra(CourseWorkFindAllResponse.class).getCourseWorkSimpleInfoList());
            } else {
                for (CourseWorkSimpleInfo courseWorkSimpleInfo : serviceResult.extra(CourseWorkFindAllResponse.class).getCourseWorkSimpleInfoList()) {
                    if (courseWorkSimpleInfo.getOpen()) {
                        courseWorkSimpleInfos.add(courseWorkSimpleInfo);
                    }
                }
            }
            refreshListView();
        }
    }

    private void handleGetCourseWorkMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            CourseWorkInfo courseWorkInfo = serviceResult.extra(CourseWorkGetResponse.class).getCourseWorkInfo();
            courseWorkInfoCache.put(courseWorkInfo.getCourseWorkId(), courseWorkInfo);
            int needReplaceIndex = -1;
            for (int i = 0; i < courseWorkSimpleInfos.size(); i++) {
                if (courseWorkSimpleInfos.get(i).getCourseWorkId() == courseWorkInfo.getCourseWorkId()) {
                    needReplaceIndex = i;
                }
            }
            if (needReplaceIndex >= 0) {
                courseWorkSimpleInfos.set(needReplaceIndex, ProtoUtils.simplifyCourseWorkInfo(courseWorkInfo));
                refreshListView();
            }
            if (startEditActivity) {
                startEditActivity = false;
                enterEditCourseWorkActivity(courseWorkInfo);
            }
            if (startDetailActivity) {
                startDetailActivity = false;
                enterCourseWorkDetailActivity(courseWorkInfo);
            }
            if (startSubmitActivity) {
                startSubmitActivity = false;
                enterSubmittedCourseWorkActivity(courseWorkInfo);
            }
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
        CourseWorkInfo courseWorkInfo = (CourseWorkInfo) data.getSerializableExtra(IntentParam.COURSE_WORK_INFO);
        if (courseWorkInfo == null) {
            return;
        }
        if (requestCode == REQUEST_CODE_CREATE_COURSE_WORK) {
            courseWorkSimpleInfos.add(ProtoUtils.simplifyCourseWorkInfo(courseWorkInfo));
            courseWorkInfoCache.put(courseWorkInfo.getCourseWorkId(), courseWorkInfo);
            refreshListView();
        } else if (requestCode == REQUEST_CODE_EDIT_COURSE_WORK){
            boolean deleteCourseWork = data.getBooleanExtra(IntentParam.DELETE_COURSE_WORK, false);
            int needModifyIndex = -1;
            for (int i = 0; i < courseWorkSimpleInfos.size(); i++) {
                if (courseWorkSimpleInfos.get(i).getCourseWorkId() == courseWorkInfo.getCourseWorkId()) {
                    needModifyIndex = i;
                    break;
                }
            }
            if (needModifyIndex >= 0) {
                if (deleteCourseWork) {
                    courseWorkSimpleInfos.remove(needModifyIndex);
                    courseWorkInfoCache.remove(courseWorkInfo.getCourseWorkId());
                    refreshListView();
                } else {
                    courseWorkSimpleInfos.set(needModifyIndex, ProtoUtils.simplifyCourseWorkInfo(courseWorkInfo));
                    courseWorkInfoCache.put(courseWorkInfo.getCourseWorkId(), courseWorkInfo);
                    refreshListView();
                }
            }
        }
    }

}
