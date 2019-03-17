package com.corkili.learningclient.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Response.CourseFindAllResponse;
import com.corkili.learningclient.service.CourseService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.ui.activity.TeacherCourseEditActivity;
import com.corkili.learningclient.ui.activity.TeacherCourseManageActivity;
import com.corkili.learningclient.ui.adapter.TeacherCourseRecyclerViewAdapter;
import com.corkili.learningclient.ui.adapter.TeacherCourseRecyclerViewAdapter.ViewHolder;
import com.corkili.learningclient.ui.other.MyRecyclerViewDivider;

import java.util.ArrayList;
import java.util.List;

public class TeacherCourseFragment extends Fragment implements TeacherCourseRecyclerViewAdapter.OnItemInteractionListener {

    public static final int REQUEST_CODE_CREATE_COURSE = 0xF1;
    public static final int REQUEST_CODE_MANAGE_COURSE = 0xF2;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton createCourseFab;

    private TeacherCourseRecyclerViewAdapter recyclerViewAdapter;

    private List<CourseInfo> courseInfos;

    private DataBus dataBus;

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
        recyclerViewAdapter = new TeacherCourseRecyclerViewAdapter(getActivity(), courseInfos, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addItemDecoration(new MyRecyclerViewDivider(getActivity(), LinearLayoutManager.HORIZONTAL,
                1,ContextCompat.getColor(getActivity(),R.color.colorBlack)));
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        swipeRefreshLayout.setOnRefreshListener(this::refreshCourseInfos);

        createCourseFab.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), TeacherCourseEditActivity.class);
            intent.putExtra(IntentParam.IS_CREATE, true);
            startActivityForResult(intent, REQUEST_CODE_CREATE_COURSE);
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
        if (serviceResult.isSuccess()) {
            courseInfos.clear();
            courseInfos.addAll(serviceResult.extra(CourseFindAllResponse.class).getCourseInfoList());
            swipeRefreshLayout.setRefreshing(false);
            recyclerViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != TeacherCourseEditActivity.RESULT_OK) {
            return;
        }
        CourseInfo courseInfo = (CourseInfo) data.getSerializableExtra(IntentParam.COURSE_INFO);
        if (requestCode == REQUEST_CODE_CREATE_COURSE && courseInfo != null) {
            courseInfos.add(courseInfo);
            recyclerViewAdapter.notifyDataSetChanged();
        } else if (requestCode == REQUEST_CODE_MANAGE_COURSE && courseInfo != null) {
            int needReplaceIndex = -1;
            for (int i = 0; i < courseInfos.size(); i++) {
                CourseInfo info = courseInfos.get(i);
                if (info.getCourseId() == courseInfo.getCourseId()) {
                    needReplaceIndex = i;
                    break;
                }
            }
            if (needReplaceIndex > 0) {
                courseInfos.set(needReplaceIndex, courseInfo);
                recyclerViewAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DataBus) {
            this.dataBus = (DataBus) context;
        } else {
            throw new RuntimeException("Activities must implement TeacherCourseFragment.DataBus");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.dataBus = null;
    }

    @Override
    public void onItemClick(ViewHolder viewHolder) {
        Intent intent = new Intent(getActivity(), TeacherCourseManageActivity.class);
        intent.putExtra(IntentParam.USER_INFO, dataBus.getUserInfoFromActivity());
        intent.putExtra(IntentParam.COURSE_INFO, viewHolder.getCourseInfo());
        startActivityForResult(intent, REQUEST_CODE_MANAGE_COURSE);
    }

    public interface DataBus {

        UserInfo getUserInfoFromActivity();

    }

}
