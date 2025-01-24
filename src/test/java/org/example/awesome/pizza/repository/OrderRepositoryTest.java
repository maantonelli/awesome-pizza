package org.example.awesome.pizza.repository;

import org.assertj.core.api.Assertions;
import org.example.awesome.pizza.AwesomePizzaApplication;
import org.example.awesome.pizza.domain.Chef;
import org.example.awesome.pizza.domain.Order;
import org.example.awesome.pizza.domain.Pizza;
import org.example.awesome.pizza.model.OrderStatus;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static org.instancio.Select.all;
import static org.instancio.Select.field;

@DataJpaTest(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:unittestdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=update"
    }, showSql = false
)
@ContextConfiguration(classes = {
    OrderRepository.class,
    PizzaRepository.class,
    ChefRepository.class,
    AwesomePizzaApplication.class
})
class OrderRepositoryTest {
  @Autowired
  private OrderRepository repository;
  @Autowired
  private PizzaRepository pizzaRepo;
  @Autowired
  private ChefRepository chefRepo;

  private Chef chef;
  private Pizza pizza;
  private Order existing;

  @BeforeEach
  void setUp() {
    chef = new Chef()
        .setFirstName("FirstName")
        .setLastName("LastName");
    chefRepo.save(chef);

    pizza = new Pizza()
        .setName("PizzaName")
        .setDescription("PizzaDescription")
        .setPrice(BigDecimal.valueOf(6.3));
    pizzaRepo.save(pizza);

    existing = new Order()
        .setStatus(OrderStatus.CREATED.name())
        .setPizzas(List.of(
            pizza
        ));
    repository.save(existing);
  }

  @AfterEach
  void dispose() {
    repository.deleteById(existing.getId());
    pizzaRepo.deleteById(pizza.getId());
  }

  @Test
  void findByIdTest_Found() {
    final Order found = repository.findById(existing.getId())
        .orElse(null);

    Assertions.assertThat(found)
        .isNotNull()
        .hasNoNullFieldsOrPropertiesExcept("chef")
        .isEqualTo(existing);
  }

  @Test
  void findByIdTest_NotFound() {
    final Order found = repository.findById(999999999L)
        .orElse(null);

    Assertions.assertThat(found)
        .isNull();
  }

  @Test
  void saveTest() {
    final Order o = Instancio.of(Order.class)
        .ignore(all(field(Pizza::getId), field(Pizza::getCreatedDate), field(Pizza::getLastModifiedDate)))
        .create();

    final Order saved = repository.save(o);

    Assertions.assertThat(saved)
        .isNotNull()
        .hasNoNullFieldsOrProperties()
        .matches(s -> Objects.equals(s.getStatus(), o.getStatus()))
        .matches(s -> Objects.equals(s.getCode(), o.getCode()));
  }

  @Test
  void findOneByCode_NotFound() {
    final Order result = repository.findOneByCode(999999999L)
        .orElse(null);

    Assertions.assertThat(result)
        .isNull();
  }

  @Test
  void findOneByCode_Found() {
    final Order result = repository.findOneByCode(existing.getCode())
        .orElse(null);

    Assertions.assertThat(result)
        .isNotNull()
        .hasNoNullFieldsOrPropertiesExcept("chef")
        .matches(r -> Objects.equals(r.getId(), existing.getId()))
        .matches(r -> Objects.equals(r.getCode(), existing.getCode()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"CREATED"})
  void findAllSortedByCreatedDateTest_Found(final String status) {
    final List<Order> results = repository.findAllSortedByCreatedDate(status);

    Assertions.assertThat(results)
        .isNotNull()
        .hasSizeGreaterThanOrEqualTo(1)
        .matches(r -> r.contains(existing));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"DONE", "COOKING", "CANCELED"})
  void findAllSortedByCreatedDateTest_NotFound(final String status) {
    final List<Order> results = repository.findAllSortedByCreatedDate(status);

    Assertions.assertThat(results)
        .isNotNull()
        .isEmpty();
  }

  @Test
  void canTakeAnyOrder_WhenNoCookingInDB_ShouldReturnTrue() {
    final boolean result = repository.canTakeAnyOrder(1L, 1L);

    Assertions.assertThat(result)
        .isTrue();
  }

  @Test
  void canTakeAnyOrder_WhenCookingInDB_ShouldReturnFalse() {
    final Order cooking = new Order()
        .setStatus(OrderStatus.COOKING.name())
        .setChef((Chef) new Chef().setId(chef.getId()))
        .setPizzas(List.of((Pizza) new Pizza().setId(pizza.getId())));
    repository.save(cooking);

    final boolean result = repository.canTakeAnyOrder(chef.getId(), 1L);

    Assertions.assertThat(result)
        .isFalse();
  }
}
