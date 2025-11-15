package com.xhu.headline_server.utils;


import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class AliyunOSSOperator {

    private final AliyunOSSProperties aliyunOSSProperties;

    @Autowired
    public AliyunOSSOperator(AliyunOSSProperties aliyunOSSProperties) {
        this.aliyunOSSProperties = aliyunOSSProperties;
    }

    public String upload(byte[] content, String originalFilename) throws Exception {
        String endpoint = aliyunOSSProperties.getEndpoint();
        String bucketName = aliyunOSSProperties.getBucketName();

        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("aliyun.oss.endpoint is not configured");
        }
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalStateException("aliyun.oss.bucket-name is not configured");
        }

        String accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
        String accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");
        if (accessKeyId == null || accessKeySecret == null) {
            throw new IllegalStateException("Environment variables OSS_ACCESS_KEY_ID and OSS_ACCESS_KEY_SECRET must be set");
        }

        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFileName = UUID.randomUUID().toString() + ext;
        String objectName = dir + "/" + newFileName;

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(content));
        } finally {
            ossClient.shutdown();
        }

        // normalize endpoint (remove protocol if present) and return https URL
        String normalized = endpoint;
        if (normalized.startsWith("http://")) normalized = normalized.substring(7);
        else if (normalized.startsWith("https://")) normalized = normalized.substring(8);

        return "https://" + bucketName + "." + normalized + "/" + objectName;
    }
}
