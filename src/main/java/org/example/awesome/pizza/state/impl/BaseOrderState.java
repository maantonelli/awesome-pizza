package org.example.awesome.pizza.state.impl;

import lombok.RequiredArgsConstructor;
import org.example.awesome.pizza.domain.Order;
import org.example.awesome.pizza.mapper.OrderMapper;
import org.example.awesome.pizza.model.OrderStatus;
import org.example.awesome.pizza.repository.OrderRepository;
import org.example.awesome.pizza.state.model.OrderStateModel;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public abstract class BaseOrderState {
  protected final OrderMapper mapper;
  protected final OrderRepository repository;

  /**
   * If current Order is present, do nothing, otherwise find current Order by ID
   * @param stateModel Main model for Order state pattern handling
   */
  public void retrieveCurrent(final OrderStateModel stateModel) {
    Optional.of(stateModel)
        .filter(model -> model.current() == null)
        .map(OrderStateModel::id)
        .flatMap(repository::findById)
        .ifPresent(stateModel::current);
  }

  public abstract List<OrderStatus> getAllowed();
  public abstract OrderStatus getStatus();

  public abstract boolean validate(final OrderStateModel stateModel);

  public abstract Order handleState(final OrderStateModel stateModel);
}
