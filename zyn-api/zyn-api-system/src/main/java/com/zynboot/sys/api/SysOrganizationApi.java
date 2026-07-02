package com.zynboot.sys.api;
import com.zynboot.infra.exchange.ExchangeClient;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.sys.command.org.OrgSaveCmd;
import com.zynboot.sys.response.org.OrgRes;
import com.zynboot.sys.response.org.OrgTreeRes;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.*;

import java.util.List;

@ExchangeClient("sys")
@HttpExchange("/sys/api/v1/org")
public interface SysOrganizationApi {

    @GetExchange
    ApiResponse<List<OrgRes>> list();

    @GetExchange("/tree")
    ApiResponse<List<OrgTreeRes>> tree();

    @GetExchange("/{id}")
    ApiResponse<OrgRes> getById(@PathVariable String id);

    @PostExchange
    ApiResponse<Void> create(@RequestBody OrgSaveCmd cmd);

    @PutExchange("/{id}")
    ApiResponse<Void> update(@PathVariable String id, @RequestBody OrgSaveCmd cmd);

    @DeleteExchange("/{id}")
    ApiResponse<Void> delete(@PathVariable String id);
}
