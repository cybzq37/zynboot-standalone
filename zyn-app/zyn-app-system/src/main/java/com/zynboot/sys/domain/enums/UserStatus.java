package com.zynboot.sys.domain.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zynboot.kit.enums.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum UserStatus implements IEnum<Integer> {
    NORMAL(1, "正常"),
    DISABLED(0, "停用"),
    LOCKED(2, "锁定"),
    ;
    private final Integer code;
    private final String desc;
}
