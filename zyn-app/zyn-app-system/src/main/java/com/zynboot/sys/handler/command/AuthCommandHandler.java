package com.zynboot.sys.handler.command;

import com.zynboot.sys.response.user.LoginRes;
import com.zynboot.sys.response.user.UserInfoRes;

public interface AuthCommandHandler {

    LoginRes login(String username, String password);

    UserInfoRes getCurrentUserInfo();
}
