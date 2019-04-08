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

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.UIHelper;
import com.corkili.learningclient.generate.protobuf.Info.MessageInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Response.MessageFindAllResponse;
import com.corkili.learningclient.service.MessageService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.ui.activity.MessageActivity;
import com.corkili.learningclient.ui.adapter.MessageFragmentRecyclerViewAdapter;
import com.corkili.learningclient.ui.adapter.MessageFragmentRecyclerViewAdapter.ViewHolder;
import com.qmuiteam.qmui.widget.pullRefreshLayout.QMUIPullRefreshLayout;
import com.qmuiteam.qmui.widget.pullRefreshLayout.QMUIPullRefreshLayout.OnPullListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

public class MessageFragment extends Fragment implements MessageFragmentRecyclerViewAdapter.OnItemInteractionListener {

    public static final int REQUEST_CODE_MESSAGE_ACTIVITY = 0xF1;

    private RecyclerView recyclerView;
    private QMUIPullRefreshLayout swipeRefreshLayout;
    private TextView tipView;

    private boolean shouldFinishRefresh = false;

    private MessageFragmentRecyclerViewAdapter recyclerViewAdapter;

    private Map<Long, List<MessageInfo>> messageInfos;
    private List<UserInfo> userInfos;

    private DataBus dataBus;

    public MessageFragment() {
    }

    public static MessageFragment newInstance() {
        MessageFragment fragment = new MessageFragment();
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
        View view = inflater.inflate(R.layout.fragment_message, container, false);
        tipView = view.findViewById(R.id.tip);
        recyclerView = view.findViewById(R.id.fragment_message_list);
        swipeRefreshLayout = view.findViewById(R.id.fragment_message_swipe_refresh_layout);
        messageInfos = new HashMap<>();
        userInfos = new ArrayList<>();
        recyclerViewAdapter = new MessageFragmentRecyclerViewAdapter(getActivity(), messageInfos, userInfos,this);
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
                refreshMessageInfos();
            }
        });

        refreshMessageInfos();
    }

    private void updateTipView() {
        if (messageInfos.isEmpty()) {
            tipView.setVisibility(View.VISIBLE);
        } else {
            tipView.setVisibility(View.GONE);
        }
    }

    private void refreshMessageInfos() {
        MessageService.getInstance().findAllMessage(handler,
                dataBus.getUserInfoFromActivity().getUserId(), null, true);
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MessageService.FIND_ALL_MESSAGE_MSG:
                    handleFindAllMessageMsg(msg);
                    break;
            }
        }
    };

    private void handleFindAllMessageMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        if (serviceResult.isSuccess()) {
            messageInfos.clear();
            userInfos.clear();
            List<MessageInfo> messageInfoList = serviceResult.extra(MessageFindAllResponse.class).getMessageInfoList();
            for (MessageInfo messageInfo : messageInfoList) {
                if (messageInfos.containsKey(messageInfo.getReceiverInfo().getUserId())) {
                    messageInfos.get(messageInfo.getReceiverInfo().getUserId()).add(messageInfo);
                } else if (messageInfos.containsKey(messageInfo.getSenderInfo().getUserId())) {
                    messageInfos.get(messageInfo.getSenderInfo().getUserId()).add(messageInfo);
                } else {
                    if (messageInfo.getReceiverInfo().getUserId() != dataBus.getUserInfoFromActivity().getUserId()) {
                        userInfos.add(messageInfo.getReceiverInfo());
                        List<MessageInfo> newList = new ArrayList<>();
                        newList.add(messageInfo);
                        messageInfos.put(messageInfo.getReceiverInfo().getUserId(), newList);
                    } else if (messageInfo.getSenderInfo().getUserId() != dataBus.getUserInfoFromActivity().getUserId()) {
                        userInfos.add(messageInfo.getSenderInfo());
                        List<MessageInfo> newList = new ArrayList<>();
                        newList.add(messageInfo);
                        messageInfos.put(messageInfo.getSenderInfo().getUserId(), newList);
                    }
                }
            }
            for (List<MessageInfo> infoList : messageInfos.values()) {
                Collections.sort(infoList, (o1, o2) -> {
                    if (o1.getCreateTime() == o2.getCreateTime()) {
                        return 0;
                    } else {
                        return o1.getCreateTime() < o2.getCreateTime() ? -1 : 1;
                    }
                });
            }
            recyclerViewAdapter.notifyDataSetChanged();
        } else {
            UIHelper.toast(getActivity(), serviceResult, raw -> "加载消息失败");
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
        if (requestCode == REQUEST_CODE_MESSAGE_ACTIVITY) {
            UserInfo userInfo = (UserInfo) data.getSerializableExtra(IntentParam.USER_INFO);
            int count = data.getIntExtra(IntentParam.COUNT, 0);
            List<MessageInfo> messageInfoList = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                messageInfoList.add((MessageInfo) data.getSerializableExtra(IntentParam.MESSAGE_INFO + i));
            }
            Collections.sort(messageInfoList, (o1, o2) -> {
                if (o1.getCreateTime() == o2.getCreateTime()) {
                    return 0;
                } else {
                    return o1.getCreateTime() < o2.getCreateTime() ? -1 : 1;
                }
            });
            messageInfos.put(userInfo.getUserId(), messageInfoList);
            recyclerViewAdapter.notifyDataSetChanged();
        }
        updateTipView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DataBus) {
            this.dataBus = (DataBus) context;
        } else {
            throw new RuntimeException("Activities must implement MessageFragment.DataBus");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.dataBus = null;
    }

    @Override
    public void onItemClick(ViewHolder viewHolder) {
        Intent intent = new Intent(getActivity(), MessageActivity.class);
        intent.putExtra(IntentParam.USER_INFO, viewHolder.getUserInfo());
        intent.putExtra(IntentParam.SELF_USER_INFO, dataBus.getUserInfoFromActivity());
        intent.putExtra(IntentParam.COUNT, viewHolder.getMessageInfoList().size());
        List<MessageInfo> messageInfoList = viewHolder.getMessageInfoList();
        for (int i = 0; i < messageInfoList.size(); i++) {
            intent.putExtra(IntentParam.MESSAGE_INFO + i, messageInfoList.get(i));
        }
        startActivityForResult(intent, REQUEST_CODE_MESSAGE_ACTIVITY);
    }

    public interface DataBus {

        UserInfo getUserInfoFromActivity();

    }

}
