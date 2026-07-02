package com.zynboot.app.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.sys.api.SysAuthApi;
import com.zynboot.sys.api.SysUserApi;
import com.zynboot.sys.response.user.UserInfoRes;
import com.zynboot.sys.response.user.UserRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sys-demo")
public class SysDemoController {

    private final SysUserApi sysUserApi;
    private final SysAuthApi sysAuthApi;

    @GetMapping("/user/{id}")
    public ApiResponse<UserRes> getUser(@PathVariable String id) {
        return sysUserApi.getById(id);
    }

    @GetMapping("/user/info")
    public ApiResponse<UserInfoRes> getUserInfo() {
        return sysAuthApi.info();
    }
}
