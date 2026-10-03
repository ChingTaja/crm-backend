package com.taja.crm.crm_backend.controller;
import com.taja.crm.crm_backend.dto.*;
import com.taja.crm.crm_backend.dto.role.*;
import com.taja.crm.crm_backend.service.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/roles") @RequiredArgsConstructor
public class RoleController {
 private final RoleService service;
 @GetMapping public PageResponse<RoleSummaryResponse> findAllRoles(Principal actor,@RequestParam(defaultValue="0") int page,
  @RequestParam(defaultValue="20") int size,@RequestParam(required=false) String keyword) {
  return service.findAllRoles(actor.getName(),page,size,keyword);
 }
 @GetMapping("/options") public List<RoleOptionResponse> roleOptions(Principal actor,@RequestParam(required=false) String keyword) {
  return service.options(actor.getName(),keyword);
 }
 @GetMapping("/{id}") public RoleDetailResponse findByIdRole(Principal actor,@PathVariable String id) {return service.findByIdRole(actor.getName(),id);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public RoleDetailResponse createRoles(Principal actor,@Valid @RequestBody CreateRoleRequest request) {
  return service.createRoles(actor.getName(),request);
 }
 @PutMapping("/{id}") public RoleDetailResponse updateRoles(Principal actor,@PathVariable String id,@Valid @RequestBody UpdateRoleRequest request) {
  return service.updateRoles(actor.getName(),id,request);
 }
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteRoles(Principal actor,@PathVariable String id) {
  service.deleteRoles(actor.getName(),id);
 }
}
