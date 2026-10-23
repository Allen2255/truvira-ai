package com.truvira.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * A person who can log in to Truvira (an investigator / reviewer).
 * Table "app_user" because "user" is a reserved word in many databases.
 *
 * We NEVER store the real password. We store a BCRYPT HASH of it:
 *   "mypassword"  ->  "$2a$10$Xk2...."  (60 characters)
 * At login, Spring hashes what was typed and compares the hashes. Even if someone stole this
 * table, they could not read the passwords back. (Like SHA-256 for files, but deliberately SLOW
 * and with a random "salt", so guessing millions of passwords takes far too long.)
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)   // unique = no two users with the same name
    private String username;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 30)
    private String role;               // e.g. "INVESTIGATOR"; Spring turns it into "ROLE_INVESTIGATOR"

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public AppUser() {
    }

    public AppUser(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; } // always a HASH, never a plain password
    public String getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
