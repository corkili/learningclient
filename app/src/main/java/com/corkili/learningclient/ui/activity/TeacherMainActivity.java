package com.corkili.learningclient.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.ui.fragment.MessageFragment;
import com.corkili.learningclient.ui.fragment.QuestionFragment;
import com.corkili.learningclient.ui.fragment.TeacherCourseFragment;
import com.corkili.learningclient.ui.fragment.UserFragment;

public class TeacherMainActivity extends AppCompatActivity
        implements UserFragment.OnUserInfoChangeListener, TeacherCourseFragment.DataBus, MessageFragment.DataBus {

    private UserInfo userInfo;

    private UserFragment userFragment;
    private TeacherCourseFragment teacherCourseFragment;
    private QuestionFragment questionFragment;
    private MessageFragment messageFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_main);

        Intent intent = getIntent();
        userInfo = (UserInfo) intent.getSerializableExtra(IntentParam.USER_INFO);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_course_manager:
                    setTitle("我的课程");
                    loadCourseFragment();
                    return true;
                case R.id.navigation_question_manager:
                    setTitle("试题仓库");
                    loadQuestionFragment();
                    return true;
                case R.id.navigation_message_manager:
                    setTitle("我的消息");
                    loadMessageFragment();
                    return true;
                case R.id.navigation_user_info:
                    setTitle("个人信息");
                    loadUserFragment();
                    return true;
            }
            return false;
        });
        navigation.setSelectedItemId(R.id.navigation_course_manager);
    }

    private void showFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private synchronized void loadCourseFragment() {
        if (teacherCourseFragment == null) {
            teacherCourseFragment = TeacherCourseFragment.newInstance();
        }
        showFragment(teacherCourseFragment);
    }

    private synchronized void loadQuestionFragment() {
        if (questionFragment == null) {
            questionFragment = QuestionFragment.newInstance();
        }
        showFragment(questionFragment);
    }

    private synchronized void loadMessageFragment() {
        if (messageFragment == null) {
            messageFragment = MessageFragment.newInstance();
        }
        showFragment(messageFragment);
    }

    private synchronized void loadUserFragment() {
        if (userFragment == null) {
            userFragment = UserFragment.newInstance(userInfo);
        }
        showFragment(userFragment);
    }

    @Override
    public void onUserInfoChange(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    public UserInfo getUserInfoFromActivity() {
        return userInfo;
    }

}
