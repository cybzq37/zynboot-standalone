package com.zynboot.sys.controller;

import com.zynboot.kit.response.ApiResponse;
import com.zynboot.sys.command.user.LoginCmd;
import com.zynboot.sys.handler.command.AuthCommandHandler;
import com.zynboot.sys.response.user.LoginRes;
import com.zynboot.sys.response.user.UserInfoRes;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthCommandHandler authService;

    @PostMapping("/login")
    public ApiResponse<LoginRes> login(@Valid @RequestBody LoginCmd cmd) {
        return ApiResponse.ok(authService.login(cmd.getUsername(), cmd.getPassword()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        StpUtil.logout();
        return ApiResponse.ok(null);
    }

    @GetMapping("/info")
    public ApiResponse<UserInfoRes> info() {
        return ApiResponse.ok(authService.getCurrentUserInfo());
    }
}
