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
import org.anyline.util.encrypt.MD5Util;

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
		String api = "/v3/geocode/regeo";
		Map<String, Object> params = new HashMap<>();
		params.put("key", config.KEY); 
		params.put("location", _lng + "," +_lat);

		Map<String, Object> ps = coordinate.getParams();
		if(null != ps){
			if(ps.containsKey("poitype")){
				params.put("poitype", ps.get("poitype"));
			}
			if(ps.containsKey("radius")){
				params.put("radius", ps.get("radius"));
			}
			if(ps.containsKey("extensions")){
				params.put("extensions", ps.get("extensions"));
			}
			if(ps.containsKey("homeorcorp")){
				params.put("homeorcorp", ps.get("homeorcorp"));
			}
			if(ps.containsKey("roadlevel")){
				params.put("roadlevel", ps.get("roadlevel"));
			}
		}

		row = get(HuaweiMapConfig.DEFAULT_HOST, api, params);
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
	private void parse(Coordinate coordinate, DataRow result){
		coordinate.setMetadata(result);
		coordinate.setId(result.getString("id"));
		coordinate.setTitle(result.getString("name"));
		Object tel = result.get("tel");
		if(tel instanceof Collection){
			if(!((Collection)tel).isEmpty()){
				coordinate.setTel(tel.toString());
			}
		}else if(tel instanceof String){
			coordinate.setTel((String) tel);
		}
		String ad_code = result.getString("adcode");
		if(null != ad_code){
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
		if(null != location && location.contains(",")){
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
	public Coordinate geo(String address, String city){
		Coordinate coordinate = new Coordinate();
		coordinate.setAddress(address);
		if(null != address){
			address = address.replace(" ","");
		}
		String api = "/v3/geocode/geo";
		Map<String, Object> params = new HashMap<>(); 
		params.put("key", config.KEY); 
		params.put("address", address); 
		if(BasicUtil.isNotEmpty(city)){
			params.put("city", city); 
		}
		DataRow row = get(HuaweiMapConfig.DEFAULT_HOST, api, params);
		coordinate.setMetadata(row);
		DataSet<DataRow> set = null;
		if(row.containsKey("geocodes")){
			set = row.getSet("geocodes");
			if(set.size()>0){
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
				if(null != number && null != street){
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
	 * http://lbs.HuaweiMap.com/api/webservice/guide/api/direction#driving			
	 * @param origin		出发地  origin		出发地
	 * @param destination	目的地  destination	目的地
	 * @param points		途经地 最多支持16个 坐标点之间用";"分隔 
	 * @param strategy		选路策略  0,不考虑当时路况,返回耗时最短的路线,但是此路线不一定距离最短 
	 *							  1,不走收费路段,且耗时最少的路线 
	 *							  2,不考虑路况,仅走距离最短的路线,但是可能存在穿越小路/小区的情况 			   
	 * @return DataRow
	 */ 
	@SuppressWarnings({"rawtypes", "unchecked" })
	public DataRow directionDrive(String origin, String destination, String points, int strategy){
		DataRow row = null; 
		String url = "http://restapi.HuaweiMap.com/v3/direction/driving"; 
		Map<String, Object> params = new HashMap<>(); 
		params.put("key", config.KEY); 
		params.put("origin", origin); 
		params.put("destination", destination); 
		params.put("strategy", strategy+""); 
		if(BasicUtil.isNotEmpty(points)){
			params.put("points", points); 
		} 
		String sign = sign(params); 
		params.put("sig", sign); 
		String txt = HttpUtil.get(url, "UTF-8", params).getText(); 
		try{
			row = DataRow.parseJson(txt); 
			DataRow route = row.getRow("route"); 
			if(null != route){
				List paths = route.getList("PATHS"); 
				if(paths.size()>0){
					DataRow path = (DataRow)paths.get(0); 
					row = path; 
					List<DataRow> steps = (List<DataRow>)path.getList("steps"); 
					List<String> polylines = new ArrayList<>();
					for(DataRow step:steps){
						String polyline = step.getString("polyline"); 
						String[] tmps = polyline.split(";"); 
						for(String tmp:tmps){
							polylines.add(tmp); 
						} 
					} 
					row.put("polylines", polylines); 
				} 
			} 
		}catch(Exception e){
			log.warn("[线路规划失败][error:{}]",e.toString()); 
		} 
		return row; 
	} 
	public DataRow directionDrive(String origin, String destination){
		return directionDrive(origin, destination, null, 0); 
	} 
	public DataRow directionDrive(String origin, String destination, String points){
		return directionDrive(origin, destination, points, 0); 
	}
	public DataSet<DataRow> poi(String city, String keywords){
		DataSet<DataRow> set = new DataSet();
		String url = "https://restapi.HuaweiMap.com/v5/place/text";
		Map<String, Object> params = new HashMap<>();
		params.put("city", city);
		params.put("keywords", keywords);
		params.put("page","1");
		params.put("offset","20");
		DataRow row = get(HuaweiMapConfig.DEFAULT_HOST, url,params);
		if(row.getInt("status",0)==1){
			List<DataRow> items = (List<DataRow>)row.get("POIS");
			for(DataRow item:items){
				set.add(item);
			}
		}
		return set;
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
		if(BasicUtil.isNotEmpty(keyword)){
			params.put("keyword", keyword);
		}
		params.put("location", lng + "," + lat);
		params.put("radius", radius);
		//filter=category= 241000,241100
		if(BasicUtil.isNotEmpty(category)){
			params.put("types", category);
		}
		params.put("extensions", "all");
		params.put("offset", 25);
		params.put("key", config.KEY);
		int page = 1;
		Map<String, Coordinate> maps = new HashMap<>();
		while (true){
			params.put("page", page++);
			DataRow row = get(HuaweiMapConfig.DEFAULT_HOST, api, params);
			if(null != row){
				DataSet<DataRow> set = row.getSet("pois");
				int exists = 0;
				if(null != set && !set.isEmpty()) {
					for(DataRow item:set){
						Coordinate coordinate = poi(item);
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
	private Coordinate poi(DataRow row){
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
	public DataRow get(String host, String api, Map<String, Object> params){
		DataRow row = null;
		sign(params);
		/*try {
			for (String key : params.keySet()) {
				Object value = params.get(key);
				if(value instanceof String) {
					params.put(key, URLEncoder.encode(value.toString(), "UTF-8"));
				}
			}
		}catch (Exception e){
			e.printStackTrace();
		}*/
		HttpResponse response = HttpUtil.get(host + api,"UTF-8", params);
		int status = response.getStatus();
		if(status == 200) {
			String txt = response.getText();
			row = DataRow.parseJson(txt);
			if(null == row){
				throw new AnylineException(status, row.getString("INFO"));
			}
			status = row.getInt("STATUS", 0);
			if (status == 0) {
				// [逆地理编码][执行失败][code:10044][info:USER_DAILY_QUERY_OVER_LIMIT]
				log.warn("[{}][执行失败][code:{}][info:{}]", api, row.getString("INFOCODE"), row.getString("INFO"));
				log.warn("[{}}][response:{}]", txt);
				String info_code = row.getString("INFOCODE");
				if ("10044".equals(info_code)) {
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
		}else{
			throw new AnylineException(status, "api执行异常");
		}
		return row;
	}
	/** 
	 * 签名 
	 * @param params  params
	 * @return String
	 */ 
	public String sign(Map<String, Object> params){
		params.remove("sig");
		String sign = ""; 
		sign = BeanUtil.map2string(params) + config.SECRET;
		sign = MD5Util.sign(sign,"UTF-8");
		params.put("sig", sign);
		return sign; 
	}

}
