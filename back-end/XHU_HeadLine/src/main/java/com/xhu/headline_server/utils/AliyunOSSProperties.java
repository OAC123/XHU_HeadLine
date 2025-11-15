package com.xhu.headline_server.utils;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliyunOSSProperties {

    /**
     * 对应配置：aliyun.oss.endpoint
     */
    private String endpoint;

    /**
     * 对应配置：aliyun.oss.bucket-name
     * Spring Boot 的绑定会把 hyphen 映射为 camelCase（bucket-name -> bucketName）
     */
    private String bucketName;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
}
