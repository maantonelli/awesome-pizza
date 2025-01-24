package org.example.awesome.pizza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
@Entity
@Table(name = "users")
@DiscriminatorColumn(name = "user_type")
public class User extends BaseEntity {
  @Column(nullable = false, updatable = false, unique = true)
  private String username;
  @Column(nullable = false)
  private String firstName;
  @Column(nullable = false)
  private String lastName;
}
