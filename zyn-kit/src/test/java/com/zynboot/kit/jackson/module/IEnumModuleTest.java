package com.zynboot.kit.jackson.module;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zynboot.kit.enums.IEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IEnumModuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new IEnumModule());

    enum Gender implements IEnum<Integer> {
        MALE(1, "男"), FEMALE(0, "女");

        private final Integer code;
        private final String desc;

        Gender(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public Integer getCode() { return code; }
        public String getDesc() { return desc; }
    }

    enum Status implements IEnum<String> {
        ACTIVE("A", "启用"), DISABLED("D", "禁用");

        private final String code;
        private final String desc;

        Status(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() { return code; }
        public String getDesc() { return desc; }
    }

    @Test
    void shouldSerializeAsObject() throws Exception {
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(Gender.MALE));

        assertThat(json.get("code").intValue()).isEqualTo(1);
        assertThat(json.get("desc").textValue()).isEqualTo("男");
    }

    @Test
    void shouldDeserializeFromObject() throws Exception {
        Gender result = objectMapper.readValue("{\"code\":1,\"desc\":\"男\"}", Gender.class);
        assertThat(result).isEqualTo(Gender.MALE);
    }

    @Test
    void shouldDeserializeFromNumberCode() throws Exception {
        // JSON 数字 1 → MALE
        Gender result = objectMapper.readValue("1", Gender.class);
        assertThat(result).isEqualTo(Gender.MALE);
    }

    @Test
    void shouldDeserializeFromStringCode() throws Exception {
        // JSON 字符串 "1" → MALE
        Gender result = objectMapper.readValue("\"1\"", Gender.class);
        assertThat(result).isEqualTo(Gender.MALE);
    }

    @Test
    void shouldDeserializeFromNegativeNumberCode() throws Exception {
        // 验证负数 code 也能正常工作
        Gender result = objectMapper.readValue("{\"code\":0,\"desc\":\"女\"}", Gender.class);
        assertThat(result).isEqualTo(Gender.FEMALE);
    }

    @Test
    void shouldRoundTrip() throws Exception {
        String json = objectMapper.writeValueAsString(Gender.FEMALE);
        Gender result = objectMapper.readValue(json, Gender.class);
        assertThat(result).isEqualTo(Gender.FEMALE);
    }

    @Test
    void shouldSerializeStringCodeEnum() throws Exception {
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(Status.ACTIVE));

        assertThat(json.get("code").textValue()).isEqualTo("A");
        assertThat(json.get("desc").textValue()).isEqualTo("启用");
    }

    @Test
    void shouldDeserializeStringCodeEnum() throws Exception {
        Status result = objectMapper.readValue("\"A\"", Status.class);
        assertThat(result).isEqualTo(Status.ACTIVE);
    }
}
