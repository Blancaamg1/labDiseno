package edu.esi.dls.esiusuarios.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(nullable = false, unique = true, length = 140)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 120)
    private String token;

    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'USER'")
    @Enumerated(EnumType.STRING) 
    private Role role;
    public enum Role {
        USER, ADMIN
    }

    private Long validationDate;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Token> confirmationTokens = new ArrayList<>();

    public User() {
    }

    public User(String name, String password, String token) {
        this.name = name;
        this.email = null;
        this.password = password;
        this.token = token;
        this.validationDate = System.currentTimeMillis();
    }

    public User(String name, String email, String password, String token) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.token = token;
        this.validationDate = null;
    }

    public User(String name, String email, String password, String token, Long validationDate) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.token = token;
        this.validationDate = validationDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getToken() {
        return token;
    }   

    public void setToken(String token) {
        this.token = token;
    }   

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getValidationDate() {
        return validationDate;
    }

    public void setValidationDate(Long validationDate) {
        this.validationDate = validationDate;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<Token> getConfirmationTokens() {
        return confirmationTokens;
    }

    // Métodos de UserDetails para Spring Security
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        }
        return authorities;
    }

    @Override
    public String getUsername() {
        return name;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public void setConfirmationTokens(List<Token> confirmationTokens) {
        this.confirmationTokens = confirmationTokens;
    }

    public void addConfirmationToken(Token confirmationToken) {
        this.confirmationTokens.add(confirmationToken);
        confirmationToken.setUser(this);
    }
}