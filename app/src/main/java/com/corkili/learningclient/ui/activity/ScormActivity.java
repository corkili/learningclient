package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.UIHelper;
import com.corkili.learningclient.generate.protobuf.Info.CourseCatalogInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseCatalogItemInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseCatalogItemInfoList;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.DeliveryContentInfo;
import com.corkili.learningclient.generate.protobuf.Info.NavigationEventType;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.CourseCatalogQueryResponse;
import com.corkili.learningclient.generate.protobuf.Response.NavigationProcessResponse;
import com.corkili.learningclient.network.HttpUtils;
import com.corkili.learningclient.service.ScormService;
import com.corkili.learningclient.service.ServiceResult;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog.CustomDialogBuilder;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScormActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CHOOSE_ITEM = 0xF1;

    private QMUITopBarLayout topBar;
    private FrameLayout scormViewLayout;
    private WebView scormView;
    private ProgressBar scormLoadProgressBar;
    private TextView tipView;
    private QMUIRoundButton chooseButton;
    private QMUIRoundButton previousButton;
    private QMUIRoundButton nextButton;
    private QMUIRoundButton suspendAndResumeButton;
    private QMUIRoundButton startAndExitButton;

    private List<WebView> openWebViewList;

    private UserInfo userInfo;
    private CourseInfo courseInfo;
    private CourseCatalogInfo courseCatalogInfo;
    private List<CourseCatalogItemInfo> level1ItemInfoList;
    private CourseCatalogItemInfo currentLevel1Item;
    private CourseCatalogItemInfo currentLevel1ItemBeforeChoose;
    private DeliveryContentInfo currentDeliveryContentInfo;

    private boolean alreadyStart;
    private boolean isSuspend;

    private boolean shouldFinish;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scorm);

        userInfo = (UserInfo) getIntent().getSerializableExtra(IntentParam.USER_INFO);
        courseInfo = (CourseInfo) getIntent().getSerializableExtra(IntentParam.COURSE_INFO);
        courseCatalogInfo = null;

        if (userInfo == null || courseInfo == null) {
            throw new RuntimeException("expected intent param");
        }

        topBar = findViewById(R.id.topbar);

        topBar.addLeftBackImageButton().setOnClickListener(v -> finishActivity());

        if (userInfo.getUserType() == UserType.Teacher) {
            topBar.setTitle("课件预览");
        } else {
            topBar.setTitle("课件学习");
        }

        scormViewLayout = findViewById(R.id.scorm_view_layout);
        scormView = findViewById(R.id.scorm_view);
        scormLoadProgressBar = findViewById(R.id.scorm_load_progress_bar);
        tipView = findViewById(R.id.scorm_view_tip);
        chooseButton = findViewById(R.id.button_choose);
        previousButton = findViewById(R.id.button_previous);
        nextButton = findViewById(R.id.button_next);
        suspendAndResumeButton = findViewById(R.id.button_suspend_and_resume);
        startAndExitButton = findViewById(R.id.button_start_and_exit);
        openWebViewList = new ArrayList<>();

        initWebView(scormView);

        chooseButton.setOnClickListener(v -> {
            Intent intent = new Intent(ScormActivity.this, CourseCatalogActivity.class);
            intent.putExtra(IntentParam.COURSE_CATALOG_INFO, courseCatalogInfo);
            startActivityForResult(intent, REQUEST_CODE_CHOOSE_ITEM);
        });

        previousButton.setOnClickListener(v -> {
            if (alreadyStart && !isSuspend) {
                UIHelper.showLoadingDialog(ScormActivity.this);
                triggerNavigationEvent(NavigationEventType.Previous);
            }
        });

        nextButton.setOnClickListener(v -> {
            if (alreadyStart && !isSuspend) {
                UIHelper.showLoadingDialog(ScormActivity.this);
                triggerNavigationEvent(NavigationEventType.Continue);
            }
        });

        suspendAndResumeButton.setOnClickListener(v -> {
            if (alreadyStart) {
                UIHelper.showLoadingDialog(ScormActivity.this);
                if (!isSuspend) {
                    triggerNavigationEvent(NavigationEventType.SuspendAll);
                } else {
                    triggerNavigationEvent(NavigationEventType.ResumeAll);
                }
            }
        });

        startAndExitButton.setOnClickListener(v -> {
            UIHelper.showLoadingDialog(ScormActivity.this);
            if (!alreadyStart) {
                // start
                if (currentLevel1Item == null) {
                    nextLevel1Item();
                }
                triggerNavigationEvent(NavigationEventType.Start);
            } else {
                // exit
                triggerNavigationEvent(NavigationEventType.UnqualifiedExit);
            }
        });

        alreadyStart = false;
        isSuspend = false;
        shouldFinish = false;
        currentLevel1Item = null;
        level1ItemInfoList = new ArrayList<>();

        UIHelper.showLoadingDialog(ScormActivity.this);

        loadCourseCatalog();
    }

    private void initWebView(WebView webView) {
        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setAllowFileAccess(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setDefaultTextEncodingName("utf-8");

        webSettings.setSupportMultipleWindows(true);

        webView.setWebViewClient(webViewClient);
        webView.setWebChromeClient(webChromeClient);
    }

    private void refreshView() {
        if (alreadyStart) {
            if (!isSuspend) {
                previousButton.setEnabled(true);
                nextButton.setEnabled(true);
            } else {
                previousButton.setEnabled(false);
                nextButton.setEnabled(false);
            }
            startAndExitButton.setEnabled(true);
            startAndExitButton.setText("停止");
            suspendAndResumeButton.setEnabled(true);
        } else {
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
            startAndExitButton.setEnabled(true);
            startAndExitButton.setText("开始");
            suspendAndResumeButton.setEnabled(false);
        }

        if (courseCatalogInfo == null) {
            chooseButton.setEnabled(false);
        } else {
            chooseButton.setEnabled(true);
        }

        if (isSuspend) {
            suspendAndResumeButton.setText("恢复");
        } else {
            suspendAndResumeButton.setText("暂停");
        }

        boolean alreadySetTitle = false;
        if (currentDeliveryContentInfo != null) {
            scormView.setVisibility(View.VISIBLE);
            tipView.setVisibility(View.GONE);
            String title = getTitleForItemInLastLevel(currentDeliveryContentInfo.getItemId());
            if (title != null) {
                alreadySetTitle = true;
                topBar.setTitle(title);
            }
            String url = HttpUtils.getLaunchContentObjectUrl(courseInfo.getCoursewareId(), currentDeliveryContentInfo.getItemId());
            for (WebView webView : openWebViewList) {
                webView.setVisibility(View.GONE);
                scormViewLayout.removeView(webView);
                webView.destroy();
            }
            openWebViewList.clear();
            scormView.loadUrl(url);
        } else {
            for (WebView webView : openWebViewList) {
                webView.setVisibility(View.GONE);
            }
            scormView.setVisibility(View.GONE);
            tipView.setVisibility(View.VISIBLE);
        }
        if (!alreadySetTitle) {
            if (userInfo.getUserType() == UserType.Teacher) {
                topBar.setTitle("课件预览");
            } else {
                topBar.setTitle("课件学习");
            }
        }
    }

    private void loadCourseCatalog() {
        ScormService.getInstance().queryCatalog(handler, courseInfo.getCoursewareId());
    }

    private void finishActivity() {
        if (courseCatalogInfo != null && currentLevel1Item != null) {
            shouldFinish = true;
            triggerNavigationEvent(NavigationEventType.ExitAll);
        } else {
            finish();
        }
    }

    private void triggerNavigationEvent(NavigationEventType eventType) {
        triggerNavigationEvent(eventType, "");
    }

    private void triggerNavigationEvent(NavigationEventType eventType, String targetItemId) {
        ScormService.getInstance().processNavigation(handler, eventType, targetItemId, courseInfo.getCoursewareId(), currentLevel1Item.getItemId());
    }

    private boolean nextLevel1Item() {
        if (currentLevel1Item == null) {
            currentLevel1Item = level1ItemInfoList.get(0);
            return true;
        } else {
            for (int i = 0; i < level1ItemInfoList.size(); i++) {
                CourseCatalogItemInfo courseCatalogItemInfo = level1ItemInfoList.get(i);
                if (courseCatalogItemInfo.getIndex() == currentLevel1Item.getIndex()) {
                    if (i + 1 < level1ItemInfoList.size()) {
                        triggerNavigationEvent(NavigationEventType.ExitAll);
                        shouldFinish = false;
                        currentLevel1Item = level1ItemInfoList.get(i + 1);
                        return true;
                    }
                    break;
                }
            }
            return false;
        }
    }

    private boolean previousLevel1Item() {
        if (currentLevel1Item == null) {
            currentLevel1Item = level1ItemInfoList.get(level1ItemInfoList.size() - 1);
            return true;
        } else {
            for (int i = level1ItemInfoList.size() - 1; i >= 0; i--) {
                CourseCatalogItemInfo courseCatalogItemInfo = level1ItemInfoList.get(i);
                if (courseCatalogItemInfo.getIndex() == currentLevel1Item.getIndex()) {
                    if (i - 1 >= 0) {
                        triggerNavigationEvent(NavigationEventType.ExitAll);
                        shouldFinish = false;
                        currentLevel1Item = level1ItemInfoList.get(i - 1);
                        return true;
                    }
                    break;
                }
            }
            return false;
        }
    }

    private String getTitleForItemInLastLevel(String itemId) {
        if (itemId == null) {
            return null;
        }
        if (courseCatalogInfo != null) {
            CourseCatalogItemInfoList courseCatalogItemInfoList = courseCatalogInfo.getItemsMap().get(courseCatalogInfo.getMaxLevel());
            if (courseCatalogItemInfoList != null) {
                for (CourseCatalogItemInfo courseCatalogItemInfo : courseCatalogItemInfoList.getCourseCatalogItemInfoList()) {
                    if (itemId.equals(courseCatalogItemInfo.getItemId())) {
                        return courseCatalogItemInfo.getItemTitle();
                    }
                }
            }
        }
        return null;
    }

    private WebViewClient webViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.i("ScormActivity", "shouldOverrideUrlLoading: " + url);
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            scormLoadProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            scormLoadProgressBar.setVisibility(View.GONE);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }
    };

    private WebChromeClient webChromeClient = new WebChromeClient() {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            scormLoadProgressBar.setProgress(newProgress);
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            new QMUIDialog.MessageDialogBuilder(ScormActivity.this)
                    .setTitle("来自网页的消息")
                    .setMessage(message)
                    .addAction(0, "确定", (dialog, index) -> {
                        result.confirm();
                        dialog.dismiss();
                    })
                    .setCancelable(false)
                    .show();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            new QMUIDialog.MessageDialogBuilder(ScormActivity.this)
                    .setTitle("来自网页的消息")
                    .setMessage(message)
                    .addAction("取消", ((dialog, index) -> {
                        result.cancel();
                        dialog.dismiss();
                    }))
                    .addAction(0, "确定", (dialog, index) -> {
                        result.confirm();
                        dialog.dismiss();
                    })
                    .setCancelable(false)
                    .show();
            return true;
        }

        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
            QMUIDialog.CustomDialogBuilder builder = new CustomDialogBuilder(ScormActivity.this);
            builder.setTitle("发表帖子");
            builder.setLayout(R.layout.dialog_message_and_editor);
            builder.addAction("取消", (dialog, index) -> {
                result.cancel();
                dialog.dismiss();
            });
            builder.addAction("确定", ((dialog, index) -> {
                String value = dialog.<EditText>findViewById(R.id.editor).getText().toString().trim();
                result.confirm(value);
                dialog.dismiss();
            }));
            QMUIDialog dialog = builder.create();
            dialog.<TextView>findViewById(R.id.message).setText(message);
            dialog.show();
            return true;
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            WebView newWebView = new WebView(ScormActivity.this);
            initWebView(newWebView);
            scormView.setVisibility(View.GONE);
            scormViewLayout.addView(newWebView);
            openWebViewList.add(newWebView);
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(newWebView);
            resultMsg.sendToTarget();
            return true;
        }
    };

    private void setState(boolean alreadyStart, boolean isSuspend) {
        this.alreadyStart = alreadyStart;
        this.isSuspend = isSuspend;
    }

    private CourseCatalogItemInfo searchParentLevel1Item(CourseCatalogItemInfo item) {
        if (item.getLevel() < 1) {
            return item;
        }
        List<CourseCatalogItemInfo> list;
        while (item.getLevel() != 1) {
            list = courseCatalogInfo.getItemsOrDefault(item.getLevel() - 1, CourseCatalogItemInfoList
                    .getDefaultInstance()).getCourseCatalogItemInfoList();
            for (CourseCatalogItemInfo courseCatalogItemInfo : list) {
                boolean searched = false;
                for (CourseCatalogItemInfo catalogItemInfo : courseCatalogItemInfo.getNextLevelItems().getCourseCatalogItemInfoList()) {
                    if (catalogItemInfo.getItemId().equals(item.getItemId())) {
                        searched = true;
                        break;
                    }
                }
                if (searched) {
                    item = courseCatalogItemInfo;
                    break;
                }
            }
        }
        return item;
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == ScormService.QUERY_CATALOG_MSG) {
                handleQueryCatalogMsg(msg);
            } else if (msg.what == ScormService.PROCESS_NAVIGATION_MSG) {
                handleProcessNavigationMsg(msg);
            }
        }
    };

    private void handleQueryCatalogMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        if (serviceResult.isSuccess()) {
            courseCatalogInfo = serviceResult.extra(CourseCatalogQueryResponse.class).getCourseCatalogInfo();
            level1ItemInfoList = new ArrayList<>(courseCatalogInfo
                    .getItemsOrDefault(1, CourseCatalogItemInfoList.getDefaultInstance())
                    .getCourseCatalogItemInfoList());
            Collections.sort(level1ItemInfoList, (o1, o2) -> o1.getIndex() - o2.getIndex());
            if (level1ItemInfoList.isEmpty()) {
                UIHelper.toast(this, "无内容可以学习");
                finish();
                return;
            }
            setState(false, false);
            refreshView();
            UIHelper.dismissLoadingDialog();
        } else {
            UIHelper.toast(this, serviceResult, raw -> "无内容可以学习");
            UIHelper.dismissLoadingDialog();
            finishActivity();
        }
    }

    private void handleProcessNavigationMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        boolean isSuccess = serviceResult.isSuccess();
        NavigationProcessResponse response = serviceResult.extra(NavigationProcessResponse.class);
        if (response == null) {
            UIHelper.dismissLoadingDialog();
            UIHelper.toast(this, serviceResult, raw -> "处理导航请求失败");
            return;
        }
        boolean hasDeliveryContentInfo = response.getHasDeliveryContentInfo();
        DeliveryContentInfo deliveryContentInfo = response.getDeliveryContentInfo();
        if (response.getNavigationEventType() == NavigationEventType.Start) {
            UIHelper.dismissLoadingDialog();
            if (isSuccess && hasDeliveryContentInfo) {
                currentDeliveryContentInfo = deliveryContentInfo;
                setState(true, false);
                refreshView();
            } else {
                UIHelper.toast(this, "[开始] 无法加载学习内容");
            }
        } else if (response.getNavigationEventType() == NavigationEventType.ResumeAll) {
            UIHelper.dismissLoadingDialog();
            if (isSuccess && hasDeliveryContentInfo) {
                currentDeliveryContentInfo = deliveryContentInfo;
                setState(true, false);
                refreshView();
            } else {
                UIHelper.toast(this, "[恢复] 无法加载学习内容");
            }
        } else if (response.getNavigationEventType() == NavigationEventType.Continue) {
            if (isSuccess && hasDeliveryContentInfo) {
                UIHelper.dismissLoadingDialog();
                currentDeliveryContentInfo = deliveryContentInfo;
                setState(true, false);
                refreshView();
            } else {
                // 尝试到下一个organization中寻找
                if (nextLevel1Item()) {
                    triggerNavigationEvent(NavigationEventType.Continue);
                } else {
                    UIHelper.dismissLoadingDialog();
                    UIHelper.toast(this, "[向后] 禁止向后导航/需先完成当前学习内容/已无学习内容");
                }
            }
        } else if (response.getNavigationEventType() == NavigationEventType.Previous) {
            if (isSuccess && hasDeliveryContentInfo) {
                UIHelper.dismissLoadingDialog();
                currentDeliveryContentInfo = deliveryContentInfo;
                setState(true, false);
                refreshView();
            } else {
                // 尝试到上一个organization中寻找
                if (previousLevel1Item()) {
                    triggerNavigationEvent(NavigationEventType.Previous);
                } else {
                    UIHelper.dismissLoadingDialog();
                    UIHelper.toast(this, "[向前] 禁止向前导航/需先完成当前学习内容/已无学习内容");
                }
            }
        } else if (response.getNavigationEventType() == NavigationEventType.Choose) {
            UIHelper.dismissLoadingDialog();
            if (isSuccess && hasDeliveryContentInfo) {
                currentDeliveryContentInfo = deliveryContentInfo;
                setState(true, false);
                refreshView();
            } else {
                currentLevel1Item = currentLevel1ItemBeforeChoose;
                currentLevel1ItemBeforeChoose = null;
                UIHelper.toast(this, "[跳转] 禁止跳转/需先完成当前学习内容");
            }
        } else if (response.getNavigationEventType() == NavigationEventType.SuspendAll) {
            UIHelper.dismissLoadingDialog();
            if (isSuccess) {
                currentDeliveryContentInfo = null;
                setState(true, true);
                refreshView();
            } else {
                UIHelper.toast(this, "无法暂停");
            }
        } else if (response.getNavigationEventType() == NavigationEventType.UnqualifiedExit) {
            UIHelper.dismissLoadingDialog();
            if (isSuccess) {
                currentDeliveryContentInfo = null;
                setState(false, false);
                refreshView();
            } else {
                UIHelper.toast(this, "无法停止");
            }
        } else if (response.getNavigationEventType() == NavigationEventType.ExitAll) {
            if (shouldFinish) {
                shouldFinish = false;
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        CourseCatalogItemInfo courseCatalogItemInfo = null;
        if (data != null) {
            courseCatalogItemInfo = (CourseCatalogItemInfo) data.getSerializableExtra(IntentParam.COURSE_CATALOG_ITEM_INFO);
        }
        if (requestCode == REQUEST_CODE_CHOOSE_ITEM) {
            if (courseCatalogItemInfo == null) {
                UIHelper.toast(this, "未选择任何活动");
                return;
            }
            if (currentDeliveryContentInfo != null && currentDeliveryContentInfo.getItemId() != null
                    && currentDeliveryContentInfo.getItemId().equals(courseCatalogItemInfo.getItemId())) {
                UIHelper.toast(this, "选择了当前的活动，不进行跳转");
                return;
            }
            currentLevel1ItemBeforeChoose = currentLevel1Item;
            currentLevel1Item = searchParentLevel1Item(courseCatalogItemInfo);
            triggerNavigationEvent(NavigationEventType.Choose, courseCatalogItemInfo.getItemId());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        scormView.destroy();
        scormView = null;

        for (WebView webView : openWebViewList) {
            webView.destroy();
        }
        openWebViewList.clear();
    }

    @Override
    public void onBackPressed() {
        finishActivity();
    }
}
