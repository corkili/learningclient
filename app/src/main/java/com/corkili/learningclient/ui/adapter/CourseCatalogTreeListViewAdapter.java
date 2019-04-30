package com.corkili.learningclient.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.CourseCatalogItemBean;
import com.corkili.learningclient.generate.protobuf.Info.CourseCatalogItemInfo;
import com.zhy.tree.bean.Node;
import com.zhy.tree.bean.TreeListViewAdapter;

import java.util.List;
import java.util.Map;

public class CourseCatalogTreeListViewAdapter extends TreeListViewAdapter<CourseCatalogItemBean> {

    private Map<Integer, CourseCatalogItemInfo> dataMap;

    public CourseCatalogTreeListViewAdapter(ListView mTree, Context context,
                                            List<CourseCatalogItemBean> datas,
                                            int defaultExpandLevel, Map<Integer, CourseCatalogItemInfo> dataMap)
            throws IllegalArgumentException, IllegalAccessException {
        super(mTree, context, datas, defaultExpandLevel);
        this.dataMap = dataMap;
    }

    @Override
    public View getConvertView(Node node, int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.activity_course_catalog_item, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        CourseCatalogItemInfo courseCatalogItemInfo = dataMap.get(node.getId());

        viewHolder.titleView.setText(node.getName());

        if (node.isLeaf()) {
            viewHolder.iconView.setVisibility(View.INVISIBLE);
        } else {
            viewHolder.iconView.setVisibility(View.VISIBLE);
            if (node.isExpand()) {
                viewHolder.iconView.setImageResource(R.drawable.ic_list_expand_24dp);
            } else {
                viewHolder.iconView.setImageResource(R.drawable.ic_list_no_expand_24dp);
            }
        }

        if (node.isLeaf()) {
            viewHolder.stateView.setVisibility(View.VISIBLE);
        } else {
            viewHolder.stateView.setVisibility(View.GONE);
        }

        if (courseCatalogItemInfo != null) {
            if (node.isLeaf() && !courseCatalogItemInfo.getSelectable()) {
                viewHolder.titleView.setTextColor(mContext.getResources().getColor(R.color.qmui_config_color_gray_5));
            } else {
                viewHolder.titleView.setTextColor(mContext.getResources().getColor(R.color.qmui_config_color_black));
            }
            if ("completed".equals(courseCatalogItemInfo.getCompletionStatus())) {
                viewHolder.stateView.setImageResource(R.drawable.ic_circle_finished_24dp);
            } else if ("incomplete".equals(courseCatalogItemInfo.getCompletionStatus())) {
                viewHolder.stateView.setImageResource(R.drawable.ic_circle_not_finish_24dp);
            } else {
                viewHolder.stateView.setImageResource(R.drawable.ic_circle_24dp);
            }
        }

        return convertView;
    }

    private final class ViewHolder {

        private View mView;
        private ImageView iconView;
        private TextView titleView;
        private ImageView stateView;

        public ViewHolder(View view) {
            this.mView = view;
            iconView = view.findViewById(R.id.icon);
            titleView = view.findViewById(R.id.catalog_item_title);
            stateView = view.findViewById(R.id.catalog_item_state);
        }
    }
}
