package com.corkili.learningclient.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.corkili.learningclient.generate.protobuf.Info.CourseCommentType;
import com.corkili.learningclient.generate.protobuf.Request.CourseCommentCreateRequest;
import com.corkili.learningclient.generate.protobuf.Request.CourseCommentFindAllRequest;
import com.corkili.learningclient.generate.protobuf.Response.CourseCommentCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.CourseCommentFindAllResponse;
import com.corkili.learningclient.network.HttpUtils;

import org.apache.commons.lang3.StringUtils;

public class CourseCommentService {

    public static final int FIND_ALL_COURSE_COMMENT_MSG = 0x31;
    public static final int CREATE_COURSE_COMMENT_MSG = 0x32;

    private static final String TAG = "CourseCommentService";
    private static CourseCommentService instance;

    private CourseCommentService() {

    }

    public static CourseCommentService getInstance() {
        if (instance == null) {
            synchronized (CourseCommentService.class) {
                if (instance == null) {
                    instance = new CourseCommentService();
                }
            }
        }
        return instance;
    }

    public void findAllCourseComment(final Handler handler, long commentedCourseId) {
        Message msg = new Message();
        msg.what = FIND_ALL_COURSE_COMMENT_MSG;
        final CourseCommentFindAllRequest request = CourseCommentFindAllRequest.newBuilder()
                .setCommentedCourseId(commentedCourseId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            CourseCommentFindAllResponse response = HttpUtils.request(request,
                    CourseCommentFindAllRequest.class, CourseCommentFindAllResponse.class, "/courseComment/findAll");
            Log.i(TAG, "findAllCourseComment: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            CourseCommentFindAllResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            CourseCommentFindAllResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void createCourseComment(final Handler handler, CourseCommentType courseCommentType,
                                    String content, long commentedCourseId) {
        Message msg = new Message();
        msg.what = CREATE_COURSE_COMMENT_MSG;
        ServiceResult result = null;
        if (courseCommentType == null || courseCommentType == CourseCommentType.UNRECOGNIZED) {
            result = ServiceResult.failResultWithMessage("评分不能为空");
        } else if (StringUtils.isBlank(content)) {
            result = ServiceResult.failResultWithMessage("评论内容不能为空");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final CourseCommentCreateRequest request = CourseCommentCreateRequest.newBuilder()
                .setCommentType(courseCommentType)
                .setContent(content)
                .setCommentedCourseId(commentedCourseId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            CourseCommentCreateResponse response = HttpUtils.request(request,
                    CourseCommentCreateRequest.class, CourseCommentCreateResponse.class, "/courseComment/create");
            Log.i(TAG, "createCourseComment: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            CourseCommentCreateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            CourseCommentCreateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }
    
}
