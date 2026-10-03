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
   for(String action:List.of("login","register","forgot-password","reset-password")) {
    var path=api.getPaths().get("/api/auth/"+action);
    if(path!=null&&path.getPost()!=null) path.getPost().setSecurity(List.of());
   }
  };
 }
}
