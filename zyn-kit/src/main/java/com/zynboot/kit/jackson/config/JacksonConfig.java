package com.zynboot.kit.jackson.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.zynboot.kit.jackson.deserializer.DateDeserializer;
import com.zynboot.kit.jackson.deserializer.LocalDateTimeDeserializer;
import com.zynboot.kit.jackson.module.IEnumModule;
import com.zynboot.kit.jackson.module.SensitiveServiceHolder;
import com.zynboot.kit.jackson.plugins.sensitive.SensitiveService;
import com.zynboot.kit.jackson.serializer.BigNumberSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@AutoConfiguration(before = JacksonAutoConfiguration.class)
public class JacksonConfig {

    private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            builder.modules(javaTimeModule(), new IEnumModule(), dateModule());
            builder.featuresToDisable(MapperFeature.DEFAULT_VIEW_INCLUSION);
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            builder.simpleDateFormat("yyyy-MM-dd HH:mm:ss");
        };
    }

    /**
     * 统一配置 ObjectMapper，供 JsonUtils 等非 Spring 场景复用。
     */
    public static ObjectMapper configureObjectMapper(ObjectMapper mapper) {
        mapper.registerModule(javaTimeModule());
        mapper.registerModule(new IEnumModule());
        mapper.registerModule(dateModule());
        mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        return mapper;
    }

    private static JavaTimeModule javaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DEFAULT_DATE_TIME_FORMATTER));
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
        module.addSerializer(Long.class, BigNumberSerializer.INSTANCE);
        module.addSerializer(Long.TYPE, BigNumberSerializer.INSTANCE);
        module.addSerializer(BigInteger.class, BigNumberSerializer.INSTANCE);
        module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
        return module;
    }

    private static SimpleModule dateModule() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(java.util.Date.class, DateDeserializer.INSTANCE);
        return module;
    }

    @Bean
    @ConditionalOnMissingBean(SensitiveService.class)
    public SensitiveService sensitiveService() {
        return () -> true;
    }

    @Bean
    public SensitiveServiceHolder sensitiveServiceHolder(SensitiveService sensitiveService) {
        return new SensitiveServiceHolder(sensitiveService);
    }
}
