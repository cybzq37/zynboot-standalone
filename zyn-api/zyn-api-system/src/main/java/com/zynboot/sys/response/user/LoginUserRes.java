package com.zynboot.sys.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserRes implements Serializable {

    private static final long serialVersionUID = 1L;

    String userId;
    String username;
    String nickname;
    String realName;
    String email;
    String phone;
    String avatar;
    Integer status;
    Set<String> roleCodes;
    Set<String> permCodes;
}
