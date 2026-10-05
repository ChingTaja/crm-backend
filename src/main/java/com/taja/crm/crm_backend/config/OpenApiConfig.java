package com.taja.crm.crm_backend.config;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.security.*;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.*;
import java.util.List;
@Configuration
public class OpenApiConfig {
 @Bean public OpenAPI crmOpenApi() {
  return new OpenAPI().components(new Components().addSecuritySchemes("bearerAuth",
   new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
   .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
 }
 @Bean public OpenApiCustomizer publicAuthEndpoints() {
  return api -> {
   api.getPaths().forEach((path, item) -> {
    if(path.startsWith("/api/quotes")) item.readOperationsMap().forEach((method, operation) -> {
     String action = switch(method) {
      case GET, HEAD -> "read";
      case DELETE -> "delete";
      case POST -> path.equals("/api/quotes") ? "create" : "update";
      default -> "update";
     };
     if(path.endsWith("/reviewer-options")) action="update";
     if(path.endsWith("/review")) {
      operation.addExtension("x-authorization", "assigned-reviewer");
      operation.setDescription("JWT 登入；由業務層驗證指定審核人、版本、狀態與 revision，不要求報價 CRUD 權限。");
      return;
     }
     operation.addExtension("x-required-permission", "quotes." + action);
     operation.setDescription("功能權限：quotes." + action + "。仍驗證資料範圍、狀態、revision、自我審批限制與其他業務規則。");
    });
   });
   for(String action:List.of("login","register","forgot-password","reset-password")) {
    var path=api.getPaths().get("/api/auth/"+action);
    if(path!=null&&path.getPost()!=null) path.getPost().setSecurity(List.of());
   }
  };
 }
}
