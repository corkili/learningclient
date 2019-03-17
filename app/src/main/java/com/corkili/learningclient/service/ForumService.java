package com.corkili.learningclient.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.corkili.learningclient.generate.protobuf.Request.ForumTopicCreateRequest;
import com.corkili.learningclient.generate.protobuf.Request.ForumTopicDeleteRequest;
import com.corkili.learningclient.generate.protobuf.Request.ForumTopicFindAllRequest;
import com.corkili.learningclient.generate.protobuf.Request.ForumTopicUpdateRequest;
import com.corkili.learningclient.generate.protobuf.Request.TopicCommentCreateRequest;
import com.corkili.learningclient.generate.protobuf.Request.TopicCommentDeleteRequest;
import com.corkili.learningclient.generate.protobuf.Request.TopicCommentFindAllRequest;
import com.corkili.learningclient.generate.protobuf.Request.TopicReplyCreateRequest;
import com.corkili.learningclient.generate.protobuf.Request.TopicReplyDeleteRequest;
import com.corkili.learningclient.generate.protobuf.Request.TopicReplyFindAllRequest;
import com.corkili.learningclient.generate.protobuf.Response.ForumTopicCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.ForumTopicDeleteResponse;
import com.corkili.learningclient.generate.protobuf.Response.ForumTopicFindAllResponse;
import com.corkili.learningclient.generate.protobuf.Response.ForumTopicUpdateResponse;
import com.corkili.learningclient.generate.protobuf.Response.TopicCommentCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.TopicCommentDeleteResponse;
import com.corkili.learningclient.generate.protobuf.Response.TopicCommentFindAllResponse;
import com.corkili.learningclient.generate.protobuf.Response.TopicReplyCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.TopicReplyDeleteResponse;
import com.corkili.learningclient.generate.protobuf.Response.TopicReplyFindAllResponse;
import com.corkili.learningclient.network.HttpUtils;

import org.apache.commons.lang3.StringUtils;

public class ForumService {

    public static final int FIND_ALL_FORUM_TOPIC_MSG = 0x51;
    public static final int CREATE_FORUM_TOPIC_MSG = 0x52;
    public static final int UPDATE_FORUM_TOPIC_MSG = 0x53;
    public static final int DELETE_FORUM_TOPIC_MSG = 0x54;
    public static final int FIND_ALL_TOPIC_COMMENT_MSG = 0x55;
    public static final int CREATE_TOPIC_COMMENT_MSG = 0x56;
    public static final int DELETE_TOPIC_COMMENT_MSG = 0x57;
    public static final int FIND_ALL_TOPIC_REPLY_MSG = 0x58;
    public static final int CREATE_TOPIC_REPLY_MSG = 0x59;
    public static final int DELETE_TOPIC_REPLY_MSG = 0x5A;

    private static final String TAG = "ForumService";
    private static ForumService instance;

    private ForumService() {

    }

    public static ForumService getInstance() {
        if (instance == null) {
            synchronized (ForumService.class) {
                if (instance == null) {
                    instance = new ForumService();
                }
            }
        }
        return instance;
    }

