package com.finpath.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {
  @Id @Column(length=36)
  private String id = UUID.randomUUID().toString();
  @Column(nullable=false, length=190) private String email;
  @Column(name="password_hash", nullable=false) private String passwordHash;
  @Column(name="full_name", nullable=false, length=120) private String fullName;
  @Column(name="created_at", nullable=false) private Instant createdAt = Instant.now();
  public String getId(){return id;} public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;} public String getFullName(){return fullName;} public Instant getCreatedAt(){return createdAt;}
  public void setEmail(String v){email=v;} public void setPasswordHash(String v){passwordHash=v;} public void setFullName(String v){fullName=v;}
}
