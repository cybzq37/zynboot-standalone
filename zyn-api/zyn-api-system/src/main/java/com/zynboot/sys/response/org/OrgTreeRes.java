package com.zynboot.sys.response.org;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class OrgTreeRes {

    String id;
    String parentId;
    String orgCode;
    String orgName;
    Integer orgType;
    Integer sort;
    List<OrgTreeRes> children;
}
