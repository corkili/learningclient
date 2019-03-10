package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.UserRegisterResponse;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.service.UserService;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private EditText phoneEditText;
    private EditText passwordEditText;
    private RadioGroup userTypeRadioGroup;
    private EditText usernameEditText;
    private Button registerButton;
    private Button cancelButton;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UserService.REGISTER_MSG:
                    handleRegisterMsg(msg);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        phoneEditText = findViewById(R.id.text_edit_phone);
        passwordEditText = findViewById(R.id.text_edit_password);
        userTypeRadioGroup = findViewById(R.id.radio_group_user_type);
        usernameEditText = findViewById(R.id.text_edit_username);
        registerButton = findViewById(R.id.button_register);
        cancelButton = findViewById(R.id.button_cancel);
        initListener();
    }

    private void initListener() {
        registerButton.setOnClickListener(view -> {
            if (view.getId() != registerButton.getId()) {
                return;
            }
            String phone = phoneEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            UserType userType = UserType.Teacher;
            if (userTypeRadioGroup.getCheckedRadioButtonId() == R.id.user_type_student) {
                userType = UserType.Student;
            }
            String username = usernameEditText.getText().toString().trim();
            UserService.getInstance().register(handler, phone, password, userType, username);
        });

        cancelButton.setOnClickListener(view -> {
            if (view.getId() != cancelButton.getId()) {
                return;
            }
            Intent intent = new Intent();
            intent.setClass(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            RegisterActivity.this.finish();
        });
    }

    private void handleRegisterMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(RegisterActivity.this, serviceResult.msg(), Toast.LENGTH_LONG).show();
        if (serviceResult.isSuccess()) {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.putExtra("userInfo", serviceResult.extra(UserRegisterResponse.class).getUserInfo());
            startActivity(intent);
            RegisterActivity.this.finish();
        }
    }

}
