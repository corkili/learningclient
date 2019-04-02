package com.corkili.learningclient.common;

import com.zhy.tree.bean.TreeNodeId;
import com.zhy.tree.bean.TreeNodeLabel;
import com.zhy.tree.bean.TreeNodePid;

public class CourseCatalogItemBean {

    @TreeNodeId
    private int id;

    @TreeNodePid
    private int parentId;

    @TreeNodeLabel
    private String title;

    public CourseCatalogItemBean(int id, int parentId, String title) {
        this.id = id;
        this.parentId = parentId;
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public int getParentId() {
        return parentId;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CourseCatalogItemBean that = (CourseCatalogItemBean) o;

        return id == that.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
