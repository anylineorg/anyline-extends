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

import org.dom4j.Document;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class XSheet {
    private XWorkBook book;
    private Document doc;
    private Element root;
    private String name;
    private List<XRow> rows = new ArrayList<>();
    public XSheet(){}
    public XSheet(XWorkBook book, Document doc, String name){
        this.name = name;
        this.book = book;
        this.doc = doc;
        load();
    }
    public void load(){
        if(null == doc){
            return;
        }
        root = doc.getRootElement();
        Element data = root.element("sheetData");
        if(null == data){
            return;
        }
        List<Element> rows = data.elements("row");
        int index = 0;
        for(Element row:rows){
            XRow xr = new XRow(book, this, row, index++);
            this.rows.add(xr);
        }
    }
    public XWorkBook book(){
        return book;
    }
    public XSheet book(XWorkBook book){
        this.book = book;
        return this;
    }
    public Document doc(){
        return doc;
    }
    public XSheet doc(Document doc){
        this.doc = doc;
        return this;
    }
    public String name(){
        return name;
    }
    public XSheet doc(String name){
        this.name = name;
        return this;
    }
    public List<XRow> rows(){
        return rows;
    }
    /**
     * 解析标签
     * 注意有跨行的情况
     */
    public void parseTag(){
        //行解析跨行
        //再解析行内
        for(XRow row:rows){
            row.parseTag();
        }
    }

    /**
     * 追加行
     * @param values
     * @return XRow
     */
    public XRow append(List<Object> values){
        int size = values.size();
        if(size == 0){
            return null;
        }
        XRow template = rows.get(rows.size()-1);
        XRow row = XRow.build(book, this, template, values);
        rows.add(row);
        return row;
    }
    public void replace(boolean parse, LinkedHashMap<String, String> replaces){
        for(XRow row:rows){
            row.replace(parse, replaces);
        }
    }


}
