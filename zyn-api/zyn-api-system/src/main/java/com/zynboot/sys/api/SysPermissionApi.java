package com.zynboot.sys.api;
import com.zynboot.infra.exchange.ExchangeClient;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.sys.command.permission.PermissionSaveCmd;
import com.zynboot.sys.query.permission.PermissionQuery;
import com.zynboot.sys.response.permission.MenuTreeRes;
import com.zynboot.sys.response.permission.PermissionRes;
import com.zynboot.infra.exchange.query.HttpQuery;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.*;

import java.util.List;

@ExchangeClient("sys")
@HttpExchange("/sys/api/v1/permission")
public interface SysPermissionApi {

    @GetExchange
    ApiResponse<List<PermissionRes>> list(@HttpQuery PermissionQuery query);

    @GetExchange("/tree")
    ApiResponse<List<MenuTreeRes>> tree();

    @GetExchange("/{id}")
    ApiResponse<PermissionRes> getById(@PathVariable String id);

    @PostExchange
    ApiResponse<Void> create(@RequestBody PermissionSaveCmd cmd);

    @PutExchange("/{id}")
    ApiResponse<Void> update(@PathVariable String id, @RequestBody PermissionSaveCmd cmd);

    @DeleteExchange("/{id}")
    ApiResponse<Void> delete(@PathVariable String id);
}
