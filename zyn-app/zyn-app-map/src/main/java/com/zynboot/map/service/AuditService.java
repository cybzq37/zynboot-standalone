package com.zynboot.map.service;

import com.zynboot.kit.util.IdUtils;
import com.zynboot.map.infrastructure.entity.MapOperationLog;
import com.zynboot.map.infrastructure.mapper.MapOperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 操作审计服务。
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final MapOperationLogMapper mapper;
    private final HttpServletRequest request;

    public void log(String targetType, String targetId, String action,
                    String operatorId, String operatorName, String detailJson) {
        MapOperationLog log = new MapOperationLog();
        log.setId(IdUtils.uuid());
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setAction(action);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setDetailJson(detailJson);
        log.setIp(getClientIp());
        log.setCreateTime(LocalDateTime.now());
        mapper.insert(log);
    }

    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) return ip;
        return request.getRemoteAddr();
    }
}
