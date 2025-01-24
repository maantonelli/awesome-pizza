package org.example.awesome.pizza.service.impl;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.example.awesome.pizza.domain.Pizza;
import org.example.awesome.pizza.exception.BadRequestException;
import org.example.awesome.pizza.exception.NotFoundException;
import org.example.awesome.pizza.mapper.PizzaMapper;
import org.example.awesome.pizza.model.PizzaDto;
import org.example.awesome.pizza.model.PizzaRequest;
import org.example.awesome.pizza.repository.PizzaRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith({SpringExtension.class})
class PizzaServiceImplTest {
  @InjectMocks
  private PizzaServiceImpl underTest;

  @Mock
  private PizzaRepository repository;
  @Spy
  private final PizzaMapper mapper = Mappers.getMapper(PizzaMapper.class);

  @ParameterizedTest
  @NullAndEmptySource
  void findByFilter_WhenNoInput_ShouldSearchAll(final String searchText) {
    final List<Pizza> entities = Instancio.createList(Pizza.class);

    doReturn(entities).when(repository).findByText(StringUtils.EMPTY);
    doCallRealMethod().when(mapper).toDto(any(Pizza.class));

    final List<PizzaDto> results = underTest.findByFilter(searchText);

    Assertions.assertThat(results)
        .isNotNull()
        .doesNotContainNull()
        .hasSize(entities.size())
        .allMatch(dto -> entities.stream().anyMatch(e -> Objects.equals(e.getId(), dto.getId())));

    verify(repository).findByText(StringUtils.EMPTY);
    verify(mapper, times(entities.size())).toDto(any(Pizza.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "margher", "MARGHER", "Margher", "maRGhEr"
  })
  void findByFilter_WhenTextInput_ShouldSearchAll(final String searchText) {
    final String actualSearchText = searchText.toLowerCase();
    final List<Pizza> entities = Instancio.ofList(Pizza.class)
        .set(field(Pizza::getName), "Margherita")
        .create();

    doReturn(entities).when(repository).findByText(actualSearchText);
    doCallRealMethod().when(mapper).toDto(any(Pizza.class));

    final List<PizzaDto> results = underTest.findByFilter(searchText);

    Assertions.assertThat(results)
        .isNotNull()
        .doesNotContainNull()
        .hasSize(entities.size())
        .allMatch(dto -> entities.stream().anyMatch(e -> Objects.equals(e.getId(), dto.getId())));

    verify(repository).findByText(actualSearchText);
    verify(mapper, times(entities.size())).toDto(any(Pizza.class));
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
    final Pizza entity = Instancio.of(Pizza.class)
        .set(field(Pizza::getId), id)
        .create();

    doReturn(Optional.of(entity)).when(repository).findById(id);

    final PizzaDto result = underTest.findById(id);

    Assertions.assertThat(result)
        .isNotNull()
        .matches(p -> Objects.equals(p.getId(), entity.getId()))
        .matches(p -> Objects.equals(p.getName(), entity.getName()))
        .matches(p -> Objects.equals(p.getDescription(), entity.getDescription()))
        .matches(p -> Objects.equals(p.getCreatedDate().toInstant(), entity.getCreatedDate()))
        .matches(p -> Objects.equals(p.getLastModifiedDate().toInstant(), entity.getLastModifiedDate()));

    verify(repository).findById(id);
    verify(mapper).toDto(entity);
  }

  @Test
  void findAll_WhenNoEntityFound_ShouldReturnEmpty() {
    doReturn(Page.empty()).when(repository).findAll(any(Pageable.class));

    final Page<PizzaDto> results = underTest.findAll(null);

    assertNotNull(results);
    assertFalse(results.hasContent());

    verify(mapper, never()).toDto(any());
    verify(repository).findAll(any(Pageable.class));
  }

  @Test
  void findAll_WhenAnyEntityFound_ShouldReturnFullPage() {
    final List<Pizza> entities = Instancio.createList(Pizza.class);

    doReturn(new PageImpl<>(entities)).when(repository).findAll(any(Pageable.class));

    final Page<PizzaDto> results = underTest.findAll(null);

    assertNotNull(results);
    assertTrue(results.hasContent());
    assertEquals(entities.size(), results.getTotalElements());

    verify(mapper, times(entities.size())).toDto(any(Pizza.class));
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
    final PizzaRequest request = Instancio.create(PizzaRequest.class);

    doCallRealMethod().when(mapper).toEntity(any(PizzaRequest.class));
    doAnswer(invocation -> {
      final Pizza arg = invocation.getArgument(0, Pizza.class);
      arg.setCreatedDate(Instant.now());
      arg.setLastModifiedDate(Instant.now());
      arg.setId(RandomUtils.insecure().randomLong());
      return arg;
    }).when(repository).save(any(Pizza.class));
    doCallRealMethod().when(mapper).toDto(any(Pizza.class));

    final PizzaDto result = underTest.save(request);

    Assertions.assertThat(result)
        .isNotNull()
        .matches(p -> Objects.equals(p.getName(), request.getName()))
        .matches(p -> Objects.equals(p.getDescription(), request.getDescription()))
        .matches(p -> Objects.nonNull(p.getCreatedDate()))
        .matches(p -> Objects.nonNull(p.getLastModifiedDate()))
        .matches(p -> Objects.nonNull(p.getId()));

    verify(mapper).toEntity(any(PizzaRequest.class));
    verify(repository).save(any(Pizza.class));
    verify(mapper).toDto(any(Pizza.class));
  }

  private static Stream<Arguments> update_ParametersKO() {
    return Stream.of(
        Arguments.of(null, null),
        Arguments.of(1L, null),
        Arguments.of(null, new PizzaRequest())
    );
  }

  @ParameterizedTest
  @MethodSource("update_ParametersKO")
  void update_WhenInvalidInput_ShouldThrow(final Long id, final PizzaRequest request) {
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
    final PizzaRequest request = Instancio.create(PizzaRequest.class);

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
    final Pizza entity = Instancio.of(Pizza.class)
        .set(field(Pizza::getId), id)
        .create();
    final PizzaRequest request = Instancio.create(PizzaRequest.class);

    doReturn(Optional.of(entity)).when(repository).findById(id);
    doCallRealMethod().when(mapper).patch(request, entity);
    doAnswer(invocation -> {
      final Pizza arg = invocation.getArgument(0, Pizza.class);
      arg.setName(request.getName());
      arg.setDescription(request.getDescription());
      arg.setLastModifiedDate(newModifiedDate);
      return arg;
    }).when(repository).save(any(Pizza.class));
    doCallRealMethod().when(mapper).toDto(any(Pizza.class));

    final PizzaDto result = underTest.update(id, request);

    Assertions.assertThat(result)
        .isNotNull()
        .matches(p -> Objects.equals(p.getName(), request.getName()))
        .matches(p -> Objects.equals(p.getDescription(), request.getDescription()))
        .matches(p -> Objects.equals(p.getCreatedDate().toInstant(), entity.getCreatedDate()))
        .matches(p -> Objects.equals(p.getLastModifiedDate().toInstant(), newModifiedDate))
        .matches(p -> Objects.equals(p.getId(), entity.getId()));

    verify(repository).findById(id);
    verify(mapper).patch(request, entity);
    verify(repository).save(any(Pizza.class));
    verify(mapper).toDto(any(Pizza.class));
  }

}
