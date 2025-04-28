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
import org.anyline.log.Log;
import org.anyline.log.LogProxy;
import org.anyline.net.HttpUtil;
import org.anyline.print.feng.util.FengConfig.URL;
import org.anyline.util.BasicUtil;
import org.anyline.util.BeanUtil;
import org.anyline.util.encrypt.MD5Util;

import java.util.*;

public class FengUtil {
    private static final Log log = LogProxy.get(FengUtil.class);

    private static DataSet accessTokens = new DataSet();
    private FengConfig config = null;

    private static Hashtable<String, FengUtil> instances = new Hashtable<String, FengUtil>();
    public static FengUtil getInstance(){
        return getInstance(FengConfig.DEFAULT_INSTANCE_KEY);
    }
    public FengUtil(FengConfig config){
        this.config = config;
    }
    public FengUtil(String key, DataRow config){
        FengConfig conf = FengConfig.parse(key, config);
        this.config = conf;
        instances.put(key, this);
    }
    public static FengUtil reg(String key, DataRow config){
        FengConfig conf = FengConfig.reg(key, config);
        FengUtil util = new FengUtil(conf);
        instances.put(key, util);
        return util;
    }
    public static FengUtil getInstance(String key){
        if(BasicUtil.isEmpty(key)){
            key = FengConfig.DEFAULT_INSTANCE_KEY;
        }
        FengUtil util = instances.get(key);
        if(null == util){
            FengConfig config = FengConfig.getInstance(key);
            if(null != config) {
                util = new FengUtil(config);
                instances.put(key, util);
            }
        }
        return util;
    }

    public FengConfig getConfig() {
        return config;
    }
    private DataRow api(FengConfig.URL url, Map<String, Object> data){
        DataRow result = null;
        long time = System.currentTimeMillis()/1000;
        String nonce = BasicUtil.getRandomLowerString(6);
        Map<String, Object> params = new HashMap<>();
        String json = BeanUtil.map2json(data);
        String sign = sign(json, nonce);
        params.put("bizData", data);
        params.put("nonce", nonce);
        params.put("appId", config.APP_ID);
        params.put("timestamp",time);
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type","application/json");
        header.put("sign", sign);
        String txt = HttpUtil.post(header,url.getCode(), "UTF-8",params).getText();
        log.info("[invoice api][result:{}]", txt);
        DataRow row = DataRow.parseJson(txt);
        if(row.getInt("code",-1) == 200){
            result = row;
            result.put("success", true);
        }else{
            result = new DataRow();
            result.put("success", false);
            result.put("error",row.getString("desc"));
        }
        return result;
    }

    /**
     * 打印机验证码 调用后打打印出来
     * @param code 打印机imei编号
     */
    public void captcha(String code){
        Map<String, Object> params = new HashMap<>();
        params.put("printerId", code);
        api(URL.CAPTCHA, params);
    }
    /**
     * 自用模式 添加打印机
     * @param code 打印机编号
     * @param captcha 验证码
     * @throws Exception 异常 添加失败时异常
     */
    public String bind(String code, String captcha) throws Exception{
        Map<String, Object> params = new HashMap<>();
        params.put("printerId", code);
        params.put("captcha", captcha);
        DataRow row = api(FengConfig.URL.ADD_PRINTER, params);
        if(!row.getBoolean("success",false)){
            throw new Exception(row.getString("error"));
        }
        return row.getString("data");
    }

    /**
     * 解绑打印机
     * @param code 打印机编号
     * @param share 打印机绑定时返回的编号
     * @throws Exception 异常 添加失败时异常
     * @return  boolean
     */
    public boolean unbind(String code, String share) throws Exception{
        Map<String, Object> params = new HashMap<>();
        params.put("printerId", code);
        params.put("shareCode", share);
        DataRow row = api(FengConfig.URL.DELETE_PRINTER, params);
        if(!row.getBoolean("success",false)){
            throw new Exception(row.getString("error"));
        }
        //0标识解绑失败，1标识解绑成功
        return "1".equals(row.getString("data"));
    }

    /**
     * 文本打印
     * @param machine 打印机
     * @param share 打印机
     * @param template 模板
     * @param data 业务数据
     * @throws Exception 异常 Exception
     * @return DataRow
     */
    public DataRow print(String machine, String share, String template, Map<String, Object> data) throws Exception{
        Map<String, Object> params = new HashMap<>();
        Map<String, Object> biz = new HashMap<>();
        biz.put("templateId", template);
        biz.put("printType", "template");
        biz.put("waybillPrinterData", BeanUtil.map2json(data));

        params.put("printerId", machine);
        params.put("shareCode", share);
        params.put("printData", biz);
        return api(URL.PRINT_TEMPLATE, params);
    }
    /**
     * 签名
     * @param data 业务数据
     * @param nonce 随机字符 6位数字或小写字母
     * @return String
     */
    public String sign(String data, String nonce) {
        String result = null;
        try {
            String md5 = MD5Util.crypto(data+nonce+config.APP_SECRET).toLowerCase();
            result = Base64.getEncoder().encodeToString(md5.getBytes());
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }
}
