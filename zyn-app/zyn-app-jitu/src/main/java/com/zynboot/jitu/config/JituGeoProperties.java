package com.zynboot.jitu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "jitu.geo")
public class JituGeoProperties {

    private Duration geocodeCacheTtl = Duration.ofHours(6);

    private Duration resultCacheTtl = Duration.ofMinutes(30);

    private String codeProperty = "code";

    private String aoiIdProperty = "aoi_id";

    private Map<String, String> layerMappings = new HashMap<>();
}
