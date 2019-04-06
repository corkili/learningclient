package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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

public class StudentCourseDetailActivity extends AppCompatActivity {

    private UserInfo userInfo;
    private CourseInfo courseInfo;
    private CourseSubscriptionInfo courseSubscriptionInfo;

    private TextView courseNameView;
    private TextView teacherView;
    private TextView tagsView;
    private TextView descriptionView;
    private Button studyScormButton;
    private Button subscribeButton;
    private View scormLayout;
    
    private Menu menu;

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
        courseNameView = findViewById(R.id.course_detail_text_edit_course_name);
        teacherView = findViewById(R.id.course_detail_text_edit_teacher);
        tagsView = findViewById(R.id.course_detail_text_edit_tags);
        descriptionView = findViewById(R.id.course_detail_text_edit_description);
        studyScormButton = findViewById(R.id.course_detail_button_study_scorm);
        subscribeButton = findViewById(R.id.course_detail_button_subscribe);
        scormLayout = findViewById(R.id.scorm_layout);

        subscribeButton.setOnClickListener(v -> {
            subscribeButton.setEnabled(false);
            if (courseSubscriptionInfo == null) {
                CourseSubscriptionService.getInstance().createCourseSubscription(
                        handler, courseInfo.getCourseId());
            } else {
                CourseSubscriptionService.getInstance().deleteCourseSubscription(
                        handler, courseSubscriptionInfo.getCourseSubscriptionId());
            }
        });

        teacherView.setOnClickListener(v -> {
            if (courseSubscriptionInfo == null) {
                return;
            }
            Intent intent = new Intent(StudentCourseDetailActivity.this, MessageActivity.class);
            intent.putExtra(IntentParam.USER_INFO, courseInfo.getTeacherInfo());
            intent.putExtra(IntentParam.SELF_USER_INFO, userInfo);
            intent.putExtra(IntentParam.COUNT, 0);
            startActivity(intent);
        });

        studyScormButton.setOnClickListener(v -> {
            Intent intent = new Intent(StudentCourseDetailActivity.this, ScormActivity.class);
            intent.putExtra(IntentParam.USER_INFO, userInfo);
            intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
            startActivity(intent);
        });

        refreshView();
    }

    private void refreshView() {
        courseNameView.setText(courseInfo.getCourseName());
        if (courseSubscriptionInfo == null) {
            teacherView.setText(courseInfo.getTeacherInfo().getUsername());
        } else {
            teacherView.setText(IUtils.format(
                    "{}(点击联系)", courseInfo.getTeacherInfo().getUsername()));
        }
        tagsView.setText(IUtils.list2String(courseInfo.getTagList(), ";"));
        descriptionView.setText(courseInfo.getDescription());
        if (courseInfo.getOpen()) {
            if (courseSubscriptionInfo == null) {
                subscribeButton.setText("订阅课程");
            } else {
                subscribeButton.setText("取消订阅");
            }
            subscribeButton.setEnabled(true);
        } else {
            subscribeButton.setVisibility(View.GONE);
            subscribeButton.setEnabled(false);
        }
        if (courseSubscriptionInfo != null) {
            if (courseInfo.getHasCourseware()) {
                studyScormButton.setText("学习课件");
                studyScormButton.setEnabled(true);
            } else {
                studyScormButton.setText("尚未上传课件");
                studyScormButton.setEnabled(false);
            }
        } else {
            scormLayout.setVisibility(View.GONE);
        }

    }

    private void refreshMenu() {
        if (courseSubscriptionInfo == null) {
            menu.findItem(R.id.menu_item_course_comment).setVisible(true);
            menu.findItem(R.id.menu_item_course_forum).setVisible(false);
            menu.findItem(R.id.menu_item_course_work).setVisible(false);
            menu.findItem(R.id.menu_item_course_exam).setVisible(false);
        } else {
            menu.findItem(R.id.menu_item_course_comment).setVisible(true);
            menu.findItem(R.id.menu_item_course_forum).setVisible(true);
            menu.findItem(R.id.menu_item_course_work).setVisible(true);
            menu.findItem(R.id.menu_item_course_exam).setVisible(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.student_course_detail_menu, menu);
        this.menu = menu;
        refreshMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_course_comment:
                selectCourseCommentItem();
                break;
            case R.id.menu_item_course_forum:
                selectCourseForumItem();
                break;
            case R.id.menu_item_course_work:
                selectCourseWorkItem();
                break;
            case R.id.menu_item_course_exam:
                selectExamItem();
                break;
        }
        return true;
    }

    private void selectCourseCommentItem() {
        Intent intent = new Intent(StudentCourseDetailActivity.this, CourseCommentActivity.class);
        intent.putExtra(IntentParam.USER_TYPE, UserType.Student);
        intent.putExtra(IntentParam.ALREADY_SUBSCRIBE, courseSubscriptionInfo != null);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        startActivity(intent);
    }

    private void selectCourseForumItem() {
        Intent intent = new Intent(StudentCourseDetailActivity.this, ForumActivity.class);
        intent.putExtra(IntentParam.USER_INFO, userInfo);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        startActivity(intent);
    }

    private void selectCourseWorkItem() {
        Intent intent = new Intent(StudentCourseDetailActivity.this, CourseWorkActivity.class);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        intent.putExtra(IntentParam.USER_INFO, userInfo);
        startActivity(intent);
    }

    private void selectExamItem() {
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
        subscribeButton.setEnabled(true);
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            courseSubscriptionInfo = serviceResult.extra(CourseSubscriptionCreateResponse.class)
                    .getCourseSubscriptionInfo();
            refreshMenu();
            refreshView();
        }
    }

    private void handleDeleteCourseSubscriptionMsg(Message msg) {
        subscribeButton.setEnabled(true);
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            courseSubscriptionInfo = null;
            refreshMenu();
            refreshView();
        }
    }
}
