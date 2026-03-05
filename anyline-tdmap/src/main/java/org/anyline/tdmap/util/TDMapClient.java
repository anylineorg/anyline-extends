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

import org.anyline.client.map.AbstractMapClient;
import org.anyline.client.map.MapClient;
import org.anyline.entity.Coordinate;
import org.anyline.entity.DataRow;
import org.anyline.entity.DataSet;
import org.anyline.entity.geometry.Point;
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
 * 天了图
 * @author zh 
 * 
 */ 
public class TDMapClient extends AbstractMapClient implements MapClient {
	private static Log log = LogProxy.get(TDMapClient.class);
	public TDMapConfig config = null;
	private static Hashtable<String, TDMapClient> instances = new Hashtable<>();

	public static Hashtable<String, TDMapClient> getInstances(){
		return instances;
	}

	public TDMapConfig getConfig(){
		return config;
	}
	public static TDMapClient getInstance() {
		return getInstance("default");
	}

	public static TDMapClient getInstance(String key) {
		if (BasicUtil.isEmpty(key)) {
			key = "default";
		}
		TDMapClient util = instances.get(key);
		if (null == util) {
			TDMapConfig config = TDMapConfig.getInstance(key);
			if(null != config) {
				util = new TDMapClient();
				util.config = config;
				instances.put(key, util);
			}
		}
		return util;
	}


	@Override
	public Coordinate ip(String ip) {
		return null;
	}

	/**
	 * 附近poi http://lbs.tianditu.gov.cn/server/search2.html
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
		String api = "/v2/search";
///v2/search?postStr={%22keyWord%22:%22%E5%85%AC%E5%9B%AD%22,%22level%22:12,%22queryRadius%22:5000,%22pointLonlat%22:%22116.48016,39.93136%22,%22queryType%22:3,%22start%22:0,%22count%22:10}&type=query&tk=f232b5b5a4f8cac146f03860e8dcacf4
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
			DataRow row = get(TDMapConfig.DEFAULT_HOST, api, params);
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

	@Override
	public List<Coordinate> poi(Double lng, Double lat, int radius, String category) {
		return null;
	}

	@Override
	public List<Coordinate> poi(List<Point> points, String category) {
		return null;
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
