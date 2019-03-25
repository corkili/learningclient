package com.corkili.learningclient.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Request.UserLoginRequest;
import com.corkili.learningclient.generate.protobuf.Request.UserLogoutRequest;
import com.corkili.learningclient.generate.protobuf.Request.UserRegisterRequest;
import com.corkili.learningclient.generate.protobuf.Request.UserUpdateInfoRequest;
import com.corkili.learningclient.generate.protobuf.Response.UserLoginResponse;
import com.corkili.learningclient.generate.protobuf.Response.UserLogoutResponse;
import com.corkili.learningclient.generate.protobuf.Response.UserRegisterResponse;
import com.corkili.learningclient.generate.protobuf.Response.UserUpdateInfoResponse;
import com.corkili.learningclient.network.HttpUtils;

import org.apache.commons.lang3.StringUtils;

public class UserService {

    public static final int REGISTER_MSG = 0x10;
    public static final int LOGIN_MSG = 0x11;
    public static final int MODIFY_USERNAME_MSG = 0x12;
    public static final int MODIFY_PASSWORD_MSG = 0x13;
    public static final int LOGOUT_MSG = 0x14;

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

    public void modifyUsername(final Handler handler, String phone, UserType userType, String newUsername) {
        final Message msg = new Message();
        msg.what = MODIFY_USERNAME_MSG;
        ServiceResult result = null;
        if (StringUtils.isBlank(phone) || userType == null) {
            result = ServiceResult.failResultWithMessage("系统错误");
        } else if (StringUtils.isBlank(newUsername)) {
            result = ServiceResult.failResultWithMessage("新用户名不能为空");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final UserUpdateInfoRequest request = UserUpdateInfoRequest.newBuilder()
                .setPhone(phone)
                .setUserType(userType)
                .setUpdateUsername(true)
                .setNewUsername(newUsername)
                .build();
        AsyncTaskExecutor.execute(() -> {
            UserUpdateInfoResponse response = HttpUtils.request(request,
                    UserUpdateInfoRequest.class, UserUpdateInfoResponse.class, "/user/updateInfo");
            Log.i(TAG, "modifyUsername: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            UserUpdateInfoResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            UserUpdateInfoResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void modifyPassword(final Handler handler, String phone, UserType userType, String newPassword) {
        final Message msg = new Message();
        msg.what = MODIFY_PASSWORD_MSG;
        ServiceResult result = null;
        if (StringUtils.isBlank(phone) || userType == null) {
            result = ServiceResult.failResultWithMessage("系统错误");
        } else if (StringUtils.isBlank(newPassword)) {
            result = ServiceResult.failResultWithMessage("新密码不能为空");
        } else if (newPassword.length() < 6) {
            result = ServiceResult.failResultWithMessage("新密码长度应大于6");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        final UserUpdateInfoRequest request = UserUpdateInfoRequest.newBuilder()
                .setPhone(phone)
                .setUserType(userType)
                .setUpdatePassword(true)
                .setNewPassword(newPassword)
                .build();
        AsyncTaskExecutor.execute(() -> {
            UserUpdateInfoResponse response = HttpUtils.request(request,
                    UserUpdateInfoRequest.class, UserUpdateInfoResponse.class, "/user/updateInfo");
            Log.i(TAG, "modifyPassword: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            UserUpdateInfoResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            UserUpdateInfoResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void logout(final Handler handler) {
        final Message msg = new Message();
        msg.what = LOGOUT_MSG;
        AsyncTaskExecutor.execute(() -> {
            UserLogoutResponse response = HttpUtils.request(UserLogoutRequest.newBuilder().build(),
                    UserLogoutRequest.class, UserLogoutResponse.class, "/user/logout");
            Log.i(TAG, "logout: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            UserLogoutResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            UserLogoutResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

}
