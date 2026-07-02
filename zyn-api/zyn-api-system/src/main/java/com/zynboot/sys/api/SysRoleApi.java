package com.zynboot.sys.api;
import com.zynboot.infra.exchange.ExchangeClient;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.sys.command.role.RoleSaveCmd;
import com.zynboot.sys.query.role.RoleQuery;
import com.zynboot.sys.response.role.RoleRes;
import com.zynboot.infra.exchange.query.HttpQuery;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.*;

import java.util.List;

@ExchangeClient("sys")
@HttpExchange("/sys/api/v1/role")
public interface SysRoleApi {

    @GetExchange
    ApiResponse<List<RoleRes>> list(@HttpQuery RoleQuery query);

    @GetExchange("/{id}")
    ApiResponse<RoleRes> getById(@PathVariable String id);

    @PostExchange
    ApiResponse<Void> create(@RequestBody RoleSaveCmd cmd);

    @PutExchange("/{id}")
    ApiResponse<Void> update(@PathVariable String id, @RequestBody RoleSaveCmd cmd);

    @DeleteExchange("/{id}")
    ApiResponse<Void> delete(@PathVariable String id);
}
