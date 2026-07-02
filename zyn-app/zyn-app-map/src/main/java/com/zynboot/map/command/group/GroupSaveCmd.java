package com.zynboot.map.command.group;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupSaveCmd {

    String id;
    String parentId;

    @NotBlank(message = "分组名称不能为空")
    String name;

    String description;
    Integer sortOrder;
    String icon;
    String color;
}
