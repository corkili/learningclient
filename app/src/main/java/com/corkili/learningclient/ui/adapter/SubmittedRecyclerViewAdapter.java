package com.corkili.learningclient.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.IUtils;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedCourseWorkSimpleInfo;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedExamSimpleInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SubmittedRecyclerViewAdapter extends RecyclerView.Adapter<SubmittedRecyclerViewAdapter.ViewHolder> {

    private final Context context;
    private final List<SubmittedCourseWorkSimpleInfo> submittedCourseWorkSimpleInfos;
    private final List<SubmittedExamSimpleInfo> submittedExamSimpleInfos;
    private final OnItemInteractionListener mListener;
    private final boolean useCourseWorkInfos;

    public SubmittedRecyclerViewAdapter(Context context,
                                        List<SubmittedCourseWorkSimpleInfo> submittedCourseWorkSimpleInfos,
                                        OnItemInteractionListener mListener) {
        this.context = context;
        this.submittedCourseWorkSimpleInfos = submittedCourseWorkSimpleInfos;
        this.submittedExamSimpleInfos = new ArrayList<>();
        this.mListener = mListener;
        this.useCourseWorkInfos = true;
    }

    public SubmittedRecyclerViewAdapter(List<SubmittedExamSimpleInfo> submittedExamSimpleInfos, Context context,
                                        OnItemInteractionListener mListener) {
        this.context = context;
        this.submittedCourseWorkSimpleInfos = new ArrayList<>();
        this.submittedExamSimpleInfos = submittedExamSimpleInfos;
        this.mListener = mListener;
        this.useCourseWorkInfos = false;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_submit_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        if (useCourseWorkInfos) {
            holder.submittedCourseWorkSimpleInfo = submittedCourseWorkSimpleInfos.get(position);
            holder.indexView.setText(String.valueOf(position + 1));
            if (holder.submittedCourseWorkSimpleInfo.getAlreadyCheckAllAnswer()) {
                holder.checkView.setText("已批改完成");
            } else {
                holder.checkView.setText("未全部批改");
            }
            holder.submitterView.setText(holder.submittedCourseWorkSimpleInfo.getSubmitterInfo().getUsername());
            holder.timeView.setText(IUtils.DATE_TIME_FORMATTER.format(new Date(holder.submittedCourseWorkSimpleInfo.getUpdateTime())));
        } else {
            holder.submittedExamSimpleInfo = submittedExamSimpleInfos.get(position);
            holder.indexView.setText(String.valueOf(position + 1));
            if (holder.submittedExamSimpleInfo.getAlreadyCheckAllAnswer()) {
                holder.checkView.setText("已批改完成");
            } else {
                holder.checkView.setText("未全部批改");
            }
            holder.submitterView.setText(holder.submittedExamSimpleInfo.getSubmitterInfo().getUsername());
            holder.timeView.setText(IUtils.DATE_TIME_FORMATTER.format(new Date(holder.submittedExamSimpleInfo.getUpdateTime())));
        }

        holder.mView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(holder);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (useCourseWorkInfos) {
            return submittedCourseWorkSimpleInfos.size();
        } else {
            return submittedExamSimpleInfos.size();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final View mView;
        private final TextView indexView;
        private final TextView checkView;
        private final TextView submitterView;
        private final TextView timeView;
        private SubmittedCourseWorkSimpleInfo submittedCourseWorkSimpleInfo;
        private SubmittedExamSimpleInfo submittedExamSimpleInfo;

        ViewHolder(View view) {
            super(view);
            mView = view;
            indexView = view.findViewById(R.id.item_index);
            checkView = view.findViewById(R.id.item_check);
            submitterView = view.findViewById(R.id.item_submitter_name);
            timeView = view.findViewById(R.id.item_time);
        }

        public SubmittedCourseWorkSimpleInfo getSubmittedCourseWorkSimpleInfo() {
            return submittedCourseWorkSimpleInfo;
        }

        public SubmittedExamSimpleInfo getSubmittedExamSimpleInfo() {
            return submittedExamSimpleInfo;
        }
    }

    public interface OnItemInteractionListener {

        void onItemClick(ViewHolder viewHolder);

    }
}

