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


package org.anyline.print.fei.util;

import org.anyline.entity.DataRow;
import org.anyline.entity.DataSet;
import org.anyline.util.AnylineConfig;
import org.anyline.util.BasicUtil;
import org.anyline.util.ConfigTable;

import java.io.File;
import java.util.Hashtable;

public class FeiConfig  extends AnylineConfig {
    private static Hashtable<String, AnylineConfig> instances = new Hashtable<>();


    public static String DEFAULT_USER                = "" ;
    public static String DEFAULT_KEY            = "" ;
    public static String DEFAULT_TYPE                  = "0"; // 0:自用 1:开放


    public String USER                = DEFAULT_USER                ;
    public String KEY                 = DEFAULT_KEY            ;
    public String TYPE                  = DEFAULT_TYPE                  ; // 0:自用 1:开放

    private static File configDir;
    public static String CONFIG_NAME = "anyline-print-fei.xml";
    public static final String HOST = "https://api.feieyun.cn/Api/Open/";

    public static enum API{
        ADD_PRINTER	        {public String getCode(){return "Open_printerAddlist";}    public String getName(){return "添加自用打印机";}},
        DELETE_PRINTER	    {public String getCode(){return "Open_printerDelList";}    public String getName(){return "删除自用打印机";}},
        PRINT_TEXT	        {public String getCode(){return "Open_printMsg";}    public String getName(){return "打印文本";}};
        public abstract String getName();
        public abstract String getCode();
    };

    public static Hashtable<String,AnylineConfig>getInstances(){
        return instances;
    }
    static{
        init();
        debug();
    }
    /**
     * 解析配置文件内容
     * @param content 配置文件内容
     */
    public static void parse(String content){
        parse(FeiConfig.class, content, instances ,compatibles);
    }
    /**
     * 初始化默认配置文件
     */
    public static void init() {
        // 加载配置文件
        load();
    }
    public static FeiConfig getInstance(){
        return getInstance(DEFAULT_INSTANCE_KEY);
    }
    public static FeiConfig getInstance(String key){
        if(BasicUtil.isEmpty(key)){
            key = DEFAULT_INSTANCE_KEY;
        }

        if(ConfigTable.getReload() > 0 && (System.currentTimeMillis() - FeiConfig.lastLoadTime)/1000 > ConfigTable.getReload() ){
            // 重新加载
            load();
        }
        return (FeiConfig)instances.get(key);
    }

    public static FeiConfig reg(String key, DataRow row){
        return parse(FeiConfig.class, key, row, instances,compatibles);
    }
    public static FeiConfig parse(String key, DataRow row){
        return parse(FeiConfig.class, key, row, instances,compatibles);
    }
    public static Hashtable<String,AnylineConfig> parse(String column, DataSet set){
        for(DataRow row:set){
            String key = row.getString(column);
            parse(key, row);
        }
        return instances;
    }
    /**
     * 加载配置文件
     * 首先加载anyline-config.xml
     * 然后加载anyline开头的xml文件并覆盖先加载的配置
     */
    private synchronized static void load() {
        load(instances, FeiConfig.class,CONFIG_NAME ,compatibles);
        FeiConfig.lastLoadTime = System.currentTimeMillis();
    }
    private static void debug(){
    }
    public FeiConfig register(String instance, DataRow row){
        FeiConfig config = parse(FeiConfig.class, instance, row, instances, compatibles);
        return config;
    }
    public FeiConfig register(DataRow row){
        return register(DEFAULT_INSTANCE_KEY, row);
    }
    public FeiConfig register(String user, String key){
        DataRow row = new DataRow();
        row.put("USER", user);
        row.put("KEY",key);
        return register(row);
    }
}
