package com.zynboot.sys.api;

import com.zynboot.infra.exchange.ExchangeClient;
import com.zynboot.kit.response.ApiResponse;
import com.zynboot.sys.command.user.LoginCmd;
import com.zynboot.sys.response.user.LoginRes;
import com.zynboot.sys.response.user.UserInfoRes;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@ExchangeClient("sys")
@HttpExchange("/sys/api/v1/auth")
public interface SysAuthApi {

    @PostExchange("/login")
    ApiResponse<LoginRes> login(@RequestBody LoginCmd cmd);

    @PostExchange("/logout")
    ApiResponse<Void> logout();

    @GetExchange("/info")
    ApiResponse<UserInfoRes> info();
}
