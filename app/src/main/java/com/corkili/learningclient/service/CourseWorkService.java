package com.corkili.learningclient.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.corkili.learningclient.generate.protobuf.Request.CourseWorkCreateRequest;
import com.corkili.learningclient.generate.protobuf.Request.CourseWorkDeleteRequest;
import com.corkili.learningclient.generate.protobuf.Request.CourseWorkFindAllRequest;
import com.corkili.learningclient.generate.protobuf.Request.CourseWorkGetRequest;
import com.corkili.learningclient.generate.protobuf.Request.CourseWorkUpdateRequest;
import com.corkili.learningclient.generate.protobuf.Response.CourseWorkCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.CourseWorkDeleteResponse;
import com.corkili.learningclient.generate.protobuf.Response.CourseWorkFindAllResponse;
import com.corkili.learningclient.generate.protobuf.Response.CourseWorkGetResponse;
import com.corkili.learningclient.generate.protobuf.Response.CourseWorkUpdateResponse;
import com.corkili.learningclient.network.HttpUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

public class CourseWorkService {

    public static final int FIND_ALL_COURSE_WORK_MSG = 0x71;
    public static final int CREATE_COURSE_WORK_MSG = 0x72;
    public static final int UPDATE_COURSE_WORK_MSG = 0x73;
    public static final int GET_COURSE_WORK_MSG = 0x74;
    public static final int DELETE_COURSE_WORK_MSG = 0x75;

    private static final String TAG = "CourseWorkService";
    private static CourseWorkService instance;

    private CourseWorkService() {

    }

    public static CourseWorkService getInstance() {
        if (instance == null) {
            synchronized (CourseWorkService.class) {
                if (instance == null) {
                    instance = new CourseWorkService();
                }
            }
        }
        return instance;
    }

    public void findAllCourseWork(final Handler handler, long belongCourseId) {
        Message msg = new Message();
        msg.what = FIND_ALL_COURSE_WORK_MSG;
        final CourseWorkFindAllRequest request = CourseWorkFindAllRequest.newBuilder()
                .setBelongCourseId(belongCourseId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            CourseWorkFindAllResponse response = HttpUtils.request(request,
                    CourseWorkFindAllRequest.class, CourseWorkFindAllResponse.class, "/courseWork/findAll");
            Log.i(TAG, "findAllCourseWork: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            CourseWorkFindAllResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            CourseWorkFindAllResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void getCourseWork(final Handler handler, long courseWorkId) {
        Message msg = new Message();
        msg.what = GET_COURSE_WORK_MSG;
        final CourseWorkGetRequest request = CourseWorkGetRequest.newBuilder()
                .setCourseWorkId(courseWorkId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            CourseWorkGetResponse response = HttpUtils.request(request,
                    CourseWorkGetRequest.class, CourseWorkGetResponse.class, "/courseWork/get");
            Log.i(TAG, "getCourseWork: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            CourseWorkGetResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            CourseWorkGetResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void createCourseWork(final Handler handler, String courseWorkName, long belongCourseId,
                                 Date deadline, Map<Integer, Long> questionIdMap) {
        Message msg = new Message();
        msg.what = CREATE_COURSE_WORK_MSG;
        ServiceResult result = null;
        if (StringUtils.isBlank(courseWorkName)) {
            result = ServiceResult.failResultWithMessage("作业名称不能为空");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final CourseWorkCreateRequest request = CourseWorkCreateRequest.newBuilder()
                .setCourseWorkName(courseWorkName)
                .setBelongCourseId(belongCourseId)
                .setHasDeadline(deadline != null)
                .setDeadline(deadline == null ? 0 : deadline.getTime())
                .putAllQuestionId(questionIdMap)
                .build();
        AsyncTaskExecutor.execute(() -> {
            CourseWorkCreateResponse response = HttpUtils.request(request,
                    CourseWorkCreateRequest.class, CourseWorkCreateResponse.class, "/courseWork/create");
            Log.i(TAG, "createCourseWork: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            CourseWorkCreateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            CourseWorkCreateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void updateCourseWork(final Handler handler, long courseWorkId, String courseWorkName,
                                 boolean updateDeadline, Date deadline,
                                 Map<Integer, Long> questionIdMap, Boolean open) {
        Message msg = new Message();
        msg.what = UPDATE_COURSE_WORK_MSG;
        ServiceResult result = null;
        if (courseWorkName != null && StringUtils.isBlank(courseWorkName)) {
            result = ServiceResult.failResultWithMessage("作业名称不能为空");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final CourseWorkUpdateRequest request = CourseWorkUpdateRequest.newBuilder()
                .setCourseWorkId(courseWorkId)
                .setUpdateOpen(open != null)
                .setOpen(open == null ? false : open)
                .setUpdateCourseWorkName(courseWorkName != null)
                .setCourseWorkName(courseWorkName == null ? "" : courseWorkName)
                .setUpdateDeadline(updateDeadline)
                .setHasDeadline(deadline != null)
                .setDeadline(deadline == null ? 0 : deadline.getTime())
                .setUpdateQuestion(questionIdMap != null)
                .putAllQuestionId(questionIdMap != null ? questionIdMap : Collections.emptyMap())
                .build();
        AsyncTaskExecutor.execute(() -> {
            CourseWorkUpdateResponse response = HttpUtils.request(request,
                    CourseWorkUpdateRequest.class, CourseWorkUpdateResponse.class, "/courseWork/update");
            Log.i(TAG, "updateCourseWork: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            CourseWorkUpdateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            CourseWorkUpdateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void deleteCourseWork(final Handler handler, long courseWorkId) {
        Message msg = new Message();
        msg.what = DELETE_COURSE_WORK_MSG;
        final CourseWorkDeleteRequest request = CourseWorkDeleteRequest.newBuilder()
                .setCourseWorkId(courseWorkId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            CourseWorkDeleteResponse response = HttpUtils.request(request,
                    CourseWorkDeleteRequest.class, CourseWorkDeleteResponse.class, "/courseWork/delete");
            Log.i(TAG, "deleteCourseWork: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            CourseWorkDeleteResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            CourseWorkDeleteResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

}
