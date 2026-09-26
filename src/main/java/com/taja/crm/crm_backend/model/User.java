package com.taja.crm.crm_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @NotBlank @Column(nullable = false, unique = true)
    private String username;
    @NotBlank @Email @Column(nullable = false, unique = true)
    private String email;
    @JsonIgnore @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;
    /** 最近三次舊密碼的 BCrypt hash，由舊至新。 */
    @JsonIgnore
    @ElementCollection
    @CollectionTable(name = "user_password_history", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "history_order")
    @Column(name = "password_hash", nullable = false)
    private List<String> passwordHistory = new ArrayList<>();
}
