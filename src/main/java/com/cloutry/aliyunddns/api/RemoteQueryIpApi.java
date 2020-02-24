package com.cloutry.aliyunddns.api;

import feign.Headers;
import feign.RequestLine;

public interface RemoteQueryIpApi {

    @RequestLine("GET /")
    @Headers(QueryIpUrlApi.USER_AGENT)
    byte[] queryIpByRemote();
}
