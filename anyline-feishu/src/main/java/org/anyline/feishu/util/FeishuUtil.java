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
import org.anyline.adapter.KeyAdapter;
import org.anyline.entity.DataRow;
import org.anyline.feishu.entity.FeishuUser;
import org.anyline.net.HttpUtil;
import org.anyline.util.AnylineConfig;
import org.anyline.util.BasicUtil;
import org.anyline.util.BeanUtil;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class FeishuUtil {
	private static final Logger log = LoggerFactory.getLogger(FeishuUtil.class); 
	private static Hashtable<String,FeishuUtil> instances = new Hashtable<String,FeishuUtil>(); 
	private FeishuConfig config = null;
	private Client client;
	private String tenant_access_token = null;
	private Long tenant_access_token_time = null;

	static {
		Hashtable<String, AnylineConfig> configs = FeishuConfig.getInstances();
		for(String key:configs.keySet()){
			instances.put(key, getInstance(key));
		}
	}

	public FeishuUtil(){

	}
	public FeishuUtil(FeishuConfig config){
		this.config = config;
		client = Client.newBuilder(config.APP_ID, config.APP_SECRET).build();
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
			FeishuConfig config = FeishuConfig.getInstance(key);
			if(null != config) {
				util = new FeishuUtil(config);
				instances.put(key, util);
			}
		}
		return util; 
	}
	public static FeishuUtil reg(String key, DataRow config){
		FeishuConfig conf = FeishuConfig.register(key, config);
		FeishuUtil util = new FeishuUtil(conf);
		instances.put(key, util);
		return util;
	}
	public static FeishuUtil reg(String key, String id, String secret){
		FeishuConfig conf = FeishuConfig.register(key, id, secret);
		FeishuUtil util = new FeishuUtil(conf);
		instances.put(key, util);
		return util;
	}
	public static FeishuUtil reg(String key, String id, String secret, String redirect){
		FeishuConfig conf = FeishuConfig.register(key, id, secret, redirect);
		FeishuUtil util = new FeishuUtil(conf);
		instances.put(key, util);
		return util;
	}
	public FeishuConfig config(){
		return config;
	}
	public String tenant_access_token(){
		if(null == tenant_access_token || (System.currentTimeMillis()-tenant_access_token_time)/1000 >7000){
			flushToken();
		}
		return tenant_access_token;
	}

	/**
	 *
	 * @param code 用户授权确认后重定向中的code
	 * @return
	 */
	public DataRow access_token(String code){
		DataRow token = null;
		String url = "https://open.feishu.cn/open-apis/authen/v1/oidc/access_token";
		Map<String, String> header = new Hashtable<>();
		header.put("Content-Type","application/json; charset=utf-8");
		header.put("Authorization", "Bearer " + tenant_access_token());
		Map<String,String> params = new HashMap<>();
		params.put("grant_type", "authorization_code");
		params.put("code", code);
		String json = BeanUtil.map2json(params);
		String body = HttpUtil.post(header, url, new StringEntity(json, "UTF-8")).getText();
		log.info("[get feishu access token][body:{}]", body);
		DataRow row = DataRow.parseJson(KeyAdapter.KEY_CASE.SRC, body);
		token = row.getRow("data");
		return token;
	}
	public FeishuUser user_info(String code){
		return user_info(access_token(code));
	}
	public FeishuUser user_info(DataRow token){
		FeishuUser user = new FeishuUser();
		String url = "https://open.feishu.cn/open-apis/authen/v1/user_info";
		Map<String, String> header = new Hashtable<>();
		header.put("Content-Type","application/json; charset=utf-8");
		header.put("Authorization", "Bearer " + token.getString("access_token"));
		String body = HttpUtil.get(header, url).getText();
		log.info("[get feishu user info][body:{}]", body);
		DataRow row = DataRow.parseJson(KeyAdapter.KEY_CASE.SRC, body);
		DataRow data = row.getRow("data");
		user.setName(data.getString("name"));
		user.setEnName(data.getString("en_name"));
		user.setAvatarBig(data.getString("avatar_big"));
		user.setAvatarUrl(data.getString("avatar_url"));
		user.setAvatarThumb(data.getString("avatar_thumb"));
		user.setAvatarMiddle(data.getString("avatar_middle"));
		user.setOpenid(data.getString("open_id"));
		user.setUnionid(data.getString("union_id"));
		user.setEmail(data.getString("email"));
		user.setMobile(data.getString("mobile"));
		user.setEnterpriseEmail(data.getString("enterprise_email"));
		user.setEmployeeNo(data.getString("employee_no"));
		user.setId(data.getString("user_id"));
		user.setTenantKey(data.getString("tenant_key"));
		return user;
	}
	private void flushToken(){
		String url = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal/";
		Map<String, String> header = new Hashtable<>();
		header.put("Content-Type","application/json; charset=utf-8");
		Map<String,String> params = new HashMap<>();
		params.put("app_id", config.APP_ID);
		params.put("app_secret", config.APP_SECRET);
		try {
			String json = BeanUtil.map2json(params);
			String body = HttpUtil.post(header, url, new StringEntity(json, "UTF-8")).getText();
			DataRow row = DataRow.parseJson(body);
			if("0".equals(row.getString("code"))){
				tenant_access_token = row.getString("tenant_access_token");
				tenant_access_token_time = System.currentTimeMillis();
			}else {
				log.warn("[flush token][response:{}]", body);
			}
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * 创建用户登录连接
	 * @param callback 回调地址(可以从url获取access_token)
	 * @param state
	 * @return
	 */
	public String createAuthUrl(String callback, String scope, String state){
		StringBuilder builder = new StringBuilder();
		//https://open.feishu.cn/open-apis/authen/v1/authorize?app_id=cli_a69b4944f47b100e&redirect_uri=https%3A%2F%2Ffs.deepbit.cn&scope=contact:contact%20bitable:app:readonly&state=213
		builder.append("https://open.feishu.cn/open-apis/authen/v1/authorize?app_id=").append(config.APP_ID);
		if(BasicUtil.isEmpty(callback)){
			callback = config.OAUTH_REDIRECT_URL;
		}
		builder.append("&redirect_uri=").append(HttpUtil.encode(callback, false, true));
		//参考https://open.feishu.cn/document/server-docs/application-scope/scope-list
		builder.append("&scope=").append(scope);
		if(BasicUtil.isNotEmpty(state)) {
			builder.append("&state=").append(state);
		}
		return builder.toString();
	}
	public String createAuthUrl(String callback, String state){
		return createAuthUrl(callback, "contact:contact.base:readonly", state);
	}
	public String createAuthUrl(String state){
		return createAuthUrl(config.OAUTH_REDIRECT_URL, state);
	}
} 
