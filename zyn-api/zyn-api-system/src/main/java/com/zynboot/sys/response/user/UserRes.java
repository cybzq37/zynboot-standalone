package com.zynboot.sys.response.user;

import com.zynboot.kit.jackson.plugins.sensitive.Sensitive;
import com.zynboot.kit.jackson.plugins.sensitive.SensitiveStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRes {

    String id;
    String username;
    String nickname;
    @Sensitive(strategy = SensitiveStrategy.NAME)
    String realName;
    @Sensitive(strategy = SensitiveStrategy.EMAIL)
    String email;
    @Sensitive(strategy = SensitiveStrategy.PHONE)
    String phone;
    String avatar;
    Integer gender;
    Integer status;
    String remark;
}
