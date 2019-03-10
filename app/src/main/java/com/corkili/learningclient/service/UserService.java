package com.corkili.learningclient.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Request.UserLoginRequest;
import com.corkili.learningclient.generate.protobuf.Request.UserRegisterRequest;
import com.corkili.learningclient.generate.protobuf.Response.UserLoginResponse;
import com.corkili.learningclient.generate.protobuf.Response.UserRegisterResponse;
import com.corkili.learningclient.network.HttpUtils;

import org.apache.commons.lang3.StringUtils;

public class UserService {

    public static final int REGISTER_MSG = 0;
    public static final int LOGIN_MSG = 1;

    private static final String TAG = "UserService";
    private static UserService instance;

    private UserService() {

    }

    public static UserService getInstance() {
        if (instance == null) {
            synchronized (UserService.class) {
                if (instance == null) {
                    instance = new UserService();
                }
            }
        }
        return instance;
    }

    public void register(final Handler handler, String phone, String password, UserType userType, String username) {
        final Message msg = new Message();
        msg.what = REGISTER_MSG;
        ServiceResult result = null;
        if (StringUtils.isBlank(phone)) {
            result = ServiceResult.failResultWithMessage("手机号不能为空");
        } else if (StringUtils.isBlank(password)) {
            result = ServiceResult.failResultWithMessage("密码不能为空");
        } else if (StringUtils.isBlank(username)) {
            result = ServiceResult.failResultWithMessage("手机号不能为空");
        } else if (phone.length() != 11) {
            result = ServiceResult.failResultWithMessage("手机号非法");
        } else if (password.length() < 6) {
            result = ServiceResult.failResultWithMessage("密码长度应大于6");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final UserRegisterRequest request = UserRegisterRequest.newBuilder()
                .setUserInfo(UserInfo.newBuilder()
                        .setPhone(phone)
                        .setUsername(username)
                        .setUserType(userType))
                .setPassword(password)
                .build();
        AsyncTaskExecutor.execute(() -> {
            UserRegisterResponse response = HttpUtils.request(request,
                    UserRegisterRequest.class, UserRegisterResponse.class, "/user/register");
            Log.i(TAG, "register: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            UserRegisterResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            UserRegisterResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void login(final Handler handler, String phone, String password, UserType userType) {
        final Message msg = new Message();
        msg.what = LOGIN_MSG;
        ServiceResult result = null;
        if (StringUtils.isBlank(phone)) {
            result = ServiceResult.failResultWithMessage("手机号不能为空");
        } else if (StringUtils.isBlank(password)) {
            result = ServiceResult.failResultWithMessage("密码不能为空");
        }  else if (phone.length() != 11) {
            result = ServiceResult.failResultWithMessage("手机号非法");
        } else if (password.length() < 6) {
            result = ServiceResult.failResultWithMessage("密码长度应大于6");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final UserLoginRequest request = UserLoginRequest.newBuilder()
                .setPhone(phone)
                .setPassword(password)
                .setUserType(userType)
                .build();
        AsyncTaskExecutor.execute(() -> {
            UserLoginResponse response = HttpUtils.request(request,
                    UserLoginRequest.class, UserLoginResponse.class, "/user/login");
            Log.i(TAG, "login: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            UserLoginResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            UserLoginResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

}
