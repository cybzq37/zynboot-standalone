package com.zynboot.sys.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

    private String parentId;
    private String permCode;
    private String permName;
    private Integer permType;
    private String path;
    private Integer sort;
    private Boolean visible;
    private Integer status;
    private String remark;
}
