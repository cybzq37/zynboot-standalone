package com.zynboot.sys.response.org;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgRes {

    String id;
    String parentId;
    String orgCode;
    String orgName;
    Integer orgType;
    String leaderId;
    String phone;
    String email;
    Integer sort;
    Integer status;
    String remark;
}
