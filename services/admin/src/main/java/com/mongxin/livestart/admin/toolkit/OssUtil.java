package com.mongxin.livestart.admin.toolkit;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.mongxin.livestart.admin.config.OssConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OssUtil {

    private final OssConfig ossConfig;

    public String upload(InputStream inputStream, String originalFilename) {
        String endpoint = ossConfig.getEndpoint().trim();
        String accessKeyId = ossConfig.getAccessKeyId().trim();
        String accessKeySecret = ossConfig.getAccessKeySecret().trim();
        String bucketName = ossConfig.getBucketName().trim();

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            String fileName = UUID.randomUUID() + "_" + originalFilename;
            ossClient.putObject(bucketName, fileName, inputStream);
            return "https://" + bucketName + "." + endpoint + "/" + fileName;
        } finally {
            ossClient.shutdown();
        }
    }
}
