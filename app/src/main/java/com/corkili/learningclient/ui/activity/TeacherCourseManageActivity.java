package com.corkili.learningclient.ui.activity;

import android.Manifest;
import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.tu.loadingdialog.LoadingDialog;
import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.CoursewareUpdateResponse;
import com.corkili.learningclient.service.ScormService;
import com.corkili.learningclient.service.ServiceResult;

import java.io.File;

public class TeacherCourseManageActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_EDIT_COURSE = 0xF1;
    public static final int REQUEST_CODE_SELECT_FILE = 0xF2;
    public static final int REQUEST_CODE_EXTERNAL_STORAGE = 0xF3;

    private UserInfo userInfo;
    private CourseInfo courseInfo;

    private TextView courseNameView;
    private TextView tagsView;
    private ImageView openView;
    private TextView descriptionView;
    private Button updateScormButton;
    private Button previewScormButton;

    private LoadingDialog waitingDialog;

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
        updateScormButton = findViewById(R.id.button_update_scorm);
        previewScormButton = findViewById(R.id.button_preview_scorm);

        updateScormButton.setOnClickListener(v -> {
            int permission1 = ActivityCompat.checkSelfPermission(TeacherCourseManageActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int permission2 = PackageManager.PERMISSION_GRANTED;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                permission2 = ActivityCompat.checkSelfPermission(TeacherCourseManageActivity.this, permission.READ_EXTERNAL_STORAGE);
            }
            if (permission1 != PackageManager.PERMISSION_GRANTED || permission2 != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        TeacherCourseManageActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_EXTERNAL_STORAGE
                );
            }else {
                //已经申请过
                showFileChooser();
            }
        });

        waitingDialog = new LoadingDialog.Builder(this)
                .setMessage("正在上传课件...")
                .setCancelable(false)
                .setCancelOutside(false)
                .create();

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
        if (courseInfo.getHasCourseware()) {
            updateScormButton.setText("更新");
            previewScormButton.setEnabled(true);
        } else {
            updateScormButton.setText("上传");
            previewScormButton.setEnabled(false);
        }
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //包含所有类型，image/*  video/*
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "选择SCORM课件ZIP包"), REQUEST_CODE_SELECT_FILE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "请安装一个文件浏览器.", Toast.LENGTH_SHORT).show();
        }
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
        } else if (requestCode == REQUEST_CODE_SELECT_FILE) {
            Uri uri = data.getData();
            Log.e("Select-SCORM-ZIP", "文件Uri: " + uri.toString());
            try {
                String path = getPath(uri);
                Log.e("Select-SCORM-ZIP", "选择的文件路径: " + path);
                if (path == null) {
                    Toast.makeText(this, "未选择文件", Toast.LENGTH_SHORT).show();
                } else {
                    if (!path.toLowerCase().endsWith(".zip")) {
                        Toast.makeText(this, "请选择.zip类型的文件", Toast.LENGTH_SHORT).show();
                    } else {
                        waitingDialog.show();
                        ScormService.getInstance().updateScorm(handler, courseInfo.getCourseId(), false, new File(path));
                    }
                }
            } catch (Exception e){
                Toast.makeText(this, "未选择文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //获取选择文件的路径
    private String getPath(Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor;
            try {
                cursor = getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_EXTERNAL_STORAGE) {
            showFileChooser();
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ScormService.UPDATE_SCORM_MSG) {
                handleUpdateScormMsg(msg);
            }
        }
    };

    private void handleUpdateScormMsg(Message msg) {
        waitingDialog.dismiss();
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(TeacherCourseManageActivity.this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            CourseInfo courseInfo = serviceResult.extra(CoursewareUpdateResponse.class).getCourseInfo();
            if (courseInfo != null) {
                this.courseInfo = courseInfo;
                refreshViewContent();
            }
        }
    }


}
