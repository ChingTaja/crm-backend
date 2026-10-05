package com.taja.crm.crm_backend.service;
import com.taja.crm.crm_backend.dto.*;
import com.taja.crm.crm_backend.dto.role.*;
import com.taja.crm.crm_backend.model.Role;
import com.taja.crm.crm_backend.repo.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class RoleService {
 private final RoleRepository roles;
 private final UserRepository users;
 private final PermissionService access;
 private QuoteException error(HttpStatus status,String code,String detail) {return new QuoteException(status,code,detail);}
 private Role find(String id) {return roles.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"ROLE_NOT_FOUND","找不到角色。"));}
 public RoleDetailResponse detail(Role role) {
  List<String> codes=access.effective(role).stream().sorted().toList();
  return new RoleDetailResponse(role.getId(),role.getCode(),role.getName(),role.getDescription(),access.system(role),
   users.countByRoleId(role.getId()),codes.size(),role.getRevision(),QuotePermissionCompatibility.pending(role),QuotePermissionCompatibility.legacy(role).stream().sorted().toList(),codes);
 }
 public RoleSummaryResponse summary(Role r) {
  var d=detail(r);return new RoleSummaryResponse(d.id(),d.code(),d.name(),d.description(),d.system(),d.userCount(),d.permissionCount(),d.revision(),d.quotePermissionMigrationRequired(),d.legacyQuotePermissionCodes());
 }
 public PageResponse<RoleSummaryResponse> findAllRoles(String actor,int page,int size,String keyword) {
  access.require(actor,"roles.read");
  String term=keyword==null?"":keyword.strip().toLowerCase(Locale.ROOT);
  if(!term.isEmpty()) term="%"+term.replace("!","!!").replace("%","!%").replace("_","!_")+"%";
  return PageResponse.fromPage(roles.search(term,Pagination.of(page,size)).map(this::summary));
 }
 public RoleDetailResponse findByIdRole(String actor,String id) {access.require(actor,"roles.read");return detail(find(id));}
 public List<RoleOptionResponse> options(String actorId,String keyword) {
  var actor=access.actor(actorId);var codes=access.effective(actor.getRole());
  if(!codes.contains("users.create")&&!codes.contains("users.update")&&!codes.contains("users.assign-role"))
   access.deny("沒有帳號角色選項的存取權限。");
  String term=keyword==null?"":keyword.strip().toLowerCase(Locale.ROOT);
  return roles.findAll().stream().filter(r->access.assignable(actor,r))
   .filter(r->r.getCode().toLowerCase(Locale.ROOT).contains(term)||r.getName().toLowerCase(Locale.ROOT).contains(term))
   .sorted(Comparator.comparing(Role::getCode)).map(r->new RoleOptionResponse(r.getId(),r.getCode(),r.getName())).toList();
 }
 private Set<String> validate(String actor,List<String> codes) {
  Set<String> values=new TreeSet<>(codes);
  if(!PermissionCatalog.CODES.containsAll(values)) throw error(HttpStatus.BAD_REQUEST,"ROLE_UNKNOWN_PERMISSION","包含系統不支援的權限代碼。");
  if(!access.permissions(actor).containsAll(values)) access.deny("不可授予自己沒有的權限。");
  return values;
 }
 private void manageable(String actorId,Role role) {
  if(access.system(role)) throw error(HttpStatus.CONFLICT,"ROLE_SYSTEM_PROTECTED","內建角色不可修改或刪除。");
  var actor=access.actor(actorId);
  if(actor.getRole()!=null&&actor.getRole().getId().equals(role.getId())) access.deny("不可修改或刪除自己的角色。");
  if(!access.effective(actor.getRole()).containsAll(access.effective(role))) access.deny("不可管理權限高於自己的角色。");
 }
 private String snapshot(Role role) {
  return "code="+role.getCode()+";name="+role.getName()+";description="+role.getDescription()
   +";revision="+role.getRevision()+";quotePermissionVersion="+role.getQuotePermissionVersion()+";permissionCodes="+role.getPermissionCodes().stream().sorted().toList();
 }
 @Transactional public RoleDetailResponse createRoles(String actor,CreateRoleRequest request) {
  access.lockAdministration();access.require(actor,"roles.create");
  String code=request.code().toUpperCase(Locale.ROOT);
  if(PermissionCatalog.SYSTEM_ROLES.contains(code)||roles.findByCode(code).isPresent())
   throw error(HttpStatus.CONFLICT,"ROLE_CODE_EXISTS","角色代碼已存在或為系統保留代碼。");
  Role role=new Role();role.setCode(code);role.setName(request.name().strip());role.setDescription(request.description());
  role.setPermissionCodes(validate(actor,request.permissionCodes()));roles.saveAndFlush(role);
  access.audit(actor,"RoleCreated",role.getId(),null,snapshot(role));return detail(role);
 }
 @Transactional public RoleDetailResponse updateRoles(String actor,String id,UpdateRoleRequest request) {
  access.lockAdministration();access.require(actor,"roles.update");Role role=find(id);manageable(actor,role);
  if(role.getRevision()!=request.expectedRevision())
   throw error(HttpStatus.CONFLICT,"ROLE_REVISION_CONFLICT","角色已被其他使用者更新，請重新載入。");
  if(QuotePermissionCompatibility.pending(role)&&!access.admin(access.actor(actor)))
   throw error(HttpStatus.FORBIDDEN,"QUOTE_PERMISSION_CONFIRMATION_REQUIRED","此角色具有部分舊報價授權，必須由管理員確認後儲存。");
  String before=snapshot(role);Set<String> codes=validate(actor,request.permissionCodes());
  role.setQuotePermissionVersion(1);
  role.setName(request.name().strip());role.setDescription(request.description());role.getPermissionCodes().clear();role.getPermissionCodes().addAll(codes);
  role.setRevision(role.getRevision()+1);roles.flush();access.audit(actor,"RoleUpdated",id,before,snapshot(role));return detail(role);
 }
 @Transactional public void deleteRoles(String actor,String id) {
  access.lockAdministration();access.require(actor,"roles.delete");Role role=find(id);manageable(actor,role);
  if(users.existsByRoleId(id)) throw error(HttpStatus.CONFLICT,"ROLE_IN_USE","此角色仍有使用者，請先重新指派角色。");
  access.audit(actor,"RoleDeleted",id,snapshot(role),null);roles.delete(role);roles.flush();
 }
}
