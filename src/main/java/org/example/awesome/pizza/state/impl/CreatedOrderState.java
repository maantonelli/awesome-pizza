package org.example.awesome.pizza.state.impl;

import org.example.awesome.pizza.domain.Order;
import org.example.awesome.pizza.exception.BadRequestException;
import org.example.awesome.pizza.exception.ForbiddenException;
import org.example.awesome.pizza.mapper.OrderMapper;
import org.example.awesome.pizza.model.OrderStatus;
import org.example.awesome.pizza.repository.OrderRepository;
import org.example.awesome.pizza.state.model.OrderStateModel;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class CreatedOrderState extends BaseOrderState {
  CreatedOrderState(OrderMapper mapper, OrderRepository repository) {
    super(mapper, repository);
  }

  @Override
  public List<OrderStatus> getAllowed() {
    return Arrays.asList(OrderStatus.COOKING, OrderStatus.CREATED);
  }

  @Override
  public OrderStatus getStatus() {
    return OrderStatus.CREATED;
  }

  @Override
  public boolean validate(OrderStateModel stateModel) {
    // When creating new Order, must have Pizzas in request
    if (CollectionUtils.isEmpty(stateModel.request().getPizzas()) && stateModel.current() == null)
      throw new BadRequestException("No valid request");

    // Only Chef that owns the Order can restore its status COOKING->CREATED
    if (stateModel.current() != null &&
        OrderStatus.COOKING.name().equals(stateModel.current().getStatus()) &&
        !Objects.equals(stateModel.current().getChef().getId(), stateModel.request().getChefId()))
      throw new BadRequestException("Not allowed to restore CREATED Order status");

    // Chef can not modify Pizzas in the Order
    if (stateModel.current() != null &&
        OrderStatus.CREATED.name().equals(stateModel.current().getStatus()) &&
        OrderStatus.CREATED.equals(stateModel.request().getStatus()) &&
        stateModel.request().getChefId() != null)
      throw new ForbiddenException("Chef is not allowed to modify order");

    return true;
  }

  @Override
  public Order handleState(OrderStateModel stateModel) {
    stateModel.request().setChefId(null);

    // If no DB entity, must create new
    if (stateModel.current() == null)
      stateModel.current(mapper.toEntity(stateModel.request()));
    else { // Otherwise, must update
      stateModel.current().setChef(null);
      mapper.patch(stateModel.request(), stateModel.current());

      // If existing DB entity is CREATED, can modify Pizzas too
      if (OrderStatus.CREATED.name().equals(stateModel.current().getStatus()) && !CollectionUtils.isEmpty(stateModel.request().getPizzas()))
        stateModel.current().setPizzas(
            stateModel.request()
                .getPizzas().stream()
                .map(mapper::toPizza)
                .collect(Collectors.toList())
        );
    }

    return stateModel.current();
  }
}
