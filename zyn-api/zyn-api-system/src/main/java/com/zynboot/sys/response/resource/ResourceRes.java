package com.zynboot.sys.response.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRes {

    String id;
    String permissionId;
    String resName;
    Integer resType;
    String requestMethod;
    String requestPath;
    Integer status;
    String remark;
}
