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


package org.anyline.baidu.map.util;

import org.anyline.client.map.AbstractMapClient;
import org.anyline.client.map.MapClient;
import org.anyline.entity.Coordinate;
import org.anyline.entity.DataRow;
import org.anyline.entity.DataSet;
import org.anyline.entity.SRS;
import org.anyline.exception.AnylineException;
import org.anyline.net.HttpResponse;
import org.anyline.net.HttpUtil;
import org.anyline.util.*;
import org.anyline.util.encrypt.MD5Util;
import org.anyline.log.Log;
import org.anyline.log.LogProxy;

import java.io.File;
import java.net.URLEncoder;
import java.util.*;

public class BaiduMapClient extends AbstractMapClient implements MapClient {
    private static Log log = LogProxy.get(BaiduMapClient.class);
    private static final String HOST = "https://api.map.baidu.com";

    public BaiduMapConfig config = null;
    private static Hashtable<String, BaiduMapClient> instances = new Hashtable<>();

    static {
        Hashtable<String, AnylineConfig> configs = BaiduMapConfig.getInstances();
        for(String key:configs.keySet()) {
            instances.put(key, getInstance(key));
        }
    }
    public static Hashtable<String, BaiduMapClient> getInstances() {
        return instances;
    }

    public BaiduMapConfig getConfig() {
        return config;
    }
    public static BaiduMapClient getInstance() {
        return getInstance("default");
    }

    public static BaiduMapClient getInstance(String key) {
        if (BasicUtil.isEmpty(key)) {
            key = "default";
        }
        BaiduMapClient client = instances.get(key);
        if (null == client) {
            BaiduMapConfig config = BaiduMapConfig.getInstance(key);
            if(null != config) {
                client = new BaiduMapClient();
                client.config = config;
                instances.put(key, client);
            }
        }
        return client;
    }

    /**
     * 通过IP地址获取其当前所在地理位置
     * @param ip ip
     * @return 坐标
     */
    @Override
    public Coordinate ip(String ip) {
        return null;
    }

