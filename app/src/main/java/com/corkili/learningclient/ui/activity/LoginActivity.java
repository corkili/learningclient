package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.UserLoginResponse;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.service.UserService;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

public class LoginActivity extends AppCompatActivity {

    private EditText phoneEditText;
    private EditText passwordEditText;
    private RadioGroup userTypeRadioGroup;
    private QMUIRoundButton loginButton;
    private QMUIRoundButton toRegisterButton;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == UserService.LOGIN_MSG) {
                handleLoginMsg(msg);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        phoneEditText = findViewById(R.id.text_edit_phone);
        passwordEditText = findViewById(R.id.text_edit_password);
        userTypeRadioGroup = findViewById(R.id.radio_group_user_type);
        loginButton = findViewById(R.id.button_login);
        toRegisterButton = findViewById(R.id.button_to_register);
        Intent intent = getIntent();
        if (intent != null) {
            UserInfo userInfo = (UserInfo) intent.getSerializableExtra(IntentParam.USER_INFO);
            if (userInfo != null) {
                phoneEditText.setText(userInfo.getPhone());
                if (userInfo.getUserType() == UserType.Teacher) {
                    userTypeRadioGroup.check(R.id.user_type_teacher);
                } else {
                    userTypeRadioGroup.check(R.id.user_type_student);
                }
            }
        }
        initListener();
    }

    private void initListener() {
        loginButton.setOnClickListener(view -> {
            if (view.getId() != loginButton.getId()) {
                return;
            }
            String phone = phoneEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            UserType userType = UserType.Teacher;
            if (userTypeRadioGroup.getCheckedRadioButtonId() == R.id.user_type_student) {
                userType = UserType.Student;
            }
            UserService.getInstance().login(handler, phone, password, userType);
        });

        toRegisterButton.setOnClickListener(view -> {
            if (view.getId() != toRegisterButton.getId()) {
                return;
            }
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            LoginActivity.this.finish();
        });
    }

    private void handleLoginMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(LoginActivity.this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            UserInfo userInfo = serviceResult.extra(UserLoginResponse.class).getUserInfo();
            Intent intent = new Intent();
            if (userInfo.getUserType() == UserType.Teacher) {
                intent.setClass(LoginActivity.this, TeacherMainActivity.class);
            } else {
                intent.setClass(LoginActivity.this, StudentMainActivity.class);
            }
            intent.putExtra(IntentParam.USER_INFO, userInfo);
            startActivity(intent);
            LoginActivity.this.finish();
        }
    }

}
