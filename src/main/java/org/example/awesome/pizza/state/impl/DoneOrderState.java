package org.example.awesome.pizza.state.impl;

import org.example.awesome.pizza.domain.Order;
import org.example.awesome.pizza.exception.BadRequestException;
import org.example.awesome.pizza.exception.InternalServerErrorException;
import org.example.awesome.pizza.mapper.OrderMapper;
import org.example.awesome.pizza.model.OrderStatus;
import org.example.awesome.pizza.repository.OrderRepository;
import org.example.awesome.pizza.state.model.OrderStateModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DoneOrderState extends BaseOrderState {
  public DoneOrderState(OrderMapper mapper, OrderRepository repository) {
    super(mapper, repository);
  }

  @Override
  public List<OrderStatus> getAllowed() {
    return List.of(OrderStatus.COOKING);
  }

  @Override
  public OrderStatus getStatus() {
    return OrderStatus.DONE;
  }

  @Override
  public void retrieveCurrent(OrderStateModel stateModel) {
    // Input request must have a Chef
    if (stateModel.request().getChefId() == null)
      throw new BadRequestException("Invalid Chef ID");

    // Find by input ID when passed; result must always belong to input Chef
    if (stateModel.id() != null)
      repository.findById(stateModel.id())
          .ifPresent(stateModel::current);
    else
      // Only Chefs can complete an Order and only COOKING Orders can be completed.
      // However, is possible that no COOKING Order exists for input Chef
      repository.findCookingOrder(stateModel.request().getChefId())
        .ifPresent(stateModel::current);
  }

  @Override
  public boolean validate(OrderStateModel stateModel) {
    // If no COOKING Orders found on DB, should not continue with no exceptions
    if (stateModel.current() == null)
      return false;

    // DB Order must belong to a Chef
    if (stateModel.current().getChef() == null || stateModel.current().getChef().getId() == null)
      throw new InternalServerErrorException("Current order has not been taken by any Chef.");

    // Input request must have a Chef
    if (stateModel.request().getChefId() == null)
      throw new BadRequestException("Invalid Chef ID");

    // The Chef that sent the request be the same that owns DB Order
    if (!stateModel.request().getChefId().equals(stateModel.current().getChef().getId()))
      throw new BadRequestException("Chef not allowed to complete Order");

    return true;
  }

  @Override
  public Order handleState(OrderStateModel stateModel) {
    return stateModel.current();
  }
}
