package org.example.awesome.pizza.service;

import org.example.awesome.pizza.model.PizzaDto;
import org.example.awesome.pizza.model.PizzaRequest;

import java.util.List;

public interface PizzaService extends
    BaseCreateService<PizzaDto, PizzaRequest>,
    BaseUpdateService<PizzaDto, PizzaRequest>,
    BaseDeleteService {
  List<PizzaDto> findByFilter(final String searchText);
}
