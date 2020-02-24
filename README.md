# aliyunddns

## 说明 
- 项目基于Spring Boot
- 使用阿里云DNS Java SDK 实现的阿里云ddns服务
- 默认端口 8080

## 配置说明
```
spring:
  application:
    name: aliyunddns
ddns:
  remote:
    queryUrl: http://ip138.com
  aliyun:
    regionId: cn-hangzhou #机房节点id，默认杭州，无需更改
    accessKeyId: xxxxxxx # 需要在阿里云控制台获取
    accessKeySecret: xxxxxxxx # 需要在阿里云控制台获取
    domainName: demo.com # 购买域名
    subDomains: '@,www' # 子域名支持配置多个多个子域名使用英文逗号分隔，支持使用 @ 或 www 配置顶级域名
  task:
    updateRecordCron: '0 0/1 * * * ?' # 查询ip地址的频率，cron表达式
    cleanIpCacheCron: '0 0 0/1 * * ?' # 清空本地ip缓存的频率，cron表达式
logging:
  level:
    root: info # 日志等级，默认即可
```
