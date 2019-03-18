package com.corkili.learningclient.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import com.corkili.learningclient.common.ProtoUtils;
import com.corkili.learningclient.generate.protobuf.Info.QuestionInfo;
import com.corkili.learningclient.generate.protobuf.Info.QuestionSimpleInfo;
import com.corkili.learningclient.generate.protobuf.Response.QuestionFindAllResponse;
import com.corkili.learningclient.generate.protobuf.Response.QuestionGetResponse;
import com.corkili.learningclient.service.QuestionService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.ui.activity.QuestionEditActivity;
import com.corkili.learningclient.ui.adapter.QuestionRecyclerViewAdapter;
import com.corkili.learningclient.ui.adapter.QuestionRecyclerViewAdapter.ViewHolder;
import com.corkili.learningclient.ui.other.MyRecyclerViewDivider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuestionFragment extends Fragment implements QuestionRecyclerViewAdapter.OnItemInteractionListener {

    public static final int REQUEST_CODE_CREATE_QUESTION = 0xF1;
    public static final int REQUEST_CODE_MANAGE_QUESTION = 0xF2;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton addQuestionFab;

    private QuestionRecyclerViewAdapter recyclerViewAdapter;

    private List<QuestionSimpleInfo> questionSimpleInfos;
    private Map<Long, QuestionInfo> questionInfoCache;

    private boolean hasStartActivityRequest;

    public QuestionFragment() {
    }

    public static QuestionFragment newInstance() {
        QuestionFragment fragment = new QuestionFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("UseSparseArrays")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_question, container, false);
        hasStartActivityRequest = false;
        recyclerView = view.findViewById(R.id.fragment_question_list);
        swipeRefreshLayout = view.findViewById(R.id.fragment_question_swipe_refresh_layout);
        addQuestionFab = view.findViewById(R.id.fab_add_question);
        questionSimpleInfos = new ArrayList<>();
        questionInfoCache = new HashMap<>();
        recyclerViewAdapter = new QuestionRecyclerViewAdapter(getActivity(), questionSimpleInfos, this);
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
        swipeRefreshLayout.setOnRefreshListener(this::refreshQuestionSimpleInfos);

        addQuestionFab.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), QuestionEditActivity.class);
            intent.putExtra(IntentParam.IS_CREATE, true);
            startActivityForResult(intent, REQUEST_CODE_CREATE_QUESTION);
        });

        refreshQuestionSimpleInfos();
    }

    private void refreshQuestionSimpleInfos() {
        QuestionService.getInstance().findAllQuestion(handler, true, null, null);
    }

    private void getQuestionInfo(Collection<Long> questionIdList) {
        QuestionService.getInstance().getQuestion(handler, questionIdList, true);
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == QuestionService.FIND_ALL_QUESTION_MSG) {
                handleFindAllQuestionMsg(msg);
            } else if (msg.what == QuestionService.GET_QUESTION_MSG) {
                handleGetQuestionMsg(msg);
            }
        }
    };

    private void handleFindAllQuestionMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(getActivity(), serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            Set<Long> cachedQuestionIdList = new HashSet<>(questionInfoCache.keySet());
            questionSimpleInfos.clear();
            questionInfoCache.clear();
            questionSimpleInfos.addAll(serviceResult.extra(QuestionFindAllResponse.class).getQuestionSimpleInfoList());
            swipeRefreshLayout.setRefreshing(false);
            recyclerViewAdapter.notifyDataSetChanged();
            List<Long> needRecoverQuestionId = new ArrayList<>();
            for (QuestionSimpleInfo questionSimpleInfo : questionSimpleInfos) {
                if (cachedQuestionIdList.contains(questionSimpleInfo.getQuestionId())) {
                    needRecoverQuestionId.add(questionSimpleInfo.getQuestionId());
                }
            }
            getQuestionInfo(needRecoverQuestionId);
        }
    }

    private void handleGetQuestionMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(getActivity(), serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            List<QuestionInfo> questionInfoList = serviceResult.extra(QuestionGetResponse.class).getQuestionInfoList();
            for (QuestionInfo questionInfo : questionInfoList) {
                questionInfoCache.put(questionInfo.getQuestionId(), questionInfo);
                int needReplaceIndex = -1;
                for (int i = 0; i < questionSimpleInfos.size(); i++) {
                    if (questionSimpleInfos.get(i).getQuestionId() == questionInfo.getQuestionId()) {
                        needReplaceIndex = i;
                        break;
                    }
                }
                if (needReplaceIndex >= 0) {
                    questionSimpleInfos.set(needReplaceIndex, ProtoUtils.simplifyQuestionInfo(questionInfo));
                }
            }
            if (!questionInfoList.isEmpty()) {
                recyclerViewAdapter.notifyDataSetChanged();
            }
            if (questionInfoList.size() == 1 && hasStartActivityRequest) {
                hasStartActivityRequest = false;
                startQuestionManageActivity(questionInfoList.get(0));
            }
        }
    }

    // TODO 跳转
    private void startQuestionManageActivity(QuestionInfo questionInfo) {
        Intent intent = new Intent(getActivity(), QuestionEditActivity.class);
        intent.putExtra(IntentParam.IS_CREATE, false);
        intent.putExtra(IntentParam.QUESTION_INFO, questionInfo);
        startActivityForResult(intent, REQUEST_CODE_MANAGE_QUESTION);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        QuestionInfo questionInfo = (QuestionInfo) data.getSerializableExtra(IntentParam.QUESTION_INFO);
        if (requestCode == REQUEST_CODE_MANAGE_QUESTION && questionInfo != null) {
            boolean deleteQuestion = data.getBooleanExtra(IntentParam.DELETE_QUESTION, false);
            int needModifyIndex = -1;
            for (int i = 0; i < questionSimpleInfos.size(); i++) {
                if (questionSimpleInfos.get(i).getQuestionId() == questionInfo.getQuestionId()) {
                    needModifyIndex = i;
                    break;
                }
            }
            if (needModifyIndex >= 0) {
                if (deleteQuestion) {
                    questionSimpleInfos.remove(needModifyIndex);
                    questionInfoCache.remove(questionInfo.getQuestionId());
                } else {
                    questionSimpleInfos.set(needModifyIndex, ProtoUtils.simplifyQuestionInfo(questionInfo));
                    questionInfoCache.put(questionInfo.getQuestionId(), questionInfo);
                }
                recyclerViewAdapter.notifyDataSetChanged();
            }
        } else if (requestCode == REQUEST_CODE_CREATE_QUESTION && questionInfo != null){
            questionSimpleInfos.add(ProtoUtils.simplifyQuestionInfo(questionInfo));
            questionInfoCache.put(questionInfo.getQuestionId(), questionInfo);
            recyclerViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(ViewHolder viewHolder) {
        QuestionInfo questionInfo = questionInfoCache.get(viewHolder.getQuestionSimpleInfo().getQuestionId());
        if (questionInfo != null) {
            startQuestionManageActivity(questionInfo);
        } else {
            hasStartActivityRequest = true;
            getQuestionInfo(Collections.singletonList(viewHolder.getQuestionSimpleInfo().getQuestionId()));
        }
    }

}
