package org.example.awesome.pizza.repository;

import org.example.awesome.pizza.domain.Chef;
import org.springframework.stereotype.Repository;

@Repository
public interface ChefRepository extends UserRepository<Chef> {
}
