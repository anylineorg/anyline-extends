package org.anyline.office.docx.entity;

public class Spans {
    /**
     * -1:未设置  1:不合并  0:被合并  >1:合并其他单元格
     */
    private int colspan = -1;
    private int rowspan = -1;

    public int getColspan() {
        return colspan;
    }

    public void setColspan(int colspan) {
        this.colspan = colspan;
    }

    public int getRowspan() {
        return rowspan;
    }

    public void setRowspan(int rowspan) {
        this.rowspan = rowspan;
    }
    public void addRowspan(int rowspan) {
        this.rowspan += rowspan;
    }
}
