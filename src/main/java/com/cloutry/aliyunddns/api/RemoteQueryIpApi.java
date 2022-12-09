package com.cloutry.aliyunddns.api;

import feign.Headers;
import feign.RequestLine;

public interface RemoteQueryIpApi {

    String USER_AGENT = "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36;";

    @RequestLine("GET /")
    @Headers(USER_AGENT)
    byte[] queryIpByRemote();
}
