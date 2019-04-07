package com.corkili.learningclient.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseSubscriptionInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Response.CourseFindAllResponse;
import com.corkili.learningclient.generate.protobuf.Response.CourseSubscriptionFindAllResponse;
import com.corkili.learningclient.service.CourseService;
import com.corkili.learningclient.service.CourseSubscriptionService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.ui.activity.StudentCourseDetailActivity;
import com.corkili.learningclient.ui.adapter.StudentCourseRecyclerViewAdapter;
import com.corkili.learningclient.ui.adapter.StudentCourseRecyclerViewAdapter.ViewHolder;
import com.qmuiteam.qmui.widget.pullRefreshLayout.QMUIPullRefreshLayout;
import com.qmuiteam.qmui.widget.pullRefreshLayout.QMUIPullRefreshLayout.OnPullListener;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class StudentCourseFragment extends Fragment implements StudentCourseRecyclerViewAdapter.OnItemInteractionListener {

    public static final int REQUEST_CODE_COURSE_DETAIL = 0xF1;
    
    private static final String PARAM_ONLY_SUBSCRIBED_COURSE = "onlySubscribedCourse";

    private boolean onlySubscribedCourse;
    
    private RecyclerView recyclerView;
    private QMUIPullRefreshLayout swipeRefreshLayout;
    private TextView tipView;

    private StudentCourseRecyclerViewAdapter recyclerViewAdapter;

    private List<CourseInfo> courseInfos;
    private List<CourseSubscriptionInfo> courseSubscriptionInfos;

    private DataBus dataBus;

    private boolean shouldFinishRefresh;

    public StudentCourseFragment() {
        shouldFinishRefresh = false;
    }

    public static StudentCourseFragment newInstance(boolean onlySubscribedCourse) {
        StudentCourseFragment fragment = new StudentCourseFragment();
        Bundle args = new Bundle();
        args.putBoolean(PARAM_ONLY_SUBSCRIBED_COURSE, onlySubscribedCourse);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.onlySubscribedCourse = getArguments().getBoolean(PARAM_ONLY_SUBSCRIBED_COURSE, true);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_course, container, false);
        tipView = view.findViewById(R.id.tip);
        recyclerView = view.findViewById(R.id.fragment_student_course_list);
        swipeRefreshLayout = view.findViewById(R.id.fragment_student_course_swipe_refresh_layout);
        courseInfos = new ArrayList<>();
        courseSubscriptionInfos = new ArrayList<>();
        recyclerViewAdapter = new StudentCourseRecyclerViewAdapter(getActivity(), courseInfos, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
//        recyclerView.addItemDecoration(new MyRecyclerViewDivider(getActivity(), LinearLayoutManager.HORIZONTAL,
//                1,ContextCompat.getColor(getActivity(),R.color.colorBlack)));
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        swipeRefreshLayout.setOnPullListener(new OnPullListener() {
            @Override
            public void onMoveTarget(int offset) {

            }

            @Override
            public void onMoveRefreshView(int offset) {

            }

            @Override
            public void onRefresh() {
                shouldFinishRefresh = true;
                refreshCourseInfos();
            }
        });

        refreshCourseInfos();
    }

    private void updateTipView() {
        if (courseInfos.isEmpty()) {
            tipView.setVisibility(View.VISIBLE);
        } else {
            tipView.setVisibility(View.GONE);
        }
    }

    private void refreshCourseInfos() {
        if (!onlySubscribedCourse) {
            CourseService.getInstance().findAllCourse(handler, true, false, null, null, null);
        }
        CourseSubscriptionService.getInstance().findAllCourseSubscription(handler,
                dataBus.getUserInfoFromActivity().getUserId(), null);
    }

    private CourseSubscriptionInfo findSubscription(long userId, long courseId) {
        for (CourseSubscriptionInfo courseSubscriptionInfo : courseSubscriptionInfos) {
            if (courseSubscriptionInfo.getSubscriberInfo().getUserId() == userId &&
                    courseSubscriptionInfo.getSubscribedCourseInfo().getCourseId() == courseId) {
                return courseSubscriptionInfo;
            }
        }
        return null;
    }

    private CourseSubscriptionInfo findSubscription(long subscriptionId) {
        for (CourseSubscriptionInfo courseSubscriptionInfo : courseSubscriptionInfos) {
            if (courseSubscriptionInfo.getCourseSubscriptionId() == subscriptionId) {
                return courseSubscriptionInfo;
            }
        }
        return null;
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CourseService.FIND_ALL_COURSE_MSG) {
                handleFindAllCourseMsg(msg);
            } else if (msg.what == CourseSubscriptionService.FIND_ALL_COURSE_SUBSCRIPTION_MSG) {
                handleFindAllSubscribedCourseMsg(msg);
            }
        }
    };

    private void handleFindAllCourseMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(getActivity(), serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess() && !onlySubscribedCourse) {
            courseInfos.clear();
            courseInfos.addAll(serviceResult.extra(CourseFindAllResponse.class).getCourseInfoList());
            recyclerViewAdapter.notifyDataSetChanged();
        }
        if (shouldFinishRefresh) {
            shouldFinishRefresh = false;
            swipeRefreshLayout.finishRefresh();
        }
        updateTipView();
    }

    private void handleFindAllSubscribedCourseMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(getActivity(), serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            courseSubscriptionInfos.clear();
            courseSubscriptionInfos.addAll(serviceResult.extra(
                    CourseSubscriptionFindAllResponse.class).getCourseSubscriptionInfoList());
            if (onlySubscribedCourse) {
                courseInfos.clear();
                for (CourseSubscriptionInfo courseSubscriptionInfo : courseSubscriptionInfos) {
                    courseInfos.add(courseSubscriptionInfo.getSubscribedCourseInfo());
                }
                recyclerViewAdapter.notifyDataSetChanged();
            }
        }
        if (shouldFinishRefresh) {
            shouldFinishRefresh = false;
            swipeRefreshLayout.finishRefresh();
        }
        updateTipView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        CourseInfo courseInfo = (CourseInfo) data.getSerializableExtra(IntentParam.COURSE_INFO);
        if (requestCode == REQUEST_CODE_COURSE_DETAIL && courseInfo != null) {
            CourseSubscriptionInfo courseSubscriptionInfo =  (CourseSubscriptionInfo) data
                    .getSerializableExtra(IntentParam.COURSE_SUBSCRIPTION_INFO);
            if (courseSubscriptionInfo != null) {
                CourseSubscriptionInfo subscriptionById = findSubscription(courseSubscriptionInfo.getCourseSubscriptionId());
                if (subscriptionById == null) {
                    CourseSubscriptionInfo subscriptionByUserAndCourse = findSubscription(courseSubscriptionInfo.getSubscriberInfo().getUserId(),
                            courseSubscriptionInfo.getSubscribedCourseInfo().getCourseId());
                    // create new course subscription
                    if (subscriptionByUserAndCourse != null) {
                        courseSubscriptionInfos.remove(subscriptionByUserAndCourse);
                    }
                    courseSubscriptionInfos.add(courseSubscriptionInfo);
                }
            } else {
                CourseSubscriptionInfo subscriptionInfo = findSubscription(
                        dataBus.getUserInfoFromActivity().getUserId(), courseInfo.getCourseId());
                if (subscriptionInfo != null) {
                    courseSubscriptionInfos.remove(subscriptionInfo);
                }
            }

            int needModifyIndex = -1;
            for (int i = 0; i < courseInfos.size(); i++) {
                CourseInfo info = courseInfos.get(i);
                if (info.getCourseId() == courseInfo.getCourseId()) {
                    needModifyIndex = i;
                    break;
                }
            }
            if (onlySubscribedCourse) {
                if (needModifyIndex >= 0) {
                    if (courseSubscriptionInfo != null) {
                        courseInfos.set(needModifyIndex, courseInfo);
                        recyclerViewAdapter.notifyItemChanged(needModifyIndex);
                    } else {
                        courseInfos.remove(needModifyIndex);
                        recyclerViewAdapter.notifyDataSetChanged();
                    }
                }
            } else {
                if (needModifyIndex >= 0) {
                    courseInfos.set(needModifyIndex, courseInfo);
                    recyclerViewAdapter.notifyItemChanged(needModifyIndex);
                }
            }
        }
        updateTipView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DataBus) {
            this.dataBus = (DataBus) context;
        } else {
            throw new RuntimeException("Activities must implement StudentCourseFragment.DataBus");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.dataBus = null;
    }

    @Override
    public void onItemClick(ViewHolder viewHolder) {
        Intent intent = new Intent(getActivity(), StudentCourseDetailActivity.class);
        intent.putExtra(IntentParam.USER_INFO, dataBus.getUserInfoFromActivity());
        intent.putExtra(IntentParam.COURSE_INFO, viewHolder.getCourseInfo());
        intent.putExtra(IntentParam.COURSE_SUBSCRIPTION_INFO, findSubscription(
                dataBus.getUserInfoFromActivity().getUserId(),
                viewHolder.getCourseInfo().getCourseId()));
        startActivityForResult(intent, REQUEST_CODE_COURSE_DETAIL);
    }

    public interface DataBus {

        UserInfo getUserInfoFromActivity();

    }

}
