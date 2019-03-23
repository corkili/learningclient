package com.corkili.learningclient.ui.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.UserUpdateInfoResponse;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.service.UserService;
import com.corkili.learningclient.ui.activity.LoginActivity;

public class UserFragment extends Fragment {

    private static final String TAG = "UserFragment";

    private OnUserInfoChangeListener onUserInfoChangeListener;

    private TextView usernameTextView;
    private EditText usernameEditText;
    private EditText phoneEditText;
    private EditText userTypeEditText;
    private Button modifyPasswordButton;
    private Button logoutButton;

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
        usernameTextView = view.findViewById(R.id.user_fragment_text_view_username);
        usernameEditText = view.findViewById(R.id.user_fragment_text_edit_username);
        phoneEditText = view.findViewById(R.id.user_fragment_text_edit_phone);
        userTypeEditText = view.findViewById(R.id.user_fragment_text_edit_user_type);
        modifyPasswordButton = view.findViewById(R.id.user_fragment_button_modify_password);
        logoutButton = view.findViewById(R.id.user_fragment_button_logout);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        usernameEditText.setText(userInfo.getUsername());
        phoneEditText.setText(userInfo.getPhone());

        if (userInfo.getUserType() == UserType.Teacher) {
            userTypeEditText.setText("老师");
        } else {
            userTypeEditText.setText("学生");
        }

        usernameTextView.setOnClickListener(view -> {
            if (view.getId() != usernameTextView.getId()) {
                return;
            }
            showModifyUsernameDialog();
        });

        modifyPasswordButton.setOnClickListener(view -> {
            if (view.getId() != modifyPasswordButton.getId()) {
                return;
            }
            showModifyPasswordDialog();
        });

        logoutButton.setOnClickListener(view -> {
            if (view.getId() != logoutButton.getId()) {
                return;
            }
            showLogoutDialog();
        });
    }

    private void showModifyUsernameDialog() {
        final EditText editText = new EditText(getActivity());
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(getActivity());
        inputDialog.setTitle("修改用户名").setView(editText);
        inputDialog
                .setPositiveButton("确定", (dialog, which) -> {
                    String username = editText.getText().toString();
                    UserService.getInstance().modifyUsername(handler,
                            userInfo.getPhone(), userInfo.getUserType(), username);
                })
                .setNegativeButton("取消", ((dialog, which) -> dialog.cancel()))
                .show();
    }

    private void showModifyPasswordDialog() {
        final EditText editText = new EditText(getActivity());
        editText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(getActivity());
        inputDialog.setTitle("修改密码").setView(editText);
        inputDialog
                .setPositiveButton("确定", (dialog, which) -> {
                    String password = editText.getText().toString();
                    UserService.getInstance().modifyPassword(handler,
                            userInfo.getPhone(), userInfo.getUserType(), password);
                })
                .setNegativeButton("取消", ((dialog, which) -> dialog.cancel()))
                .show();
    }

    private void showLogoutDialog() {
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(getActivity());
        inputDialog.setTitle("注销").setMessage("是否确定注销");
        inputDialog
                .setPositiveButton("确定", (dialog, which) -> UserService.getInstance().logout(handler))
                .setNegativeButton("取消", ((dialog, which) -> dialog.cancel()))
                .show();
    }

    private void handleModifyUsernameMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(getActivity(), serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            UserUpdateInfoResponse response = serviceResult.extra(UserUpdateInfoResponse.class);
            refreshUserInfo(response.getUserInfo());
            usernameEditText.setText(userInfo.getUsername());
        }
    }

    private void handleModifyPasswordMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(getActivity(), serviceResult.msg(), Toast.LENGTH_SHORT).show();
    }

    private void handleLogoutMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(getActivity(), serviceResult.msg(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        startActivity(intent);
        getActivity().finish();
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
