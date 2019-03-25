package com.corkili.learningclient.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.corkili.learningclient.generate.protobuf.Request.CourseSubscriptionCreateRequest;
import com.corkili.learningclient.generate.protobuf.Request.CourseSubscriptionDeleteRequest;
import com.corkili.learningclient.generate.protobuf.Request.CourseSubscriptionFindAllRequest;
import com.corkili.learningclient.generate.protobuf.Response.CourseSubscriptionCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.CourseSubscriptionDeleteResponse;
import com.corkili.learningclient.generate.protobuf.Response.CourseSubscriptionFindAllResponse;
import com.corkili.learningclient.network.HttpUtils;

public class CourseSubscriptionService {
    
    public static final int FIND_ALL_COURSE_SUBSCRIPTION_MSG = 0x41;
    public static final int CREATE_COURSE_SUBSCRIPTION_MSG = 0x42;
    public static final int DELETE_COURSE_SUBSCRIPTION_MSG = 0x43;

    private static final String TAG = "SubscriptionService";
    private static CourseSubscriptionService instance;

    private CourseSubscriptionService() {

    }

    public static CourseSubscriptionService getInstance() {
        if (instance == null) {
            synchronized (CourseSubscriptionService.class) {
                if (instance == null) {
                    instance = new CourseSubscriptionService();
                }
            }
        }
        return instance;
    }

    public void findAllCourseSubscription(final Handler handler, Long subscriberId, Long subscribedCourseId) {
        Message msg = new Message();
        msg.what = FIND_ALL_COURSE_SUBSCRIPTION_MSG;
        final CourseSubscriptionFindAllRequest request = CourseSubscriptionFindAllRequest.newBuilder()
                .setBySubscriberId(subscriberId != null)
                .setSubscriberId(subscriberId != null ? subscriberId : 0)
                .setBySubscribedCourseId(subscribedCourseId != null)
                .setSubscribedCourseId(subscribedCourseId != null ? subscribedCourseId : 0)
                .build();
        AsyncTaskExecutor.execute(() -> {
            CourseSubscriptionFindAllResponse response = HttpUtils.request(request,
                    CourseSubscriptionFindAllRequest.class, CourseSubscriptionFindAllResponse.class, "/courseSubscription/findAll");
            Log.i(TAG, "findAllCourseSubscription: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            CourseSubscriptionFindAllResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            CourseSubscriptionFindAllResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void createCourseSubscription(final Handler handler, long subscribedCourseId) {
        Message msg = new Message();
        msg.what = CREATE_COURSE_SUBSCRIPTION_MSG;
        final CourseSubscriptionCreateRequest request = CourseSubscriptionCreateRequest.newBuilder()
                .setSubscribedCourseId(subscribedCourseId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            CourseSubscriptionCreateResponse response = HttpUtils.request(request,
                    CourseSubscriptionCreateRequest.class, CourseSubscriptionCreateResponse.class, "/courseSubscription/create");
            Log.i(TAG, "createCourseSubscription: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            CourseSubscriptionCreateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            CourseSubscriptionCreateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void deleteCourseSubscription(final Handler handler, long courseSubscriptionId) {
        Message msg = new Message();
        msg.what = DELETE_COURSE_SUBSCRIPTION_MSG;
        final CourseSubscriptionDeleteRequest request = CourseSubscriptionDeleteRequest.newBuilder()
                .setCourseSubscriptionId(courseSubscriptionId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            CourseSubscriptionDeleteResponse response = HttpUtils.request(request,
                    CourseSubscriptionDeleteRequest.class, CourseSubscriptionDeleteResponse.class, "/courseSubscription/delete");
            Log.i(TAG, "deleteCourseSubscription: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            CourseSubscriptionDeleteResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            CourseSubscriptionDeleteResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }
    
}
