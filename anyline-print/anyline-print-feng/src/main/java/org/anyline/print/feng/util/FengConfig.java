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


package org.anyline.print.feng.util;

import org.anyline.entity.DataRow;
import org.anyline.entity.DataSet;
import org.anyline.util.AnylineConfig;
import org.anyline.util.BasicUtil;
import org.anyline.util.ConfigTable;

import java.io.File;
import java.util.Hashtable;

public class FengConfig extends AnylineConfig {
    private static Hashtable<String, AnylineConfig> instances = new Hashtable<>();


    public static String DEFAULT_APP_ID                = "" ;
    public static String DEFAULT_APP_SECRET            = "" ;
    public static String DEFAULT_TYPE                  = "0"; // 0:自用 1:开放
    public static String DEFAULT_ACCESS_TOKEN_SERVER   = "" ;



    public String APP_ID                = DEFAULT_APP_ID                ;
    public String APP_SECRET            = DEFAULT_APP_SECRET            ;
    public String TYPE                  = DEFAULT_TYPE                  ; // 0:自用 1:开放
    public String ACCESS_TOKEN_SERVER   = DEFAULT_ACCESS_TOKEN_SERVER   ;


    private static File configDir;
    public static String CONFIG_NAME = "anyline-feng.xml";

    public static enum URL{
        CAPTCHA             {public String getCode(){return "/mk/api/print/captcha";} 	        public String getName(){return "验证码";}},
        ACCESS_TOKEN		{public String getCode(){return "https://open-api.10ss.net/oauth/oauth";} 	        public String getName(){return "ACCESS TOKEN";}},
        ADD_PRINTER	        {public String getCode(){return "/mk/api/printer/bind";}    public String getName(){return "绑定打印机";}},
        DELETE_PRINTER	    {public String getCode(){return "https://open-api.10ss.net/printer/deleteprinter";}    public String getName(){return "删除自用打印机";}},
        PRINT_TEMPLATE      {public String getCode(){return "/mk/api/print";}    public String getName(){return "模板打印";}};
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
        parse(FengConfig.class, content, instances ,compatibles);
    }
    /**
     * 初始化默认配置文件
     */
    public static void init() {
        // 加载配置文件
        load();
    }
    public static FengConfig getInstance(){
        return getInstance(DEFAULT_INSTANCE_KEY);
    }
    public static FengConfig getInstance(String key){
        if(BasicUtil.isEmpty(key)){
            key = DEFAULT_INSTANCE_KEY;
        }

        if(ConfigTable.getReload() > 0 && (System.currentTimeMillis() - FengConfig.lastLoadTime)/1000 > ConfigTable.getReload() ){
            // 重新加载
            load();
        }
        return (FengConfig)instances.get(key);
    }

    public static FengConfig reg(String key, DataRow row){
        return parse(FengConfig.class, key, row, instances,compatibles);
    }
    public static FengConfig parse(String key, DataRow row){
        return parse(FengConfig.class, key, row, instances,compatibles);
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
        load(instances, FengConfig.class,CONFIG_NAME ,compatibles);
        FengConfig.lastLoadTime = System.currentTimeMillis();
    }
    private static void debug(){
    }
    public FengConfig register(String instance, DataRow row){
        FengConfig config = parse(FengConfig.class, instance, row, instances, compatibles);
        return config;
    }
    public FengConfig register(DataRow row){
        return register(DEFAULT_INSTANCE_KEY, row);
    }
    public FengConfig register(String app, String secret){
        DataRow row = new DataRow();
        row.put("APP_ID", app);
        row.put("APP_SECRET",secret);
        return register(row);
    }
}
