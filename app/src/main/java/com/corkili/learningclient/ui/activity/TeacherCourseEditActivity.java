package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.UIHelper;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Response.CourseCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.CourseUpdateResponse;
import com.corkili.learningclient.service.CourseService;
import com.corkili.learningclient.service.ServiceResult;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;

import java.util.List;
import java.util.regex.Pattern;

public class TeacherCourseEditActivity extends AppCompatActivity {

    private QMUITopBarLayout topBar;
    private EditText courseNameEditText;
    private EditText tagsEditText;
    private RadioGroup openRadioGroup;
    private EditText descriptionEditText;

    private boolean isCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_course_edit);

        isCreate = getIntent().getBooleanExtra(IntentParam.IS_CREATE, true);

        topBar = findViewById(R.id.topbar);
        courseNameEditText = findViewById(R.id.course_edit_text_edit_course_name);
        tagsEditText = findViewById(R.id.course_edit_text_edit_tags);
        openRadioGroup = findViewById(R.id.course_edit_radio_group_open);
        descriptionEditText = findViewById(R.id.course_edit_text_edit_description);

        if (isCreate) {
            topBar.setTitle("创建课程");
        } else {
            topBar.setTitle("编辑课程");
        }

        CourseInfo courseInfo = (CourseInfo) getIntent().getSerializableExtra(IntentParam.COURSE_INFO);

        if (!isCreate) {
            if (courseInfo == null) {
                throw new RuntimeException("expected courseInfo");
            }
            courseNameEditText.setText(courseInfo.getCourseName());
            tagsEditText.setText(IUtils.list2String(courseInfo.getTagList(), ";"));
            if (courseInfo.getOpen()) {
                openRadioGroup.check(R.id.open_yes);
            } else {
                openRadioGroup.check(R.id.open_no);
            }
            descriptionEditText.setText(courseInfo.getDescription());
        }

        topBar.addRightTextButton("保存", R.id.topbar_right_save).setOnClickListener(v -> {
            String courseName = courseNameEditText.getText().toString().trim();
            List<String> tags = IUtils.string2List(tagsEditText.getText().toString().trim(), Pattern.compile(";"));
            boolean open = openRadioGroup.getCheckedRadioButtonId() == R.id.open_yes;
            String description = descriptionEditText.getText().toString().trim();
            if (isCreate) {
                CourseService.getInstance().createCourse(handler, courseName, tags, open, description);
            } else {
                boolean updateTags = false;
                if (tags.size() != courseInfo.getTagCount()) {
                    updateTags = true;
                } else {
                    for (String tag : tags) {
                        if (!courseInfo.getTagList().contains(tag)) {
                            updateTags = true;
                        }
                    }
                }
                CourseService.getInstance().updateCourse(handler, courseInfo.getCourseId(),
                        courseName.equals(courseInfo.getCourseName()) ? null : courseName,
                        updateTags ? tags : null,
                        open == courseInfo.getOpen() ? null : open,
                        description.equals(courseInfo.getDescription()) ? null : description);
            }
        });

        topBar.addLeftBackImageButton().setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            TeacherCourseEditActivity.this.finish();
        });
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CourseService.CREATE_COURSE_MSG) {
                handleCreateCourseMsg(msg);
            } else if (msg.what == CourseService.UPDATE_COURSE_MSG) {
                handleUpdateCourseMsg(msg);
            }
        }
    };

    private void handleCreateCourseMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        UIHelper.toast(this, serviceResult, raw -> serviceResult.isSuccess() ? "创建课程成功" : "创建课程失败");
        if (serviceResult.isSuccess()) {
            Intent intent = new Intent();
            intent.putExtra(IntentParam.COURSE_INFO, serviceResult.extra(CourseCreateResponse.class).getCourseInfo());
            setResult(RESULT_OK, intent);
            this.finish();
        }
    }

    private void handleUpdateCourseMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        UIHelper.toast(this, serviceResult, raw -> serviceResult.isSuccess() ? "更新课程成功" : "更新课程失败");
        if (serviceResult.isSuccess()) {
            Intent intent = new Intent();
            intent.putExtra(IntentParam.COURSE_INFO, serviceResult.extra(CourseUpdateResponse.class).getCourseInfo());
            setResult(RESULT_OK, intent);
            this.finish();
        }
    }
}
