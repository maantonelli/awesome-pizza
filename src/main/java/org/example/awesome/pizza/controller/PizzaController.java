package org.example.awesome.pizza.controller;

import lombok.RequiredArgsConstructor;
import org.example.awesome.pizza.model.PizzaDto;
import org.example.awesome.pizza.model.PizzaRequest;
import org.example.awesome.pizza.service.PizzaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PizzaController implements PizzaApi {
  private final PizzaService service;

  /**
   * REST Endpoint for creating new Pizza entity: for Pizza restaurant staff only
   * @param pizzaRequest Pizza object to be added to the DB (optional)
   * @return the saved PizzaDto instance on DB
   */
  @Override
  public ResponseEntity<PizzaDto> createPizza(PizzaRequest pizzaRequest) {
    final PizzaDto saved = service.save(pizzaRequest);
    return ResponseEntity
        .created(URI.create("/pizza/%d".formatted(saved.getId())))
        .body(saved);
  }

  /**
   * REST Endpoint for deleting a Pizza entity from DB: for Pizza restaurant staff only
   * @param id ID of the entity (required)
   * @return nothing
   */
  @Override
  public ResponseEntity<Void> deletePizza(Long id) {
    service.delete(id);
    return ResponseEntity.ok().build();
  }

  /**
   * REST Endpoint for searching Pizza entities optionally by a searchString, that filters both
   * Pizza name and description: mainly for Customer use, for choosing his/her Pizza
   * @param searchString String to filter on both name or description (optional)
   * @return a list of PizzaDto entity
   */
  @Override
  public ResponseEntity<List<PizzaDto>> findPizzas(String searchString) {
    final List<PizzaDto> result = service.findByFilter(searchString);
    return ResponseEntity.ok(result);
  }

  /**
   * REST Endpoint for updating name and/or description of Pizza entity by its ID: for Pizza restaurant staff use
   * @param id ID of the entity (required)
   * @param pizzaRequest Pizza object to be added to the DB (optional)
   * @return updated PizzaDto instance
   */
  @Override
  public ResponseEntity<PizzaDto> updatePizza(Long id, PizzaRequest pizzaRequest) {
    final PizzaDto updated = service.update(id, pizzaRequest);
    return ResponseEntity.ok(updated);
  }
}
