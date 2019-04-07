package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseSubscriptionInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.CourseSubscriptionCreateResponse;
import com.corkili.learningclient.service.CourseSubscriptionService;
import com.corkili.learningclient.service.ServiceResult;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;

public class StudentCourseDetailActivity extends AppCompatActivity {

    private UserInfo userInfo;
    private CourseInfo courseInfo;
    private CourseSubscriptionInfo courseSubscriptionInfo;

    private QMUITopBarLayout topBar;
    private QMUIGroupListView courseInfoListView;
    private QMUICommonListItemView courseNameItem;
    private QMUICommonListItemView openItem;
    private QMUICommonListItemView teacherItem;
    private QMUICommonListItemView tagsItem;
    private QMUICommonListItemView descriptionItem;
    private QMUICommonListItemView subscriptionItem;
    private QMUICommonListItemView scormItem;

    private QMUICommonListItemView commentItem;
    private QMUICommonListItemView forumItem;
    private QMUICommonListItemView workItem;
    private QMUICommonListItemView examItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_course_detail);
        userInfo = (UserInfo) getIntent().getSerializableExtra(IntentParam.USER_INFO);
        courseInfo = (CourseInfo) getIntent().getSerializableExtra(IntentParam.COURSE_INFO);
        courseSubscriptionInfo = (CourseSubscriptionInfo) getIntent().getSerializableExtra(IntentParam.COURSE_SUBSCRIPTION_INFO);
        if (userInfo == null || courseInfo == null) {
            throw new RuntimeException("Intent param expected");
        }

        topBar = findViewById(R.id.topbar);

        topBar.setTitle("课程详情");

        topBar.addLeftBackImageButton().setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
            intent.putExtra(IntentParam.COURSE_SUBSCRIPTION_INFO, courseSubscriptionInfo);
            setResult(RESULT_OK, intent);
            StudentCourseDetailActivity.this.finish();
        });

        courseInfoListView = findViewById(R.id.course_info_list);

        int size = QMUIDisplayHelper.dp2px(this, 24);

        courseNameItem = courseInfoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_course_manage_24dp),
                "名称",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        openItem = courseInfoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_lock_24dp),
                "状态",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        teacherItem = courseInfoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_user_24dp),
                "老师",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        tagsItem = courseInfoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_tags_24dp),
                "标签",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);


        descriptionItem = courseInfoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_description_24dp),
                "描述",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        subscriptionItem = courseInfoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_subscribe_24dp),
                "订阅",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);

        scormItem = courseInfoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_scorm_24dp),
                "课件",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);

        QMUIGroupListView.newSection(this)
                .setTitle("课程基本信息")
                .setLeftIconSize(size, ViewGroup.LayoutParams.WRAP_CONTENT)
                .addItemView(courseNameItem, null)
                .addItemView(openItem, null)
                .addItemView(teacherItem, null)
                .addItemView(tagsItem, null)
                .addItemView(descriptionItem, null)
                .addItemView(subscriptionItem, null)
                .addItemView(scormItem, null)
                .addTo(courseInfoListView);

        commentItem = courseInfoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_comment_24dp),
                "课程评论",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);

        forumItem = courseInfoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_forum_24dp),
                "课程论坛",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);

        workItem = courseInfoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_coursework_24dp),
                "作业管理",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);

        examItem = courseInfoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_exam_24dp),
                "考试管理",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);

        QMUIGroupListView.newSection(this)
                .setTitle("课程拓展信息")
                .setLeftIconSize(size, ViewGroup.LayoutParams.WRAP_CONTENT)
                .addItemView(commentItem, v -> enterCourseComment())
                .addItemView(forumItem, v -> enterCourseForum())
                .addItemView(workItem, v -> enterCourseWork())
                .addItemView(examItem, v -> enterExam())
                .addTo(courseInfoListView);

        subscriptionItem.setOnClickListener(v -> {
            if (!courseInfo.getOpen()) {
                return;
            }
            subscriptionItem.setClickable(false);
            if (courseSubscriptionInfo == null) {
                CourseSubscriptionService.getInstance().createCourseSubscription(
                        handler, courseInfo.getCourseId());
            } else {
                CourseSubscriptionService.getInstance().deleteCourseSubscription(
                        handler, courseSubscriptionInfo.getCourseSubscriptionId());
            }
        });

        teacherItem.setOnClickListener(v -> {
            if (courseSubscriptionInfo == null) {
                return;
            }
            Intent intent = new Intent(StudentCourseDetailActivity.this, MessageActivity.class);
            intent.putExtra(IntentParam.USER_INFO, courseInfo.getTeacherInfo());
            intent.putExtra(IntentParam.SELF_USER_INFO, userInfo);
            intent.putExtra(IntentParam.COUNT, 0);
            startActivity(intent);
        });

        scormItem.setOnClickListener(v -> {
            if (!courseInfo.getHasCourseware()) {
                return;
            }
            Intent intent = new Intent(StudentCourseDetailActivity.this, ScormActivity.class);
            intent.putExtra(IntentParam.USER_INFO, userInfo);
            intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
            startActivity(intent);
        });

        refreshView();
    }

    private void refreshView() {
        courseNameItem.setDetailText(courseInfo.getCourseName());
        openItem.setDetailText(courseInfo.getOpen() ? "已开放订阅" : "未开放订阅");
        teacherItem.setDetailText(courseInfo.getTeacherInfo().getUsername());
        teacherItem.setAccessoryType(courseSubscriptionInfo == null ?
                QMUICommonListItemView.ACCESSORY_TYPE_NONE : QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);
        tagsItem.setDetailText(IUtils.list2String(courseInfo.getTagList(), ";"));
        descriptionItem.setDetailText(courseInfo.getDescription());
        subscriptionItem.setDetailText(courseSubscriptionInfo != null ? "已订阅（点击取消）" : "未订阅（点击订阅）");
        subscriptionItem.setVisibility(courseInfo.getOpen() ? View.VISIBLE : View.GONE);
        scormItem.setDetailText(courseInfo.getHasCourseware() ? "点击学习" : "老师未上传课件");
        scormItem.setAccessoryType(!courseInfo.getHasCourseware() ?
                QMUICommonListItemView.ACCESSORY_TYPE_NONE : QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);

        if (courseSubscriptionInfo != null) {
            scormItem.setVisibility(View.VISIBLE);
            forumItem.setVisibility(View.VISIBLE);
            workItem.setVisibility(View.VISIBLE);
            examItem.setVisibility(View.VISIBLE);
        } else {
            forumItem.setVisibility(View.GONE);
            workItem.setVisibility(View.GONE);
            examItem.setVisibility(View.GONE);
            scormItem.setVisibility(View.GONE);
        }

    }

    private void enterCourseComment() {
        Intent intent = new Intent(StudentCourseDetailActivity.this, CourseCommentActivity.class);
        intent.putExtra(IntentParam.USER_TYPE, UserType.Student);
        intent.putExtra(IntentParam.ALREADY_SUBSCRIBE, courseSubscriptionInfo != null);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        startActivity(intent);
    }

    private void enterCourseForum() {
        Intent intent = new Intent(StudentCourseDetailActivity.this, ForumActivity.class);
        intent.putExtra(IntentParam.USER_INFO, userInfo);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        startActivity(intent);
    }

    private void enterCourseWork() {
        Intent intent = new Intent(StudentCourseDetailActivity.this, CourseWorkActivity.class);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        intent.putExtra(IntentParam.USER_INFO, userInfo);
        startActivity(intent);
    }

    private void enterExam() {
        Intent intent = new Intent(StudentCourseDetailActivity.this, ExamActivity.class);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        intent.putExtra(IntentParam.USER_INFO, userInfo);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        intent.putExtra(IntentParam.COURSE_SUBSCRIPTION_INFO, courseSubscriptionInfo);
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CourseSubscriptionService.CREATE_COURSE_SUBSCRIPTION_MSG) {
                handleCreateCourseSubscriptionMsg(msg);
            } else if (msg.what == CourseSubscriptionService.DELETE_COURSE_SUBSCRIPTION_MSG) {
                handleDeleteCourseSubscriptionMsg(msg);
            }
        }
    };

    private void handleCreateCourseSubscriptionMsg(Message msg) {
        subscriptionItem.setClickable(true);
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            courseSubscriptionInfo = serviceResult.extra(CourseSubscriptionCreateResponse.class)
                    .getCourseSubscriptionInfo();
            refreshView();
        }
    }

    private void handleDeleteCourseSubscriptionMsg(Message msg) {
        subscriptionItem.setClickable(true);
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            courseSubscriptionInfo = null;
            refreshView();
        }
    }
}
