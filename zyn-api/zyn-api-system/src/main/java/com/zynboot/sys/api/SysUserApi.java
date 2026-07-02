package com.zynboot.sys.api;
import com.zynboot.infra.exchange.ExchangeClient;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.sys.command.user.UserSaveCmd;
import com.zynboot.sys.query.user.UserPageQuery;
import com.zynboot.sys.response.PageRes;
import com.zynboot.sys.response.user.UserRes;
import com.zynboot.infra.exchange.query.HttpQuery;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.*;

@ExchangeClient("sys")
@HttpExchange("/sys/api/v1/user")
public interface SysUserApi {

    @GetExchange
    ApiResponse<PageRes<UserRes>> page(@HttpQuery UserPageQuery query);

    @GetExchange("/{id}")
    ApiResponse<UserRes> getById(@PathVariable String id);

    @PostExchange
    ApiResponse<Void> create(@RequestBody UserSaveCmd cmd);

    @PutExchange("/{id}")
    ApiResponse<Void> update(@PathVariable String id, @RequestBody UserSaveCmd cmd);

    @DeleteExchange("/{id}")
    ApiResponse<Void> delete(@PathVariable String id);
}
