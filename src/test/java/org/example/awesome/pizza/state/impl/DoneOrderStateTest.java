package org.example.awesome.pizza.state.impl;

import org.assertj.core.api.Assertions;
import org.example.awesome.pizza.domain.Chef;
import org.example.awesome.pizza.domain.Order;
import org.example.awesome.pizza.exception.BadRequestException;
import org.example.awesome.pizza.exception.InternalServerErrorException;
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

import static org.instancio.Select.all;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith({SpringExtension.class})
class DoneOrderStateTest {
  @InjectMocks
  private DoneOrderState underTest;

  @Mock
  private OrderRepository repository;
  @Spy
  private OrderMapper mapper = Mappers.getMapper(OrderMapper.class);

  @Test
  void getStatusTest() {
    Assertions.assertThat(underTest.getStatus())
        .isEqualTo(OrderStatus.DONE);
  }

  @Test
  void getAllowedTest() {
    Assertions.assertThat(underTest.getAllowed())
        .isEqualTo(List.of(OrderStatus.COOKING));
  }

  private static Stream<Arguments> validate_ParametersFail() {
    return Stream.of(
        Arguments.of(InternalServerErrorException.class, new OrderStateModel().current(new Order())),
        Arguments.of(InternalServerErrorException.class, new OrderStateModel().current(new Order().setChef(new Chef()))),
        Arguments.of(BadRequestException.class, new OrderStateModel().current(new Order().setChef((Chef) new Chef().setId(1L))).request(new OrderInternalReq())),
        Arguments.of(BadRequestException.class, new OrderStateModel().current(new Order().setChef((Chef) new Chef().setId(1L))).request(new OrderInternalReq().setChefId(2L)))
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
        .set(field(Order::getStatus), OrderStatus.COOKING.name())
        .set(field(Chef::getId), 1L)
        .set(field(OrderInternalReq::getChefId), 1L)
        .create();

    Assertions.assertThat(underTest.validate(model))
        .isTrue();
  }

  @Test
  void validate_WhenKo_ShouldReturnFalse() {
    final OrderStateModel model = Instancio.of(OrderStateModel.class)
        .ignore(field(OrderStateModel.class, "current"))
        .create();

    Assertions.assertThat(underTest.validate(model))
        .isFalse();
  }

  @Test
  void retrieveCurrent_WhenNoChefInRequest_ShouldThrow() {
    final OrderStateModel model = Instancio.of(OrderStateModel.class)
        .ignore(field(OrderInternalReq::getChefId))
        .create();

    Assertions.assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> underTest.retrieveCurrent(model));

    verify(repository, never()).findById(any());
    verify(repository, never()).findCookingOrder(any());
  }

  @Test
  void retrieveCurrent_WhenIdPresent_ShouldFindById() {
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

    verify(repository).findById(model.id());
    verify(repository, never()).findCookingOrder(any());
  }

  @Test
  void retrieveCurrent_WhenIdNotPresent_ShouldFindByChefId() {
    final OrderStateModel model = Instancio.of(OrderStateModel.class)
        .ignore(all(field(OrderStateModel.class, "current"), field(OrderStateModel.class, "id")))
        .create();
    final Order entity = Instancio.of(Order.class)
        .set(field(Order::getId), model.id())
        .create();
    doReturn(Optional.of(entity)).when(repository).findCookingOrder(model.request().getChefId());

    underTest.retrieveCurrent(model);

    Assertions.assertThat(model.current())
        .isNotNull()
        .isEqualTo(entity);

    verify(repository, never()).findById(any());
    verify(repository).findCookingOrder(model.request().getChefId());
  }

  @Test
  void handleState_WhenOk_ShouldReturnCurrent() {
    final OrderStateModel model = Instancio.of(OrderStateModel.class)
        .create();

    final Order result = underTest.handleState(model);

    Assertions.assertThat(result)
        .isNotNull()
        .isEqualTo(model.current());
  }
}
