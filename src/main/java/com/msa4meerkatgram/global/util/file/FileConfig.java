package com.msa4meerkatgram.global.util.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "file")
public record FileConfig(
    String serverUri
    , String storagePath
    , String profilePath
    , String postPath
    , List<String> allowExtensionList
    ) {
}
