package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.dto.metadata.FieldMetadata;
import com.taja.crm.crm_backend.model.LeadStatus;
import com.taja.crm.crm_backend.model.QualificationDecision;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EntityMetadataService {

    public List<FieldMetadata> findFieldsByEntityName(String entityName) {
        return switch (entityName) {
            case "roles" -> List.of(
                    new FieldMetadata("id", "角色 ID", "string", "id", true, null, null, null),
                    new FieldMetadata("code", "角色代碼", "string", "code", true, null, null, null),
                    field("name", "角色名稱", "string", "name"),
                    field("description", "角色說明", "text", "description"),
                    new FieldMetadata("revision", "版本", "number", "revision", true, null, null, null));
            case "orders" -> List.of(
                    new FieldMetadata("number", "訂單編號", "text", "number", true, null, null, null),
                    new FieldMetadata("name", "訂單名稱", "text", "name", true, null, null, null),
                    new FieldMetadata("customer_id", "客戶", "lookup", "customerId", true, null, "customers", "id"),
                    new FieldMetadata("customer_name", "客戶名稱", "text", "customerName", true, null, null, null),
                    new FieldMetadata("quote_number", "報價編號", "text", "quoteNumber", true, null, null, null),
                    new FieldMetadata("total_cents", "總額（分）", "number", "totalCents", true, null, null, null),
                    new FieldMetadata("created_at", "建立時間", "dateTime", "createdAt", true, null, null, null),
                    new FieldMetadata("status", "狀態", "option", "status", true,
                        List.of(new com.taja.crm.crm_backend.model.Option("Confirmed", "已確認"),
                                new com.taja.crm.crm_backend.model.Option("Processing", "處理中"),
                                new com.taja.crm.crm_backend.model.Option("Completed", "已完成"),
                                new com.taja.crm.crm_backend.model.Option("Cancelled", "已取消")), null, null));
            case "quotes" -> List.of(
                    field("name", "報價名稱", "text", "name"),
                    lookup("customer_id", "客戶", "customerId", "customers"),
                    lookup("opportunity_id", "商機", "opportunityId", "opportunities"),
                    field("valid_until", "有效期限", "date", "validUntil"),
                    field("payment_terms", "付款條件", "text", "paymentTerms"),
                    field("delivery_terms", "交付條件", "text", "deliveryTerms"),
                    field("warranty", "保固條款", "text", "warranty"),
                    field("notes", "備註", "text", "notes"));
            case "products" -> List.of(
                    field("name", "名稱", "text", "name"),
                    field("sku", "產品編號", "text", "sku"),
                    field("price", "單價（TWD）", "number", "price"),
                    new FieldMetadata("status", "狀態", "option", "status", false,
                            List.of(new com.taja.crm.crm_backend.model.Option("啟用", "啟用"),
                                    new com.taja.crm.crm_backend.model.Option("停用", "停用")), null, null));
            case "users" -> List.of(
                    field("username", "帳號", "string", "username"),
                    field("email", "電子郵件", "email", "email"),
                    lookup("role_id", "角色", "roleId", "roles"));
            case "leads" -> leadFields();
            case "opportunities" -> List.of(
                    new FieldMetadata("id", "商機 ID", "string", "id", true, null, null, null),
                    field("name", "商機名稱", "string", "name"),
                    lookup("customer_id", "所屬客戶", "customerId", "customers"),
                    lookup("lead_id", "來源 Lead", "leadId", "leads"),
                    field("amount", "預估金額（TWD）", "number", "amount"),
                    field("expected_close_date", "預計成交日", "date", "expectedCloseDate"),
                    field("owner", "負責人", "string", "owner"),
                    new FieldMetadata("stage", "商機階段", "optionSet", "stage", false,
                            java.util.stream.Stream.of("需求確認", "提案報價", "協商中", "已成交", "已失單")
                                    .map(value -> new com.taja.crm.crm_backend.model.Option(value, value)).toList(),
                            null, null));
            case "customers" -> List.copyOf(commonFields("客戶名稱"));
            case "contacts" -> {
                List<FieldMetadata> fields = commonFields("聯絡人姓名");
                fields.add(lookup("customer_id", "所屬客戶", "customerId", "customers"));
                yield List.copyOf(fields);
            }
            default -> throw new EntityNotFoundException("找不到 entity：" + entityName);
        };
    }

    private List<FieldMetadata> commonFields(String nameLabel) {
        return new ArrayList<>(List.of(
                new FieldMetadata("id", "唯一識別碼", "string", "id", true, null, null, null),
                field("name", nameLabel, "string", "name"),
                field("company", "公司名稱", "string", "company"),
                field("email", "電子郵件", "email", "email"),
                field("phone", "電話", "phone", "phone"),
                field("owner", "負責人", "string", "owner")));
    }

    private List<FieldMetadata> leadFields() {
        List<FieldMetadata> fields = commonFields("潛在客戶名稱");
        fields.add(field("source", "來源", "string", "source"));
        fields.add(new FieldMetadata("status", "狀態", "optionSet", "status", false,
                LeadStatus.options(), null, null));
        fields.add(new FieldMetadata("qualification_decision", "審核結果", "optionSet",
                "qualification.decision", false, QualificationDecision.options(), null, null));
        fields.add(field("qualification_reviewed_at", "審核時間", "dateTime", "qualification.reviewedAt"));
        fields.add(field("qualification_reason", "不符合資格原因", "text", "qualification.reason"));
        fields.add(field("qualification_note", "審核備註", "text", "qualification.note"));
        fields.add(lookup("qualification_customer_id", "轉換後客戶", "qualification.customerId", "customers"));
        fields.add(lookup("qualification_contact_id", "轉換後聯絡人", "qualification.contactId", "contacts"));
        fields.add(lookup("qualification_opportunity_id", "轉換後商機", "qualification.opportunityId", "opportunities"));
        return List.copyOf(fields);
    }

    private FieldMetadata field(String name, String displayName, String type, String apiFieldName) {
        return new FieldMetadata(name, displayName, type, apiFieldName, false, null, null, null);
    }

    private FieldMetadata lookup(String name, String displayName, String apiFieldName, String relatedEntity) {
        return new FieldMetadata(name, displayName, "lookup", apiFieldName, false, null, relatedEntity, "id");
    }
}
