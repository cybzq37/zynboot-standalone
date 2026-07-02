package com.zynboot.sys.command.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 资源保存命令（创建 / 更新）。
 * <p>
 * id 为空时创建，id 非空时更新。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceSaveCmd {

    /** 资源 ID（更新时必填，创建时为空）。 */
    String id;

    /** 关联权限 ID。 */
    @NotBlank(message = "关联权限ID不能为空")
    String permissionId;

    /** 资源名称。 */
    @NotBlank(message = "资源名称不能为空")
    @Size(max = 100, message = "资源名称长度不能超过100")
    String resName;

    /** 资源类型：1=API 2=文件 3=数据。 */
    @NotNull(message = "资源类型不能为空")
    Integer resType;

    /** 请求方法（GET/POST/PUT/DELETE）。 */
    @NotBlank(message = "请求方法不能为空")
    @Size(max = 10, message = "请求方法长度不能超过10")
    String requestMethod;

    /** 请求路径。 */
    @NotBlank(message = "请求路径不能为空")
    @Size(max = 200, message = "请求路径长度不能超过200")
    String requestPath;

    /** 状态：0=禁用 1=启用。 */
    Integer status;

    /** 备注。 */
    @Size(max = 500, message = "备注长度不能超过500")
    String remark;
}
