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
import com.corkili.learningclient.ui.fragment.TeacherCourseFragment;
import com.corkili.learningclient.ui.fragment.UserFragment;

public class TeacherMainActivity extends AppCompatActivity
        implements UserFragment.OnUserInfoChangeListener, TeacherCourseFragment.DataBus {

    private UserInfo userInfo;

    private UserFragment userFragment;
    private TeacherCourseFragment teacherCourseFragment;

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
                    loadCourseFragment();
                    return true;
                case R.id.navigation_question_manager:
                    loadQuestionFragment();
                    return true;
                case R.id.navigation_message_manager:
                    loadMessageFragment();
                    return true;
                case R.id.navigation_user_info:
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

    private void loadCourseFragment() {
        if (teacherCourseFragment == null) {
            teacherCourseFragment = TeacherCourseFragment.newInstance();
        }
        showFragment(teacherCourseFragment);
    }

    private void loadQuestionFragment() {

    }

    private void loadMessageFragment() {

    }

    private void loadUserFragment() {
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
