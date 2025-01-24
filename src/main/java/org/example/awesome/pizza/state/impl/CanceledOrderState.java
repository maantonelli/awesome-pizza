package org.example.awesome.pizza.state.impl;

import org.example.awesome.pizza.domain.Order;
import org.example.awesome.pizza.exception.BadRequestException;
import org.example.awesome.pizza.exception.ForbiddenException;
import org.example.awesome.pizza.mapper.OrderMapper;
import org.example.awesome.pizza.model.OrderStatus;
import org.example.awesome.pizza.repository.OrderRepository;
import org.example.awesome.pizza.state.model.OrderStateModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CanceledOrderState extends BaseOrderState {
  public CanceledOrderState(OrderMapper mapper, OrderRepository repository) {
    super(mapper, repository);
  }

  @Override
  public List<OrderStatus> getAllowed() {
    return List.of(OrderStatus.CREATED);
  }

  @Override
  public OrderStatus getStatus() {
    return OrderStatus.CANCELED;
  }

  @Override
  public boolean validate(OrderStateModel stateModel) {
    if (stateModel.current() == null)
      throw new BadRequestException("No current Order found");

    if (stateModel.request().getChefId() != null)
      throw new ForbiddenException("Chef can not cancel an Order");

    return true;
  }

  @Override
  public Order handleState(OrderStateModel stateModel) {
    return stateModel.current();
  }
}
