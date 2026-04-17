package edu.esi.dls.esiusuarios.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

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

    public List<Token> getConfirmationTokens() {
        return confirmationTokens;
    }

    public void setConfirmationTokens(List<Token> confirmationTokens) {
        this.confirmationTokens = confirmationTokens;
    }

    public void addConfirmationToken(Token confirmationToken) {
        this.confirmationTokens.add(confirmationToken);
        confirmationToken.setUser(this);
    }
}