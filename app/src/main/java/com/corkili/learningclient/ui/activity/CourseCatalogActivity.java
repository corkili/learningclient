package com.corkili.learningclient.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.CourseCatalogItemBean;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.UIHelper;
import com.corkili.learningclient.generate.protobuf.Info.CourseCatalogInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseCatalogItemInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseCatalogItemInfoList;
import com.corkili.learningclient.ui.adapter.CourseCatalogTreeListViewAdapter;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.zhy.tree.bean.Node;
import com.zhy.tree.bean.TreeListViewAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CourseCatalogActivity extends AppCompatActivity implements TreeListViewAdapter.OnTreeNodeClickListener {

    private QMUITopBarLayout topBar;
    private ListView catalogListView;
    private TreeListViewAdapter mAdapter;

    private CourseCatalogInfo courseCatalogInfo;

    private List<CourseCatalogItemBean> mDatas;
    private Map<Integer, CourseCatalogItemInfo> dataMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_catalog);

        courseCatalogInfo = (CourseCatalogInfo) getIntent().getSerializableExtra(IntentParam.COURSE_CATALOG_INFO);

        if (courseCatalogInfo == null) {
            UIHelper.toast(this, "没有可选择的学习内容");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        topBar = findViewById(R.id.topbar);
        topBar.setTitle("选择要跳转到的活动");
        topBar.addLeftBackImageButton().setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        catalogListView = findViewById(R.id.catalog_item_list);

        initData();

        try {
            mAdapter = new CourseCatalogTreeListViewAdapter(catalogListView, this, mDatas,
                    courseCatalogInfo.getMaxLevel(), dataMap);
            mAdapter.setOnTreeNodeClickListener(this);
            catalogListView.setAdapter(mAdapter);
        } catch (Exception e) {
            UIHelper.toast(this, "没有可选择的学习内容");
            setResult(RESULT_CANCELED);
            finish();
        }

    }

    private void initData() {
        mDatas = new ArrayList<>();
        dataMap = new HashMap<>();
        AtomicInteger idCounter = new AtomicInteger(0);

        List<CourseCatalogItemInfo> level1ItemList = new ArrayList<>(courseCatalogInfo.getItemsOrDefault(1,
                CourseCatalogItemInfoList.getDefaultInstance()).getCourseCatalogItemInfoList());

        Collections.sort(level1ItemList, (o1, o2) -> o1.getIndex() - o2.getIndex());

        for (int i = 0; i < level1ItemList.size(); i++) {
            CourseCatalogItemInfo courseCatalogItemInfo = level1ItemList.get(i);
            initData(courseCatalogItemInfo, idCounter, 0);
        }

    }

    private void initData(CourseCatalogItemInfo courseCatalogItemInfo, AtomicInteger idCounter, int parentId) {
        CourseCatalogItemBean bean = new CourseCatalogItemBean(idCounter.incrementAndGet(), parentId, courseCatalogItemInfo.getItemTitle());
        if (courseCatalogItemInfo.getVisible()) {
            mDatas.add(bean);
            dataMap.put(bean.getId(), courseCatalogItemInfo);
            List<CourseCatalogItemInfo> courseCatalogItemInfoList = new ArrayList<>(courseCatalogItemInfo
                    .getNextLevelItems().getCourseCatalogItemInfoList());
            Collections.sort(courseCatalogItemInfoList, (o1, o2) -> o1.getIndex() - o2.getIndex());
            for (int i = 0; i < courseCatalogItemInfoList.size(); i++) {
                CourseCatalogItemInfo childCourseCatalogItemInfo = courseCatalogItemInfoList.get(i);
                initData(childCourseCatalogItemInfo, idCounter, bean.getId());
            }
        }
    }


    @Override
    public void onClick(Node node, int position) {
        CourseCatalogItemInfo courseCatalogItemInfo = dataMap.get(node.getId());
        if (courseCatalogItemInfo == null) {
            return;
        }
        if (node.isLeaf()) {
            StringBuilder sb = new StringBuilder();
            String completionStatus = courseCatalogItemInfo.getCompletionStatus();
            boolean hasCT = courseCatalogItemInfo.getHasCompletionThreshold();
            double ct = courseCatalogItemInfo.getCompletionThreshold();
            double pm = courseCatalogItemInfo.getProgressMeasure();
            StringBuilder progress = new StringBuilder();
            if (hasCT) {
                progress.append("学习进度-");
                progress.append(pm * 100).append("%");
                progress.append("（达到 ").append(ct * 100).append("%即为完成学习）");
            }
            if ("completed".equals(completionStatus)) {
                sb.append("已完成学习");
                if (hasCT) {
                    sb.append("，").append(progress.toString());
                }
            } else if ("incomplete".equals(completionStatus)){
                sb.append("未完成学习");
                if (hasCT) {
                    sb.append("，").append(progress.toString());
                }
            } else {
                sb.append("未开始学习");
            }
            QMUIDialog.MessageDialogBuilder builder = new QMUIDialog.MessageDialogBuilder(this)
                    .setTitle(courseCatalogItemInfo.getItemTitle())
                    .setMessage(sb.toString())
                    .setCanceledOnTouchOutside(true)
                    .setCancelable(true);
            if (courseCatalogItemInfo.getSelectable()) {
                builder.addAction(0, "去学习", QMUIDialogAction.ACTION_PROP_NEGATIVE, (dialog, index) -> {
                    if (courseCatalogItemInfo.getSelectable()) {
                        Intent intent = new Intent();
                        intent.putExtra(IntentParam.COURSE_CATALOG_ITEM_INFO, courseCatalogItemInfo);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                    dialog.dismiss();
                });
            }
            builder.show();
        }

    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}
