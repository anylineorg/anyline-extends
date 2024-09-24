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

import org.anyline.log.Log;
import org.anyline.log.LogProxy;
import org.anyline.util.ZipUtil;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class XWorkBook {

    private static Log log = LogProxy.get(XWorkBook.class);
    private File file;
    private String charset = "UTF-8";
    private String xml = null;      // workbook.xml文本
    private org.dom4j.Document doc = null;
    private LinkedHashMap<String, String> txt_replaces = new LinkedHashMap<>();
    private List<ShareString> shares = new ArrayList<>();
    LinkedHashMap<String, XSheet> sheets = new LinkedHashMap<>();

    public XWorkBook(File file){
        this.file = file;
    }

    public XWorkBook(String file){
        this.file = new File(file);
    }

    private void load(){
        if(null == xml){
            reload();
        }
    }

    public void reload(){
        try {
            xml = ZipUtil.read(file, "xl/workbook.xml", charset);
            doc = DocumentHelper.parseText(xml);
            List<String> items = ZipUtil.getEntriesNames(file);
            String shares = ZipUtil.read(file, "xl/sharedStrings.xml", charset);
            shares(shares);
            for(String item:items){
                if(item.contains("xl/worksheets")){
                    String name = item.replace("xl/worksheets", "").replace(".xml", "");
                    Document doc = DocumentHelper.parseText(ZipUtil.read(file, item, charset));
                    XSheet sheet = new XSheet(doc);
                    sheets.put(name, sheet);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void shares(String xml) throws Exception{
       Document doc = DocumentHelper.parseText(xml);
       Element root = doc.getRootElement();
       List<Element> list = root.elements();
       for(Element item:list){
           List<Element> runs = item.elements("r");
           for(Element run:runs){
               XRun xr = new XRun();
               Element property = run.element("rPr");
               if(null != property){
                   XProperty xp = new XProperty(property);
                   xr.setProperty(xp);
               }
               Element text = run.element("t");
               if(null != text){
                   xr.setText(text.getTextTrim());
               }
           }
       }
    }
}
