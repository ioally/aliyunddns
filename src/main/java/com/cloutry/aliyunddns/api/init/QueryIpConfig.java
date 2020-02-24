package com.cloutry.aliyunddns.api.init;

import com.cloutry.aliyunddns.api.QueryIpUrlApi;
import feign.Feign;
import feign.Logger;
import feign.slf4j.Slf4jLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class QueryIpConfig {

    private static final Logger LOGGER = new Slf4jLogger(QueryIpConfig.class);

    @Value("${ddns.remote.queryUrl}")
    private String masterUrl;

    @Bean
    public QueryIpUrlApi queryIpUrlApi() {
        return Feign.builder()
                .logger(LOGGER)
                .target(QueryIpUrlApi.class, masterUrl);
    }

}
