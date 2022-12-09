FROM openjdk:8u232-jre

RUN mkdir /aliyunddns /aliyunddns/logs

WORKDIR /aliyunddns

COPY config/ ./config
ADD https://github.com/cloudtry/aliyunddns/releases/download/v1.0.5/aliyunddns-1.0.5.jar ./aliyunddns.jar

ENV TZ Asia/Shanghai

VOLUME /aliyunddns/config
VOLUME /aliyunddns/logs

ENTRYPOINT ["java", "-jar", "aliyunddns.jar"]
