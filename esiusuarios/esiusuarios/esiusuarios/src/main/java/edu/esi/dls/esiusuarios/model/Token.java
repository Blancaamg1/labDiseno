package edu.esi.dls.esiusuarios.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "confirmation_tokens")
public class Token {

    public static final String PURPOSE_EMAIL_CONFIRMATION = "EMAIL_CONFIRMATION";
    public static final String PURPOSE_PASSWORD_RESET = "PASSWORD_RESET";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_value", nullable = false, unique = true, length = 120)
    private String value;

    @Column(nullable = false, length = 40)
    private String purpose;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Token() {
    }

    public Token(String value, LocalDateTime createdAt, LocalDateTime expiresAt, User user) {
        this.value = value;
        this.purpose = PURPOSE_EMAIL_CONFIRMATION;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    public Token(String value, String purpose, LocalDateTime createdAt, LocalDateTime expiresAt, User user) {
        this.value = value;
        this.purpose = purpose;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}
