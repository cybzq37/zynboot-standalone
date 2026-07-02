package com.zynboot.kit.jackson.module;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.zynboot.kit.enums.IEnum;
import com.zynboot.kit.jackson.deserializer.IEnumDeserializer;
import com.zynboot.kit.jackson.deserializer.IEnumKeyDeserializer;
import com.zynboot.kit.jackson.serializer.IEnumSerializer;

/**
 * Jackson 模块，自动注册 {@link IEnum} 的序列化/反序列化器。
 * <p>
 * 注册后，所有实现 {@link IEnum} 的枚举无需额外配置即可：
 * <ul>
 *   <li>序列化：返回 {@code {"code":1,"desc":"男"}} 格式</li>
 *   <li>反序列化：接受对象格式或 code 值格式</li>
 * </ul>
 * 通常由 {@link com.zynboot.kit.jackson.config.JacksonConfig} 自动注册，无需手动使用。
 */
public class IEnumModule extends SimpleModule {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public IEnumModule() {
        super("IEnumModule");
        addSerializer((Class) IEnum.class, new IEnumSerializer());
        addKeyDeserializer(IEnum.class, new IEnumKeyDeserializer());
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        // 通过 mixin 为所有 IEnum 实现类注册自定义反序列化器
        context.setMixInAnnotations(IEnum.class, IEnumMixin.class);
    }

    @JsonDeserialize(using = IEnumDeserializer.class)
    abstract static class IEnumMixin {
    }
}
