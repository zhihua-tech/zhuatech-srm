/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.service;

import cn.zhuatech.srm.model.AuditLog;
import cn.zhuatech.srm.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 记录关键业务操作的前后内容、操作者、时间和客户端地址。 */
@Service
public class AuditService {
    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<HttpServletRequest> requestProvider;
    public AuditService(AuditLogRepository repository, ObjectMapper objectMapper,
                        ObjectProvider<HttpServletRequest> requestProvider) {
        this.repository = repository; this.objectMapper = objectMapper; this.requestProvider = requestProvider;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String organizationCode, String actor, String module, String action,
                       String objectType, Object objectId, Object before, Object after) {
        HttpServletRequest request = requestProvider.getIfAvailable();
        String ip = request == null ? null : firstForwardedAddress(request);
        repository.save(new AuditLog(organizationCode, actor, module, action, objectType,
            objectId == null ? null : String.valueOf(objectId), json(before), json(after), ip));
    }

    public Page<AuditLog> search(String organizationCode, Pageable pageable) {
        return repository.findByOrganizationCodeOrderByOccurredAtDesc(organizationCode, pageable);
    }

    public Page<AuditLog> search(String organizationCode, String actor, String module,
                                 LocalDateTime fromTime, LocalDateTime toTime, Pageable pageable) {
        return repository.search(organizationCode, normalize(actor), normalize(module), fromTime, toTime, pageable);
    }

    public List<AuditLog> history(String organizationCode, String objectType, Object objectId) {
        return repository.findByOrganizationCodeAndObjectTypeAndObjectIdOrderByOccurredAtDesc(
            organizationCode, objectType, String.valueOf(objectId));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String json(Object value) {
        if (value == null) return null;
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { return "{\"serializationError\":true}"; }
    }

    private String firstForwardedAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
}
