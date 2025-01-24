package org.example.awesome.pizza.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.awesome.pizza.domain.Order;
import org.example.awesome.pizza.exception.BadRequestException;
import org.example.awesome.pizza.exception.InternalServerErrorException;
import org.example.awesome.pizza.exception.NotFoundException;
import org.example.awesome.pizza.mapper.OrderMapper;
import org.example.awesome.pizza.model.OrderDto;
import org.example.awesome.pizza.model.OrderInternalReq;
import org.example.awesome.pizza.model.OrderStatus;
import org.example.awesome.pizza.repository.OrderRepository;
import org.example.awesome.pizza.service.OrderService;
import org.example.awesome.pizza.state.OrderState;
import org.example.awesome.pizza.state.model.OrderStateModel;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class OrderServiceImpl extends BaseService<OrderDto, OrderInternalReq, Order> implements OrderService {
  private final OrderRepository repo;
  private final OrderState orderState;

  OrderServiceImpl(
      final OrderRepository repository,
      final OrderState orderState,
      final OrderMapper mapper
  ) {
    super(repository, mapper);
    this.repo = repository;
    this.orderState = orderState;
  }

  /**
   * Finds one Order by its code; For customer use
   * @param code: Code of the Order
   * @return OrderDto instance
   */
  public OrderDto findByCode(final Long code) {
    if (code == null)
      throw new BadRequestException("Invalid order code");

    return repo.findOneByCode(code)
        .map(mapper::toDto)
        .orElseThrow(() -> new NotFoundException("No orders found by code %d".formatted(code)));
  }

  /**
   * Finds a list of Orders by input statuses: if no input statuses, finds for all statuses
   * @param status: optional input list of status
   * @return List of OrderDto instance
   */
  public List<OrderDto> findAllOrders(final List<OrderStatus> status) {
    final String[] stringStatus = Optional.ofNullable(status)
        .filter(list -> !CollectionUtils.isEmpty(list))
        .orElseGet(() -> List.of(OrderStatus.values())).stream()
        .map(OrderStatus::name)
        .toArray(String[]::new);

    return Optional.of(repo.findAllSortedByCreatedDate(stringStatus)).stream()
        .flatMap(Collection::stream)
        .map(mapper::toDto)
        .toList();
  }

  /**
   * Updates an existing Order based on input ID and request
   * @param id: ID of the Order to update: required for CANCELED and CREATED target status
   * @param request: Request with updated values to save: required
   * @param chefId: ID of the chef: required for COOKING and DONE target status
   * @return updated Order
   */
  public OrderDto updateOrder(final Long id, final OrderInternalReq request, final Long chefId) {
    final OrderStateModel stateModel = new OrderStateModel()
        .id(id)
        .request(request.setChefId(chefId));

    return this.orderState.handleState(request.getStatus(), stateModel)
        .orElseThrow(() -> new InternalServerErrorException("Impossible to update Order %d".formatted(id)));
  }


  /**
   * Updates to DONE the current COOKING Order, if present; then updates to COOKING the next Order, returning
   * it to the invoker
   * @param chefId: Chef that requested next Order to cook: required
   * @param id: ID of the next Order to cook: optional, if null, finds next CREATED Order based on createdDate
   * @return next COOKING Order, if found
   */
  public Optional<OrderDto> takeNext(final Long chefId, final Long id) {
    final OrderInternalReq internalRequest = new OrderInternalReq()
        .setChefId(chefId);

    // Complete current COOKING Order, if present
    final OrderStateModel doneModel = new OrderStateModel()
        .request(internalRequest);
    this.orderState.handleState(OrderStatus.DONE, doneModel);

    // Start cooking request Order, when specified, otherwise next CREATED Order based on createdDate
    final OrderStateModel nextCooking = new OrderStateModel()
        .id(id)
        .request(internalRequest);
    return this.orderState.handleState(OrderStatus.COOKING, nextCooking);
  }
}
