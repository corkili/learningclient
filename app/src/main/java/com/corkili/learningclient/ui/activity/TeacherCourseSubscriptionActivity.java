package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.UIHelper;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseSubscriptionInfo;
import com.corkili.learningclient.generate.protobuf.Response.CourseSubscriptionFindAllResponse;
import com.corkili.learningclient.service.CourseSubscriptionService;
import com.corkili.learningclient.service.ServiceResult;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView.Section;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TeacherCourseSubscriptionActivity extends AppCompatActivity {

    private static final String SUBSCRIPTION_NUMBER_TEXT_FORMAT = "当前共有{}位订阅者";

    private QMUITopBarLayout topBar;
    private QMUIGroupListView courseSubscriberListView;

    private CourseInfo courseInfo;
    private List<CourseSubscriptionInfo> courseSubscriptionInfos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_course_subscription);

        courseInfo = (CourseInfo) getIntent().getSerializableExtra(IntentParam.COURSE_INFO);
        if (courseInfo == null) {
            throw new RuntimeException("Intent param lost");
        }

        topBar = findViewById(R.id.topbar);
        topBar.setTitle("订阅列表");
        topBar.addLeftBackImageButton().setOnClickListener(v -> TeacherCourseSubscriptionActivity.this.finish());
        topBar.addRightImageButton(R.drawable.ic_refresh_24dp, R.id.topbar_right_refresh)
                .setOnClickListener(v -> refreshCourseSubscriptionInfos());

        courseSubscriberListView = findViewById(R.id.course_subscriber_list);

        courseSubscriptionInfos = new ArrayList<>();
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
                handleFindAllCourseSubscriptionMsg(msg);
            }
        }
    };

    private void handleFindAllCourseSubscriptionMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        if (serviceResult.isSuccess()) {
            courseSubscriptionInfos.clear();
            courseSubscriptionInfos.addAll(serviceResult.extra(CourseSubscriptionFindAllResponse.class).getCourseSubscriptionInfoList());
            courseSubscriberListView.removeAllViews();
            int size = QMUIDisplayHelper.dp2px(this, 24);
            Section section = QMUIGroupListView.newSection(this)
                    .setLeftIconSize(size, ViewGroup.LayoutParams.WRAP_CONTENT);
            for (CourseSubscriptionInfo courseSubscriptionInfo : courseSubscriptionInfos) {
                QMUICommonListItemView itemView = courseSubscriberListView.createItemView(
                        ContextCompat.getDrawable(this, R.drawable.ic_user_24dp),
                        courseSubscriptionInfo.getSubscriberInfo().getUsername(),
                        IUtils.format("订阅时间:{}", IUtils.DATE_TIME_FORMATTER.format(
                                new Date(courseSubscriptionInfo.getCreateTime()))),
                        QMUICommonListItemView.VERTICAL,
                        QMUICommonListItemView.ACCESSORY_TYPE_CHEVRON);
                section.addItemView(itemView, v -> onItemClick(courseSubscriptionInfo));
            }
            String title;
            if (!courseSubscriptionInfos.isEmpty()) {
                title = IUtils.format(SUBSCRIPTION_NUMBER_TEXT_FORMAT, courseSubscriptionInfos.size());
                section.setDescription("点击订阅者可以发送消息哦~");
            } else {
                title = "当前没有订阅者";
            }
            section.setTitle(title);
            section.addTo(courseSubscriberListView);
        } else {
            UIHelper.toast(this, serviceResult, raw -> "加载课程订阅信息失败");
        }
    }

    private void onItemClick(CourseSubscriptionInfo courseSubscriptionInfo) {
        Intent intent = new Intent(TeacherCourseSubscriptionActivity.this, MessageActivity.class);
        intent.putExtra(IntentParam.USER_INFO, courseSubscriptionInfo.getSubscriberInfo());
        intent.putExtra(IntentParam.SELF_USER_INFO, courseInfo.getTeacherInfo());
        intent.putExtra(IntentParam.COUNT, 0);
        startActivity(intent);
    }
}

