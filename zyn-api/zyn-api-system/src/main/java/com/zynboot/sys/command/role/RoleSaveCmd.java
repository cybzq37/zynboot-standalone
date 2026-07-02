package com.zynboot.sys.command.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色保存命令（创建 / 更新）。
 * <p>
 * id 为空时创建，id 非空时更新。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleSaveCmd {

    /** 角色 ID（更新时必填，创建时为空）。 */
    String id;

    /** 角色编码。 */
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码长度不能超过50")
    String roleCode;

    /** 角色名称。 */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50")
    String roleName;

    /** 数据范围：1=全部 2=本部门及子部门 3=本部门 4=仅本人。 */
    Integer dataScope;

    /** 排序。 */
    Integer sort;

    /** 状态：0=禁用 1=启用。 */
    Integer status;

    /** 备注。 */
    @Size(max = 500, message = "备注长度不能超过500")
    String remark;
    Integer roleType;
}
