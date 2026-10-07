package com.taja.crm.crm_backend.service;
import com.taja.crm.crm_backend.model.*;
import com.taja.crm.crm_backend.repo.*;
import com.taja.crm.crm_backend.dto.role.*;
import com.taja.crm.crm_backend.dto.user.UpdateUserRequest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class RoleSecurityTransactionTests {
 @Autowired RoleService service;
 @Autowired UserService userService;
 @Autowired RoleRepository roles;
 @Autowired PlatformTransactionManager transactions;
 @MockitoSpyBean UserRepository users;
 @MockitoSpyBean SecurityAuditRepository audits;
 User admin() {
  User u=new User();u.setUsername("security-"+UUID.randomUUID());u.setEmail(u.getUsername()+"@example.com");
  u.setPasswordHash("unused");u.setRole(roles.findByCode("ADMIN").orElseThrow());return users.saveAndFlush(u);
 }
 @Test void auditFailureRollsBackRoleAndPermissions() {
  long count=roles.count(), auditCount=audits.count();
  doThrow(new IllegalStateException("audit unavailable")).when(audits).save(any(SecurityAudit.class));
  assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).execute(status-> {
   User actor=admin();
   return service.createRoles(actor.getId(),new CreateRoleRequest("TX_"+UUID.randomUUID().toString().replace("-",""),
    "transaction",null,List.of("orders.read")));
  }));
  assertEquals(count,roles.count());assertEquals(auditCount,audits.count());
 }
 @Test @Transactional void lastAdministratorCannotBeDeletedOrReassigned() {
  User actor=admin(), target=admin();
  // Exercise the last-admin branch without altering real administrators in the development database.
  doReturn(1L).when(users).countByRoleCodeAndEnabledTrue("ADMIN");
  QuoteException deletion=assertThrows(QuoteException.class,()->userService.deleteUsers(actor.getId(),target.getId()));
  assertEquals("LAST_ADMIN_PROTECTED",deletion.getCode());
  QuoteException assignment=assertThrows(QuoteException.class,()->userService.updateUsers(actor.getId(),target.getId(),
   new UpdateUserRequest(target.getUsername(),target.getEmail(),roles.findByCode("USER").orElseThrow().getId())));
  assertEquals("LAST_ADMIN_PROTECTED",assignment.getCode());
  QuoteException disabled=assertThrows(QuoteException.class,()->userService.updateUserStatus(actor.getId(),target.getId(),false));
  assertEquals("LAST_ADMIN_PROTECTED",disabled.getCode());
  assertTrue(target.isEnabled());
  assertTrue(users.existsById(target.getId()));
 }
}
