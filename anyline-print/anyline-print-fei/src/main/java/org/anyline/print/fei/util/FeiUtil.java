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
import org.anyline.net.HttpUtil;
import org.anyline.print.fei.util.FeiConfig.URL;
import org.anyline.util.BasicUtil;
import org.anyline.util.encrypt.MD5Util;
import org.anyline.log.Log;
import org.anyline.log.LogProxy;
import org.anyline.util.encrypt.SHA1Util;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

public class FeiUtil {
    private static final Log log = LogProxy.get(FeiUtil.class);

    private FeiConfig config = null;

    private static Hashtable<String, FeiUtil> instances = new Hashtable<String, FeiUtil>();
    public static FeiUtil getInstance(){
        return getInstance(FeiConfig.DEFAULT_INSTANCE_KEY);
    }
    public FeiUtil(FeiConfig config){
        this.config = config;
    }
    public FeiUtil(String key, DataRow config){
        FeiConfig conf = FeiConfig.parse(key, config);
        this.config = conf;
        instances.put(key, this);
    }
    public static FeiUtil reg(String key, DataRow config){
        FeiConfig conf = FeiConfig.reg(key, config);
        FeiUtil util = new FeiUtil(conf);
        instances.put(key, util);
        return util;
    }
    public static FeiUtil getInstance(String key){
        if(BasicUtil.isEmpty(key)){
            key = FeiConfig.DEFAULT_INSTANCE_KEY;
        }
        FeiUtil util = instances.get(key);
        if(null == util){
            FeiConfig config = FeiConfig.getInstance(key);
            if(null != config) {
                util = new FeiUtil(config);
                instances.put(key, util);
            }
        }
        return util;
    }

    public FeiConfig getConfig() {
        return config;
    }
    private DataRow api(FeiConfig.API api, Map<String,Object> params){
        DataRow result = null;
        long time = System.currentTimeMillis()/1000;
        params.put("user", config.USER);
        params.put("stime",time);
        params.put("sign", sign(time));
        params.put("apiname",api.getCode());
        Map<String, String> header = new HashMap<String, String>();
        header.put("Content-Type","application/x-www-form-urlencoded");
        String txt = HttpUtil.post(header, FeiConfig.HOST, "UTF-8", params).getText();
        log.info("[invoice api][result:{}]", txt);
        DataRow row = DataRow.parseJson(txt);
        if(row.getInt("error",-1) ==0){
            result = row.getRow("body");
            if(null == result){
                result = new DataRow();
            }
            result.put("success", true);
        }else{
            result = new DataRow();
            result.put("success", false);
            result.put("error",row.getString("error_description"));
        }
        return result;
    }
    /**
     * 自用模式 添加打印机
     * @param code 打印机编号
     * @param secret 打印机密钥
     * @param phone phone
     * @param name name
     * @throws Exception 异常 添加失败时异常
     */
    public void addPrinter(String code, String secret, String phone, String name) throws Exception{
        Map<String,Object> params = new HashMap<String,Object>();
        StringBuilder builder = new StringBuilder();
        builder.append(code).append("#").append(secret).append("#").append(name).append("#").append(phone).append("#1");
        params.put("printerContent", builder.toString());
        DataRow row = api(FeiConfig.API.ADD_PRINTER, params);
        if(row.getInt("ret",0) != 0){
            throw new Exception(row.getString("error"));
        }
    }
    public void addPrinter(String code, String secret) throws Exception{
        addPrinter(code, secret, null, null);
    }
    /**
     * 自用模式 添加打印机
     * @param code 打印机编号
     * @throws Exception 异常 添加失败时异常
     */
    public void deletePrinter(String code) throws Exception{
        Map<String,Object> params = new HashMap<String,Object>();
        StringBuilder builder = new StringBuilder();
        builder.append(code);
        params.put("snlist", builder.toString());
        DataRow row = api(FeiConfig.API.DELETE_PRINTER, params);
        if(row.getInt("ret",0) != 0){
            throw new Exception(row.getString("error"));
        }
    }

    /**
     * 文本打印
     * @param machine machine
     * @param times 打印次数
     * @param text text
     * @throws Exception 异常 Exception
     * @return DataRow
     */
    public DataRow print(String machine, String text, int times) throws Exception{
        Map<String,Object> params = new HashMap<>();
        params.put("content", text);
        params.put("sn", machine);
        params.put("times", times);
        DataRow row = api(FeiConfig.API.PRINT_TEXT, params);
        if(row.getInt("ret",0) != 0){
            throw new Exception(row.getString("error"));
        }
        return row;
    }
    public DataRow print(String machine, String text) throws Exception{
        return print(machine, text, 1);
    }
    //对参数user+UKEY+stime 拼接后（+号表示连接符）进行SHA1加密得到签名，加密后签名值为40位小写字符串
    private String sign(long time){
        String result = SHA1Util.sign(config.USER + config.KEY + time).toLowerCase();
        return result;
    }
}
