package com.zynboot.sys.response.permission;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionRes {

    String id;
    String parentId;
    String permCode;
    String permName;
    Integer permType;
    String path;
    Integer sort;
    Boolean visible;
    Integer status;
    String remark;
}
