package com.zynboot.sys.response.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleRes {

    String id;
    String roleCode;
    String roleName;
    Integer roleType;
    Integer sort;
    Integer status;
    Integer dataScope;
    String remark;
}
