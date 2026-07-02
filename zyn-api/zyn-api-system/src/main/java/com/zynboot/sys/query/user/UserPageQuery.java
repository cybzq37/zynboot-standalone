package com.zynboot.sys.query.user;

import lombok.Data;

@Data
public class UserPageQuery {

    private String username;
    private String nickname;
    private String phone;
    private Integer status;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
