package com.github.sunnysuperman.serverpub;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.github.sunnysuperman.commons.config.PropertiesConfig;

public class U {

    public static void sleep(long mills) {
        try {
            Thread.sleep(mills);
        } catch (Throwable t) {
            L.error(t);
        }
    }

    public static Map<String, Object> readPropertiesAsMap(InputStream in) {
        PropertiesConfig config = new PropertiesConfig(in);
        Map<String, Object> configAsMap = new HashMap<>(config.size());
        for (String key : config.keySet()) {
            configAsMap.put(key, config.getString(key));
        }
        return configAsMap;
    }
}
