package com.zynboot.sys.response.permission;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class MenuTreeRes {

    String id;
    String parentId;
    String permName;
    Integer permType;
    String path;
    Integer sort;
    Boolean visible;
    List<MenuTreeRes> children;
}
