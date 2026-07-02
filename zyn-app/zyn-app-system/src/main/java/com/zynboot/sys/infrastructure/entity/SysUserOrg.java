package com.zynboot.sys.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_org")
public class SysUserOrg extends RelationEntity {

    private String userId;
    private String orgId;
}
