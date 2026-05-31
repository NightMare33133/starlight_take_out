package com.sky.config;


import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类用于创建AliOSSUtil对象
 */

@Configuration//@Configuration
@Slf4j
public class OssConfiguration {

    @Bean//作用是将自定义类注入容器
    @ConditionalOnMissingBean//当容器中没有指定类型的Bean时，才会创建该Bean
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties) {
        log.info("开始上传阿里云文件上传工具类：{}", aliOssProperties);
        return new AliOssUtil(aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName());
    }
}
