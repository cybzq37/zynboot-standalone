package com.zynboot.sys.domain.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zynboot.kit.enums.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum PermType implements IEnum<Integer> {
    DIRECTORY(1, "目录"),
    MENU(2, "菜单"),
    BUTTON(3, "按钮"),
    API(4, "API"),
    ;
    private final Integer code;
    private final String desc;
}
