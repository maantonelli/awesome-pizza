package org.example.awesome.pizza.mapper;

import org.example.awesome.pizza.domain.Chef;
import org.example.awesome.pizza.domain.Order;
import org.example.awesome.pizza.domain.Pizza;
import org.example.awesome.pizza.model.OrderDto;
import org.example.awesome.pizza.model.OrderInternalReq;
import org.example.awesome.pizza.model.OrderRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, uses = {MapperUtils.class})
public interface OrderMapper extends BaseMapper<OrderDto, OrderInternalReq, Order> {

  @Mapping(target = "status", defaultValue = "CREATED")
  @Mapping(target = "chef", ignore = true)
  Order toEntity(final OrderInternalReq orderRequest);

  @Mapping(target = "totalAmount", source = "pizzas", qualifiedByName = "calcTotalAmount")
  OrderDto toDto(final Order order);

  @Mapping(target = "pizzas", ignore = true)
  @Mapping(target = "target.chef", source = "source", qualifiedByName = "patchChef")
  void patch(final OrderInternalReq source, @MappingTarget final Order target);

  OrderInternalReq toInternalReq(final OrderRequest source);

  @Named("calcTotalAmount")
  default BigDecimal totalAmount(final List<Pizza> pizzas) {
    if (CollectionUtils.isEmpty(pizzas))
      return null;

    return pizzas.stream()
        .filter(p -> p.getPrice() != null)
        .reduce(
            BigDecimal.ZERO,
            (partial, current) -> partial.add(current.getPrice()),
            BigDecimal::add)
        ;
  }

  @Named("patchChef")
  default Chef patchChef(final OrderInternalReq source) {
    if (source == null || source.getChefId() == null)
      return null;

    return (Chef) new Chef().setId(source.getChefId());
  }

  default Pizza toPizza(final Long id) {
    return (Pizza) new Pizza().setId(id);
  }
}
