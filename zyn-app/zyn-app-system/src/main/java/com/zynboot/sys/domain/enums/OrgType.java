package com.zynboot.sys.domain.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zynboot.kit.enums.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum OrgType implements IEnum<Integer> {
    HEADQUARTERS(1, "总公司"),
    BRANCH(2, "分公司"),
    DEPARTMENT(3, "部门"),
    GROUP(4, "小组"),
    ;
    private final Integer code;
    private final String desc;
}
