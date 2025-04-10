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
import org.anyline.util.BasicUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component("anyline.print.fei.load.bean")
public class FeiBean implements InitializingBean {

    @Value("${anyline.print.fei.user:}")
    public String USER                   ;
    @Value("${anyline.print.fei.key:}")
    public String KEY               ;
    @Value("${anyline.print.fei.app:type:}")
    public String TYPE                     ; // 0:自用 1:开放


    @Override
    public void afterPropertiesSet()  {
        USER = BasicUtil.evl(USER, FeiConfig.DEFAULT_USER);
        if(BasicUtil.isEmpty(USER)){
            return;
        }
        DataRow row = new DataRow();
        row.put("USER", BasicUtil.evl(USER, FeiConfig.DEFAULT_USER));
        row.put("KEY", BasicUtil.evl(KEY, FeiConfig.DEFAULT_KEY));
        row.put("TYPE", BasicUtil.evl(TYPE, FeiConfig.DEFAULT_TYPE));
    }
    @Bean("anyline.print.fei.init.util")
    public FeiUtil instance(){
        return FeiUtil.getInstance();
    }
}
