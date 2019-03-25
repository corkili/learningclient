package com.corkili.learningclient.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.corkili.learningclient.generate.protobuf.Info.ExamSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Request.SubmittedExamCreateRequest;
import com.corkili.learningclient.generate.protobuf.Request.SubmittedExamDeleteRequest;
import com.corkili.learningclient.generate.protobuf.Request.SubmittedExamFindAllRequest;
import com.corkili.learningclient.generate.protobuf.Request.SubmittedExamGetRequest;
import com.corkili.learningclient.generate.protobuf.Request.SubmittedExamUpdateRequest;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedExamCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedExamDeleteResponse;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedExamFindAllResponse;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedExamGetResponse;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedExamUpdateResponse;
import com.corkili.learningclient.network.HttpUtils;

import java.util.Collections;
import java.util.Map;

public class SubmittedExamService {

    public static final int FIND_ALL_SUBMITTED_EXAM_MSG = 0xB1;
    public static final int CREATE_SUBMITTED_EXAM_MSG = 0xB2;
    public static final int UPDATE_SUBMITTED_EXAM_MSG = 0xB3;
    public static final int GET_SUBMITTED_EXAM_MSG = 0xB4;
    public static final int DELETE_SUBMITTED_EXAM_MSG = 0xB5;

    private static final String TAG = "SubmittedExamService";
    private static SubmittedExamService instance;

    private SubmittedExamService() {

    }

    public static SubmittedExamService getInstance() {
        if (instance == null) {
            synchronized (SubmittedExamService.class) {
                if (instance == null) {
                    instance = new SubmittedExamService();
                }
            }
        }
        return instance;
    }

    public void findAllSubmittedExam(final Handler handler, long belongExamId) {
        Message msg = new Message();
        msg.what = FIND_ALL_SUBMITTED_EXAM_MSG;
        final SubmittedExamFindAllRequest request = SubmittedExamFindAllRequest.newBuilder()
                .setBelongExamId(belongExamId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            SubmittedExamFindAllResponse response = HttpUtils.request(request,
                    SubmittedExamFindAllRequest.class, SubmittedExamFindAllResponse.class, "/submittedExam/findAll");
            Log.i(TAG, "findAllSubmittedExam: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            SubmittedExamFindAllResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            SubmittedExamFindAllResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void getSubmittedExam(final Handler handler, boolean byId, long submittedExamId, 
                                       long belongExamId, long submitterId) {
        Message msg = new Message();
        msg.what = GET_SUBMITTED_EXAM_MSG;
        final SubmittedExamGetRequest request = SubmittedExamGetRequest.newBuilder()
                .setById(byId)
                .setSubmittedExamId(submittedExamId)
                .setBelongExamId(belongExamId)
                .setSubmitterId(submitterId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            SubmittedExamGetResponse response = HttpUtils.request(request,
                    SubmittedExamGetRequest.class, SubmittedExamGetResponse.class, "/submittedExam/get");
            Log.i(TAG, "getSubmittedExam: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResult("网络请求错误", Boolean.class, true);
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            SubmittedExamGetResponse.class, response, Boolean.class, false);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            SubmittedExamGetResponse.class, response, Boolean.class, false);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void createSubmittedExam(final Handler handler, long belongExamId, boolean finished,
                                          Map<Integer, SubmittedAnswer> submittedAnswerMap) {
        Message msg = new Message();
        msg.what = CREATE_SUBMITTED_EXAM_MSG;
        final SubmittedExamCreateRequest request = SubmittedExamCreateRequest.newBuilder()
                .putAllSubmittedAnswer(submittedAnswerMap)
                .setBelongExamId(belongExamId)
                .setFinished(finished)
                .build();
        AsyncTaskExecutor.execute(() -> {
            SubmittedExamCreateResponse response = HttpUtils.request(request,
                    SubmittedExamCreateRequest.class, SubmittedExamCreateResponse.class, "/submittedExam/create");
            Log.i(TAG, "createSubmittedExam: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            SubmittedExamCreateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            SubmittedExamCreateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void updateSubmittedExam(final Handler handler, long submittedExamId, 
                                          boolean updateSubmittedAnswer, 
                                          Map<Integer, ExamSubmittedAnswer> submittedAnswerMap,
                                          boolean updateFinished, boolean finished) {
        Message msg = new Message();
        msg.what = UPDATE_SUBMITTED_EXAM_MSG;
        final SubmittedExamUpdateRequest request = SubmittedExamUpdateRequest.newBuilder()
                .setSubmittedExamId(submittedExamId)
                .setUpdateSubmittedAnswer(updateSubmittedAnswer)
                .putAllSubmittedAnswer(updateSubmittedAnswer ? submittedAnswerMap : Collections.emptyMap())
                .setUpdateFinished(updateFinished)
                .setFinished(finished)
                .build();
        AsyncTaskExecutor.execute(() -> {
            SubmittedExamUpdateResponse response = HttpUtils.request(request,
                    SubmittedExamUpdateRequest.class, SubmittedExamUpdateResponse.class, "/submittedExam/update");
            Log.i(TAG, "updateSubmittedExam: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            SubmittedExamUpdateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            SubmittedExamUpdateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void deleteSubmittedExam(final Handler handler, long submittedExamId) {
        Message msg = new Message();
        msg.what = DELETE_SUBMITTED_EXAM_MSG;
        final SubmittedExamDeleteRequest request = SubmittedExamDeleteRequest.newBuilder()
                .setSubmittedExamId(submittedExamId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            SubmittedExamDeleteResponse response = HttpUtils.request(request,
                    SubmittedExamDeleteRequest.class, SubmittedExamDeleteResponse.class, "/submittedExam/delete");
            Log.i(TAG, "deleteSubmittedExam: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            SubmittedExamDeleteResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            SubmittedExamDeleteResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

}
