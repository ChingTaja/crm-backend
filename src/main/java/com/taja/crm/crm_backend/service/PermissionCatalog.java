package com.taja.crm.crm_backend.service;
import com.taja.crm.crm_backend.dto.role.PermissionResponse;
import java.util.*;
public final class PermissionCatalog {
 private PermissionCatalog() {}
 public static final Set<String> SYSTEM_ROLES=Set.of("ADMIN","MANAGER","USER");
 public static final List<PermissionResponse> ALL;
 public static final Set<String> CODES;
 static {
  List<PermissionResponse> result=new ArrayList<>();
  Map<String,String> groups=new LinkedHashMap<>();
  groups.put("customers","客戶");groups.put("contacts","聯絡人");groups.put("opportunities","商機");
  groups.put("products","產品");groups.put("leads","潛在客戶");groups.put("quotes","報價");
  groups.put("orders","訂單");groups.put("users","帳號");groups.put("roles","角色");groups.put("permissions","權限清單");
  groups.forEach((entity,label)->{
   List<String> actions=switch(entity) {
    case "orders" -> List.of("read","process","complete","cancel");
    case "quotes" -> List.of("read","create","update","delete","new-version","request-approval","approve","send","convert","record-decision");
    case "leads" -> List.of("read","create","update","delete","qualify");
    case "users" -> List.of("read","create","update","delete","assign-role");
    case "permissions" -> List.of("read");
    default -> List.of("read","create","update","delete");
   };
   Map<String,String> names=Map.ofEntries(Map.entry("read","查看"),Map.entry("create","新增"),Map.entry("update","修改"),
    Map.entry("delete","刪除"),Map.entry("qualify","資格審核"),Map.entry("new-version","建立新版本"),
    Map.entry("request-approval","申請審批"),Map.entry("approve","審批"),Map.entry("send","送出"),Map.entry("convert","轉訂單"),
    Map.entry("record-decision","代錄客戶回覆"),Map.entry("process","開始處理"),Map.entry("complete","確認完成"),
    Map.entry("cancel","取消"),Map.entry("assign-role","指派角色"));
   actions.forEach(action->result.add(new PermissionResponse(entity+"."+action,names.get(action)+label,entity,label,null)));
  });
  ALL=List.copyOf(result);CODES=Collections.unmodifiableSet(new TreeSet<>(ALL.stream().map(PermissionResponse::code).toList()));
 }
}
