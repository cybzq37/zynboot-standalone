package com.zynboot.sys.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_resource")
public class SysResource extends BaseEntity {

    private String permissionId;
    private String resName;
    private Integer resType;
    private String requestMethod;
    private String requestPath;
    private Integer status;
    private String remark;
}
