package org.example.awesome.pizza.state;

import org.example.awesome.pizza.model.OrderDto;
import org.example.awesome.pizza.model.OrderStatus;
import org.example.awesome.pizza.state.model.OrderStateModel;

import java.util.Optional;

public interface OrderState {
  Optional<OrderDto> handleState(final OrderStatus targetStatus, final OrderStateModel stateModel);
}
