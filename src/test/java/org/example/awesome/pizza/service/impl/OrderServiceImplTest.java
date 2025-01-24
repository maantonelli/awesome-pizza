package org.example.awesome.pizza.service.impl;

import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.Assertions;
import org.example.awesome.pizza.domain.Order;
import org.example.awesome.pizza.exception.BadRequestException;
import org.example.awesome.pizza.exception.InternalServerErrorException;
import org.example.awesome.pizza.exception.NotFoundException;
import org.example.awesome.pizza.mapper.OrderMapper;
import org.example.awesome.pizza.model.OrderDto;
import org.example.awesome.pizza.model.OrderInternalReq;
import org.example.awesome.pizza.model.OrderStatus;
import org.example.awesome.pizza.repository.OrderRepository;
import org.example.awesome.pizza.state.OrderState;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.instancio.Select.all;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith({SpringExtension.class})
@TestPropertySource(properties = {
    "awesome-pizza.config.cooking-threshold=1"
})
class OrderServiceImplTest {
  @InjectMocks
  private OrderServiceImpl underTest;

  @Mock
  private OrderRepository repository;
  @Spy
  private final OrderMapper mapper = Mappers.getMapper(OrderMapper.class);
  //@Value("${awesome-pizza.config.cooking-threshold:1}")
  @Mock
  private OrderState orderState;

  @Test
  void findByCode_WhenInvalidInput_ShouldThrow() {
    Assertions.assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> underTest.findByCode(null));

