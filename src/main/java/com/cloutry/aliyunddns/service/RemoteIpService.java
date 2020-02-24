package com.cloutry.aliyunddns.service;

import com.cloutry.aliyunddns.api.QueryIpUrlApi;
import com.cloutry.aliyunddns.api.RemoteQueryIpApi;
import feign.Feign;
import feign.slf4j.Slf4jLogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("remoteIpService")
public class RemoteIpService implements Supplier<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteIpService.class);
    private static final Slf4jLogger FEIGN_LOGGER = new Slf4jLogger(RemoteIpService.class);

    public static final Pattern IP_PATTERN = Pattern.compile("((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)");

    @Autowired
    private QueryIpUrlApi queryIpUrlApi;

    private static RemoteQueryIpApi remoteQueryIpApi = null;

    /**
     * 获取ip地址服务
     *
     * @return 外网ip
     */
    @Override
    public String get() {
        remoteQueryIpApi = Optional.ofNullable(remoteQueryIpApi).orElseGet(this::getRemoteQueryIpApi);
        byte[] bytes;
        try {
            bytes = remoteQueryIpApi.queryIpByRemote();
        } catch (Exception e) {
            LOGGER.debug("查询ip错误", e);
            LOGGER.error("查询ip失败, 可能是查询地址变更, 尝试重新获取查询地址");
            remoteQueryIpApi = this.getRemoteQueryIpApi();
            bytes = remoteQueryIpApi.queryIpByRemote();
        }
        String responseHTML = new String(bytes, Charset.forName("GB2312"));
        LOGGER.debug("ip地址查询结果 {}", responseHTML);
        Document queryIpResponse = Jsoup.parse(responseHTML);
        String bodyHTML = queryIpResponse.body().html();
        List<String> ipList = findIP(bodyHTML);
        LOGGER.info("解析到的ip地址列表{}", ipList);
        if (ipList.size() < 1) {
            LOGGER.error("ip查询结果:\n{}", responseHTML);
            throw new NoSuchElementException("未找到有效ip地址, 请检查查询结果可用性");
        }
        return ipList.get(0);
    }

    /**
     * 获取查询ip地址的feignClient
     *
     * @return RemoteQueryIpApi
     */
    private RemoteQueryIpApi getRemoteQueryIpApi() {
        LOGGER.info("开始获取查询ip地址的url");
        String queryIpUrl;
        byte[] resBytes = queryIpUrlApi.queryIpMaster();
        String masterResultHTML = new String(resBytes, Charset.forName("GB2312"));
        LOGGER.debug("主站点响应\n{}", masterResultHTML);
        Document masterDocument = Jsoup.parse(masterResultHTML);
        Element masterBody = masterDocument.body();
        Elements iframe = masterBody.getElementsByTag("iframe");
        if (iframe.size() < 1) {
            throw new NoSuchElementException(String.format("未找到iframe标签，请检查配置[%s]的地址可用性。", "ddns.route.queryUrl"));
        }
        queryIpUrl = iframe.get(0).attr("src").toLowerCase();
        // https会导致请求失败
        queryIpUrl = queryIpUrl.replace("https", "http");
        LOGGER.info("获取成功, 查询ip地址的url: {}", queryIpUrl);
        return Feign.builder()
                .logger(FEIGN_LOGGER)
                .target(RemoteQueryIpApi.class, queryIpUrl);
    }

    /**
     * 在指定字符串中查找是否有ip地址
     *
     * @param ipStr 待查找的字符串
     * @return 匹配得到的ip地址列表
     * @author heweidong
     * @date 2020/2/21
     */
    private List<String> findIP(String ipStr) {
        List<String> ipList = new ArrayList<>();
        Matcher matcher = IP_PATTERN.matcher(ipStr);
        while (matcher.find()) {
            String result = matcher.group();
            ipList.add(result);
        }
        return ipList;
    }
}
