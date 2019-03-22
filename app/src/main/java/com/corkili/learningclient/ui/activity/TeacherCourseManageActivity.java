package com.corkili.learningclient.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;

public class TeacherCourseManageActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_EDIT_COURSE = 0xF1;

    private UserInfo userInfo;
    private CourseInfo courseInfo;

    private TextView courseNameView;
    private TextView tagsView;
    private ImageView openView;
    private TextView descriptionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_course_manage);
        userInfo = (UserInfo) getIntent().getSerializableExtra(IntentParam.USER_INFO);
        courseInfo = (CourseInfo) getIntent().getSerializableExtra(IntentParam.COURSE_INFO);
        courseNameView = findViewById(R.id.course_manage_text_edit_course_name);
        tagsView = findViewById(R.id.course_manage_text_edit_tags);
        openView = findViewById(R.id.course_manage_image_view_open);
        descriptionView = findViewById(R.id.course_manage_text_edit_description);

        refreshViewContent();
    }

    private void refreshViewContent() {
        courseNameView.setText(courseInfo.getCourseName());
        tagsView.setText(IUtils.list2String(courseInfo.getTagList(), ";"));
        if (courseInfo.getOpen()) {
            openView.setImageDrawable(getResources().getDrawable(R.drawable.ic_yes_bk_green));
        } else {
            openView.setImageDrawable(getResources().getDrawable(R.drawable.ic_no_bk_red));
        }
        descriptionView.setText(courseInfo.getDescription());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.teacher_course_manage_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_course_edit:
                selectCourseEditItem();
                break;
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
            case R.id.menu_item_course_subscription:
                selectCourseSubscriptionItem();
                break;
        }
        return true;
    }

    private void selectCourseEditItem() {
        Intent intent = new Intent(TeacherCourseManageActivity.this, TeacherCourseEditActivity.class);
        intent.putExtra(IntentParam.IS_CREATE, false);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        startActivityForResult(intent, REQUEST_CODE_EDIT_COURSE);
    }

    private void selectCourseCommentItem() {
        Intent intent = new Intent(TeacherCourseManageActivity.this, CourseCommentActivity.class);
        intent.putExtra(IntentParam.USER_TYPE, UserType.Teacher);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        startActivity(intent);
    }

    private void selectCourseForumItem() {
        Intent intent = new Intent(TeacherCourseManageActivity.this, ForumActivity.class);
        intent.putExtra(IntentParam.USER_INFO, userInfo);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        startActivity(intent);
    }

    private void selectCourseWorkItem() {
        Intent intent = new Intent(TeacherCourseManageActivity.this, CourseWorkActivity.class);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        intent.putExtra(IntentParam.USER_INFO, userInfo);
        startActivity(intent);
    }

    private void selectExamItem() {
        Intent intent = new Intent(TeacherCourseManageActivity.this, ExamActivity.class);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        intent.putExtra(IntentParam.USER_INFO, userInfo);
        startActivity(intent);
    }

    private void selectCourseSubscriptionItem() {
        Intent intent = new Intent(TeacherCourseManageActivity.this, TeacherCourseSubscriptionActivity.class);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE_EDIT_COURSE) {
            CourseInfo info = (CourseInfo) data.getSerializableExtra(IntentParam.COURSE_INFO);
            if (info != null) {
                courseInfo = info;
                refreshViewContent();
            }
        }
    }
}
