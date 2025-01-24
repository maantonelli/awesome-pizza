package org.example.awesome.pizza.service;

import org.example.awesome.pizza.model.OrderDto;
import org.example.awesome.pizza.model.OrderInternalReq;
import org.example.awesome.pizza.model.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderService extends BaseReadService<OrderDto>, BaseCreateService<OrderDto, OrderInternalReq> {
  OrderDto findByCode(final Long code);
  List<OrderDto> findAllOrders(final List<OrderStatus> status);
  OrderDto updateOrder(final Long id, final OrderInternalReq request, final Long chefId);
  Optional<OrderDto> takeNext(final Long chefId, final Long id);
}
