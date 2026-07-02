package com.zynboot.infra.satoken.service;

import cn.dev33.satoken.stp.StpInterface;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限接口默认实现
 * <p>
 * 提供空实现作为占位符，业务模块应覆盖此实现以提供实际权限逻辑
 *
 * @author lichunqing
 */
public class SaPermissionImpl implements StpInterface {

    /**
     * 获取权限列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    /**
     * 获取角色列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return Collections.emptyList();
    }
}
