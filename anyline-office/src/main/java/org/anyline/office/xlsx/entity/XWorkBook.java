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
import org.anyline.util.BeanUtil;
import org.anyline.util.ZipUtil;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XWorkBook {

    private static Log log = LogProxy.get(XWorkBook.class);
    private File file;
    private String charset = "UTF-8";
    private String xml = null;      // workbook.xml文本
    private org.dom4j.Document doc = null;
    private LinkedHashMap<String, String> replaces = new LinkedHashMap<>();
    /**
     * 文本原样替换，不解析原文没有${}的也不要添加
     */
    private LinkedHashMap<String, String> txt_replaces = new LinkedHashMap<>();
    private boolean autoMergePlaceholder = true;
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


    /**
     * 解析标签
     */
    public void parseTag(){
        for(XSheet sheet:sheets.values()){
            sheet.parseTag();
        }
    }
    /**
     * 设置占位符替换值 在调用save时执行替换<br/>
     * 注意如果不解析的话 不会添加自动${}符号 按原文替换,是替换整个文件的纯文件，包括标签名在内
     * @param parse 是否解析标签 true:解析HTML标签 false:直接替换文本
     * @param key 占位符
     * @param content 替换值
     */
    public void replace(boolean parse, String key, String content){
        if(null == key && key.trim().length()==0){
            return;
        }
        if(parse) {
            replaces.put(key, content);
        }else{
            txt_replaces.put(key, content);
        }
    }
    public void replace(String key, String content){
        replace(true, key, content);
    }
    public void replace(boolean parse, String key, File ... words){
        replace(parse, key, BeanUtil.array2list(words));
    }
    public void replace(String key, File ... words){
        replace(true, key, BeanUtil.array2list(words));
    }
    public void replace(boolean parse, String key, List<File> words){
        if(null != words) {
            StringBuilder content = new StringBuilder();
            for(File word:words) {
                content.append("<word>").append(word.getAbsolutePath()).append("</word>");
            }
            if(parse) {
                replaces.put(key, content.toString());
            }else{
                txt_replaces.put(key, content.toString());
            }
        }
    }

    public void replace(String key, List<File> words){
        replace(true, key, words);
    }
    public void save(){
        save(Charset.forName("UTF-8"));
    }
    public void save(Charset charset){
        try {
            //加载文件
            load();
            if(autoMergePlaceholder){
                mergePlaceholder();
            }
            for(XSheet sheet:sheets.values()){
                sheet.replace(replaces);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    //直接替换文本不解析
    public String replace(String text, Map<String, String> replaces){
        if(null != text){
            for(String key:replaces.keySet()){
                String value = replaces.get(key);
                //原文没有${}的也不要添加
                text = text.replace(key, value);
            }
        }
        return text;
    }

    /**
     * 合并点位符 ${key} 拆分到3个t中的情况
     * 调用完replace后再调用当前方法，因为需要用到replace里提供的占位符列表
     */
    public void mergePlaceholder(){
        List<String> placeholders = new ArrayList<>();
        placeholders.addAll(replaces.keySet());
        mergePlaceholder(placeholders);
    }
    /**
     * 合并点位符 ${key} 拆分到3个t中的情况
     * @param placeholders 占位符列表 带不还${}都可以 最终会处理掉${}
     */
    public void mergePlaceholder(List<String> placeholders){
    }
    public void mergePlaceholder(Element box, List<String> placeholders){
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
