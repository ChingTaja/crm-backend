package com.taja.crm.crm_backend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leads")
@Getter
@Setter
@NoArgsConstructor
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;
    private String company;
    private String email;
    private String phone;
    private String source;

    // 目前儲存負責人姓名。
    private String owner;

    @Pattern(regexp = "待聯繫|聯繫中|已合格|不合格")
    private String status = LeadStatus.PENDING.getValue();

    @Valid
    @Embedded
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LeadQualification qualification;
}
