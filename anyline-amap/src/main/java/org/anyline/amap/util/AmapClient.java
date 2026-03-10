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


package org.anyline.amap.util;

import org.anyline.client.map.AbstractMapClient;
import org.anyline.client.map.MapClient;
import org.anyline.entity.*;
import org.anyline.exception.AnylineException;
import org.anyline.log.Log;
import org.anyline.log.LogProxy;
import org.anyline.net.HttpResponse;
import org.anyline.net.HttpUtil;
import org.anyline.util.*;
import org.anyline.util.encrypt.MD5Util;

import java.io.File;
import java.util.*;

/**
 * 高德云图
 * @author zh
 *
 */
public class AmapClient extends AbstractMapClient implements MapClient {
	private static Log log = LogProxy.get(AmapClient.class);
	public AmapConfig config = null;
	private static Hashtable<String, AmapClient> instances = new Hashtable<>();

	public static Hashtable<String, AmapClient> getInstances() {
		return instances;
	}

	public AmapConfig getConfig() {
		return config;
	}
	public static AmapClient getInstance() {
		return getInstance("default");
	}

	public static AmapClient getInstance(String key) {
		if (BasicUtil.isEmpty(key)) {
			key = "default";
		}
		AmapClient util = instances.get(key);
		if (null == util) {
			AmapConfig config = AmapConfig.getInstance(key);
			if(null != config) {
				util = new AmapClient();
				util.config = config;
				instances.put(key, util);
			}
		}
		return util;
	}


