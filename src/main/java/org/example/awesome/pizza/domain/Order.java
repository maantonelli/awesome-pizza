package org.example.awesome.pizza.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.example.awesome.pizza.domain.utils.FromSequence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
@Entity
@Table(name = "orders")
public class Order extends BaseEntity implements Serializable {
  @Column(nullable = false, unique = true, updatable = false)
  @FromSequence
  private Long code;
  @Column(nullable = false)
  @ManyToMany(targetEntity = Pizza.class, cascade = {CascadeType.REFRESH})
  private List<Pizza> pizzas = new ArrayList<>();
  @Column(nullable = false)
  private String status;
  @JoinColumn(name = "chef_id", referencedColumnName = "id")
  @ManyToOne(targetEntity = Chef.class, fetch = FetchType.LAZY)
  private Chef chef;
  @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false, updatable = false)
  @ManyToOne(targetEntity = Customer.class, cascade = {CascadeType.PERSIST}, fetch = FetchType.LAZY, optional = false)
  private Customer customer;
}
