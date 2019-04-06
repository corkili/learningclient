package com.corkili.learningclient.ui.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.corkili.learningclient.R;
import com.corkili.learningclient.common.CourseCatalogItemBean;
import com.zhy.tree.bean.Node;
import com.zhy.tree.bean.TreeListViewAdapter;

import java.util.List;

public class CourseCatalogTreeListViewAdapter extends TreeListViewAdapter<CourseCatalogItemBean> {

    public CourseCatalogTreeListViewAdapter(ListView mTree, Context context,
                                            List<CourseCatalogItemBean> datas,
                                            int defaultExpandLevel) throws IllegalArgumentException, IllegalAccessException {
        super(mTree, context, datas, defaultExpandLevel);
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

        viewHolder.titleView.setText(node.getName());

        return convertView;
    }

    private final class ViewHolder {

        private View mView;
        private TextView titleView;

        public ViewHolder(View view) {
            this.mView = view;
            titleView = view.findViewById(R.id.catalog_item_title);
        }
    }
}