    verify(repository, never()).findOneByCode(any());
    verify(mapper, never()).toDto(any());
  }

  @Test
  void findByCode_WhenNoEntityFound_ShouldThrow() {
    final Long code = 1L;

    doReturn(Optional.empty()).when(repository).findOneByCode(code);

    Assertions.assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> underTest.findByCode(code));

    verify(repository).findOneByCode(code);
    verify(mapper, never()).toDto(any());
  }

  @Test
  void findByCode_WhenEntityFound_ShouldReturnOK() {
    final Long code = 1L;
    final Order entity = Instancio.of(Order.class)
        .set(field(Order::getCode), code)
        .generate(field(Order::getStatus), g -> g.enumOf(OrderStatus.class).as(OrderStatus::name))
        .create();

    doReturn(Optional.of(entity)).when(repository).findOneByCode(code);

    final OrderDto result = underTest.findByCode(code);

    Assertions.assertThat(result)
        .isNotNull()
        .matches(o -> Objects.equals(o.getId(), entity.getId()))
        .matches(o -> Objects.equals(o.getCode(), entity.getCode()))
        .matches(o -> Objects.equals(o.getStatus().name(), entity.getStatus()))
        .matches(o -> Objects.equals(o.getPizzas().size(), entity.getPizzas().size()))
        .matches(o -> Objects.equals(o.getCreatedDate().toInstant(), entity.getCreatedDate()))
        .matches(o -> Objects.equals(o.getLastModifiedDate().toInstant(), entity.getLastModifiedDate()));

    verify(repository).findOneByCode(code);
    verify(mapper).toDto(entity);
  }

  @ParameterizedTest
  @NullSource
  @EnumSource(value = OrderStatus.class)
  void findAllOrders_WhenNoEntityFound_ShouldReturnEmptyList(final OrderStatus status) {
    final Optional<OrderStatus> optStatus = Optional.ofNullable(status);
    final String[] stringStatus = optStatus
        .map(List::of)
        .orElseGet(() -> List.of(OrderStatus.values())).stream()
        .map(OrderStatus::name)
        .toArray(String[]::new);
    final List<OrderStatus> statuses = optStatus
        .map(List::of)
        .orElseGet(() -> List.of(OrderStatus.values()));
    doReturn(List.of()).when(repository).findAllSortedByCreatedDate(stringStatus);

    final List<OrderDto> results = underTest.findAllOrders(statuses);

    Assertions.assertThat(results)
        .isNotNull()
        .isEmpty();

    verify(mapper, never()).toDto(any());
  }

  @ParameterizedTest
  @NullSource
  @EnumSource(value = OrderStatus.class)
  void findAllOrders_WhenAnyEntityFound_ShouldReturnFullList(final OrderStatus status) {
    final Optional<OrderStatus> optStatus = Optional.ofNullable(status);
    final String[] stringStatus = optStatus
        .map(List::of)
        .orElseGet(() -> List.of(OrderStatus.values())).stream()
        .map(OrderStatus::name)
        .toArray(String[]::new);
    final List<OrderStatus> statuses = optStatus
        .map(List::of)
        .orElseGet(() -> List.of(OrderStatus.values()));
    final List<Order> entities = Instancio.ofList(Order.class)
        .set(field(Order::getStatus), stringStatus[0])
        .create();

    doReturn(entities).when(repository).findAllSortedByCreatedDate(stringStatus);

    final List<OrderDto> results = underTest.findAllOrders(statuses);

    Assertions.assertThat(results)
        .isNotNull()
        .isNotEmpty()
        .hasSize(entities.size())
        .allMatch(o -> entities.stream().anyMatch(e -> Objects.equals(e.getId(), o.getId())));

    verify(mapper, times(entities.size())).toDto(any());
  }

  private static Stream<Arguments> updateOrder_Parameters() {
    return Stream.of(
        Arguments.of(null, null, null),
        Arguments.of(1L, null, null),
        Arguments.of(null, OrderStatus.COOKING, null),
        Arguments.of(null, null, 1L),
        //
        Arguments.of(1L, OrderStatus.CREATED, 1L),
        Arguments.of(1L, OrderStatus.COOKING, 1L),
        Arguments.of(1L, OrderStatus.DONE, 1L),
        Arguments.of(1L, OrderStatus.CANCELED, 1L)
    );
  }

  @ParameterizedTest
  @MethodSource("updateOrder_Parameters")
  void updateOrder_WhenException_ShouldThrow(final Long id, final OrderStatus status, final Long chefId) {
    doThrow(new BadRequestException("Bad request")).when(orderState).handleState(any(), any());

    Assertions.assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> underTest.updateOrder(id, (OrderInternalReq) new OrderInternalReq().status(status), chefId));

    verify(orderState).handleState(any(), any());
  }

  @ParameterizedTest
  @MethodSource("updateOrder_Parameters")
  void updateOrder_WhenEmptyResult_ShouldThrow(final Long id, final OrderStatus status, final Long chefId) {
    doThrow(new InternalServerErrorException("Internal server error")).when(orderState).handleState(any(), any());

    Assertions.assertThatExceptionOfType(InternalServerErrorException.class)
        .isThrownBy(() -> underTest.updateOrder(id, (OrderInternalReq) new OrderInternalReq().status(status), chefId));

    verify(orderState).handleState(any(), any());
  }

  @ParameterizedTest
  @EnumSource(value = OrderStatus.class)
  void updateOrder_WhenFullResult_ShouldReturnOk(final OrderStatus newStatus) {
    final Long id = 1L;
    final Long chefId = 1L;
    final OrderDto expected = Instancio.of(OrderDto.class)
        .set(field(OrderDto::getId), id)
        .set(field(OrderDto::getStatus), newStatus)
        .create();

    doReturn(Optional.of(expected)).when(orderState).handleState(eq(newStatus), any());

    final OrderDto result = underTest.updateOrder(id, (OrderInternalReq) new OrderInternalReq().status(newStatus), chefId);

    Assertions.assertThat(result)
        .isNotNull()
        .isEqualTo(expected);

    verify(orderState).handleState(eq(newStatus), any());
  }

  @Test
  void findById_WhenInvalidInput_ShouldThrow() {
    Assertions.assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> underTest.findById(null));

    verify(repository, never()).findById(any());
    verify(mapper, never()).toDto(any());
  }

  @Test
  void findById_WhenNoEntityFound_ShouldThrow() {
    final Long id = 1L;

    doReturn(Optional.empty()).when(repository).findById(id);

    Assertions.assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> underTest.findById(id));

    verify(repository).findById(id);
    verify(mapper, never()).toDto(any());
  }

  @Test
  void findById_WhenFound_ShouldReturnOK() {
    final Long id = 1L;
    final Order entity = Instancio.of(Order.class)
        .set(field(Order::getId), id)
        .generate(field(Order::getStatus), g -> g.enumOf(OrderStatus.class).as(OrderStatus::name))
        .create();

    doReturn(Optional.of(entity)).when(repository).findById(id);

    final OrderDto result = underTest.findById(id);

    Assertions.assertThat(result)
        .isNotNull()
        .matches(o -> Objects.equals(o.getId(), entity.getId()))
        .matches(o -> Objects.equals(o.getCode(), entity.getCode()))
        .matches(o -> Objects.equals(o.getStatus().name(), entity.getStatus()))
        .matches(o -> Objects.equals(o.getPizzas().size(), entity.getPizzas().size()))
        .matches(o -> Objects.equals(o.getCreatedDate().toInstant(), entity.getCreatedDate()))
        .matches(o -> Objects.equals(o.getLastModifiedDate().toInstant(), entity.getLastModifiedDate()));

    verify(repository).findById(id);
    verify(mapper).toDto(entity);
  }

  @Test
  void findAll_WhenNoEntityFound_ShouldReturnEmpty() {
    doReturn(Page.empty()).when(repository).findAll(any(Pageable.class));

    final Page<OrderDto> results = underTest.findAll(null);

    assertNotNull(results);
    assertFalse(results.hasContent());

    verify(mapper, never()).toDto(any());
    verify(repository).findAll(any(Pageable.class));
  }

  @Test
  void findAll_WhenAnyEntityFound_ShouldReturnFullPage() {
    final List<Order> entities = Instancio.ofList(Order.class)
        .generate(field(Order::getStatus), g -> g.enumOf(OrderStatus.class).as(OrderStatus::name))
        .create();

    doReturn(new PageImpl<>(entities)).when(repository).findAll(any(Pageable.class));

    final Page<OrderDto> results = underTest.findAll(null);

    assertNotNull(results);
    assertTrue(results.hasContent());
    assertEquals(entities.size(), results.getTotalElements());

    verify(mapper, times(entities.size())).toDto(any(Order.class));
    verify(repository).findAll(any(Pageable.class));
  }

  @Test
  void delete_WhenInvalidInput_ShouldThrow() {
    Assertions.assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> underTest.delete(null));

    verify(repository, never()).deleteById(any());
  }

  @Test
  void delete_WhenValidInput_ShouldInvokeDelete() {
    final Long id = 1L;

    doNothing().when(repository).deleteById(id);

    underTest.delete(id);

    verify(repository).deleteById(id);
  }

  @Test
  void save_WhenInvalidInput_ShouldThrow() {
    Assertions.assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> underTest.save(null));

    verify(mapper, never()).toEntity(any());
    verify(repository, never()).save(any());
    verify(mapper, never()).toDto(any());
  }

  @Test
  void save_WhenValidRequest_ShouldInsert() {
    final OrderInternalReq request = Instancio.of(OrderInternalReq.class)
        .ignore(all(field(OrderInternalReq::getStatus), field(OrderInternalReq::getChefId)))
        .create();

    doCallRealMethod().when(mapper).toEntity(any(OrderInternalReq.class));
    doAnswer(invocation -> {
      final Order arg = invocation.getArgument(0, Order.class);
      arg.setCreatedDate(Instant.now());
      arg.setLastModifiedDate(Instant.now());
      arg.setId(RandomUtils.insecure().randomLong());
      arg.setCode(RandomUtils.insecure().randomLong());
      return arg;
    }).when(repository).save(any(Order.class));
    doCallRealMethod().when(mapper).toDto(any(Order.class));

    final OrderDto result = underTest.save(request);

    Assertions.assertThat(result)
        .isNotNull()
        .matches(o -> Objects.equals(o.getStatus(), OrderStatus.CREATED))
        .matches(o -> Objects.equals(o.getPizzas().size(), request.getPizzas().size()))
        .matches(o -> Objects.nonNull(o.getCreatedDate()))
        .matches(o -> Objects.nonNull(o.getLastModifiedDate()))
        .matches(o -> Objects.nonNull(o.getCode()))
        .matches(o -> Objects.nonNull(o.getId()));

    verify(mapper).toEntity(any(OrderInternalReq.class));
    verify(repository).save(any(Order.class));
    verify(mapper).toDto(any(Order.class));
  }

  private static Stream<Arguments> update_ParametersKO() {
    return Stream.of(
        Arguments.of(null, null),
        Arguments.of(1L, null),
        Arguments.of(null, new OrderInternalReq())
    );
  }

  @ParameterizedTest
  @MethodSource("update_ParametersKO")
  void update_WhenInvalidInput_ShouldThrow(final Long id, final OrderInternalReq request) {
    Assertions.assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> underTest.update(id, request));

    verify(repository, never()).findById(any());
    verify(mapper, never()).patch(any(), any());
    verify(repository, never()).save(any());
    verify(mapper, never()).toDto(any());
  }

  @Test
  void update_WhenNoEntityFound_ShouldThrow() {
    final Long id = 1L;
    final OrderInternalReq request = Instancio.create(OrderInternalReq.class);

    doReturn(Optional.empty()).when(repository).findById(id);

    Assertions.assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> underTest.update(id, request));

    verify(repository).findById(id);
    verify(mapper, never()).patch(any(), any());
    verify(repository, never()).save(any());
    verify(mapper, never()).toDto(any());
  }

  @Test
  void save_WhenValidRequest_ShouldUpdate() {
    final Long id = 1L;
    final Instant newModifiedDate = Instant.now();
    final Order entity = Instancio.of(Order.class)
        .set(field(Order::getId), id)
        .create();
    final OrderInternalReq request = Instancio.of(OrderInternalReq.class)
        .ignore(all(field(OrderInternalReq::getPizzas), field(OrderInternalReq::getChefId)))
        .create();

    doReturn(Optional.of(entity)).when(repository).findById(id);
    doCallRealMethod().when(mapper).patch(request, entity);
    doAnswer(invocation -> {
      final Order arg = invocation.getArgument(0, Order.class);
      arg.setStatus(request.getStatus().name());
      arg.setLastModifiedDate(newModifiedDate);
      return arg;
    }).when(repository).save(any(Order.class));
    doCallRealMethod().when(mapper).toDto(any(Order.class));

    final OrderDto result = underTest.update(id, request);

    Assertions.assertThat(result)
        .isNotNull()
        .matches(o -> Objects.equals(o.getStatus(), request.getStatus()))
        .matches(o -> Objects.equals(o.getPizzas().size(), entity.getPizzas().size()))
        .matches(o -> Objects.equals(o.getCreatedDate().toInstant(), entity.getCreatedDate()))
        .matches(o -> Objects.equals(o.getLastModifiedDate().toInstant(), newModifiedDate))
        .matches(o -> Objects.equals(o.getId(), entity.getId()))
        .matches(o -> Objects.equals(o.getCode(), entity.getCode()));

    verify(repository).findById(id);
    verify(mapper).patch(request, entity);
    verify(repository).save(any(Order.class));
    verify(mapper).toDto(any(Order.class));
  }

  @Test
  void takeNext_WhenError_ShouldThrow() {
    doThrow(new BadRequestException("Bad request")).when(orderState).handleState(any(), any());

    Assertions.assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> underTest.takeNext(null, null));

    verify(orderState).handleState(any(), any());
  }

  @Test
  void takeNext_WhenNoNext_ShouldReturnEmpty() {
    final Long id = null;
    final Long chefId = 1L;

    doReturn(Optional.empty()).when(orderState).handleState(any(OrderStatus.class), any());

    final Optional<OrderDto> optResult = underTest.takeNext(chefId, id);

    Assertions.assertThat(optResult)
        .isNotNull()
        .isEmpty();

    verify(orderState).handleState(eq(OrderStatus.DONE), any());
    verify(orderState).handleState(eq(OrderStatus.COOKING), any());
  }

  @Test
  void takeNext_WhenFoundNext_ShouldReturnNextCooking() {
    final Long id = null;
    final Long chefId = 1L;

    final OrderDto expected = Instancio.of(OrderDto.class)
        .set(field(OrderDto::getStatus), OrderStatus.COOKING)
        .create();

    doReturn(Optional.empty()).when(orderState).handleState(eq(OrderStatus.DONE), any());
    doReturn(Optional.of(expected)).when(orderState).handleState(eq(OrderStatus.COOKING), any());

    final Optional<OrderDto> optResult = underTest.takeNext(chefId, id);

    Assertions.assertThat(optResult)
        .isNotNull()
        .contains(expected);

    verify(orderState).handleState(eq(OrderStatus.DONE), any());
    verify(orderState).handleState(eq(OrderStatus.COOKING), any());
  }
}
