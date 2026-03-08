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


package org.anyline.tdmap.util;

import org.anyline.util.BasicUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component("anyline.tdmap.load.bean")
public class TDMapBean implements InitializingBean {

    @Value("${anyline.tdmap.host:}")
    private String HOST		;
    @Value("${anyline.tdmap.key:}")
    private String KEY		;
    @Value("${anyline.tdmap.secret:}")
    private String SECRET 	;
    @Value("${anyline.tdmap.table:}")
    private String TABLE 	;

    @Override
    public void afterPropertiesSet()  {
        KEY = BasicUtil.evl(KEY, TDMapConfig.DEFAULT_KEY);
        if(BasicUtil.isEmpty(KEY)) {
            return;
        }
        TDMapConfig config = TDMapConfig.register(KEY, BasicUtil.evl(SECRET, TDMapConfig.DEFAULT_SECRET)
                , BasicUtil.evl(TABLE, TDMapConfig.DEFAULT_TABLE));
        if(BasicUtil.isNotEmpty(this.HOST)) {
            config.HOST = this.HOST;
        }
    }
    @Bean("anyline.tdmap.init.client")
    public TDMapClient instance() {
        return TDMapClient.getInstance();
    }
}
