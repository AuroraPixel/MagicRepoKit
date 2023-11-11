package com.magicrepokit.chat.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MetadataEntity {
    @JsonProperty("is_complete")
    private Boolean isComplete;
    @JsonProperty("message_type")
    private String messageType;
    @JsonProperty("model_slug")
    private String modelSlug;
    @JsonProperty("parent_id")
    private String parentId;
    @JsonProperty("timestamp_")
    private String timestamp;

    @Data
    public static class FinishDetails{
        private String type;
        @JsonProperty("stop_tokens")
        private String[] stopTokens;

    }
}
