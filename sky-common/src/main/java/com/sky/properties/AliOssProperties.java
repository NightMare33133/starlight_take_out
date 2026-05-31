package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component//@Component注解将该类注册为Spring容器中的一个Bean，以便在其他地方通过@Autowired注入使用
@ConfigurationProperties(prefix = "sky.alioss")//读取以sky.alioss开头的配置项
@Data
public class AliOssProperties {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

}
