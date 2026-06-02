package com.jxl.membership.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class StaticResourceConfig implements WebMvcConfigurer {

    private final SocietyFileProperties societyFileProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = normalizePrefix(societyFileProperties.getPublicPrefix());
        Path uploadRoot = Paths.get(societyFileProperties.getUploadRoot()).toAbsolutePath().normalize();
        registry.addResourceHandler("/" + prefix + "/**")
                .addResourceLocations(uploadRoot.toUri().toString());
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return "files";
        }
        return prefix.replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
