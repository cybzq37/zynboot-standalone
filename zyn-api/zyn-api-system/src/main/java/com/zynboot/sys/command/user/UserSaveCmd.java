package com.zynboot.sys.command.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户保存命令（创建 / 更新）。
 * <p>
 * id 为空时创建，id 非空时更新。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSaveCmd {

    /** 用户 ID（更新时必填，创建时为空）。 */
    String id;

    /** 登录用户名。 */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50")
    String username;

    /** 密码（创建时必填，更新时可选）。 */
    String password;

    /** 昵称。 */
    @Size(max = 50, message = "昵称长度不能超过50")
    String nickname;

    /** 真实姓名。 */
    @Size(max = 50, message = "真实姓名长度不能超过50")
    String realName;

    /** 邮箱。 */
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100")
    String email;

    /** 手机号。 */
    @Size(max = 20, message = "手机号长度不能超过20")
    String phone;

    /** 头像 URL。 */
    @Size(max = 500, message = "头像URL长度不能超过500")
    String avatar;

    /** 性别：0=未知 1=男 2=女。 */
    Integer gender;

    /** 状态：0=禁用 1=启用。 */
    Integer status;

    /** 备注。 */
    @Size(max = 500, message = "备注长度不能超过500")
    String remark;
}
