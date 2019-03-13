package com.corkili.learningclient.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.corkili.learningclient.generate.protobuf.Request.CourseCreateRequest;
import com.corkili.learningclient.generate.protobuf.Request.CourseFindAllRequest;
import com.corkili.learningclient.generate.protobuf.Request.CourseUpdateRequest;
import com.corkili.learningclient.generate.protobuf.Response.CourseCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.CourseFindAllResponse;
import com.corkili.learningclient.generate.protobuf.Response.CourseUpdateResponse;
import com.corkili.learningclient.network.HttpUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Collections;

public class CourseService {

    public static final int FIND_ALL_COURSE_MSG = 0x21;
    public static final int CREATE_COURSE_MSG = 0x22;
    public static final int UPDATE_COURSE_MSG = 0x23;

    private static final String TAG = "CourseService";
    private static CourseService instance;

    private CourseService() {

    }

    public static CourseService getInstance() {
        if (instance == null) {
            synchronized (CourseService.class) {
                if (instance == null) {
                    instance = new CourseService();
                }
            }
        }
        return instance;
    }

    public void findAllCourse(final Handler handler, boolean all, boolean teacherSelf,
                                     Long teacherId, String teacherName, Collection<String> keyword) {
        Message msg = new Message();
        msg.what = FIND_ALL_COURSE_MSG;
        final CourseFindAllRequest request = CourseFindAllRequest.newBuilder()
                .setAll(all)
                .setByTeacherId(teacherSelf || teacherId != null)
                .setTeacherId((teacherSelf || teacherId == null) ? -1 : teacherId)
                .setByTeacherName(teacherName != null)
                .setTeacherName(teacherName == null ? "" : teacherName)
                .setByKeyword(keyword != null && !keyword.isEmpty())
                .addAllKeyword(keyword == null ? Collections.emptyList() : keyword)
                .build();
        AsyncTaskExecutor.execute(() -> {
            CourseFindAllResponse response = HttpUtils.request(request,
                    CourseFindAllRequest.class, CourseFindAllResponse.class, "/course/findAll");
            Log.i(TAG, "findAllCourse: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            CourseFindAllResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            CourseFindAllResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void createCourse(final Handler handler, String courseName, Collection<String> tags,
                             boolean open, String description) {
        Message msg = new Message();
        msg.what = CREATE_COURSE_MSG;
        ServiceResult result = null;
        if (StringUtils.isBlank(courseName)) {
            result = ServiceResult.failResultWithMessage("课程名称不能为空");
        } else if (courseName.length() > 50) {
            result = ServiceResult.failResultWithMessage("课程名称最大长度为50");
        } else if (tags == null || tags.isEmpty()) {
            result = ServiceResult.failResultWithMessage("标签不能为空");
        } else if (StringUtils.isBlank(description)) {
            result = ServiceResult.failResultWithMessage("课程描述不能为空");
        } else if (description.length() > 2000) {
            result = ServiceResult.failResultWithMessage("课程描述最大长度为2000");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final CourseCreateRequest request = CourseCreateRequest.newBuilder()
                .setCourseName(courseName)
                .setDescription(description)
                .addAllTag(tags)
                .setOpen(open)
                .build();
        AsyncTaskExecutor.execute(() -> {
            CourseCreateResponse response = HttpUtils.request(request,
                    CourseCreateRequest.class, CourseCreateResponse.class, "/course/create");
            Log.i(TAG, "createCourse: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            CourseCreateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            CourseCreateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void updateCourse(final Handler handler, Long courseId, String courseName,
                             Collection<String> tags, Boolean open, String description) {
        Message msg = new Message();
        msg.what = UPDATE_COURSE_MSG;
        ServiceResult result = null;
        if (courseId == null) {
            result = ServiceResult.failResultWithMessage("系统错误：课程ID为空");
        } else if (courseName != null && StringUtils.isBlank(courseName)) {
            result = ServiceResult.failResultWithMessage("课程名称不能为空");
        } else if (courseName != null && courseName.length() > 50) {
            result = ServiceResult.failResultWithMessage("课程名称最大长度为50");
        } else if (tags != null && tags.isEmpty()) {
            result = ServiceResult.failResultWithMessage("标签不能为空");
        } else if (description != null && StringUtils.isBlank(description)) {
            result = ServiceResult.failResultWithMessage("课程描述不能为空");
        } else if (description != null && description.length() > 2000) {
            result = ServiceResult.failResultWithMessage("课程描述最大长度为2000");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final CourseUpdateRequest request = CourseUpdateRequest.newBuilder()
                .setCourseId(courseId)
                .setUpdateCourseName(courseName != null)
                .setCourseName(courseName)
                .setUpdateDescription(description != null)
                .setDescription(description)
                .setUpdateImage(false)
                .setUpdateTags(tags != null)
                .addAllTag(tags)
                .setUpdateOpen(open != null)
                .setOpen(open != null ? open : true)
                .build();
        AsyncTaskExecutor.execute(() -> {
            CourseUpdateResponse response = HttpUtils.request(request,
                    CourseUpdateRequest.class, CourseUpdateResponse.class, "/course/create");
            Log.i(TAG, "updateCourse: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            CourseUpdateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            CourseUpdateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

}
