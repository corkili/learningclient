package com.corkili.learningclient.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.UIHelper;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.UserUpdateInfoResponse;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.service.UserService;
import com.corkili.learningclient.ui.activity.LoginActivity;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;

public class UserFragment extends Fragment {

    private OnUserInfoChangeListener onUserInfoChangeListener;

    private QMUICommonListItemView usernameItem;
    private QMUICommonListItemView phoneItem;
    private QMUICommonListItemView userTypeItem;
    private QMUICommonListItemView passwordItem;
    private QMUICommonListItemView logoutItem;

    private QMUIGroupListView userInfoListView;

    private UserInfo userInfo;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == UserService.MODIFY_USERNAME_MSG) {
                handleModifyUsernameMsg(msg);
            } else if (msg.what == UserService.MODIFY_PASSWORD_MSG) {
                handleModifyPasswordMsg(msg);
            } else if (msg.what == UserService.LOGOUT_MSG) {
                handleLogoutMsg(msg);
            }
        }
    };

    public UserFragment() {
        // Required empty public constructor
    }


    public static UserFragment newInstance(UserInfo userInfo) {
        UserFragment fragment = new UserFragment();
        Bundle args = new Bundle();
        args.putSerializable(IntentParam.USER_INFO, userInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userInfo = (UserInfo) getArguments().getSerializable(IntentParam.USER_INFO);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_user, container, false);
        userInfoListView = view.findViewById(R.id.user_info_list);

        usernameItem = userInfoListView.createItemView(
                ContextCompat.getDrawable(getContext(), R.drawable.ic_user_24dp),
                "用户名",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);

        phoneItem = userInfoListView.createItemView(
                ContextCompat.getDrawable(getContext(), R.drawable.ic_phone_24dp),
                "手机号",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        userTypeItem = userInfoListView.createItemView(
                ContextCompat.getDrawable(getContext(), R.drawable.ic_usertype_24dp),
                "类型",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        passwordItem = userInfoListView.createItemView(
                ContextCompat.getDrawable(getContext(), R.drawable.ic_password_24dp),
                "修改密码",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);

        logoutItem = userInfoListView.createItemView(
                ContextCompat.getDrawable(getContext(), R.drawable.ic_logout_24dp),
                "退出登录",
                null,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);

        int size = QMUIDisplayHelper.dp2px(getContext(), 24);

        QMUIGroupListView.newSection(getContext())
                .setTitle("基本信息")
                .setLeftIconSize(size, ViewGroup.LayoutParams.WRAP_CONTENT)
                .addItemView(usernameItem, null)
                .addItemView(phoneItem, null)
                .addItemView(userTypeItem, null)
                .addTo(userInfoListView);

        QMUIGroupListView.newSection(getContext())
                .setTitle("账号操作")
                .setLeftIconSize(size, ViewGroup.LayoutParams.WRAP_CONTENT)
                .addItemView(passwordItem, null)
                .addItemView(logoutItem, null)
                .addTo(userInfoListView);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        usernameItem.setDetailText(userInfo.getUsername());
        phoneItem.setDetailText(userInfo.getPhone());

        if (userInfo.getUserType() == UserType.Teacher) {
            userTypeItem.setDetailText("老师");
        } else {
            userTypeItem.setDetailText("学生");
        }

        usernameItem.setOnClickListener(view -> showModifyUsernameDialog());

        passwordItem.setOnClickListener(view -> showModifyPasswordDialog());

        logoutItem.setOnClickListener(view -> showLogoutDialog());
    }

    private void showModifyUsernameDialog() {
        final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(getActivity());
        builder.setTitle("修改用户名")
                .setPlaceholder("请输入新的用户名")
                .setInputType(InputType.TYPE_CLASS_TEXT)
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("确定", (dialog, index) -> {
                    CharSequence username = builder.getEditText().getText();
                    if (username != null && username.length() > 0 && !username.toString().equals(userInfo.getUsername())) {
                        UserService.getInstance().modifyUsername(handler,
                                userInfo.getPhone(), userInfo.getUserType(), username.toString());
                    }
                    dialog.dismiss();
                })
                .show();
    }

    private void showModifyPasswordDialog() {
        final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(getActivity());
        builder.setTitle("修改密码")
                .setPlaceholder("请输入新密码")
                .setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction("确定", (dialog, index) -> {
                    CharSequence password = builder.getEditText().getText();
                    if (password != null && password.length() > 0) {
                        UserService.getInstance().modifyPassword(handler,
                                userInfo.getPhone(), userInfo.getUserType(), password.toString());
                    }
                    dialog.dismiss();
                })
                .show();
    }

    private void showLogoutDialog() {
        new QMUIDialog.MessageDialogBuilder(getActivity())
                .setTitle("注销")
                .setMessage("确定退出登录吗？")
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction(0, "注销", QMUIDialogAction.ACTION_PROP_NEGATIVE, (dialog, index) -> {
                    UserService.getInstance().logout(handler);
                    dialog.dismiss();
                })
                .show();
    }

    private void handleModifyUsernameMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        UIHelper.toast(getActivity(), serviceResult, raw -> serviceResult.isSuccess() ? "修改用户名成功" : "修改用户名失败");
        if (serviceResult.isSuccess()) {
            UserUpdateInfoResponse response = serviceResult.extra(UserUpdateInfoResponse.class);
            refreshUserInfo(response.getUserInfo());
            usernameItem.setDetailText(userInfo.getUsername());
        }
    }

    private void handleModifyPasswordMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        UIHelper.toast(getActivity(), serviceResult, raw -> serviceResult.isSuccess() ? "修改用户名成功" : "修改用户名失败");
    }

    private void handleLogoutMsg(Message msg) {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void refreshUserInfo(UserInfo newUserInfo) {
        if (onUserInfoChangeListener != null) {
            onUserInfoChangeListener.onUserInfoChange(newUserInfo);
        }
        this.userInfo = newUserInfo;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnUserInfoChangeListener) {
            this.onUserInfoChangeListener = (OnUserInfoChangeListener) context;
        } else {
            throw new RuntimeException("Activities must implement UserFragment.OnUserInfoChangeListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.onUserInfoChangeListener = null;
    }

    public interface OnUserInfoChangeListener {
        void onUserInfoChange(UserInfo userInfo);
    }
}
