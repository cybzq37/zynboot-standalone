package com.zynboot.map.response.group;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Jacksonized
@Builder
public class GroupTreeRes {

    String id;
    String parentId;
    String name;
    String description;
    Integer sortOrder;
    String icon;
    String color;
    List<GroupTreeRes> children;
}
