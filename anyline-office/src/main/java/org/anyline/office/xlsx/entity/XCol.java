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

import org.anyline.office.docx.util.DocxUtil;
import org.dom4j.Element;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 参考 https://learn.microsoft.com/en-us/dotnet/api/documentformat.openxml.spreadsheet.cell?view=openxml-3.0.1
 * 说明 http://office.anyline.org/v/86_14141
 */
public class XCol extends XElement{
    private XRow row;
    private String type; //t属性
    private String style; //s属性
    private String value; //ShareString.id或text t="s"时 value=ShareString
    private String text; //最终文本
    private String formula;
    private int index;

    public XCol(XWorkBook book, XSheet sheet, XRow row, Element src, int index){
        this.book = book;
        this.sheet = sheet;
        this.row = row;
        this.src = src;
        this.index = index;
    }
    public void load(){
        if(null == src){
            return;
        }
        type = src.attributeValue("s");
    }

    public int index(){
        return index;
    }
    public XCol index(int index){
        this.index = index;
        return this;
    }
    /**
     * 解析标签
     */
    public void parseTag(){
    }
    public void replace(boolean parse, LinkedHashMap<String, String> replaces){
        if("s".equals(type)){
            //文本类型
            int idx = Integer.parseInt(value);
            ShareString ss = book.share(idx);
            String txt = ss.text();
        }
    }

    public static String replace(boolean parse, String src, Map<String, String> replaces){
        String txt = src;
        List<String> flags = DocxUtil.splitKey(txt);
        if(flags.size() == 0){
            return src;
        }
        Collections.reverse(flags);
        boolean exists = false;
        for(int i=0; i<flags.size(); i++){
            String flag = flags.get(i);
            String content = flag;
            String key = null;
            if(flag.startsWith("${") && flag.endsWith("}")) {
                key = flag.substring(2, flag.length() - 1);
                content = replaces.get(key);
                exists = exists || replaces.containsKey(key);
                if(null == content){
                    exists =  exists || replaces.containsKey(flag);
                    content = replaces.get(flag);
                }
            }else if(flag.startsWith("{") && flag.endsWith("}")){
                key = flag.substring(1, flag.length() - 1);
                content = replaces.get(key);
                exists =  exists || replaces.containsKey(key);
                if(null == content){
                    content = replaces.get(flag);
                    exists = exists || replaces.containsKey(flag);
                }
            }else{
                content = replaces.get(flag);
                exists =  exists || replaces.containsKey(flag);
            }
            txt = txt.replace(flag, content);
        }
        return txt;
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