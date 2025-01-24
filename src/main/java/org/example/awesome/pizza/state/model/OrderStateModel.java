package org.example.awesome.pizza.state.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.example.awesome.pizza.domain.Order;
import org.example.awesome.pizza.model.OrderInternalReq;

@Getter
@Setter
@Accessors(chain = true, fluent = true)
public class OrderStateModel {
  private Long id;
  private Order current;
  private OrderInternalReq request;
}
