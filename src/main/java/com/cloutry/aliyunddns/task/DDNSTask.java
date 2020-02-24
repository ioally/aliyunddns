package com.cloutry.aliyunddns.task;

import com.cloutry.aliyunddns.common.DDNSContext;
import com.cloutry.aliyunddns.service.DDNSService;
import com.cloutry.aliyunddns.service.RemoteIpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class DDNSTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(DDNSTask.class);

    @Autowired
    private DDNSService ddnsService;

    @Autowired
    private RemoteIpService remoteIpService;

    /**
     * 更新解析记录服务
     */
    @Scheduled(cron = "${ddns.task.updateRecordCron}")
    public void updateRecord() {
        DDNSContext.newIp = remoteIpService.get();
        if (Objects.equals(DDNSContext.newIp, DDNSContext.oldIp)) {
            LOGGER.info("外网ip未发生变更，无需更新域名解析记录 [{}]", DDNSContext.newIp);
        } else {
            LOGGER.info("检测到外网ip地址发生变更(oldIp: [{}], newIp: [{}]), 开始更新域名解析记录",
                    DDNSContext.oldIp, DDNSContext.newIp);
            ddnsService.accept(DDNSContext.newIp);
            DDNSContext.oldIp = DDNSContext.newIp;
        }
    }

    /**
     * 定时清理本地oldIp缓存地址
     */
    @Scheduled(cron = "${ddns.task.cleanIpCacheCron}")
    public void cleanOldIpCache() {
        LOGGER.warn("重置本地外网ip地址缓存, 防止信息差异导致无法更新");
        DDNSContext.oldIp = DDNSContext.LOCAL_IP;
    }
}