    public void findAllForumTopic(final Handler handler, long belongCourseId) {
        Message msg = new Message();
        msg.what = FIND_ALL_FORUM_TOPIC_MSG;
        final ForumTopicFindAllRequest request = ForumTopicFindAllRequest.newBuilder()
                .setBelongCourseId(belongCourseId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            ForumTopicFindAllResponse response = HttpUtils.request(request,
                    ForumTopicFindAllRequest.class, ForumTopicFindAllResponse.class, "/forumTopic/findAll");
            Log.i(TAG, "findAllForumTopic: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            ForumTopicFindAllResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            ForumTopicFindAllResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void createForumTopic(final Handler handler, String title, String description, long belongCourseId) {
        Message msg = new Message();
        msg.what = CREATE_FORUM_TOPIC_MSG;
        ServiceResult result = null;
        if (StringUtils.isBlank(title)) {
            result = ServiceResult.failResultWithMessage("标题不能为空");
        } else if (StringUtils.isBlank(description)) {
            result = ServiceResult.failResultWithMessage("帖子描述不能为空");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final ForumTopicCreateRequest request = ForumTopicCreateRequest.newBuilder()
                .setTitle(title)
                .setDescription(description)
                .setBelongCourseId(belongCourseId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            ForumTopicCreateResponse response = HttpUtils.request(request,
                    ForumTopicCreateRequest.class, ForumTopicCreateResponse.class, "/forumTopic/create");
            Log.i(TAG, "createForumTopic: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            ForumTopicCreateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            ForumTopicCreateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void updateForumTopic(final Handler handler, long forumTopicId, String description) {
        Message msg = new Message();
        msg.what = UPDATE_FORUM_TOPIC_MSG;
        ServiceResult result = null;
        if (StringUtils.isBlank(description)) {
            result = ServiceResult.failResultWithMessage("帖子描述不能为空");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final ForumTopicUpdateRequest request = ForumTopicUpdateRequest.newBuilder()
                .setForumTopicId(forumTopicId)
                .setUpdateDescription(true)
                .setDescription(description)
                .build();
        AsyncTaskExecutor.execute(() -> {
            ForumTopicUpdateResponse response = HttpUtils.request(request,
                    ForumTopicUpdateRequest.class, ForumTopicUpdateResponse.class, "/forumTopic/update");
            Log.i(TAG, "updateForumTopic: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            ForumTopicUpdateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            ForumTopicUpdateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void deleteForumTopic(final Handler handler, long forumTopicId) {
        Message msg = new Message();
        msg.what = DELETE_FORUM_TOPIC_MSG;
        final ForumTopicDeleteRequest request = ForumTopicDeleteRequest.newBuilder()
                .setForumTopicId(forumTopicId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            ForumTopicDeleteResponse response = HttpUtils.request(request,
                    ForumTopicDeleteRequest.class, ForumTopicDeleteResponse.class, "/forumTopic/delete");
            Log.i(TAG, "deleteForumTopic: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            ForumTopicDeleteResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            ForumTopicDeleteResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void findAllTopicComment(final Handler handler, long belongTopicId) {
        Message msg = new Message();
        msg.what = FIND_ALL_TOPIC_COMMENT_MSG;
        final TopicCommentFindAllRequest request = TopicCommentFindAllRequest.newBuilder()
                .setBelongTopicId(belongTopicId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            TopicCommentFindAllResponse response = HttpUtils.request(request,
                    TopicCommentFindAllRequest.class, TopicCommentFindAllResponse.class, "/topicComment/findAll");
            Log.i(TAG, "findAllTopicComment: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            TopicCommentFindAllResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            TopicCommentFindAllResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void createTopicComment(final Handler handler, String content, long belongTopicId) {
        Message msg = new Message();
        msg.what = CREATE_TOPIC_COMMENT_MSG;
        ServiceResult result = null;
        if (StringUtils.isBlank(content)) {
            result = ServiceResult.failResultWithMessage("内容不能为空");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final TopicCommentCreateRequest request = TopicCommentCreateRequest.newBuilder()
                .setContent(content)
                .setBelongTopicId(belongTopicId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            TopicCommentCreateResponse response = HttpUtils.request(request,
                    TopicCommentCreateRequest.class, TopicCommentCreateResponse.class, "/topicComment/create");
            Log.i(TAG, "createTopicComment: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            TopicCommentCreateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            TopicCommentCreateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void deleteTopicComment(final Handler handler, long topicCommentId) {
        Message msg = new Message();
        msg.what = DELETE_TOPIC_COMMENT_MSG;
        final TopicCommentDeleteRequest request = TopicCommentDeleteRequest.newBuilder()
                .setTopicCommentId(topicCommentId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            TopicCommentDeleteResponse response = HttpUtils.request(request,
                    TopicCommentDeleteRequest.class, TopicCommentDeleteResponse.class, "/topicComment/delete");
            Log.i(TAG, "deleteTopicComment: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            TopicCommentDeleteResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            TopicCommentDeleteResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void findAllTopicReply(final Handler handler, long belongCommentId) {
        Message msg = new Message();
        msg.what = FIND_ALL_TOPIC_REPLY_MSG;
        final TopicReplyFindAllRequest request = TopicReplyFindAllRequest.newBuilder()
                .setBelongCommentId(belongCommentId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            TopicReplyFindAllResponse response = HttpUtils.request(request,
                    TopicReplyFindAllRequest.class, TopicReplyFindAllResponse.class, "/topicReply/findAll");
            Log.i(TAG, "findAllTopicReply: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            TopicReplyFindAllResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            TopicReplyFindAllResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void createTopicReply(final Handler handler, String content, long belongCommentId) {
        Message msg = new Message();
        msg.what = CREATE_TOPIC_REPLY_MSG;
        ServiceResult result = null;
        if (StringUtils.isBlank(content)) {
            result = ServiceResult.failResultWithMessage("内容不能为空");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final TopicReplyCreateRequest request = TopicReplyCreateRequest.newBuilder()
                .setContent(content)
                .setBelongCommentId(belongCommentId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            TopicReplyCreateResponse response = HttpUtils.request(request,
                    TopicReplyCreateRequest.class, TopicReplyCreateResponse.class, "/topicReply/create");
            Log.i(TAG, "createTopicReply: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            TopicReplyCreateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            TopicReplyCreateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void deleteTopicReply(final Handler handler, long topicReplyId) {
        Message msg = new Message();
        msg.what = DELETE_TOPIC_REPLY_MSG;
        final TopicReplyDeleteRequest request = TopicReplyDeleteRequest.newBuilder()
                .setTopicReplyId(topicReplyId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            TopicReplyDeleteResponse response = HttpUtils.request(request,
                    TopicReplyDeleteRequest.class, TopicReplyDeleteResponse.class, "/topicReply/delete");
            Log.i(TAG, "deleteTopicReply: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            TopicReplyDeleteResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            TopicReplyDeleteResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }
    
}
