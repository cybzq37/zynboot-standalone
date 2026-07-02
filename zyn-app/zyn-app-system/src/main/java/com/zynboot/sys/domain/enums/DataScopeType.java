package com.zynboot.sys.domain.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zynboot.kit.enums.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum DataScopeType implements IEnum<Integer> {
    ALL(1, "全部数据"),
    DEPT_AND_CHILDREN(2, "本部门及以下"),
    DEPT_ONLY(3, "本部门"),
    SELF_ONLY(4, "仅本人"),
    ;
    private final Integer code;
    private final String desc;
}
