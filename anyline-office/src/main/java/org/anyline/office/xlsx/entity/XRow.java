/*
 * Copyright 2006-2023 www.anyline.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.anyline.office.xlsx.entity;

import org.anyline.util.BasicUtil;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class XRow extends XElement{
    private int index;  //下标 从0开始
    private int r;      //行号从1开始
    private String spans;
    private List<XCol> cols = new ArrayList<>();
    public XRow(XWorkBook book, XSheet sheet, Element src, int index){
        this.book = book;
        this.sheet = sheet;
        this.src = src;
        this.index = index;
        load();
    }
    public void load(){
        if(null == src){
            return;
        }
        //<row r="6" spans="1:6" ht="55.2" customHeight="1">
        this.r = BasicUtil.parseInt(src.attributeValue("r"), index+1);
        this.spans = src.attributeValue("spans");
        List<Element> cols = src.elements("c");
        int index = 0;
        for(Element col:cols){
            this.cols.add(new XCol(book, sheet, this, col, index));
        }
    }
    public static XRow build(XWorkBook book, XSheet sheet, XRow template, List<Object> values){
        if(null == values || values.isEmpty()){
            return null;
        }
        Element datas = sheet.doc().getRootElement().element("sheetData");
        Element row = datas.addElement("row");
        int rows = sheet.rows().size();
        int x = rows + 1;
        int cols = values.size();
        String spans = "1:"+cols;
        row.addAttribute("r",x+"");
        row.addAttribute("spans", spans);
        XRow xr = new XRow(book, sheet, row, x);
        xr.spans = spans;
        xr.r = x;
        int y = 0;
        for(Object value:values){
            XCol tc = template.col(y);
            XCol col = XCol.build(book, sheet, xr, tc, value, x, y++);
            if(null != col){
                xr.add(col);
            }
            cols ++;
        }
        return xr;
    }
    public int r(){
        return r;
    }
    public String spans(){
        return spans;
    }
    public XRow add(XCol col){
        cols.add(col);
        return this;
    }
    public XCol col(int index) {
        if(index < cols.size()){
            return cols.get(index);
        }
        return null;
    }
    public int index(){
        return index;
    }
    public XRow index(int index){
        this.index = index;
        return this;
    }
    /**
     * 解析标签
     * 注意有跨单元格的情况
     */
    public void parseTag(){
        //行解析跨单元格
        //再解析单元格内
        for(XCol col:cols){
            col.parseTag();
        }
    }
    public void replace(boolean parse, LinkedHashMap<String, String> replaces){
        for(XCol col:cols){
            col.replace(parse, replaces);
        }
    }
}
