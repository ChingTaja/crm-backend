package com.taja.crm.crm_backend.service;
import com.taja.crm.crm_backend.model.*;
import com.taja.crm.crm_backend.repo.*;
import java.time.Clock;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class QuotePermissionMigration {
 private final RoleRepository roles;
 private final SecurityAuditRepository audits;
 private final Clock clock;
 @Transactional public void migrate() {
  roles.lockAdministration().orElseThrow();
  for(Role role:roles.findAll()) {
   if(role.getQuotePermissionVersion()>=1 || QuotePermissionCompatibility.pending(role)) continue;
   String before=new TreeSet<>(role.getPermissionCodes()).toString();
   boolean complete=role.getPermissionCodes().containsAll(QuotePermissionCompatibility.OLD_UPDATE);
   role.getPermissionCodes().removeAll(QuotePermissionCompatibility.OLD_UPDATE);
   if(complete) role.getPermissionCodes().add("quotes.update");
   role.setQuotePermissionVersion(1);role.setRevision(role.getRevision()+1);
   SecurityAudit entry=new SecurityAudit();entry.setAt(clock.instant());entry.setActorName("系統遷移");
   entry.setAction("QuotePermissionsMigrated");entry.setTargetId(role.getId());entry.setBeforeValue(before);
   entry.setAfterValue(new TreeSet<>(role.getPermissionCodes()).toString());audits.save(entry);
  }
  roles.flush();
 }
}
