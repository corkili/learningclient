package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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
import com.corkili.learningclient.generate.protobuf.Info.EssaySubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.ExamInfo;
import com.corkili.learningclient.generate.protobuf.Info.ExamQuestionInfo;
import com.corkili.learningclient.generate.protobuf.Info.ExamSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.MultipleChoiceSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.MultipleFillingSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.MultipleFillingSubmittedAnswer.Pair;
import com.corkili.learningclient.generate.protobuf.Info.QuestionInfo;
import com.corkili.learningclient.generate.protobuf.Info.QuestionType;
import com.corkili.learningclient.generate.protobuf.Info.Score;
import com.corkili.learningclient.generate.protobuf.Info.SingleChoiceSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.SingleFillingSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedExamInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;
import com.corkili.learningclient.generate.protobuf.Response.QuestionGetResponse;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedExamCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedExamGetResponse;
import com.corkili.learningclient.generate.protobuf.Response.SubmittedExamUpdateResponse;
import com.corkili.learningclient.service.QuestionService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.service.SubmittedExamService;
import com.corkili.learningclient.ui.adapter.SubmittedQuestionRecyclerViewAdapter;
import com.corkili.learningclient.ui.adapter.SubmittedQuestionRecyclerViewAdapter.CheckView;
import com.corkili.learningclient.ui.adapter.SubmittedQuestionRecyclerViewAdapter.ChoiceView;
import com.corkili.learningclient.ui.adapter.SubmittedQuestionRecyclerViewAdapter.FillingView;
import com.corkili.learningclient.ui.adapter.SubmittedQuestionRecyclerViewAdapter.ViewHolder;
import com.corkili.learningclient.ui.other.MyRecyclerViewDivider;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
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

