package com.zynboot.app.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zynboot.app.dto.HealthDto;
import com.zynboot.kit.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/template")
public class TemplateController {

    @GetMapping("/ping")
    public ApiResponse<HealthDto> ping() {
        return ApiResponse.ok(HealthDto.builder()
                .status("UP")
                .time(OffsetDateTime.now())
                .build());
    }

    @GetMapping("/sa-login")
    public ApiResponse<Map<String, Object>> saLogin() {
        StpUtil.login(10001L);
        Map<String, Object> payload = new HashMap<>();
        payload.put("token", StpUtil.getTokenValue());
        payload.put("loginId", StpUtil.getLoginIdAsLong());
        return ApiResponse.ok(payload);
    }
}
