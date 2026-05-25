package com.mongxin.livestart.admin.toolkit;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;

import com.mongxin.livestart.admin.config.OssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.util.UUID;

@Component
public class OssUtil {

    @Autowired
    private OssConfig ossConfig;

    public String upload(InputStream inputStream, String originalFilename) {
        // 创建 OSSClient 实例
        OSS ossClient = new OSSClientBuilder().build(
                ossConfig.getEndpoint(),
                ossConfig.getAccessKeyId(),
                ossConfig.getAccessKeySecret());

        // 使用 UUID 拼接原始文件名，防止文件名冲突导致覆盖
        String fileName = UUID.randomUUID().toString() + "_" + originalFilename;

        // 上传文件到指定的 Bucket
        ossClient.putObject(ossConfig.getBucketName(), fileName, inputStream);

        // 关闭 OSSClient
        ossClient.shutdown();

        // 拼接并返回图片在云端的公共访问绝对路径
        return "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/" + fileName;
    }
}