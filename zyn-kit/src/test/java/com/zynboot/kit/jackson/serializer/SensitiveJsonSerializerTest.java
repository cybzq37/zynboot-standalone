package com.zynboot.kit.jackson.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zynboot.kit.jackson.config.JacksonConfig;
import com.zynboot.kit.jackson.plugins.sensitive.Sensitive;
import com.zynboot.kit.jackson.plugins.sensitive.SensitiveService;
import com.zynboot.kit.jackson.plugins.sensitive.SensitiveStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveJsonSerializerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, JacksonConfig.class));

    @Test
    void shouldMaskSensitiveFieldByDefault() {
        contextRunner.run(context -> {
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

            String json = toJson(objectMapper, new SensitivePayload("13812345678"));

            assertThat(json).contains("\"phone\":\"138****5678\"");
        });
    }

    @Test
    void shouldAllowCustomServiceToDisableMasking() {
        contextRunner
                .withBean(SensitiveService.class, () -> () -> false)
                .run(context -> {
                    ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

                    String json = toJson(objectMapper, new SensitivePayload("13812345678"));

                    assertThat(json).contains("\"phone\":\"13812345678\"");
                });
    }

    private static String toJson(ObjectMapper objectMapper, Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record SensitivePayload(@Sensitive(strategy = SensitiveStrategy.PHONE) String phone) {
    }
}
