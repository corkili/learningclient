package com.corkili.learningclient.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.common.ProtoUtils;
import com.corkili.learningclient.generate.protobuf.Info.MultipleChoiceAnswer;
import com.corkili.learningclient.generate.protobuf.Info.MultipleFillingAnswer;
import com.corkili.learningclient.generate.protobuf.Info.QuestionInfo;
import com.corkili.learningclient.generate.protobuf.Info.QuestionType;
import com.corkili.learningclient.generate.protobuf.Info.Score;
import com.corkili.learningclient.generate.protobuf.Info.SingleFillingAnswer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class QuestionRecyclerViewAdapter extends RecyclerView.Adapter<QuestionRecyclerViewAdapter.ViewHolder> {

    private Context context;
    private final List<QuestionInfo> questionInfos;
    private OnItemInteractionListener mListener;
    private ScoreDataBus scoreDataBus;
    private int opened;

    public QuestionRecyclerViewAdapter(Context context, List<QuestionInfo> questionInfos,
                                       OnItemInteractionListener mListener) {
        this.context = context;
        this.questionInfos = questionInfos;
        this.mListener = mListener;
        this.opened = -1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_question_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        holder.mItem = questionInfos.get(position);
        holder.indexView.setText(String.valueOf(position + 1));
        if (holder.mItem.getAutoCheck()) {
            holder.autoCheckView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_yes_bk_green));
        } else {
            holder.autoCheckView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_no_bk_red));
        }
        holder.questionTypeView.setText(ProtoUtils.getQuestionTypeUIName(holder.mItem.getQuestionType()));
        holder.simpleDescriptionView.setText(holder.mItem.getQuestion());
        holder.descriptionView.setText(holder.mItem.getQuestion());

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
                StringBuilder descriptionText = new StringBuilder();
                descriptionText.append(holder.mItem.getQuestion()).append("\n");
                Map<Integer, String> choices = holder.mItem.getChoicesMap();
                List<Integer> indexList = new ArrayList<>(choices.keySet());
                Collections.sort(indexList, (o1, o2) -> o1 - o2);
                for (int i = 0; i < indexList.size(); i++) {
                    Integer index = indexList.get(i);
                    if (choices.containsKey(index)) {
                        descriptionText.append("(").append(index).append(") ").append(choices.get(index)).append("\n");
                    }
                }
                holder.descriptionView.setText(descriptionText.substring(0, descriptionText.lastIndexOf("\n")));
                StringBuilder ans = new StringBuilder();
                if (holder.mItem.getQuestionType() == QuestionType.SingleChoice) {
                    if (holder.mItem.getAnswer().hasSingleChoiceAnswer()) {
                        ans.append("正确选项为： ");
                        ans.append(holder.mItem.getAnswer().getSingleChoiceAnswer().getChoice());
                    } else {
                        ans.append("未提供正确选项");
                    }
                } else {
                    if (holder.mItem.getAnswer().hasMultipleChoiceAnswer()) {
                        MultipleChoiceAnswer multipleChoiceAnswer = holder.mItem.getAnswer().getMultipleChoiceAnswer();
                        if (multipleChoiceAnswer.getChoiceCount() > 0) {
                            ans.append("正确选项为");
                            if (multipleChoiceAnswer.getSelectAllIsCorrect()) {
                                ans.append("(少选或错选不得分)： ");
                            } else {
                                ans.append("(错选不得分，少选得一半分)： ");
                            }
                            ans.append(IUtils.list2String(multipleChoiceAnswer.getChoiceList(), ", "));
                        } else {
                            ans.append("未提供正确选项");
                        }
                    } else {
                        ans.append("未提供正确选项");
                    }
                }
                holder.answerView.setText(ans.toString());
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

        updateScoreView(holder);

        holder.mView.setOnLongClickListener(v -> {
            if (this.mListener != null) {
                return this.mListener.onItemLongClick(holder);
            }
            return false;
        });

        holder.scoreView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onScoreViewClick(holder);
            }
        });

        if (position == opened) {
            holder.answerLayout.setVisibility(View.VISIBLE);
            holder.simpleDescriptionView.setVisibility(View.GONE);
        } else {
            holder.answerLayout.setVisibility(View.GONE);
            holder.simpleDescriptionView.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public int getItemCount() {
        return questionInfos.size();
    }

    public void setScoreDataBus(ScoreDataBus scoreDataBus) {
        this.scoreDataBus = scoreDataBus;
    }

    @SuppressLint("DefaultLocale")
    private void updateScoreView(ViewHolder holder) {
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
                            sb.append(String.format("%.2f", scoreMap.get(index)));
                        } else {
                            sb.append("0.00");
                        }
                        if (i != indexList.size() - 1) {
                            sb.append(", ");
                        }
                    }
                } else {
                    if (!score.hasMultipleScore()) {
                        sb.append(String.format("%.2f", score.getSingleScore()));
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
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final View mView;
        private final TextView indexView;
        private final ImageView autoCheckView;
        private final TextView questionTypeView;
        private final TextView simpleDescriptionView;
        private final TextView descriptionView;
        private final TextView answerView;
        private final TextView scoreView;
        private final View answerLayout;
        private QuestionInfo mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            indexView = view.findViewById(R.id.question_index);
            autoCheckView = view.findViewById(R.id.question_auto_check_image);
            questionTypeView = view.findViewById(R.id.question_type);
            simpleDescriptionView = view.findViewById(R.id.question_simple_description);
            descriptionView = view.findViewById(R.id.question_description);
            answerView = view.findViewById(R.id.question_answer);
            scoreView = view.findViewById(R.id.question_score);
            answerLayout = view.findViewById(R.id.answer_layout);

            mView.setOnClickListener(v -> {
                if (opened == getAdapterPosition()) {
                    opened = -1;
                    notifyItemChanged(getAdapterPosition());
                } else {
                    int oldOpened = opened;
                    opened = getAdapterPosition();
                    notifyItemChanged(oldOpened);
                    notifyItemChanged(opened);
                }
            });

        }

        public QuestionInfo getQuestionInfo() {
            return mItem;
        }

    }

    public interface OnItemInteractionListener {

        boolean onItemLongClick(ViewHolder viewHolder);

        default void onScoreViewClick(ViewHolder viewHolder) {}

    }

    public interface ScoreDataBus {

        Score requireScoreFor(long questionId);

    }

}
