package com.corkili.learningclient.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.corkili.learningclient.generate.protobuf.Info.CourseWorkSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Request.SubmittedCourseWorkCreateRequest;
import com.corkili.learningclient.generate.protobuf.Request.SubmittedCourseWorkDeleteRequest;
import com.corkili.learningclient.generate.protobuf.Request.SubmittedCourseWorkFindAllRequest;
import com.corkili.learningclient.generate.protobuf.Request.SubmittedCourseWorkGetRequest;
import com.corkili.learningclient.generate.protobuf.Request.SubmittedCourseWorkUpdateRequest;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedCourseWorkCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedCourseWorkDeleteResponse;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedCourseWorkFindAllResponse;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedCourseWorkGetResponse;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedCourseWorkUpdateResponse;
import com.corkili.learningclient.network.HttpUtils;

import java.util.Collections;
import java.util.Map;

public class SubmittedCourseWorkService {

    public static final int FIND_ALL_SUBMITTED_COURSE_WORK_MSG = 0xA1;
    public static final int CREATE_SUBMITTED_COURSE_WORK_MSG = 0xA2;
    public static final int UPDATE_SUBMITTED_COURSE_WORK_MSG = 0xA3;
    public static final int GET_SUBMITTED_COURSE_WORK_MSG = 0xA4;
    public static final int DELETE_SUBMITTED_COURSE_WORK_MSG = 0xA5;

    private static final String TAG = "SubmittedWorkService";
    private static SubmittedCourseWorkService instance;

    private SubmittedCourseWorkService() {

    }

    public static SubmittedCourseWorkService getInstance() {
        if (instance == null) {
            synchronized (SubmittedCourseWorkService.class) {
                if (instance == null) {
                    instance = new SubmittedCourseWorkService();
                }
            }
        }
        return instance;
    }

    public void findAllSubmittedCourseWork(final Handler handler, long belongCourseWorkId) {
        Message msg = new Message();
        msg.what = FIND_ALL_SUBMITTED_COURSE_WORK_MSG;
        final SubmittedCourseWorkFindAllRequest request = SubmittedCourseWorkFindAllRequest.newBuilder()
                .setBelongCourseWorkId(belongCourseWorkId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            SubmittedCourseWorkFindAllResponse response = HttpUtils.request(request,
                    SubmittedCourseWorkFindAllRequest.class, SubmittedCourseWorkFindAllResponse.class, "/submittedCourseWork/findAll");
            Log.i(TAG, "findAllSubmittedCourseWork: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            SubmittedCourseWorkFindAllResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            SubmittedCourseWorkFindAllResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void getSubmittedCourseWork(final Handler handler, boolean byId, long submittedCourseWorkId, 
                                       long belongCourseWorkId, long submitterId) {
        Message msg = new Message();
        msg.what = GET_SUBMITTED_COURSE_WORK_MSG;
        final SubmittedCourseWorkGetRequest request = SubmittedCourseWorkGetRequest.newBuilder()
                .setById(byId)
                .setSubmittedCourseWorkId(submittedCourseWorkId)
                .setBelongCourseWorkId(belongCourseWorkId)
                .setSubmitterId(submitterId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            SubmittedCourseWorkGetResponse response = HttpUtils.request(request,
                    SubmittedCourseWorkGetRequest.class, SubmittedCourseWorkGetResponse.class, "/submittedCourseWork/get");
            Log.i(TAG, "getSubmittedCourseWork: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResult("网络请求错误", Boolean.class, true);
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            SubmittedCourseWorkGetResponse.class, response, Boolean.class, false);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            SubmittedCourseWorkGetResponse.class, response, Boolean.class, false);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void createSubmittedCourseWork(final Handler handler, long belongCourseWorkId, boolean finished,
                                          Map<Integer, SubmittedAnswer> submittedAnswerMap) {
        Message msg = new Message();
        msg.what = CREATE_SUBMITTED_COURSE_WORK_MSG;
        final SubmittedCourseWorkCreateRequest request = SubmittedCourseWorkCreateRequest.newBuilder()
                .putAllSubmittedAnswer(submittedAnswerMap)
                .setBelongCourseWorkId(belongCourseWorkId)
                .setFinished(finished)
                .build();
        AsyncTaskExecutor.execute(() -> {
            SubmittedCourseWorkCreateResponse response = HttpUtils.request(request,
                    SubmittedCourseWorkCreateRequest.class, SubmittedCourseWorkCreateResponse.class, "/submittedCourseWork/create");
            Log.i(TAG, "createSubmittedCourseWork: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            SubmittedCourseWorkCreateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            SubmittedCourseWorkCreateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void updateSubmittedCourseWork(final Handler handler, long submittedCourseWorkId,
                                          boolean updateSubmittedAnswer,
                                          Map<Integer, CourseWorkSubmittedAnswer> submittedAnswerMap,
                                          boolean updateFinished, boolean finished) {
        Message msg = new Message();
        msg.what = UPDATE_SUBMITTED_COURSE_WORK_MSG;
        final SubmittedCourseWorkUpdateRequest request = SubmittedCourseWorkUpdateRequest.newBuilder()
                .setSubmittedCourseWorkId(submittedCourseWorkId)
                .setUpdateSubmittedAnswer(updateSubmittedAnswer)
                .putAllSubmittedAnswer(updateSubmittedAnswer ? submittedAnswerMap : Collections.emptyMap())
                .setUpdateFinished(updateFinished)
                .setFinished(finished)
                .build();
        AsyncTaskExecutor.execute(() -> {
            SubmittedCourseWorkUpdateResponse response = HttpUtils.request(request,
                    SubmittedCourseWorkUpdateRequest.class, SubmittedCourseWorkUpdateResponse.class, "/submittedCourseWork/update");
            Log.i(TAG, "updateSubmittedCourseWork: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            SubmittedCourseWorkUpdateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            SubmittedCourseWorkUpdateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void deleteSubmittedCourseWork(final Handler handler, long submittedCourseWorkId) {
        Message msg = new Message();
        msg.what = DELETE_SUBMITTED_COURSE_WORK_MSG;
        final SubmittedCourseWorkDeleteRequest request = SubmittedCourseWorkDeleteRequest.newBuilder()
                .setSubmittedCourseWorkId(submittedCourseWorkId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            SubmittedCourseWorkDeleteResponse response = HttpUtils.request(request,
                    SubmittedCourseWorkDeleteRequest.class, SubmittedCourseWorkDeleteResponse.class, "/submittedCourseWork/delete");
            Log.i(TAG, "deleteSubmittedCourseWork: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            SubmittedCourseWorkDeleteResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            SubmittedCourseWorkDeleteResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

}
