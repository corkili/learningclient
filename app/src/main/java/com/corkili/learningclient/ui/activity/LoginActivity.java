package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.SPParam;
import com.corkili.learningclient.common.UIHelper;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.UserLoginResponse;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.service.UserService;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

import org.apache.commons.lang3.StringUtils;

public class LoginActivity extends AppCompatActivity {

    private QMUITopBarLayout topBar;
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
        topBar = findViewById(R.id.topbar);
        phoneEditText = findViewById(R.id.text_edit_phone);
        passwordEditText = findViewById(R.id.text_edit_password);
        userTypeRadioGroup = findViewById(R.id.radio_group_user_type);
        loginButton = findViewById(R.id.button_login);
        toRegisterButton = findViewById(R.id.button_to_register);

        SharedPreferences sp = getSharedPreferences(SPParam.SP_NAME_USER_INFO, MODE_PRIVATE);
        String phone = sp.getString(SPParam.SP_KEY_PHONE, "");
        String userType = sp.getString(SPParam.SP_KEY_USER_TYPE, "");

        String password;
        if (SPParam.SP_VALUE_USER_TYPE_STUDENT.equals(userType)) {
            userTypeRadioGroup.check(R.id.user_type_student);
            password = sp.getString(SPParam.SP_KEY_PASSWORD_FOR_STUDENT, "");
        } else {
            userTypeRadioGroup.check(R.id.user_type_teacher);
            password = sp.getString(SPParam.SP_KEY_PASSWORD_FOR_TEACHER, "");
        }
        phoneEditText.setText(phone);
        passwordEditText.setText(password);

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
                passwordEditText.setText("");
            }
        }
        initListener();
        topBar.setTitle("登录");
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

        phoneEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String phoneNow = phoneEditText.getText().toString().trim();
                SharedPreferences sp = getSharedPreferences(SPParam.SP_NAME_USER_INFO, MODE_PRIVATE);
                String phone = sp.getString(SPParam.SP_KEY_PHONE, "");
                if (StringUtils.isBlank(phone)) {
                    return;
                }
                if (!phone.equals(phoneNow)) {
                    passwordEditText.setText("");
                } else {
                    String password;
                    if (userTypeRadioGroup.getCheckedRadioButtonId() == R.id.user_type_student) {
                        password = sp.getString(SPParam.SP_KEY_PASSWORD_FOR_STUDENT, "");
                    } else {
                        password = sp.getString(SPParam.SP_KEY_PASSWORD_FOR_TEACHER, "");
                    }
                    passwordEditText.setText(password);
                }
            }
        });

        userTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String phoneNow = phoneEditText.getText().toString().trim();
            SharedPreferences sp = getSharedPreferences(SPParam.SP_NAME_USER_INFO, MODE_PRIVATE);
            String phone = sp.getString(SPParam.SP_KEY_PHONE, "");
            if (phone != null && !phone.equals(phoneNow)) {
                return;
            }
            String password = "";
            if (checkedId == R.id.user_type_student) {
                password = sp.getString(SPParam.SP_KEY_PASSWORD_FOR_STUDENT, "");
            } else if (checkedId == R.id.user_type_teacher) {
                password = sp.getString(SPParam.SP_KEY_PASSWORD_FOR_TEACHER, "");
            }
            passwordEditText.setText(password);
        });
    }

    private void handleLoginMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        if (serviceResult.isSuccess()) {
            UserInfo userInfo = serviceResult.extra(UserLoginResponse.class).getUserInfo();
            SharedPreferences sp = getSharedPreferences(SPParam.SP_NAME_USER_INFO, MODE_PRIVATE);
            Editor editor = sp.edit();
            editor.putString(SPParam.SP_KEY_PHONE, phoneEditText.getText().toString().trim());
            Intent intent = new Intent();
            if (userInfo.getUserType() == UserType.Teacher) {
                editor.putString(SPParam.SP_KEY_PASSWORD_FOR_TEACHER, passwordEditText.getText().toString().trim());
                editor.putString(SPParam.SP_KEY_USER_TYPE, SPParam.SP_VALUE_USER_TYPE_TEACHER);
                intent.setClass(LoginActivity.this, TeacherMainActivity.class);
            } else {
                editor.putString(SPParam.SP_KEY_PASSWORD_FOR_STUDENT, passwordEditText.getText().toString().trim());
                editor.putString(SPParam.SP_KEY_USER_TYPE, SPParam.SP_VALUE_USER_TYPE_STUDENT);
                intent.setClass(LoginActivity.this, StudentMainActivity.class);
            }
            editor.apply();
            intent.putExtra(IntentParam.USER_INFO, userInfo);
            startActivity(intent);
            LoginActivity.this.finish();
        } else {
            UIHelper.toast(this, serviceResult, raw -> "用户名或密码错误");
        }
    }

}
