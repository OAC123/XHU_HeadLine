package com.xhu.headline_server.utils;


import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class util1 {

    // 将http实例的参数转换为map
    public static <T> T requestToMode(HttpServletRequest request, T object) {

        //
        if (request == null || object == null) return object;
        Map<String, String[]> params = request.getParameterMap();
        Map<String, Object> single = new HashMap<>();
        params.forEach((k, v) -> {
            if (v != null && v.length > 0) single.put(k, v[0]);
        });
        return requestToModeFromMap(single, object);
    }

    // 将map的参数转换为model对象
    public static <T> T requestToModeFromMap(Map<String, ?> params, T object) {
        if (params == null || object == null) return object;
        Class<?> cls = object.getClass();
        for (Field f : cls.getDeclaredFields()) {
            String name = f.getName();
            if (!params.containsKey(name)) continue;
            Object val = params.get(name);
            if (val == null) continue;
            try {
                f.setAccessible(true);
                Class<?> t = f.getType();
                if (t == String.class) {
                    f.set(object, String.valueOf(val));
                } else if (t == Integer.class || t == int.class) {
                    f.set(object, toInteger(val));
                } else if (t == Long.class || t == long.class) {
                    f.set(object, toLong(val));
                } else if (t == Double.class || t == double.class) {
                    f.set(object, toDouble(val));
                } else if (t == Boolean.class || t == boolean.class) {
                    f.set(object, toBoolean(val));
                } else {
                    if (t.isInstance(val)) f.set(object, val);
                }
            } catch (Exception ignored) {}
        }
        return object;
    }

    //模型对象转换为sql
    public static String ModelToDb(Object object, String dbfile) {
        if (object == null || dbfile == null || dbfile.isEmpty()) return "";
        Class<?> cls = object.getClass();
        List<String> cols = new ArrayList<>();
        List<String> vals = new ArrayList<>();
        for (Field f : cls.getDeclaredFields()) {
            try {
                f.setAccessible(true);
                Object v = f.get(object);
                if (v == null) continue;
                String name = f.getName();
                if ("id".equalsIgnoreCase(name) && isZero(v)) continue;
                cols.add(name);
                if (v instanceof String) vals.add("'" + escapeSql((String) v) + "'");
                else if (v instanceof Boolean) vals.add(((Boolean) v) ? "1" : "0");
                else vals.add(String.valueOf(v));
            } catch (Exception ignored) {}
        }
        if (cols.isEmpty()) return "";
        String colPart = String.join(", ", cols);
        String valPart = String.join(", ", vals);
        return "INSERT INTO " + dbfile + " (" + colPart + ") VALUES (" + valPart + ");";
    }

    // 实现列表过滤月分页
    public static <T> Map<String, Object> filterAndPage(List<T> all,
                                                        Map<String, String> containsFilters,
                                                        Map<String, String> equalsFilters,
                                                        int page, int size) {
        if (all == null) all = Collections.emptyList();
        List<T> filtered = all.stream().filter(item -> {
            try {
                // 处理包含过滤条件
                for (Map.Entry<String, String> e : Optional.ofNullable(containsFilters).orElse(Collections.emptyMap()).entrySet()) {
                    Object fv = getFieldValue(item, e.getKey());
                    String sval = fv == null ? "" : String.valueOf(fv);
                    if (!sval.contains(Optional.ofNullable(e.getValue()).orElse(""))) return false;
                }
                // 处理过滤等于的条件
                for (Map.Entry<String, String> e : Optional.ofNullable(equalsFilters).orElse(Collections.emptyMap()).entrySet()) {
                    Object fv = getFieldValue(item, e.getKey());
                    String sval = fv == null ? "" : String.valueOf(fv);
                    if (!sval.equals(Optional.ofNullable(e.getValue()).orElse(""))) return false;
                }
                return true;
            } catch (Exception ex) {
                return false;
            }
        }).collect(Collectors.toList());

        int total = filtered.size();
        int from = Math.max(0, (page - 1) * size);
        int to = Math.min(total, from + size);
        List<T> rows = from >= to ? Collections.emptyList() : filtered.subList(from, to);

        Map<String, Object> data = new HashMap<>();
        data.put("rows", rows);
        data.put("total", total);
        return data;
    }
    // 反射获取对象字段值
    private static Object getFieldValue(Object obj, String name) {
        if (obj == null || name == null) return null;
        try {
            Field f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception e) {
            return null;
        }
    }
    //对象转换方法

    private static Integer toInteger(Object v) {
        try { if (v instanceof Number) return ((Number) v).intValue(); return Integer.parseInt(String.valueOf(v)); }
        catch (Exception e) { return null; }
    }

    private static Long toLong(Object v) {
        try { if (v instanceof Number) return ((Number) v).longValue(); return Long.parseLong(String.valueOf(v)); }
        catch (Exception e) { return null; }
    }

    private static Double toDouble(Object v) {
        try { if (v instanceof Number) return ((Number) v).doubleValue(); return Double.parseDouble(String.valueOf(v)); }
        catch (Exception e) { return null; }
    }

    private static Boolean toBoolean(Object v) {
        try { if (v instanceof Boolean) return (Boolean) v; return Boolean.parseBoolean(String.valueOf(v)); }
        catch (Exception e) { return null; }
    }

    private static boolean isZero(Object v) {
        if (v instanceof Number) return ((Number) v).longValue() == 0L;
        if (v instanceof String) return "0".equals(v);
        return false;
    }

    private static String escapeSql(String s) {
        return s == null ? null : s.replace("'", "''");
    }
}
