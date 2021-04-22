package com.cleafy.elasticsearch6.plugins.http.utils;

import java.io.InputStream;
import java.util.Properties;

/**
 * 描述信息
 * 配置文件读取类
 *
 * @author 杨祎
 * @date 2021/4/22
 */
public class AppProperties {
    private static  Properties properties;
    static {
        properties = new Properties();
       // 使用ClassLoader加载properties配置文件生成对应的输入流
        try {
            InputStream in = AppProperties.class.getClassLoader().getResourceAsStream("application.properties");
            // 使用properties对象加载输入流
            properties.load(in);
        }catch (Exception e) {
            e.printStackTrace();
            LoggerUtils.log(e.getMessage());
        }
    }

    /**
     * 获取配置参数
     * @param key 键
     * @return
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
