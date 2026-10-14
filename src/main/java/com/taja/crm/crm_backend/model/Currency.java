package com.taja.crm.crm_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "currencies")
@Getter @Setter
public class Currency {
    @Id @Column(length = 3, updatable = false)
    private String code;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String symbol;
    @Column(nullable = false, updatable = false) private int fractionDigits = 2;
    @Column(nullable = false) private boolean active = true;
}
