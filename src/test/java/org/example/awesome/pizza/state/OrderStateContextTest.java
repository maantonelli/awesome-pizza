package org.example.awesome.pizza.state;

import org.assertj.core.api.Assertions;
import org.example.awesome.pizza.domain.Order;
import org.example.awesome.pizza.exception.BadRequestException;
import org.example.awesome.pizza.exception.InternalServerErrorException;
import org.example.awesome.pizza.mapper.OrderMapper;
import org.example.awesome.pizza.model.OrderDto;
import org.example.awesome.pizza.model.OrderInternalReq;
import org.example.awesome.pizza.model.OrderStatus;
import org.example.awesome.pizza.repository.OrderRepository;
import org.example.awesome.pizza.state.impl.BaseOrderState;
import org.example.awesome.pizza.state.impl.CanceledOrderState;
import org.example.awesome.pizza.state.impl.CookingOrderState;
import org.example.awesome.pizza.state.impl.DoneOrderState;
import org.example.awesome.pizza.state.model.OrderStateModel;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith({SpringExtension.class})
class OrderStateContextTest {
  private final OrderStateContext underTest;

  private final OrderRepository repository;
  private final OrderMapper mapper;

  private final List<BaseOrderState> instances;
  private final CookingOrderState cookingOrderState;
  private final CanceledOrderState canceledOrderState;
  private final DoneOrderState doneOrderState;

  OrderStateContextTest() {
    this.repository = mock(OrderRepository.class);
    this.mapper = spy(Mappers.getMapper(OrderMapper.class));

    this.cookingOrderState = mock(CookingOrderState.class);
    this.canceledOrderState = mock(CanceledOrderState.class);
    this.doneOrderState = mock(DoneOrderState.class);

    setUpMockedInstance(this.cookingOrderState);
    setUpMockedInstance(this.canceledOrderState);
    setUpMockedInstance(this.doneOrderState);

    this.instances = List.of(
        this.cookingOrderState,
        this.canceledOrderState,
        this.doneOrderState
    );

    this.underTest = new OrderStateContext(this.repository, this.mapper, this.instances);
  }

  private <T extends BaseOrderState> void setUpMockedInstance(T instance) {
    doCallRealMethod().when(instance).getStatus();
    doCallRealMethod().when(instance).getAllowed();
  }

  private static Stream<Arguments> handleState_Parameters_BadRequest() {
    return Stream.of(
        Arguments.of(null, null),
        Arguments.of(OrderStatus.COOKING, null),
        Arguments.of(null, new OrderStateModel().request(new OrderInternalReq())),
        Arguments.of(OrderStatus.COOKING, new OrderStateModel())
    );
  }

  @ParameterizedTest
  @MethodSource("handleState_Parameters_BadRequest")
  void handleState_WhenInvalidInput_ShouldThrow(final OrderStatus status, final OrderStateModel model) {
    Assertions.assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> underTest.handleState(status, model));

    verify(cookingOrderState, never()).retrieveCurrent(any());
    verify(cookingOrderState, never()).validate(any());
    verify(cookingOrderState, never()).getAllowed();
    verify(cookingOrderState, never()).handleState(any());

    verify(repository, never()).save(any());
    verify(mapper, never()).toDto(any());
  }

  @Test
  void handleState_WhenNoInstanceFound_ShouldThrow() {
    final OrderStateModel stateModel = Instancio.create(OrderStateModel.class);

    Assertions.assertThatExceptionOfType(InternalServerErrorException.class)
        .isThrownBy(() -> underTest.handleState(OrderStatus.CREATED, stateModel));

    verify(cookingOrderState, never()).retrieveCurrent(any());
    verify(cookingOrderState, never()).validate(any());
    verify(cookingOrderState, never()).getAllowed();
    verify(cookingOrderState, never()).handleState(any());

    verify(repository, never()).save(any());
    verify(mapper, never()).toDto(any());
  }

  @Test
  void handleState_WhenValidateFalse_ShouldReturnEmpty() {
    final OrderStatus status = OrderStatus.COOKING;
    final OrderStateModel stateModel = Instancio.create(OrderStateModel.class);

    doNothing().when(cookingOrderState).retrieveCurrent(stateModel);
    doReturn(false).when(cookingOrderState).validate(stateModel);

    final Optional<OrderDto> optResult = underTest.handleState(status, stateModel);

    Assertions.assertThat(optResult)
        .isNotNull()
        .isEmpty();

    verify(cookingOrderState).retrieveCurrent(stateModel);
    verify(cookingOrderState).validate(stateModel);
    verify(cookingOrderState, never()).getAllowed();
    verify(cookingOrderState, never()).handleState(stateModel);

    verify(repository, never()).save(any());
    verify(mapper, never()).toDto(any());
  }

  @Test
  void handleState_WhenValidateThroes_ShouldThrow() {
    final OrderStatus status = OrderStatus.COOKING;
    final OrderStateModel stateModel = Instancio.create(OrderStateModel.class);

    doNothing().when(cookingOrderState).retrieveCurrent(stateModel);
    doThrow(new BadRequestException("Bad request")).when(cookingOrderState).validate(stateModel);

    Assertions.assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> underTest.handleState(status, stateModel));

    verify(cookingOrderState).retrieveCurrent(stateModel);
    verify(cookingOrderState).validate(stateModel);
    verify(cookingOrderState, never()).getAllowed();
    verify(cookingOrderState, never()).handleState(stateModel);

    verify(repository, never()).save(any());
    verify(mapper, never()).toDto(any());
  }

  @Test
  void handleState_WhenTargetStatusNotCompatible_ShouldThrow() {
    final OrderStatus status = OrderStatus.CANCELED;
    final OrderStateModel stateModel = Instancio.of(OrderStateModel.class)
        .set(Select.field(Order::getStatus), OrderStatus.DONE.name())
        .create();

    doNothing().when(canceledOrderState).retrieveCurrent(stateModel);
    doReturn(true).when(canceledOrderState).validate(stateModel);

    Assertions.assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> underTest.handleState(status, stateModel));

    verify(canceledOrderState).retrieveCurrent(stateModel);
    verify(canceledOrderState).validate(stateModel);
    verify(canceledOrderState).getAllowed();
    verify(canceledOrderState, never()).handleState(stateModel);

    verify(repository, never()).save(any());
    verify(mapper, never()).toDto(any());
  }

  @Test
  void handleState_WhenHandleOk_ShouldUpdate() {
    final OrderStatus status = OrderStatus.CANCELED;
    final OrderStateModel stateModel = Instancio.of(OrderStateModel.class)
        .set(Select.field(Order::getStatus), OrderStatus.CREATED.name())
        .create();

    doNothing().when(canceledOrderState).retrieveCurrent(stateModel);
    doReturn(true).when(canceledOrderState).validate(stateModel);
    final Order order = stateModel.current();
    doReturn(order).when(canceledOrderState).handleState(stateModel);
    doAnswer(returnsFirstArg()).when(repository).save(any(Order.class));
    doCallRealMethod().when(mapper).toDto(any(Order.class));

    final Optional<OrderDto> optResult = underTest.handleState(status, stateModel);

    Assertions.assertThat(optResult)
        .isNotNull()
        .matches(o -> o.isPresent() && status.equals(o.get().getStatus()));

    verify(canceledOrderState).retrieveCurrent(stateModel);
    verify(canceledOrderState).validate(stateModel);
    verify(canceledOrderState).getAllowed();
    verify(canceledOrderState).handleState(stateModel);

    verify(repository).save(any(Order.class));
    verify(mapper).toDto(any(Order.class));
  }
}
