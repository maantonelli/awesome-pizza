package org.example.awesome.pizza.state;

import org.apache.commons.lang3.ObjectUtils;
import org.example.awesome.pizza.domain.Order;
import org.example.awesome.pizza.exception.BadRequestException;
import org.example.awesome.pizza.exception.InternalServerErrorException;
import org.example.awesome.pizza.mapper.OrderMapper;
import org.example.awesome.pizza.model.OrderDto;
import org.example.awesome.pizza.model.OrderStatus;
import org.example.awesome.pizza.repository.OrderRepository;
import org.example.awesome.pizza.state.impl.BaseOrderState;
import org.example.awesome.pizza.state.model.OrderStateModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OrderStateContext implements OrderState {
  private final OrderRepository repository;
  private final OrderMapper mapper;
  private final Map<OrderStatus, BaseOrderState> instances;

  public OrderStateContext(final OrderRepository repository, final OrderMapper mapper, final List<BaseOrderState> instances) {
    this.repository = repository;
    this.mapper = mapper;
    this.instances = instances.stream()
        .collect(Collectors.toMap(BaseOrderState::getStatus, Function.identity()));
  }

  public Optional<OrderDto> handleState(final OrderStatus targetStatus, final OrderStateModel stateModel) {
    // Check input consistency
    if (ObjectUtils.anyNull(targetStatus, stateModel))
      throw new BadRequestException("Invalid input for handling state");

    // Request must exists
    if (stateModel.request() == null)
      throw new BadRequestException("Invalid request for handling state");

    // Retrieve handler based on target state
    final BaseOrderState instance = instances.get(targetStatus);
    if (instance == null)
      throw new InternalServerErrorException("No instance state found for [%s]".formatted(targetStatus.name()));

    // Retrieve current DB Order entity
    instance.retrieveCurrent(stateModel);

    // Preliminary checks:
    //   1. if true, can continue and handle target state
    //   2. if false, should not continue with no exceptions
    //   3. if error, must raise exception
    if (!instance.validate(stateModel))
      return Optional.empty();

    // Decode current DB Order status
    final OrderStatus currentStatus = Optional.ofNullable(stateModel.current())
        .map(Order::getStatus)
        .map(OrderStatus::valueOf)
        .orElse(null);

    // Check if target status is compatible with current status
    if (!instance.getAllowed().contains(currentStatus))
      throw new BadRequestException("Target status [%s] not compatible with current Order status [%s]".formatted(targetStatus, currentStatus));

    // Handle new target Order state
    final Order handled = instance.handleState(stateModel);

    // Setting new target Order state
    handled.setStatus(instance.getStatus().name());

    // Save new Order on DB
    return Optional.of(repository.save(handled))
        .map(mapper::toDto);
  }
}
