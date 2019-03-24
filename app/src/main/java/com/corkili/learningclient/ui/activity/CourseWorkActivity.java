package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.ProtoUtils;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkSimpleInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.CourseWorkFindAllResponse;
import com.corkili.learningclient.generate.protobuf.Response.CourseWorkGetResponse;
import com.corkili.learningclient.service.CourseWorkService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.ui.adapter.CourseWorkRecyclerViewAdapter;
import com.corkili.learningclient.ui.adapter.CourseWorkRecyclerViewAdapter.ViewHolder;
import com.corkili.learningclient.ui.other.MyRecyclerViewDivider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CourseWorkActivity extends AppCompatActivity implements CourseWorkRecyclerViewAdapter.OnItemInteractionListener {

    private static final int REQUEST_CODE_CREATE_COURSE_WORK = 0xF1;
    private static final int REQUEST_CODE_EDIT_COURSE_WORK = 0xF2;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton addCourseWorkFab;

    private CourseInfo courseInfo;
    private UserInfo userInfo;

    private List<CourseWorkSimpleInfo> courseWorkSimpleInfos;
    private Map<Long, CourseWorkInfo> courseWorkInfoCache;

    private CourseWorkRecyclerViewAdapter recyclerViewAdapter;

    private boolean startEditActivity;
    private boolean startDetailActivity;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_work);
        courseInfo = (CourseInfo) getIntent().getSerializableExtra(IntentParam.COURSE_INFO);
        userInfo = (UserInfo) getIntent().getSerializableExtra(IntentParam.USER_INFO);
        if (userInfo == null || courseInfo == null) {
            throw new RuntimeException("Intent param lost");
        }
        startEditActivity = false;
        startDetailActivity = false;
        recyclerView = findViewById(R.id.activity_course_work_list);
        swipeRefreshLayout = findViewById(R.id.activity_course_work_swipe_refresh_layout);
        addCourseWorkFab = findViewById(R.id.fab_add_course_work);
        if (userInfo.getUserType() == UserType.Teacher) {
            addCourseWorkFab.setOnClickListener(v -> enterAddCourseWorkActivity());
        } else {
            addCourseWorkFab.setVisibility(View.GONE);
        }
        courseWorkSimpleInfos = new ArrayList<>();
        courseWorkInfoCache = new ConcurrentHashMap<>();
        recyclerViewAdapter = new CourseWorkRecyclerViewAdapter(this, courseWorkSimpleInfos, this, userInfo);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addItemDecoration(new MyRecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL,
                1,ContextCompat.getColor(this,R.color.colorBlack)));
        swipeRefreshLayout.setOnRefreshListener(this::refreshCourseWorkSimpleInfos);
        refreshCourseWorkSimpleInfos();
    }

    private void refreshCourseWorkSimpleInfos() {
        CourseWorkService.getInstance().findAllCourseWork(handler, courseInfo.getCourseId());
    }

    private void enterAddCourseWorkActivity() {
        Intent intent = new Intent(CourseWorkActivity.this, CourseWorkEditActivity.class);
        intent.putExtra(IntentParam.IS_CREATE, true);
        intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
        startActivityForResult(intent, REQUEST_CODE_CREATE_COURSE_WORK);
    }

    private void enterEditCourseWorkActivity(CourseWorkInfo courseWorkInfo) {
        if (userInfo.getUserType() == UserType.Teacher) {
            Intent intent = new Intent(CourseWorkActivity.this, CourseWorkEditActivity.class);
            intent.putExtra(IntentParam.IS_CREATE, false);
            intent.putExtra(IntentParam.COURSE_INFO, courseInfo);
            intent.putExtra(IntentParam.COURSE_WORK_INFO, courseWorkInfo);
            startActivityForResult(intent, REQUEST_CODE_EDIT_COURSE_WORK);
        }
    }

    private void enterCourseWorkDetailActivity(CourseWorkInfo courseWorkInfo) {
        if (userInfo.getUserType() == UserType.Student) {
            Intent intent = new Intent(CourseWorkActivity.this, CourseWorkDetailActivity.class);
            intent.putExtra(IntentParam.USER_INFO, userInfo);
            intent.putExtra(IntentParam.COURSE_WORK_INFO, courseWorkInfo);
            startActivity(intent);
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CourseWorkService.FIND_ALL_COURSE_WORK_MSG) {
                handleFindAllCourseWorkMsg(msg);
            } else if (msg.what == CourseWorkService.GET_COURSE_WORK_MSG) {
                handleGetCourseWorkMsg(msg);
            }
        }
    };

    private void handleFindAllCourseWorkMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            courseWorkSimpleInfos.clear();
            courseWorkInfoCache.clear();
            if (userInfo.getUserType() == UserType.Teacher) {
                courseWorkSimpleInfos.addAll(serviceResult.extra(CourseWorkFindAllResponse.class).getCourseWorkSimpleInfoList());
            } else {
                for (CourseWorkSimpleInfo courseWorkSimpleInfo : serviceResult.extra(CourseWorkFindAllResponse.class).getCourseWorkSimpleInfoList()) {
                    if (courseWorkSimpleInfo.getOpen()) {
                        courseWorkSimpleInfos.add(courseWorkSimpleInfo);
                    }
                }
            }
            swipeRefreshLayout.setRefreshing(false);
            recyclerViewAdapter.notifyDataSetChanged();
        }
    }

    private void handleGetCourseWorkMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            CourseWorkInfo courseWorkInfo = serviceResult.extra(CourseWorkGetResponse.class).getCourseWorkInfo();
            courseWorkInfoCache.put(courseWorkInfo.getCourseWorkId(), courseWorkInfo);
            int needReplaceIndex = -1;
            for (int i = 0; i < courseWorkSimpleInfos.size(); i++) {
                if (courseWorkSimpleInfos.get(i).getCourseWorkId() == courseWorkInfo.getCourseWorkId()) {
                    needReplaceIndex = i;
                }
            }
            if (needReplaceIndex >= 0) {
                courseWorkSimpleInfos.set(needReplaceIndex, ProtoUtils.simplifyCourseWorkInfo(courseWorkInfo));
                recyclerViewAdapter.notifyItemChanged(needReplaceIndex);
            }
            if (startEditActivity) {
                startEditActivity = false;
                enterEditCourseWorkActivity(courseWorkInfo);
            }
            if (startDetailActivity) {
                startDetailActivity = false;
                enterCourseWorkDetailActivity(courseWorkInfo);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        if (data == null) {
            return;
        }
        CourseWorkInfo courseWorkInfo = (CourseWorkInfo) data.getSerializableExtra(IntentParam.COURSE_WORK_INFO);
        if (courseWorkInfo == null) {
            return;
        }
        if (requestCode == REQUEST_CODE_CREATE_COURSE_WORK) {
            courseWorkSimpleInfos.add(ProtoUtils.simplifyCourseWorkInfo(courseWorkInfo));
            courseWorkInfoCache.put(courseWorkInfo.getCourseWorkId(), courseWorkInfo);
            recyclerViewAdapter.notifyDataSetChanged();
        } else if (requestCode == REQUEST_CODE_EDIT_COURSE_WORK){
            boolean deleteCourseWork = data.getBooleanExtra(IntentParam.DELETE_COURSE_WORK, false);
            int needModifyIndex = -1;
            for (int i = 0; i < courseWorkSimpleInfos.size(); i++) {
                if (courseWorkSimpleInfos.get(i).getCourseWorkId() == courseWorkInfo.getCourseWorkId()) {
                    needModifyIndex = i;
                    break;
                }
            }
            if (needModifyIndex >= 0) {
                if (deleteCourseWork) {
                    courseWorkSimpleInfos.remove(needModifyIndex);
                    courseWorkInfoCache.remove(courseWorkInfo.getCourseWorkId());
                    recyclerViewAdapter.notifyDataSetChanged();
                } else {
                    courseWorkSimpleInfos.set(needModifyIndex, ProtoUtils.simplifyCourseWorkInfo(courseWorkInfo));
                    courseWorkInfoCache.put(courseWorkInfo.getCourseWorkId(), courseWorkInfo);
                    recyclerViewAdapter.notifyItemChanged(needModifyIndex);
                }
            }
        }
    }

    @Override
    public void onItemClick(ViewHolder viewHolder) {
        final CourseWorkSimpleInfo courseWorkSimpleInfo = viewHolder.getCourseWorkSimpleInfo();
        CourseWorkInfo courseWorkInfo = courseWorkInfoCache.get(courseWorkSimpleInfo.getCourseWorkId());
        if (courseWorkInfo != null) {
            if (userInfo.getUserType() == UserType.Teacher) {
                enterEditCourseWorkActivity(courseWorkInfo);
            } else {
                enterCourseWorkDetailActivity(courseWorkInfo);
            }
        } else {
            if (userInfo.getUserType() == UserType.Teacher) {
                startEditActivity = true;
            } else {
                startDetailActivity = true;
            }
            CourseWorkService.getInstance().getCourseWork(handler, courseWorkSimpleInfo.getCourseWorkId());
        }
    }

    @Override
    public void onSubmitViewClick(ViewHolder viewHolder) {
        if (userInfo.getUserType() == UserType.Teacher) {
            final CourseWorkSimpleInfo courseWorkSimpleInfo = viewHolder.getCourseWorkSimpleInfo();
            // TODO 跳转
        }
    }
}
