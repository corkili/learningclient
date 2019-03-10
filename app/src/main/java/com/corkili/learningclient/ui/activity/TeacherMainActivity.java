package com.corkili.learningclient.ui.activity;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.corkili.learningclient.R;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.ui.fragment.UserFragment;

public class TeacherMainActivity extends AppCompatActivity
        implements UserFragment.OnFragmentInteractionListener {

    private UserFragment userFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_main);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        Resources resource = getBaseContext().getResources();
        ColorStateList csl = resource.getColorStateList(R.color.navigation_menu_item_color);
        navigation.setItemTextColor(csl);
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
        navigation.setSelectedItemId(R.id.navigation_user_info);
    }

    private void loadCourseFragment() {

    }

    private void loadQuestionFragment() {

    }

    private void loadMessageFragment() {

    }

    private void loadUserFragment() {
        UserFragment fragment;
        if (userFragment == null) {
            fragment = UserFragment.newInstance(UserInfo.getDefaultInstance());
        } else {
            fragment = userFragment;
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
