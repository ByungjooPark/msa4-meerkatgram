package com.msa4meerkatgram.global.util.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "file")
public record FileConfig(
        String storagePath
        ,String profilePath
        ,String postPath
        ,String serverUri
) {
}
