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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.UIHelper;
import com.corkili.learningclient.generate.protobuf.Info.QuestionInfo;
import com.corkili.learningclient.generate.protobuf.Response.QuestionFindAllResponse;
import com.corkili.learningclient.service.QuestionService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.ui.activity.QuestionEditActivity;
import com.corkili.learningclient.ui.activity.QuestionSelectActivity;
import com.corkili.learningclient.ui.activity.TeacherMainActivity;
import com.corkili.learningclient.ui.adapter.QuestionRecyclerViewAdapter;
import com.corkili.learningclient.ui.adapter.QuestionRecyclerViewAdapter.ViewHolder;
import com.corkili.learningclient.ui.other.MyRecyclerViewDivider;
import com.qmuiteam.qmui.widget.pullRefreshLayout.QMUIPullRefreshLayout;
import com.qmuiteam.qmui.widget.pullRefreshLayout.QMUIPullRefreshLayout.OnPullListener;

import java.util.ArrayList;
import java.util.List;

public class QuestionFragment extends Fragment implements QuestionRecyclerViewAdapter.OnItemInteractionListener {

    public static final int REQUEST_CODE_CREATE_QUESTION = 0xF1;
    public static final int REQUEST_CODE_UPDATE_OR_DELETE_QUESTION = 0xF2;

    private RecyclerView recyclerView;
    private QMUIPullRefreshLayout swipeRefreshLayout;
    private FloatingActionButton addQuestionFab;
    private TextView tipView;

    private QuestionRecyclerViewAdapter recyclerViewAdapter;

    private List<QuestionInfo> questionInfos;

    private boolean shouldFinishRefresh;

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
        tipView = view.findViewById(R.id.tip);
        recyclerView = view.findViewById(R.id.fragment_question_list);
        swipeRefreshLayout = view.findViewById(R.id.fragment_question_swipe_refresh_layout);
        addQuestionFab = view.findViewById(R.id.fab_add_question);
        questionInfos = new ArrayList<>();
        recyclerViewAdapter = new QuestionRecyclerViewAdapter(getActivity(), questionInfos, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addItemDecoration(new MyRecyclerViewDivider(getActivity(), LinearLayoutManager.HORIZONTAL,
                1,ContextCompat.getColor(getActivity(),R.color.colorBlack)));
        shouldFinishRefresh = false;
        return view;
    }

    @SuppressLint("RestrictedApi")
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
                refreshQuestionInfos();
            }
        });

        addQuestionFab.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), QuestionEditActivity.class);
            intent.putExtra(IntentParam.IS_CREATE, true);
            startActivityForResult(intent, REQUEST_CODE_CREATE_QUESTION);
        });

        if (getActivity() instanceof QuestionSelectActivity) {
            addQuestionFab.setEnabled(false);
            addQuestionFab.setVisibility(View.GONE);
        }

        refreshQuestionInfos();
    }

    private void updateTipView() {
        if (questionInfos.isEmpty()) {
            tipView.setVisibility(View.VISIBLE);
        } else {
            tipView.setVisibility(View.GONE);
        }
    }

    private void refreshQuestionInfos() {
        QuestionService.getInstance().findAllQuestion(handler, true, null, null);
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == QuestionService.FIND_ALL_QUESTION_MSG) {
                handleFindAllQuestionMsg(msg);
            }
        }
    };

    private void handleFindAllQuestionMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        if (serviceResult.isSuccess()) {
            questionInfos.clear();
            questionInfos.addAll(serviceResult.extra(QuestionFindAllResponse.class).getQuestionInfoList());
            recyclerViewAdapter.notifyDataSetChanged();
        }
        if (shouldFinishRefresh) {
            shouldFinishRefresh = false;
            swipeRefreshLayout.finishRefresh();
        } else {
            UIHelper.toast(getActivity(), serviceResult, raw -> "加载试题信息失败");
        }
        updateTipView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        QuestionInfo questionInfo = (QuestionInfo) data.getSerializableExtra(IntentParam.QUESTION_INFO);
        if (requestCode == REQUEST_CODE_UPDATE_OR_DELETE_QUESTION && questionInfo != null) {
            boolean deleteQuestion = data.getBooleanExtra(IntentParam.DELETE_QUESTION, false);
            int needModifyIndex = -1;
            for (int i = 0; i < questionInfos.size(); i++) {
                if (questionInfos.get(i).getQuestionId() == questionInfo.getQuestionId()) {
                    needModifyIndex = i;
                    break;
                }
            }
            if (needModifyIndex >= 0) {
                if (deleteQuestion) {
                    questionInfos.remove(needModifyIndex);
                } else {
                    questionInfos.set(needModifyIndex, questionInfo);
                }
                recyclerViewAdapter.notifyDataSetChanged();
            }
        } else if (requestCode == REQUEST_CODE_CREATE_QUESTION && questionInfo != null){
            questionInfos.add(questionInfo);
            recyclerViewAdapter.notifyDataSetChanged();
        }
        updateTipView();
    }

    @Override
    public boolean onItemLongClick(ViewHolder viewHolder) {
        if (getActivity() instanceof TeacherMainActivity) {
            Intent intent = new Intent(getActivity(), QuestionEditActivity.class);
            intent.putExtra(IntentParam.IS_CREATE, false);
            intent.putExtra(IntentParam.QUESTION_INFO, viewHolder.getQuestionInfo());
            startActivityForResult(intent, REQUEST_CODE_UPDATE_OR_DELETE_QUESTION);
        } else if (getActivity() instanceof QuestionSelectActivity){
            Intent intent = new Intent();
            intent.putExtra(IntentParam.QUESTION_INFO, viewHolder.getQuestionInfo());
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        }
        return true;
    }

}