public class ExamDetailActivity extends AppCompatActivity implements
        SubmittedQuestionRecyclerViewAdapter.OnItemInteractionListener,
        SubmittedQuestionRecyclerViewAdapter.ScoreDataBus,
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
    private ExamInfo examInfo;
    private long submittedExamId;

    private LoadingDailog waitingDialog;
    private AtomicInteger counter;
    private boolean isSystemSubmit;

    private SubmittedExamInfo submittedExamInfo;
    private Map<Integer, ExamSubmittedAnswer> submittedAnswerMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_detail);

        userInfo = (UserInfo) getIntent().getSerializableExtra(IntentParam.USER_INFO);
        examInfo = (ExamInfo) getIntent().getSerializableExtra(IntentParam.EXAM_INFO);
        submittedExamId = getIntent().getLongExtra(IntentParam.SUBMITTED_EXAM_ID, -1);

        if (userInfo == null || examInfo == null) {
            throw new RuntimeException("Intent param expected");
        }

        topBar = findViewById(R.id.topbar);

        topBar.setTitle("考试详情");

        topBar.addLeftBackImageButton().setOnClickListener(v -> {
            if (userInfo.getUserType() == UserType.Teacher) {
                Intent intent = new Intent();
                intent.putExtra(IntentParam.SUBMITTED_COURSE_WORK_INFO, submittedExamInfo);
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

        infoListView = findViewById(R.id.exam_info_list);

        QMUICommonListItemView examNameItemView = infoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_coursework_24dp),
                "考试名称",
                examInfo.getExamName(),
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        String submitMsg;
        if (examInfo.getStartTime() <= System.currentTimeMillis()) {
            if (examInfo.getEndTime() <= System.currentTimeMillis()) {
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

        QMUICommonListItemView startTimeItemView = infoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_timer_24dp),
                "开始时间",
                IUtils.DATE_TIME_FORMATTER.format(new Date(examInfo.getStartTime())),
                QMUICommonListItemView.HORIZONTAL,
                QMUICommonListItemView.ACCESSORY_TYPE_NONE);

        QMUICommonListItemView endTimeItemView = infoListView.createItemView(
                ContextCompat.getDrawable(this, R.drawable.ic_timer_24dp),
                "结束时间",
                IUtils.DATE_TIME_FORMATTER.format(new Date(examInfo.getEndTime())),
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
                .setTitle("考试基本信息")
                .setLeftIconSize(size, ViewGroup.LayoutParams.WRAP_CONTENT)
                .addItemView(examNameItemView, null)
                .addItemView(submitItemView, null)
                .addItemView(startTimeItemView, null)
                .addItemView(endTimeItemView, null)
                .addItemView(checkResultItemView, null)
                .addTo(infoListView);

        QMUIGroupListView.newSection(this)
                .setTitle("考试题目详情")
                .setLeftIconSize(size, ViewGroup.LayoutParams.WRAP_CONTENT)
                .addTo(infoListView);

        recyclerView = findViewById(R.id.question_list);

        questionInfos = new ArrayList<>();
        recyclerViewAdapter = new SubmittedQuestionRecyclerViewAdapter(this, questionInfos, this, userInfo);
        recyclerViewAdapter.setSubmitDataBus(this);
        recyclerViewAdapter.setScoreDataBus(this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addItemDecoration(new MyRecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL,
                1,ContextCompat.getColor(this,R.color.colorBlack)));

        submittedAnswerMap = new HashMap<>();

        submitButton.setOnClickListener(v -> submitOrSave(true));

        saveButton.setOnClickListener(v -> submitOrSave(false));

        if (userInfo.getUserType() == UserType.Teacher) {
            saveButton.setVisibility(View.GONE);
        }

        if (userInfo.getUserType() == UserType.Student) {
            SubmittedExamService.getInstance().getSubmittedExam(handler, false, 0,
                    examInfo.getExamId(), userInfo.getUserId());
        } else {
            SubmittedExamService.getInstance().getSubmittedExam(handler, true,
                    submittedExamId, 0,0 );
        }

        List<Long> questionIdList = new ArrayList<>();
        for (ExamQuestionInfo examQuestionInfo : examInfo.getExamQuestionInfoList()) {
            questionIdList.add(examQuestionInfo.getQuestionId());
        }
        QuestionService.getInstance().getQuestion(handler, questionIdList,false);

    }

    private void submitOrSave(boolean finished) {
        submitButton.setEnabled(false);
        saveButton.setEnabled(false);
        if (submittedAnswerMap == null || submittedAnswerMap.size() != examInfo.getExamQuestionInfoCount()) {
            submittedAnswerMap = ProtoUtils.generateSubmittedExamAnswerMap(examInfo,
                    questionInfos, submittedExamInfo != null ? submittedExamInfo.getSubmittedAnswerMap() : null);
        }
        List<String> notDoQuestionIndexList = new ArrayList<>();
        if (userInfo.getUserType() == UserType.Student) {
            for (ExamQuestionInfo examQuestionInfo : examInfo.getExamQuestionInfoList()) {
                ViewHolder viewHolder = recyclerViewAdapter.getViewHolder(examQuestionInfo.getQuestionId());
                ExamSubmittedAnswer examSubmittedAnswer = submittedAnswerMap.get(examQuestionInfo.getIndex());
                if (viewHolder != null && examSubmittedAnswer != null) {
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
                            notDoQuestionIndexList.add(String.valueOf(examQuestionInfo.getIndex()));
                        }
                        SingleChoiceSubmittedAnswer singleChoiceSubmittedAnswer =
                                SingleChoiceSubmittedAnswer.newBuilder().setChoice(choice).build();
                        examSubmittedAnswer = examSubmittedAnswer.toBuilder()
                                .setSubmittedAnswer(examSubmittedAnswer.getSubmittedAnswer().toBuilder()
                                        .setSingleChoiceSubmittedAnswer(singleChoiceSubmittedAnswer)
                                        .build())
                                .build();
                        submittedAnswerMap.put(examQuestionInfo.getIndex(), examSubmittedAnswer);
                    } else if (questionInfo.getQuestionType() == QuestionType.MultipleChoice) {
                        List<Integer> choiceList = new ArrayList<>();
                        for (Entry<Integer, ChoiceView> entry : viewHolder.getChoiceViewMap().entrySet()) {
                            if (entry.getValue().getCheckBox().isChecked()) {
                                choiceList.add(entry.getKey());
                            }
                        }
                        if (choiceList.isEmpty()) {
                            notDoQuestionIndexList.add(String.valueOf(examQuestionInfo.getIndex()));
                        }
                        MultipleChoiceSubmittedAnswer multipleChoiceSubmittedAnswer =
                                MultipleChoiceSubmittedAnswer.newBuilder().addAllChoice(choiceList).build();
                        examSubmittedAnswer = examSubmittedAnswer.toBuilder()
                                .setSubmittedAnswer(examSubmittedAnswer.getSubmittedAnswer().toBuilder()
                                        .setMultipleChoiceSubmittedAnswer(multipleChoiceSubmittedAnswer)
                                        .build())
                                .build();
                        submittedAnswerMap.put(examQuestionInfo.getIndex(), examSubmittedAnswer);
                    } else if (questionInfo.getQuestionType() == QuestionType.SingleFilling) {
                        String filling = "";
                        Iterator<Entry<Integer, FillingView>> iterator = viewHolder.getFillingViewMap().entrySet().iterator();
                        if (iterator.hasNext()) {
                            Entry<Integer, FillingView> entry = iterator.next();
                            filling = entry.getValue().getFillingEditor().getText().toString().trim();
                        }
                        if (StringUtils.isBlank(filling)) {
                            notDoQuestionIndexList.add(String.valueOf(examQuestionInfo.getIndex()));
                        }
                        SingleFillingSubmittedAnswer singleFillingSubmittedAnswer
                                = SingleFillingSubmittedAnswer.newBuilder().setAnswer(filling).build();
                        examSubmittedAnswer = examSubmittedAnswer.toBuilder()
                                .setSubmittedAnswer(examSubmittedAnswer.getSubmittedAnswer().toBuilder()
                                        .setSingleFillingSubmittedAnswer(singleFillingSubmittedAnswer)
                                        .build())
                                .build();
                        submittedAnswerMap.put(examQuestionInfo.getIndex(), examSubmittedAnswer);
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
                            notDoQuestionIndexList.add(String.valueOf(examQuestionInfo.getIndex()));
                        }
                        MultipleFillingSubmittedAnswer rawMultipleFillingSubmittedAnswer =
                                examSubmittedAnswer.getSubmittedAnswer().getMultipleFillingSubmittedAnswer();
                        Map<Integer, Pair> pairMap = new HashMap<>();
                        for (Entry<Integer, Pair> pairEntry : rawMultipleFillingSubmittedAnswer.getAnswerMap().entrySet()) {
                            pairMap.put(pairEntry.getKey(), pairEntry.getValue().toBuilder()
                                    .setAnswer(ansMap.get(pairEntry.getKey()))
                                    .build());
                        }
                        MultipleFillingSubmittedAnswer multipleFillingSubmittedAnswer =
                                MultipleFillingSubmittedAnswer.newBuilder().putAllAnswer(pairMap).build();
                        examSubmittedAnswer = examSubmittedAnswer.toBuilder()
                                .setSubmittedAnswer(examSubmittedAnswer.getSubmittedAnswer().toBuilder()
                                        .setMultipleFillingSubmittedAnswer(multipleFillingSubmittedAnswer)
                                        .build())
                                .build();
                        submittedAnswerMap.put(examQuestionInfo.getIndex(), examSubmittedAnswer);
                    } else if (questionInfo.getQuestionType() == QuestionType.Essay) {
                        String text = viewHolder.getEssayAnswerEditor().getText().toString().trim();
                        if (StringUtils.isBlank(text)) {
                            notDoQuestionIndexList.add(String.valueOf(examQuestionInfo.getIndex()));
                        }
                        EssaySubmittedAnswer essaySubmittedAnswer =
                                EssaySubmittedAnswer.newBuilder().setText(text).build();
                        examSubmittedAnswer = examSubmittedAnswer.toBuilder()
                                .setSubmittedAnswer(examSubmittedAnswer.getSubmittedAnswer().toBuilder()
                                        .setEssaySubmittedAnswer(essaySubmittedAnswer)
                                        .build())
                                .build();
                        submittedAnswerMap.put(examQuestionInfo.getIndex(), examSubmittedAnswer);
                    }
                }
            }
        } else { // 教师端
            for (ExamQuestionInfo examQuestionInfo : examInfo.getExamQuestionInfoList()) {
                ViewHolder viewHolder = recyclerViewAdapter.getViewHolder(examQuestionInfo.getQuestionId());
                ExamSubmittedAnswer examSubmittedAnswer = submittedAnswerMap.get(examQuestionInfo.getIndex());
                if (viewHolder != null && examSubmittedAnswer != null
                        && !viewHolder.getQuestionInfo().getAutoCheck()
                        && examSubmittedAnswer.getScore() < 0) {
                    QuestionInfo questionInfo = viewHolder.getQuestionInfo();
                    if (questionInfo.getQuestionType() == QuestionType.SingleFilling
                            || questionInfo.getQuestionType() == QuestionType.Essay) {
                        if (!viewHolder.getCheckViewMap().isEmpty()) {
                            CheckView checkView = viewHolder.getCheckViewMap().values().iterator().next();
                            double pickedScore = checkView.getScorePicker().getValue();
                            if (pickedScore < 0) {
                                pickedScore = 0;
                            } else if (pickedScore > examQuestionInfo.getScore().getSingleScore()) {
                                pickedScore = examQuestionInfo.getScore().getSingleScore();
                            }
                            examSubmittedAnswer = examSubmittedAnswer.toBuilder()
                                    .setScore(pickedScore)
                                    .build();
                            submittedAnswerMap.put(examQuestionInfo.getIndex(), examSubmittedAnswer);
                        }
                    } else if (questionInfo.getQuestionType() == QuestionType.MultipleFilling) {
                        Map<Integer, Double> scoreMap = examQuestionInfo.getScore().getMultipleScore().getScoreMap();
                        Map<Integer, Double> pickedScoreMap = new HashMap<>();
                        for (Entry<Integer, CheckView> entry : viewHolder.getCheckViewMap().entrySet()) {
                            double picked = entry.getValue().getScorePicker().getValue();
                            Double max = scoreMap.get(entry.getKey());
                            if (max == null) {
                                max = 0d;
                            }
                            if (picked < 0) {
                                picked = 0;
                            } else if (picked > max) {
                                picked = max;
                            }
                            pickedScoreMap.put(entry.getKey(), picked);
                        }
                        MultipleFillingSubmittedAnswer rawMultipleFillingSubmittedAnswer =
                                examSubmittedAnswer.getSubmittedAnswer().getMultipleFillingSubmittedAnswer();
                        Map<Integer, Pair> pairMap = new HashMap<>();
                        for (Entry<Integer, Pair> pairEntry : rawMultipleFillingSubmittedAnswer.getAnswerMap().entrySet()) {
                            Double score = pickedScoreMap.get(pairEntry.getKey());
                            pairMap.put(pairEntry.getKey(), pairEntry.getValue().toBuilder()
                                    .setScoreOrCheckStatus(score == null ? -1 : score)
                                    .build());
                        }
                        double sumScore = 0;
                        for (Pair pair : pairMap.values()) {
                            if (pair.getScoreOrCheckStatus() >= 0) {
                                sumScore += pair.getScoreOrCheckStatus();
                                break;
                            }
                        }
                        MultipleFillingSubmittedAnswer multipleFillingSubmittedAnswer =
                                MultipleFillingSubmittedAnswer.newBuilder().putAllAnswer(pairMap).build();
                        examSubmittedAnswer = examSubmittedAnswer.toBuilder()
                                .setSubmittedAnswer(examSubmittedAnswer.getSubmittedAnswer().toBuilder()
                                        .setMultipleFillingSubmittedAnswer(multipleFillingSubmittedAnswer)
                                        .build())
                                .setScore(sumScore)
                                .build();
                        submittedAnswerMap.put(examQuestionInfo.getIndex(), examSubmittedAnswer);
                    }
                }
            }
        }

        if (finished && !isSystemSubmit) {
            if (userInfo.getUserType() == UserType.Student) {
                AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this);
                confirmDialog.setTitle("确认保存？");
                if (notDoQuestionIndexList.isEmpty()) {
                    confirmDialog.setMessage("你已完成所有题目，确认保存（保存后不可修改）？");
                } else {
                    confirmDialog.setMessage(IUtils.format("你有{}个题目（{}）尚未完成，确认保存（保存后不可修改）？",
                            notDoQuestionIndexList.size(), IUtils.list2String(notDoQuestionIndexList, ", ")));
                }
                confirmDialog.setPositiveButton("确认", (dialog, which) -> {
                    if (alreadySubmitted()) {
                        if (!submittedExamInfo.getFinished()) {
                            SubmittedExamService.getInstance().updateSubmittedExam(handler,
                                    submittedExamInfo.getSubmittedExamId(),
                                    !submittedAnswerMap.equals(submittedExamInfo.getSubmittedAnswerMap()),
                                    submittedAnswerMap, true, true);
                        } else {
                            Toast.makeText(this, "无法修改", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Map<Integer, SubmittedAnswer> map = new HashMap<>();
                        for (Entry<Integer, ExamSubmittedAnswer> entry : submittedAnswerMap.entrySet()) {
                            map.put(entry.getKey(), entry.getValue().getSubmittedAnswer());
                        }
                        SubmittedExamService.getInstance().createSubmittedExam(handler,
                                examInfo.getExamId(), true, map);
                    }
                });
                confirmDialog.setNegativeButton("取消", ((dialog, which) -> {
                    dialog.cancel();
                    dialog.dismiss();
                    saveButton.setEnabled(true);
                    submitButton.setEnabled(true);
                }));
                confirmDialog.show();
            } else {
                if (alreadySubmitted()) {
                    SubmittedExamService.getInstance().updateSubmittedExam(handler,
                            submittedExamInfo.getSubmittedExamId(),
                            !submittedAnswerMap.equals(submittedExamInfo.getSubmittedAnswerMap()),
                            submittedAnswerMap, true, true);
                } else {
                    saveButton.setEnabled(true);
                    submitButton.setEnabled(true);
                }
            }
        } else {
            if (alreadySubmitted()) {
                if (!submittedExamInfo.getFinished()) {
                    SubmittedExamService.getInstance().updateSubmittedExam(handler,
                            submittedExamInfo.getSubmittedExamId(),
                            !submittedAnswerMap.equals(submittedExamInfo.getSubmittedAnswerMap()),
                            submittedAnswerMap, isSystemSubmit, isSystemSubmit);
                } else {
                    Toast.makeText(this, "无法修改", Toast.LENGTH_SHORT).show();
                }
            } else {
                Map<Integer, SubmittedAnswer> map = new HashMap<>();
                for (Entry<Integer, ExamSubmittedAnswer> entry : submittedAnswerMap.entrySet()) {
                    map.put(entry.getKey(), entry.getValue().getSubmittedAnswer());
                }
                SubmittedExamService.getInstance().createSubmittedExam(handler,
                        examInfo.getExamId(), isSystemSubmit, map);
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
            topBar.setTitle(submittedExamInfo.getSubmitterInfo().getUsername());
        }

        // submittedAnswerMap
        if (alreadySubmitted()) {
            submittedAnswerMap = ProtoUtils.generateSubmittedExamAnswerMap(examInfo,
                    questionInfos, submittedExamInfo.getSubmittedAnswerMap());
        } else {
            submittedAnswerMap = ProtoUtils.generateSubmittedExamAnswerMap(examInfo,
                    questionInfos, null);
        }

        // check result
        if (alreadySubmitted()) {
            StringBuilder sb = new StringBuilder();
            double total = 0;
            for (ExamQuestionInfo examQuestionInfo : examInfo.getExamQuestionInfoList()) {
                Score score = examQuestionInfo.getScore();
                if (!score.hasMultipleScore()) {
                    total += score.getSingleScore();
                } else {
                    for (Double singleScore : score.getMultipleScore().getScoreMap().values()) {
                        total += singleScore;
                    }
                }
            }
            double count = 0;
            for (ExamSubmittedAnswer examSubmittedAnswer : submittedExamInfo.getSubmittedAnswerMap().values()) {
                if (examSubmittedAnswer.getScore() >= 0) {
                    count += examSubmittedAnswer.getScore();
                }
            }
            if (submittedExamInfo.getAlreadyCheckAllAnswer()) {
                sb.append("[已全部批改] 得分/总分：");
            } else {
                sb.append("[尚未全部批改] 得分/总分：");
            }
            sb.append(count).append("/").append(total);
            checkResultItemView.setDetailText(sb.toString().trim());
        } else {
            checkResultItemView.setDetailText("[尚未全部批改]");
        }
        if (userInfo.getUserType() == UserType.Teacher) {
            checkResultItemView.setVisibility(View.VISIBLE);
        } else {
            if (canSubmitAnswer()) {
                if (alreadySubmitted()) {
                    if (submittedExamInfo.getFinished()) {
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
                    if (submittedExamInfo.getFinished()) {
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
                    if (submittedExamInfo.getFinished()) {
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
        return submittedExamInfo != null && submittedExamInfo.getSubmittedExamId() > 0;
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SubmittedExamService.GET_SUBMITTED_EXAM_MSG) {
                handleGetSubmittedExamMsg(msg);
            } else if (msg.what == SubmittedExamService.CREATE_SUBMITTED_EXAM_MSG) {
                handleCreateSubmittedExamMsg(msg);
            } else if (msg.what == SubmittedExamService.UPDATE_SUBMITTED_EXAM_MSG) {
                handleUpdateSubmittedExamMsg(msg);
            } else if (msg.what == QuestionService.GET_QUESTION_MSG) {
                handleGetQuestionMsg(msg);
            }
        }
    };

    private void handleGetSubmittedExamMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            submittedExamInfo = serviceResult.extra(SubmittedExamGetResponse.class).getSubmittedExamInfo();
            finishInit();
        } else {
            if (serviceResult.extra(Boolean.class)) {
                ExamDetailActivity.this.finish();
            } else {
                if (userInfo.getUserType() == UserType.Teacher) {
                    setResult(RESULT_CANCELED);
                    ExamDetailActivity.this.finish();
                } else {
                    submittedExamInfo = null;
                    finishInit();
                }
            }
        }
    }

    private void handleCreateSubmittedExamMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        submitButton.setEnabled(true);
        saveButton.setEnabled(true);
        if (serviceResult.isSuccess()) {
            submittedExamInfo = serviceResult.extra(SubmittedExamCreateResponse.class).getSubmittedExamInfo();
            refresh();
        }
    }

    private void handleUpdateSubmittedExamMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        submitButton.setEnabled(true);
        saveButton.setEnabled(true);
        if (serviceResult.isSuccess()) {
            submittedExamInfo = serviceResult.extra(SubmittedExamUpdateResponse.class).getSubmittedExamInfo();
            refresh();
        }
    }

    private void handleGetQuestionMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess() || examInfo.getExamQuestionInfoCount() <= 0) {
            questionInfos.clear();
            questionInfos.addAll(serviceResult.extra(QuestionGetResponse.class).getQuestionInfoList());
            if (questionInfos.size() != examInfo.getExamQuestionInfoCount()) {
                Toast.makeText(this, "加载失败", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                ExamDetailActivity.this.finish();
            } else {
                finishInit();
            }
        } else {
            Toast.makeText(this, "加载失败", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            ExamDetailActivity.this.finish();
        }
    }

    private void finishInit() {
        if (waitingDialog != null && counter.incrementAndGet() == 2) {
            waitingDialog.dismiss();
            refresh();
            if (userInfo.getUserType() == UserType.Student) {
                if (!canSubmitAnswer()) {
                    if (!alreadySubmitted() || !submittedExamInfo.getFinished()) {
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
            intent.putExtra(IntentParam.SUBMITTED_EXAM_INFO, submittedExamInfo);
            setResult(RESULT_OK, intent);
        }
        super.onBackPressed();
    }

    @Override
    public boolean isFinished() {
        if (alreadySubmitted()) {
            return submittedExamInfo.getFinished();
        } else {
            return false;
        }
    }

    @Override
    public boolean canSubmitAnswer() {
        if (isFinished()) {
            return false;
        }
        long now = System.currentTimeMillis();
        return examInfo.getStartTime() <= now && now <= examInfo.getEndTime();
    }

    @Override
    public SubmittedAnswer requireSubmittedAnswerFor(long questionId) {
        for (ExamQuestionInfo examQuestionInfo : examInfo.getExamQuestionInfoList()) {
            if (examQuestionInfo.getQuestionId() == questionId) {
                ExamSubmittedAnswer examSubmittedAnswer = submittedAnswerMap.get(examQuestionInfo.getIndex());
                if (examSubmittedAnswer != null) {
                    return examSubmittedAnswer.getSubmittedAnswer();
                }
                break;
            }
        }
        return null;
    }

    @Override
    public double requireCheckStatusOrScoreFor(long questionId) {
        for (ExamQuestionInfo examQuestionInfo : examInfo.getExamQuestionInfoList()) {
            if (examQuestionInfo.getQuestionId() == questionId) {
                ExamSubmittedAnswer examSubmittedAnswer = submittedAnswerMap.get(examQuestionInfo.getIndex());
                if (examSubmittedAnswer != null) {
                    return examSubmittedAnswer.getScore();
                }
                break;
            }
        }
        return -1;
    }

    @Override
    public Score requireScoreFor(long questionId) {
        for (ExamQuestionInfo examQuestionInfo : examInfo.getExamQuestionInfoList()) {
            if (examQuestionInfo.getQuestionId() == questionId) {
                return examQuestionInfo.getScore();
            }
        }
        return null;
    }
}
