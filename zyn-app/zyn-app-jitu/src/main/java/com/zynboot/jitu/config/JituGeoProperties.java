package com.zynboot.jitu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "jitu.geo")
public class JituGeoProperties {

    private Duration geocodeCacheTtl = Duration.ofHours(1);

    private Duration resultCacheTtl = Duration.ofMinutes(5);

    /** 围栏 properties 中四段码的字段名 */
    private String codeProperty = "code";

    /** 围栏图层 ID（type=1取件/2收件均查此图层，通过 properties.type 区分） */
    private String layerId;

    /** 上游地图服务 API Key（minedata） */
    private String apiKey;
}
