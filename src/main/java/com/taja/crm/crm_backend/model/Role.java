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
}
