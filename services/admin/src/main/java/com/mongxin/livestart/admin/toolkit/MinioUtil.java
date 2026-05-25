package com.mongxin.livestart.admin.toolkit;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MinioUtil {

    private final MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    public String upload(MultipartFile file) throws Exception {
        // 生成云端唯一文件名
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        log.info("【MinIO工具】正在上传文件至存储桶 [{}], 文件名: {}", bucketName, fileName);

        // 调用 MinIO 官方 SDK 核心方法
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
        // 直接从 file 中提取流、大小和 Content-Type
                        .stream(file.getInputStream(), file.getSize(), 10 * 1024 * 1024L)
                        .contentType(file.getContentType())
                        .build()
        );

        // 返回文件在本地 MinIO 容器的公共绝对路径
        return "http://localhost:1900/" + bucketName + "/" + fileName;
    }
}