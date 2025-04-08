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


package org.anyline.tencent.cos;
import com.tencent.cloud.Policy;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.utils.Jackson;
import com.tencent.cloud.CosStsClient;
import com.tencent.cloud.Statement;
import org.anyline.log.Log;
import org.anyline.log.LogProxy;
import org.anyline.util.AnylineConfig;
import org.anyline.util.BasicUtil;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

public class COSUtil {
    private static final Log log = LogProxy.get(COSUtil.class);
     private COSConfig config = null;
    private static Hashtable<String, COSUtil> instances = new Hashtable<>();

    static {
        Hashtable<String, AnylineConfig> configs = COSConfig.getInstances();
        for(String key:configs.keySet()){
            instances.put(key, getInstance(key));
        }
    }
    public static Hashtable<String, COSUtil> getInstances(){
        return instances;
    }

    public COSUtil(){}
    public COSUtil(String endpoint, String bucket, String id, String key){
        COSConfig config = new COSConfig();
        config.ENDPOINT = endpoint;
        config.SECRET_ID = id;
        config.SECRET_KEY = key;
        config.BUCKET = bucket;
        this.config = config;

     }

    public static COSUtil getInstance() {
        return getInstance(COSConfig.DEFAULT_INSTANCE_KEY);
    }

    public COSConfig getConfig(){
        return config;
    }
    public void setConfig(COSConfig config){
        this.config = config;
    }
    @SuppressWarnings("deprecation")
    public static COSUtil getInstance(String key) {
        if (BasicUtil.isEmpty(key)) {
            key = COSConfig.DEFAULT_INSTANCE_KEY;
        }
        COSUtil util = instances.get(key);
        if (null == util) {
            COSConfig config = COSConfig.getInstance(key);
            if(null != config) {
                util = new COSUtil();
                util.config = config;
                instances.put(key, util);
            }
        }
        return util;
    }
    public Map<String, String> signature(String dir, int second) {
        TreeMap<String, Object> config = new TreeMap<>();
        config.put("secretId", this.config.SECRET_ID);
        config.put("secretKey", this.config.SECRET_KEY);

        // 初始化 policy
        Policy policy = new Policy();
        // 设置域名:
        // 如果您使用了腾讯云 cvm，可以设置内部域名
        //config.put("host", "sts.internal.tencentcloudapi.com");

        if(second == 0){
            second = this.config.EXPIRE_SECOND;
        }
        // 临时密钥有效时长，单位是秒，默认 1800 秒，目前主账号最长 2 小时（即 7200 秒），子账号最长 36 小时（即 129600）秒
        config.put("durationSeconds", second);
        // 换成您的 bucket
        config.put("bucket", this.config.BUCKET);
        // 换成 bucket 所在地区
        config.put("region", this.config.ENDPOINT);

        // 开始构建一条 statement
        Statement statement = new Statement();
        // 声明设置的结果是允许操作
        statement.setEffect("allow");
        /**
         * 密钥的权限列表。必须在这里指定本次临时密钥所需要的权限。
         * 权限列表请参见 https://cloud.tencent.com/document/product/436/31923
         * 规则为 {project}:{interfaceName}
         * project : 产品缩写  cos相关授权为值为cos,数据万象(数据处理)相关授权值为ci
         * 授权所有接口用*表示，例如 cos:*,ci:*
         * 添加一批操作权限 :
         */
        statement.addActions(new String[]{
                "cos:PutObject",
                // 表单上传、小程序上传
                "cos:PostObject",
                // 分块上传
                "cos:InitiateMultipartUpload",
                "cos:ListMultipartUploads",
                "cos:ListParts",
                "cos:UploadPart",
                "cos:CompleteMultipartUpload",
                // 处理相关接口一般为数据万象产品 权限中以ci开头
                // 创建媒体处理任务
                "ci:CreateMediaJobs",
                // 文件压缩
                "ci:CreateFileProcessJobs"
        });

        /**
         * 这里改成允许的路径前缀，可以根据自己网站的用户登录态判断允许上传的具体路径
         * 资源表达式规则分对象存储(cos)和数据万象(ci)两种
         * 数据处理、审核相关接口需要授予ci资源权限
         *  cos : qcs::cos:{region}:uid/{appid}:{bucket}/{path}
         *  ci  : qcs::ci:{region}:uid/{appid}:bucket/{bucket}/{path}
         * 列举几种典型的{path}授权场景：
         * 1、允许访问所有对象："*"
         * 2、允许访问指定的对象："a/a1.txt", "b/b1.txt"
         * 3、允许访问指定前缀的对象："a*", "a/*", "b/*"
         *  如果填写了“*”，将允许用户访问所有资源；除非业务需要，否则请按照最小权限原则授予用户相应的访问权限范围。
         *
         * 示例：授权examplebucket-1250000000 bucket目录下的所有资源给cos和ci 授权两条Resource
         */
        statement.addResources(new String[]{"*"});

        // 把一条 statement 添加到 policy
        // 可以添加多条
        policy.addStatement(statement);
        // 将 Policy 示例转化成 String，可以使用任何 json 转化方式，这里是本 SDK 自带的推荐方式
        config.put("policy", Jackson.toJsonPrettyString(policy));
        Map<String, String> result = new HashMap<String, String>();
        try {

            com.tencent.cloud.Response response = CosStsClient.getCredential(config);
            System.out.println(response.credentials.tmpSecretId);
            System.out.println(response.credentials.tmpSecretKey);
            System.out.println(response.credentials.sessionToken);

            result.put("SECRET_ID", response.credentials.tmpSecretId);
            result.put("SECRET_KEY", response.credentials.tmpSecretKey);
            result.put("SESSION_TOKEN", response.credentials.sessionToken);
            result.put("EXPIRE", String.valueOf((System.currentTimeMillis() + second * 1000)/1000));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}