package com.taja.crm.crm_backend.service;
import com.taja.crm.crm_backend.model.*;
import com.taja.crm.crm_backend.repo.*;
import java.util.*;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class PermissionService {
 private final UserRepository users;
 private final RoleRepository roles;
 private final SecurityAuditRepository audits;
 private final Clock clock;
 public User actor(String id) {
  if(id==null) throw new QuoteException(HttpStatus.UNAUTHORIZED,"UNAUTHORIZED","請先登入。");
  return users.findById(id).orElseThrow(()->new QuoteException(HttpStatus.UNAUTHORIZED,"UNAUTHORIZED","請重新登入。"));
 }
 public boolean admin(User user) {return user.getRole()!=null&&"ADMIN".equals(user.getRole().getCode());}
 public boolean system(Role role) {return PermissionCatalog.SYSTEM_ROLES.contains(role.getCode());}
 public Set<String> effective(Role role) {
  if(role==null) return Set.of();
  return switch(role.getCode()) {
   case "ADMIN" -> PermissionCatalog.CODES;
   case "USER" -> Set.of();
   case "MANAGER" -> {
    Set<String> result=new TreeSet<>(PermissionCatalog.CODES);
    result.removeIf(c->c.startsWith("users.")||c.startsWith("roles.")||c.startsWith("permissions."));
    yield result;
   }
   default -> {
    Set<String> result=new TreeSet<>(role.getPermissionCodes());result.retainAll(PermissionCatalog.CODES);yield result;
   }
  };
 }
 public Set<String> permissions(String id) {return effective(actor(id).getRole());}
 public boolean has(String id,String permission) {return permissions(id).contains(permission);}
 public void require(String id,String permission) {
  if(!has(id,permission)) deny("缺少操作權限："+permission);
 }
 public void deny(String message) {throw new QuoteException(HttpStatus.FORBIDDEN,"PERMISSION_DENIED",message);}
 // Serialize role edits and assignments, including last-admin checks, with one stable guard row.
 @Transactional public void lockAdministration() {roles.lockAdministration().orElseThrow(()->new IllegalStateException("缺少 ADMIN 角色"));}
 public boolean assignable(User actor,Role role) {
  if("USER".equals(role.getCode())) return true;
  if(!effective(actor.getRole()).contains("users.assign-role")) return false;
  if(system(role)&&!admin(actor)) return false;
  return effective(actor.getRole()).containsAll(effective(role));
 }
 public void requireAssignable(String actorId,Role role) {
  if(!assignable(actor(actorId),role)) deny("不可指派超出您權限範圍的角色。");
 }
 public void manageTarget(String actorId,User target) {
  User actor=actor(actorId);
  if(target.getRole()!=null && ((system(target.getRole())&&!"USER".equals(target.getRole().getCode())&&!admin(actor))
       || !effective(actor.getRole()).containsAll(effective(target.getRole()))))
   deny("不可管理權限高於自己的帳號。");
 }
 @Transactional public void audit(String actorId,String action,String target,String before,String after) {
  SecurityAudit entry=new SecurityAudit();entry.setAt(clock.instant());entry.setActorId(actorId);
  entry.setActorName(actorId==null?"公開註冊":actor(actorId).getUsername());
  entry.setAction(action);entry.setTargetId(target);entry.setBeforeValue(before);entry.setAfterValue(after);audits.save(entry);
 }
}
