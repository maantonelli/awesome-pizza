package org.example.awesome.pizza.controller;

import lombok.RequiredArgsConstructor;
import org.example.awesome.pizza.mapper.OrderMapper;
import org.example.awesome.pizza.model.OrderDto;
import org.example.awesome.pizza.model.OrderInternalReq;
import org.example.awesome.pizza.model.OrderRequest;
import org.example.awesome.pizza.model.OrderStatus;
import org.example.awesome.pizza.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderController implements OrderApi {
  private final OrderService service;
  private final OrderMapper mapper;

  /**
   * REST Endpoint for creating new Order entity: for Customer use
   * @param orderRequest Order object to be added by customer (required)
   * @return the saved OrderDto instance
   */
  @Override
  public ResponseEntity<OrderDto> createOrders(OrderRequest orderRequest) {
    final OrderInternalReq request = mapper.toInternalReq(orderRequest);
    final OrderDto saved = service.save(request);

    return ResponseEntity
        .created(URI.create("/order/%d".formatted(saved.getId())))
        .body(saved);
  }

  /**
   * REST Endpoint for finding single Order by its code: for Customer use
   * @param code Code of the Order (required)
   * @return the retrieved OrderDto instance
   */
  @Override
  public ResponseEntity<OrderDto> findOrderByCode(Long code) {
    final OrderDto result = service.findByCode(code);
    return ResponseEntity.ok(result);
  }

  /**
   * REST Endpoint for finding single Order by its ID: for all users use
   * @param id ID of the entity (required)
   * @return the retrieved OrderDto instance
   */
  @Override
  public ResponseEntity<OrderDto> findOrderById(Long id) {
    final OrderDto result = service.findById(id);
    return ResponseEntity.ok(result);
  }

  /**
   * REST Endpoint for finding a list of Order, potentially filtered by statuses: for Pizza restaurant staff use
   * @param statuses Statuses of the order (optional)
   * @return a list of OrderDto instance
   */
  @Override
  public ResponseEntity<List<OrderDto>> findOrders(List<OrderStatus> statuses) {
    final List<OrderDto> results = service.findAllOrders(statuses);

    return ResponseEntity.ok(results);
  }

  /**
   * REST Endpoint for updating a single Order: for both Customer and Pizza restaurant staff use
   * @param id ID of the entity (required)
   * @param orderRequest Order object to be added by customer (required)
   * @param xChefId ID of the Pizza Chef (optional)
   * @return updated OrderDto instance
   */
  @Override
  public ResponseEntity<OrderDto> updateOrder(Long id, OrderRequest orderRequest, Long xChefId) {
    final OrderInternalReq request = mapper.toInternalReq(orderRequest);
    final OrderDto result = service.updateOrder(id, request, xChefId);

    return ResponseEntity.ok(result);
  }

  /**
   * REST Endpoint for completing current COOKING Order, and start COOKING the next Order, filtered by
   * Order ID when passed: for Chef use
   * @param xChefId ID of the Pizza Chef (required)
   * @param id ID of the entity optional (optional)
   * @return the next Order instance that is now COOKING, when found any
   */
  @Override
  public ResponseEntity<OrderDto> takeChargeNext(Long xChefId, Long id) {

    return service.takeNext(xChefId, id)
        .map(ResponseEntity::ok)
        .orElseGet(ResponseEntity.noContent()::build);
  }
}
