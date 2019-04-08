package com.corkili.learningclient.ui.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.corkili.learningclient.R;
import com.corkili.learningclient.ui.fragment.QuestionFragment;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;

public class QuestionSelectActivity extends AppCompatActivity {

    private QMUITopBarLayout topBar;
    private QuestionFragment questionFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_select);

        topBar = findViewById(R.id.topbar);

        topBar.setTitle("长按选择试题");

        topBar.addLeftBackImageButton().setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        loadQuestionFragment();

    }

    private void loadQuestionFragment() {
        if (questionFragment == null) {
            questionFragment = QuestionFragment.newInstance();
        }
        showFragment(questionFragment);
    }

    private void showFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}
