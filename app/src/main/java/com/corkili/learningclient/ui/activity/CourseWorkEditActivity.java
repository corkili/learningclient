package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.UIHelper;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkQuestionInfo;
import com.corkili.learningclient.generate.protobuf.Info.QuestionInfo;
import com.corkili.learningclient.generate.protobuf.Response.CourseWorkCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.CourseWorkUpdateResponse;
import com.corkili.learningclient.generate.protobuf.Response.QuestionFindAllResponse;
import com.corkili.learningclient.service.CourseWorkService;
import com.corkili.learningclient.service.QuestionService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.ui.adapter.QuestionRecyclerViewAdapter;
import com.corkili.learningclient.ui.adapter.QuestionRecyclerViewAdapter.ViewHolder;
import com.corkili.learningclient.ui.other.MyRecyclerViewDivider;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.qmuiteam.qmui.widget.roundwidget.QMUIRoundButton;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CourseWorkEditActivity extends AppCompatActivity implements QuestionRecyclerViewAdapter.OnItemInteractionListener {

    private static final int REQUEST_CODE_SELECT_QUESTION = 0xF1;

    private boolean isCreate;
    private CourseWorkInfo courseWorkInfo;
    private CourseInfo courseInfo;
    private Calendar deadline;

    private QMUITopBarLayout topBar;
    private EditText courseWorkNameEditor;
    private RadioGroup openSelector;
    private RadioGroup hasDdlSelector;
    private EditText deadlineEditor;
    private QMUIRoundButton addWorkQuestionButton;
    private View openLayout;
    private View deadlineLayout;

    private RecyclerView recyclerView;
    private QuestionRecyclerViewAdapter recyclerViewAdapter;

    private List<QuestionInfo> questionInfos;
    private Map<Long, QuestionInfo> allQuestionInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_work_edit);

        isCreate = getIntent().getBooleanExtra(IntentParam.IS_CREATE, true);
        courseWorkInfo = (CourseWorkInfo) getIntent().getSerializableExtra(IntentParam.COURSE_WORK_INFO);
        courseInfo = (CourseInfo) getIntent().getSerializableExtra(IntentParam.COURSE_INFO);

        if (courseInfo == null || (!isCreate && courseWorkInfo == null)) {
            throw new RuntimeException("Intent param expected");
        }

        topBar = findViewById(R.id.topbar);

        topBar.setTitle(isCreate ? "布置作业" : "修改作业");
        topBar.addLeftBackImageButton().setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        if (!isCreate) {
            topBar.addRightImageButton(R.drawable.ic_remove_24dp, R.id.topbar_right_delete).setOnClickListener(v -> {
                if (isCreate) {
                    return;
                }
                new QMUIDialog.MessageDialogBuilder(this)
                        .setTitle("删除作业")
                        .setMessage("确定删除该作业？")
                        .addAction("取消", (dialog, index) -> dialog.dismiss())
                        .addAction(0, "删除", QMUIDialogAction.ACTION_PROP_NEGATIVE, (dialog, index) -> {
                            CourseWorkService.getInstance().deleteCourseWork(handler, courseWorkInfo.getCourseWorkId());
                            dialog.dismiss();
                        })
                        .show();
            });
        }

        topBar.addRightImageButton(R.drawable.ic_save_24dp, R.id.topbar_right_save).setOnClickListener(v -> {
            boolean hasDeadline = hasDdlSelector.getCheckedRadioButtonId() == R.id.has_ddl_yes;
            if (hasDeadline) {
                if (StringUtils.isBlank(deadlineEditor.getText().toString())) {
                    UIHelper.toast(this, "请设置截止日期");
                    return;
                }
                Calendar today = Calendar.getInstance(Locale.CHINA);
                today.set(Calendar.HOUR_OF_DAY, 23);
                today.set(Calendar.MINUTE, 59);
                today.set(Calendar.SECOND, 59);
                today.set(Calendar.MILLISECOND, 999);
                if (deadline.before(today)) {
                    UIHelper.toast(this, "请设置今天及以后的日期");
                    return;
                }
            }
            String courseWorkName = courseWorkNameEditor.getText().toString().trim();
            Boolean open = openSelector.getCheckedRadioButtonId() == R.id.open_yes;
            Map<Integer, Long> selectedQuestions = new HashMap<>();
            for (int i = 0; i < questionInfos.size(); i++) {
                QuestionInfo questionInfo = questionInfos.get(i);
                selectedQuestions.put(i + 1, questionInfo.getQuestionId());
            }
            if (isCreate) {
                CourseWorkService.getInstance().createCourseWork(handler, courseWorkName,
                        courseInfo.getCourseId(), hasDeadline ? deadline.getTime() : null, selectedQuestions);
            } else {
                if (courseWorkName.equals(courseWorkInfo.getCourseWorkName())) {
                    courseWorkName = null;
                }
                if (open == courseWorkInfo.getOpen()) {
                    open = null;
                }
                boolean updateDeadline = hasDeadline != courseWorkInfo.getHasDeadline() ||
                        deadline.getTimeInMillis() != courseWorkInfo.getDeadline();
                boolean updateQuestion = false;
                if (selectedQuestions.size() == courseWorkInfo.getCourseWorkQuestionInfoCount()) {
                    for (CourseWorkQuestionInfo questionInfo : courseWorkInfo.getCourseWorkQuestionInfoList()) {
                        if (!selectedQuestions.containsKey(questionInfo.getIndex())
                                || selectedQuestions.get(questionInfo.getIndex()) != questionInfo.getQuestionId()) {
                            updateQuestion = true;
                            break;
                        }
                    }
                } else {
                    updateQuestion = true;
                }
                if (courseWorkInfo.getOpen()) {
                    open = null;
                    updateQuestion = false;
                    selectedQuestions = null;
                }
                CourseWorkService.getInstance().updateCourseWork(handler, courseWorkInfo.getCourseWorkId(),
                        courseWorkName, updateDeadline, hasDeadline ? deadline.getTime() : null,
                        updateQuestion ? selectedQuestions : null, open);
            }
        });

        courseWorkNameEditor = findViewById(R.id.course_work_edit_text_edit_name);
        openSelector = findViewById(R.id.course_work_edit_radio_group_open);
        hasDdlSelector = findViewById(R.id.course_work_edit_radio_group_has_ddl);
        deadlineEditor = findViewById(R.id.course_work_edit_text_edit_deadline);
        addWorkQuestionButton = findViewById(R.id.course_work_button_add_question);
        openLayout = findViewById(R.id.open_layout);
        deadlineLayout = findViewById(R.id.deadline_layout);

        deadline = Calendar.getInstance(Locale.CHINA);

        hasDdlSelector.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.has_ddl_yes:
                    deadlineLayout.setVisibility(View.VISIBLE);
                    deadlineEditor.setEnabled(true);
                    break;
                case R.id.has_ddl_no:
                    deadlineLayout.setVisibility(View.GONE);
                    deadlineEditor.setEnabled(false);
                    break;
            }
        });

        deadlineEditor.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(CourseWorkEditActivity.this, (view, year, month, dayOfMonth) -> {
                deadline.set(Calendar.YEAR, year);
                deadline.set(Calendar.MONTH, month);
                deadline.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                deadlineEditor.setText(IUtils.DATE_FORMATTER.format(deadline.getTime()));
            }, deadline.get(Calendar.YEAR), deadline.get(Calendar.MONTH), deadline.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        addWorkQuestionButton.setOnClickListener(v -> {
            Intent intent = new Intent(CourseWorkEditActivity.this, QuestionSelectActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SELECT_QUESTION);
        });

        allQuestionInfo = new HashMap<>();

        recyclerView = findViewById(R.id.question_list);
        questionInfos = new ArrayList<>();
        recyclerViewAdapter = new QuestionRecyclerViewAdapter(this, questionInfos, this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addItemDecoration(new MyRecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL,
                1,ContextCompat.getColor(this,R.color.colorBlack)));

        QuestionService.getInstance().findAllQuestion(handler, true, null, null);

        if (isCreate) {
            openSelector.check(R.id.open_no);
            UIHelper.disableRadioGroup(openSelector);
            openLayout.setVisibility(View.GONE);
        } else {
            courseWorkNameEditor.setText(courseWorkInfo.getCourseWorkName());
            openSelector.check(courseWorkInfo.getOpen() ? R.id.open_yes : R.id.open_no);
            if (courseWorkInfo.getHasDeadline()) {
                hasDdlSelector.check(R.id.has_ddl_yes);
                deadlineLayout.setVisibility(View.VISIBLE);
                deadline.setTimeInMillis(courseWorkInfo.getDeadline());
                deadlineEditor.setText(IUtils.DATE_FORMATTER.format(deadline.getTime()));
            } else {
                hasDdlSelector.check(R.id.has_ddl_no);
                deadlineLayout.setVisibility(View.GONE);
                deadlineEditor.setText("");
            }
            if (courseWorkInfo.getOpen()) {
                UIHelper.disableRadioGroup(openSelector);
                addWorkQuestionButton.setEnabled(false);
                addWorkQuestionButton.setVisibility(View.GONE);
            }
        }

    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == QuestionService.FIND_ALL_QUESTION_MSG) {
                handleFindAllQuestionMsg(msg);
            } else if (msg.what == CourseWorkService.CREATE_COURSE_WORK_MSG) {
                handleCreateCourseWorkMsg(msg);
            } else if (msg.what == CourseWorkService.UPDATE_COURSE_WORK_MSG) {
                handleUpdateCourseWorkMsg(msg);
            } else if (msg.what == CourseWorkService.DELETE_COURSE_WORK_MSG) {
                handleDeleteCourseWorkMsg(msg);
            }
        }
    };

    private void handleFindAllQuestionMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        if (serviceResult.isSuccess()) {
            allQuestionInfo.clear();
            for (QuestionInfo questionInfo : serviceResult.extra(QuestionFindAllResponse.class).getQuestionInfoList()) {
                allQuestionInfo.put(questionInfo.getQuestionId(), questionInfo);
            }
            if (!isCreate) {
                List<CourseWorkQuestionInfo> courseWorkQuestionInfoList = new ArrayList<>(
                        courseWorkInfo.getCourseWorkQuestionInfoList());
                Collections.sort(courseWorkQuestionInfoList, ((o1, o2) -> o1.getIndex() - o2.getIndex()));
                for (CourseWorkQuestionInfo courseWorkQuestionInfo : courseWorkQuestionInfoList) {
                    if (allQuestionInfo.containsKey(courseWorkQuestionInfo.getQuestionId())) {
                        questionInfos.add(allQuestionInfo.get(courseWorkQuestionInfo.getQuestionId()));
                    }
                }
                recyclerViewAdapter.notifyDataSetChanged();
            }
        } else {
            UIHelper.toast(this, serviceResult, raw -> "加载试题信息失败");
        }
    }

    private void handleCreateCourseWorkMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        UIHelper.toast(this, serviceResult, raw -> serviceResult.isSuccess() ? "布置作业成功" : "创建作业失败");
        if (serviceResult.isSuccess()) {
            CourseWorkInfo courseWorkInfo = serviceResult.extra(CourseWorkCreateResponse.class).getCourseWorkInfo();
            Intent intent = new Intent();
            intent.putExtra(IntentParam.COURSE_WORK_INFO, courseWorkInfo);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void handleUpdateCourseWorkMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        UIHelper.toast(this, serviceResult, raw -> serviceResult.isSuccess() ? "更新作业成功" : "更新作业失败");
        if (serviceResult.isSuccess()) {
            CourseWorkInfo courseWorkInfo = serviceResult.extra(CourseWorkUpdateResponse.class).getCourseWorkInfo();
            Intent intent = new Intent();
            intent.putExtra(IntentParam.COURSE_WORK_INFO, courseWorkInfo);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void handleDeleteCourseWorkMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        UIHelper.toast(this, serviceResult, raw -> serviceResult.isSuccess() ? "删除作业成功" : "删除作业失败");
        if (serviceResult.isSuccess()) {
            Intent intent = new Intent();
            intent.putExtra(IntentParam.COURSE_WORK_INFO, courseWorkInfo);
            intent.putExtra(IntentParam.DELETE_COURSE_WORK, true);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE_SELECT_QUESTION && data != null) {
            QuestionInfo questionInfo = (QuestionInfo) data.getSerializableExtra(IntentParam.QUESTION_INFO);
            for (QuestionInfo info : questionInfos) {
                if (info.getQuestionId() == questionInfo.getQuestionId()) {
                    UIHelper.toast(this, "不能选择相同的试题");
                    return;
                }
            }
            questionInfos.add(questionInfo);
            allQuestionInfo.put(questionInfo.getQuestionId(), questionInfo);
            recyclerViewAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onItemLongClick(ViewHolder viewHolder) {
        if (courseWorkInfo.getOpen()) {
            return false;
        }
        new QMUIDialog.MessageDialogBuilder(this)
                .setTitle("删除试题")
                .setMessage("确定删除该试题？")
                .addAction("取消", (dialog, index) -> dialog.dismiss())
                .addAction(0, "删除", QMUIDialogAction.ACTION_PROP_NEGATIVE, (dialog, index) -> {
                    int needRemoveIndex = -1;
                    for (int i = 0; i < questionInfos.size(); i++) {
                        QuestionInfo questionInfo = questionInfos.get(i);
                        if (questionInfo.getQuestionId() == viewHolder.getQuestionInfo().getQuestionId()) {
                            needRemoveIndex = i;
                        }
                    }
                    if (needRemoveIndex >= 0) {
                        questionInfos.remove(needRemoveIndex);
                        recyclerViewAdapter.notifyDataSetChanged();
                    }
                    dialog.dismiss();
                })
                .show();
        return true;
    }
}
