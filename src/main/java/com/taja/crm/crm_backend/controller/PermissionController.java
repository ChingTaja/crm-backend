package com.taja.crm.crm_backend.controller;
import com.taja.crm.crm_backend.dto.role.PermissionResponse;
import com.taja.crm.crm_backend.service.PermissionCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController
public class PermissionController {
 @GetMapping("/api/permissions") public List<PermissionResponse> findAllPermissions() {return PermissionCatalog.ALL;}
}
