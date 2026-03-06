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


package org.anyline.qq.map.util;

import org.anyline.client.map.AbstractMapClient;
import org.anyline.client.map.MapClient;
import org.anyline.entity.Coordinate;
import org.anyline.entity.DataRow;
import org.anyline.entity.DataSet;
import org.anyline.entity.SRS;
import org.anyline.entity.geometry.Point;
import org.anyline.entity.geometry.Ring;
import org.anyline.exception.AnylineException;
import org.anyline.log.Log;
import org.anyline.log.LogProxy;
import org.anyline.net.HttpResponse;
import org.anyline.net.HttpUtil;
import org.anyline.util.AnylineConfig;
import org.anyline.util.BasicUtil;
import org.anyline.util.BeanUtil;
import org.anyline.util.encrypt.MD5Util;

import java.util.*;

public class QQMapClient extends AbstractMapClient implements MapClient {
    private static Log log = LogProxy.get(QQMapClient.class);
    public QQMapConfig config = null;
    private static Hashtable<String, QQMapClient> instances = new Hashtable<>();

    static {
        Hashtable<String, AnylineConfig> configs = QQMapConfig.getInstances();
        for(String key:configs.keySet()){
            instances.put(key, getInstance(key));
        }
    }
    public static Hashtable<String, QQMapClient> getInstances(){
        return instances;
    }
    public QQMapConfig getConfig(){
        return config;
    }
    public static QQMapClient getInstance() {
        return getInstance("default");
    }

