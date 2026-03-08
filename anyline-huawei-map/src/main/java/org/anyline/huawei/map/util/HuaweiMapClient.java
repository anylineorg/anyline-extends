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

import org.anyline.client.map.AbstractMapClient;
import org.anyline.client.map.MapClient;
import org.anyline.entity.Coordinate;
import org.anyline.entity.DataRow;
import org.anyline.entity.DataSet;
import org.anyline.entity.SRS;
import org.anyline.exception.AnylineException;
import org.anyline.log.Log;
import org.anyline.log.LogProxy;
import org.anyline.net.HttpResponse;
import org.anyline.net.HttpUtil;
import org.anyline.util.BasicUtil;
import org.anyline.util.BeanUtil;
import org.anyline.util.DateUtil;
import org.anyline.util.FileUtil;
import org.apache.http.entity.StringEntity;

import java.io.File;
import java.util.*;

/**
 * 高德云图 
 * @author zh 
 * 
 */ 
public class HuaweiMapClient extends AbstractMapClient implements MapClient {
	private static Log log = LogProxy.get(HuaweiMapClient.class);
	public HuaweiMapConfig config = null;
	private static Hashtable<String, HuaweiMapClient> instances = new Hashtable<>();

	public static Hashtable<String, HuaweiMapClient> getInstances(){
		return instances;
	}

	public HuaweiMapConfig getConfig(){
		return config;
	}
	public static HuaweiMapClient getInstance() {
		return getInstance("default");
	}

