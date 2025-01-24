package org.example.awesome.pizza.repository;

import jakarta.annotation.Nonnull;
import org.example.awesome.pizza.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
  Optional<Order> findOneByCode(final Long code);

  @Query(value = """
      SELECT o FROM Order o WHERE o.status IN ?1 ORDER BY o.createdDate ASC
      """)
  List<Order> findAllSortedByCreatedDate(@Nonnull final String... statuses);

  @Query(value = """
      SELECT o FROM Order o WHERE o.chef.id = ?1 AND o.status = 'COOKING'
      """)
  Optional<Order> findCookingOrder(final Long chefId);

  @Query(value = """
      SELECT COUNT(1) < ?2 as boolean FROM Order o WHERE o.chef.id = ?1 AND  o.status = 'COOKING'
      """)
  boolean canTakeAnyOrder(final Long chefId, final Long threshold);
}
