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
import java.util.List;

public class XSheet {
    private Document doc;
    private Element root;
    private List<XRow> rows = new ArrayList<>();
    public XSheet(){}
    public XSheet(Document doc){
        this.doc = doc;
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
        for(Element row:rows){
            XRow xr = new XRow(row);
            this.rows.add(xr);
        }
    }
}
