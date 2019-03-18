package com.corkili.learningclient.common;

import android.widget.RadioGroup;

public class UIHelper {

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

}
