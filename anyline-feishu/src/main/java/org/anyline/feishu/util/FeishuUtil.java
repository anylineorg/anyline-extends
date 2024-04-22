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

import com.lark.oapi.Client;
import org.anyline.entity.DataRow;
import org.anyline.net.HttpUtil;
import org.anyline.util.AnylineConfig;
import org.anyline.util.BasicUtil;
import org.anyline.util.ConfigTable;
import org.anyline.util.regular.RegularUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.Hashtable;

public class FeishuUtil {
	private static final Logger log = LoggerFactory.getLogger(FeishuUtil.class); 
	private static Hashtable<String,FeishuUtil> instances = new Hashtable<String,FeishuUtil>(); 
	private FeishuConfig config = null;
	private Client client;

	static {
		Hashtable<String, AnylineConfig> configs = FeishuConfig.getInstances();
		for(String key:configs.keySet()){
			instances.put(key, getInstance(key));
		}
	}

	public static Hashtable<String, FeishuUtil> getInstances(){
		return instances;
	}
	public Client client(){
		return client;
	}
	public void client(Client client){
		this.client = client;
	}

	public static FeishuUtil getInstance(){
		return getInstance(FeishuConfig.DEFAULT_INSTANCE_KEY);
	} 
	public static FeishuUtil getInstance(String key){
		if(BasicUtil.isEmpty(key)){
			key = FeishuConfig.DEFAULT_INSTANCE_KEY;
		} 
		FeishuUtil util = instances.get(key); 
		if(null == util){
			util = new FeishuUtil();
			FeishuConfig config = FeishuConfig.getInstance(key);
			if(null != config) {
				util.config = config;
				instances.put(key, util);
				util.client = Client.newBuilder(config.APP_ID, config.APP_SECRET).build();
			}
		}
		return util; 
	}

} 