    @Override
    public Coordinate geo(String address, String city) {
        String api = "/geocoding/v3/";

        Coordinate coordinate = new Coordinate();
        coordinate.setAddress(address);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("address", address);
        params.put("output", "json");
        DataRow row = api(api, params);
        if(null != row) {
            DataRow location = row.getRow("result","location");
            if(null != location) {
                coordinate.setLng(location.getString("lng"));
                coordinate.setLat(location.getString("lat"));
                coordinate.setSuccess(true);
            }
        }
        coordinate.correct();
        return coordinate;
    }
    @Override
    public Coordinate regeo(Coordinate coordinate) {
        String api = "/reverse_geocoding/v3/";

        SRS _type = coordinate.getSrs();
        Double _lng = coordinate.getLng();
        Double _lat = coordinate.getLat();
        coordinate.convert(SRS.BD09LL);
        coordinate.setSuccess(false);

        // 换回原坐标系
        coordinate.setLng(_lng);
        coordinate.setLat(_lat);
        coordinate.setSrs(_type);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("location",coordinate.getLat()+","+coordinate.getLng());
        params.put("extensions_town","true");
        params.put("output","json");

        DataRow row = api(api, params);
        if(null != row) {
            parse(coordinate, row);
            //解析附近poi
            DataSet<DataRow> pois = row.getSet("pois");
            List<Coordinate> poi_coordinates = new ArrayList<>();
            for (DataRow poi : pois) {
                Coordinate poi_coordinate = new Coordinate();
                poi(poi_coordinate, poi);
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
        coordinate.correct();
        return coordinate;
    }
    /**
     *
     * @param city 城市 或 区县
     * @param category 类别(中文)
     * @param keyword 关键定
     * @return List
     */
    public List<Coordinate> poi(String city, String category, String keyword) {
        List<Coordinate> coordinates = new ArrayList<>();
        String api = "/place/v3/region";

        Map<String, Object> params = new HashMap<>();
        if(BasicUtil.isNotEmpty(keyword)) {
            params.put("query", keyword);
        }
        if(BasicUtil.isNotEmpty(category)) {
            params.put("type", category);
        }
        params.put("page_size", 20);
        params.put("region", city);
        params.put("extensions", "all");
        params.put("scope", 2);//详细POI

        int page = 0; //从0开始
        Map<String, Coordinate> maps = new HashMap<>();
        int pages = 0;
        while (pages == 0 || page <= pages) {
            params.put("page_num", page++);
            DataRow row = api(api, params);
            if(null == row) {
                break;
            }
            int total = row.getInt("total");
            pages = (total-1)/20+1;
            DataSet<DataRow> set = row.getSet("results");
            if(null == set || set.isEmpty()) {
                break;
            }
            for(DataRow item:set) {
                Coordinate coordinate = new Coordinate();
                parse(coordinate, item);
                if(!maps.containsKey(coordinate.getId())) {
                    coordinates.add(coordinate);
                }
                maps.put(coordinate.getId(), coordinate);
            }
        }
        log.warn("[查询结果:{}]", coordinates.size());
        return coordinates;
    }
    private Coordinate poi(Coordinate coordinate, DataRow row) {
        coordinate.setAddress(row.getString("addr"));
        coordinate.setTitle(row.getString("name"));
        coordinate.setPoiCategoryName(row.getString("tag"));
        coordinate.setTel(row.getString("tel"));
        coordinate.setId(row.getString("uid"));
        DataRow point = row.getRow("point");
        if(null != point) {
            coordinate.setLng(point.getDouble("x"));
            coordinate.setLat(point.getDouble("y"));
        }
        coordinate.setSuccess(true);
        coordinate.setMetadata(row);
        return coordinate;
    }

    private Coordinate parse(Coordinate coordinate, DataRow row) {
        coordinate.setId(row.getString("uid"));
        coordinate.setTitle(row.getString("name"));

        DataRow point = row.getRow("location");
        if(null != point) {
            coordinate.setLng(point.getDouble("lng"));
            coordinate.setLat(point.getDouble("lat"));
        }
        String address = row.getString("address");
        if(BasicUtil.isEmpty(address)) {
            address = row.getString("formatted_address");
        }
        coordinate.setAddress(address);
        coordinate.setTel(row.getString("telephone"));
        DataRow adr = row.getRow("result","addressComponent");
        if(null == adr){
            adr = row;
        }
        String adcode = adr.getString("adcode");
        String provinceCode = adcode.substring(0,2);
        String cityCode = adcode.substring(0,4);
        coordinate.setProvinceCode(provinceCode);
        coordinate.setProvinceName(adr.getString("province"));
        coordinate.setCityCode(cityCode);
        coordinate.setCityName(adr.getString("city"));
        coordinate.setCountyName(adr.getString("district"));
        coordinate.setCountyCode(adr.getString("adcode"));
        coordinate.setTownCode(adr.getString("town_code"));
        coordinate.setTownName(adr.getString("town"));

        String street = adr.getString("street");
        coordinate.setStreet(street);
        String number = adr.getString("street_number");
        if(null != number && null != street) {
            number = number.replace(street,"");
        }
        coordinate.setStreet(street);
        coordinate.setStreetNumber(number);
        coordinate.setSuccess(true);
        coordinate.setMetadata(row);
        return coordinate;
    }

    private DataRow api(String api, Map<String, Object> params) {
        if(limit()) {
            return null;
        }
        DataRow row = null;
        sign(api, params);
        HttpResponse response = HttpUtil.get(HOST + api,"UTF-8", params);
        int status = response.getStatus();
        if(status == 200) {
            String txt = response.getText();
            if(null != BaiduMapConfig.CACHE_DIR) {
                File dir = new File(BaiduMapConfig.CACHE_DIR, config.AK+"/"+api.replace("/","_")+"/"+ DateUtil.format("yyyyMMddHH"));
                File file = new File(dir, System.currentTimeMillis()+"_"+BasicUtil.getRandomString(8)+".txt");
                FileUtil.write(BeanUtil.map2string(params) + "\r\n" + txt, file);
            }
            row = DataRow.parseJson(txt);
            if(null != row) {
                status = row.getInt("status",-1);
                if(status != 0) {
                    log.warn("[{}][执行失败][status:{}][info:{}]", api , status, row.getString("message"));
                    log.warn("[{}][response:{}]", api, txt);
                    if (302 ==status) {
                        last_limit = DateUtil.format("yyyy-MM-dd");
                        throw new AnylineException("API_OVER_LIMIT", "访问已超出日访问量");
                    } else if (401 == status || 402 == status) {
                        try {
                            log.warn("并发量已达到上限,sleep 100 ...");
                            Thread.sleep(100);
                            api(api, params);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        throw new AnylineException(status, row.getString("message"));
                    }
                }
            }
        }
        return row;
    }
    public void sign(String api, Map<String, Object> params) {
        params.put("ak", config.AK);
        try {
            for (String key : params.keySet()) {
                String value = (String)params.get(key);
                value = URLEncoder.encode(value, "UTF-8");
                params.put(key, value);
            }
            String str = api + "?" + BeanUtil.map2string(params) + config.SK;
            str = URLEncoder.encode(str, "UTF-8");
            params.put("sn", MD5Util.crypto(str));
        }catch (Exception e) {
            e.printStackTrace();
        }

    }



}
