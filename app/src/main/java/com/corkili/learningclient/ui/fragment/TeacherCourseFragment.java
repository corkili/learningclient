package com.corkili.learningclient.ui.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Response.CourseFindAllResponse;
import com.corkili.learningclient.service.CourseService;
import com.corkili.learningclient.service.ServiceResult;

import java.util.ArrayList;
import java.util.List;

public class TeacherCourseFragment extends Fragment {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton createCourseFab;

    private TeacherCourseRecyclerViewAdapter recyclerViewAdapter;

    private List<CourseInfo> courseInfos;

    public TeacherCourseFragment() {
    }

    public static TeacherCourseFragment newInstance() {
        TeacherCourseFragment fragment = new TeacherCourseFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_course, container, false);
        recyclerView = view.findViewById(R.id.fragment_teacher_course_list);
        swipeRefreshLayout = view.findViewById(R.id.fragment_teacher_course_swipe_refresh_layout);
        createCourseFab = view.findViewById(R.id.fab_create_course);
        courseInfos = new ArrayList<>();
        recyclerViewAdapter = new TeacherCourseRecyclerViewAdapter(getActivity(), courseInfos);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        swipeRefreshLayout.setOnRefreshListener(this::refreshCourseInfos);

        createCourseFab.setOnClickListener(view -> {
            // TODO 跳转 课程信息编辑Activity
        });

        refreshCourseInfos();
    }

    private void refreshCourseInfos() {
        CourseService.getInstance().findAllCourse(handler, false, true, null, null, null);
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CourseService.FIND_ALL_COURSE_MSG:
                    handleFindAllCourseMsg(msg);
                    break;
            }
        }
    };

    private void handleFindAllCourseMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(getActivity(), serviceResult.msg(), Toast.LENGTH_SHORT).show();
        courseInfos.clear();
        courseInfos.addAll(serviceResult.extra(CourseFindAllResponse.class).getCourseInfoList());
        swipeRefreshLayout.setRefreshing(false);
        recyclerViewAdapter.notifyDataSetChanged();
    }

}
