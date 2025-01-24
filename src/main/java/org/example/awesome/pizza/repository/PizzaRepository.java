package org.example.awesome.pizza.repository;

import org.example.awesome.pizza.domain.Pizza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PizzaRepository extends JpaRepository<Pizza, Long> {
  @Query("select p from Pizza p where lower(p.name) like %?1% escape '\\' or lower(p.description) like %?1% escape '\\'")
  List<Pizza> findByText(final String searchText);
}
