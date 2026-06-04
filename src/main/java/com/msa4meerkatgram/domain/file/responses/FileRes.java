package com.msa4meerkatgram.domain.file.responses;

import lombok.Builder;

@Builder
public record FileRes(
    String fileUri
) {
}
