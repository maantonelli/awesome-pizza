package org.example.awesome.pizza.repository;

import org.example.awesome.pizza.domain.Customer;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends UserRepository<Customer> {
}
