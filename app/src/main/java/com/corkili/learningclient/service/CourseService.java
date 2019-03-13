package com.corkili.learningclient.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.corkili.learningclient.generate.protobuf.Request.CourseFindAllRequest;
import com.corkili.learningclient.generate.protobuf.Response.CourseFindAllResponse;
import com.corkili.learningclient.network.HttpUtils;

import java.util.Collection;
import java.util.Collections;

public class CourseService {

    public static final int FIND_ALL_COURSE_MSG = 0x21;

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

}