	/**
	 * 添加记录
	 * @param name  name
	 * @param loctype 1:经纬度 2:地址
	 * @param lng 经度
	 * @param lat 纬度
	 * @param address  address
	 * @param extras  extras
	 * @return String
	 */
	public String create(String name, int loctype, String lng, String lat, String address, Map<String, Object> extras) {
		String url = AmapConfig.DEFAULT_YUNTU_HOST + "/datamanage/data/create";
		Map<String, Object> params = new HashMap<>();
		params.put("key", config.KEY);
		params.put("tableId", config.TABLE);
		params.put("loctype", loctype+"");
		Map<String, Object> data = new HashMap<>();
		if(null != extras) {
			Iterator<String> keys = extras.keySet().iterator();
			while(keys.hasNext()) {
				String key = keys.next();
				Object value = extras.get(key);
				if(BasicUtil.isNotEmpty(value)) {
					data.put(key, value);
				}
			}
		}
		data.put("_name", name);
		if(BasicUtil.isNotEmpty(lng) && BasicUtil.isNotEmpty(lat)) {
			data.put("_location", lng+","+lat);
		}
		if(BasicUtil.isNotEmpty(address)) {
			data.put("_address", address);
		}
		params.put("data", BeanUtil.map2json(data));
		String sign = sign(params);
		params.put("sig", sign);
		String txt = HttpUtil.post(url, "UTF-8", params).getText();
		String id = null;
		try{
			DataRow row = DataRow.parseJson(txt);
			if(row.containsKey("status")) {
				String status = row.getString("status");
				if("1".equals(status) && row.containsKey("_id")) {
					id = row.getString("_id");
					log.info("[添加标注完成][id:{}][name:{}]", id, name);
				}else{
					log.warn("[添加标注失败][name:{}][info:{}]", name, row.getString("info"));
					log.warn("[param:{}]",BeanUtil.map2string(params));
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return id;
	}
	public String create(String name, String lng, String lat, String address, Map<String, Object> extras) {
		return create(name, 1, lng, lat, address, extras);
	}
	public String create(String name, String lng, String lat, Map<String, Object> extras) {
		return create(name, 1, lng, lat, null, extras);
	}
	public String create(String name, int loctype, String lng, String lat, String address) {
		return create(name, loctype, lng, lat, address, null);
	}
	public String create(String name, String lng, String lat, String address) {
		return create(name, lng, lat, address, null);
	}
	public String create(String name, String lng, String lat) {
		return create(name, lng, lat, null, null);
	}
	public String create(String name, String address) {
		return create(name, null, null, address);
	}


	/**
	 * 删除标注
	 * @param ids  ids
	 * @return int
	 */
	public long delete(String ... ids) {
		if(null == ids) {
			return 0;
		}
		List<String> list = new ArrayList<>();
		for(String id:ids) {
			list.add(id);
		}
		return delete(list);
	}
	public long delete(List<String> ids) {
		long cnt = 0;
		if(null == ids || ids.isEmpty()) {
			return cnt;
		}
		String param = "";
		int size = ids.size();
		// 一次删除最多50条 大于50打后拆分数据
		if(size > 50) {
			int navi = (size-1)/50 + 1;
			for(int i=0; i<navi; i++) {
				int fr = i*50;
				int to = i*50 + 49;
				if(to > size-1) {
					to = size - 1;
				}
				List<String> clds = ids.subList(fr, to);
				cnt += delete(clds);
			}
			return cnt;
		}

		for(int i=0; i<size; i++) {
			if(i==0) {
				param += ids.get(i);
			}else{
				param += "," + ids.get(i);
			}
		}
		Map<String, Object> params = new HashMap<>();
		params.put("key", config.KEY);
		params.put("tableId", config.TABLE);
		params.put("ids", param);
		params.put("sig", sign(params));
		String url = AmapConfig.DEFAULT_YUNTU_HOST + "/datamanage/data/delete";
		String txt = HttpUtil.post(url, "UTF-8", params).getText();
		if(ConfigTable.IS_DEBUG && log.isWarnEnabled()) {
			log.info("[删除标注][param:{}]", BeanUtil.map2string(params));
		}
		try{
			DataRow json = DataRow.parseJson(txt);
			if(json.containsKey("status")) {
				String status = json.getString("status");
				if("1".equals(status)) {
					cnt = json.getInt("success");
					log.info("[删除标注完成][success:{}][fail:{}]", cnt, json.getInt("fail"));
				}else{
					log.info("[删除标注失败][info:{}]", json.getString("info"));
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
			cnt = -1;
		}
		return cnt;
	}
	/**
	 * 更新地图
	 * @param id  id
	 * @param name  name
	 * @param loctype  loctype
	 * @param lng 经度
	 * @param lat 纬度
	 * @param address  address
	 * @param extras  extras
	 * @return int 0:更新失败,没有对应的id  1:更新完成  -1:异常
	 */
	public long update(String id, String name, int loctype, String lng, String lat, String address, Map<String, Object> extras) {
		long cnt = 0;
		String url = AmapConfig.DEFAULT_YUNTU_HOST + "/datamanage/data/update";
		Map<String, Object> params = new HashMap<>();
		params.put("key", config.KEY);
		params.put("tableId", config.TABLE);
		params.put("loctype", loctype+"");

		Map<String, Object> data = new HashMap<>();
		if(null != extras) {
			Iterator<String> keys = extras.keySet().iterator();
			while(keys.hasNext()) {
				String key = keys.next();
				Object value = extras.get(key);
				data.put(key, value);
			}
		}
		data.put("_id", id);
		data.put("_name", name);
		if(BasicUtil.isNotEmpty(lng) && BasicUtil.isNotEmpty(lat)) {
			data.put("_location", lng+","+lat);
		}
		if(BasicUtil.isNotEmpty(address)) {
			data.put("_address", address);
		}
		params.put("data", BeanUtil.map2json(data));
		params.put("sig", sign(params));
		String txt = HttpUtil.post(url, "UTF-8", params).getText();
		if(ConfigTable.IS_DEBUG && log.isWarnEnabled()) {
			log.info("[更新标注][param:{}]",BeanUtil.map2string(params));
		}
		try{
			DataRow json = DataRow.parseJson(txt);
			if(json.containsKey("status")) {
				String status = json.getString("status");
				if("1".equals(status)) {
					cnt = 1;
					log.info("[更新标注完成][id:{}][name:{}]",id,name);
				}else{
					log.warn("[更新标注失败][name:{}][info:{}]",name,json.getString("info"));
					cnt = 0;
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
			cnt = -1;
		}
		return cnt;
	}
	public long update(String id, String name, String lng, String lat, String address, Map<String, Object> extras) {
		return update(id, name, 1, lng, lat, address, extras);
	}
	public long update(String id, String name, String lng, String lat, Map<String, Object> extras) {
		return update(id, name, 1, lng, lat, null, extras);
	}
	public long update(String id, String name, int loctype, String lng, String lat, String address) {
		return update(id, name, loctype, lng, lat, address, null);
	}
	public long update(String id, String name, String lng, String lat, String address) {
		return update(id, name, lng, lat, address, null);
	}
	public long update(String id, String name, String lng, String lat) {
		return update(id, name, lng, lat, null, null);
	}
	public long update(String id, String name, String address) {
		return update(id, name, null, null, address);
	}
	public long update(String id, String name) {
		return update(id, name, null);
	}

	/**
	 * 创建新地图
	 * @param name  name
	 * @return String
	 */
	public String createTable(String name) {
		String tableId = null;
		String url = AmapConfig.DEFAULT_YUNTU_HOST + "/datamanage/table/create";
		Map<String, Object> params = new HashMap<>();
		params.put("key", config.KEY);
		params.put("name", name);
		String sign = sign(params);
		params.put("sig", sign);
		String txt = HttpUtil.post(url, "UTF-8", params).getText();
		DataRow json = DataRow.parseJson(txt);
		if(json.containsKey("tableId")) {
			tableId = json.getString("tableId");
			log.info("[创建地图完成][tableId:{}]", tableId);
		}else{
			log.info("[创建地图失败][info:{}][param:{}]",txt,BeanUtil.map2string(params));
		}
		return tableId;
	}
	/**
	 * 本地检索 检索指定云图tableId里,对应城市（全国/省/市/区县）范围的POI信息
	 * API:http://lbs.amap.com/yuntu/reference/cloudsearch/#t1
	 * @param keywords  keywords
	 * @param city  city
	 * @param filter  filter
	 * @param sortrule  sortrule
	 * @param limit  limit
	 * @param page  page
	 * @return DataSet
	 */
	public DataSet<DataRow> local(String keywords, String city, String filter, String sortrule, int limit, int page) {
		DataSet<DataRow> set = null;
		String url = AmapConfig.DEFAULT_YUNTU_HOST + "/datasearch/local";
		Map<String, Object> params = new HashMap<>();
		params.put("key", config.KEY);
		params.put("tableId", config.TABLE);
		params.put("keywords", keywords);
		if(BasicUtil.isEmpty(city)) {
			city = "全国";
		}
		params.put("city", city);
		params.put("filter", filter);
		params.put("sortrule", sortrule);
		limit = NumberUtil.min(limit, 100);
		params.put("limit", limit+"");
		page = NumberUtil.max(page, 1);
		params.put("page", page+"");
		String sign = sign(params);
		params.put("sig", sign);
		String txt = HttpUtil.post(url, "UTF-8", params).getText();
		PageNavi navi = new DefaultPageNavi();
		navi.setCurPage(page);
		navi.setPageRows(limit);
		try{
			DataRow json = DataRow.parseJson(txt);
			if(json.containsKey("count")) {
				navi.setTotalRow(json.getInt("count"));
			}
			if(json.containsKey("datas")) {
				set = json.getSet("datas");
			}else{
				set = new DataSet();
				log.info("[本地搜索失败][info:{}]",json.getString("info"));
				log.info("[本地搜索失败][params:{}]",BeanUtil.map2string(params));
				set.setException(new Exception(json.getString("info")));
			}
		}catch(Exception e) {
			log.warn("[本地搜索失败][info:{}]",e.toString());
			set = new DataSet();
			set.setException(e);
		}
		set.setNavi(navi);
		log.info("[本地搜索][size:{}]",navi.getTotalRow());
		return set;
	}
	/**
	 * 周边搜索 在指定tableId的数据表内,搜索指定中心点和半径范围内,符合筛选条件的位置数据
	 * API:http://lbs.amap.com/yuntu/reference/cloudsearch/#t2
	 * @param center  center
	 * @param radius 查询半径
	 * @param keywords 关键词
	 * @param filters 过滤条件
	 * @param sortrule 排序
	 * @param limit 每页多少条
	 * @param page 第几页
	 * @return DataSet
	 */
	public DataSet<DataRow> around(String center, int radius, String keywords, Map<String, String> filters, String sortrule, int limit, int page) {
		DataSet<DataRow> set = null;
		String url = AmapConfig.DEFAULT_YUNTU_HOST + "/datasearch/around";
		Map<String, Object> params = new HashMap<>();
		params.put("key", config.KEY);
		params.put("tableId", config.TABLE);
		params.put("center", center);
		params.put("radius", radius+"");
		if(BasicUtil.isNotEmpty(keywords)) {
			params.put("keywords", keywords);
		}
		// 过滤条件
		if(null != filters && !filters.isEmpty()) {
			String filter = "";
			Iterator<String> keys = filters.keySet().iterator();
			while(keys.hasNext()) {
				String key = keys.next();
				String value = filters.get(key);
				if(BasicUtil.isEmpty(value)) {
					continue;
				}
				if("".equals(filter)) {
					filter = key + ":" + value;
				}else{
					filter = filter + "+" + key + ":" + value;
				}
			}
			if(!"".equals(filter)) {
				params.put("filter", filter);
			}
		}
		if(BasicUtil.isNotEmpty(sortrule)) {
			params.put("sortrule", sortrule);
		}
		limit = NumberUtil.min(limit, 100);
		params.put("limit", limit+"");
		page = NumberUtil.max(page, 1);
		params.put("page", page+"");
		String sign = sign(params);
		params.put("sig", sign);
		String txt = HttpUtil.post(url, "UTF-8", params).getText();
		PageNavi navi = new DefaultPageNavi();
		navi.setCurPage(page);
		navi.setPageRows(limit);
		try{
			DataRow json = DataRow.parseJson(txt);
			if(json.containsKey("count")) {
				navi.setTotalRow(json.getInt("count"));
			}
			if(json.containsKey("datas")) {
				set = json.getSet("datas");
			}else{
				log.warn("[周边搜索失败][info:{}]",json.getString("info"));
				log.warn("[周边搜索失败][params:{}]",BeanUtil.map2string(params));
				set = new DataSet();
				set.setException(new Exception(json.getString("info")));
			}
		}catch(Exception e) {
			log.warn("[周边搜索失败][error:{}]",e.toString());
			e.printStackTrace();
			set = new DataSet();
			set.setException(e);
		}
		set.setNavi(navi);
		log.info("[周边搜索][size:{}]",navi.getTotalRow());
		return set;
	}

	public DataSet<DataRow> around(String center, int radius, Map<String, String> filters, String sortrule, int limit, int page) {
		return around(center, radius, null, filters, sortrule, limit, page);
	}
	public DataSet<DataRow> around(String center, int radius, Map<String, String> filters, int limit, int page) {
		return around(center, radius, null, filters, null, limit, page);
	}
	public DataSet<DataRow> around(String center, int radius, Map<String, String> filters, int limit) {
		return around(center, radius, null, filters, null, limit, 1);
	}
	public DataSet<DataRow> around(String center, int radius, String keywords, String sortrule, int limit, int page) {
		Map<String, String> filter = new HashMap<String, String>();
		return around(center, radius, keywords, filter, sortrule, limit, page);
	}

	public DataSet<DataRow> around(String center, int radius, String keywords, int limit, int page) {
		return around(center, radius, keywords, "", limit, page);
	}
	public DataSet<DataRow> around(String center, int radius, int limit, int page) {
		return around(center, radius, "", limit, page);
	}
	public DataSet<DataRow> around(String center, int radius, int limit) {
		return around(center, radius, "", limit, 1);
	}
	public DataSet<DataRow> around(String center, int radius) {
		return around(center, radius, "", 100, 1);
	}
	public DataSet<DataRow> around(String center) {
		return around(center, ConfigTable.getInt("AMAP_MAX_RADIUS"));
	}
	/**
	 * 按条件检索数据（可遍历整表数据） 根据筛选条件检索指定tableId数据表中的数据
	 * API:http://lbs.amap.com/yuntu/reference/cloudsearch/#t5
	 * AmapClient.getInstance(TABLE_TENANT).list("tenant_id:1","shop_id:1", 10, 1);
	 * @param filter 查询条件
	 * filter=key1:value1+key2:[value2,value3]
	 * filter=type:酒店+star:[3,5]  等同于SQL语句的: WHERE type = "酒店" AND star BETWEEN 3 AND 5
	 * @param sortrule 排序条件
	 * 支持按用户自选的字段（仅支持数值类型字段）升降序排序.1:升序,0:降序
	 * 若不填升降序,默认按升序排列. 示例:按年龄age字段升序排序 sortrule = age:1
	 * @param limit 每页最大记录数为100
	 * @param page 当前页数 &gt;=1
	 * @return DataSet
	 */
	public DataSet<DataRow> list(String filter, String sortrule, int limit, int page) {
		DataSet<DataRow> set = null;
		String url = AmapConfig.DEFAULT_YUNTU_HOST + "/datamanage/data/list";
		Map<String, Object> params = new HashMap<>();
		params.put("key", config.KEY);
		params.put("tableId", config.TABLE);
		params.put("filter", filter);
		if(BasicUtil.isNotEmpty(sortrule)) {
			params.put("sortrule", sortrule);
		}
		limit = NumberUtil.min(limit, 100);
		params.put("limit", limit+"");
		page = NumberUtil.max(page, 1);
		params.put("page", page+"");
		String sign = sign(params);
		params.put("sig", sign);
		String txt = HttpUtil.post(url, "UTF-8", params).getText();
		PageNavi navi = new DefaultPageNavi();
		navi.setCurPage(page);
		navi.setPageRows(limit);
		try{
			DataRow json = DataRow.parseJson(txt);
			if(json.containsKey("count")) {
				navi.setTotalRow(json.getInt("count"));
			}
			if(json.containsKey("datas")) {
				set = json.getSet("datas");
				if(ConfigTable.IS_DEBUG && log.isWarnEnabled()) {
					log.info("[条件搜索][结果数量:{}]",set.size());
				}
			}else{
				set = new DataSet();
				log.warn("[条件搜索失败][info:{}]",json.getString("info"));
				log.warn("[条件搜索失败][params:{}]",BeanUtil.map2string(params));
				set.setException(new Exception(json.getString("info")));
			}
		}catch(Exception e) {
			log.warn("[条件搜索失败][error:{}]",e.toString());
			set = new DataSet();
			set.setException(e);
		}
		set.setNavi(navi);
		log.info("[条件搜索][size:{}]",navi.getTotalRow());
		return set;
	}
	/**
	 * ID检索 在指定tableId的数据表内,查询对应数据id的数据详情
	 * API:http://lbs.amap.com/yuntu/reference/cloudsearch/#t4
	 * API:在指定tableId的数据表内,查询对应数据id的数据详情
	 * @param id  id
	 * @return DataRow
	 */
	public DataRow info(String id) {
		DataRow row = null;
		String url = AmapConfig.DEFAULT_YUNTU_HOST + "/datasearch/id";
		Map<String, Object> params = new HashMap<>();
		params.put("key", config.KEY);
		params.put("tableId", config.TABLE);
		params.put("_id", id);
		String sign = sign(params);
		params.put("sig", sign);
		String txt = HttpUtil.post(url, "UTF-8", params).getText();
		try{
			DataRow json = DataRow.parseJson(txt);
			if(json.containsKey("datas")) {
				DataSet<DataRow> set = json.getSet("datas");
				if(set.size() > 0) {
					row = set.getRow(0);
				}
			}else{
				log.warn("[周边搜索失败][info:{}]",json.getString("info"));
				log.warn("[周边搜索失败][params:{}]",BeanUtil.map2string(params));
			}
		}catch(Exception e) {
			log.warn("[周边搜索失败][error:{}]",e.toString());
			e.printStackTrace();
		}
		return row;
	}
	/**
	 * 省数据分布检索 检索指定云图tableId里,全表数据或按照一定查询或筛选过滤而返回的数据中,含有数据的省名称（中文名称）和对应POI个数（count）的信息列表,按照count从高到低的排序展现
	 * API:http://lbs.amap.com/yuntu/reference/cloudsearch/#t6
	 * @param keywords 关键字 必须
	 * @param country ""或null时 默认:中国
	 * @param filter 条件
	 * @return DataSet
	 */
	public DataSet<DataRow> statByProvince(String keywords, String country, String filter) {
		DataSet<DataRow> set = null;
		String url = AmapConfig.DEFAULT_YUNTU_HOST + "/datasearch/statistics/province";
		Map<String, Object> params = new HashMap<>();
		params.put("key", config.KEY);
		params.put("tableId", config.TABLE);
		params.put("filter", filter);
		params.put("keywords", keywords);
		country = BasicUtil.evl(country, "中国")+"";
		params.put("country", country);
		String sign = sign(params);
		params.put("sig", sign);
		String txt = HttpUtil.post(url, "UTF-8", params).getText();
		try{
			DataRow json = DataRow.parseJson(txt);
			if(json.containsKey("datas")) {
				set = json.getSet("datas");
			}else{
				set = new DataSet();
				log.warn("[数据分布检索失败][info:{}]",json.getString("info"));
				log.warn("[数据分布检索失败][params:{}]",BeanUtil.map2string(params));
				set.setException(new Exception(json.getString("info")));
			}
		}catch(Exception e) {
			log.warn("[数据分布检索失败][error:{}]",e.toString());
			set = new DataSet();
			set.setException(e);
		}
		return set;
	}

	/**
	 * 市数据分布检索 检索指定云图tableId里,全表数据或按照一定查询或筛选过滤而返回的数据中,含有数据的市名称（中文名称）和对应POI个数（count）的信息列表,按照count从高到低的排序展现
	 * API:http://lbs.amap.com/yuntu/reference/cloudsearch/#t6
	 * @param keywords 关键字 必须
	 * @param province ""或null时 默认:全国
	 * @param filter 条件
	 * @return DataSet
	 */
	public DataSet<DataRow> statByCity(String keywords, String province, String filter) {
		DataSet<DataRow> set = null;
		String url = AmapConfig.DEFAULT_YUNTU_HOST + "/datasearch/statistics/city";
		Map<String, Object> params = new HashMap<>();
		params.put("key", config.KEY);
		params.put("tableId", config.TABLE);
		params.put("filter", filter);
		params.put("keywords", keywords);
		province = BasicUtil.evl(province, "全国")+"";
		params.put("country", province);
		String sign = sign(params);
		params.put("sig", sign);
		String txt = HttpUtil.post(url, "UTF-8", params).getText();
		try{
			DataRow json = DataRow.parseJson(txt);
			if(json.containsKey("datas")) {
				set = json.getSet("datas");
			}else{
				set = new DataSet();
				log.warn("[数据分布检索失败][info:{}]",json.getString("info"));
				log.warn("[数据分布检索失败][params:{}]",BeanUtil.map2string(params));
				set.setException(new Exception(json.getString("info")));
			}
		}catch(Exception e) {
			log.warn("[数据分布检索失败][error:{}]",e.toString());
			set = new DataSet();
			set.setException(e);
		}
		return set;
	}

	/**
	 * 区数据分布检索 检索指定云图tableId里,在指定的省,市下面全表数据或按照一定查询或筛选过滤而返回的数据中,所有区县名称（中文名称）和对应POI个数（count）的信息列表,按照count从高到低的排序展现
	 * API:http://lbs.amap.com/yuntu/reference/cloudsearch/#t6
	 * @param keywords 关键字 必须
	 * @param province   province
	 * @param city   city
	 * @param filter 条件
	 * @return DataSet
	 */
	public DataSet<DataRow> statByDistrict(String keywords, String province, String city, String filter) {
		DataSet<DataRow> set = null;
		String url = AmapConfig.DEFAULT_YUNTU_HOST + "/datasearch/statistics/province";
		Map<String, Object> params = new HashMap<>();
		params.put("key", config.KEY);
		params.put("tableId", config.TABLE);
		params.put("filter", filter);
		params.put("keywords", keywords);
		params.put("province", province);
		params.put("city", city);
		String sign = sign(params);
		params.put("sig", sign);
		String txt = HttpUtil.post(url, "UTF-8", params).getText();
		try{
			DataRow json = DataRow.parseJson(txt);
			if(json.containsKey("datas")) {
				set = json.getSet("datas");
			}else{
				set = new DataSet();
				log.warn("[数据分布检索失败][info:{}]",json.getString("info"));
				log.warn("[数据分布检索失败][params:{}]",BeanUtil.map2string(params));
				set.setException(new Exception(json.getString("info")));
			}
		}catch(Exception e) {
			log.warn("[数据分布检索失败][error:{}]",e.toString());
			set = new DataSet();
			set.setException(e);
		}
		return set;
	}
	/**
	 * 检索1个中心点,周边一定公里范围内（直线距离或者导航距离最大10公里）,一定时间范围内（最大24小时）上传过用户位置信息的用户,返回用户标识,经纬度,距离中心点距离.
	 * @param center  center
	 * @param radius  radius
	 * @param limit  limit
	 * @param timerange  timerange
	 * @return DataSet
	 */
	public DataSet<DataRow> nearby(String center, String radius, int limit, int timerange ) {
		DataSet<DataRow> set = null;
		String url = AmapConfig.DEFAULT_YUNTU_HOST + "/datasearch/statistics/province";
		Map<String, Object> params = new HashMap<>();
		params.put("key", config.KEY);
		params.put("center", center);
		params.put("radius", radius);
		params.put("searchtype", "0");
		params.put("limit", NumberUtil.min(limit, 100)+"");
		params.put("timerange", BasicUtil.evl(timerange,"1800")+"");
		String sign = sign(params);
		params.put("sig", sign);
		String txt = HttpUtil.post(url, "UTF-8", params).getText();
		try{
			DataRow json = DataRow.parseJson(txt);
			if(json.containsKey("datas")) {
				set = json.getSet("datas");
			}else{
				set = new DataSet();
				log.warn("[附近检索失败][info:}{}]",json.getString("info"));
				log.warn("[附近检索失败][params:{}]",BeanUtil.map2string(params));
				set.setException(new Exception(json.getString("info")));
			}
		}catch(Exception e) {
			log.warn("[附近检索失败][error:{}]",e.toString());
			set = new DataSet();
			set.setException(e);
		}
		return set;
	}
	/**
	 * 逆地理编码 按坐标查地址
	 * "country" :"中国",
	 * "province" :"山东省",
	 * "city" :"青岛市",
	 * "citycode" :"0532",
	 * "district" :"市南区",
	 * "adcode" :"370215",
	 * "township" :"**街道",
	 * "towncode" :"370215010000",
	 *
	 * @param coordinate  坐标
	 * @return Coordinate
	 */
	@Override
	public Coordinate regeo(Coordinate coordinate)  {

		SRS _type = coordinate.getSrs();
		Double _lng = coordinate.getLng();
		Double _lat = coordinate.getLat();

		coordinate.convert(SRS.GCJ02LL);
		coordinate.setSuccess(false);
		DataRow row = null;
		String api = "/v3/geocode/regeo";
		Map<String, Object> params = new HashMap<>();
		params.put("key", config.KEY);
		params.put("location", _lng + "," +_lat);

		Map<String, Object> ps = coordinate.getParams();
		if(null != ps) {
			if(ps.containsKey("poitype")) {
				params.put("poitype", ps.get("poitype"));
			}
			if(ps.containsKey("radius")) {
				params.put("radius", ps.get("radius"));
			}
			if(ps.containsKey("extensions")) {
				params.put("extensions", ps.get("extensions"));
			}
			if(ps.containsKey("homeorcorp")) {
				params.put("homeorcorp", ps.get("homeorcorp"));
			}
			if(ps.containsKey("roadlevel")) {
				params.put("roadlevel", ps.get("roadlevel"));
			}
		}

		row = get(AmapConfig.DEFAULT_HOST, api, params);
		row = row.getRow("regeocode");
		if (null != row) {
			coordinate.setAddress(row.getString("formatted_address"));
			DataRow adr = row.getRow("addressComponent");
			if (null != adr) {
				parse(coordinate, adr);
			}
			//解析附近poi
			DataSet<DataRow> pois = row.getSet("pois");
			List<Coordinate> poi_coordinates = new ArrayList<>();
			for (DataRow poi : pois) {
				Coordinate poi_coordinate = new Coordinate();
				parse(poi_coordinate, poi);
				poi_coordinate.setProvinceCode(coordinate.getProvinceCode());
				poi_coordinate.setProvinceName(coordinate.getProvinceName());
				poi_coordinate.setCityCode(coordinate.getCityCode());
				poi_coordinate.setCityName(coordinate.getCityName());
				poi_coordinate.setCountyCode(coordinate.getCountyCode());
				poi_coordinate.setCountyName(coordinate.getCountyName());
				poi_coordinate.setTownCode(coordinate.getTownCode());
				poi_coordinate.setTownName(coordinate.getTownName());
				poi_coordinate.setVillageCode(coordinate.getVillageCode());
				poi_coordinate.setVillageName(coordinate.getVillageName());

				poi_coordinates.add(poi_coordinate);
			}
			coordinate.setPois(poi_coordinates);
		}
		// 换回原坐标系
		coordinate.setLng(_lng);
		coordinate.setLat(_lat);
		coordinate.setSrs(_type);
		coordinate.setSuccess(true);
		return coordinate;
	}


	/**
	 * 解析返回结果赋值
	 * @param coordinate 有可能是oi
	 * @param result 返回内容
	 */
	private void parse(Coordinate coordinate, DataRow result) {
		coordinate.setMetadata(result);
		coordinate.setId(result.getString("id"));
		coordinate.setTitle(result.getString("name"));
		Object tel = result.get("tel");
		if(tel instanceof Collection) {
			if(!((Collection)tel).isEmpty()) {
				coordinate.setTel(tel.toString());
			}
		}else if(tel instanceof String) {
			coordinate.setTel((String) tel);
		}
		String ad_code = result.getString("adcode");
		if(null != ad_code) {
			String provinceCode = ad_code.substring(0, 2);
			String cityCode = ad_code.substring(0, 4);
			coordinate.setProvinceCode(provinceCode);
			coordinate.setProvinceName(result.getString("province"));
			coordinate.setCityCode(cityCode);
			coordinate.setCityName(result.getString("city"));
			coordinate.setCountyCode(ad_code);
			coordinate.setCountyName(result.getString("district"));
			coordinate.setTownCode(result.getString("towncode"));
			coordinate.setTownName(result.getString("township"));
			DataRow st = result.getRow("streetNumber");
			if (null != st) {
				String street = st.getString("street");
				String number = st.getString("number");
				if (null != number && null != street) {
					number = number.replace(street, "");
				}
				coordinate.setStreet(street);
				coordinate.setStreetNumber(number);

			}
		}else{
			//poi数据
			coordinate.setPoiCategoryName(result.getString("type"));
			coordinate.setAddress(result.getString("address"));
		}
		String location = result.getString("location");
		if(null != location && location.contains(",")) {
			String[] locations = location.split(",");
			coordinate.setLng(locations[0]);
			coordinate.setLat(locations[1]);
		}
		coordinate.correct();
	}
	/**
	 * 根据地址查坐标
	 * @param address  address
	 * @param city  city
	 * @return Coordinate
	 */
	@Override
	public Coordinate geo(String address, String city) {
		Coordinate coordinate = new Coordinate();
		coordinate.setAddress(address);
		if(null != address) {
			address = address.replace(" ","");
		}
		String api = "/v3/geocode/geo";
		Map<String, Object> params = new HashMap<>();
		params.put("key", config.KEY);
		params.put("address", address);
		if(BasicUtil.isNotEmpty(city)) {
			params.put("city", city);
		}
		DataRow row = get(AmapConfig.DEFAULT_HOST, api, params);
		coordinate.setMetadata(row);
		DataSet<DataRow> set = null;
		if(row.containsKey("geocodes")) {
			set = row.getSet("geocodes");
			if(set.size()>0) {
				DataRow first = set.getRow(0);
				String adcode = first.getString("ADCODE");
				coordinate.setLocation(first.getString("LOCATION"));
				coordinate.setCode(adcode);
				coordinate.setProvinceCode(BasicUtil.cut(adcode,0,2));
				coordinate.setProvinceName(first.getString("PROVINCE"));
				coordinate.setCityCode(BasicUtil.cut(adcode,0,4));
				coordinate.setCityName(first.getString("CITY"));
				coordinate.setCountyCode(first.getString("ADCODE"));
				coordinate.setCountyName(first.getString("DISTRICT"));
				coordinate.setAddress(first.getString("FORMATTED_ADDRESS"));
				coordinate.setLevel(first.getInt("LEVEL",0));
				String street = first.getString("STREET");
				String number = first.getString("NUMBER");
				if(null != number && null != street) {
					number = number.replace(street,"");
				}
				coordinate.setStreet(street);
				coordinate.setStreetNumber(number);
				coordinate.setSrs(SRS.GCJ02LL);
			}
		}else{
			log.warn("[坐标查询失败][info:{}][params:{}]",row.getString("info"),BeanUtil.map2string(params));
		}

		if(null != coordinate) {
			coordinate.correct();
		}
		return coordinate;
	}

	@Override
	public Coordinate ip(String ip) {
		return null;
	}
	/**
	 * 驾车路线规划
	 * http://lbs.amap.com/api/webservice/guide/api/direction#driving
	 * @param origin		出发地  origin		出发地
	 * @param destination	目的地  destination	目的地
	 * @param points		途经地 最多支持16个 坐标点之间用";"分隔
	 * @param strategy		选路策略  0,不考虑当时路况,返回耗时最短的路线,但是此路线不一定距离最短
	 *							  1,不走收费路段,且耗时最少的路线
	 *							  2,不考虑路况,仅走距离最短的路线,但是可能存在穿越小路/小区的情况
	 * @return DataRow
	 */
	@SuppressWarnings({"rawtypes", "unchecked" })
	public DataRow directionDrive(String origin, String destination, String points, int strategy) {
		DataRow row = null;
		String url = "http://restapi.amap.com/v3/direction/driving";
		Map<String, Object> params = new HashMap<>();
		params.put("key", config.KEY);
		params.put("origin", origin);
		params.put("destination", destination);
		params.put("strategy", strategy+"");
		if(BasicUtil.isNotEmpty(points)) {
			params.put("points", points);
		}
		String sign = sign(params);
		params.put("sig", sign);
		String txt = HttpUtil.get(url, "UTF-8", params).getText();
		try{
			row = DataRow.parseJson(txt);
			DataRow route = row.getRow("route");
			if(null != route) {
				List paths = route.getList("PATHS");
				if(paths.size()>0) {
					DataRow path = (DataRow)paths.get(0);
					row = path;
					List<DataRow> steps = (List<DataRow>)path.getList("steps");
					List<String> polylines = new ArrayList<>();
					for(DataRow step:steps) {
						String polyline = step.getString("polyline");
						String[] tmps = polyline.split(";");
						for(String tmp:tmps) {
							polylines.add(tmp);
						}
					}
					row.put("polylines", polylines);
				}
			}
		}catch(Exception e) {
			log.warn("[线路规划失败][error:{}]",e.toString());
		}
		return row;
	}
	public DataRow directionDrive(String origin, String destination) {
		return directionDrive(origin, destination, null, 0);
	}
	public DataRow directionDrive(String origin, String destination, String points) {
		return directionDrive(origin, destination, points, 0);
	}

	/**
	 *
	 * @param district 行政区 一般支持到区县级
	 * @param category 类别
	 * @param keyword 关键定
	 * @return List
	 */
	public List<Coordinate> poi(String district, String category, String keyword) {
		List<Coordinate> coordinates = new ArrayList<>();
		String api = "/v3/place/text";
		int vol = 25;
		Map<String, Object> params = new HashMap<>();
		if(BasicUtil.isNotEmpty(keyword)) {
			params.put("keywords", keyword);
		}
		if(BasicUtil.isNotEmpty(category)) {
			params.put("types", category);
		}
		params.put("city", district);
		params.put("extensions", "all");
		params.put("offset", vol);
		params.put("key", config.KEY);
		int page = 1;
		Map<String, Coordinate> maps = new HashMap<>();
		while (true) {
			params.put("page", page++);
			DataRow row = get(AmapConfig.DEFAULT_HOST, api, params);
			if(null == row) {
				break;
			}
			DataSet<DataRow> set = row.getSet("pois");
			int exists = 0;
			if(null == set || set.isEmpty()) {
				break;
			}
			for(DataRow item:set) {
				Coordinate coordinate = coordinate(item);
				if(maps.containsKey(coordinate.getId())) {
					exists++;
				}else{
					coordinates.add(coordinate);
					maps.put(coordinate.getId(), coordinate);
				}
			}
			//最后一页小于20个
			if(set.size() < vol) {
				break;
			}
			//有10个以上重复的中断(算成最后一页)
			if(exists > vol/2){
				break;
			}
		}
		log.warn("[查询结果:{}]", coordinates.size());
		return coordinates;
	}

	/**
	 * 附近poi https://lbs.qq.com/service/webService/webServiceGuide/search/webServiceSearch
	 * @param lng 经度
	 * @param lat 经度
	 * @param radius 半径
	 * @param category 类别
	 * @param keyword 关键定
	 * @return List
	 */
	@Override
	public List<Coordinate> poi(Double lng, Double lat, int radius, String category, String keyword) {
		List<Coordinate> coordinates = new ArrayList<>();
		String api = "/v3/place/around";

		Map<String, Object> params = new HashMap<>();
		if(BasicUtil.isNotEmpty(keyword)) {
			params.put("keywords", keyword);
		}
		params.put("location", lng + "," + lat);
		params.put("radius", radius);
		//filter=category= 241000,241100
		if(BasicUtil.isNotEmpty(category)) {
			params.put("types", category);
		}
		params.put("extensions", "all");
		params.put("offset", 25);
		params.put("key", config.KEY);
		int page = 1;
		Map<String, Coordinate> maps = new HashMap<>();
		while (true) {
			params.put("page", page++);
			DataRow row = get(AmapConfig.DEFAULT_HOST, api, params);
			if(null == row) {
				break;
			}
			DataSet<DataRow> set = row.getSet("pois");
			int exists = 0;
			if(null == set || set.isEmpty()) {
				break;
			}
			for(DataRow item:set) {
				Coordinate coordinate = coordinate(item);
				if(maps.containsKey(coordinate.getId())) {
					exists++;
				}else{
					coordinates.add(coordinate);
				}
				maps.put(coordinate.getId(), coordinate);
			}
			//最后一页小于20个
			if(set.size() < 25) {
				break;
			}
			//有10个以上重复的中断(算成最后一页)
			if(exists > 10) {
				break;
			}
		}
		log.warn("[查询结果:{}]", coordinates.size());
		return coordinates;
	}
	private Coordinate coordinate(DataRow row) {
		Coordinate coordinate = new Coordinate();
		coordinate.setPoiCategoryName(row.getString("type"));
		coordinate.setPoiCategoryCode(row.getString("typecode"));
		coordinate.setLocation(row.getString("location"));
		coordinate.setAddress(row.getString("address"));
		coordinate.setTitle(row.getString("name"));
		coordinate.setId(row.getString("id"));
		coordinate.setTel(row.getString("tel"));
		String adcode = row.getString("adcode");
		if(BasicUtil.isNotEmpty(adcode)) {
			String provinceCode = adcode.substring(0, 2);
			String cityCode = adcode.substring(0, 4);
			coordinate.setProvinceCode(provinceCode);
			coordinate.setCityCode(cityCode);
			coordinate.setCountyCode(adcode);
		}
		coordinate.setMetadata(row);
		return coordinate;
	}
	public DataRow get(String host, String api, Map<String, Object> params) {
		if(limit()) {
			return null;
		}
		DataRow row = null;
		File file = null;
		String txt = null;
		int status = 0;
		String json = BeanUtil.map2json(params);
		if(null != AmapConfig.CACHE_DIR) {
			File dir = new File(AmapConfig.CACHE_DIR, config.KEY+"/"+api.replace("/","_")+"/"+DateUtil.format("yyyyMMddHH"));
			String md5 = MD5Util.crypto(api + json);
			file = new File(dir, md5+".txt");
			if(file.exists() && AmapConfig.READ_CACHE) {
				txt = FileUtil.read(file, "UTF-8").toString().replace(json, "").trim();
			}
		}
		if(BasicUtil.isEmpty(txt)){
			sign(params);
			/*try {
				for (String key : params.keySet()) {
					Object value = params.get(key);
					if(value instanceof String) {
						params.put(key, URLEncoder.encode(value.toString(), "UTF-8"));
					}
				}
			}catch (Exception e) {
				e.printStackTrace();
			}*/
			HttpResponse response = HttpUtil.get(host + api,"UTF-8", params);
			status = response.getStatus();
			if(status == 200) {
				txt = response.getText();
				if(null != AmapConfig.CACHE_DIR) {
					FileUtil.write(json + "\r\n" + txt, file);
				}

			}else{
				throw new AnylineException(status, "api执行异常");
			}
		}
		if(BasicUtil.isNotEmpty(txt)){
			row = DataRow.parseJson(txt);
			if(null == row) {
				throw new AnylineException(status, row.getString("INFO"));
			}
			status = row.getInt("STATUS", 0);
			if (status == 0) {
				// [逆地理编码][执行失败][code:10044][info:USER_DAILY_QUERY_OVER_LIMIT]
				log.warn("[{}][执行失败][code:{}][info:{}]", api, row.getString("INFOCODE"), row.getString("INFO"));
				log.warn("[{}}][response:{}]", txt);
				String info_code = row.getString("INFOCODE");
				if ("10044".equals(info_code)) {
					last_limit = DateUtil.format("yyyy-MM-dd");
					throw new AnylineException("API_OVER_LIMIT", "访问已超出日访问量");
				} else if ("10019".equals(info_code) || "10020".equals(info_code) || "10021".equals(info_code)) {
					log.warn("并发量已达到上限,sleep 100 ...");
					try {
						Thread.sleep(100);
					} catch (Exception e) {
						e.printStackTrace();
					}
					return get(host, api, params);
				} else {
					throw new AnylineException(status, row.getString("INFO"));
				}
			}
		}
		return row;
	}
	/**
	 * 签名
	 * @param params  params
	 * @return String
	 */
	public String sign(Map<String, Object> params) {
		params.remove("sig");
		String sign = "";
		sign = BeanUtil.map2string(params) + config.SECRET;
		sign = MD5Util.sign(sign,"UTF-8");
		params.put("sig", sign);
		return sign;
	}

}
