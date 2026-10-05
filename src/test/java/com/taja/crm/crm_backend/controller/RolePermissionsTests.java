package com.taja.crm.crm_backend.controller;
import com.jayway.jsonpath.JsonPath;
import com.taja.crm.crm_backend.model.*;
import com.taja.crm.crm_backend.repo.*;
import com.taja.crm.crm_backend.service.*;
import com.taja.crm.crm_backend.dto.auth.UserResponse;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class RolePermissionsTests {
 @Autowired MockMvc mvc;
 @Autowired RoleRepository roles;
 @Autowired UserRepository users;
 @Autowired SecurityAuditRepository audits;
 @Autowired JwtService jwt;
 String adminToken;
 User admin;
 @BeforeEach void setup() {
  admin=user(roles.findByCode("ADMIN").orElseThrow());adminToken=token(admin);
 }
 User user(Role role) {
  User u=new User();u.setUsername("rbac-"+UUID.randomUUID());u.setEmail(u.getUsername()+"@example.com");
  u.setPasswordHash("unused");u.setRole(role);return users.saveAndFlush(u);
 }
 String token(User u) {return "Bearer "+jwt.issue(UserResponse.fromEntity(u)).accessToken();}
 Role role(String... codes) {
  Role r=new Role();r.setCode("TEST_"+UUID.randomUUID().toString().replace("-",""));r.setName("測試角色");
  r.getPermissionCodes().addAll(List.of(codes));return roles.saveAndFlush(r);
 }
 ResultActions create(String token,String code,String permissions) throws Exception {
  return mvc.perform(post("/api/roles").header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
   .content("{\"code\":\""+code+"\",\"name\":\"業務\",\"permissionCodes\":"+permissions+"}"));
 }
 ResultActions update(String token,String id,int revision,String permissions) throws Exception {
  return mvc.perform(put("/api/roles/"+id).header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
   .content("{\"name\":\"新名稱\",\"expectedRevision\":"+revision+",\"permissionCodes\":"+permissions+"}"));
 }
 @Test void roleCrudValidatesCodesRevisionUsageAndAudit() throws Exception {
  long initial=audits.count();String code="SALES_"+UUID.randomUUID().toString().replace("-","");
  String json=create(adminToken,code,"[\"orders.read\",\"orders.read\"]").andExpect(status().isCreated())
   .andExpect(jsonPath("$.revision").value(1)).andExpect(jsonPath("$.permissionCount").value(1))
   .andExpect(jsonPath("$.system").value(false)).andReturn().getResponse().getContentAsString();
  String id=JsonPath.read(json,"$.id");
  create(adminToken,code.toLowerCase(),"[]").andExpect(status().isConflict());
  create(adminToken,"INVALID_"+UUID.randomUUID().toString().replace("-",""),"[\"orders.create\"]").andExpect(status().isBadRequest());
  mvc.perform(get("/api/roles").param("keyword",code).header("Authorization",adminToken)).andExpect(status().isOk())
   .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].permissionCodes").doesNotExist());
  update(adminToken,id,1,"[\"orders.read\",\"orders.process\"]").andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(2));
  update(adminToken,id,1,"[]").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ROLE_REVISION_CONFLICT"));
  User assigned=user(roles.findById(id).orElseThrow());
  mvc.perform(delete("/api/roles/"+id).header("Authorization",adminToken)).andExpect(status().isConflict())
   .andExpect(jsonPath("$.code").value("ROLE_IN_USE"));
  users.delete(assigned);users.flush();
  mvc.perform(delete("/api/roles/"+id).header("Authorization",adminToken)).andExpect(status().isNoContent());
  assertEquals(initial+3,audits.count());
 }
 @Test void builtinsAndSelfEscalationAreProtected() throws Exception {
  String adminRole=admin.getRole().getId();
  update(adminToken,adminRole,1,"[]").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ROLE_SYSTEM_PROTECTED"));
  mvc.perform(delete("/api/roles/"+adminRole).header("Authorization",adminToken)).andExpect(status().isConflict());
  Role delegated=role("roles.read","roles.create","roles.update","roles.delete","orders.read","users.create","users.update","users.assign-role");
  User operator=user(delegated);String token=token(operator);
  update(token,delegated.getId(),1,"[]").andExpect(status().isForbidden());
  create(token,"ESCALATE_"+UUID.randomUUID().toString().replace("-",""),"[\"orders.cancel\"]").andExpect(status().isForbidden());
  Role powerful=role("orders.read","orders.cancel");
  update(token,powerful.getId(),1,"[]").andExpect(status().isForbidden());
  mvc.perform(get("/api/roles/options").header("Authorization",token)).andExpect(status().isOk())
   .andExpect(jsonPath("$[?(@.code == 'ADMIN')]").isEmpty())
   .andExpect(jsonPath("$[?(@.code == 'MANAGER')]").isEmpty());
  mvc.perform(put("/api/users/"+operator.getId()).header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
   .content("{\"username\":\""+operator.getUsername()+"\",\"email\":\""+operator.getEmail()+"\",\"roleId\":\""+adminRole+"\"}"))
   .andExpect(status().isConflict());
  mvc.perform(put("/api/users/"+admin.getId()).header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
   .content("{\"username\":\""+admin.getUsername()+"\",\"email\":\""+admin.getEmail()+"\"}"))
   .andExpect(status().isForbidden());
 }
 @Test void revocationAppliesToSameTokenAndUserHasReadOnlyBusinessAccess() throws Exception {
  Role r=role("products.read");User u=user(r);String token=token(u);
  mvc.perform(get("/api/products").header("Authorization",token)).andExpect(status().isOk());
  mvc.perform(get("/api/auth/me").header("Authorization",token)).andExpect(status().isOk())
   .andExpect(jsonPath("$.permissionCodes[0]").value("products.read"));
  update(adminToken,r.getId(),1,"[]").andExpect(status().isOk());
  mvc.perform(get("/api/products").header("Authorization",token)).andExpect(status().isForbidden());
  mvc.perform(get("/api/auth/me").header("Authorization",token)).andExpect(status().isOk())
   .andExpect(jsonPath("$.permissionCodes").isEmpty());
  String name="lowest-"+UUID.randomUUID();
  mvc.perform(post("/api/users").header("Authorization",adminToken).contentType(MediaType.APPLICATION_JSON)
   .content("{\"username\":\""+name+"\",\"email\":\""+name+"@example.com\",\"password\":\"test-password\"}"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.role.code").value("USER"));
  User lowest=users.findByUsername(name).orElseThrow();
  mvc.perform(get("/api/auth/me").header("Authorization",token(lowest))).andExpect(status().isOk())
   .andExpect(jsonPath("$.permissionCodes",org.hamcrest.Matchers.containsInAnyOrder(
    "customers.read","contacts.read","leads.read","opportunities.read","products.read","quotes.read","orders.read")));
  for(String entity:List.of("customers","contacts","leads","opportunities","products","quotes","orders")) {
   mvc.perform(get("/api/"+entity).header("Authorization",token(lowest))).andExpect(status().isOk());
   mvc.perform(get("/api/entities/"+entity+"/fields").header("Authorization",token(lowest))).andExpect(status().isOk());
   if(!entity.equals("orders")) {
    mvc.perform(post("/api/"+entity).header("Authorization",token(lowest)).contentType(MediaType.APPLICATION_JSON).content("{}"))
     .andExpect(status().isForbidden());
    mvc.perform(put("/api/"+entity+(entity.equals("quotes")?"/missing/versions/missing":"/missing"))
     .header("Authorization",token(lowest)).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    mvc.perform(delete("/api/"+entity+"/missing").header("Authorization",token(lowest))).andExpect(status().isForbidden());
   }
  }
  mvc.perform(patch("/api/orders/missing/status").header("Authorization",token(lowest))
   .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"Processing\",\"expectedRevision\":1}"))
   .andExpect(status().isForbidden());
  for(String path:List.of("/api/roles","/api/permissions","/api/users","/api/entities/users/fields"))
   mvc.perform(get(path).header("Authorization",token(lowest))).andExpect(status().isForbidden());
 }
 @Test void assignmentRequiresPermissionAndGrantableRole() throws Exception {
  Role basicCreator=role("users.create");User creator=user(basicCreator);String token=token(creator);
  String name="created-"+UUID.randomUUID();
  String body="{\"username\":\""+name+"\",\"email\":\""+name+"@example.com\",\"password\":\"test-password\",\"roleId\":\"%s\"}";
  mvc.perform(post("/api/users").header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
   .content(body.formatted(admin.getRole().getId()))).andExpect(status().isForbidden());
  mvc.perform(post("/api/users").header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
   .content(body.formatted(roles.findByCode("USER").orElseThrow().getId()))).andExpect(status().isCreated());
  mvc.perform(get("/api/roles/options").header("Authorization",token)).andExpect(status().isOk())
   .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].code").value("USER"));
 }
 @Test void catalogJwtAndOpenapiContract() throws Exception {
  mvc.perform(get("/api/roles")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/permissions")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/permissions").header("Authorization",adminToken)).andExpect(status().isOk())
   .andExpect(jsonPath("$[?(@.code == 'orders.create')]").isEmpty())
   .andExpect(jsonPath("$[?(@.code == 'quotes.record-decision')]").isEmpty());
  String spec=mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
   .andExpect(jsonPath("$.components.schemas.RoleDetailResponse.properties.permissionCodes").exists())
   .andExpect(jsonPath("$.components.schemas.CurrentUserResponse.properties.permissionCodes").exists())
   .andExpect(jsonPath("$.paths['/api/quote-reviews'].get").exists())
   .andExpect(jsonPath("$.paths['/api/quotes/{id}/versions/{versionId}/review'].post['x-required-permission']").doesNotExist())
   .andReturn().getResponse().getContentAsString();
  java.nio.file.Files.writeString(java.nio.file.Path.of("target/openapi.json"),spec);
 }

 @Autowired QuotePermissionMigration quoteMigration;
 @Test void quoteCrudMigrationDoesNotExpandPartialGrants() throws Exception {
  Role complete=role();complete.setQuotePermissionVersion(0);
  complete.getPermissionCodes().addAll(QuotePermissionCompatibility.OLD_UPDATE);
  Role partial=role("quotes.update","quotes.send","quotes.read");partial.setQuotePermissionVersion(0);
  roles.saveAndFlush(complete);roles.saveAndFlush(partial);
  String partialToken=token(user(partial));
  mvc.perform(get("/api/auth/me").header("Authorization",partialToken)).andExpect(status().isOk())
   .andExpect(jsonPath("$.permissionCodes[?(@ == 'quotes.update')]").isEmpty());
  quoteMigration.migrate();
  assertEquals(Set.of("quotes.update"),complete.getPermissionCodes());
  assertEquals(1,complete.getQuotePermissionVersion());
  assertEquals(0,partial.getQuotePermissionVersion());
  long revision=complete.getRevision();quoteMigration.migrate();assertEquals(revision,complete.getRevision());
  mvc.perform(get("/api/roles/"+partial.getId()).header("Authorization",adminToken)).andExpect(status().isOk())
   .andExpect(jsonPath("$.quotePermissionMigrationRequired").value(true))
   .andExpect(jsonPath("$.legacyQuotePermissionCodes.length()").value(2));
  Role delegated=role("roles.update","quotes.update","quotes.read");
  update(token(user(delegated)),partial.getId(),1,"[\"quotes.update\"]").andExpect(status().isForbidden())
   .andExpect(jsonPath("$.code").value("QUOTE_PERMISSION_CONFIRMATION_REQUIRED"));
  update(adminToken,partial.getId(),1,"[\"quotes.update\",\"quotes.read\"]").andExpect(status().isOk())
   .andExpect(jsonPath("$.quotePermissionMigrationRequired").value(false))
   .andExpect(jsonPath("$.legacyQuotePermissionCodes").isEmpty());
  mvc.perform(get("/api/auth/me").header("Authorization",partialToken)).andExpect(status().isOk())
   .andExpect(jsonPath("$.permissionCodes[?(@ == 'quotes.update')]").isNotEmpty());
 }
 @Test void quoteActionRoutesUseOnlyUpdatePermission() throws Exception {
  String updateOnly=token(user(role("quotes.update")));
  String readOnly=token(user(role("quotes.read")));
  for(String action:List.of("new-version","request-approval","send","decision","convert-to-order")) {
   String body=switch(action) {
    case "request-approval" -> "{\"expectedRevision\":1,\"reviewerId\":\"missing\"}";
    case "review" -> "{\"expectedRevision\":1,\"decision\":\"approved\"}";
    case "decision" -> "{\"expectedRevision\":1,\"decision\":\"accepted\"}";
    default -> "{\"expectedRevision\":1}";
   };
   String path="/api/quotes/missing/versions/missing/"+action;
   mvc.perform(post(path).header("Authorization",updateOnly).contentType(MediaType.APPLICATION_JSON).content(body))
    .andExpect(status().isNotFound());
   mvc.perform(post(path).header("Authorization",readOnly).contentType(MediaType.APPLICATION_JSON).content(body))
    .andExpect(status().isForbidden());
  }
  mvc.perform(get("/api/permissions").header("Authorization",adminToken)).andExpect(status().isOk())
   .andExpect(jsonPath("$[?(@.entity == 'quotes')]", org.hamcrest.Matchers.hasSize(4)));
  create(adminToken,"OLD_"+UUID.randomUUID().toString().replace("-",""),"[\"quotes.send\"]")
   .andExpect(status().isBadRequest());
 }
}
