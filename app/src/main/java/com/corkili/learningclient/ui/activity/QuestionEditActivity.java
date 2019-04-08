package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.UIHelper;
import com.corkili.learningclient.generate.protobuf.Info.Answer;
import com.corkili.learningclient.generate.protobuf.Info.EssayAnswer;
import com.corkili.learningclient.generate.protobuf.Info.MultipleChoiceAnswer;
import com.corkili.learningclient.generate.protobuf.Info.MultipleFillingAnswer;
import com.corkili.learningclient.generate.protobuf.Info.QuestionInfo;
import com.corkili.learningclient.generate.protobuf.Info.QuestionType;
import com.corkili.learningclient.generate.protobuf.Info.SingleChoiceAnswer;
import com.corkili.learningclient.generate.protobuf.Info.SingleFillingAnswer;
import com.corkili.learningclient.generate.protobuf.Response.QuestionImportResponse;
import com.corkili.learningclient.generate.protobuf.Response.QuestionUpdateResponse;
import com.corkili.learningclient.service.QuestionService;
import com.corkili.learningclient.service.ServiceResult;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestionEditActivity extends AppCompatActivity {

    private boolean isCreate;
    private QuestionInfo questionInfo;
    private boolean editMode;

    private QMUITopBarLayout topBar;

    private EditText questionEditor;
    private RadioGroup questionTypeSelector;
    private RadioGroup autoCheckSelector;
    private View singleFillingAttachLayout;
    private View multipleFillingAttachLayout;
    private View choiceAttachLayout;
    private View essayAttachLayout;
    private List<View> infoViewList;
    private View answerInfoLayout;

    // single_filling
    private LinearLayout singleFillingAnswerLayout;
    private SingleFillingItemView singleFillingRequiredAnswerView;
    private QMUIRoundButton addSingleFillingAnswerButton;
    private List<SingleFillingItemView> singleFillingAnswerViewList;

    // multiple_filling
    private LinearLayout multipleFillingAnswerLayout;
    private MultipleFillingItemView multipleFillingRequiredAnswerView;
    private QMUIRoundButton addMultipleFillingAnswerButton;
    private List<MultipleFillingItemView> multipleFillingAnswerViewList;

    // choice
    private LinearLayout multipleChoiceAllSelectLayout;
    private RadioGroup choiceAllSelectRadioGroup;
    private TextView choiceInfoView;
    private LinearLayout choiceAnswerLayout;
    private ChoiceItemView choiceRequiredAnswerView1;
    private ChoiceItemView choiceRequiredAnswerView2;
    private QMUIRoundButton addChoiceAnswerButton;
    private List<ChoiceItemView> choiceAnswerViewList;

    // essay
    private EditText essayAnswerEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_edit);

        isCreate = getIntent().getBooleanExtra(IntentParam.IS_CREATE, true);
        questionInfo = (QuestionInfo) getIntent().getSerializableExtra(IntentParam.QUESTION_INFO);
        if (!isCreate && questionInfo == null) {
            throw new RuntimeException("Intent param expected");
        }

        editMode = true;

        topBar = findViewById(R.id.topbar);

        if (isCreate) {
            topBar.setTitle("导入试题");
        } else {
            topBar.setTitle("试题详情");
        }

        questionEditor = findViewById(R.id.question_edit_text_edit_question);
        questionTypeSelector = findViewById(R.id.question_edit_radio_group_question_type);
        autoCheckSelector = findViewById(R.id.question_edit_radio_group_auto_check);
        singleFillingAttachLayout = findViewById(R.id.attach_single_filling_layout);
        multipleFillingAttachLayout = findViewById(R.id.attach_multiple_filling_layout);
        choiceAttachLayout = findViewById(R.id.attach_choice_layout);
        essayAttachLayout = findViewById(R.id.attach_essay_layout);

        infoViewList = new ArrayList<>();
        infoViewList.add(findViewById(R.id.single_filling_info_layout));
        infoViewList.add(findViewById(R.id.multiple_filling_info_layout));
        infoViewList.add(findViewById(R.id.choice_info_layout));
        infoViewList.add(findViewById(R.id.essay_info_layout));
        
        answerInfoLayout = findViewById(R.id.answer_info_layout);

        questionTypeSelector.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.type_single_filling:
                    singleFillingAttachLayout.setVisibility(View.VISIBLE);
                    multipleFillingAttachLayout.setVisibility(View.GONE);
                    choiceAttachLayout.setVisibility(View.GONE);
                    essayAttachLayout.setVisibility(View.GONE);
                    UIHelper.enableRadioGroup(autoCheckSelector);
                    autoCheckSelector.check(R.id.auto_check_yes);
                    break;
                case R.id.type_multiple_filling:
                    singleFillingAttachLayout.setVisibility(View.GONE);
                    multipleFillingAttachLayout.setVisibility(View.VISIBLE);
                    choiceAttachLayout.setVisibility(View.GONE);
                    essayAttachLayout.setVisibility(View.GONE);
                    UIHelper.enableRadioGroup(autoCheckSelector);
                    autoCheckSelector.check(R.id.auto_check_yes);
                    break;
                case R.id.type_single_choice:
                    singleFillingAttachLayout.setVisibility(View.GONE);
                    multipleFillingAttachLayout.setVisibility(View.GONE);
                    choiceAttachLayout.setVisibility(View.VISIBLE);
                    essayAttachLayout.setVisibility(View.GONE);
                    UIHelper.disableRadioGroup(autoCheckSelector);
                    autoCheckSelector.check(R.id.auto_check_yes);
                    multipleChoiceAllSelectLayout.setVisibility(View.GONE);
                    choiceInfoView.setText(R.string.single_choice_info);
                    clearOtherChoiceCheckedStateIfIsSingleChoice(null);
                    break;
                case R.id.type_multiple_choice:
                    singleFillingAttachLayout.setVisibility(View.GONE);
                    multipleFillingAttachLayout.setVisibility(View.GONE);
                    choiceAttachLayout.setVisibility(View.VISIBLE);
                    essayAttachLayout.setVisibility(View.GONE);
                    UIHelper.disableRadioGroup(autoCheckSelector);
                    autoCheckSelector.check(R.id.auto_check_yes);
                    multipleChoiceAllSelectLayout.setVisibility(View.VISIBLE);
                    choiceInfoView.setText(R.string.multiple_choice_info);
                    break;
                case R.id.type_essay:
                    singleFillingAttachLayout.setVisibility(View.GONE);
                    multipleFillingAttachLayout.setVisibility(View.GONE);
                    choiceAttachLayout.setVisibility(View.GONE);
                    essayAttachLayout.setVisibility(View.VISIBLE);
                    UIHelper.disableRadioGroup(autoCheckSelector);
                    autoCheckSelector.check(R.id.auto_check_no);
                    break;
            }
        });

        if (isCreate) {
            topBar.addRightTextButton("保存", R.id.topbar_right_save).setOnClickListener(v -> save());
        }

        topBar.addLeftBackImageButton().setOnClickListener(v -> {
            if (editMode) {
                if (isCreate) {
                    setResult(RESULT_CANCELED);
                    QuestionEditActivity.this.finish();
                } else {
                    switchEditMode();
                }
            } else {
                setResult(RESULT_CANCELED);
                QuestionEditActivity.this.finish();
            }
        });

        // single_filling
        singleFillingAnswerLayout = findViewById(R.id.single_filling_answer_layout);
        singleFillingRequiredAnswerView = new SingleFillingItemView(findViewById(R.id.single_filling_required_answer));
        addSingleFillingAnswerButton = findViewById(R.id.add_a_single_filling_answer);
        singleFillingAnswerViewList = new ArrayList<>();

        singleFillingRequiredAnswerView.deleteView.setVisibility(View.INVISIBLE);
        singleFillingRequiredAnswerView.deleteView.setEnabled(false);

        addSingleFillingAnswerButton.setOnClickListener(v -> {
            View view = LayoutInflater.from(QuestionEditActivity.this)
                    .inflate(R.layout.activity_question_edit_attach_single_filling_item, null);
            SingleFillingItemView itemView = new SingleFillingItemView(view);
            itemView.deleteView.setOnClickListener(tv -> {
                try {
                    addSingleFillingAnswerButton.setEnabled(false);
                    singleFillingAnswerViewList.remove(itemView);
                    singleFillingAnswerLayout.removeView(view);
                } finally {
                    addSingleFillingAnswerButton.setEnabled(true);
                }
            });
            singleFillingAnswerViewList.add(itemView);
            singleFillingAnswerLayout.addView(view);
        });

        // multiple_filling
        multipleFillingAnswerLayout = findViewById(R.id.multiple_filling_answer_layout);
        multipleFillingRequiredAnswerView = new MultipleFillingItemView(findViewById(R.id.multiple_filling_required_answer));
        addMultipleFillingAnswerButton = findViewById(R.id.add_a_multiple_filling_answer);
        multipleFillingAnswerViewList = new ArrayList<>();

        multipleFillingRequiredAnswerView.indexView.setEnabled(false);
        multipleFillingRequiredAnswerView.multipleFillingRequiredAnswerRequiredFillingView.deleteView.setEnabled(false);
        multipleFillingRequiredAnswerView.multipleFillingRequiredAnswerRequiredFillingView.deleteView.setVisibility(View.INVISIBLE);
        multipleFillingRequiredAnswerView.addFillingAnswerInCurrentFillingButton
                .setOnClickListener(v -> {
                    View view = LayoutInflater.from(QuestionEditActivity.this)
                            .inflate(R.layout.activity_question_edit_attach_single_filling_item, null);
                    SingleFillingItemView itemView = new SingleFillingItemView(view);
                    itemView.deleteView.setOnClickListener(tv -> {
                        try {
                            multipleFillingRequiredAnswerView.addFillingAnswerInCurrentFillingButton.setEnabled(false);
                            multipleFillingRequiredAnswerView.multipleFillingAnswerFillingViewList.remove(itemView);
                            multipleFillingRequiredAnswerView.multipleFillingAnswerItemLayout.removeView(view);
                        } finally {
                            multipleFillingRequiredAnswerView.addFillingAnswerInCurrentFillingButton.setEnabled(true);
                        }
                    });
                    multipleFillingRequiredAnswerView.multipleFillingAnswerFillingViewList.add(itemView);
                    multipleFillingRequiredAnswerView.multipleFillingAnswerItemLayout.addView(view);
                });

        addMultipleFillingAnswerButton.setOnClickListener(v -> {
            View view = LayoutInflater.from(QuestionEditActivity.this)
                    .inflate(R.layout.activity_question_edit_attach_multiple_filling_item, null);
            MultipleFillingItemView itemView = new MultipleFillingItemView(view);

            itemView.indexView.setOnClickListener(tv -> {
                try {
                    addMultipleFillingAnswerButton.setEnabled(false);
                    multipleFillingAnswerViewList.remove(itemView);
                    multipleFillingAnswerLayout.removeView(view);
                    resetMultipleFillingAnswerItemIndex();
                } finally {
                    addMultipleFillingAnswerButton.setEnabled(true);
                }
            });

            itemView.multipleFillingRequiredAnswerRequiredFillingView.deleteView.setEnabled(false);
            itemView.multipleFillingRequiredAnswerRequiredFillingView.deleteView.setVisibility(View.INVISIBLE);

            itemView.addFillingAnswerInCurrentFillingButton.setOnClickListener(b -> {
                View innerView = LayoutInflater.from(QuestionEditActivity.this)
                        .inflate(R.layout.activity_question_edit_attach_single_filling_item, null);
                SingleFillingItemView innerItemView = new SingleFillingItemView(innerView);
                innerItemView.deleteView.setOnClickListener(tv -> {
                    try {
                        itemView.addFillingAnswerInCurrentFillingButton.setEnabled(false);
                        itemView.multipleFillingAnswerFillingViewList.remove(innerItemView);
                        itemView.multipleFillingAnswerItemLayout.removeView(innerView);
                    } finally {
                        itemView.addFillingAnswerInCurrentFillingButton.setEnabled(true);
                    }
                });
                itemView.multipleFillingAnswerFillingViewList.add(innerItemView);
                itemView.multipleFillingAnswerItemLayout.addView(innerView);
            });

            multipleFillingAnswerViewList.add(itemView);
            multipleFillingAnswerLayout.addView(view);

            resetMultipleFillingAnswerItemIndex();
        });

        // choice
        multipleChoiceAllSelectLayout = findViewById(R.id.multiple_choice_all_select_layout);
        choiceAllSelectRadioGroup = findViewById(R.id.all_select_radio_group_all_select);
        choiceInfoView = findViewById(R.id.choice_info);
        choiceAnswerLayout = findViewById(R.id.choice_answer_layout);
        choiceRequiredAnswerView1 = new ChoiceItemView(findViewById(R.id.choice_required_answer_1));
        choiceRequiredAnswerView2 = new ChoiceItemView(findViewById(R.id.choice_required_answer_2));
        addChoiceAnswerButton = findViewById(R.id.add_a_choice_answer);
        choiceAnswerViewList = new ArrayList<>();

        choiceRequiredAnswerView1.deleteView.setEnabled(false);
        choiceRequiredAnswerView2.deleteView.setEnabled(false);
        choiceRequiredAnswerView1.deleteView.setVisibility(View.INVISIBLE);
        choiceRequiredAnswerView2.deleteView.setVisibility(View.INVISIBLE);

        choiceRequiredAnswerView1.isCorrectChoiceView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                clearOtherChoiceCheckedStateIfIsSingleChoice(choiceRequiredAnswerView1);
            }
        });

        choiceRequiredAnswerView2.isCorrectChoiceView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                clearOtherChoiceCheckedStateIfIsSingleChoice(choiceRequiredAnswerView2);
            }
        });

        addChoiceAnswerButton.setOnClickListener(v -> {
            View view = LayoutInflater.from(QuestionEditActivity.this)
                    .inflate(R.layout.activity_question_edit_attach_choice_item, null);
            ChoiceItemView itemView = new ChoiceItemView(view);

            itemView.deleteView.setOnClickListener(tv -> {
                try {
                    addChoiceAnswerButton.setEnabled(false);
                    choiceAnswerViewList.remove(itemView);
                    choiceAnswerLayout.removeView(view);
                } finally {
                    addChoiceAnswerButton.setEnabled(true);
                }
            });

            itemView.isCorrectChoiceView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    clearOtherChoiceCheckedStateIfIsSingleChoice(itemView);
                }
            });

            choiceAnswerViewList.add(itemView);
            choiceAnswerLayout.addView(view);
        });

        // essay
        essayAnswerEditor = findViewById(R.id.essay_answer_editor);

        initIfIsUpdate();

        if (!isCreate && editMode) {
            switchEditMode();
        }
    }

    private void resetMultipleFillingAnswerItemIndex() {
        for (int i = 0; i < multipleFillingAnswerViewList.size(); i++) {
            multipleFillingAnswerViewList.get(i).indexView.setText(String.valueOf(i + 2));
        }
    }

    private void clearOtherChoiceCheckedStateIfIsSingleChoice(ChoiceItemView checkedChoice) {
        if (getSelectedQuestionType() == QuestionType.SingleChoice) {
            if (!choiceRequiredAnswerView1.equals(checkedChoice)) {
                choiceRequiredAnswerView1.isCorrectChoiceView.setChecked(false);
            }
            if (!choiceRequiredAnswerView2.equals(checkedChoice)) {
                choiceRequiredAnswerView2.isCorrectChoiceView.setChecked(false);
            }
            for (ChoiceItemView itemView : choiceAnswerViewList) {
                if (!itemView.equals(checkedChoice)) {
                    itemView.isCorrectChoiceView.setChecked(false);
                }
            }
        }
    }

    private void clear() {
        questionEditor.setText("");

        singleFillingRequiredAnswerView.editor.setText("");
        for (SingleFillingItemView itemView : singleFillingAnswerViewList) {
            singleFillingAnswerLayout.removeView(itemView.mView);
        }
        singleFillingAnswerViewList.clear();

        multipleFillingRequiredAnswerView.multipleFillingRequiredAnswerRequiredFillingView.editor.setText("");
        for (SingleFillingItemView itemView : multipleFillingRequiredAnswerView.multipleFillingAnswerFillingViewList) {
            multipleFillingRequiredAnswerView.multipleFillingAnswerItemLayout.removeView(itemView.mView);
        }
        multipleFillingRequiredAnswerView.multipleFillingAnswerFillingViewList.clear();

        for (MultipleFillingItemView multipleFillingItemView : multipleFillingAnswerViewList) {
            multipleFillingAnswerLayout.removeView(multipleFillingItemView.mView);
        }
        multipleFillingAnswerViewList.clear();

        choiceRequiredAnswerView1.editor.setText("");
        choiceRequiredAnswerView1.isCorrectChoiceView.setChecked(false);
        choiceRequiredAnswerView2.editor.setText("");
        choiceRequiredAnswerView2.isCorrectChoiceView.setChecked(false);
        for (ChoiceItemView itemView : choiceAnswerViewList) {
            choiceAnswerLayout.removeView(itemView.mView);
        }
        choiceAnswerViewList.clear();

    }

    private void switchEditMode() {
        if (editMode) {
            clear();
            initIfIsUpdate();
            editMode = false;
            questionEditor.setEnabled(false);
            UIHelper.disableRadioGroup(autoCheckSelector);
            for (View view : infoViewList) {
                view.setVisibility(View.GONE);
            }
            answerInfoLayout.setVisibility(View.VISIBLE);
            
            singleFillingRequiredAnswerView.deleteView.setVisibility(View.GONE);
            singleFillingRequiredAnswerView.editor.setEnabled(false);
            addSingleFillingAnswerButton.setEnabled(false);
            addSingleFillingAnswerButton.setVisibility(View.GONE);
            for (SingleFillingItemView itemView : singleFillingAnswerViewList) {
                itemView.deleteView.setVisibility(View.GONE);
                itemView.editor.setEnabled(false);
            }
            
            multipleFillingRequiredAnswerView.indexView.setEnabled(false);
            multipleFillingRequiredAnswerView.addFillingAnswerInCurrentFillingButton.setEnabled(false);
            multipleFillingRequiredAnswerView.addFillingAnswerInCurrentFillingButton.setVisibility(View.GONE);
            multipleFillingRequiredAnswerView.multipleFillingRequiredAnswerRequiredFillingView.deleteView.setVisibility(View.GONE);
            multipleFillingRequiredAnswerView.multipleFillingRequiredAnswerRequiredFillingView.editor.setEnabled(false);
            for (SingleFillingItemView itemView : multipleFillingRequiredAnswerView.multipleFillingAnswerFillingViewList) {
                itemView.deleteView.setVisibility(View.GONE);
                itemView.editor.setEnabled(false);
            }
            addMultipleFillingAnswerButton.setEnabled(false);
            addMultipleFillingAnswerButton.setVisibility(View.GONE);
            for (MultipleFillingItemView multipleFillingItemView : multipleFillingAnswerViewList) {
                multipleFillingItemView.indexView.setEnabled(false);
                multipleFillingItemView.addFillingAnswerInCurrentFillingButton.setEnabled(false);
                multipleFillingItemView.addFillingAnswerInCurrentFillingButton.setVisibility(View.GONE);
                multipleFillingItemView.multipleFillingRequiredAnswerRequiredFillingView.deleteView.setVisibility(View.GONE);
                multipleFillingItemView.multipleFillingRequiredAnswerRequiredFillingView.editor.setEnabled(false);
                for (SingleFillingItemView itemView : multipleFillingItemView.multipleFillingAnswerFillingViewList) {
                    itemView.deleteView.setVisibility(View.GONE);
                    itemView.editor.setEnabled(false);
                }
            }
            
            choiceRequiredAnswerView1.editor.setEnabled(false);
            choiceRequiredAnswerView1.deleteView.setEnabled(false);
            choiceRequiredAnswerView1.deleteView.setVisibility(View.GONE);
            choiceRequiredAnswerView1.isCorrectChoiceView.setEnabled(false);
            if (!choiceRequiredAnswerView1.isCorrectChoiceView.isChecked()) {
                choiceRequiredAnswerView1.isCorrectChoiceView.setVisibility(View.INVISIBLE);
            }
            choiceRequiredAnswerView2.editor.setEnabled(false);
            choiceRequiredAnswerView2.deleteView.setEnabled(false);
            choiceRequiredAnswerView2.deleteView.setVisibility(View.GONE);
            choiceRequiredAnswerView2.isCorrectChoiceView.setEnabled(false);
            if (!choiceRequiredAnswerView2.isCorrectChoiceView.isChecked()) {
                choiceRequiredAnswerView2.isCorrectChoiceView.setVisibility(View.INVISIBLE);
            }
            addChoiceAnswerButton.setEnabled(false);
            addChoiceAnswerButton.setVisibility(View.GONE);
            for (ChoiceItemView itemView : choiceAnswerViewList) {
                itemView.editor.setEnabled(false);
                itemView.deleteView.setEnabled(false);
                itemView.deleteView.setVisibility(View.GONE);
                itemView.isCorrectChoiceView.setEnabled(false);
                if (!itemView.isCorrectChoiceView.isChecked()) {
                    itemView.isCorrectChoiceView.setVisibility(View.INVISIBLE);
                }
            }
            UIHelper.disableRadioGroup(choiceAllSelectRadioGroup);

            essayAnswerEditor.setEnabled(false);

            topBar.setTitle("试题详情");

            topBar.removeAllRightViews();
            topBar.addRightImageButton(R.drawable.ic_more_24dp, R.id.topbar_right_more).setOnClickListener(v -> {
                new QMUIBottomSheet.BottomListSheetBuilder(QuestionEditActivity.this)
                        .addItem("编辑")
                        .addItem("删除")
                        .setOnSheetItemClickListener((dialog, itemView, position, tag) -> {
                            if (position == 0) {
                                if (!isCreate && !editMode) {
                                    switchEditMode();
                                }
                            } else if (position == 1) {
                                if (!isCreate) {
                                    QuestionService.getInstance().deleteQuestion(handler, questionInfo.getQuestionId());
                                }
                            }
                            dialog.dismiss();
                        })
                        .build()
                        .show();
            });

//            clear();
//            initIfIsUpdate();
        } else {
            editMode = true;
            questionEditor.setEnabled(true);
            switch (getSelectedQuestionType()) {
                case SingleFilling:
                case MultipleFilling:
                    UIHelper.enableRadioGroup(autoCheckSelector);
                    break;
                case SingleChoice:
                case MultipleChoice:
                case Essay:
                    UIHelper.disableRadioGroup(autoCheckSelector);
                    break;
            }
            for (View view : infoViewList) {
                view.setVisibility(View.VISIBLE);
            }
            answerInfoLayout.setVisibility(View.GONE);

            singleFillingRequiredAnswerView.deleteView.setVisibility(View.INVISIBLE);
            singleFillingRequiredAnswerView.editor.setEnabled(true);
            addSingleFillingAnswerButton.setEnabled(true);
            addSingleFillingAnswerButton.setVisibility(View.VISIBLE);
            for (SingleFillingItemView itemView : singleFillingAnswerViewList) {
                itemView.deleteView.setVisibility(View.VISIBLE);
                itemView.editor.setEnabled(true);
            }

            multipleFillingRequiredAnswerView.indexView.setEnabled(true);
            multipleFillingRequiredAnswerView.addFillingAnswerInCurrentFillingButton.setEnabled(true);
            multipleFillingRequiredAnswerView.addFillingAnswerInCurrentFillingButton.setVisibility(View.VISIBLE);
            multipleFillingRequiredAnswerView.multipleFillingRequiredAnswerRequiredFillingView.deleteView.setVisibility(View.INVISIBLE);
            multipleFillingRequiredAnswerView.multipleFillingRequiredAnswerRequiredFillingView.editor.setEnabled(true);
            for (SingleFillingItemView itemView : multipleFillingRequiredAnswerView.multipleFillingAnswerFillingViewList) {
                itemView.deleteView.setVisibility(View.VISIBLE);
                itemView.editor.setEnabled(true);
            }
            addMultipleFillingAnswerButton.setEnabled(true);
            addMultipleFillingAnswerButton.setVisibility(View.VISIBLE);
            for (MultipleFillingItemView multipleFillingItemView : multipleFillingAnswerViewList) {
                multipleFillingItemView.indexView.setEnabled(true);
                multipleFillingItemView.addFillingAnswerInCurrentFillingButton.setEnabled(true);
                multipleFillingItemView.addFillingAnswerInCurrentFillingButton.setVisibility(View.VISIBLE);
                multipleFillingItemView.multipleFillingRequiredAnswerRequiredFillingView.deleteView.setVisibility(View.INVISIBLE);
                multipleFillingItemView.multipleFillingRequiredAnswerRequiredFillingView.editor.setEnabled(true);
                for (SingleFillingItemView itemView : multipleFillingItemView.multipleFillingAnswerFillingViewList) {
                    itemView.deleteView.setVisibility(View.VISIBLE);
                    itemView.editor.setEnabled(true);
                }
            }

            choiceRequiredAnswerView1.editor.setEnabled(true);
            choiceRequiredAnswerView1.deleteView.setEnabled(true);
            choiceRequiredAnswerView1.deleteView.setVisibility(View.INVISIBLE);
            choiceRequiredAnswerView1.isCorrectChoiceView.setEnabled(true);
            if (!choiceRequiredAnswerView1.isCorrectChoiceView.isChecked()) {
                choiceRequiredAnswerView1.isCorrectChoiceView.setVisibility(View.VISIBLE);
            }
            choiceRequiredAnswerView2.editor.setEnabled(true);
            choiceRequiredAnswerView2.deleteView.setEnabled(true);
            choiceRequiredAnswerView2.deleteView.setVisibility(View.INVISIBLE);
            choiceRequiredAnswerView2.isCorrectChoiceView.setEnabled(true);
            if (!choiceRequiredAnswerView2.isCorrectChoiceView.isChecked()) {
                choiceRequiredAnswerView2.isCorrectChoiceView.setVisibility(View.VISIBLE);
            }
            addChoiceAnswerButton.setEnabled(true);
            addChoiceAnswerButton.setVisibility(View.VISIBLE);
            for (ChoiceItemView itemView : choiceAnswerViewList) {
                itemView.editor.setEnabled(true);
                itemView.deleteView.setEnabled(true);
                itemView.deleteView.setVisibility(View.VISIBLE);
                itemView.isCorrectChoiceView.setEnabled(true);
                if (!itemView.isCorrectChoiceView.isChecked()) {
                    itemView.isCorrectChoiceView.setVisibility(View.VISIBLE);
                }
            }
            UIHelper.enableRadioGroup(choiceAllSelectRadioGroup);

            essayAnswerEditor.setEnabled(true);

            topBar.setTitle("编辑试题");

            topBar.removeAllRightViews();
            topBar.addRightTextButton("保存", R.id.topbar_right_save).setOnClickListener(v -> save());

        }
    }

    private void save() {
        Answer answer =  getAnswer();
        String question = questionEditor.getText().toString().trim();
        QuestionType questionType = getSelectedQuestionType();
        Boolean autoCheck = getSelectedAutoCheck();
        Map<Integer, String> choices = getChoices();
        if (answer == null) {
            return;
        }
        if (isCreate) {
            QuestionService.getInstance().createQuestion(handler, question, questionType,
                    autoCheck, choices, answer);
        } else {
            if (question.equals(questionInfo.getQuestion())) {
                question = null;
            }
            if (autoCheck == questionInfo.getAutoCheck()) {
                autoCheck = null;
            }
            boolean updateChoices = false;
            if (choices != null && !choices.equals(questionInfo.getChoicesMap())) {
                updateChoices = true;
            }
            if (answer.equals(questionInfo.getAnswer())) {
                answer = null;
            }
            QuestionService.getInstance().updateQuestion(handler, questionInfo.getQuestionId(),
                    questionType, question, autoCheck, updateChoices, choices, answer);
        }
    }

    private void initIfIsUpdate() {
        if (isCreate) {
            return;
        }
        questionEditor.setText(questionInfo.getQuestion());
        switch (questionInfo.getQuestionType()) {
            case SingleFilling:
                questionTypeSelector.check(R.id.type_single_filling);
                break;
            case MultipleFilling:
                questionTypeSelector.check(R.id.type_multiple_filling);
                break;
            case SingleChoice:
                questionTypeSelector.check(R.id.type_single_choice);
                break;
            case MultipleChoice:
                questionTypeSelector.check(R.id.type_multiple_choice);
                break;
            case Essay:
                questionTypeSelector.check(R.id.type_essay);
                break;
        }
        UIHelper.disableRadioGroup(questionTypeSelector);
        if (questionInfo.getAutoCheck()) {
            autoCheckSelector.check(R.id.auto_check_yes);
        } else {
            autoCheckSelector.check(R.id.auto_check_no);
        }
        if (questionInfo.getQuestionType() == QuestionType.SingleChoice
                || questionInfo.getQuestionType() == QuestionType.MultipleChoice
                || questionInfo.getQuestionType() == QuestionType.Essay) {
            UIHelper.disableRadioGroup(autoCheckSelector);
        }
        if (questionInfo.getQuestionType() == QuestionType.MultipleChoice) {
            MultipleChoiceAnswer multipleChoiceAnswer = questionInfo.getAnswer().getMultipleChoiceAnswer();
            if (multipleChoiceAnswer.getSelectAllIsCorrect()) {
                choiceAllSelectRadioGroup.check(R.id.all_select_yes);
            } else {
                choiceAllSelectRadioGroup.check(R.id.all_select_no);
            }
        }
        if (questionInfo.getQuestionType() == QuestionType.SingleFilling) {
            SingleFillingAnswer singleFillingAnswer = questionInfo.getAnswer().getSingleFillingAnswer();
            List<String> answerList = singleFillingAnswer.getAnswerList();
            singleFillingRequiredAnswerView.editor.setText(answerList.get(0));
            for (int i = 1; i < answerList.size(); i++) {
                addSingleFillingAnswerButton.performClick();
                singleFillingAnswerViewList.get(i - 1).editor.setText(answerList.get(i));
            }
        } else if (questionInfo.getQuestionType() == QuestionType.MultipleFilling) {
            MultipleFillingAnswer multipleFillingAnswer = questionInfo.getAnswer().getMultipleFillingAnswer();
            List<Integer> indexList = new ArrayList<>(multipleFillingAnswer.getAnswerMap().keySet());
            Collections.sort(indexList, (o1, o2) -> o1 - o2);
            SingleFillingAnswer singleFillingAnswer = multipleFillingAnswer.getAnswerMap().get(indexList.get(0));
            List<String> answerList = singleFillingAnswer.getAnswerList();
            multipleFillingRequiredAnswerView.multipleFillingRequiredAnswerRequiredFillingView.editor.setText(answerList.get(0));
            for (int i = 1; i < answerList.size(); i++) {
                multipleFillingRequiredAnswerView.addFillingAnswerInCurrentFillingButton.performClick();
                multipleFillingRequiredAnswerView.multipleFillingAnswerFillingViewList.get(i - 1).editor.setText(answerList.get(i));
            }
            for (int i = 1; i < indexList.size(); i++) {
                singleFillingAnswer = multipleFillingAnswer.getAnswerMap().get(indexList.get(i));
                answerList = singleFillingAnswer.getAnswerList();
                addMultipleFillingAnswerButton.performClick();
                multipleFillingAnswerViewList.get(i - 1).indexView.setText(String.valueOf(i + 1));
                multipleFillingAnswerViewList.get(i - 1).multipleFillingRequiredAnswerRequiredFillingView.editor.setText(answerList.get(0));
                for (int j = 1; j < answerList.size(); j++) {
                    multipleFillingAnswerViewList.get(i - 1).addFillingAnswerInCurrentFillingButton.performClick();
                    multipleFillingAnswerViewList.get(i - 1).multipleFillingAnswerFillingViewList.get(j - 1).editor.setText(answerList.get(j));
                }
            }
        } else if (questionInfo.getQuestionType() == QuestionType.SingleChoice
                || questionInfo.getQuestionType() == QuestionType.MultipleChoice) {
            Map<Integer, String> choices = questionInfo.getChoicesMap();
            List<Integer> indexList = new ArrayList<>(choices.keySet());
            Collections.sort(indexList, (o1, o2) -> o1 - o2);
            choiceRequiredAnswerView1.editor.setText(choices.get(indexList.get(0)));
            choiceRequiredAnswerView2.editor.setText(choices.get(indexList.get(1)));
            for (int i = 2; i < indexList.size(); i++) {
                addChoiceAnswerButton.performClick();
                choiceAnswerViewList.get(i - 2).editor.setText(choices.get(indexList.get(i)));
            }
            List<Integer> correctChoices = new ArrayList<>();
            if (questionInfo.getQuestionType() == QuestionType.SingleChoice) {
                SingleChoiceAnswer singleChoiceAnswer = questionInfo.getAnswer().getSingleChoiceAnswer();
                correctChoices.add(singleChoiceAnswer.getChoice());
            } else {
                MultipleChoiceAnswer multipleChoiceAnswer = questionInfo.getAnswer().getMultipleChoiceAnswer();
                correctChoices.addAll(multipleChoiceAnswer.getChoiceList());
            }
            for (Integer correctChoice : correctChoices) {
                if (correctChoice == 1) {
                    choiceRequiredAnswerView1.isCorrectChoiceView.setChecked(true);
                } else if (correctChoice == 2) {
                    choiceRequiredAnswerView2.isCorrectChoiceView.setChecked(true);
                } else {
                    choiceAnswerViewList.get(correctChoice - 3).isCorrectChoiceView.setChecked(true);
                }
            }
        } else if (questionInfo.getQuestionType() == QuestionType.Essay) {
            EssayAnswer essayAnswer = questionInfo.getAnswer().getEssayAnswer();
            essayAnswerEditor.setText(essayAnswer.getText());
        }
    }

    private boolean getSelectedAutoCheck() {
        switch (autoCheckSelector.getCheckedRadioButtonId()) {
            case R.id.auto_check_yes:
                return true;
            case R.id.auto_check_no:
                return false;
        }
        return false;
    }

    private boolean getSelectedAllSelectIsCorrect() {
        switch (choiceAllSelectRadioGroup.getCheckedRadioButtonId()) {
            case R.id.all_select_yes:
                return true;
            case R.id.all_select_no:
                return false;
        }
        return true;
    }

    private QuestionType getSelectedQuestionType() {
        switch (questionTypeSelector.getCheckedRadioButtonId()) {
            case R.id.type_single_filling:
                return QuestionType.SingleFilling;
            case R.id.type_multiple_filling:
                return QuestionType.MultipleFilling;
            case R.id.type_single_choice:
                return QuestionType.SingleChoice;
            case R.id.type_multiple_choice:
                return QuestionType.MultipleChoice;
            case R.id.type_essay:
                return QuestionType.Essay;
        }
        return QuestionType.UNRECOGNIZED;
    }

    private Map<Integer, String> getChoices() {
        if (getSelectedQuestionType() != QuestionType.SingleChoice
                && getSelectedQuestionType() != QuestionType.MultipleChoice) {
            return null;
        }
        @SuppressLint("UseSparseArrays")
        Map<Integer, String> choices = new HashMap<>();
        choices.put(1, choiceRequiredAnswerView1.editor.getText().toString().trim());
        choices.put(2, choiceRequiredAnswerView2.editor.getText().toString().trim());
        for (int i = 0; i < choiceAnswerViewList.size(); i++) {
            ChoiceItemView itemView = choiceAnswerViewList.get(i);
            choices.put(i + 3, itemView.editor.getText().toString().trim());
        }
        return choices;
    }

    private Answer getAnswer() {
        QuestionType questionType = getSelectedQuestionType();
        String tmp;
        if (questionType == QuestionType.SingleFilling) {
            List<String> answerList = new ArrayList<>();
            tmp = singleFillingRequiredAnswerView.editor.getText().toString().trim();
            if (StringUtils.isBlank(tmp)) {
                UIHelper.toast(this, "答案内容不能为空");
                return null;
            }
            answerList.add(tmp);
            for (SingleFillingItemView singleFillingItemView : singleFillingAnswerViewList) {
                tmp = singleFillingItemView.editor.getText().toString().trim();
                if (StringUtils.isBlank(tmp)) {
                    UIHelper.toast(this, "答案内容不能为空");
                    return null;
                }
                answerList.add(tmp);
            }
            SingleFillingAnswer singleFillingAnswer = SingleFillingAnswer.newBuilder()
                    .addAllAnswer(answerList)
                    .build();
            return Answer.newBuilder().setSingleFillingAnswer(singleFillingAnswer).build();
        } else if (questionType == QuestionType.MultipleFilling) {
            @SuppressLint("UseSparseArrays")
            Map<Integer, SingleFillingAnswer> answerMap = new HashMap<>();
            List<String> answerList = new ArrayList<>();
            tmp = multipleFillingRequiredAnswerView.multipleFillingRequiredAnswerRequiredFillingView
                    .editor.getText().toString().trim();
            if (StringUtils.isBlank(tmp)) {
                UIHelper.toast(this, "答案内容不能为空");
                return null;
            }
            answerList.add(tmp);
            for (SingleFillingItemView singleFillingItemView : multipleFillingRequiredAnswerView.multipleFillingAnswerFillingViewList) {
                tmp = singleFillingItemView.editor.getText().toString().trim();
                if (StringUtils.isBlank(tmp)) {
                    UIHelper.toast(this, "答案内容不能为空");
                    return null;
                }
                answerList.add(tmp);
            }
            answerMap.put(1, SingleFillingAnswer.newBuilder().addAllAnswer(answerList).build());
            for (int i = 0; i < multipleFillingAnswerViewList.size(); i++) {
                MultipleFillingItemView itemView = multipleFillingAnswerViewList.get(i);
                answerList.clear();
                tmp = itemView.multipleFillingRequiredAnswerRequiredFillingView
                        .editor.getText().toString().trim();
                if (StringUtils.isBlank(tmp)) {
                    UIHelper.toast(this, "答案内容不能为空");
                    return null;
                }
                answerList.add(tmp);
                for (SingleFillingItemView singleFillingItemView : itemView.multipleFillingAnswerFillingViewList) {
                    tmp = singleFillingItemView.editor.getText().toString().trim();
                    if (StringUtils.isBlank(tmp)) {
                        UIHelper.toast(this, "答案内容不能为空");
                        return null;
                    }
                    answerList.add(tmp);
                }
                answerMap.put(i + 2, SingleFillingAnswer.newBuilder().addAllAnswer(answerList).build());
            }
            MultipleFillingAnswer multipleFillingAnswer = MultipleFillingAnswer.newBuilder().putAllAnswer(answerMap).build();
            return Answer.newBuilder().setMultipleFillingAnswer(multipleFillingAnswer).build();
        } else if (questionType == QuestionType.SingleChoice) {
            int correctChoice = 0;
            if (choiceRequiredAnswerView1.isCorrectChoiceView.isChecked()) {
                correctChoice = 1;
            } else if (choiceRequiredAnswerView2.isCorrectChoiceView.isChecked()) {
                correctChoice = 2;
            } else {
                for (int i = 0; i < choiceAnswerViewList.size(); i++) {
                    ChoiceItemView itemView = choiceAnswerViewList.get(i);
                    if (itemView.isCorrectChoiceView.isChecked()) {
                        correctChoice = i + 3;
                        break;
                    }
                }
            }
            if (correctChoice == 0) {
                UIHelper.toast(this, "单选题必须提供一个正确选项");
                return null;
            }
            SingleChoiceAnswer singleChoiceAnswer = SingleChoiceAnswer
                    .newBuilder().setChoice(correctChoice).build();
            return Answer.newBuilder().setSingleChoiceAnswer(singleChoiceAnswer).build();
        } else if (questionType == QuestionType.MultipleChoice) {
            List<Integer> choiceList = new ArrayList<>();
            if (choiceRequiredAnswerView1.isCorrectChoiceView.isChecked()) {
                choiceList.add(1);
            }
            if (choiceRequiredAnswerView2.isCorrectChoiceView.isChecked()) {
                choiceList.add(2);
            }
            for (int i = 0; i < choiceAnswerViewList.size(); i++) {
                ChoiceItemView itemView = choiceAnswerViewList.get(i);
                if (itemView.isCorrectChoiceView.isChecked()) {
                    choiceList.add(i + 3);
                    break;
                }
            }
            if (choiceList.isEmpty()) {
                UIHelper.toast(this, "多选题必须提供至少一个正确选项");
                return null;
            }
            MultipleChoiceAnswer multipleChoiceAnswer = MultipleChoiceAnswer.newBuilder()
                    .addAllChoice(choiceList)
                    .setSelectAllIsCorrect(getSelectedAllSelectIsCorrect())
                    .build();
            return Answer.newBuilder().setMultipleChoiceAnswer(multipleChoiceAnswer).build();
        } else if (questionType == QuestionType.Essay) {
            tmp = essayAnswerEditor.getText().toString().trim();
            if (StringUtils.isBlank(tmp)) {
                UIHelper.toast(this, "答案内容不能为空");
                return null;
            }
            EssayAnswer essayAnswer = EssayAnswer.newBuilder().setText(tmp).build();
            return Answer.newBuilder().setEssayAnswer(essayAnswer).build();
        } else {
            UIHelper.toast(this, "系统错误：未正确设置答案");
            return null;
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == QuestionService.CREATE_QUESTION_MSG) {
                handleCreateQuestionMsg(msg);
            } else if (msg.what == QuestionService.UPDATE_QUESTION_MSG) {
                handleUpdateQuestionMsg(msg);
            } else if (msg.what == QuestionService.DELETE_QUESTION_MSG) {
                handleDeleteQuestionMsg(msg);
            }
        }
    };

    private void handleCreateQuestionMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        UIHelper.toast(this, serviceResult, raw -> serviceResult.isSuccess() ? "导入试题成功" : "导入试题失败");
        if (serviceResult.isSuccess()) {
            QuestionInfo questionInfo = serviceResult.extra(QuestionImportResponse.class).getQuestionInfo();
            if (questionInfo != null) {
                Intent intent = new Intent();
                intent.putExtra(IntentParam.QUESTION_INFO, questionInfo);
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }

    private void handleUpdateQuestionMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        UIHelper.toast(this, serviceResult, raw -> serviceResult.isSuccess() ? "更新试题成功" : "更新试题失败");
        if (serviceResult.isSuccess()) {
            QuestionInfo questionInfo = serviceResult.extra(QuestionUpdateResponse.class).getQuestionInfo();
            if (questionInfo != null) {
                this.questionInfo = questionInfo;
                clear();
                initIfIsUpdate();
                if (editMode) {
                    switchEditMode();
                }
            }
        }
    }

    private void handleDeleteQuestionMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        UIHelper.toast(this, serviceResult, raw -> serviceResult.isSuccess() ? "删除试题成功" : "该试题已被作业/考试引用，无法删除");
        if (serviceResult.isSuccess()) {
            Intent intent = new Intent();
            intent.putExtra(IntentParam.DELETE_QUESTION, true);
            intent.putExtra(IntentParam.QUESTION_INFO, questionInfo);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (isCreate) {
            setResult(RESULT_CANCELED);
        } else {
            Intent intent = new Intent();
            intent.putExtra(IntentParam.DELETE_QUESTION, false);
            intent.putExtra(IntentParam.QUESTION_INFO, questionInfo);
            setResult(RESULT_OK, intent);
        }
        super.onBackPressed();
    }

    private class SingleFillingItemView {
        private View mView;
        private ImageView deleteView;
        private EditText editor;

        SingleFillingItemView(View view) {
            this.mView = view;
            deleteView = mView.findViewById(R.id.single_filling_answer_delete);
            editor = mView.findViewById(R.id.single_filling_answer_editor);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SingleFillingItemView that = (SingleFillingItemView) o;

            return mView != null ? mView.equals(that.mView) : that.mView == null;
        }

        @Override
        public int hashCode() {
            return mView != null ? mView.hashCode() : 0;
        }
    }

    private class MultipleFillingItemView {
        private View mView;
        private TextView indexView;
        private LinearLayout multipleFillingAnswerItemLayout;
        private SingleFillingItemView multipleFillingRequiredAnswerRequiredFillingView;
        private List<SingleFillingItemView> multipleFillingAnswerFillingViewList;
        private QMUIRoundButton addFillingAnswerInCurrentFillingButton;

        MultipleFillingItemView(View view) {
            this.mView = view;
            indexView = mView.findViewById(R.id.multiple_filling_answer_index);
            multipleFillingAnswerItemLayout = mView.findViewById(R.id.multiple_filling_answer_item_layout);
            multipleFillingRequiredAnswerRequiredFillingView = new SingleFillingItemView(
                    mView.findViewById(R.id.multiple_filling_required_answer_required_filling));
            multipleFillingAnswerFillingViewList = new ArrayList<>();
            addFillingAnswerInCurrentFillingButton = mView.findViewById(R.id.add_a_filling_answer_in_current_filling);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MultipleFillingItemView itemView = (MultipleFillingItemView) o;

            return mView != null ? mView.equals(itemView.mView) : itemView.mView == null;
        }

        @Override
        public int hashCode() {
            return mView != null ? mView.hashCode() : 0;
        }
    }

    private class ChoiceItemView {
        private View mView;
        private ImageView deleteView;
        private EditText editor;
        private CheckBox isCorrectChoiceView;

        ChoiceItemView(View view) {
            this.mView = view;
            deleteView = mView.findViewById(R.id.choice_answer_delete);
            editor = mView.findViewById(R.id.choice_answer_editor);
            isCorrectChoiceView = mView.findViewById(R.id.is_correct_choice_check_box);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ChoiceItemView that = (ChoiceItemView) o;

            return mView != null ? mView.equals(that.mView) : that.mView == null;
        }

        @Override
        public int hashCode() {
            return mView != null ? mView.hashCode() : 0;
        }
    }

}
