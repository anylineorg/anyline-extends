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
import org.anyline.log.Log;
import org.anyline.log.LogProxy;
import org.anyline.net.HttpResponse;
import org.anyline.net.HttpUtil;
import org.anyline.util.*;
import org.anyline.util.encrypt.MD5Util;

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

    /**
     *
     * <a href="https://lbsyun.baidu.com/faq/api?title=webapi/guide/webservice-geocoding-base">参考</a>
     * @param address  地址
     * @param city  城市
     * @return 坐标
     */
    @Override
    public Coordinate geo(String address, String city) {
        String api = "/geocoding/v3/";

        Coordinate coordinate = new Coordinate();
        coordinate.setAddress(address);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("address", address);
        if(BasicUtil.isNotEmpty(city)) {
            params.put("city", city);
        }
        params.put("extension_poi_infos", true);
        Map<String, Object> ps = coordinate.getParams();
        SRS output_srs = null;
        if(null != ps){
            //返回的坐标类型，可选参数，添加后返回国测局经纬度坐标或百度米制坐标 坐标系说明
            if(ps.containsKey("ret_coordtype")){
                output_srs = SRS.valueOf(ps.get("ret_coordtype").toString().toUpperCase());
            }
            //是否触发解析到最小地址结构功能
            if (ps.containsKey("extension_analys_level")){
                params.put("extension_analys_level", ps.get("extension_analys_level"));
            }
            //是否返回经纬度所在的地址信息
            if (ps.containsKey("extension_poi_infos")){
                params.put("extension_poi_infos", ps.get("extension_poi_infos"));
            }
        }

        if(null != output_srs){
            output_srs = BaiduMapConfig.DEFAULT_OUTPUT_SRS;
        }
        if(null != output_srs){
            params.put("ret_coordtype", output_srs.name().toLowerCase());
        }

        params.put("output", "json");
        DataRow row = api(api, METHOD.GEO, params);
        if(null != row) {
            row = row.getRow("result");
            parse(coordinate, row);
        }
        coordinate.setSrs(output_srs);
        coordinate.correct();
        return coordinate;
    }
    @Override
    public Coordinate regeo(Coordinate coordinate) {
        String api = "/reverse_geocoding/v3/";

        SRS input_srs = null;
        SRS output_srs = null; //输出
        Double _lng = coordinate.getLng();
        Double _lat = coordinate.getLat();
        //coordinate.convert(SRS.BD09LL);
        coordinate.setSuccess(false);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("location", coordinate.getLat()+","+coordinate.getLng());
        params.put("extensions_town","true");
        params.put("output","json");
        Map<String, Object> ps = coordinate.getParams();
        params.put("extensions_poi",1);
        params.put("entire_poi",1);
        if(null != ps){
            if(ps.containsKey("poi_types")){
                params.put("poi_types", ps.get("poi_types"));
            }
            if(ps.containsKey("radius")){
                params.put("radius", ps.get("radius"));
            }
            if(ps.containsKey("entire_poi")){
                params.put("entire_poi", ps.get("entire_poi"));
            }
            if(ps.containsKey("coordtype")){
                input_srs = SRS.valueOf(ps.get("coordtype").toString().toUpperCase());
            }

            if(ps.containsKey("ret_coordtype")){
                output_srs = SRS.valueOf(ps.get("ret_coordtype").toString().toUpperCase());
            }

            if(ps.containsKey("sort_strategy")){
                params.put("sort_strategy", ps.get("sort_strategy"));
            }
        }
        if(null == input_srs) {
            input_srs = coordinate.getSrs();
        }
        if(null == input_srs) {
            input_srs = BaiduMapConfig.DEFAULT_INPUT_SRS;
        }
        if(null == output_srs) {
            output_srs = BaiduMapConfig.DEFAULT_OUTPUT_SRS;
        }
        if(null != input_srs) {
            params.put("coordtype", input_srs.name().toLowerCase());
        }
        if(null != output_srs) {
            params.put("ret_coordtype", output_srs.name().toLowerCase());
        }

        DataRow row = api(api, METHOD.RE_GEO, params);
        if(null != row) {
            row = row.getRow("result");
            parse(coordinate, row);
            coordinate.setSrs(output_srs);
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
                poi_coordinate.setSrs(output_srs);
                poi_coordinates.add(poi_coordinate);
            }
            coordinate.setPois(poi_coordinates);
        }
        coordinate.correct();
        return coordinate;
    }
    /**
     *
     * @param district 行政区 一般支持到区县级
     * @param category 类别(中文)
     * @param keyword 关键定
     * @return List
     */
    public List<Coordinate> poi(String district, String category, String keyword) {
        List<Coordinate> coordinates = new ArrayList<>();
        String api = "/place/v3/region";
        int page = 0; //从0开始
        int vol = 20;
        Map<String, Coordinate> maps = new HashMap<>();
        int pages = 0;
        while (pages == 0 || page <= pages) {
            Map<String, Object> params = new HashMap<>();
            if(BasicUtil.isNotEmpty(keyword)) {
                params.put("query", keyword);
            }
            if(BasicUtil.isNotEmpty(category)) {
                params.put("type", category);
            }
            SRS input_srs = BaiduMapConfig.DEFAULT_INPUT_SRS;
            SRS output_srs = BaiduMapConfig.DEFAULT_OUTPUT_SRS;
            if(null != input_srs) {
                params.put("coord_type", input_srs.name().toLowerCase());
            }
            if(null != output_srs) {
                params.put("ret_coordtype", output_srs.name().toLowerCase());
            }
            params.put("page_size", vol);
            params.put("region", district);
            params.put("extensions", "all");
            params.put("scope", 2);//详细POI
            params.put("page_num", page++);
            DataRow row = api(api, METHOD.SEARCH, params);
            if(null == row) {
                break;
            }
            int total = row.getInt("total");
            pages = (total-1)/vol+1;
            DataSet<DataRow> set = row.getSet("results");
            if(null == set || set.isEmpty()) {
                break;
            }
            for(DataRow item:set) {
                Coordinate coordinate = new Coordinate();
                parse(coordinate, item);
                coordinate.setSrs(output_srs);
                if(BasicUtil.isEmpty(coordinate.getId())){
                    //返回了各地区统计数量
                    continue;
                }
                if(!maps.containsKey(coordinate.getId())) {
                    coordinates.add(coordinate);
                    maps.put(coordinate.getId(), coordinate);
                }
            }
            if(coordinates.isEmpty()){
                break;
            }
        }
        log.warn("[查询结果:{}]", coordinates.size());
        return coordinates;
    }

    /**
     * 附近poi
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
        String api = "/place/v3/around";
        Map<String, Object> params = new HashMap<>();

        int page = 0; //从0开始
        int vol = 20;
        Map<String, Coordinate> maps = new HashMap<>();
        int pages = 0;
        while (pages == 0 || page <= pages) {
            if(BasicUtil.isNotEmpty(keyword)) {
                params.put("query", keyword);
            }

            SRS input_srs = BaiduMapConfig.DEFAULT_INPUT_SRS;
            SRS output_srs = BaiduMapConfig.DEFAULT_OUTPUT_SRS;

            if(null != input_srs) {
                params.put("coord_type", input_srs.name().toLowerCase());
            }
            if(null != output_srs) {
                params.put("ret_coordtype", output_srs.name().toLowerCase());
            }

            params.put("location", lat + "," + lng);
            params.put("radius", radius);
            //filter=category= 241000,241100
            if(BasicUtil.isNotEmpty(category)) {
                params.put("type", category);
            }
            params.put("extensions_adcode", true);
            params.put("page_size", vol);
            params.put("page_num", page++);
            DataRow row = api(api, METHOD.SEARCH, params);
            if(null == row) {
                break;
            }
            int total = row.getInt("total");
            pages = (total-1)/vol+1;
            DataSet<DataRow> set = row.getSet("results");
            if(null == set || set.isEmpty()) {
                break;
            }
            for(DataRow item:set) {
                Coordinate coordinate = new Coordinate();
                parse(coordinate, item);
                coordinate.setSrs(output_srs);
                if(BasicUtil.isEmpty(coordinate.getId())){
                    //返回了各地区统计数量
                    continue;
                }
                if(!maps.containsKey(coordinate.getId())) {
                    coordinates.add(coordinate);
                }
                maps.put(coordinate.getId(), coordinate);
            }
            if(coordinates.isEmpty()){
                break;
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

        DataRow location = row.getRow("location");
        if(null != location) {
            coordinate.setLng(location.getDouble("lng"));
            coordinate.setLat(location.getDouble("lat"));
        }
        String address = row.getString("address");
        if(BasicUtil.isEmpty(address)) {
            address = row.getString("formatted_address");
        }
        coordinate.setAddress(address);
        coordinate.setTel(row.getString("telephone"));
        DataRow adr = row.getRow("result","addressComponent");
        if(null == adr) {
            adr = row.getRow("addressComponent");
        }
        if(null == adr){
            adr = row;
        }
        String town_code = adr.getString("town_code");
        coordinate.setTownCode(town_code);
        String adcode = adr.getString("adcode");
        if(null == adcode && null != town_code) {
            adcode = town_code.substring(0, 6);
        }
        if (null != adcode && adcode.length() > 4) {
            String provinceCode = adcode.substring(0,2);
            String cityCode = adcode.substring(0,4);
            coordinate.setProvinceCode(provinceCode);
            coordinate.setCityCode(cityCode);
        }
        coordinate.setProvinceName(adr.getString("province"));
        coordinate.setCityName(adr.getString("city"));
        coordinate.setCountyName(adr.getString("area"));
        coordinate.setCountyCode(adcode);
        coordinate.setTownCode(adr.getString("town_code"));
        coordinate.setTownName(adr.getString("town"));

        String street = adr.getString("street");
        coordinate.setStreet(street);
        String number = adr.getString("street_number");
        if(null != number && null != street) {
            number = number.replace(street,"");
        }
        DataRow detail_info = row.getRow("detail_info");
        if(null != detail_info) {
            coordinate.setPoiCategoryName(detail_info.getString("classified_poi_tag"));
        }
        coordinate.setStreet(street);
        coordinate.setStreetNumber(number);
        coordinate.setSuccess(true);
        coordinate.setMetadata(row);
        return coordinate;
    }

    private DataRow api(String api, METHOD  method, Map<String, Object> params) {
        if(limit(method)) {
            return null;
        }
        File file = null;
        String txt = null;
        DataRow row = null;
        String json = BeanUtil.map2json(params);
        if(null != BaiduMapConfig.CACHE_DIR){
            String md5 = MD5Util.crypto(api + json);
            File dir = new File(BaiduMapConfig.CACHE_DIR, config.AK+"/"+api.replace("/","_")+"/"+ DateUtil.format("yyyyMMddHH"));
            file = new File(dir, md5+".txt");
            if(file.exists() && BaiduMapConfig.READ_CACHE) {
                txt = FileUtil.read(file, "UTF-8").toString().replace(json, "").trim();
            }
        }
        if(BasicUtil.isEmpty(txt)){
            sign(api, params);
            HttpResponse response = HttpUtil.get(HOST + api,"UTF-8", params);
            int status = response.getStatus();
            if(status == 200){
                txt = response.getText();
                if(null != BaiduMapConfig.CACHE_DIR) {
                    FileUtil.write(json + "\r\n" + txt, file);
                }
            }
        }
        if(BasicUtil.isNotEmpty(txt)) {
            row = DataRow.parseJson(txt);
            if (null != row) {
                int status = row.getInt("status", -1);
                if (status != 0) {
                    log.warn("[{}][执行失败][status:{}][info:{}]", api, status, row.getString("message"));
                    log.warn("[{}][response:{}]", api, txt);
                    if (302 == status) {
                        limit(method, DateUtil.format("yyyy-MM-dd"));
                        throw new AnylineException("API_OVER_LIMIT", "访问已超出日访问量");
                    } else if (401 == status || 402 == status) {
                        try {
                            log.warn("并发量已达到上限,sleep 100 ...");
                            Thread.sleep(100);
                            api(api, method, params);
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
        params.remove("sn");
        try {
            for (String key : params.keySet()) {
                Object value = params.get(key);
                if(value instanceof String) {
                    value = URLEncoder.encode(value.toString(), "UTF-8");
                    params.put(key, value);
                }
            }
            String str = api + "?" + BeanUtil.map2string(params) + config.SK;
            str = URLEncoder.encode(str, "UTF-8");
            params.put("sn", MD5Util.crypto(str));
        }catch (Exception e) {
            e.printStackTrace();
        }

    }



}
