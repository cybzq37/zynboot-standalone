package com.zynboot.sys.command.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限保存命令（创建 / 更新）。
 * <p>
 * id 为空时创建，id 非空时更新。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionSaveCmd {

    /** 权限 ID（更新时必填，创建时为空）。 */
    String id;

    /** 父级 ID。 */
    String parentId;

    /** 权限编码。 */
    @NotBlank(message = "权限编码不能为空")
    @Size(max = 100, message = "权限编码长度不能超过100")
    String permCode;

    /** 权限名称。 */
    @NotBlank(message = "权限名称不能为空")
    @Size(max = 100, message = "权限名称长度不能超过100")
    String permName;

    /** 权限类型：1=目录 2=菜单 3=按钮 4=API。 */
    @NotNull(message = "权限类型不能为空")
    Integer permType;

    /** 路由路径。 */
    @Size(max = 200, message = "路由路径长度不能超过200")
    String path;

    /** 排序。 */
    Integer sort;

    /** 是否可见。 */
    Boolean visible;

    /** 状态：0=禁用 1=启用。 */
    Integer status;

    /** 备注。 */
    @Size(max = 500, message = "备注长度不能超过500")
    String remark;
}
