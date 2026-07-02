package com.zynboot.sys.domain.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zynboot.kit.enums.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ResourceType implements IEnum<Integer> {
    API(1, "API接口"),
    FILE(2, "文件"),
    DATA(3, "数据"),
    ;
    private final Integer code;
    private final String desc;
}
