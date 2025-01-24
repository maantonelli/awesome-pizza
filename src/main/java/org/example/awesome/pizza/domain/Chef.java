package org.example.awesome.pizza.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
@Entity
public class Chef extends BaseEntity {
  @Column(nullable = false)
  private String firstName;
  @Column(nullable = false)
  private String lastName;
}
