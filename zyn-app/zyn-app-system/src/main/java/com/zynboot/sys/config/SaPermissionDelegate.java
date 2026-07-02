package com.zynboot.sys.config;

import cn.dev33.satoken.stp.StpInterface;
import com.zynboot.sys.handler.query.PermissionQueryHandler;
import com.zynboot.sys.util.PermConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SaPermissionDelegate implements StpInterface {

    private final PermissionQueryHandler permissionManager;

    @Value("${zyn.satoken.root-user-id:}")
    private String rootUserId;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (isRoot(loginId)) {
            return List.of(PermConstants.ALL_PERMISSION);
        }
        String userId = String.valueOf(loginId);
        return new ArrayList<>(permissionManager.getPermCodes(userId));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (isRoot(loginId)) {
            return List.of(PermConstants.SUPER_ROLE);
        }
        String userId = String.valueOf(loginId);
        return new ArrayList<>(permissionManager.getRoleCodes(userId));
    }

    private boolean isRoot(Object loginId) {
        if (!StringUtils.hasText(rootUserId) || loginId == null) {
            return false;
        }
        return rootUserId.equals(String.valueOf(loginId));
    }
}
