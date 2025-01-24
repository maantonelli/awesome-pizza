package org.example.awesome.pizza.repository;

import org.assertj.core.api.Assertions;
import org.example.awesome.pizza.AwesomePizzaApplication;
import org.example.awesome.pizza.domain.Pizza;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
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
@ContextConfiguration(classes = {PizzaRepository.class, AwesomePizzaApplication.class})
class PizzaRepositoryTest {
  @Autowired
  private PizzaRepository repository;

  private Pizza existing;

  @BeforeEach
  void setUp() {
    existing = new Pizza()
        .setName("PizzaName")
        .setDescription("PizzaDescription")
        .setPrice(BigDecimal.valueOf(6.3));
    this.repository.save(existing);
  }

  @AfterEach
  void dispose() {
    this.repository.deleteById(existing.getId());
  }

  @Test
  void findByIdTest_Found() {
    final Pizza found = repository.findById(existing.getId())
        .orElse(null);

    Assertions.assertThat(found)
        .isNotNull()
        .hasNoNullFieldsOrProperties()
        .isEqualTo(existing);
  }

  @Test
  void findByIdTest_NotFound() {
    final Pizza found = repository.findById(999999999L)
        .orElse(null);

    Assertions.assertThat(found)
        .isNull();
  }

  @Test
  void saveTest() {
    final Pizza p = Instancio.of(Pizza.class)
        .ignore(all(field(Pizza::getId), field(Pizza::getCreatedDate), field(Pizza::getLastModifiedDate)))
        .create();

    final Pizza saved = repository.save(p);

    Assertions.assertThat(saved)
        .isNotNull()
        .hasNoNullFieldsOrProperties()
        .matches(s -> Objects.equals(s.getName(), p.getName()))
        .matches(s -> Objects.equals(s.getDescription(), p.getDescription()));
  }

  @Test
  void updateTest() {
    final String newName = Instancio.gen().string().get();
    final String newDescription = Instancio.gen().string().get();

    existing.setName(newName);
    existing.setDescription(newDescription);

    final Pizza updated = repository.save(existing);

    Assertions.assertThat(updated)
        .isNotNull()
        .hasNoNullFieldsOrProperties()
        .matches(u -> Objects.equals(u.getId(), existing.getId()))
        .matches(u -> Objects.equals(u.getCreatedDate(), existing.getCreatedDate()))
        .matches(u -> Objects.equals(u.getName(), newName))
        .matches(u -> Objects.equals(u.getDescription(), newDescription));
  }

  @ParameterizedTest
  @EmptySource
  @ValueSource(strings = {"name", "desc"})
  void findByTextTest_Found(final String searchText) {
    final List<Pizza> results = repository.findByText(searchText);

    Assertions.assertThat(results)
        .isNotNull()
        .hasSizeGreaterThanOrEqualTo(1)
        .matches(r -> r.contains(existing));
  }
}
