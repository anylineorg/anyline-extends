package org.anyline.office.xlsx.entity;

import org.dom4j.Element;

public class XProperty {
    private Element src;
    public XProperty(Element src) {
        this.src = src;
    }
}
/*
			<rPr>
				<sz val="11"/>
				<color rgb="FFF8F8F8"/>
				<rFont val="宋体"/>
				<family val="3"/>
				<charset val="134"/>
			</rPr>*/