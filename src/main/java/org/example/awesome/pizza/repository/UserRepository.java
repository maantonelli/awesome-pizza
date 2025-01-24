package org.example.awesome.pizza.repository;

import org.example.awesome.pizza.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository<T extends User> extends JpaRepository<T, Long> {
  @Query(value = """
      SELECT u FROM User u WHERE u.username = ?1
      """)
  Optional<T> findByUsername(final String username);
}
