# Aliyun DDNS

## 更新说明
- v1.0.5 2022.12.09 更换为亚马逊的ip查询api
- v1.0.4 2022.05.09 更新查询ip的默认地址
- v1.0.2 2020.03.31 精简依赖
- v1.0.1 2020.03.24 修复查询ip地址问题
- v1.0.0 2020.02.29 初始版本提交

## 说明 
- 使用Aliyun提供的 `Java sdk` 实现的 `DDNS` 功能的Docker镜像

## 配置

### 文件名称 `application.yml`
```
spring:
  application:
    name: aliyunddns
ddns:
  remote:
    queryUrl: http://checkip.amazonaws.com/
  aliyun:
    regionId: cn-hangzhou #机房节点id，默认杭州，无需更改
    accessKeyId: xxxxxxx # 需要在阿里云控制台获取
    accessKeySecret: xxxxxxxx # 需要在阿里云控制台获取
    domainName: demo.com # 已经购买的域名
    subDomains: '@,www' # 子域名支持配置多个多个子域名使用英文逗号分隔，支持使用 @ 或 www 配置顶级域名
  task:
    updateRecordCron: '0 0/1 * * * ?' # 查询ip地址的频率，cron表达式 ，默认每分钟
    cleanIpCacheCron: '0 0 0/1 * * ?' # 清空本地ip缓存的频率，cron表达式，默认每小时
logging:
  level:
    root: info # 日志等级，默认即可
```

### 关于阿里云AccessKey需要注意
> 1. 为了阿里云账号安全，不建议使用主账号的AccessKey，建议创建使用RAM子用户AccessKey
> 
> 2. 如果使用RAM子用户AccessKey，需要给新的子账号分配以下权限，否则会因为没有足够的权限导致更新失败
>     - AliyunDNSFullAccess （管理云解析(DNS)的权限）
>     - AliyunDomainFullAccess （管理域名服务的权限）

## Dokcer使用说明

1. 拉取镜像
```
docker pull cloudtry/aliyunddns
```
2. 初始化配置文件
   - 由于该服务依赖阿里云的`accessKeyId`和`accessKeySecret`, 所以默认的配置文件并不能使用 
   - 在启动容器之前需要您使用上述的配置文件模版，在合适的位置创建一个名称为`application.yml`文件（如：`~/config`目录）
   - 创建配置文件后，请按照自己的实际情况结合配置描述，配置您的文件

3. 容器内目录说明
   - `/aliyunddns/logs` - 服务运行时产生的日志
   - `/aliyunddns/config` - 配置文件（`application.yml`）存放目录

4. 启动容器
```
docker run -v ~/logs:/aliyunddns/logs -v ~/config:/aliyunddns/config -d cloudtry/aliyunddns
```
