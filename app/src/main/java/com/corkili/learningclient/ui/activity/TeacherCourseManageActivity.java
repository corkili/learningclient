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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.UIHelper;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.CoursewareUpdateResponse;
import com.corkili.learningclient.service.ScormService;
import com.corkili.learningclient.service.ServiceResult;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;

import java.io.File;

public class TeacherCourseManageActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_EDIT_COURSE = 0xF1;
    public static final int REQUEST_CODE_SELECT_FILE = 0xF2;
    public static final int REQUEST_CODE_EXTERNAL_STORAGE = 0xF3;

    private UserInfo userInfo;
    private CourseInfo courseInfo;

    private QMUITopBarLayout topBar;
    private QMUIGroupListView courseInfoListView;
    private QMUICommonListItemView courseNameItem;
    private QMUICommonListItemView tagsItem;
    private QMUICommonListItemView openItem;
    private QMUICommonListItemView descriptionItem;
    private QMUICommonListItemView scormItem;

    private QMUICommonListItemView commentItem;
    private QMUICommonListItemView forumItem;
    private QMUICommonListItemView workItem;
    private QMUICommonListItemView examItem;
    private QMUICommonListItemView subscriptionItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_course_manage);
        userInfo = (UserInfo) getIntent().getSerializableExtra(IntentParam.USER_INFO);
        courseInfo = (CourseInfo) getIntent().getSerializableExtra(IntentParam.COURSE_INFO);

        topBar = findViewById(R.id.topbar);
        courseInfoListView = findViewById(R.id.course_info_list);

        topBar.setTitle("课程详情");

        topBar.addLeftBackImageButton().setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
            setResult(RESULT_OK, intent);
            TeacherCourseManageActivity.this.finish();
        });

        topBar.addRightImageButton(R.drawable.ic_edit_24dp, R.id.topbar_right_edit).setOnClickListener(v -> {
            enterCourseEdit();
        });

        int size = QMUIDisplayHelper.dp2px(this, 24);

        courseNameItem = courseInfoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_course_manage_24dp),
                "名称",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        tagsItem = courseInfoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_tags_24dp),
                "标签",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        openItem = courseInfoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_lock_24dp),
                "状态",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        descriptionItem = courseInfoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_description_24dp),
                "描述",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

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
                .addItemView(tagsItem, null)
                .addItemView(openItem, null)
                .addItemView(descriptionItem, null)
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

        subscriptionItem = courseInfoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_subscribe_24dp),
                "订阅列表",
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
                .addItemView(subscriptionItem, v -> enterCourseSubscription())
                .addTo(courseInfoListView);

        refreshViewContent();
    }

    private void refreshViewContent() {
        courseNameItem.setDetailText(courseInfo.getCourseName());
        tagsItem.setDetailText(IUtils.list2String(courseInfo.getTagList(), ";"));
        if (courseInfo.getOpen()) {
            openItem.setDetailText("已开放");
        } else {
            openItem.setDetailText("未开放");
        }
        descriptionItem.setDetailText(courseInfo.getDescription());
        scormItem.setDetailText(courseInfo.getHasCourseware() ? "已上传课件" : "未上传课件");
        scormItem.setOnClickListener(v -> {
            QMUIBottomSheet.BottomListSheetBuilder builder = new QMUIBottomSheet
                    .BottomListSheetBuilder(TeacherCourseManageActivity.this);
            builder.addItem(courseInfo.getHasCourseware() ? "上传课件" : "更新课件");
            if (courseInfo.getHasCourseware()) {
                builder.addItem("预览课件");
            }
            builder.setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                dialog.dismiss();
                if (position == 0) {
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
                } else if (position == 1) {
                    Intent intent = new Intent(TeacherCourseManageActivity.this, ScormActivity.class);
                    intent.putExtra(IntentParam.USER_INFO, userInfo);
                    intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
                    startActivity(intent);
                }
            });
            builder.build().show();
        });
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //包含所有类型，image/*  video/*
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "选择SCORM课件ZIP包"), REQUEST_CODE_SELECT_FILE);
        } catch (android.content.ActivityNotFoundException ex) {
            UIHelper.toast(this, "请安装一个文件浏览器");
        }
    }

    private void enterCourseEdit() {
        Intent intent = new Intent(TeacherCourseManageActivity.this, TeacherCourseEditActivity.class);
        intent.putExtra(IntentParam.IS_CREATE, false);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        startActivityForResult(intent, REQUEST_CODE_EDIT_COURSE);
    }

    private void enterCourseComment() {
        Intent intent = new Intent(TeacherCourseManageActivity.this, CourseCommentActivity.class);
        intent.putExtra(IntentParam.USER_TYPE, UserType.Teacher);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        startActivity(intent);
    }

    private void enterCourseForum() {
        Intent intent = new Intent(TeacherCourseManageActivity.this, ForumActivity.class);
        intent.putExtra(IntentParam.USER_INFO, userInfo);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        startActivity(intent);
    }

    private void enterCourseWork() {
        Intent intent = new Intent(TeacherCourseManageActivity.this, CourseWorkActivity.class);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        intent.putExtra(IntentParam.USER_INFO, userInfo);
        startActivity(intent);
    }

    private void enterExam() {
        Intent intent = new Intent(TeacherCourseManageActivity.this, ExamActivity.class);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        intent.putExtra(IntentParam.USER_INFO, userInfo);
        startActivity(intent);
    }

    private void enterCourseSubscription() {
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
                    UIHelper.toast(this, "未选择文件");
                } else {
                    if (!path.toLowerCase().endsWith(".zip")) {
                        UIHelper.toast(this, "请选择.zip类型的文件");
                    } else {
                        UIHelper.showLoadingDialog(this);
                        ScormService.getInstance().updateScorm(handler, courseInfo.getCourseId(), false, new File(path));
                    }
                }
            } catch (Exception e){
                UIHelper.toast(this, "未选择文件");
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
            boolean success = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    success = false;
                    break;
                }
            }
            if (success) {
                showFileChooser();
            }
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
        UIHelper.dismissLoadingDialog();
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        UIHelper.toast(this, serviceResult, raw -> serviceResult.isSuccess() ? "上传课件成功" : "上传课件失败");
        if (serviceResult.isSuccess()) {
            CourseInfo courseInfo = serviceResult.extra(CoursewareUpdateResponse.class).getCourseInfo();
            if (courseInfo != null) {
                this.courseInfo = courseInfo;
                refreshViewContent();
            }
        }
    }


}
