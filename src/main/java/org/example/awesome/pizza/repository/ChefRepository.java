package org.example.awesome.pizza.repository;

import org.example.awesome.pizza.domain.Chef;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChefRepository extends CrudRepository<Chef, Long> {
}
