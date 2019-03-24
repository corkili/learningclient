package com.corkili.learningclient.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.ProtoUtils;
import com.corkili.learningclient.common.QuestionCheckStatus;
import com.corkili.learningclient.generate.protobuf.Info.MultipleChoiceAnswer;
import com.corkili.learningclient.generate.protobuf.Info.MultipleFillingAnswer;
import com.corkili.learningclient.generate.protobuf.Info.MultipleFillingSubmittedAnswer.Pair;
import com.corkili.learningclient.generate.protobuf.Info.QuestionInfo;
import com.corkili.learningclient.generate.protobuf.Info.QuestionType;
import com.corkili.learningclient.generate.protobuf.Info.Score;
import com.corkili.learningclient.generate.protobuf.Info.SingleFillingAnswer;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.UserInfo;
import com.corkili.learningclient.generate.protobuf.Info.UserType;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class SubmittedQuestionRecyclerViewAdapter extends RecyclerView.Adapter<SubmittedQuestionRecyclerViewAdapter.ViewHolder> {

    private Context context;
    private final List<QuestionInfo> questionInfos;
    private OnItemInteractionListener mListener;
    private ScoreDataBus scoreDataBus;
    private SubmitDataBus submitDataBus;
    private UserInfo userInfo;
//    private int opened;
    private final Map<Long, ViewHolder> viewHolderMap;

    public SubmittedQuestionRecyclerViewAdapter(Context context, List<QuestionInfo> questionInfos,
                                                OnItemInteractionListener mListener, UserInfo userInfo) {
        this.context = context;
        this.questionInfos = questionInfos;
        this.mListener = mListener;
        this.userInfo = userInfo;
//        this.opened = -1;
        viewHolderMap = new HashMap<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_question_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.mItem = questionInfos.get(position);
        viewHolderMap.put(holder.mItem.getQuestionId(), holder);
        holder.indexView.setText(String.valueOf(position + 1));
        holder.questionTypeView.setText(ProtoUtils.getQuestionTypeUIName(holder.mItem.getQuestionType()));
        holder.simpleDescriptionView.setText(holder.mItem.getQuestion());

        StringBuilder descriptionText = new StringBuilder();
        descriptionText.append(holder.mItem.getQuestion());
        if (holder.mItem.getQuestionType() == QuestionType.SingleChoice
                || holder.mItem.getQuestionType() == QuestionType.MultipleChoice) {
            if (holder.mItem.getChoicesCount() > 0) {
                if (isStudentFinishedState()) {
                    descriptionText.append("\n");
                    Map<Integer, String> choices = holder.mItem.getChoicesMap();
                    List<Integer> indexList = new ArrayList<>(choices.keySet());
                    Collections.sort(indexList, (o1, o2) -> o1 - o2);
                    for (int i = 0; i < indexList.size(); i++) {
                        Integer index = indexList.get(i);
                        if (choices.containsKey(index)) {
                            descriptionText.append("(").append(index).append(") ").append(choices.get(index)).append("\n");
                        }
                    }
                }
            }
        }
        if (descriptionText.toString().endsWith("\n")) {
            holder.descriptionView.setText(descriptionText.substring(0, descriptionText.lastIndexOf("\n")).trim());
        } else {
            holder.descriptionView.setText(descriptionText.toString().trim());
        }

        if (isStudentFinishedState()) {
            boolean setAnswer = false;
            if (holder.mItem.getQuestionType() == QuestionType.SingleFilling) {
                if (holder.mItem.getAnswer().hasSingleFillingAnswer()) {
                    StringBuilder sb = new StringBuilder();
                    SingleFillingAnswer singleFillingAnswer = holder.mItem.getAnswer().getSingleFillingAnswer();
                    List<String> answerList = singleFillingAnswer.getAnswerList();
                    for (int i = 0; i < answerList.size(); i++) {
                        String ans = answerList.get(i);
                        sb.append(ans);
                        if (i != answerList.size() - 1) {
                            sb.append(" | ");
                        }
                    }
                    if (sb.length() != 0) {
                        holder.answerView.setText(sb.toString());
                        setAnswer = true;
                    }
                }
            } else if (holder.mItem.getQuestionType() == QuestionType.MultipleFilling) {
                if (holder.mItem.getAnswer().hasMultipleFillingAnswer()) {
                    StringBuilder sb = new StringBuilder();
                    MultipleFillingAnswer multipleFillingAnswer = holder.mItem.getAnswer().getMultipleFillingAnswer();
                    Map<Integer, SingleFillingAnswer> answerMap = multipleFillingAnswer.getAnswerMap();
                    List<Integer> indexList = new ArrayList<>(answerMap.keySet());
                    Collections.sort(indexList, (o1, o2) -> o1 - o2);
                    for (int i = 0; i < indexList.size(); i++) {
                        Integer index = indexList.get(i);
                        if (answerMap.containsKey(index)) {
                            SingleFillingAnswer singleFillingAnswer = answerMap.get(index);
                            sb.append("(").append(index).append(") ");
                            List<String> answerList = singleFillingAnswer.getAnswerList();
                            for (int j = 0; j < answerList.size(); j++) {
                                String ans = answerList.get(j);
                                sb.append(ans);
                                if (j != answerList.size() - 1) {
                                    sb.append(" | ");
                                }
                            }
                            if (i != indexList.size() - 1) {
                                sb.append("\n");
                            }
                        }
                    }
                    if (sb.length() != 0) {
                        holder.answerView.setText(sb.toString());
                        setAnswer = true;
                    }
                }
            } else if (holder.mItem.getQuestionType() == QuestionType.SingleChoice
                    || holder.mItem.getQuestionType() == QuestionType.MultipleChoice) {
                if (holder.mItem.getChoicesCount() > 0) {
                    StringBuilder sb = new StringBuilder();
                    if (holder.mItem.getQuestionType() == QuestionType.SingleChoice) {
                        if (holder.mItem.getAnswer().hasSingleChoiceAnswer() && holder.mItem.getAnswer().getSingleChoiceAnswer().getChoice() > 0) {
                            sb.append("正确选项为： ");
                            sb.append(holder.mItem.getAnswer().getSingleChoiceAnswer().getChoice());
                        } else {
                            sb.append("未提供正确选项");
                        }
                    } else {
                        if (holder.mItem.getAnswer().hasMultipleChoiceAnswer()) {
                            MultipleChoiceAnswer multipleChoiceAnswer = holder.mItem.getAnswer().getMultipleChoiceAnswer();
                            if (multipleChoiceAnswer.getChoiceCount() > 0) {
                                sb.append("正确选项为");
                                if (multipleChoiceAnswer.getSelectAllIsCorrect()) {
                                    sb.append("(少选或错选不得分)： ");
                                } else {
                                    sb.append("(错选不得分，少选得一半分)： ");
                                }
                                sb.append(IUtils.list2String(multipleChoiceAnswer.getChoiceList(), ", "));
                            } else {
                                sb.append("未提供正确选项");
                            }
                        } else {
                            sb.append("未提供正确选项");
                        }
                    }
                    holder.answerView.setText(sb.toString());
                    setAnswer = true;
                }
            } else if (holder.mItem.getQuestionType() == QuestionType.Essay) {
                if (holder.mItem.getAnswer().hasEssayAnswer()) {
                    holder.answerView.setText(holder.mItem.getAnswer().getEssayAnswer().getText());
                    setAnswer = true;
                }
            }

            if (!setAnswer) {
                holder.answerView.setText("未设置标准答案");
            }

            holder.answerTextView.setVisibility(View.VISIBLE);
            holder.answerView.setVisibility(View.VISIBLE);
        } else {
            holder.answerTextView.setVisibility(View.GONE);
            holder.answerView.setVisibility(View.GONE);
        }

        if (scoreDataBus != null) {
            StringBuilder sb = new StringBuilder("分数： ");
            Score score = scoreDataBus.requireScoreFor(holder.mItem.getQuestionId());
            if (score != null) {
                if (holder.mItem.getQuestionType() == QuestionType.MultipleFilling) {
                    List<Integer> indexList = new ArrayList<>(holder.mItem.getAnswer()
                            .getMultipleFillingAnswer().getAnswerMap().keySet());
                    Collections.sort(indexList, ((o1, o2) -> o1 - o2));
                    Map<Integer, Double> scoreMap;
                    if (score.hasMultipleScore()) {
                        scoreMap = score.getMultipleScore().getScoreMap();
                    } else {
                        scoreMap = Collections.emptyMap();
                    }
                    for (int i = 0; i < indexList.size(); i++) {
                        Integer index = indexList.get(i);
                        if (scoreMap.containsKey(index)) {
                            sb.append(IUtils.formatScore(scoreMap.get(index)));
                        } else {
                            sb.append("0.00");
                        }
                        if (i != indexList.size() - 1) {
                            sb.append(", ");
                        }
                    }
                } else {
                    if (!score.hasMultipleScore()) {
                        sb.append(IUtils.formatScore(score.getSingleScore()));
                    } else {
                        sb.append("0.00");
                    }
                }
            } else {
                sb.append("0.00");
            }
            holder.scoreView.setVisibility(View.VISIBLE);
            holder.scoreView.setText(sb.toString());
        } else {
            holder.scoreView.setVisibility(View.GONE);
        }

        for (FillingView fillingView : holder.fillingViewMap.values()) {
            holder.submitAnswerLayout.removeView(fillingView.view);
        }
        for (ChoiceView choiceView : holder.choiceViewMap.values()) {
            holder.submitAnswerLayout.removeView(choiceView.view);
        }
        holder.fillingViewMap.clear();
        holder.choiceViewMap.clear();
        if (isStudentFinishedState()) {
            holder.submitAnswerTextView.setVisibility(View.GONE);
            holder.submitAnswerLayout.setVisibility(View.GONE);
        } else {
            SubmittedAnswer submittedAnswer = null;
            if (submitDataBus != null) {
                submittedAnswer = submitDataBus.requireSubmittedAnswerFor(holder.mItem.getQuestionId());
            }
            if (holder.mItem.getQuestionType() == QuestionType.SingleFilling
                    || holder.mItem.getQuestionType() == QuestionType.MultipleFilling) {
                holder.essayAnswerEditor.setVisibility(View.GONE);
                List<Integer> fillingIndexList = new ArrayList<>();
                Map<Integer, String> submitFillingMap = new HashMap<>();
                if (holder.mItem.getAnswer().hasSingleFillingAnswer()) {
                    fillingIndexList.add(1);
                    if (submittedAnswer != null && submittedAnswer.hasSingleFillingSubmittedAnswer()) {
                        submitFillingMap.put(1, submittedAnswer.getSingleFillingSubmittedAnswer().getAnswer());
                    }
                } else if (holder.mItem.getAnswer().hasMultipleFillingAnswer()) {
                    fillingIndexList.addAll(holder.mItem.getAnswer().getMultipleFillingAnswer().getAnswerMap().keySet());
                    Collections.sort(fillingIndexList, (o1, o2) -> o1 - o2);
                    if (submittedAnswer != null && submittedAnswer.hasMultipleFillingSubmittedAnswer()) {
                        for (Entry<Integer, Pair> entry : submittedAnswer.getMultipleFillingSubmittedAnswer().getAnswerMap().entrySet()) {
                            submitFillingMap.put(entry.getKey(), entry.getValue().getAnswer());
                        }
                    }
                }
                if (fillingIndexList.isEmpty()) {
                    throw new RuntimeException("invalid Answer Of Multiple Filling Question");
                }
                for (Integer index : fillingIndexList) {
                    View view = LayoutInflater.from(context)
                            .inflate(R.layout.activity_question_submit_answer_filling, null);
                    FillingView fillingView = new FillingView(view);
                    fillingView.indexView.setText(IUtils.format("({}) ", index));
                    String ans = submitFillingMap.get(index);
                    if (StringUtils.isNotBlank(ans)) {
                        fillingView.fillingEditor.setText(ans);
                    } else {
                        fillingView.fillingEditor.setText("");
                    }
                    holder.fillingViewMap.put(index, fillingView);
                    holder.submitAnswerLayout.addView(view);
                }
            } else if (holder.mItem.getQuestionType() == QuestionType.SingleChoice
                    || holder.mItem.getQuestionType() == QuestionType.MultipleChoice){
                holder.essayAnswerEditor.setVisibility(View.GONE);
                Set<Integer> submitChoiceSet = new HashSet<>();
                if (submittedAnswer != null && submittedAnswer.hasSingleChoiceSubmittedAnswer()) {
                    submitChoiceSet.add(submittedAnswer.getSingleChoiceSubmittedAnswer().getChoice());
                } else if (submittedAnswer != null && submittedAnswer.hasMultipleChoiceSubmittedAnswer()) {
                    submitChoiceSet.addAll(submittedAnswer.getMultipleChoiceSubmittedAnswer().getChoiceList());
                }
                if (holder.mItem.getChoicesCount() <= 0) {
                    throw new RuntimeException("choice question lost choices");
                }
                Map<Integer, String> choices = holder.mItem.getChoicesMap();
                List<Integer> indexList = new ArrayList<>(choices.keySet());
                Collections.sort(indexList, (o1, o2) -> o1 - o2);
                for (Integer index : indexList) {
                    View view = LayoutInflater.from(context)
                            .inflate(R.layout.activity_question_submit_answer_choice, null);
                    ChoiceView choiceView = new ChoiceView(view);
                    choiceView.choiceView.setText(choices.get(index));
                    if (submitChoiceSet.contains(index)) {
                        choiceView.checkBox.setChecked(true);
                    }
                    if (holder.mItem.getQuestionType() == QuestionType.SingleChoice) {
                        choiceView.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            if (isChecked) {
                                for (ChoiceView cv : holder.choiceViewMap.values()) {
                                    if (!cv.view.equals(view)) {
                                        cv.checkBox.setChecked(false);
                                    }
                                }
                            }
                        });
                    }
                    holder.choiceViewMap.put(index, choiceView);
                    holder.submitAnswerLayout.addView(view);
                }
            } else {
                holder.essayAnswerEditor.setVisibility(View.VISIBLE);
                if (submittedAnswer != null && submittedAnswer.hasEssaySubmittedAnswer()) {
                    holder.essayAnswerEditor.setText(submittedAnswer.getEssaySubmittedAnswer().getText());
                } else {
                    holder.essayAnswerEditor.setText("");
                }
            }

            holder.submitAnswerTextView.setVisibility(View.VISIBLE);
            holder.submitAnswerLayout.setVisibility(View.VISIBLE);
        }

        if (isStudentFinishedState()) {
            SubmittedAnswer submittedAnswer = null;
            double checkStatusOrScore = QuestionCheckStatus.NOT_CHECK;
            if (submitDataBus != null) {
                submittedAnswer = submitDataBus.requireSubmittedAnswerFor(holder.mItem.getQuestionId());
                checkStatusOrScore = submitDataBus.requireCheckStatusOrScoreFor(holder.mItem.getQuestionId());
            }

            if (submittedAnswer == null) {
                holder.myAnswerView.setText("[未作答]");
            } else {
                StringBuilder sb = new StringBuilder();
                if (scoreDataBus != null) {
                    if (checkStatusOrScore < 0) {
                        sb.append("[未批改]");
                    } else {
                        sb.append("[得分：").append(IUtils.formatScore(checkStatusOrScore)).append("]");
                    }
                } else {
                    sb.append(QuestionCheckStatus.getStatusUI((int) checkStatusOrScore));
                }
                sb.append("\n");
                boolean setMyAns = false;
                if (holder.mItem.getQuestionType() == QuestionType.SingleFilling) {
                    if (submittedAnswer.hasSingleFillingSubmittedAnswer()) {
                        sb.append("(1) ");
                        if (StringUtils.isBlank(submittedAnswer.getSingleFillingSubmittedAnswer().getAnswer())) {
                            sb.append("[未作答]");
                        } else {
                            sb.append(submittedAnswer.getSingleFillingSubmittedAnswer().getAnswer());
                        }
                        setMyAns = true;
                    }
                } else if (holder.mItem.getQuestionType() == QuestionType.MultipleFilling) {
                    if (submittedAnswer.hasMultipleFillingSubmittedAnswer()) {
                        List<Pair> pairList = new ArrayList<>(submittedAnswer
                                .getMultipleFillingSubmittedAnswer().getAnswerMap().values());
                        Collections.sort(pairList, (o1, o2) -> o1.getIndex() - o2.getIndex());
                        for (int i = 0; i < pairList.size(); i++) {
                            Pair pair = pairList.get(i);
                            sb.append("(").append(i + 1).append(") ");
                            if (scoreDataBus != null) {
                                if (pair.getScoreOrCheckStatus() < 0) {
                                    sb.append("[未批改]");
                                } else {
                                    sb.append("[得分：").append(IUtils.formatScore(pair.getScoreOrCheckStatus())).append("]");
                                }
                            } else {
                                sb.append(QuestionCheckStatus.getStatusUI((int) pair.getScoreOrCheckStatus()));
                            }
                            if (StringUtils.isBlank(pair.getAnswer())) {
                                sb.append("[未作答]");
                            } else {
                                sb.append(pair.getAnswer());
                            }
                            if (i != pairList.size() - 1) {
                                sb.append("\n");
                            }
                        }
                        setMyAns = true;
                    }
                } else if (holder.mItem.getQuestionType() == QuestionType.SingleChoice) {
                    if (submittedAnswer.hasSingleChoiceSubmittedAnswer()) {
                        if (submittedAnswer.getSingleChoiceSubmittedAnswer().getChoice() > 0) {
                            sb.append(submittedAnswer.getSingleChoiceSubmittedAnswer().getChoice());
                        } else {
                            sb.append("[未作答]");
                        }
                        setMyAns = true;
                    }
                } else if (holder.mItem.getQuestionType() == QuestionType.MultipleChoice) {
                    if (submittedAnswer.hasMultipleChoiceSubmittedAnswer()) {
                        if (submittedAnswer.getMultipleChoiceSubmittedAnswer().getChoiceCount() > 0) {
                            sb.append(IUtils.list2String(submittedAnswer
                                    .getMultipleChoiceSubmittedAnswer().getChoiceList(), ","));
                        } else {
                            sb.append("[未作答]");
                        }
                        setMyAns = true;
                    }
                } else if (holder.mItem.getQuestionType() == QuestionType.Essay) {
                    if (submittedAnswer.hasEssaySubmittedAnswer()) {
                        if (StringUtils.isBlank(submittedAnswer.getEssaySubmittedAnswer().getText())) {
                            sb.append("[未作答]");
                        } else {
                            sb.append(submittedAnswer.getEssaySubmittedAnswer().getText());
                        }
                        setMyAns = true;
                    }
                }
                if (!setMyAns) {
                    sb.append("[未作答]");
                }
                holder.myAnswerView.setText(sb.toString().trim());
            }

            holder.myAnswerTextView.setVisibility(View.VISIBLE);
            holder.myAnswerView.setVisibility(View.VISIBLE);
        } else {
            holder.myAnswerTextView.setVisibility(View.GONE);
            holder.myAnswerView.setVisibility(View.GONE);
        }

        holder.mView.setOnLongClickListener(v -> {
            if (this.mListener != null) {
                return this.mListener.onItemLongClick(holder);
            }
            return false;
        });

