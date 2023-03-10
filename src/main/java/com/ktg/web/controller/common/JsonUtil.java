package com.ktg.web.controller.common;

import java.util.*;

/**
 *
 *
 *      json处理工具类
 */
public class JsonUtil {

    /**
     * 将json对象转换为HashMap
     * @param json
     * @return
     */
    public static Map<String, Object> parseJSON2Map(net.sf.json.JSONObject json) {
        Map<String, Object> map = new HashMap<String, Object>();
        // 最外层解析
        for (Object k : json.keySet()) {
            Object v = json.get(k);
            // 如果内层还是json数组的话，继续解析
            if (v instanceof net.sf.json.JSONArray) {
                List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
                Iterator<net.sf.json.JSONObject> it = ((net.sf.json.JSONArray) v).iterator();
                while (it.hasNext()) {
                    net.sf.json.JSONObject json2 = it.next();
                    list.add(parseJSON2Map(json2));
                }
                map.put(k.toString(), list);
            } else if (v instanceof net.sf.json.JSONObject) {
                // 如果内层是json对象的话，继续解析
                map.put(k.toString(), parseJSON2Map((net.sf.json.JSONObject) v));
            } else {
                // 如果内层是普通对象的话，直接放入map中
                map.put(k.toString(), v);
            }
        }
        return map;
    }
    /**
     * 将json字符串转换为Map
     * @param jsonStr
     * @return
     */
    public static Map<String, Object> parseJSONstr2Map(String jsonStr) {
        net.sf.json.JSONObject json = net.sf.json.JSONObject.fromObject(jsonStr);
        Map<String, Object> map = parseJSON2Map(json);
        return map;
    }
}

