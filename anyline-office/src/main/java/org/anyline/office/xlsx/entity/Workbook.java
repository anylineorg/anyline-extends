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
import org.dom4j.DocumentHelper;

import java.io.File;
import java.util.LinkedHashMap;

public class Workbook {

    private static Log log = LogProxy.get(Workbook.class);
    private File file;
    private String charset = "UTF-8";
    private String xml = null;      // workbook.xml文本
    private org.dom4j.Document doc = null;
    private LinkedHashMap<String, String> txt_replaces = new LinkedHashMap<>();

    LinkedHashMap<String, Sheet> sheets = new LinkedHashMap<>();

    public Workbook(File file){
        this.file = file;
    }

    public Workbook(String file){
        this.file = new File(file);
    }

    private void load(){
        if(null == xml){
            reload();
        }
    }

    public void reload(){
        try {
            xml = ZipUtil.read(file, "word/document.xml", charset);
            doc = DocumentHelper.parseText(xml);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
