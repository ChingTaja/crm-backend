package com.taja.crm.crm_backend.config;
import com.taja.crm.crm_backend.service.*;
import jakarta.servlet.http.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.method.HandlerMethod;
@Configuration @RequiredArgsConstructor
public class PermissionWebConfig implements WebMvcConfigurer {
 private final PermissionService access;
 @Override public void addInterceptors(InterceptorRegistry registry) {
  registry.addInterceptor(new HandlerInterceptor() {
   @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) {
    if(!(handler instanceof HandlerMethod)) return true;
    // Use Spring's matched pattern, not the raw URL, to cover aliases and encoded path segments consistently.
    String pattern=(String)request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    if(pattern==null||!pattern.startsWith("/api/")||pattern.startsWith("/api/auth/")) return true;
    String actor=request.getUserPrincipal()==null?null:request.getUserPrincipal().getName();
    @SuppressWarnings("unchecked") Map<String,String> variables=(Map<String,String>)request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    if(pattern.equals("/api/quote-reviews") || pattern.equals("/api/quote-reviews/{quoteId}")
       || pattern.equals("/api/quotes/{id}/versions/{versionId}/review")) {
      access.actor(actor); return true; // Assignment and state are enforced by the business layer.
    }
    if(pattern.equals("/api/roles/options")) {access.actor(actor);return true;} // service filters assignable roles
    if(pattern.equals("/api/orders/{id}/status")) {access.actor(actor);return true;} // permission depends on validated target status
    String entity=pattern.split("/")[2];
    if("entities".equals(entity)) entity=variables.get("entityName");
    else if("{entity}".equals(entity)) entity=variables.get("entity");
    String action=switch(request.getMethod()) {
     case "GET","HEAD" -> "read";
     case "POST" -> pattern.endsWith("/search")?"read":"create";
     case "PUT","PATCH" -> "update";
     case "DELETE" -> "delete";
     default -> "unsupported";
    };
    if(pattern.equals("/api/opportunities/{id}/close")) action="update";
    if(pattern.endsWith("/qualification")) action="qualify";
    if(pattern.endsWith("/reviewer-options")) action="update";
    if("quotes".equals(entity)) {
     String suffix=pattern.substring(pattern.lastIndexOf('/')+1);
     action=switch(suffix) {
      case "new-version","request-approval","send","review","decision","convert-to-order" -> "update";
      default -> action;
     };
    }
    final String module = entity;
    if(PermissionCatalog.CODES.stream().noneMatch(code -> code.startsWith(module+".")))
      throw new QuoteException(org.springframework.http.HttpStatus.NOT_FOUND,"ENTITY_NOT_FOUND","找不到 entity。");
    access.require(actor,entity+"."+action);
    return true;
   }
  }).addPathPatterns("/api/**");
 }
}
