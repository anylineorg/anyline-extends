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

import java.util.LinkedHashMap;

/**
 * 参考 https://learn.microsoft.com/en-us/dotnet/api/documentformat.openxml.spreadsheet.cell?view=openxml-3.0.1
 * 说明 http://office.anyline.org/v/86_14141
 */
public class XCol {
    private Element src;
    private XRow row;
    private String type; //t属性
    private String style; //s属性
    private String value; //ShareString.id或text t="s"时 value=ShareString
    private String text; //最终文本
    private String formula;
    private int index;

    public XCol(XRow row, Element src){
        this.row = row;
        this.src = src;
    }
    public void load(){
        if(null == src){
            return;
        }
        type = src.attributeValue("s");
    }

    /**
     * 解析标签
     */
    public void parseTag(){
    }
    public void replace(LinkedHashMap<String, String> replaces){

    }
}
/*
<c r="A1" s="20" t="s">
    <v>48</v>
</c>
<c r="C6" s="1" vm="15">
  <f>CUBEVALUE("xlextdat9 Adventure Works",C$5,$A6)</f>
  <v>2838512.355</v>
</c>

<row r="1" spans="1:1">
  <c r="A1" t="inlineStr">
    <is><t>This is inline string example</t></is>
  </c>
</row>

* */