package org.example.awesome.pizza.state.impl;

import org.assertj.core.api.Assertions;
import org.example.awesome.pizza.domain.Order;
import org.example.awesome.pizza.exception.BadRequestException;
import org.example.awesome.pizza.exception.ForbiddenException;
import org.example.awesome.pizza.mapper.OrderMapper;
import org.example.awesome.pizza.model.OrderInternalReq;
import org.example.awesome.pizza.model.OrderStatus;
import org.example.awesome.pizza.repository.OrderRepository;
import org.example.awesome.pizza.state.model.OrderStateModel;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith({SpringExtension.class})
class CanceledOrderStatusTest {
  @InjectMocks
  private CanceledOrderState underTest;

  @Mock
  private OrderRepository repository;
  @Spy
  private OrderMapper mapper = Mappers.getMapper(OrderMapper.class);

  @Test
  void getStatusTest() {
    Assertions.assertThat(underTest.getStatus())
        .isEqualTo(OrderStatus.CANCELED);
  }

  @Test
  void getAllowedTest() {
    Assertions.assertThat(underTest.getAllowed())
        .isEqualTo(List.of(OrderStatus.CREATED));
  }

  private static Stream<Arguments> validate_ParametersFail() {
    return Stream.of(
        Arguments.of(BadRequestException.class, new OrderStateModel()),
        Arguments.of(ForbiddenException.class, new OrderStateModel().current(new Order()).request(new OrderInternalReq().setChefId(1L)))
    );
  }

  @ParameterizedTest
  @MethodSource("validate_ParametersFail")
  void validate_WhenFails_ShouldThrow(final Class<Throwable> t, final OrderStateModel model) {
    Assertions.assertThatExceptionOfType(t)
        .isThrownBy(() -> underTest.validate(model));
  }

  @Test
  void validate_WhenOk_ShouldReturnTrue() {
    final OrderStateModel model = Instancio.of(OrderStateModel.class)
        .ignore(field(OrderInternalReq::getChefId))
        .create();

    Assertions.assertThat(underTest.validate(model))
        .isTrue();
  }

  @Test
  void retrieveCurrent_WhenCurrentPresent_ShouldNotFindInDB() {
    final OrderStateModel model = Instancio.create(OrderStateModel.class);

    underTest.retrieveCurrent(model);

    verify(repository, never()).findById(any());
  }

  @Test
  void retrieveCurrent_WhenCurrentNotPresent_ShouldFindInDB() {
    final OrderStateModel model = Instancio.of(OrderStateModel.class)
        .ignore(field(OrderStateModel.class, "current"))
        .create();
    final Order entity = Instancio.of(Order.class)
        .set(field(Order::getId), model.id())
        .create();
    doReturn(Optional.of(entity)).when(repository).findById(model.id());

    underTest.retrieveCurrent(model);

    Assertions.assertThat(model.current())
        .isNotNull()
        .isEqualTo(entity);

    verify(repository).findById(any());
  }

  @Test
  void handleState_WhenOk_ShouldReturnEntity() {
    final OrderStateModel model = Instancio.create(OrderStateModel.class);

    Assertions.assertThat(underTest.handleState(model))
        .isEqualTo(model.current());
  }
}
