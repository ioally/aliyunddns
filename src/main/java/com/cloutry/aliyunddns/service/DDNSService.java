package com.cloutry.aliyunddns.service;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.alidns.model.v20150109.*;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.FormatType;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.http.ProtocolType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.aliyuncs.alidns.model.v20150109.DescribeDomainRecordsResponse.Record;

@Service
public class DDNSService implements Consumer<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DDNSService.class);

    @Value("${ddns.aliyun.regionId}")
    private String regionId;

    @Value("${ddns.aliyun.accessKeyId}")
    private String accessKeyId;

    @Value("${ddns.aliyun.accessKeySecret}")
    private String accessKeySecret;

    @Value("${ddns.aliyun.domainName}")
    private String domainName;

    @Value("${ddns.aliyun.subDomains}")
    private String subDomains;

    private static IAcsClient client;

    @Override
    public void accept(String newIp) {
        getClient(regionId, accessKeyId, accessKeySecret);
        try {
            if (newIp != null) {
                List<Record> records = getRecords();
                String[] subDomainArr = subDomains.split(",");
                if (subDomainArr.length == records.size()) {
                    LOGGER.info("未发现新增子域名, 开始更新!");
                    updateRecords(records, newIp);
                } else {
                    LOGGER.info("发现新增子域名, 开始添加解析记录!");
                    List<String> rrList = records.stream().map(Record::getRR).collect(Collectors.toList());
                    List<String> subDomainList = excludeDuplicates(rrList, subDomainArr);
                    addRecords(domainName, subDomainList, newIp);
                    if (subDomainList.size() != subDomainArr.length) {
                        LOGGER.info("发现存在更新的子域名, 开始更新解析记录!");
                        List<String> subDomains = Arrays.stream(subDomainArr).collect(Collectors.toList());
                        List<Record> filterR = records.stream()
                                .filter(r -> subDomains.indexOf(r.getRR()) > -1)
                                .collect(Collectors.toList());
                        updateRecords(filterR, newIp);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("DDNS刷新失败", e);
        }
    }


    /**
     * 根据配置信息查询域名解析A记录
     *
     * @throws ClientException
     * @author heweidong
     * @date 2020/2/21
     */
    private List<Record> getRecords() throws ClientException {
        DescribeDomainRecordsRequest describeDomainRecordsRequest = new DescribeDomainRecordsRequest();
        describeDomainRecordsRequest.setDomainName(domainName);
        describeDomainRecordsRequest.setSysProtocol(ProtocolType.HTTPS);
        describeDomainRecordsRequest.setAcceptFormat(FormatType.JSON);
        describeDomainRecordsRequest.setSysMethod(MethodType.POST);
        List<Record> records = client.getAcsResponse(describeDomainRecordsRequest).getDomainRecords();
        records = records.stream()
                .filter(record -> subDomains.isEmpty() || subDomains.contains(record.getRR()))
                .filter(record -> record.getType().equals("A"))
                .collect(Collectors.toList());
        return Optional.of(records).orElse(new ArrayList<>());
    }

    /**
     * 更新域名解析记录
     *
     * @param domainRecords 解析记录
     * @param newIp         新的ip
     */
    private void updateRecords(List<Record> domainRecords, String newIp) {
        AtomicBoolean isUpdate = new AtomicBoolean(false);
        domainRecords.stream().filter(record -> !record.getValue().equals(newIp))
                .forEach(record -> {
                    UpdateDomainRecordRequest updateDomainRecordRequest = new UpdateDomainRecordRequest();
                    updateDomainRecordRequest.setRecordId(record.getRecordId());
                    updateDomainRecordRequest.setRR(record.getRR());
                    updateDomainRecordRequest.setType(record.getType());
                    updateDomainRecordRequest.setValue(newIp);
                    updateDomainRecordRequest.setSysProtocol(ProtocolType.HTTPS);
                    updateDomainRecordRequest.setAcceptFormat(FormatType.JSON);
                    updateDomainRecordRequest.setSysMethod(MethodType.POST);
                    try {
                        UpdateDomainRecordResponse response = client.getAcsResponse(updateDomainRecordRequest);
                        isUpdate.set(true);
                        LOGGER.info("更新记录成功, domainName: {}, rr: {}, recordId: {}, oldIp: {}, newIp: {}",
                                record.getDomainName(), record.getRR(), response.getRecordId(), record.getValue(), newIp);
                    } catch (ClientException e) {
                        LOGGER.error("更新域名解析错误, domainName: {}, rr: {}, oldIp: {}, newIp: {}",
                                record.getDomainName(), record.getRR(), record.getValue(), newIp);
                    }
                });
        if (!isUpdate.get()) {
            LOGGER.info("域名({})的解析记录ip与查询得到的ip地址[{}]一致，无需更新！", String.format("[%s].%s", subDomains, domainName), newIp);
        }
    }

    /**
     * 新增解析记录
     *
     * @param domainName     一级域名
     * @param subDomainNames 二级或子域名
     * @param newIp          新的ip
     */
    private void addRecords(String domainName, List<String> subDomainNames, String newIp) {
        subDomainNames.stream().map(sdn -> {
            AddDomainRecordRequest addDomainRecordRequest = new AddDomainRecordRequest();
            addDomainRecordRequest.setDomainName(domainName);
            addDomainRecordRequest.setRR(sdn);
            addDomainRecordRequest.setValue(newIp);
            addDomainRecordRequest.setType("A");
            addDomainRecordRequest.setSysProtocol(ProtocolType.HTTPS);
            addDomainRecordRequest.setAcceptFormat(FormatType.JSON);
            addDomainRecordRequest.setSysMethod(MethodType.POST);
            return addDomainRecordRequest;
        }).forEach(addDomainRecordRequest -> {
            try {
                AddDomainRecordResponse acsResponse = client.getAcsResponse(addDomainRecordRequest);
                LOGGER.info("新增记录成功, domainName: {}, subDomain: {}, recordId: {}, newIp: {}"
                        , domainName, addDomainRecordRequest.getRR(), acsResponse.getRecordId(), newIp);
            } catch (ClientException e) {
                LOGGER.error("新增域名解析错误, domainName: {}, subDomainNames: {}, newIp: {}"
                        , domainName, subDomainNames, newIp);
            }
        });
    }

    /**
     * 排除重复的数据
     *
     * @param rrList
     * @param subDomainArr
     * @return
     */
    private List<String> excludeDuplicates(List<String> rrList, String[] subDomainArr) {
        List<String> subDomainList = Arrays.stream(subDomainArr).collect(Collectors.toList());
        subDomainList.removeAll(rrList);
        return subDomainList;
    }

    /**
     * 创建新的客户端
     *
     * @param regionId        区域id
     * @param accessKeyId
     * @param accessKeySecret
     * @return
     */
    private static IAcsClient getClient(String regionId, String accessKeyId, String accessKeySecret) {
        if (client == null) {
            IClientProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
            client = new DefaultAcsClient(profile);
        }
        return client;
    }
}
