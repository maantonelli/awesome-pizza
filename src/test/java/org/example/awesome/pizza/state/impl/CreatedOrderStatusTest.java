package org.example.awesome.pizza.state.impl;

import org.assertj.core.api.Assertions;
import org.example.awesome.pizza.domain.Chef;
import org.example.awesome.pizza.domain.Order;
import org.example.awesome.pizza.domain.Pizza;
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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith({SpringExtension.class})
class CreatedOrderStatusTest {
  @InjectMocks
  private CreatedOrderState underTest;

  @Mock
  private OrderRepository repository;
  @Spy
  private OrderMapper mapper = Mappers.getMapper(OrderMapper.class);

  @Test
  void getStatusTest() {
    Assertions.assertThat(underTest.getStatus())
        .isEqualTo(OrderStatus.CREATED);
  }

  @Test
  void getAllowedTest() {
    Assertions.assertThat(underTest.getAllowed())
        .isEqualTo(Arrays.asList(OrderStatus.COOKING, OrderStatus.CREATED));
  }

  private static Stream<Arguments> validate_ParametersFail() {
    final OrderStateModel restoreStatusModel = new OrderStateModel()
        .current(new Order().setStatus(OrderStatus.COOKING.name()).setChef((Chef) new Chef().setId(1L)))
        .request((OrderInternalReq) new OrderInternalReq().setChefId(2L).pizzas(List.of(1L)));

    final OrderStateModel chefModifyPizzasModel = new OrderStateModel()
        .current(new Order().setStatus(OrderStatus.CREATED.name()))
        .request((OrderInternalReq) new OrderInternalReq().setChefId(1L).status(OrderStatus.CREATED));

    return Stream.of(
        Arguments.of(BadRequestException.class, new OrderStateModel().request(new OrderInternalReq())),
        Arguments.of(BadRequestException.class, restoreStatusModel),
        Arguments.of(ForbiddenException.class, chefModifyPizzasModel)
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
  void handleState_WhenCurrentNull_ShouldCallToEntity() {
    final OrderStateModel model = Instancio.of(OrderStateModel.class)
        .ignore(field(OrderStateModel.class, "current"))
        .create();

    final Order result = underTest.handleState(model);

    Assertions.assertThat(result)
        .isNotNull()
        .matches(o -> o.getId() == null)
        .matches(o -> o.getPizzas() != null)
        .matches(o -> o.getPizzas().size() == model.request().getPizzas().size())
        .matches(o -> o.getChef() == null);

    verify(mapper).toEntity(any());
    verify(mapper, never()).patch(any(), any());
  }

  @Test
  void handleState_WhenCurrentNotNull_ShouldCallPatch() {
    final OrderStateModel model = Instancio.of(OrderStateModel.class)
        .set(field(OrderInternalReq::getStatus), OrderStatus.COOKING)
        .create();

    final Order result = underTest.handleState(model);

    Assertions.assertThat(result)
        .isNotNull()
        .matches(o -> o.getId() != null)
        .matches(o -> o.getPizzas() != null)
        .matches(o -> o.getChef() == null || o.getChef().getId() == null);

    verify(mapper, never()).toEntity(any());
    verify(mapper).patch(any(), any());
  }

  @Test
  void handleState_WhenCurrentCreatedWitNoRequestPizzas_ShouldNotUpdatePizzas() {
    final OrderStateModel model = Instancio.of(OrderStateModel.class)
        .ignore(field(OrderInternalReq::getPizzas))
        .set(field(OrderInternalReq::getStatus), OrderStatus.CREATED)
        .create();

    final Order result = underTest.handleState(model);

    Assertions.assertThat(result)
        .isNotNull()
        .matches(o -> o.getId() != null)
        .matches(o -> o.getPizzas() != null)
        .matches(o -> model.current().getPizzas().containsAll(o.getPizzas()))
        .matches(o -> o.getChef() == null || o.getChef().getId() == null);

    verify(mapper, never()).toEntity(any());
    verify(mapper).patch(any(), any());
  }

  @Test
  void handleState_WhenCurrentCreatedWithRequestPizzas_ShouldUpdatePizzas() {
    final OrderStateModel model = Instancio.of(OrderStateModel.class)
        .set(field(OrderInternalReq::getStatus), OrderStatus.CREATED)
        .create();

    final Order result = underTest.handleState(model);

    Assertions.assertThat(result)
        .isNotNull()
        .matches(o -> o.getId() != null)
        .matches(o -> o.getPizzas() != null)
        .matches(o -> model.current().getPizzas().stream().allMatch(p -> model.request().getPizzas().stream().anyMatch(rp -> Objects.equals(rp, p.getId()))))
        .matches(o -> o.getChef() == null || o.getChef().getId() == null);

    verify(mapper, never()).toEntity(any());
    verify(mapper).patch(any(), any());
  }
}
