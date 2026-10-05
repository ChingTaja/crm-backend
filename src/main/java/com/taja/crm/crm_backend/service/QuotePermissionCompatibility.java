package com.taja.crm.crm_backend.service;
import com.taja.crm.crm_backend.model.Role;
import java.util.*;
public final class QuotePermissionCompatibility {
 private QuotePermissionCompatibility() {}
 public static final Set<String> OLD_UPDATE = Set.of("quotes.update","quotes.new-version","quotes.request-approval",
  "quotes.approve","quotes.send","quotes.record-decision","quotes.convert");
 public static Set<String> legacy(Role role) {
  if(role.getQuotePermissionVersion() >= 1 || PermissionCatalog.SYSTEM_ROLES.contains(role.getCode())) return Set.of();
  Set<String> result=new TreeSet<>(role.getPermissionCodes());result.retainAll(OLD_UPDATE);return result;
 }
 public static boolean pending(Role role) {
  Set<String> old=legacy(role);return !old.isEmpty()&&!old.containsAll(OLD_UPDATE);
 }
 public static Set<String> effective(Role role) {
  Set<String> result=new TreeSet<>(role.getPermissionCodes());
  if(role.getQuotePermissionVersion()<1) {
   boolean complete=result.containsAll(OLD_UPDATE);
   result.removeAll(OLD_UPDATE);
   if(complete) result.add("quotes.update");
  }
  result.retainAll(PermissionCatalog.CODES);return result;
 }
}
