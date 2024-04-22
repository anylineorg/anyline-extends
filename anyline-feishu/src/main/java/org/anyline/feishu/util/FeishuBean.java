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


package org.anyline.feishu.util;

import org.anyline.entity.DataRow;
import org.anyline.util.BasicUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component("anyline.feishu.load.bean")
public class FeishuBean implements InitializingBean {

    @Value("${anyline.feishu.app:}")
    private String APP_ID			 	; // AppID(应用ID)
    @Value("${anyline.feishu.key:}")
    private String APP_SECRET 			 	; // APPKEY(应用密钥)
    @Value("${anyline.feishu.redirect:}")
    private String OAUTH_REDIRECT_URL 	; // 登录成功回调URL
    @Override
    public void afterPropertiesSet()  {
        APP_ID = BasicUtil.evl(APP_ID, FeishuConfig.DEFAULT_APP_ID);
        if(BasicUtil.isEmpty(APP_ID)){
            return;
        }
        DataRow row = new DataRow();
        row.put("APP_ID", BasicUtil.evl(APP_ID, FeishuConfig.DEFAULT_APP_ID));
        row.put("APP_SECRET", BasicUtil.evl(APP_SECRET, FeishuConfig.DEFAULT_APP_SECRET));
        row.put("OAUTH_REDIRECT_URL", BasicUtil.evl(OAUTH_REDIRECT_URL, FeishuConfig.DEFAULT_OAUTH_REDIRECT_URL));
        FeishuConfig.register(row);
    }

    @Bean("anyline.feishu.init.bean")
    public FeishuUtil instance(){
        return FeishuUtil.getInstance();
    }
}