	public static HuaweiMapClient getInstance(String key) {
		if (BasicUtil.isEmpty(key)) {
			key = "default";
		}
		HuaweiMapClient util = instances.get(key);
		if (null == util) {
			HuaweiMapConfig config = HuaweiMapConfig.getInstance(key);
			if(null != config) {
				util = new HuaweiMapClient();
				util.config = config;
				instances.put(key, util);
			}
		}
		return util;
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
		String api = "/mapApi/v1/siteService/reverseGeocode";
		Map<String, Object> params = new HashMap<>();
		Map<String, Object> location = new HashMap<>();
		location.put("lng", _lng);
		location.put("lat", _lat);
		params.put("location", location);
		params.put("radius", 50);

		row = post(HuaweiMapConfig.DEFAULT_HOST, api, params);
		if (null != row) {
			//解析附近poi
			DataSet<DataRow> pois = row.getSet("sites");
			if(!pois.isEmpty()){
				parse(coordinate, pois.getRow(0));
			}
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
	private void parse(Coordinate coordinate, DataRow result){
		coordinate.setMetadata(result);
		DataRow poi = result.getRow("poi");
		if(null != poi){
			coordinate.setTel(poi.getString("phone"));
			List types = poi.getList("hwPoiTypes");
			if(null != types){
				coordinate.setPoiCategoryCode(BeanUtil.concat(types));
			}

		}
		coordinate.setId(result.getString("siteId"));
		coordinate.setTitle(result.getString("name"));
		coordinate.setAddress(result.getString("formatAddress"));
		DataRow location = result.getRow("location");
		if(null != location){
			coordinate.setLng(location.getString("lng"));
			coordinate.setLat(location.getString("lat"));
		}
		DataRow adr = result.getRow("address");
		if(null != adr){
			coordinate.setProvinceName(adr.getString("adminArea"));
			coordinate.setCityName(adr.getString("subAdminArea"));
			coordinate.setCountyName(adr.getString("tertiaryAdminArea"));
			coordinate.setTownName(adr.getString("subLocality"));
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
	public Coordinate geo(String address, String city){
		Coordinate coordinate = new Coordinate();
		coordinate.setAddress(address);
		if(null != address){
			address = address.replace(" ","");
		}
		String api = "/mapApi/v1/siteService/geocode";
		Map<String, Object> params = new HashMap<>();
		params.put("address", address);
		DataRow row = post(HuaweiMapConfig.DEFAULT_HOST, api, params);
		coordinate.setMetadata(row);
		DataSet<DataRow> set = null;
		if(row.containsKey("sites")){
			set = row.getSet("sites");
			if(!set.isEmpty()){
				DataRow first = set.getRow(0);
				parse(coordinate, first);
				coordinate.setSrs(SRS.GCJ02LL);
			}
		}else{
			log.warn("[坐标查询失败][info:{}][params:{}]",row.getString("info"),BeanUtil.map2json(params));
		}
		return coordinate;
	}

	@Override
	public Coordinate ip(String ip) {
		return null;
	}
	/**
	 * 附近poi https://lbs.qq.com/service/webService/webServiceGuide/search/webServiceSearch
	 * @param lng 经度
	 * @param lat 经度
	 * @param radius 半径
	 * @param category 类别
	 * @param keyword 关键词
	 * @return List
	 */
	@Override
	public List<Coordinate> poi(Double lng, Double lat, int radius, String category, String keyword) {
		List<Coordinate> coordinates = new ArrayList<>();
		String api = "/mapApi/v1/siteService/nearbySearch";

		Map<String, Object> params = new HashMap<>();
		if(BasicUtil.isNotEmpty(keyword)){
			params.put("query", keyword);
		}
		Map<String, Object> location = new HashMap<>();
		location.put("lng", lng);
		location.put("lat", lat);
		params.put("location", location);
		params.put("radius", radius);

		if(BasicUtil.isNotEmpty(category)){
			params.put("hwPoiTypes", category);
		}
		params.put("pageSize", 20);

		int page = 1;
		Map<String, Coordinate> maps = new HashMap<>();
		while (true){
			params.put("pageIndex", page++);
			DataRow row = post(HuaweiMapConfig.DEFAULT_HOST, api, params);
			if(null != row){
				DataSet<DataRow> set = row.getSet("sites");
				int exists = 0;
				if(null != set && !set.isEmpty()) {
					for(DataRow item:set){
						Coordinate coordinate = new Coordinate();
						parse(coordinate,item);
						if(maps.containsKey(coordinate.getId())){
							exists++;
						}else{
							coordinates.add(coordinate);
						}
						maps.put(coordinate.getId(), coordinate);
					}
					//最后一页小于20个
					if(set.size() < 25){
						break;
					}
					//有10个以上重复的中断(算成最后一页)
					if(exists > 10){
						break;
					}
				}else{
					break;
				}
			}else{
				break;
			}
		}
		return coordinates;
	}
	public DataRow post(String host, String api, Map<String, Object> params){
		DataRow row = null;
		String body = BeanUtil.map2json(params);
		StringEntity entity = null;
		try{
			entity = new StringEntity(body);
		}catch (Exception e){
			e.printStackTrace();
		}
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Accept", "application/json");
		HttpResponse response = HttpUtil.post(headers, host + api + "?key="+config.SECRET,"UTF-8", entity);
		int status = response.getStatus();
		if(status == 200) {
			String txt = response.getText();
			if(null != HuaweiMapConfig.CACHE_DIR){
				File dir = new File(HuaweiMapConfig.CACHE_DIR, config.SECRET+"/"+api.replace("/","_")+"/"+DateUtil.format("yyyyMMddHH"));
				File file = new File(dir, System.currentTimeMillis()+"_"+BasicUtil.getRandomString(8)+".txt");
				FileUtil.write(body+"\r\n"+txt, file);
			}
			row = DataRow.parseJson(txt);
			String code = row.getString("returnCode");
			if (!"0".equalsIgnoreCase(code)) {
				// [逆地理编码][执行失败][code:10044][info:USER_DAILY_QUERY_OVER_LIMIT]
				log.warn("[{}][执行失败][code:{}][info:{}]", api, row.getString("INFOCODE"), row.getString("INFO"));
				log.warn("[{}}][response:{}]", txt);
				String info_code = row.getString("INFOCODE");
				if ("010024".equals(info_code)) {
					last_limit = DateUtil.format("yyyy-MM-dd");
					throw new AnylineException("API_OVER_LIMIT", "访问已超出日访问量(或接口欠费)");
				} else if ("010037".equals(info_code)) {
					log.warn("并发量已达到上限,sleep 100 ...");
					try {
						Thread.sleep(100);
					} catch (Exception e) {
						e.printStackTrace();
					}
					return post(host, api, params);
				} else {
					throw new AnylineException(status, row.getString("returnDesc"));
				}
			}
		}else{
			throw new AnylineException(status, "api执行异常");
		}
		return row;
	}

}
