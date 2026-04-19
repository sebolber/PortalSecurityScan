package com.ahs.cvm.api.parameter;

import com.ahs.cvm.application.parameter.SystemParameterAuditLogView;
import com.ahs.cvm.application.parameter.SystemParameterService;
import com.ahs.cvm.application.parameter.SystemParameterView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/parameters")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CVM_ADMIN')")
public class SystemParameterController {

    private final SystemParameterService service;

    @GetMapping
    public List<SystemParameterView> list(@RequestParam(required = false) String category) {
        return service.list(category);
    }

    @GetMapping("/{id}")
    public SystemParameterView get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<SystemParameterView> create(
            @Valid @RequestBody SystemParameterRequests.CreateRequest request,
            Principal principal
    ) {
        SystemParameterView view = service.create(request.toCommand(), actor(principal));
        return ResponseEntity.ok(view);
    }

    @PutMapping("/{id}")
    public SystemParameterView update(
            @PathVariable UUID id,
            @Valid @RequestBody SystemParameterRequests.UpdateRequest request,
            Principal principal
    ) {
        return service.update(id, request.toCommand(), actor(principal));
    }

    @PatchMapping("/{id}/value")
    public SystemParameterView changeValue(
            @PathVariable UUID id,
            @RequestBody SystemParameterRequests.ChangeValueRequest request,
            Principal principal
    ) {
        return service.changeValue(id, request.toCommand(), actor(principal));
    }

    @PatchMapping("/{id}/reset")
    public SystemParameterView reset(@PathVariable UUID id, Principal principal) {
        return service.reset(id, actor(principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Principal principal) {
        service.delete(id, actor(principal));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/audit-log")
    public List<SystemParameterAuditLogView> auditLog(@RequestParam(required = false) UUID parameterId) {
        return service.auditLog(parameterId);
    }

    private String actor(Principal principal) {
        return principal != null ? principal.getName() : "system";
    }
}
