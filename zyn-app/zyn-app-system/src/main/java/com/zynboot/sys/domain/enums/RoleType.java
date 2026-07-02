package com.zynboot.sys.domain.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zynboot.kit.enums.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum RoleType implements IEnum<Integer> {
    SYSTEM(0, "系统内置"),
    CUSTOM(1, "自定义"),
    ;
    private final Integer code;
    private final String desc;
}
