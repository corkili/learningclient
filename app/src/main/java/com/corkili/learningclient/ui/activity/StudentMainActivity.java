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
import com.corkili.learningclient.ui.fragment.StudentCourseFragment;
import com.corkili.learningclient.ui.fragment.UserFragment;

public class StudentMainActivity extends AppCompatActivity
        implements UserFragment.OnUserInfoChangeListener, MessageFragment.DataBus, StudentCourseFragment.DataBus {

    private UserInfo userInfo;

    private UserFragment userFragment;
    private MessageFragment messageFragment;
    private StudentCourseFragment courseFragment;
    private StudentCourseFragment subscriptionFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_main);

        Intent intent = getIntent();
        userInfo = (UserInfo) intent.getSerializableExtra(IntentParam.USER_INFO);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_course:
                    loadCourseFragment();
                    return true;
                case R.id.navigation_subscription:
                    loadSubscriptionFragment();
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
        navigation.setSelectedItemId(R.id.navigation_subscription);
    }

    private void showFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private synchronized void loadCourseFragment() {
        if (courseFragment == null) {
            courseFragment = StudentCourseFragment.newInstance(false);
        }
        showFragment(courseFragment);
    }

    private synchronized void loadSubscriptionFragment() {
        if (courseFragment == null) {
            subscriptionFragment = StudentCourseFragment.newInstance(true);
        }
        showFragment(subscriptionFragment);
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
