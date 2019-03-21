package com.corkili.learningclient.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.corkili.learningclient.common.ProtoUtils;
import com.corkili.learningclient.generate.protobuf.Info.Answer;
import com.corkili.learningclient.generate.protobuf.Info.QuestionType;
import com.corkili.learningclient.generate.protobuf.Request.QuestionDeleteRequest;
import com.corkili.learningclient.generate.protobuf.Request.QuestionFindAllRequest;
import com.corkili.learningclient.generate.protobuf.Request.QuestionGetRequest;
import com.corkili.learningclient.generate.protobuf.Request.QuestionImportRequest;
import com.corkili.learningclient.generate.protobuf.Request.QuestionUpdateRequest;
import com.corkili.learningclient.generate.protobuf.Response.QuestionDeleteResponse;
import com.corkili.learningclient.generate.protobuf.Response.QuestionFindAllResponse;
import com.corkili.learningclient.generate.protobuf.Response.QuestionGetResponse;
import com.corkili.learningclient.generate.protobuf.Response.QuestionImportResponse;
import com.corkili.learningclient.generate.protobuf.Response.QuestionUpdateResponse;
import com.corkili.learningclient.network.HttpUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class QuestionService {

    public static final int FIND_ALL_QUESTION_MSG = 0x61;
    public static final int CREATE_QUESTION_MSG = 0x62;
    public static final int UPDATE_QUESTION_MSG = 0x63;
    public static final int GET_QUESTION_MSG = 0x64;
    public static final int DELETE_QUESTION_MSG = 0x65;

    private static final String TAG = "QuestionService";
    private static QuestionService instance;

    private QuestionService() {

    }

    public static QuestionService getInstance() {
        if (instance == null) {
            synchronized (QuestionService.class) {
                if (instance == null) {
                    instance = new QuestionService();
                }
            }
        }
        return instance;
    }

    public void findAllQuestion(final Handler handler, boolean all, Collection<String> keywords,
                                Collection<QuestionType> questionTypes) {
        Message msg = new Message();
        msg.what = FIND_ALL_QUESTION_MSG;
        final QuestionFindAllRequest request = QuestionFindAllRequest.newBuilder()
                .setAll(all)
                .setByKeyword(keywords != null && !keywords.isEmpty())
                .addAllKeyword(keywords == null ? Collections.emptyList() : keywords)
                .setByQuestionType(questionTypes != null && !questionTypes.isEmpty())
                .addAllQuestionType(questionTypes == null ? Collections.emptyList() : questionTypes)
                .build();
        AsyncTaskExecutor.execute(() -> {
            QuestionFindAllResponse response = HttpUtils.request(request,
                    QuestionFindAllRequest.class, QuestionFindAllResponse.class, "/question/findAll");
            Log.i(TAG, "findAllQuestion: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            QuestionFindAllResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            QuestionFindAllResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void getQuestion(final Handler handler, Collection<Long> questionIdList, boolean loadImage) {
        Message msg = new Message();
        msg.what = GET_QUESTION_MSG;
        final QuestionGetRequest request = QuestionGetRequest.newBuilder()
                .addAllQuestionId(questionIdList)
                .setLoadImage(loadImage)
                .build();
        AsyncTaskExecutor.execute(() -> {
            QuestionGetResponse response = HttpUtils.request(request,
                    QuestionGetRequest.class, QuestionGetResponse.class, "/question/get");
            Log.i(TAG, "getQuestion: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            QuestionGetResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            QuestionGetResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void createQuestion(final Handler handler, String question, QuestionType questionType,
                               boolean autoCheck, Map<Integer, String> choices, Answer answer) {
        Message msg = new Message();
        msg.what = CREATE_QUESTION_MSG;
        ServiceResult result = null;
        if (StringUtils.isBlank(question)) {
            result = ServiceResult.failResultWithMessage("问题描述不能为空");
        } else if (questionType == null || questionType == QuestionType.UNRECOGNIZED) {
            result = ServiceResult.failResultWithMessage("系统错误");
        } else if ((questionType == QuestionType.SingleChoice || questionType == QuestionType.MultipleChoice)
                && (choices == null || choices.isEmpty())) {
            result = ServiceResult.failResultWithMessage("选择题必须提供选项");
        } else if ((questionType == QuestionType.SingleChoice || questionType == QuestionType.MultipleChoice)
                && !ProtoUtils.checkChoices(choices)) {
            result = ServiceResult.failResultWithMessage("选项内容不能为空");
        } else if (!ProtoUtils.checkAnswer(answer, questionType, choices)) {
            result = ServiceResult.failResultWithMessage("必须提供符合题型的答案");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final QuestionImportRequest request = QuestionImportRequest.newBuilder()
                .setQuestion(question)
                .setQuestionType(questionType)
                .setAutoCheck(autoCheck)
                .putAllChoices(choices == null ? Collections.emptyMap() : choices)
                .setAnswer(answer)
                .build();
        AsyncTaskExecutor.execute(() -> {
            QuestionImportResponse response = HttpUtils.request(request,
                    QuestionImportRequest.class, QuestionImportResponse.class, "/question/import");
            Log.i(TAG, "createQuestion: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            QuestionImportResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            QuestionImportResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void updateQuestion(final Handler handler, Long questionId, QuestionType questionType, String question,
                               Boolean autoCheck, boolean updateChoices, Map<Integer, String> choices, Answer answer) {
        Message msg = new Message();
        msg.what = UPDATE_QUESTION_MSG;
        ServiceResult result = null;
        if (questionId == null || questionId < 0 || questionType == null || questionType == QuestionType.UNRECOGNIZED) {
            result = ServiceResult.failResultWithMessage("系统错误");
        } else if (question != null && StringUtils.isBlank(question)) {
            result = ServiceResult.failResultWithMessage("问题描述不能为空");
        } else if ((questionType == QuestionType.SingleChoice || questionType == QuestionType.MultipleChoice)
                && (choices == null || choices.isEmpty())) {
            result = ServiceResult.failResultWithMessage("选择题必须提供选项");
        } else if ((questionType == QuestionType.SingleChoice || questionType == QuestionType.MultipleChoice)
                && (!ProtoUtils.checkChoices(choices))) {
            result = ServiceResult.failResultWithMessage("选项内容不能为空");
        } else if (answer != null && !ProtoUtils.checkAnswer(answer, questionType, choices)) {
            result = ServiceResult.failResultWithMessage("必须提供符合题型的答案");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final QuestionUpdateRequest request = QuestionUpdateRequest.newBuilder()
                .setQuestionId(questionId)
                .setUpdateQuestion(question != null)
                .setQuestion(question != null ? question : "")
                .setUpdateQuestionType(false)
                .setQuestionType(questionType)
                .setUpdateAutoCheck(autoCheck != null)
                .setAutoCheck(autoCheck != null ? autoCheck : false)
                .setUpdateChoices(updateChoices)
                .putAllChoices(updateChoices ? choices : Collections.emptyMap())
                .setUpdateAnswer(answer != null)
                .setAnswer(answer != null ? answer : Answer.getDefaultInstance())
                .build();
        AsyncTaskExecutor.execute(() -> {
            QuestionUpdateResponse response = HttpUtils.request(request,
                    QuestionUpdateRequest.class, QuestionUpdateResponse.class, "/question/update");
            Log.i(TAG, "updateQuestion: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            QuestionUpdateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            QuestionUpdateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void deleteQuestion(final Handler handler, long questionId) {
        Message msg = new Message();
        msg.what = DELETE_QUESTION_MSG;
        final QuestionDeleteRequest request = QuestionDeleteRequest.newBuilder()
                .setQuestionId(questionId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            QuestionDeleteResponse response = HttpUtils.request(request,
                    QuestionDeleteRequest.class, QuestionDeleteResponse.class, "/question/delete");
            Log.i(TAG, "deleteQuestion: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            QuestionDeleteResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            QuestionDeleteResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

}