    public static QQMapClient getInstance(String key) {
        if (BasicUtil.isEmpty(key)) {
            key = "default";
        }
        QQMapClient client = instances.get(key);
        if (null == client) {
            QQMapConfig config = QQMapConfig.getInstance(key);
            if(null != config) {
                client = new QQMapClient();
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
        Coordinate coordinate = null;
        String api = "/ws/location/v1/ip";
        Map<String, Object> params = new HashMap<>();
        params.put("ip", ip);
        params.put("key", config.KEY);
        DataRow row = api(api, params);
        if(null != row){
            coordinate = new Coordinate();
            coordinate.setMetadata(row);
            DataRow result = row.getRow("result");
            if(null != result) {
                DataRow point = result.getRow("location");
                if(null != point){
                    coordinate.setLng(point.getDouble("lng", -1.0));
                    coordinate.setLat(point.getDouble("lat", -1.d));
                }
                DataRow ad = result.getRow("ad_info");
                if(null != ad){
                    coordinate.setProvinceName(ad.getString("province"));
                    coordinate.setCityName(ad.getString("city"));
                    coordinate.setCountyName(ad.getString("district"));
                    coordinate.setCountyCode(ad.getString("adcode"));
                }
            }
        }
        if(null != coordinate) {
            coordinate.setSrs(SRS.GCJ02LL);
        }
        if(null != coordinate) {
            coordinate.correct();
        }
        return coordinate;
    }

    /**
     * 根据地址解析 坐标
     * https://lbs.qq.com/service/webService/webServiceGuide/webServiceGeocoder
     * @param address 地址 用原文签名 用url encode后提交
     * @param city 城市(没有用到可以不传)
     * @return Coordinate
     */
    @Override
    public Coordinate geo(String address, String city){
        Coordinate coordinate = new Coordinate();
        coordinate.setAddress(address);
        if(null != address){
            address = address.replace(" ","");
            if(null != city && !address.contains(city)){
                address = city + address;
            }
        }
        String api = "/ws/geocoder/v1";
        Map<String, Object> params = new HashMap<>();
        params.put("address", address);
        DataRow row = api(api, params);
        if(null != row){
            DataRow result = row.getRow("result");
            DataRow location = row.getRow("result","location");
            if(null != location){
                coordinate.setLat(location.getString("lat"));
                coordinate.setLng(location.getString("lng"));
            }

            DataRow adr = row.getRow("result","address_components");
            if(null != adr) {
                coordinate.setProvinceName(adr.getString("province"));
                coordinate.setCityName(adr.getString("city"));
                coordinate.setCountyName(adr.getString("district"));
                String street = adr.getString("street");
                coordinate.setStreet(street);
                String number = adr.getString("street_number");
                if(null != number && null != street){
                    number = number.replace(street,"");
                }
                coordinate.setStreetNumber(number);
            }
            adr = row.getRow("result","ad_info");
            if(null != adr) {
                String adcode = adr.getString("adcode");
                String provinceCode = adcode.substring(0,2);
                String cityCode = adcode.substring(0,4);
                coordinate.setProvinceCode(provinceCode);
                coordinate.setCityCode(cityCode);
                coordinate.setCountyCode(adcode);
            }
            coordinate.setReliability(result.getInt("reliability",0));
            coordinate.setAccuracy(result.getInt("level",0));
            coordinate.setSuccess(true);
            coordinate.setMetadata(row);

        }
        if(null != coordinate) {
            coordinate.correct();
        }
        return coordinate;
    }
    /**
     * 逆地址解析 根据坐标返回详细地址及各级地区编号
     * https://lbs.qq.com/service/webService/webServiceGuide/webServiceGcoder
     * @param coordinate 坐标
     * @return Coordinate
     */
    @Override
    public Coordinate regeo(Coordinate coordinate){
        String api = "/ws/geocoder/v1";

        SRS _type = coordinate.getSrs();
        Double _lng = coordinate.getLng();
        Double _lat = coordinate.getLat();

        coordinate.convert(SRS.GCJ02LL);
        coordinate.setSuccess(false);

        Map<String, Object> params = new HashMap<>();
        params.put("location", _lat +"," + _lng);        // 这里是纬度在前
        Map<String, Object> ps = coordinate.getParams();
        if(null != ps){
            if(ps.containsKey("radius")){
                params.put("radius", ps.get("radius"));
            }
            if(ps.containsKey("get_poi")){
                params.put("get_poi", ps.get("get_poi"));
            }
            if(ps.containsKey("poi_options")){
                params.put("poi_options", ps.get("poi_options"));
            }
        }
        DataRow row = api(api, params);
        if(null != row){
            DataRow result = row.getRow("result");
            if(null != result) {
                parse(coordinate, result);
                //附近poi
                DataSet<DataRow> pois = result.getSet("pois");
                if(null != pois){
                    List<Coordinate> poi_coordinates = new ArrayList<>();
                    for(DataRow poi : pois){
                        Coordinate poi_coordinate = new Coordinate();
                        parse(poi_coordinate, poi);
                        poi_coordinates.add(poi_coordinate);
                    }
                    coordinate.setPois(poi_coordinates);
                }
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
        coordinate.setAddress(result.getString("address"));
        coordinate.setId(result.getString("id")); //poi时有id值
        DataRow address_component = result.getRow("address_component");
        if(null != address_component) {
            coordinate.setProvinceName(address_component.getString("province"));
            coordinate.setCityName(address_component.getString("city"));
            coordinate.setCountyName(address_component.getString("district"));

            String street = address_component.getString("street");
            coordinate.setStreet(street);
            String number = address_component.getString("street_number");
            if(null != number && null != street){
                number = number.replace(street,"");
            }
        }
        DataRow ad_info = result.getRow("ad_info");
        if(null != ad_info) {
            String adcode = ad_info.getString("adcode");
            if(BasicUtil.isNotEmpty(adcode)) {
                String provinceCode = adcode.substring(0, 2);
                String cityCode = adcode.substring(0, 4);
                coordinate.setProvinceCode(provinceCode);
                coordinate.setCityCode(cityCode);
                coordinate.setCountyCode(adcode);
            }
            String provinceName = ad_info.getString("province");
            if(BasicUtil.isNotEmpty(provinceName)) {
                coordinate.setProvinceName(provinceName);
            }
            String cityName = ad_info.getString("city");
            if(BasicUtil.isNotEmpty(cityName)) {
                coordinate.setCityName(cityName);
            }
            String districtName = ad_info.getString("district");
            if(BasicUtil.isNotEmpty(districtName)) {
                coordinate.setCountyName(districtName);
            }
        }
        //相对参考
        DataRow address_reference = result.getRow("address_reference","town");
        if(null != address_reference){
            coordinate.setTownCode(address_reference.getString("id"));
            coordinate.setTownName(address_reference.getString("title"));
        }
        DataRow location = result.getRow("location");
        if(null != location){
            coordinate.setLng(location.getString("lng"));
            coordinate.setLat(location.getString("lat"));
        }
        coordinate.setTitle(result.getString("title"));
        coordinate.setTel(result.getString("tel"));
        coordinate.setPoiCategoryCode(result.getString("category_code"));
        coordinate.setPoiCategoryName(result.getString("category"));

        coordinate.correct();
    }
    /**
     * 附近poi https://lbs.qq.com/service/webService/webServiceGuide/search/webServiceSearch
     * @param city 城市
     * @param category 类别
     * @param keyword 关键词
     * @return List
     */
    @Override
    public List<Coordinate> poi(String city, String category, String keyword) {
        List<Coordinate> coordinates = new ArrayList<>();
        String api = "/ws/place/v1/search";

        Map<String, Object> params = new HashMap<>();
        if(BasicUtil.isNotEmpty(keyword)){
            params.put("keyword", keyword);
        }
        String boundary = "region("+city+",0)";
        params.put("boundary", boundary);
        //filter=category= 241000,241100
        if(BasicUtil.isNotEmpty(category)){
            String filter = "category="+category;
            params.put("filter", filter);
        }
        params.put("page_size", 20);
        params.put("added_fields", "category_code");
        int page = 1;
        Map<String, Coordinate> maps = new HashMap<>();
        while (true){
            params.put("page_index", page++);
            DataRow row = api(api, params);
            if(null == row){
                break;
            }
            DataSet<DataRow> set = row.getSet("data");
            int exists = 0;
            if(null == set || set.isEmpty()){
                break;
            }
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
            if(set.size() < 20){
                break;
            }
            //有10个以上重复的中断(算成最后一页)
            if(exists > 10){
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
     * @param keyword 关键词
     * @return List
     */
    @Override
    public List<Coordinate> poi(Double lng, Double lat, int radius, String category, String keyword) {
        List<Coordinate> coordinates = new ArrayList<>();
        String api = "/ws/place/v1/search";

        Map<String, Object> params = new HashMap<>();
        if(BasicUtil.isNotEmpty(keyword)){
            params.put("keyword", keyword);
        }
        String boundary = "nearby("+lng+","+lat+","+radius+",1)";
        params.put("boundary", boundary);
        //filter=category= 241000,241100
        if(BasicUtil.isNotEmpty(category)){
            String filter = "category="+category;
            params.put("filter", filter);
        }
        params.put("page_size", 20);
        params.put("added_fields", "category_code");
        int page = 1;
        Map<String, Coordinate> maps = new HashMap<>();
        while (true){
            params.put("page_index", page++);
            DataRow row = api(api, params);
            if(null == row){
                break;
            }
            DataSet<DataRow> set = row.getSet("data");
            int exists = 0;
            if(null == set || set.isEmpty()){
                break;
            }
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
            if(set.size() < 20){
                break;
            }
            //有10个以上重复的中断(算成最后一页)
            if(exists >= set.size()-1 || exists > 10){
                break;
            }
        }
        log.warn("[查询结果:{}]", coordinates.size());
        return coordinates;
    }
    private Coordinate poi(DataRow row){
        Coordinate coordinate = new Coordinate();
        coordinate.setMetadata(row);
        coordinate.setId(row.getString("id"));
        coordinate.setTitle(row.getString("title"));
        coordinate.setAddress(row.getString("address"));
        coordinate.setPoiCategoryName(row.getString("category"));
        coordinate.setPoiCategoryCode(row.getString("category_code"));
        coordinate.setTel(row.getString("tel"));
        DataRow location = row.getRow("location");
        if(null != location) {
            coordinate.setLng(location.getDouble("lng"));
            coordinate.setLat(location.getDouble("lat"));
        }
        DataRow ad = row.getRow("ad_info");
        if(null != ad) {
            String adcode = ad.getString("adcode");
            if(BasicUtil.isNotEmpty(adcode)) {
                String provinceCode = adcode.substring(0, 2);
                String cityCode = adcode.substring(0, 4);
                coordinate.setProvinceCode(provinceCode);
                coordinate.setCityCode(cityCode);
                coordinate.setCountyCode(adcode);
            }
        }
        return coordinate;
    }

    /**
     * 轮廓
     * @param keyword 地区 如青岛/3702
     * @return List 如果有飞地，返回多个多边形
     */
    @Override
    public List<Ring> outline(String keyword) {
        List<Ring> rings = new ArrayList<>();
        String api = "/ws/district/v1/search";
        Map<String, Object> params = new HashMap<>();
        if(BasicUtil.isNotEmpty(keyword)){
            params.put("keyword", keyword);
        }
        params.put("get_polygon", 2);
        params.put("max_offset", 100);
        DataRow row = api(api, params);
        List list = row.getList("result");
        if(null != list && !list.isEmpty()){
            Object obj = list.get(0);
            if(obj instanceof DataSet){
                DataSet<DataRow> set = (DataSet<DataRow>) obj;
                for (DataRow item : set) {
                    List polygons = (List)item.get("polygon");
                    if(null != polygons && !polygons.isEmpty()){
                        for (Object polygon : polygons) {
                            Ring ring = new Ring();
                            rings.add(ring);
                            if(polygon instanceof List){
                                List polygonPoints = (List)polygon;
                                int size = polygonPoints.size();
                                //lng,lat,lng,lat格式
                                for (int i = 0; i < size-1; i+=2) {
                                    Point point = new Point(BasicUtil.parseDouble(polygonPoints.get(i).toString(),0d), BasicUtil.parseDouble(polygonPoints.get(i+1).toString(),0d));
                                    ring.add(point);
                                }
                            }
                        }
                    }

                }
            }
        }
        return rings;
    }


    /**
     * 参数签名
     * @param api 接口
     * @param params 参数
     * @return String
     */
    private String sign(String api, Map<String, Object> params){
        params.put("key", config.KEY);
        params.remove("sig");
        String sign = null;
        String src = api + "?" + BeanUtil.map2string(params) + config.SECRET;
        sign = MD5Util.crypto(src);
        params.put("sig", sign);
        return sign;
    }
    private DataRow api(String api, Map<String, Object> params){
        DataRow row = null;
        sign(api, params);
        HttpResponse response = HttpUtil.get(QQMapConfig.HOST + api,"UTF-8", params);
        int status = response.getStatus();
        if(status == 200){
            String txt = response.getText();
            row = DataRow.parseJson(txt);
            if(null != row){
                status = row.getInt("status",-1);
                if(status != 0) {
                    log.warn("[{}][执行失败][status:{}][info:{}]", api , status, row.getString("message"));
                    log.warn("[{}][response:{}]", api, txt);
                    if (status ==302 || status == 121) {
                        throw new AnylineException("API_OVER_LIMIT", "访问已超出日访问量");
                    } else if (status == 401 || status == 402) {
                        try {
                            log.warn("并发量已达到上限,sleep 100 ...");
                            Thread.sleep(100);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        api(api, params);
                    } else {
                        throw new AnylineException(status, row.getString("message"));
                    }
                }
            }
        }
        return row;
    }

}
