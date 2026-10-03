package com.taja.crm.crm_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @NotBlank @Column(nullable = false, unique = true)
    private String code;
    @NotBlank @Column(nullable = false)
    private String name;
    @Column(length=2000) private String description;
    @Column(nullable=false, columnDefinition="bigint default 1") private long revision = 1;
    @ElementCollection
    @CollectionTable(name="role_permissions", joinColumns=@JoinColumn(name="role_id"),
        uniqueConstraints=@UniqueConstraint(columnNames={"role_id","permission_code"}))
    @Column(name="permission_code", nullable=false)
    private java.util.Set<String> permissionCodes = new java.util.HashSet<>();

}
