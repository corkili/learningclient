package com.corkili.learningclient.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.corkili.learningclient.generate.protobuf.Request.MessageCreateRequest;
import com.corkili.learningclient.generate.protobuf.Request.MessageFindAllRequest;
import com.corkili.learningclient.generate.protobuf.Response.MessageCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.MessageFindAllResponse;
import com.corkili.learningclient.network.HttpUtils;

import org.apache.commons.lang3.StringUtils;

public class MessageService {

    public static final int FIND_ALL_MESSAGE_MSG = 0x91;
    public static final int CREATE_MESSAGE_MSG = 0x92;

    private static final String TAG = "MessageService";
    private static MessageService instance;

    private MessageService() {

    }

    public static MessageService getInstance() {
        if (instance == null) {
            synchronized (MessageService.class) {
                if (instance == null) {
                    instance = new MessageService();
                }
            }
        }
        return instance;
    }

    public void findAllMessage(final Handler handler, Long receiverId, Long senderId, boolean reverse) {
        Message msg = new Message();
        msg.what = FIND_ALL_MESSAGE_MSG;
        final MessageFindAllRequest request = MessageFindAllRequest.newBuilder()
                .setByReceiverId(receiverId != null)
                .setReceiverId(receiverId != null ? receiverId : 0)
                .setBySenderId(senderId != null)
                .setSenderId(senderId != null ? senderId : 0)
                .setReverse(reverse)
                .build();
        AsyncTaskExecutor.execute(() -> {
            MessageFindAllResponse response = HttpUtils.request(request,
                    MessageFindAllRequest.class, MessageFindAllResponse.class, "/message/findAll");
            Log.i(TAG, "findAllMessage: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            MessageFindAllResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            MessageFindAllResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void createMessage(final Handler handler, String content, long receiverId, long senderId) {
        Message msg = new Message();
        msg.what = CREATE_MESSAGE_MSG;
        ServiceResult result = null;
        if (StringUtils.isBlank(content)) {
            result = ServiceResult.failResultWithMessage("消息内容不能为空");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final MessageCreateRequest request = MessageCreateRequest.newBuilder()
                .setText(content)
                .setReceiverId(receiverId)
                .setSenderId(senderId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            MessageCreateResponse response = HttpUtils.request(request,
                    MessageCreateRequest.class, MessageCreateResponse.class, "/message/create");
            Log.i(TAG, "createMessage: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            MessageCreateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            MessageCreateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

}
