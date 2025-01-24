package org.example.awesome.pizza.state.impl;

import org.example.awesome.pizza.domain.Order;
import org.example.awesome.pizza.exception.BadRequestException;
import org.example.awesome.pizza.exception.ConflictException;
import org.example.awesome.pizza.mapper.OrderMapper;
import org.example.awesome.pizza.model.OrderStatus;
import org.example.awesome.pizza.repository.OrderRepository;
import org.example.awesome.pizza.state.model.OrderStateModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CookingOrderState extends BaseOrderState {
  private final Long cookingThreshold;

  public CookingOrderState(
      final OrderMapper mapper,
      final OrderRepository repository,
      @Value("${awesome-pizza.config.cooking-threshold:1}") final Long cookingThreshold
  ) {
    super(mapper, repository);
    this.cookingThreshold = cookingThreshold;
  }

  @Override
  public List<OrderStatus> getAllowed() {
    return List.of(OrderStatus.CREATED);
  }

  @Override
  public OrderStatus getStatus() {
    return OrderStatus.COOKING;
  }

  @Override
  public void retrieveCurrent(OrderStateModel stateModel) {
    final Optional<Order> optCurrent;

    // If a specific Order is requested, find that specific CREATED Order
    if (stateModel.id() != null)
      optCurrent = repository.findById(stateModel.id());
    else // Otherwise, find nearly next CREATED Order
      optCurrent = repository.findAllSortedByCreatedDate(OrderStatus.CREATED.name()).stream()
          .findFirst();

    optCurrent.ifPresent(stateModel::current);
  }

  @Override
  public boolean validate(OrderStateModel stateModel) {
    // If no CREATED Order found, should not continue with no exceptions
    if (stateModel.current() == null)
      return false;

    // Input request must have a Chef
    if (stateModel.request().getChefId() == null)
      throw new BadRequestException("Invalid Chef ID");

    // Must not exceed maximum Order that a Chef can manage
    if (!repository.canTakeAnyOrder(stateModel.request().getChefId(), this.cookingThreshold))
      throw new ConflictException("Maximum COOKING order reached [%d]".formatted(stateModel.request().getChefId()));

    return true;
  }

  @Override
  public Order handleState(OrderStateModel stateModel) {

    mapper.patch(stateModel.request(), stateModel.current());

    return stateModel.current();
  }
}
