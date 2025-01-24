package org.example.awesome.pizza.mapper;

import org.example.awesome.pizza.domain.Pizza;
import org.example.awesome.pizza.model.PizzaDto;
import org.example.awesome.pizza.model.PizzaRequest;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, uses = {MapperUtils.class})
public interface PizzaMapper extends BaseMapper<PizzaDto, PizzaRequest, Pizza> {

  void patch(final PizzaRequest source, @MappingTarget final Pizza target);
}
