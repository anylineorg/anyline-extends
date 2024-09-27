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

import org.dom4j.Element;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class XRow extends XElement{
    private int index;
    private List<XCol> cols = new ArrayList<>();
    public XRow(XWorkBook book, XSheet sheet, Element src, int index){
        this.book = book;
        this.sheet = sheet;
        this.src = src;
        this.index = index;
    }
    public void load(){
        if(null == src){
            return;
        }
        List<Element> cols = src.elements("c");
        int index = 0;
        for(Element col:cols){
            this.cols.add(new XCol(book, sheet, this, col, index));
        }
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