//        if (position == opened) {
//            holder.detailLayout.setVisibility(View.VISIBLE);
//            holder.simpleDescriptionView.setVisibility(View.GONE);
//        } else {
//            holder.detailLayout.setVisibility(View.GONE);
//            holder.simpleDescriptionView.setVisibility(View.VISIBLE);
//        }

    }

    @Override
    public int getItemCount() {
        return questionInfos.size();
    }

    public void setScoreDataBus(ScoreDataBus scoreDataBus) {
        this.scoreDataBus = scoreDataBus;
    }

    public void setSubmitDataBus(SubmitDataBus submitDataBus) {
        this.submitDataBus = submitDataBus;
    }

    public ViewHolder getViewHolder(long questionId) {
        return viewHolderMap.get(questionId);
    }

    private boolean isStudentFinishedState() {
        if (userInfo.getUserType() == UserType.Student) {
            if (submitDataBus != null) {
                return submitDataBus.isFinished();
            }
        }
        return false;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final View mView;
        private final TextView indexView;
        private final TextView questionTypeView;
        private final TextView simpleDescriptionView;
        private final TextView descriptionView;
        private final TextView answerTextView;
        private final TextView answerView;
        private final TextView scoreView;
        private final TextView submitAnswerTextView;
        private final LinearLayout submitAnswerLayout;
        private final Map<Integer, ChoiceView> choiceViewMap;
        private final Map<Integer, FillingView> fillingViewMap;
        private final EditText essayAnswerEditor;
        private final TextView myAnswerTextView;
        private final TextView myAnswerView;
        private final View detailLayout;
        private QuestionInfo mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            indexView = view.findViewById(R.id.question_index);
            questionTypeView = view.findViewById(R.id.question_type);
            simpleDescriptionView = view.findViewById(R.id.question_simple_description);
            descriptionView = view.findViewById(R.id.question_description);
            answerTextView = view.findViewById(R.id.answer_text);
            answerView = view.findViewById(R.id.question_answer);
            scoreView = view.findViewById(R.id.question_score);
            submitAnswerTextView = view.findViewById(R.id.submit_answer_text);
            submitAnswerLayout = view.findViewById(R.id.submit_answer);
            choiceViewMap = new HashMap<>();
            fillingViewMap = new HashMap<>();
            essayAnswerEditor = view.findViewById(R.id.essay_answer);
            myAnswerTextView = view.findViewById(R.id.my_answer_text);
            myAnswerView = view.findViewById(R.id.my_answer);
            detailLayout = view.findViewById(R.id.detail_layout);

//            mView.setOnClickListener(v -> {
//                if (opened == getAdapterPosition()) {
//                    opened = -1;
//                    notifyItemChanged(getAdapterPosition());
//                } else {
//                    int oldOpened = opened;
//                    opened = getAdapterPosition();
//                    notifyItemChanged(oldOpened);
//                    notifyItemChanged(opened);
//                }
//            });

        }

        public QuestionInfo getQuestionInfo() {
            return mItem;
        }

        public Map<Integer, ChoiceView> getChoiceViewMap() {
            return choiceViewMap;
        }

        public Map<Integer, FillingView> getFillingViewMap() {
            return fillingViewMap;
        }

        public EditText getEssayAnswerEditor() {
            return essayAnswerEditor;
        }
    }

    public class ChoiceView {
        private final View view;
        private final CheckBox checkBox;
        private final TextView choiceView;

        ChoiceView(View view) {
            this.view = view;
            checkBox = view.findViewById(R.id.check_choice);
            choiceView = view.findViewById(R.id.choice);
        }

        public CheckBox getCheckBox() {
            return checkBox;
        }

    }

    public class FillingView {
        private final View view;
        private final TextView indexView;
        private final EditText fillingEditor;

        FillingView(View view) {
            this.view = view;
            indexView = view.findViewById(R.id.filling_index);
            fillingEditor = view.findViewById(R.id.filling);
        }

        public EditText getFillingEditor() {
            return fillingEditor;
        }
    }

    public interface OnItemInteractionListener {

        boolean onItemLongClick(ViewHolder viewHolder);

    }

    public interface ScoreDataBus {

        Score requireScoreFor(long questionId);

    }

    public interface SubmitDataBus {

        boolean isFinished();

        SubmittedAnswer requireSubmittedAnswerFor(long questionId);

        double requireCheckStatusOrScoreFor(long questionId);

    }

}
