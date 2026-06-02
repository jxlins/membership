package com.jxl.membership.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "society.file")
public class SocietyFileProperties {

    private String uploadRoot = "uploads";
    private String publicPrefix = "files";
}
