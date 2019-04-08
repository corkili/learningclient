package com.corkili.learningclient.common;

import android.content.Context;
import android.widget.RadioGroup;

import com.qmuiteam.qmui.widget.dialog.QMUIDialog.MessageDialogBuilder;
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
        if (context != null && loadingDialog == null) {
            loadingDialog = new QMUITipDialog.Builder(context)
                    .setIconType(QMUITipDialog.Builder.ICON_TYPE_LOADING)
                    .setTipWord("请稍后")
                    .create();
        }
        if (!loadingDialog.isShowing()) {
            loadingDialog.show();
        }
    }

    public synchronized static void dismissLoadingDialog() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

    public static void showFailTip(Context context, String message) {
        if (context == null || StringUtils.isBlank(message)) {
            return;
        }
        new MessageDialogBuilder(context)
                .setMessage(message)
                .addAction("确定", (dialog, index) -> dialog.dismiss()).show();
    }


}
