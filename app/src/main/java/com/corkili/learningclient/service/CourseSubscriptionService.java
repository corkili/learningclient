package com.corkili.learningclient.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.corkili.learningclient.generate.protobuf.Request.CourseSubscriptionFindAllRequest;
import com.corkili.learningclient.generate.protobuf.Response.CourseSubscriptionFindAllResponse;
import com.corkili.learningclient.network.HttpUtils;

public class CourseSubscriptionService {
    
    public static final int FIND_ALL_COURSE_SUBSCRIPTION_MSG = 0x41;

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
    
}
