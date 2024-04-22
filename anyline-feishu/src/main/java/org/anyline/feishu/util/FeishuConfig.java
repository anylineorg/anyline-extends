package org.anyline.feishu.util;

import org.anyline.entity.DataRow;
import org.anyline.util.AnylineConfig;
import org.anyline.util.BasicUtil;
import org.anyline.util.ConfigTable;

import java.util.Hashtable;

public class FeishuConfig extends AnylineConfig {
    public static String CONFIG_NAME = "anyline-qq-mp.xml";
    private static Hashtable<String,AnylineConfig> instances = new Hashtable<>();

    public static String DEFAULT_APP_ID = ""				; // AppID(应用ID)
    public static String DEFAULT_APP_SECRET = ""				; // APPKEY(应用密钥)
    public static String DEFAULT_OAUTH_REDIRECT_URL		; // 登录成功回调URL

    /**
     * 服务号相关信息
     */
    public String APP_ID			    = DEFAULT_APP_ID				; // AppID(应用ID)
    public String APP_SECRET 			= DEFAULT_APP_SECRET				; // APPKEY(应用密钥)
    public String OAUTH_REDIRECT_URL    = DEFAULT_OAUTH_REDIRECT_URL	; // 登录成功回调URL

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
        parse(FeishuConfig.class, content, instances ,compatibles);
    }
    /**
     * 初始化默认配置文件
     */
    public static void init() {
        // 加载配置文件
        load();
    }

    public static FeishuConfig getInstance(){
        return getInstance(DEFAULT_INSTANCE_KEY);
    }
    public static FeishuConfig getInstance(String key){
        if(BasicUtil.isEmpty(key)){
            key = DEFAULT_INSTANCE_KEY;
        }

        if(ConfigTable.getReload() > 0 && (System.currentTimeMillis() - FeishuConfig.lastLoadTime)/1000 > ConfigTable.getReload() ){
            // 重新加载
            load();
        }
        return (FeishuConfig)instances.get(key);
    }
    /**
     * 加载配置文件
     * 首先加载anyline-config.xml
     * 然后加载anyline开头的xml文件并覆盖先加载的配置
     */
    private synchronized static void load() {
        load(instances, FeishuConfig.class, CONFIG_NAME);
        FeishuConfig.lastLoadTime = System.currentTimeMillis();
    }
    private static void debug(){
    }
    public static FeishuConfig register(String instance, DataRow row){
        FeishuConfig config = parse(FeishuConfig.class, instance, row, instances, compatibles);
        FeishuUtil.getInstance(instance);
        return config;
    }
    public static FeishuConfig register(DataRow row){
        return register(DEFAULT_INSTANCE_KEY, row);
    }
    public static FeishuConfig register(String instance,  String app, String secret, String redirect){
        DataRow row = new DataRow();
        row.put("APP_ID", app);
        row.put("APP_SECRET", secret);
        row.put("OAUTH_REDIRECT_URL", redirect);
        return register(instance, row);
    }
    public static FeishuConfig register(String app, String secret, String redirect){
        return register(DEFAULT_INSTANCE_KEY, app, secret, redirect);
    }
    public static FeishuConfig register(String app, String secret){
        return register(DEFAULT_INSTANCE_KEY, app, secret, null);
    }
}
