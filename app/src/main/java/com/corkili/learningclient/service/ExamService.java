package com.corkili.learningclient.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.corkili.learningclient.generate.protobuf.Info.Score;
import com.corkili.learningclient.generate.protobuf.Request.ExamCreateRequest;
import com.corkili.learningclient.generate.protobuf.Request.ExamDeleteRequest;
import com.corkili.learningclient.generate.protobuf.Request.ExamFindAllRequest;
import com.corkili.learningclient.generate.protobuf.Request.ExamGetRequest;
import com.corkili.learningclient.generate.protobuf.Request.ExamUpdateRequest;
import com.corkili.learningclient.generate.protobuf.Response.ExamCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.ExamDeleteResponse;
import com.corkili.learningclient.generate.protobuf.Response.ExamFindAllResponse;
import com.corkili.learningclient.generate.protobuf.Response.ExamGetResponse;
import com.corkili.learningclient.generate.protobuf.Response.ExamUpdateResponse;
import com.corkili.learningclient.network.HttpUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

public class ExamService {

    public static final int FIND_ALL_EXAM_MSG = 0x81;
    public static final int CREATE_EXAM_MSG = 0x82;
    public static final int UPDATE_EXAM_MSG = 0x83;
    public static final int GET_EXAM_MSG = 0x84;
    public static final int DELETE_EXAM_MSG = 0x85;

    private static final String TAG = "ExamService";
    private static ExamService instance;

    private ExamService() {

    }

    public static ExamService getInstance() {
        if (instance == null) {
            synchronized (ExamService.class) {
                if (instance == null) {
                    instance = new ExamService();
                }
            }
        }
        return instance;
    }

    public void findAllExam(final Handler handler, long belongCourseId) {
        Message msg = new Message();
        msg.what = FIND_ALL_EXAM_MSG;
        final ExamFindAllRequest request = ExamFindAllRequest.newBuilder()
                .setBelongCourseId(belongCourseId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            ExamFindAllResponse response = HttpUtils.request(request,
                    ExamFindAllRequest.class, ExamFindAllResponse.class, "/exam/findAll");
            Log.i(TAG, "findAllExam: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            ExamFindAllResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            ExamFindAllResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void getExam(final Handler handler, long examId) {
        Message msg = new Message();
        msg.what = GET_EXAM_MSG;
        final ExamGetRequest request = ExamGetRequest.newBuilder()
                .setExamId(examId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            ExamGetResponse response = HttpUtils.request(request,
                    ExamGetRequest.class, ExamGetResponse.class, "/exam/get");
            Log.i(TAG, "getExam: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            ExamGetResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            ExamGetResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void createExam(final Handler handler, String examName, long belongCourseId, Date startTime,
                           Date endTime, Map<Integer, Long> questionIdMap, Map<Integer, Score> questionScoreMap) {
        Message msg = new Message();
        msg.what = CREATE_EXAM_MSG;
        ServiceResult result = null;
        if (StringUtils.isBlank(examName)) {
            result = ServiceResult.failResultWithMessage("考试名称不能为空");
        } else if (startTime == null) {
            result = ServiceResult.failResultWithMessage("开始时间不能为空");
        } else if (endTime == null) {
            result = ServiceResult.failResultWithMessage("结束时间不能为空");
        } else if (endTime.before(new Date())) {
            result = ServiceResult.failResultWithMessage("结束时间不能在当前时间之前");
        } else if (startTime.after(endTime)) {
            result = ServiceResult.failResultWithMessage("开始时间不能在结束时间之前");
        } else if (questionIdMap.size() != questionScoreMap.size()) {
            result = ServiceResult.failResultWithMessage("每个题目必须设置分数");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final ExamCreateRequest request = ExamCreateRequest.newBuilder()
                .setExamName(examName)
                .setBelongCourseId(belongCourseId)
                .setStartTime(startTime.getTime())
                .setEndTime(endTime.getTime())
                .setDuration(0)
                .putAllQuestionId(questionIdMap)
                .putAllQuestionScore(questionScoreMap)
                .build();
        AsyncTaskExecutor.execute(() -> {
            ExamCreateResponse response = HttpUtils.request(request,
                    ExamCreateRequest.class, ExamCreateResponse.class, "/exam/create");
            Log.i(TAG, "createExam: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            ExamCreateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            ExamCreateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void updateExam(final Handler handler, long examId, String examName, boolean updateStartTime,
                           Date startTime, boolean updateEndTime, Date endTime, boolean updateQuestion,
                           Map<Integer, Long> questionIdMap,Map<Integer, Score> questionScoreMap) {
        Message msg = new Message();
        msg.what = UPDATE_EXAM_MSG;
        ServiceResult result = null;
        if (examName != null && StringUtils.isBlank(examName)) {
            result = ServiceResult.failResultWithMessage("考试名称不能为空");
        } else if (startTime == null) {
            result = ServiceResult.failResultWithMessage("开始时间不能为空");
        } else if (endTime == null) {
            result = ServiceResult.failResultWithMessage("结束时间不能为空");
        } else if (updateEndTime && endTime.before(new Date())) {
            result = ServiceResult.failResultWithMessage("结束时间不能在当前时间之前");
        } else if ((updateStartTime || updateEndTime) && startTime.after(endTime)) {
            result = ServiceResult.failResultWithMessage("开始时间不能在结束时间之前");
        } else if (updateQuestion && questionIdMap.size() != questionScoreMap.size()) {
            result = ServiceResult.failResultWithMessage("每个题目必须设置分数");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final ExamUpdateRequest request = ExamUpdateRequest.newBuilder()
                .setExamId(examId)
                .setUpdateExamName(examName != null)
                .setExamName(examName == null ? "" : examName)
                .setUpdateStartTime(updateStartTime)
                .setStartTime(startTime.getTime())
                .setUpdateEndTime(updateEndTime)
                .setEndTime(endTime.getTime())
                .setUpdateDuration(false)
                .setDuration(0)
                .setUpdateQuestion(updateQuestion)
                .putAllQuestionId(questionIdMap != null ? questionIdMap : Collections.emptyMap())
                .putAllQuestionScore(questionScoreMap != null ? questionScoreMap : Collections.emptyMap())
                .build();
        AsyncTaskExecutor.execute(() -> {
            ExamUpdateResponse response = HttpUtils.request(request,
                    ExamUpdateRequest.class, ExamUpdateResponse.class, "/exam/update");
            Log.i(TAG, "updateExam: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            ExamUpdateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            ExamUpdateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void deleteExam(final Handler handler, long examId) {
        Message msg = new Message();
        msg.what = DELETE_EXAM_MSG;
        final ExamDeleteRequest request = ExamDeleteRequest.newBuilder()
                .setExamId(examId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            ExamDeleteResponse response = HttpUtils.request(request,
                    ExamDeleteRequest.class, ExamDeleteResponse.class, "/exam/delete");
            Log.i(TAG, "deleteExam: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            ExamDeleteResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            ExamDeleteResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

}
