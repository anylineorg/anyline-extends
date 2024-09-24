package org.anyline.office.xlsx.entity;

public class XRun {
    private String text;
    private XProperty property;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public XProperty getProperty() {
        return property;
    }

    public void setProperty(XProperty property) {
        this.property = property;
    }
}
