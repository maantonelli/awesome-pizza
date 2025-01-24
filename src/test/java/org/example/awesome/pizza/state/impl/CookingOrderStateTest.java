package org.example.awesome.pizza.state.impl;

import org.assertj.core.api.Assertions;
import org.example.awesome.pizza.domain.Order;
import org.example.awesome.pizza.exception.BadRequestException;
import org.example.awesome.pizza.exception.ConflictException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
class CookingOrderStateTest {
  @InjectMocks
  private CookingOrderState underTest;

  @Mock
  private OrderRepository repository;
  @Spy
  private OrderMapper mapper = Mappers.getMapper(OrderMapper.class);

  @Value("${awesome-pizza.config.cooking-threshold:1}")
  private Long cookingThreshold;

  @Test
  void getStatusTest() {
    Assertions.assertThat(underTest.getStatus())
        .isEqualTo(OrderStatus.COOKING);
  }

  @Test
  void getAllowedTest() {
    Assertions.assertThat(underTest.getAllowed())
        .isEqualTo(List.of(OrderStatus.CREATED));
  }

  private static Stream<Arguments> validate_ParametersFail() {
    return Stream.of(
        Arguments.of(ConflictException.class, Instancio.create(OrderStateModel.class)),
        Arguments.of(BadRequestException.class, new OrderStateModel().current(new Order()).request(new OrderInternalReq()))
    );
  }

  @ParameterizedTest
  @MethodSource("validate_ParametersFail")
  void validate_WhenFails_ShouldThrow(final Class<Throwable> t, final OrderStateModel model) {
    ReflectionTestUtils.setField(underTest, "cookingThreshold", cookingThreshold);
    doReturn(false).when(repository).canTakeAnyOrder(model.request().getChefId(), cookingThreshold);

    Assertions.assertThatExceptionOfType(t)
        .isThrownBy(() -> underTest.validate(model));
  }

  @Test
  void validate_WhenKo_ShouldReturnFalse() {
    final OrderStateModel model = new OrderStateModel();

    Assertions.assertThat(underTest.validate(model))
        .isFalse();
  }

  @Test
  void validate_WhenOk_ShouldReturnTrue() {
    final OrderStateModel model = Instancio.of(OrderStateModel.class)
        .create();

    ReflectionTestUtils.setField(underTest, "cookingThreshold", cookingThreshold);
    doReturn(true).when(repository).canTakeAnyOrder(model.request().getChefId(), cookingThreshold);

    Assertions.assertThat(underTest.validate(model))
        .isTrue();
  }

  @Test
  void retrieveCurrent_WhenInputId_ShouldRetrieveById() {
    final OrderStateModel model = new OrderStateModel().id(1L);
    final Order entity = Instancio.of(Order.class)
        .set(field(Order::getId), model.id())
        .create();
    doReturn(Optional.of(entity)).when(repository).findById(model.id());

    underTest.retrieveCurrent(model);

    Assertions.assertThat(model.current())
        .isNotNull()
        .isEqualTo(entity);

    verify(repository).findById(model.id());
    verify(repository, never()).findAllSortedByCreatedDate(any());
  }

  @Test
  void retrieveCurrent_WhenNoInputId_ShouldRetrieveNearest() {
    final OrderStateModel model = new OrderStateModel();
    final Order entity = Instancio.of(Order.class)
        .set(field(Order::getId), model.id())
        .create();
    doReturn(List.of(entity)).when(repository).findAllSortedByCreatedDate(OrderStatus.CREATED.name());

    underTest.retrieveCurrent(model);

    Assertions.assertThat(model.current())
        .isNotNull()
        .isEqualTo(entity);

    verify(repository, never()).findById(any());
    verify(repository).findAllSortedByCreatedDate(OrderStatus.CREATED.name());
  }

  @Test
  void handleState_WhenOk_ShouldReturnEntity() {
    final OrderStateModel model = Instancio.create(OrderStateModel.class);

    Assertions.assertThat(underTest.handleState(model))
        .isNotNull()
        .matches(r -> Objects.equals(r.getChef().getId(), model.request().getChefId()));
  }
}
