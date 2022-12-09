package com.cloutry.aliyunddns.service;

import com.cloutry.aliyunddns.api.RemoteQueryIpApi;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("remoteIpService")
public class RemoteIpService implements Supplier<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteIpService.class);
    public static final Pattern IP_PATTERN = Pattern.compile("((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)");

    @Autowired
    private RemoteQueryIpApi remoteQueryIpApi;

    /**
     * 获取ip地址服务
     *
     * @return 外网ip
     */
    @Override
    public String get() {
        byte[] bytes;
        try {
            bytes = remoteQueryIpApi.queryIpByRemote();
        } catch (Exception e) {
            LOGGER.debug("查询ip错误", e);
            bytes = remoteQueryIpApi.queryIpByRemote();
        }
        String responseHTML = new String(bytes, Charset.forName("GB2312"));
        LOGGER.debug("ip地址查询结果 {}", responseHTML);
        Document queryIpResponse = Jsoup.parse(responseHTML);
        String bodyHTML = queryIpResponse.body().html();
        Set<String> ipList = findIP(bodyHTML);
        LOGGER.info("解析到的ip地址列表{}", ipList);
        if (ipList.size() < 1) {
            LOGGER.error("ip查询结果:\n{}", responseHTML);
            throw new NoSuchElementException("未找到有效ip地址, 请检查查询结果可用性");
        }
        return ipList.stream().findFirst().get();
    }

    /**
     * 在指定字符串中查找是否有ip地址
     *
     * @param ipStr 待查找的字符串
     * @return 匹配得到的ip地址列表
     * @author heweidong
     * @date 2020/2/21
     */
    private Set<String> findIP(String ipStr) {
        Set<String> ipList = new HashSet<>();
        Matcher matcher = IP_PATTERN.matcher(ipStr);
        while (matcher.find()) {
            String result = matcher.group();
            ipList.add(result);
        }
        return ipList;
    }
}
