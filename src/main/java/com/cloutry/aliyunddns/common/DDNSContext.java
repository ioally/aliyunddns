package com.cloutry.aliyunddns.common;

public abstract class DDNSContext {

    /**
     * 本地回环地址
     */
    public static final String LOCAL_IP = "127.0.0.1";

    /**
     * 上次更新解析记录时使用的ip地址, 服务启动时默认为本地地址
     */
    public static String oldIp = LOCAL_IP;

    /**
     * 本次查询获取的外网ip地址
     */
    public static String newIp = null;


}
