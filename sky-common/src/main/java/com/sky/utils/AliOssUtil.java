package com.sky.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.ByteArrayInputStream;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;      // 留着当占位符，免得改配置类
    private String accessKeySecret;  // 留着当占位符，免得改配置类
    private String bucketName;

    /**
     * 文件上传
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {
        OSS ossClient = null;

        try {
            // 核心修改：无视传进来的假密钥，直接从电脑的环境变量里读取真实密钥
            EnvironmentVariableCredentialsProvider credentialsProvider =
                    CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

            // 使用环境变量凭证创建 OSSClient 实例
            ossClient = new OSSClientBuilder().build(endpoint, credentialsProvider);

            // 创建PutObject请求。
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));

        } catch (Exception e) {
            // 捕获获取环境变量凭证可能抛出的异常
            log.error("OSS 客户端初始化或上传失败。请确认环境变量 OSS_ACCESS_KEY_ID 和 OSS_ACCESS_KEY_SECRET 已配置", e);
            if (e instanceof OSSException) {
                OSSException oe = (OSSException) e;
                System.out.println("Caught an OSSException, which means your request made it to OSS, "
                        + "but was rejected with an error response for some reason.");
                System.out.println("Error Message:" + oe.getErrorMessage());
                System.out.println("Error Code:" + oe.getErrorCode());
                System.out.println("Request ID:" + oe.getRequestId());
                System.out.println("Host ID:" + oe.getHostId());
            } else if (e instanceof ClientException) {
                ClientException ce = (ClientException) e;
                System.out.println("Caught an ClientException, which means the client encountered "
                        + "a serious internal problem while trying to communicate with OSS, "
                        + "such as not being able to access the network.");
                System.out.println("Error Message:" + ce.getMessage());
            }
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        //文件访问路径规则 https://BucketName.Endpoint/ObjectName
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)
                .append(".")
                .append(endpoint)
                .append("/")
                .append(objectName);

        log.info("文件上传到:{}", stringBuilder.toString());

        return stringBuilder.toString();
    }
}