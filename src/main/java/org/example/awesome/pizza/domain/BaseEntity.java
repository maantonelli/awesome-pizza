package org.example.awesome.pizza.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.io.Serializable;
import java.time.Instant;

@Data
@Accessors(chain = true)
@MappedSuperclass
public class BaseEntity implements Serializable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Instant createdDate;

  @Column(nullable = false)
  private Instant lastModifiedDate;

  @PrePersist
  void preInsert() {
    this.createdDate = Instant.now();
    this.lastModifiedDate = Instant.now();
  }

  @PreUpdate
  void preUpdate() {
    this.lastModifiedDate = Instant.now();
  }
}
