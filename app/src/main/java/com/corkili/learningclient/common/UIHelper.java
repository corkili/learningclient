package com.corkili.learningclient.common;

import android.content.Context;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.corkili.learningclient.service.ServiceResult;
import com.qmuiteam.qmui.widget.dialog.QMUITipDialog;

import org.apache.commons.lang3.StringUtils;

public class UIHelper {

    private static QMUITipDialog loadingDialog = null;

    public static void disableRadioGroup(RadioGroup radioGroup) {
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            radioGroup.getChildAt(i).setEnabled(false);
        }
        radioGroup.setEnabled(false);
    }

    public static void enableRadioGroup(RadioGroup radioGroup) {
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            radioGroup.getChildAt(i).setEnabled(true);
        }
        radioGroup.setEnabled(true);
    }

    public synchronized static void showLoadingDialog(Context context) {
        if (context != null) {
            dismissLoadingDialog();
            loadingDialog = new QMUITipDialog.Builder(context)
                    .setIconType(QMUITipDialog.Builder.ICON_TYPE_LOADING)
                    .setTipWord("请稍后")
                    .create();
            loadingDialog.setCancelable(false);
            loadingDialog.setCanceledOnTouchOutside(false);
            loadingDialog.show();
        }
    }

    public synchronized static void dismissLoadingDialog() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

    public static void toast(Context context, ServiceResult serviceResult, MessageTranslator translator) {
        if (context == null || serviceResult == null || translator == null) {
            return;
        }
        String raw = serviceResult.msg();
        if (StringUtils.isBlank(raw)) {
            return;
        }
        String message = raw.contains("网络") ? raw : (raw.contains("nologin") ? "登录已失效，请重启APP登录" : translator.translate(raw));
        if (StringUtils.isBlank(message)) {
            return;
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void toast(Context context, String message) {
        if (context == null || StringUtils.isBlank(message)) {
            return;
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

}
