package com.zynboot.sys.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String password;
    private String nickname;
    private String realName;
    private String email;
    private String phone;
    private String avatar;
    private Integer gender;
    private Integer status;
    private String loginIp;
    private LocalDateTime loginTime;
    private LocalDateTime pwdUpdateTime;
    private Integer loginAttempts;
    private LocalDateTime lockTime;
    private String remark;
}
