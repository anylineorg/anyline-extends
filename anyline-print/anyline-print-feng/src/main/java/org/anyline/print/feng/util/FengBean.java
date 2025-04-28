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
import org.anyline.util.BasicUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component("anyline.feng.load.bean")
public class FengBean implements InitializingBean {

    @Value("${anyline.feng.app:}")
    public String APP_ID                   ;
    @Value("${anyline.feng.secret:}")
    public String APP_SECRET               ;
    @Value("${anyline.feng.app:type:}")
    public String TYPE                     ; // 0:自用 1:开放
    @Value("${anyline.feng.server:}")
    public String ACCESS_TOKEN_SERVER      ;


    @Override
    public void afterPropertiesSet()  {
        APP_ID = BasicUtil.evl(APP_ID, FengConfig.DEFAULT_APP_ID);
        if(BasicUtil.isEmpty(APP_ID)){
            return;
        }
        DataRow row = new DataRow();
        row.put("APP_ID", BasicUtil.evl(APP_ID, FengConfig.DEFAULT_APP_ID));
        row.put("APP_SECRET", BasicUtil.evl(APP_SECRET, FengConfig.DEFAULT_APP_SECRET));
        row.put("TYPE", BasicUtil.evl(TYPE, FengConfig.DEFAULT_TYPE));
        row.put("ACCESS_TOKEN_SERVER", BasicUtil.evl(ACCESS_TOKEN_SERVER, FengConfig.DEFAULT_ACCESS_TOKEN_SERVER));
    }
    @Bean("anyline.feng.init.util")
    public FengUtil instance(){
        return FengUtil.getInstance();
    }
}
