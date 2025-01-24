package org.example.awesome.pizza.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.example.awesome.pizza.domain.Pizza;
import org.example.awesome.pizza.mapper.PizzaMapper;
import org.example.awesome.pizza.model.PizzaDto;
import org.example.awesome.pizza.model.PizzaRequest;
import org.example.awesome.pizza.repository.PizzaRepository;
import org.example.awesome.pizza.service.PizzaService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class PizzaServiceImpl extends BaseService<PizzaDto, PizzaRequest, Pizza> implements PizzaService {
  private final PizzaRepository repo;

  PizzaServiceImpl(final PizzaRepository repository, final PizzaMapper mapper) {
    super(repository, mapper);
    this.repo = repository;
  }

  /**
   * Find list of Pizza filtered by input text on name or description Pizza fields, when passed
   * @param searchText: string to search: optional, if empty, finds all pizzas
   * @return a list of Pizza model
   */
  public List<PizzaDto> findByFilter(final String searchText) {
    final String actualFilter = StringUtils.defaultString(searchText)
        .toLowerCase();

    return Optional.of(repo.findByText(actualFilter)).stream()
        .flatMap(Collection::stream)
        .map(mapper::toDto)
        .toList();
  }

}
