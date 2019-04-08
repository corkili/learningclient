package com.corkili.learningclient.ui.activity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import android.widget.TextView;
import android.widget.Toast;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.IntentParam;
import com.corkili.learningclient.common.ProtoUtils;
import com.corkili.learningclient.generate.protobuf.Info.CourseInfo;
import com.corkili.learningclient.generate.protobuf.Info.ExamInfo;
import com.corkili.learningclient.generate.protobuf.Info.ExamQuestionInfo;
import com.corkili.learningclient.generate.protobuf.Info.QuestionInfo;
import com.corkili.learningclient.generate.protobuf.Info.QuestionType;
import com.corkili.learningclient.generate.protobuf.Info.Score;
import com.corkili.learningclient.generate.protobuf.Response.ExamCreateResponse;
import com.corkili.learningclient.generate.protobuf.Response.ExamUpdateResponse;
import com.corkili.learningclient.generate.protobuf.Response.QuestionFindAllResponse;
import com.corkili.learningclient.service.ExamService;
import com.corkili.learningclient.service.QuestionService;
import com.corkili.learningclient.service.ServiceResult;
import com.corkili.learningclient.ui.adapter.QuestionRecyclerViewAdapter;
import com.corkili.learningclient.ui.adapter.QuestionRecyclerViewAdapter.ViewHolder;
import com.corkili.learningclient.ui.other.MyRecyclerViewDivider;
import com.qmuiteam.qmui.widget.QMUITopBarLayout;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog.CustomDialogBuilder;
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

