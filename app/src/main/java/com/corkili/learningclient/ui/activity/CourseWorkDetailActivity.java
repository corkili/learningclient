package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.android.tu.loadingdialog.LoadingDailog;
import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.ProtoUtils;
import com.corkili.learningclient.common.QuestionCheckStatus;
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkQuestionInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.EssaySubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.MultipleChoiceSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.MultipleFillingSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.MultipleFillingSubmittedAnswer.Pair;
import com.corkili.learningclient.generate.protobuf.Info.QuestionInfo;
import com.corkili.learningclient.generate.protobuf.Info.QuestionType;
import com.corkili.learningclient.generate.protobuf.Info.SingleChoiceSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.SingleFillingSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedCourseWorkInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.QuestionGetResponse;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedCourseWorkCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedCourseWorkGetResponse;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedCourseWorkUpdateResponse;
import com.corkili.learningclient.service.QuestionService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.service.SubmittedCourseWorkService;
import com.corkili.learningclient.ui.adapter.SubmittedQuestionRecyclerViewAdapter;
import com.corkili.learningclient.ui.adapter.SubmittedQuestionRecyclerViewAdapter.CheckView;
import com.corkili.learningclient.ui.adapter.SubmittedQuestionRecyclerViewAdapter.ChoiceView;
import com.corkili.learningclient.ui.adapter.SubmittedQuestionRecyclerViewAdapter.FillingView;
import com.corkili.learningclient.ui.adapter.SubmittedQuestionRecyclerViewAdapter.ViewHolder;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.qmuiteam.qmui.widget.grouplist.QMUICommonListItemView;
import com.qmuiteam.qmui.widget.grouplist.QMUIGroupListView;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class CourseWorkDetailActivity extends AppCompatActivity implements
        SubmittedQuestionRecyclerViewAdapter.OnItemInteractionListener,
        SubmittedQuestionRecyclerViewAdapter.SubmitDataBus {

    private QMUITopBarLayout topBar;
    private QMUIGroupListView infoListView;
    private QMUICommonListItemView checkResultItemView;

    private RecyclerView recyclerView;
    private SubmittedQuestionRecyclerViewAdapter recyclerViewAdapter;

    private Button submitButton;
    private Button saveButton;

    private List<QuestionInfo> questionInfos;

    private UserInfo userInfo;
    private CourseWorkInfo courseWorkInfo;
    private long submittedCourseWorkId;

    private LoadingDailog waitingDialog;
    private AtomicInteger counter;
    private boolean isSystemSubmit;

    private SubmittedCourseWorkInfo submittedCourseWorkInfo;
    private Map<Integer, CourseWorkSubmittedAnswer> submittedAnswerMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_work_detail);

        userInfo = (UserInfo) getIntent().getSerializableExtra(IntentParam.USER_INFO);
        courseWorkInfo = (CourseWorkInfo) getIntent().getSerializableExtra(IntentParam.COURSE_WORK_INFO);
        submittedCourseWorkId = getIntent().getLongExtra(IntentParam.SUBMITTED_COURSE_WORK_ID, -1);

        if (userInfo == null || courseWorkInfo == null) {
            throw new RuntimeException("Intent param expected");
        }

        topBar = findViewById(R.id.topbar);

        topBar.setTitle("作业详情");

        topBar.addLeftBackImageButton().setOnClickListener(v -> {
            if (userInfo.getUserType() == UserType.Teacher) {
                Intent intent = new Intent();
                intent.putExtra(IntentParam.SUBMITTED_COURSE_WORK_INFO, submittedCourseWorkInfo);
                setResult(RESULT_OK, intent);
            }
            finish();
        });

        submitButton = topBar.addRightTextButton("提交", R.id.topbar_right_submit);
        saveButton = topBar.addRightTextButton("暂存", R.id.topbar_right_save);

        waitingDialog = new LoadingDailog.Builder(this)
                .setMessage("请稍后...")
                .setCancelable(false)
                .setCancelOutside(false)
                .create();

        waitingDialog.show();
        counter = new AtomicInteger(0);
        isSystemSubmit = false;

        infoListView = findViewById(R.id.course_work_info_list);

        QMUICommonListItemView courseWorkNameItemView = infoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_coursework_24dp),
                "作业名称",
                courseWorkInfo.getCourseWorkName(),
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        String submitMsg;
        if (courseWorkInfo.getOpen()) {
            if (courseWorkInfo.getHasDeadline() && courseWorkInfo.getDeadline() <= System.currentTimeMillis()) {
                submitMsg = "已关闭提交";
            } else {
                submitMsg = "已开放提交";
            }
        } else {
            submitMsg = "未开放提交";
        }

        QMUICommonListItemView submitItemView = infoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_state_24dp),
                "提交状态",
                submitMsg,
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        QMUICommonListItemView deadlineItemView = infoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_timer_24dp),
                "截止日期",
                courseWorkInfo.getHasDeadline()
                        ? IUtils.DATE_TIME_FORMATTER.format(new Date(courseWorkInfo.getDeadline()))
                        : "无限期",
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        checkResultItemView = infoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_check_result_24dp),
                "批改状态",
                "",
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        int size = QMUIDisplayHelper.dp2px(this, 24);

        QMUIGroupListView.newSection(this)
                .setTitle("作业基本信息")
                .setLeftIconSize(size, ViewGroup.LayoutParams.WRAP_CONTENT)
                .addItemView(courseWorkNameItemView, null)
                .addItemView(submitItemView, null)
                .addItemView(deadlineItemView, null)
                .addItemView(checkResultItemView, null)
                .addTo(infoListView);

        QMUIGroupListView.newSection(this)
                .setTitle("作业题目详情")
                .setLeftIconSize(size, ViewGroup.LayoutParams.WRAP_CONTENT)
                .addTo(infoListView);

        recyclerView = findViewById(R.id.question_list);

        questionInfos = new ArrayList<>();
        recyclerViewAdapter = new SubmittedQuestionRecyclerViewAdapter(this, questionInfos, this, userInfo);
        recyclerViewAdapter.setSubmitDataBus(this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);

        submittedAnswerMap = new HashMap<>();

        submitButton.setOnClickListener(v -> submitOrSave(true));

        saveButton.setOnClickListener(v -> submitOrSave(false));

        if (userInfo.getUserType() == UserType.Teacher) {
            saveButton.setVisibility(View.GONE);
        }

        if (userInfo.getUserType() == UserType.Student) {
            SubmittedCourseWorkService.getInstance().getSubmittedCourseWork(handler, false, 0,
                    courseWorkInfo.getCourseWorkId(), userInfo.getUserId());
        } else {
            SubmittedCourseWorkService.getInstance().getSubmittedCourseWork(handler, true,
                    submittedCourseWorkId, 0,0 );
        }

        List<Long> questionIdList = new ArrayList<>();
        for (CourseWorkQuestionInfo courseWorkQuestionInfo : courseWorkInfo.getCourseWorkQuestionInfoList()) {
            questionIdList.add(courseWorkQuestionInfo.getQuestionId());
        }
        QuestionService.getInstance().getQuestion(handler, questionIdList,false);

    }

    private void submitOrSave(boolean finished) {
        submitButton.setEnabled(false);
        saveButton.setEnabled(false);
        if (submittedAnswerMap == null || submittedAnswerMap.size() != courseWorkInfo.getCourseWorkQuestionInfoCount()) {
            submittedAnswerMap = ProtoUtils.generateSubmittedCourseWorkAnswerMap(courseWorkInfo,
                    questionInfos, submittedCourseWorkInfo != null ? submittedCourseWorkInfo.getSubmittedAnswerMap() : null);
        }
        List<String> notDoQuestionIndexList = new ArrayList<>();
        if (userInfo.getUserType() == UserType.Student) {
            for (CourseWorkQuestionInfo courseWorkQuestionInfo : courseWorkInfo.getCourseWorkQuestionInfoList()) {
                ViewHolder viewHolder = recyclerViewAdapter.getViewHolder(courseWorkQuestionInfo.getQuestionId());
                CourseWorkSubmittedAnswer courseWorkSubmittedAnswer = submittedAnswerMap.get(courseWorkQuestionInfo.getIndex());
                if (viewHolder != null && courseWorkSubmittedAnswer != null) {
                    QuestionInfo questionInfo = viewHolder.getQuestionInfo();
                    if (questionInfo.getQuestionType() == QuestionType.SingleChoice) {
                        int choice = -1;
                        for (Entry<Integer, ChoiceView> entry : viewHolder.getChoiceViewMap().entrySet()) {
                            if (entry.getValue().getCheckBox().isChecked()) {
                                choice = entry.getKey();
                                break;
                            }
                        }
                        if (choice < 0) {
                            notDoQuestionIndexList.add(String.valueOf(courseWorkQuestionInfo.getIndex()));
                        }
                        SingleChoiceSubmittedAnswer singleChoiceSubmittedAnswer =
                                SingleChoiceSubmittedAnswer.newBuilder().setChoice(choice).build();
                        courseWorkSubmittedAnswer = courseWorkSubmittedAnswer.toBuilder()
                                .setSubmittedAnswer(courseWorkSubmittedAnswer.getSubmittedAnswer().toBuilder()
                                        .setSingleChoiceSubmittedAnswer(singleChoiceSubmittedAnswer)
                                        .build())
                                .build();
                        submittedAnswerMap.put(courseWorkQuestionInfo.getIndex(), courseWorkSubmittedAnswer);
                    } else if (questionInfo.getQuestionType() == QuestionType.MultipleChoice) {
                        List<Integer> choiceList = new ArrayList<>();
                        for (Entry<Integer, ChoiceView> entry : viewHolder.getChoiceViewMap().entrySet()) {
                            if (entry.getValue().getCheckBox().isChecked()) {
                                choiceList.add(entry.getKey());
                            }
                        }
                        if (choiceList.isEmpty()) {
                            notDoQuestionIndexList.add(String.valueOf(courseWorkQuestionInfo.getIndex()));
                        }
                        MultipleChoiceSubmittedAnswer multipleChoiceSubmittedAnswer =
                                MultipleChoiceSubmittedAnswer.newBuilder().addAllChoice(choiceList).build();
                        courseWorkSubmittedAnswer = courseWorkSubmittedAnswer.toBuilder()
                                .setSubmittedAnswer(courseWorkSubmittedAnswer.getSubmittedAnswer().toBuilder()
                                        .setMultipleChoiceSubmittedAnswer(multipleChoiceSubmittedAnswer)
                                        .build())
                                .build();
                        submittedAnswerMap.put(courseWorkQuestionInfo.getIndex(), courseWorkSubmittedAnswer);
                    } else if (questionInfo.getQuestionType() == QuestionType.SingleFilling) {
                        String filling = "";
                        Iterator<Entry<Integer, FillingView>> iterator = viewHolder.getFillingViewMap().entrySet().iterator();
                        if (iterator.hasNext()) {
                            Entry<Integer, FillingView> entry = iterator.next();
                            filling = entry.getValue().getFillingEditor().getText().toString().trim();
                        }
                        if (StringUtils.isBlank(filling)) {
                            notDoQuestionIndexList.add(String.valueOf(courseWorkQuestionInfo.getIndex()));
                        }
                        SingleFillingSubmittedAnswer singleFillingSubmittedAnswer
                                = SingleFillingSubmittedAnswer.newBuilder().setAnswer(filling).build();
                        courseWorkSubmittedAnswer = courseWorkSubmittedAnswer.toBuilder()
                                .setSubmittedAnswer(courseWorkSubmittedAnswer.getSubmittedAnswer().toBuilder()
                                        .setSingleFillingSubmittedAnswer(singleFillingSubmittedAnswer)
                                        .build())
                                .build();
                        submittedAnswerMap.put(courseWorkQuestionInfo.getIndex(), courseWorkSubmittedAnswer);
                    } else if (questionInfo.getQuestionType() == QuestionType.MultipleFilling) {
                        Map<Integer, String> ansMap = new HashMap<>();
                        for (Entry<Integer, FillingView> entry : viewHolder.getFillingViewMap().entrySet()) {
                            ansMap.put(entry.getKey(), entry.getValue().getFillingEditor().getText().toString().trim());
                        }
                        boolean finish = ansMap.size() == questionInfo.getAnswer().getMultipleFillingAnswer().getAnswerCount();
                        if (finish) {
                            for (String ans : ansMap.values()) {
                                if (StringUtils.isBlank(ans)) {
                                    finish = false;
                                    break;
                                }
                            }
                        }
                        if (!finish) {
                            notDoQuestionIndexList.add(String.valueOf(courseWorkQuestionInfo.getIndex()));
                        }
                        MultipleFillingSubmittedAnswer rawMultipleFillingSubmittedAnswer =
                                courseWorkSubmittedAnswer.getSubmittedAnswer().getMultipleFillingSubmittedAnswer();
                        Map<Integer, Pair> pairMap = new HashMap<>();
                        for (Entry<Integer, Pair> pairEntry : rawMultipleFillingSubmittedAnswer.getAnswerMap().entrySet()) {
                            pairMap.put(pairEntry.getKey(), pairEntry.getValue().toBuilder()
                                    .setAnswer(ansMap.get(pairEntry.getKey()))
                                    .build());
                        }
                        MultipleFillingSubmittedAnswer multipleFillingSubmittedAnswer =
                                MultipleFillingSubmittedAnswer.newBuilder().putAllAnswer(pairMap).build();
                        courseWorkSubmittedAnswer = courseWorkSubmittedAnswer.toBuilder()
                                .setSubmittedAnswer(courseWorkSubmittedAnswer.getSubmittedAnswer().toBuilder()
                                        .setMultipleFillingSubmittedAnswer(multipleFillingSubmittedAnswer)
                                        .build())
                                .build();
                        submittedAnswerMap.put(courseWorkQuestionInfo.getIndex(), courseWorkSubmittedAnswer);
                    } else if (questionInfo.getQuestionType() == QuestionType.Essay) {
                        String text = viewHolder.getEssayAnswerEditor().getText().toString().trim();
                        if (StringUtils.isBlank(text)) {
                            notDoQuestionIndexList.add(String.valueOf(courseWorkQuestionInfo.getIndex()));
                        }
                        EssaySubmittedAnswer essaySubmittedAnswer =
                                EssaySubmittedAnswer.newBuilder().setText(text).build();
                        courseWorkSubmittedAnswer = courseWorkSubmittedAnswer.toBuilder()
                                .setSubmittedAnswer(courseWorkSubmittedAnswer.getSubmittedAnswer().toBuilder()
                                        .setEssaySubmittedAnswer(essaySubmittedAnswer)
                                        .build())
                                .build();
                        submittedAnswerMap.put(courseWorkQuestionInfo.getIndex(), courseWorkSubmittedAnswer);
                    }
                }
            }
        } else { // 教师端
            for (CourseWorkQuestionInfo courseWorkQuestionInfo : courseWorkInfo.getCourseWorkQuestionInfoList()) {
                ViewHolder viewHolder = recyclerViewAdapter.getViewHolder(courseWorkQuestionInfo.getQuestionId());
                CourseWorkSubmittedAnswer courseWorkSubmittedAnswer = submittedAnswerMap.get(courseWorkQuestionInfo.getIndex());
                if (viewHolder != null && courseWorkSubmittedAnswer != null
                        && !viewHolder.getQuestionInfo().getAutoCheck()
                        && courseWorkSubmittedAnswer.getCheckStatus() < 0) {
                    QuestionInfo questionInfo = viewHolder.getQuestionInfo();
                    if (questionInfo.getQuestionType() == QuestionType.SingleFilling
                            || questionInfo.getQuestionType() == QuestionType.Essay) {
                        if (!viewHolder.getCheckViewMap().isEmpty()) {
                            CheckView checkView = viewHolder.getCheckViewMap().values().iterator().next();
                            int pickedCheckStatus = checkView.getScorePicker().getValue();
                            if (pickedCheckStatus < 0) {
                                pickedCheckStatus = 0;
                            } else if (pickedCheckStatus > 1) {
                                pickedCheckStatus = 1;
                            }
                            courseWorkSubmittedAnswer = courseWorkSubmittedAnswer.toBuilder()
                                    .setCheckStatus(pickedCheckStatus)
                                    .build();
                            submittedAnswerMap.put(courseWorkQuestionInfo.getIndex(), courseWorkSubmittedAnswer);
                        }
                    } else if (questionInfo.getQuestionType() == QuestionType.MultipleFilling) {
                        Map<Integer, Integer> checkStatusMap = new HashMap<>();
                        for (Entry<Integer, CheckView> entry : viewHolder.getCheckViewMap().entrySet()) {
                            int picked = entry.getValue().getScorePicker().getValue();
                            if (picked < 0) {
                                picked = 0;
                            } else if (picked > 1) {
                                picked = 1;
                            }
                            checkStatusMap.put(entry.getKey(), picked);
                        }
                        MultipleFillingSubmittedAnswer rawMultipleFillingSubmittedAnswer =
                                courseWorkSubmittedAnswer.getSubmittedAnswer().getMultipleFillingSubmittedAnswer();
                        Map<Integer, Pair> pairMap = new HashMap<>();
                        for (Entry<Integer, Pair> pairEntry : rawMultipleFillingSubmittedAnswer.getAnswerMap().entrySet()) {
                            Integer checkStatus = checkStatusMap.get(pairEntry.getKey());
                            pairMap.put(pairEntry.getKey(), pairEntry.getValue().toBuilder()
                                    .setScoreOrCheckStatus(checkStatus == null ? QuestionCheckStatus.NOT_CHECK : checkStatus)
                                    .build());
                        }
                        int sumCheckStatus = QuestionCheckStatus.CHECK_TRUE;
                        for (Pair pair : pairMap.values()) {
                            if ((int) pair.getScoreOrCheckStatus() == QuestionCheckStatus.CHECK_FALSE) {
                                sumCheckStatus = QuestionCheckStatus.CHECK_FALSE;
                                break;
                            } else if ((int) pair.getScoreOrCheckStatus() == QuestionCheckStatus.NOT_CHECK) {
                                sumCheckStatus = QuestionCheckStatus.NOT_CHECK;
                                break;
                            }
                        }
                        MultipleFillingSubmittedAnswer multipleFillingSubmittedAnswer =
                                MultipleFillingSubmittedAnswer.newBuilder().putAllAnswer(pairMap).build();
                        courseWorkSubmittedAnswer = courseWorkSubmittedAnswer.toBuilder()
                                .setSubmittedAnswer(courseWorkSubmittedAnswer.getSubmittedAnswer().toBuilder()
                                        .setMultipleFillingSubmittedAnswer(multipleFillingSubmittedAnswer)
                                        .build())
                                .setCheckStatus(sumCheckStatus)
                                .build();
                        submittedAnswerMap.put(courseWorkQuestionInfo.getIndex(), courseWorkSubmittedAnswer);
                    }
                }
            }
        }

        if (finished && !isSystemSubmit) {
            if (userInfo.getUserType() == UserType.Student) {
                String message;
                if (notDoQuestionIndexList.isEmpty()) {
                    message = "你已完成所有题目，确认保存（保存后不可修改）？";
                } else {
                    message = IUtils.format("你有{}个题目（{}）尚未完成，确认保存（保存后不可修改）？",
                            notDoQuestionIndexList.size(), IUtils.list2String(notDoQuestionIndexList, ", "));
                }
                new QMUIDialog.MessageDialogBuilder(this)
                        .setTitle("确认提交")
                        .setMessage(message)
                        .addAction("取消", (dialog, index) -> {
                            dialog.cancel();
                            dialog.dismiss();
                            saveButton.setEnabled(true);
                            submitButton.setEnabled(true);
                        })
                        .addAction(0, "提交", QMUIDialogAction.ACTION_PROP_NEGATIVE, (dialog, index) -> {
                            if (alreadySubmitted()) {
                                if (!submittedCourseWorkInfo.getFinished()) {
                                    SubmittedCourseWorkService.getInstance().updateSubmittedCourseWork(handler,
                                            submittedCourseWorkInfo.getSubmittedCourseWorkId(),
                                            !submittedAnswerMap.equals(submittedCourseWorkInfo.getSubmittedAnswerMap()),
                                            submittedAnswerMap, true, true);
                                } else {
                                    Toast.makeText(this, "无法修改", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Map<Integer, SubmittedAnswer> map = new HashMap<>();
                                for (Entry<Integer, CourseWorkSubmittedAnswer> entry : submittedAnswerMap.entrySet()) {
                                    map.put(entry.getKey(), entry.getValue().getSubmittedAnswer());
                                }
                                SubmittedCourseWorkService.getInstance().createSubmittedCourseWork(handler,
                                        courseWorkInfo.getCourseWorkId(), true, map);
                            }
                            dialog.dismiss();
                        })
                        .show();
            } else {
                if (alreadySubmitted()) {
                    SubmittedCourseWorkService.getInstance().updateSubmittedCourseWork(handler,
                            submittedCourseWorkInfo.getSubmittedCourseWorkId(),
                            !submittedAnswerMap.equals(submittedCourseWorkInfo.getSubmittedAnswerMap()),
                            submittedAnswerMap, true, true);
                } else {
                    saveButton.setEnabled(true);
                    submitButton.setEnabled(true);
                }
            }
        } else {
            if (alreadySubmitted()) {
                if (!submittedCourseWorkInfo.getFinished()) {
                    SubmittedCourseWorkService.getInstance().updateSubmittedCourseWork(handler,
                            submittedCourseWorkInfo.getSubmittedCourseWorkId(),
                            !submittedAnswerMap.equals(submittedCourseWorkInfo.getSubmittedAnswerMap()),
                            submittedAnswerMap, isSystemSubmit, isSystemSubmit);
                } else {
                    Toast.makeText(this, "无法修改", Toast.LENGTH_SHORT).show();
                }
            } else {
                Map<Integer, SubmittedAnswer> map = new HashMap<>();
                for (Entry<Integer, CourseWorkSubmittedAnswer> entry : submittedAnswerMap.entrySet()) {
                    map.put(entry.getKey(), entry.getValue().getSubmittedAnswer());
                }
                SubmittedCourseWorkService.getInstance().createSubmittedCourseWork(handler,
                        courseWorkInfo.getCourseWorkId(), isSystemSubmit, map);
            }
        }
        isSystemSubmit = false;
    }

    private boolean allIsAutoCheck() {
        for (QuestionInfo questionInfo : questionInfos) {
            if (!questionInfo.getAutoCheck()) {
                return false;
            }
        }
        return true;
    }

    private void refresh() {
        if (userInfo.getUserType() == UserType.Teacher && alreadySubmitted()) {
            topBar.setTitle(submittedCourseWorkInfo.getSubmitterInfo().getUsername());
        }

        // submittedAnswerMap
        if (alreadySubmitted()) {
            submittedAnswerMap = ProtoUtils.generateSubmittedCourseWorkAnswerMap(courseWorkInfo,
                    questionInfos, submittedCourseWorkInfo.getSubmittedAnswerMap());
        } else {
            submittedAnswerMap = ProtoUtils.generateSubmittedCourseWorkAnswerMap(courseWorkInfo,
                    questionInfos, null);
        }

        // check result
        if (alreadySubmitted()) {
            if (submittedCourseWorkInfo.getAlreadyCheckAllAnswer()) {
                StringBuilder sb = new StringBuilder();
                sb.append("[已全部批改] 正确率：");
                int total = courseWorkInfo.getCourseWorkQuestionInfoCount();
                double count = 0;
                for (CourseWorkSubmittedAnswer courseWorkSubmittedAnswer : submittedCourseWorkInfo.getSubmittedAnswerMap().values()) {
                    if (courseWorkSubmittedAnswer.getCheckStatus() == QuestionCheckStatus.CHECK_TRUE) {
                        count += 1;
                    } else if (courseWorkSubmittedAnswer.getCheckStatus() == QuestionCheckStatus.CHECK_HALF_TRUE) {
                        count += 0.5;
                    }
                }
                sb.append(count).append("/").append(total);
                checkResultItemView.setDetailText(sb.toString().trim());
            } else {
                checkResultItemView.setDetailText("[尚未全部批改]");
            }
        } else {
            checkResultItemView.setDetailText("[尚未全部批改]");
        }
        if (userInfo.getUserType() == UserType.Teacher) {
            checkResultItemView.setVisibility(View.VISIBLE);
        } else {
            if (canSubmitAnswer()) {
                if (alreadySubmitted()) {
                    if (submittedCourseWorkInfo.getFinished()) {
                        checkResultItemView.setVisibility(View.VISIBLE);
                    } else {
                        checkResultItemView.setVisibility(View.GONE);
                    }
                } else {
                    checkResultItemView.setVisibility(View.GONE);
                }
            } else {
                checkResultItemView.setVisibility(View.VISIBLE);
            }
        }

        // button
        if (userInfo.getUserType() == UserType.Teacher) {
            saveButton.setVisibility(View.GONE);
            if (allIsAutoCheck()) {
                submitButton.setVisibility(View.GONE);
            } else {
                if (alreadySubmitted()) {
                    if (submittedCourseWorkInfo.getFinished()) {
                        submitButton.setVisibility(View.VISIBLE);
                    } else {
                        submitButton.setVisibility(View.GONE);
                    }
                } else {
                    submitButton.setVisibility(View.GONE);
                }
            }
        } else {
            if (canSubmitAnswer()) {
                if (alreadySubmitted()) {
                    if (submittedCourseWorkInfo.getFinished()) {
                        submitButton.setVisibility(View.GONE);
                        saveButton.setVisibility(View.GONE);
                    } else {
                        submitButton.setVisibility(View.VISIBLE);
                        saveButton.setVisibility(View.VISIBLE);
                    }
                } else {
                    submitButton.setVisibility(View.VISIBLE);
                    saveButton.setVisibility(View.VISIBLE);
                }
            } else{
                submitButton.setVisibility(View.GONE);
                saveButton.setVisibility(View.GONE);
            }
        }

        recyclerViewAdapter.notifyDataSetChanged();
    }

    private boolean alreadySubmitted() {
        return submittedCourseWorkInfo != null && submittedCourseWorkInfo.getSubmittedCourseWorkId() > 0;
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SubmittedCourseWorkService.GET_SUBMITTED_COURSE_WORK_MSG) {
                handleGetSubmittedCourseWorkMsg(msg);
            } else if (msg.what == SubmittedCourseWorkService.CREATE_SUBMITTED_COURSE_WORK_MSG) {
                handleCreateSubmittedCourseWorkMsg(msg);
            } else if (msg.what == SubmittedCourseWorkService.UPDATE_SUBMITTED_COURSE_WORK_MSG) {
                handleUpdateSubmittedCourseWorkMsg(msg);
            } else if (msg.what == QuestionService.GET_QUESTION_MSG) {
                handleGetQuestionMsg(msg);
            }
        }
    };

    private void handleGetSubmittedCourseWorkMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            submittedCourseWorkInfo = serviceResult.extra(SubmittedCourseWorkGetResponse.class).getSubmittedCourseWorkInfo();
            finishInit();
        } else {
            if (serviceResult.extra(Boolean.class)) {
                setResult(RESULT_CANCELED);
                CourseWorkDetailActivity.this.finish();
            } else {
                if (userInfo.getUserType() == UserType.Teacher) {
                    setResult(RESULT_CANCELED);
                    CourseWorkDetailActivity.this.finish();
                } else {
                    submittedCourseWorkInfo = null;
                    finishInit();
                }
            }
        }
    }

    private void handleCreateSubmittedCourseWorkMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        submitButton.setEnabled(true);
        saveButton.setEnabled(true);
        if (serviceResult.isSuccess()) {
            submittedCourseWorkInfo = serviceResult.extra(SubmittedCourseWorkCreateResponse.class).getSubmittedCourseWorkInfo();
            refresh();
        }
    }

    private void handleUpdateSubmittedCourseWorkMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        submitButton.setEnabled(true);
        saveButton.setEnabled(true);
        if (serviceResult.isSuccess()) {
            submittedCourseWorkInfo = serviceResult.extra(SubmittedCourseWorkUpdateResponse.class).getSubmittedCourseWorkInfo();
            refresh();
        }
    }

    private void handleGetQuestionMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess() || courseWorkInfo.getCourseWorkQuestionInfoCount() <= 0) {
            questionInfos.clear();
            questionInfos.addAll(serviceResult.extra(QuestionGetResponse.class).getQuestionInfoList());
            if (questionInfos.size() != courseWorkInfo.getCourseWorkQuestionInfoCount()) {
                Toast.makeText(this, "加载失败", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                CourseWorkDetailActivity.this.finish();
            } else {
                finishInit();
            }
        } else {
            Toast.makeText(this, "加载失败", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            CourseWorkDetailActivity.this.finish();
        }
    }

    private void finishInit() {
        if (counter.incrementAndGet() == 2) {
            if (waitingDialog != null) {
                waitingDialog.dismiss();
            }
            refresh();
            if (userInfo.getUserType() == UserType.Student) {
                if (!canSubmitAnswer()) {
                    if (!alreadySubmitted() || !submittedCourseWorkInfo.getFinished()) {
                        isSystemSubmit = true;
                        submitButton.performClick();
                    }
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (userInfo.getUserType() == UserType.Teacher) {
            Intent intent = new Intent();
            intent.putExtra(IntentParam.SUBMITTED_COURSE_WORK_INFO, submittedCourseWorkInfo);
            setResult(RESULT_OK, intent);
        }
        super.onBackPressed();
    }

    @Override
    public boolean isFinished() {
        if (alreadySubmitted()) {
            return submittedCourseWorkInfo.getFinished();
        } else {
            return false;
        }
    }

    @Override
    public boolean canSubmitAnswer() {
        if (isFinished()) {
            return false;
        }
        if (courseWorkInfo.getOpen()) {
            if (courseWorkInfo.getHasDeadline()) {
                return System.currentTimeMillis() <= courseWorkInfo.getDeadline();
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public SubmittedAnswer requireSubmittedAnswerFor(long questionId) {
        for (CourseWorkQuestionInfo courseWorkQuestionInfo : courseWorkInfo.getCourseWorkQuestionInfoList()) {
            if (courseWorkQuestionInfo.getQuestionId() == questionId) {
                CourseWorkSubmittedAnswer courseWorkSubmittedAnswer = submittedAnswerMap.get(courseWorkQuestionInfo.getIndex());
                if (courseWorkSubmittedAnswer != null) {
                    return courseWorkSubmittedAnswer.getSubmittedAnswer();
                }
                break;
            }
        }
        return null;
    }

    @Override
    public double requireCheckStatusOrScoreFor(long questionId) {
        for (CourseWorkQuestionInfo courseWorkQuestionInfo : courseWorkInfo.getCourseWorkQuestionInfoList()) {
            if (courseWorkQuestionInfo.getQuestionId() == questionId) {
                CourseWorkSubmittedAnswer courseWorkSubmittedAnswer = submittedAnswerMap.get(courseWorkQuestionInfo.getIndex());
                if (courseWorkSubmittedAnswer != null) {
                    return courseWorkSubmittedAnswer.getCheckStatus();
                }
                break;
            }
        }
        return -1;
    }
}
