package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseSubscriptionInfo;
import com.corkili.learningclient.generate.protobuf.Response.CourseSubscriptionFindAllResponse;
import com.corkili.learningclient.service.CourseSubscriptionService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.ui.other.MyRecyclerViewDivider;

import java.util.ArrayList;
import java.util.List;

public class TeacherCourseSubscriptionActivity extends AppCompatActivity {

    private static final String SUBSCRIPTION_NUMBER_TEXT_FORMAT = "当前共有{}位订阅者";

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView subscriptionNumberTextView;

    private CourseInfo courseInfo;
    private List<CourseSubscriptionInfo> courseSubscriptionInfos;

    private TeacherCourseSubscriptionRecyclerViewAdapter recyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_course_subscription);
        courseInfo = (CourseInfo) getIntent().getSerializableExtra(IntentParam.COURSE_INFO);
        if (courseInfo == null) {
            throw new RuntimeException("Intent param lost");
        }
        recyclerView = findViewById(R.id.activity_teacher_course_subscription_list);
        swipeRefreshLayout = findViewById(R.id.activity_teacher_course_subscription_swipe_refresh_layout);
        subscriptionNumberTextView = findViewById(R.id.text_view_current_subscription_number);
        courseSubscriptionInfos = new ArrayList<>();
        subscriptionNumberTextView.setText(IUtils.format(SUBSCRIPTION_NUMBER_TEXT_FORMAT, courseSubscriptionInfos.size()));
        recyclerViewAdapter = new TeacherCourseSubscriptionRecyclerViewAdapter(this, courseSubscriptionInfos);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addItemDecoration(new MyRecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL,
                1,ContextCompat.getColor(this,R.color.colorBlack)));
        swipeRefreshLayout.setOnRefreshListener(this::refreshCourseSubscriptionInfos);
        refreshCourseSubscriptionInfos();
    }

    private void refreshCourseSubscriptionInfos() {
        CourseSubscriptionService.getInstance().findAllCourseSubscription(handler, null, courseInfo.getCourseId());
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CourseSubscriptionService.FIND_ALL_COURSE_SUBSCRIPTION_MSG) {
                handleFindAllCourseCommentMsg(msg);
            }
        }
    };

    private void handleFindAllCourseCommentMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            courseSubscriptionInfos.clear();
            courseSubscriptionInfos.addAll(serviceResult.extra(CourseSubscriptionFindAllResponse.class).getCourseSubscriptionInfoList());
            swipeRefreshLayout.setRefreshing(false);
            subscriptionNumberTextView.setText(IUtils.format(SUBSCRIPTION_NUMBER_TEXT_FORMAT, courseSubscriptionInfos.size()));
            recyclerViewAdapter.notifyDataSetChanged();
        }
    }
}