public class ExamEditActivity extends AppCompatActivity 
        implements QuestionRecyclerViewAdapter.OnItemInteractionListener, QuestionRecyclerViewAdapter.ScoreDataBus {

    private static final int REQUEST_CODE_SELECT_QUESTION = 0xF1;

    private boolean isCreate;
    private ExamInfo examInfo;
    private CourseInfo courseInfo;
    private Calendar startTime;
    private Calendar endTime;

    private QMUITopBarLayout topBar;
    private EditText examNameEditor;
    private EditText startTimeEditor;
    private EditText endTimeEditor;
    private QMUIRoundButton addExamQuestionButton;

    private RecyclerView recyclerView;
    private QuestionRecyclerViewAdapter recyclerViewAdapter;

    private List<QuestionInfo> questionInfos;
    private Map<Long, Score> questionScoreMap;
    private Map<Long, QuestionInfo> allQuestionInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_edit);

        isCreate = getIntent().getBooleanExtra(IntentParam.IS_CREATE, true);
        examInfo = (ExamInfo) getIntent().getSerializableExtra(IntentParam.EXAM_INFO);
        courseInfo = (CourseInfo) getIntent().getSerializableExtra(IntentParam.COURSE_INFO);

        if (courseInfo == null || (!isCreate && examInfo == null)) {
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
                        .setTitle("删除考试")
                        .setMessage("确定删除该考试？")
                        .addAction("取消", (dialog, index) -> dialog.dismiss())
                        .addAction(0, "删除", QMUIDialogAction.ACTION_PROP_NEGATIVE, (dialog, index) -> {
                            ExamService.getInstance().deleteExam(handler, examInfo.getExamId());
                            dialog.dismiss();
                        })
                        .show();
            });
        }

        topBar.addRightImageButton(R.drawable.ic_save_24dp, R.id.topbar_right_save).setOnClickListener(v -> {
            if (StringUtils.isBlank(startTimeEditor.getText().toString())) {
                Toast.makeText(ExamEditActivity.this, "请设置开始时间", Toast.LENGTH_SHORT).show();
                return;
            }
            if (StringUtils.isBlank(endTimeEditor.getText().toString())) {
                Toast.makeText(ExamEditActivity.this, "请设置结束时间", Toast.LENGTH_SHORT).show();
                return;
            }
            String examName = examNameEditor.getText().toString().trim();
            Map<Integer, Long> selectedQuestions = new HashMap<>();
            Map<Integer, Score> selectedQuestionScoreMap = new HashMap<>();
            for (int i = 0; i < questionInfos.size(); i++) {
                QuestionInfo questionInfo = questionInfos.get(i);
                selectedQuestions.put(i + 1, questionInfo.getQuestionId());
                Score score = questionScoreMap.get(questionInfo.getQuestionId());
                if (score == null) {
                    score = ProtoUtils.generateScore(questionInfo);
                    questionScoreMap.put(questionInfo.getQuestionId(), score);
                }
                selectedQuestionScoreMap.put(i + 1, score);
            }
            if (isCreate) {
                ExamService.getInstance().createExam(handler, examName, courseInfo.getCourseId(),
                        startTime.getTime(), endTime.getTime(), selectedQuestions, selectedQuestionScoreMap);
            } else {
                if (examName.equals(examInfo.getExamName())) {
                    examName = null;
                }
                boolean updateQuestion = false;
                if (selectedQuestions.size() == examInfo.getExamQuestionInfoCount()) {
                    for (ExamQuestionInfo questionInfo : examInfo.getExamQuestionInfoList()) {
                        if (!selectedQuestions.containsKey(questionInfo.getIndex())
                                || selectedQuestions.get(questionInfo.getIndex()) != questionInfo.getQuestionId()
                                || !questionInfo.getScore().equals(selectedQuestionScoreMap.get(questionInfo.getIndex()))) {
                            updateQuestion = true;
                            break;
                        }
                    }
                } else {
                    updateQuestion = true;
                }
                boolean updateStartTime = startTime.getTimeInMillis() != examInfo.getStartTime();
                boolean updateEndTime = endTime.getTimeInMillis() != examInfo.getEndTime();
                if (examInfo.getStartTime() <= System.currentTimeMillis()) {
                    updateStartTime = false;
                    updateQuestion = false;
                    selectedQuestions = null;
                    selectedQuestionScoreMap = null;
                }
                ExamService.getInstance().updateExam(handler, examInfo.getExamId(), examName, updateStartTime,
                        startTime.getTime(), updateEndTime, endTime.getTime(), updateQuestion,
                        selectedQuestions, selectedQuestionScoreMap);
            }
        });

        examNameEditor = findViewById(R.id.exam_edit_text_edit_name);
        startTimeEditor = findViewById(R.id.exam_edit_text_edit_start_time);
        endTimeEditor = findViewById(R.id.exam_edit_text_edit_end_time);
        addExamQuestionButton = findViewById(R.id.exam_button_add_question);

        startTime = Calendar.getInstance(Locale.CHINA);
        endTime = Calendar.getInstance(Locale.CHINA);

        startTimeEditor.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(ExamEditActivity.this, (view, year, month, dayOfMonth) -> {
                startTime.set(Calendar.YEAR, year);
                startTime.set(Calendar.MONTH, month);
                startTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                startTimeEditor.setText(IUtils.DATE_TIME_NO_SEC_FORMATTER.format(startTime.getTime()));
                TimePickerDialog timePickerDialog = new TimePickerDialog(ExamEditActivity.this, (view1, hourOfDay, minute) -> {
                    startTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    startTime.set(Calendar.MINUTE, minute);
                    startTime.set(Calendar.SECOND, 0);
                    startTime.set(Calendar.MILLISECOND, 0);
                    startTimeEditor.setText(IUtils.DATE_TIME_NO_SEC_FORMATTER.format(startTime.getTime()));
                }, startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE), true);
                timePickerDialog.show();
            }, startTime.get(Calendar.YEAR), startTime.get(Calendar.MONTH), startTime.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        endTimeEditor.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(ExamEditActivity.this, (view, year, month, dayOfMonth) -> {
                endTime.set(Calendar.YEAR, year);
                endTime.set(Calendar.MONTH, month);
                endTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                endTimeEditor.setText(IUtils.DATE_TIME_NO_SEC_FORMATTER.format(endTime.getTime()));
                TimePickerDialog timePickerDialog = new TimePickerDialog(ExamEditActivity.this, (view1, hourOfDay, minute) -> {
                    endTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    endTime.set(Calendar.MINUTE, minute);
                    endTime.set(Calendar.SECOND, 0);
                    endTime.set(Calendar.MILLISECOND, 0);
                    endTimeEditor.setText(IUtils.DATE_TIME_NO_SEC_FORMATTER.format(endTime.getTime()));
                }, endTime.get(Calendar.HOUR_OF_DAY), endTime.get(Calendar.MINUTE), true);
                timePickerDialog.show();
            }, endTime.get(Calendar.YEAR), endTime.get(Calendar.MONTH), endTime.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        addExamQuestionButton.setOnClickListener(v -> {
            Intent intent = new Intent(ExamEditActivity.this, QuestionSelectActivity.class);
            startActivityForResult(intent, REQUEST_CODE_SELECT_QUESTION);
        });

        allQuestionInfo = new HashMap<>();

        recyclerView = findViewById(R.id.question_list);
        questionInfos = new ArrayList<>();
        questionScoreMap = new HashMap<>();
        recyclerViewAdapter = new QuestionRecyclerViewAdapter(this, questionInfos, this);
        recyclerViewAdapter.setScoreDataBus(this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.addItemDecoration(new MyRecyclerViewDivider(this, LinearLayoutManager.HORIZONTAL,
                1,ContextCompat.getColor(this,R.color.colorBlack)));

        QuestionService.getInstance().findAllQuestion(handler, true, null, null);

        if (!isCreate) {
            examNameEditor.setText(examInfo.getExamName());
            startTime.setTimeInMillis(examInfo.getStartTime());
            startTimeEditor.setText(IUtils.DATE_TIME_NO_SEC_FORMATTER.format(startTime.getTime()));
            endTime.setTimeInMillis(examInfo.getEndTime());
            endTimeEditor.setText(IUtils.DATE_TIME_NO_SEC_FORMATTER.format(endTime.getTime()));

            if (examInfo.getStartTime() <= System.currentTimeMillis()) {
                startTimeEditor.setEnabled(false);
                addExamQuestionButton.setEnabled(false);
                addExamQuestionButton.setVisibility(View.GONE);
            }
        }

    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == QuestionService.FIND_ALL_QUESTION_MSG) {
                handleFindAllQuestionMsg(msg);
            } else if (msg.what == ExamService.CREATE_EXAM_MSG) {
                handleCreateExamMsg(msg);
            } else if (msg.what == ExamService.UPDATE_EXAM_MSG) {
                handleUpdateExamMsg(msg);
            } else if (msg.what == ExamService.DELETE_EXAM_MSG) {
                handleDeleteExamMsg(msg);
            }
        }
    };

    private void handleFindAllQuestionMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            allQuestionInfo.clear();
            for (QuestionInfo questionInfo : serviceResult.extra(QuestionFindAllResponse.class).getQuestionInfoList()) {
                allQuestionInfo.put(questionInfo.getQuestionId(), questionInfo);
            }
            if (!isCreate) {
                List<ExamQuestionInfo> examQuestionInfoList = new ArrayList<>(
                        examInfo.getExamQuestionInfoList());
                Collections.sort(examQuestionInfoList, ((o1, o2) -> o1.getIndex() - o2.getIndex()));
                for (ExamQuestionInfo examQuestionInfo : examQuestionInfoList) {
                    if (allQuestionInfo.containsKey(examQuestionInfo.getQuestionId())) {
                        questionInfos.add(allQuestionInfo.get(examQuestionInfo.getQuestionId()));
                        questionScoreMap.put(examQuestionInfo.getQuestionId(), examQuestionInfo.getScore());
                    }
                }
                recyclerViewAdapter.notifyDataSetChanged();
            }

        }
    }

    private void handleCreateExamMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            ExamInfo examInfo = serviceResult.extra(ExamCreateResponse.class).getExamInfo();
            Intent intent = new Intent();
            intent.putExtra(IntentParam.EXAM_INFO, examInfo);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void handleUpdateExamMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            ExamInfo examInfo = serviceResult.extra(ExamUpdateResponse.class).getExamInfo();
            Intent intent = new Intent();
            intent.putExtra(IntentParam.EXAM_INFO, examInfo);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void handleDeleteExamMsg(Message msg) {
        ServiceResult serviceResult = (ServiceResult) msg.obj;
        Toast.makeText(this, serviceResult.msg(), Toast.LENGTH_SHORT).show();
        if (serviceResult.isSuccess()) {
            Intent intent = new Intent();
            intent.putExtra(IntentParam.EXAM_INFO, examInfo);
            intent.putExtra(IntentParam.DELETE_EXAM, true);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void showQuestionScoreUpdateDialog(final QuestionInfo questionInfo) {
        QMUIDialog.CustomDialogBuilder scoreUpdateDialog = new CustomDialogBuilder(this);
        scoreUpdateDialog.setLayout(R.layout.dialog_score_editor);
        scoreUpdateDialog.setTitle("设置分数");
        scoreUpdateDialog.addAction("取消", (dialog, which) -> {
            dialog.cancel();
            dialog.dismiss();
        });
        scoreUpdateDialog.addAction("确定", (dialog, which) -> {
            EditText scoreEditView = dialog.findViewById(R.id.text_edit_score);
            String scoreStr = scoreEditView.getText().toString().trim();
            Score score;
            boolean formatError = false;
            boolean lessScore = false;
            if (questionInfo.getQuestionType() != QuestionType.MultipleFilling) {
                double singleScore;
                try {
                    singleScore = Double.parseDouble(scoreStr);
                } catch (NumberFormatException e) {
                    singleScore = 0.0;
                    formatError = true;
                }
                if (singleScore < 0) {
                    singleScore = 0.0;
                    formatError = true;
                }
                score = ProtoUtils.generateScore(questionInfo, singleScore);
            } else {
                String[] scoreStrArr = scoreStr.split(" ");
                double[] scoreArr = new double[questionInfo.getAnswer().getMultipleFillingAnswer().getAnswerCount()];
                for (int i = 0; i < scoreArr.length; i++) {
                    double scoreValue;
                    if (i < scoreStrArr.length) {
                        try {
                            scoreValue = Double.parseDouble(scoreStrArr[i]);
                        } catch (NumberFormatException e) {
                            scoreValue = 0.0;
                            formatError = true;
                        }
                    } else {
                        scoreValue = 0.0;
                        lessScore = true;
                    }
                    if (scoreValue < 0) {
                        scoreValue = 0.0;
                        formatError = true;
                    }
                    scoreArr[i] = scoreValue;
                }
                score = ProtoUtils.generateScore(questionInfo, scoreArr);
            }
            questionScoreMap.put(questionInfo.getQuestionId(), score);
            if (formatError && !lessScore) {
                Toast.makeText(ExamEditActivity.this, "输入的分数必须大于或等于0", Toast.LENGTH_SHORT).show();
            } else if (lessScore && !formatError) {
                Toast.makeText(ExamEditActivity.this, "填空题（多空）设置的分数过少", Toast.LENGTH_SHORT).show();
            } else if (lessScore && formatError) {
                Toast.makeText(ExamEditActivity.this, "填空题（多空）设置的分数过少，且输入的分数必须大于或等于0", Toast.LENGTH_SHORT).show();
            }
            int index = -1;
            for (int i = 0; i < questionInfos.size(); i++) {
                if (questionInfo.getQuestionId() == questionInfos.get(i).getQuestionId()) {
                    index = i;
                }
            }
            if (index >= 0) {
                recyclerViewAdapter.notifyItemChanged(index);
            }
            dialog.dismiss();
        });
        QMUIDialog dialog = scoreUpdateDialog.create();
        if (questionInfo.getQuestionType() != QuestionType.MultipleFilling) {
            dialog.<TextView>findViewById(R.id.text_view_tip).setText(IUtils.format(
                    "本题是{}，请输入一个分数",
                    ProtoUtils.getQuestionTypeUIName(questionInfo.getQuestionType())));
        } else {
            int count = questionInfo.getAnswer().getMultipleFillingAnswer().getAnswerCount();
            dialog.<TextView>findViewById(R.id.text_view_tip).setText(IUtils.format(
                    "本题是{}，共有{}个空，请依次输入{}个分数（用空格分割）",
                    ProtoUtils.getQuestionTypeUIName(questionInfo.getQuestionType()),count, count));
        }
        dialog.show();
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
                    Toast.makeText(this, "不能选择相同的试题", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            questionInfos.add(questionInfo);
            questionScoreMap.put(questionInfo.getQuestionId(), ProtoUtils.generateScore(questionInfo));
            allQuestionInfo.put(questionInfo.getQuestionId(), questionInfo);
            recyclerViewAdapter.notifyDataSetChanged();
            showQuestionScoreUpdateDialog(questionInfo);
        }
    }

    @Override
    public boolean onItemLongClick(ViewHolder viewHolder) {
        if (examInfo.getStartTime() <= System.currentTimeMillis()) {
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

    @Override
    public void onScoreViewClick(ViewHolder viewHolder) {
        if (examInfo.getStartTime() <= System.currentTimeMillis()) {
            return;
        }
        showQuestionScoreUpdateDialog(viewHolder.getQuestionInfo());
    }

    @Override
    public Score requireScoreFor(long questionId) {
        return questionScoreMap.get(questionId);
    }
}
