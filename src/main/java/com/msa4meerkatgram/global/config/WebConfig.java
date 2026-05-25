package com.msa4meerkatgram.global.config;

import com.msa4meerkatgram.global.util.file.FileConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final FileConfig fileConfig;

    public WebConfig(FileConfig fileConfig) {
        this.fileConfig = fileConfig;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resourceLocation = Paths.get(fileConfig.storagePath() + "/images").toUri().toString();

        registry.addResourceHandler("/images/**").addResourceLocations(resourceLocation);
    }
}