package com.corkili.learningclient.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.ProtoUtils;
import com.corkili.learningclient.generate.protobuf.Info.QuestionSimpleInfo;

import java.util.List;


public class QuestionRecyclerViewAdapter extends RecyclerView.Adapter<QuestionRecyclerViewAdapter.ViewHolder> {

    private Context context;
    private final List<QuestionSimpleInfo> questionSimpleInfos;
    private OnItemInteractionListener mListener;

    public QuestionRecyclerViewAdapter(Context context, List<QuestionSimpleInfo> questionSimpleInfos,
                                       OnItemInteractionListener mListener) {
        this.context = context;
        this.questionSimpleInfos = questionSimpleInfos;
        this.mListener = mListener;
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
        holder.mItem = questionSimpleInfos.get(position);
        holder.indexView.setText(String.valueOf(position + 1));
        if (holder.mItem.getAutoCheck()) {
            holder.autoCheckView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_yes_bk_green));
        } else {
            holder.autoCheckView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_no_bk_red));
        }
        holder.questionTypeView.setText(ProtoUtils.getQuestionTypeUIName(holder.mItem.getQuestionType()));
        holder.descriptionView.setText(holder.mItem.getQuestion());

        holder.mView.setOnClickListener(v -> {
            if (this.mListener != null) {
                this.mListener.onItemClick(holder);
            }
        });

    }

    @Override
    public int getItemCount() {
        return questionSimpleInfos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final View mView;
        private final TextView indexView;
        private final ImageView autoCheckView;
        private final TextView questionTypeView;
        private final TextView descriptionView;
        private QuestionSimpleInfo mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            indexView = view.findViewById(R.id.question_index);
            autoCheckView = view.findViewById(R.id.question_auto_check_image);
            questionTypeView = view.findViewById(R.id.question_type);
            descriptionView = view.findViewById(R.id.question_description);
        }

        public QuestionSimpleInfo getQuestionSimpleInfo() {
            return mItem;
        }
    }

    public interface OnItemInteractionListener {

        void onItemClick(ViewHolder viewHolder);

    }
}
