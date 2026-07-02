package com.zynboot.kit.enums;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Arrays;

/**
 * 通用枚举接口，用于 code-value 模式的枚举。
 * <p>
 * 实现此接口后：
 * <ul>
 *   <li>数据库存储：自动取 {@link #getCode()} 的值</li>
 *   <li>前端序列化：自动返回 {@code {"code":1,"desc":"男"}} 格式</li>
 *   <li>前端反序列化：自动按 code 值匹配枚举</li>
 * </ul>
 * <pre>
 * &#64;JsonFormat(shape = JsonFormat.Shape.OBJECT)
 * public enum Gender implements IEnum&lt;Integer&gt; {
 *     MALE(1, "男"), FEMALE(0, "女");
 *     private final Integer code;
 *     private final String desc;
 *     Gender(Integer code, String desc) { this.code = code; this.desc = desc; }
 *     public Integer getCode() { return code; }
 *     public String getDesc() { return desc; }
 * }
 * </pre>
 *
 * @param <T> code 值类型（通常为 Integer 或 String）
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public interface IEnum<T> {

    /**
     * 枚举编码，用于数据库存储和前端交互。
     */
    T getCode();

    /**
     * 枚举描述，用于前端展示。
     */
    String getDesc();

    /**
     * 根据 code 值查找枚举常量。
     *
     * @param enumType 枚举类型
     * @param code     code 值
     * @return 匹配的枚举常量，未匹配返回 null
     */
    static <E extends Enum<E> & IEnum<?>> E ofCode(Class<E> enumType, Object code) {
        if (code == null || enumType == null) {
            return null;
        }
        String codeStr = String.valueOf(code);
        return Arrays.stream(enumType.getEnumConstants())
                .filter(e -> e.getCode() != null && String.valueOf(e.getCode()).equals(codeStr))
                .findFirst()
                .orElse(null);
    }
}
