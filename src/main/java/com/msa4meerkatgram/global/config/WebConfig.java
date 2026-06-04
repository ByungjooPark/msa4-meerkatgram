package com.msa4meerkatgram.global.config;

import com.msa4meerkatgram.global.util.file.FileConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final FileConfig fileConfig;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resourceLocation = Paths.get(fileConfig.storagePath() + "/files").toUri().toString();

        registry.addResourceHandler("/files/**").addResourceLocations(resourceLocation);
    }
}
