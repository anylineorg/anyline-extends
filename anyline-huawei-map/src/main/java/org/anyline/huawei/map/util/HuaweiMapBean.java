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


package org.anyline.huawei.map.util;

import org.anyline.util.BasicUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component("anyline.huawei.map.load.bean")
public class HuaweiMapBean implements InitializingBean {

    @Value("${anyline.HuaweiMap.host:}")
    private String HOST		;
    @Value("${anyline.HuaweiMap.secret:}")
    private String SECRET 	;
    @Value("${anyline.HuaweiMap.table:}")
    private String TABLE 	;

    @Override
    public void afterPropertiesSet()  {
        HuaweiMapConfig config = HuaweiMapConfig.register(BasicUtil.evl(SECRET, HuaweiMapConfig.DEFAULT_SECRET)
                , BasicUtil.evl(TABLE, HuaweiMapConfig.DEFAULT_TABLE));
        if(BasicUtil.isNotEmpty(this.HOST)) {
            config.HOST = this.HOST;
        }
    }
    @Bean("anyline.huawei.map.init.client")
    public HuaweiMapClient instance() {
        return HuaweiMapClient.getInstance();
    }
}
